package com.example.currency_converter.repository

import com.example.currency_converter.R
import com.example.currency_converter.data.CurrencyDatabase
import com.example.currency_converter.data.ExchangeRatesDao
import com.example.currency_converter.data.SelectedCurrenciesDao
import com.example.currency_converter.model.Currency
import com.example.currency_converter.model.ExchangeRateEntity
import com.example.currency_converter.model.SelectedCurrencyEntity
import com.example.currency_converter.network.CurrencyApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Репозиторий для работы с валютами, курсами обмена и пользовательскими предпочтениями.
 */
class CurrencyRepository(
    private val database: CurrencyDatabase,
    private val apiService: CurrencyApiService
) {
    private val exchangeRatesDao: ExchangeRatesDao = database.exchangeRatesDao()
    private val selectedCurrenciesDao: SelectedCurrenciesDao = database.selectedCurrenciesDao()

    // Кэш для обменных курсов (для быстрого доступа)
    private var cachedExchangeRates: Map<String, Double>? = null

    // Базовая валюта по умолчанию
    private val defaultBaseCurrency = "USD"

    /**
     * Получает список всех доступных валют.
     * @return Список валют приложения.
     */
    fun getAvailableCurrencies(): List<Currency> = AVAILABLE_CURRENCIES

    /**
     * Получает информацию о валюте по её коду.
     * @param code Код валюты.
     * @return Объект валюты или null, если валюта не найдена.
     */
    fun getCurrencyByCode(code: String): Currency? {
        return AVAILABLE_CURRENCIES.find { it.code == code }
    }

    /**
     * Получает список выбранных пользователем валют.
     * @return Список выбранных валют, отсортированный по позиции.
     */
    suspend fun getSelectedCurrencies(): List<Currency> = withContext(Dispatchers.IO) {
        try {
            val selectedEntities = selectedCurrenciesDao.getSelectedCurrencies()

            // Если нет выбранных валют, добавляем валюты по умолчанию
            if (selectedEntities.isEmpty()) {
                return@withContext addDefaultCurrencies()
            }

            // Иначе возвращаем выбранные валюты в правильном порядке
            return@withContext AVAILABLE_CURRENCIES
                .filter { currency ->
                    selectedEntities.any { entity -> entity.currencyCode == currency.code }
                }
                .map { currency ->
                    // Находим позицию из сущности
                    val entityPosition = selectedEntities.find {
                        it.currencyCode == currency.code
                    }?.position ?: Int.MAX_VALUE

                    currency.copy(
                        isSelected = true,
                        position = entityPosition
                    )
                }
                .sortedBy { it.position }
        } catch (e: Exception) {
            Timber.e(e, "Error getting selected currencies")
            emptyList()
        }
    }

    /**
     * Добавляет валюты по умолчанию, если нет выбранных.
     * @return Список добавленных валют по умолчанию.
     */
    private suspend fun addDefaultCurrencies(): List<Currency> = withContext(Dispatchers.IO) {
        val defaultCurrencyCodes = listOf("USD", "EUR", "GBP")
        val defaultCurrencies = mutableListOf<SelectedCurrencyEntity>()

        defaultCurrencyCodes.forEachIndexed { index, code ->
            defaultCurrencies.add(SelectedCurrencyEntity(code, index))
        }

        try {
            defaultCurrencies.forEach {
                selectedCurrenciesDao.insertSelectedCurrency(it)
            }

            return@withContext AVAILABLE_CURRENCIES
                .filter { it.code in defaultCurrencyCodes }
                .map { it.copy(isSelected = true) }
                .sortedBy { defaultCurrencyCodes.indexOf(it.code) }
        } catch (e: Exception) {
            Timber.e(e, "Error adding default currencies")
            emptyList()
        }
    }

    /**
     * Добавляет валюту в список выбранных.
     * @param currencyCode Код добавляемой валюты.
     */
    suspend fun addCurrencyToSelected(currencyCode: String) = withContext(Dispatchers.IO) {
        try {
            val selectedCurrencies = selectedCurrenciesDao.getSelectedCurrencies()
            val newPosition = selectedCurrencies.size
            selectedCurrenciesDao.insertSelectedCurrency(
                SelectedCurrencyEntity(currencyCode, newPosition)
            )
            true
        } catch (e: Exception) {
            Timber.e(e, "Error adding currency to selected")
            false
        }
    }

    /**
     * Удаляет валюту из списка выбранных.
     * @param currencyCode Код удаляемой валюты.
     */
    suspend fun removeCurrencyFromSelected(currencyCode: String) = withContext(Dispatchers.IO) {
        try {
            selectedCurrenciesDao.deleteByCode(currencyCode)
            true
        } catch (e: Exception) {
            Timber.e(e, "Error removing currency from selected")
            false
        }
    }

    /**
     * Загружает актуальные обменные курсы с API.
     * @param baseCurrency Базовая валюта для курсов (по умолчанию USD).
     * @return Результат с картой обменных курсов или ошибкой.
     */
    suspend fun fetchLatestRates(baseCurrency: String = defaultBaseCurrency): Result<Map<String, Double>> {
        return withContext(Dispatchers.IO) {
            try {
                // В реальном приложении делаем вызов API
                // Сейчас используем моковые данные
                val rates = getMockExchangeRates()

                // Кэшируем курсы в БД
                val entities = rates.map { (code, rate) ->
                    ExchangeRateEntity(code, rate, System.currentTimeMillis())
                }
                exchangeRatesDao.insertAllRates(entities)

                // Обновляем локальный кэш
                cachedExchangeRates = rates

                Result.success(rates)
            } catch (e: Exception) {
                Timber.e(e, "Error fetching latest rates")

                // Если API не сработал, пробуем получить кэшированные курсы
                val cachedRates = getCachedRates()
                if (cachedRates.isNotEmpty()) {
                    Result.success(cachedRates)
                } else {
                    Result.failure(e)
                }
            }
        }
    }

    /**
     * Получает кэшированные курсы из базы данных.
     * @return Карта курсов валют.
     */
    private suspend fun getCachedRates(): Map<String, Double> = withContext(Dispatchers.IO) {
        cachedExchangeRates?.let { return@withContext it }

        try {
            val rateEntities = exchangeRatesDao.getAllRates()
            val rates = rateEntities.associate { it.currencyCode to it.rate }
            cachedExchangeRates = rates
            rates
        } catch (e: Exception) {
            Timber.e(e, "Error getting cached rates")
            getMockExchangeRates()
        }
    }

    /**
     * Проверяет, нужно ли обновлять курсы (старше 6 часов).
     * @return true, если нужно обновление, иначе false.
     */
    suspend fun needsRateUpdate(): Boolean = withContext(Dispatchers.IO) {
        try {
            val lastUpdate = exchangeRatesDao.getLastUpdateTime() ?: 0
            val sixHoursInMillis = TimeUnit.HOURS.toMillis(6)
            System.currentTimeMillis() - lastUpdate > sixHoursInMillis
        } catch (e: Exception) {
            Timber.e(e, "Error checking if rates need update")
            true // при ошибке лучше обновить
        }
    }

    /**
     * Конвертирует сумму из одной валюты в другую.
     * @param amount Конвертируемая сумма.
     * @param fromCurrency Исходная валюта.
     * @param toCurrency Целевая валюта.
     * @return Сконвертированная сумма.
     */
    suspend fun convertCurrency(
        amount: Double,
        fromCurrency: String,
        toCurrency: String
    ): Double = withContext(Dispatchers.IO) {
        if (fromCurrency == toCurrency) return@withContext amount

        try {
            val rates = getCachedRates()

            // Получаем курсы относительно базовой валюты
            val fromRate = rates[fromCurrency] ?: 1.0
            val toRate = rates[toCurrency] ?: 1.0

            // Проверяем корректность курсов
            if (fromRate <= 0) {
                Timber.w("Invalid exchange rate for $fromCurrency: $fromRate")
                return@withContext 0.0
            }

            // Конвертируем и округляем до 2 знаков
            val convertedAmount = amount * (toRate / fromRate)
            return@withContext (Math.round(convertedAmount * 100) / 100.0)
        } catch (e: Exception) {
            Timber.e(e, "Error converting currency")
            0.0
        }
    }

    /**
     * Возвращает моковые обменные курсы для тестирования.
     * @return Карта обменных курсов.
     */
    private fun getMockExchangeRates(): Map<String, Double> {
        return mapOf(
            "USD" to 1.0,
            "EUR" to 0.92,
            "GBP" to 0.8,
            "JPY" to 110.0,
            "GEL" to 2.75,
            "THB" to 33.5,
            "AED" to 3.67,
            "VND" to 25000.0,
            "RUB" to 83.5
        )
    }

    companion object {
        /**
         * Список доступных валют приложения.
         */
        private val AVAILABLE_CURRENCIES = listOf(
            Currency("USD", "US Dollar", "$", R.drawable.flag_usa),
            Currency("GEL", "Georgian Lari", "₾", R.drawable.flag_georgia),
            Currency("THB", "Thai Baht", "฿", R.drawable.flag_thailand),
            Currency("AED", "UAE Dirham", "د.إ", R.drawable.flag_uae),
            Currency("VND", "Vietnamese Dong", "₫", R.drawable.flag_vietnam),
            Currency("RUB", "Russian Ruble", "₽", R.drawable.flag_russia),
            //Currency("EUR", "Euro", "€", R.drawable.flag_euro),
            //Currency("GBP", "British Pound", "£", R.drawable.flag_uk)
        )
    }
}
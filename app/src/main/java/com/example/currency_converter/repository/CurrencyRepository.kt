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
 * Repository class for handling currency-related operations including:
 * - Managing available currencies
 * - Handling selected currencies
 * - Fetching and caching exchange rates
 * - Performing currency conversions
 */
class CurrencyRepository(
    private val database: CurrencyDatabase,
    private val apiService: CurrencyApiService
) {
    private val exchangeRatesDao: ExchangeRatesDao = database.exchangeRatesDao()
    private val selectedCurrenciesDao: SelectedCurrenciesDao = database.selectedCurrenciesDao()

    // Cache for exchange rates to improve performance
    private var cachedExchangeRates: Map<String, Double>? = null

    // Default base currency for exchange rates
    private val defaultBaseCurrency = "USD"

    /**
     * Retrieves the list of all available currencies in the application
     * @return List of available currencies
     */
    fun getAvailableCurrencies(): List<Currency> = AVAILABLE_CURRENCIES

    /**
     * Finds a currency by its code
     * @param code Currency code to search for
     * @return Currency object if found, null otherwise
     */
    fun getCurrencyByCode(code: String): Currency? {
        return AVAILABLE_CURRENCIES.find { it.code == code }
    }

    /**
     * Retrieves the list of user-selected currencies
     * @return List of selected currencies sorted by position
     */
    suspend fun getSelectedCurrencies(): List<Currency> = withContext(Dispatchers.IO) {
        try {
            val selectedEntities = selectedCurrenciesDao.getSelectedCurrencies()

            // If no currencies are selected, add default currencies
            if (selectedEntities.isEmpty()) {
                return@withContext addDefaultCurrencies()
            }

            // Return selected currencies in correct order
            return@withContext AVAILABLE_CURRENCIES
                .filter { currency ->
                    selectedEntities.any { entity -> entity.currencyCode == currency.code }
                }
                .map { currency ->
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
     * Adds default currencies if none are selected
     * @return List of default currencies
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
     * Adds a currency to the list of selected currencies
     * @param currencyCode Code of the currency to add
     * @return true if successful, false otherwise
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
     * Removes a currency from the list of selected currencies
     * @param currencyCode Code of the currency to remove
     * @return true if successful, false otherwise
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
     * Fetches the latest exchange rates from the API
     * @param baseCurrency Base currency for rates (defaults to USD)
     * @return Result containing either the rates map or an error
     */
    suspend fun fetchLatestRates(baseCurrency: String = defaultBaseCurrency): Result<Map<String, Double>> {
        return withContext(Dispatchers.IO) {
            try {
                // In a real application, this would make an API call
                // Currently using mock data
                val rates = getMockExchangeRates()

                // Cache rates in the database
                val entities = rates.map { (code, rate) ->
                    ExchangeRateEntity(code, rate, System.currentTimeMillis())
                }
                exchangeRatesDao.insertAllRates(entities)

                // Update local cache
                cachedExchangeRates = rates

                Result.success(rates)
            } catch (e: Exception) {
                Timber.e(e, "Error fetching latest rates")

                // If API fails, try to get cached rates
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
     * Retrieves cached exchange rates from the database
     * @return Map of currency codes to exchange rates
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
     * Checks if exchange rates need to be updated (older than 6 hours)
     * @return true if update is needed, false otherwise
     */
    suspend fun needsRateUpdate(): Boolean = withContext(Dispatchers.IO) {
        try {
            val lastUpdate = exchangeRatesDao.getLastUpdateTime() ?: 0
            val sixHoursInMillis = TimeUnit.HOURS.toMillis(6)
            System.currentTimeMillis() - lastUpdate > sixHoursInMillis
        } catch (e: Exception) {
            Timber.e(e, "Error checking if rates need update")
            true // Better to update on error
        }
    }

    /**
     * Converts an amount from one currency to another
     * @param amount Amount to convert
     * @param fromCurrency Source currency code
     * @param toCurrency Target currency code
     * @return Converted amount
     */
    suspend fun convertCurrency(
        amount: Double,
        fromCurrency: String,
        toCurrency: String
    ): Double = withContext(Dispatchers.IO) {
        if (fromCurrency == toCurrency) return@withContext amount

        try {
            val rates = getCachedRates()

            // Get rates relative to base currency
            val fromRate = rates[fromCurrency] ?: 1.0
            val toRate = rates[toCurrency] ?: 1.0

            // Validate rates
            if (fromRate <= 0) {
                Timber.w("Invalid exchange rate for $fromCurrency: $fromRate")
                return@withContext 0.0
            }

            // Convert and round to 2 decimal places
            val convertedAmount = amount * (toRate / fromRate)
            return@withContext (Math.round(convertedAmount * 100) / 100.0)
        } catch (e: Exception) {
            Timber.e(e, "Error converting currency")
            0.0
        }
    }

    /**
     * Provides mock exchange rates for testing purposes
     * @return Map of currency codes to mock exchange rates
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
         * List of available currencies in the application
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
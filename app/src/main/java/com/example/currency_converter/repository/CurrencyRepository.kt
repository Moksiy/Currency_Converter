package com.example.currency_converter.repository

import com.example.currency_converter.data.CurrencyDatabase
import com.example.currency_converter.data.ExchangeRatesDao
import com.example.currency_converter.data.SelectedCurrenciesDao
import com.example.currency_converter.model.Currency
import com.example.currency_converter.model.ExchangeRateEntity
import com.example.currency_converter.model.ExchangeRatesResponse
import com.example.currency_converter.model.SelectedCurrencyEntity
import com.example.currency_converter.network.CurrencyApiService
import com.example.currency_converter.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class CurrencyRepository(
    private val database: CurrencyDatabase,
    private val apiService: CurrencyApiService
) {
    private val exchangeRatesDao: ExchangeRatesDao = database.exchangeRatesDao()
    private val selectedCurrenciesDao: SelectedCurrenciesDao = database.selectedCurrenciesDao()

    // Default list of available currencies
    // We would typically get this from an API, but for simplicity, we'll hardcode some currencies
    private val availableCurrencies = listOf(
        Currency("USD", "US Dollar", "$", R.drawable.flag_usa),
        //Currency("EUR", "Euro", "€", R.drawable.flag_eu),
        //Currency("GBP", "British Pound", "£", R.drawable.flag_uk),
        //Currency("JPY", "Japanese Yen", "¥", R.drawable.flag_japan),
        //Currency("AUD", "Australian Dollar", "A$", R.drawable.flag_australia),
        //Currency("CAD", "Canadian Dollar", "C$", R.drawable.flag_canada),
        Currency("GEL", "Georgian Lari", "₾", R.drawable.flag_georgia),
        Currency("THB", "Thai Baht", "฿", R.drawable.flag_thailand),
        Currency("AED", "UAE Dirham", "د.إ", R.drawable.flag_uae),
        Currency("VND", "Vietnamese Dong", "₫", R.drawable.flag_vietnam),
        Currency("RUB", "Russian Ruble", "₽", R.drawable.flag_russia),
        //Currency("INR", "Indian Rupee", "₹", R.drawable.flag_india),
        //Currency("CNY", "Chinese Yuan", "¥", R.drawable.flag_china),
        //Currency("BRL", "Brazilian Real", "R$", R.drawable.flag_brazil),
        //Currency("MXN", "Mexican Peso", "$", R.drawable.flag_mexico)
    )

    // Get all available currencies
    fun getAvailableCurrencies(): List<Currency> {
        return availableCurrencies
    }

    // Get currency by code
    fun getCurrencyByCode(code: String): Currency? {
        return availableCurrencies.find { it.code == code }
    }

    // Get selected currencies
    suspend fun getSelectedCurrencies(): List<Currency> = withContext(Dispatchers.IO) {
        val selectedEntities = selectedCurrenciesDao.getSelectedCurrencies()
        if (selectedEntities.isEmpty()) {
            // If no currencies are selected, add default currencies (USD and EUR)
            val defaultCurrencies = listOf(
                SelectedCurrencyEntity("USD", 0),
                SelectedCurrencyEntity("EUR", 1)
            )
            defaultCurrencies.forEach { selectedCurrenciesDao.insertSelectedCurrency(it) }
            return@withContext availableCurrencies
                .filter { it.code in defaultCurrencies.map { entity -> entity.currencyCode } }
                .apply { forEach { it.isSelected = true } }
        }

        return@withContext availableCurrencies
            .filter { it.code in selectedEntities.map { entity -> entity.currencyCode } }
            .apply { forEach { it.isSelected = true } }
            .sortedBy { currency ->
                selectedEntities.find { it.currencyCode == currency.code }?.position ?: Int.MAX_VALUE
            }
    }

    // Add a currency to selected list
    suspend fun addCurrencyToSelected(currencyCode: String) = withContext(Dispatchers.IO) {
        val selectedCurrencies = selectedCurrenciesDao.getSelectedCurrencies()
        val newPosition = selectedCurrencies.size
        selectedCurrenciesDao.insertSelectedCurrency(
            SelectedCurrencyEntity(currencyCode, newPosition)
        )
    }

    // Remove a currency from selected list
    suspend fun removeCurrencyFromSelected(currencyCode: String) = withContext(Dispatchers.IO) {
        selectedCurrenciesDao.deleteByCode(currencyCode)
    }

    // Fetch latest exchange rates
    suspend fun fetchLatestRates(baseCurrency: String = "USD"): Result<Map<String, Double>> {
        return try {
            val response = apiService.getLatestRates(baseCurrency)

            // Cache the rates
            val entities = response.rates.map { (code, rate) ->
                ExchangeRateEntity(code, rate, System.currentTimeMillis())
            }
            exchangeRatesDao.insertAllRates(entities)

            Result.success(response.rates)
        } catch (e: Exception) {
            // If API call fails, try to get cached rates
            val cachedRates = getCachedRates()
            if (cachedRates.isNotEmpty()) {
                Result.success(cachedRates)
            } else {
                Result.failure(e)
            }
        }
    }

    // Get cached exchange rates
    private suspend fun getCachedRates(): Map<String, Double> = withContext(Dispatchers.IO) {
        val rateEntities = exchangeRatesDao.getAllRates()
        return@withContext rateEntities.associate { it.currencyCode to it.rate }
    }

    // Check if rates need update (older than 6 hours)
    suspend fun needsRateUpdate(): Boolean = withContext(Dispatchers.IO) {
        val lastUpdate = exchangeRatesDao.getLastUpdateTime() ?: 0
        val sixHoursInMillis = TimeUnit.HOURS.toMillis(6)
        return@withContext System.currentTimeMillis() - lastUpdate > sixHoursInMillis
    }

    // Convert amount from one currency to another
    suspend fun convertCurrency(
        amount: Double,
        fromCurrency: String,
        toCurrency: String
    ): Double {
        val rates = getCachedRates()

        // If we don't have rates, return 0
        if (rates.isEmpty()) return 0.0

        // Get rates for both currencies (relative to base currency, usually USD)
        val fromRate = rates[fromCurrency] ?: 1.0
        val toRate = rates[toCurrency] ?: 1.0

        // Conversion formula: amount * (toRate / fromRate)
        return amount * (toRate / fromRate)
    }
}
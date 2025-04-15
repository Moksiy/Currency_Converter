package com.example.currency_converter.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.currency_converter.data.CurrencyDatabase
import com.example.currency_converter.data.CurrencyPreferencesManager
import com.example.currency_converter.model.Currency
import com.example.currency_converter.network.RetrofitClient
import com.example.currency_converter.repository.CurrencyRepository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

/**
 * ViewModel that manages currency data, conversions and user preferences.
 */
class CurrencyViewModel(application: Application) : AndroidViewModel(application) {
    // Repository layer
    private val database = CurrencyDatabase.getDatabase(application)
    private val apiService = RetrofitClient.currencyApiService
    private val repository = CurrencyRepository(database, apiService)
    private val prefsManager = CurrencyPreferencesManager(application)

    // UI state
    private val _selectedCurrencies = MutableLiveData<List<Currency>>()
    val selectedCurrencies: LiveData<List<Currency>> = _selectedCurrencies

    private val _availableCurrencies = MutableLiveData<List<Currency>>()
    val availableCurrencies: LiveData<List<Currency>> = _availableCurrencies

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Business logic state
    private val decimalFormat = (NumberFormat.getNumberInstance(Locale.getDefault()) as DecimalFormat).apply {
        applyPattern("#,##0.00")
    }

    private var activeCurrencyCode: String = DEFAULT_CURRENCY_CODE

    // Coroutine exception handler
    private val errorHandler = CoroutineExceptionHandler { _, exception ->
        _error.postValue("Error: ${exception.localizedMessage ?: exception.message ?: "Unknown error"}")
        _isLoading.postValue(false)
    }

    init {
        loadAllCurrencies()
        loadSelectedCurrencies()
        fetchLatestRates()
    }

    /**
     * Clears the current error message.
     */
    fun clearError() {
        _error.value = ""
    }

    /**
     * Loads user selected currencies from preferences.
     */
    private fun loadSelectedCurrencies() {
        launchWithErrorHandling {
            val selectedCodes = prefsManager.getSelectedCurrencies()
            val allCurrencies = repository.getAvailableCurrencies()

            val selectedCurrencies = allCurrencies
                .filter { it.code in selectedCodes }
                .map { it.copy(isSelected = true) }

            _selectedCurrencies.postValue(selectedCurrencies)

            // Set first currency as active if available
            if (selectedCurrencies.isNotEmpty()) {
                activeCurrencyCode = selectedCurrencies[0].code
            }
        }
    }

    /**
     * Loads all available currencies.
     */
    private fun loadAllCurrencies() {
        launchWithErrorHandling {
            val allCurrencies = repository.getAvailableCurrencies()
            val selectedCodes = prefsManager.getSelectedCurrencies()

            val updatedCurrencies = allCurrencies.map { currency ->
                currency.copy(isSelected = currency.code in selectedCodes)
            }

            _availableCurrencies.postValue(updatedCurrencies)
        }
    }

    /**
     * Fetches latest exchange rates from the API.
     */
    fun fetchLatestRates() {
        launchWithErrorHandling {
            _isLoading.postValue(true)

            // TODO: Replace mock data with actual API call
            // For development only - mock data
            fetchMockExchangeRates()

            _isLoading.postValue(false)
        }
    }

    /**
     * Temporary method to provide mock exchange rates.
     * Should be removed in production.
     */
    private suspend fun fetchMockExchangeRates() {
        // Simulate network delay
        delay(500)

        // This should be replaced with actual API call in production
        /* Example:
        val rates = repository.fetchLatestRates("USD")
        _exchangeRates.postValue(rates)
        */
    }

    /**
     * Adds a currency to the selected list.
     * @param currencyCode The code of the currency to add.
     */
    fun addCurrency(currencyCode: String) {
        prefsManager.addCurrency(currencyCode)
        refreshCurrencyLists()
    }

    /**
     * Removes a currency from the selected list.
     * @param currencyCode The code of the currency to remove.
     */
    fun removeCurrency(currencyCode: String) {
        prefsManager.removeCurrency(currencyCode)
        refreshCurrencyLists()
    }

    /**
     * Checks if a currency is selected by the user.
     * @param currencyCode The code of the currency to check.
     * @return True if the currency is selected, false otherwise.
     */
    fun isCurrencySelected(currencyCode: String): Boolean {
        return prefsManager.isCurrencySelected(currencyCode)
    }

    /**
     * Refreshes both currency lists after user preference changes.
     */
    private fun refreshCurrencyLists() {
        loadAllCurrencies()
        loadSelectedCurrencies()
    }

    /**
     * Sets the active currency for input.
     * @param currencyCode The code of the currency to set as active.
     */
    fun setActiveCurrency(currencyCode: String) {
        activeCurrencyCode = currencyCode
    }

    /**
     * Updates the amount for the active currency and converts to all others.
     * @param amount The new amount for the active currency.
     */
    fun updateCurrencyAmount(amount: Double) {
        launchWithErrorHandling {
            val currentCurrencies = _selectedCurrencies.value ?: return@launchWithErrorHandling

            val updatedCurrencies = currentCurrencies.map { currency ->
                if (currency.code == activeCurrencyCode) {
                    currency.copy(amount = amount)
                } else {
                    val convertedAmount = repository.convertCurrency(
                        amount,
                        activeCurrencyCode,
                        currency.code
                    )
                    currency.copy(amount = convertedAmount)
                }
            }

            _selectedCurrencies.postValue(updatedCurrencies)
        }
    }

    /**
     * Formats a currency amount for display.
     * @param amount The amount to format.
     * @return Formatted string representation of the amount.
     */
    fun formatAmount(amount: Double): String {
        return decimalFormat.format(amount)
    }

    /**
     * Helper method to launch coroutines with error handling.
     */
    private fun launchWithErrorHandling(block: suspend () -> Unit) {
        viewModelScope.launch(errorHandler) {
            try {
                block()
            } catch (e: Exception) {
                _error.postValue("Error: ${e.localizedMessage ?: e.message ?: "Unknown error"}")
            }
        }
    }

    companion object {
        private const val DEFAULT_CURRENCY_CODE = "USD"
    }
}
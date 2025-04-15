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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DecimalFormat

class CurrencyViewModel(application: Application) : AndroidViewModel(application) {
    // Initialize database, API service, and repository
    private val database = CurrencyDatabase.getDatabase(application)
    private val apiService = RetrofitClient.currencyApiService
    private val repository = CurrencyRepository(database, apiService)

    // Preferences manager для сохранения выбранных валют
    private val prefsManager = CurrencyPreferencesManager(application)

    // LiveData for selected currencies
    private val _selectedCurrencies = MutableLiveData<List<Currency>>()
    val selectedCurrencies: LiveData<List<Currency>> = _selectedCurrencies

    // LiveData for available currencies
    private val _availableCurrencies = MutableLiveData<List<Currency>>()
    val availableCurrencies: LiveData<List<Currency>> = _availableCurrencies

    // LiveData for API/network errors
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    // LiveData for loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Decimal formatter for currency display
    private val decimalFormat = DecimalFormat("#,##0.00")

    // Active currency (the one user is inputting value for)
    private var activeCurrencyCode: String = "USD"

    init {
        // Load selected currencies and exchange rates on initialization
        loadAllCurrencies()
        loadSelectedCurrencies()
        fetchLatestRates()
    }

    // Load user's selected currencies from preferences
    private fun loadSelectedCurrencies() {
        viewModelScope.launch {
            try {
                // Получаем коды выбранных валют из SharedPreferences
                val selectedCodes = prefsManager.getSelectedCurrencies()

                // Загружаем полные объекты валют
                val allCurrencies = repository.getAvailableCurrencies()

                // Фильтруем только выбранные
                val selectedCurrencies = allCurrencies
                    .filter { it.code in selectedCodes }
                    .map { it.copy(isSelected = true) }

                _selectedCurrencies.value = selectedCurrencies

                // Set active currency to first in the list if available
                if (selectedCurrencies.isNotEmpty()) {
                    activeCurrencyCode = selectedCurrencies[0].code
                }
            } catch (e: Exception) {
                _error.value = "Failed to load selected currencies: ${e.message}"
            }
        }
    }

    // Load all available currencies
    private fun loadAllCurrencies() {
        val allCurrencies = repository.getAvailableCurrencies()

        // Обновляем флаг isSelected для каждой валюты
        val selectedCodes = prefsManager.getSelectedCurrencies()
        val updatedCurrencies = allCurrencies.map { currency ->
            currency.copy(isSelected = currency.code in selectedCodes)
        }

        _availableCurrencies.value = updatedCurrencies
    }

    // Fetch latest exchange rates
    fun fetchLatestRates() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Временно используем моковые данные вместо реального API
                val mockRates = mapOf(
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

                // Имитация задержки сети
                delay(500)

                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Error checking rate update status: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    // Add a currency to selected list
    fun addCurrency(currencyCode: String) {
        // Сохраняем в preferences
        prefsManager.addCurrency(currencyCode)

        // Обновляем список выбранных валют
        refreshCurrencyLists()
    }

    // Remove a currency from selected list
    fun removeCurrency(currencyCode: String) {
        // Удаляем из preferences
        prefsManager.removeCurrency(currencyCode)

        // Обновляем список выбранных валют
        refreshCurrencyLists()
    }

    // Check if a currency is selected
    fun isCurrencySelected(currencyCode: String): Boolean {
        return prefsManager.isCurrencySelected(currencyCode)
    }

    // Refresh both currency lists after changes
    private fun refreshCurrencyLists() {
        loadAllCurrencies()
        loadSelectedCurrencies()
    }

    // Set active currency (the one user is inputting)
    fun setActiveCurrency(currencyCode: String) {
        activeCurrencyCode = currencyCode
    }

    // Update amount for active currency and convert to all others
    fun updateCurrencyAmount(amount: Double) {
        viewModelScope.launch {
            try {
                val currentCurrencies = _selectedCurrencies.value ?: return@launch

                // Update amounts for all currencies based on conversion from active currency
                val updatedCurrencies = currentCurrencies.map { currency ->
                    if (currency.code == activeCurrencyCode) {
                        // For active currency, use input amount directly
                        currency.copy(amount = amount)
                    } else {
                        // For other currencies, convert from active currency
                        val convertedAmount = repository.convertCurrency(
                            amount,
                            activeCurrencyCode,
                            currency.code
                        )
                        currency.copy(amount = convertedAmount)
                    }
                }

                _selectedCurrencies.value = updatedCurrencies
            } catch (e: Exception) {
                _error.value = "Error during conversion: ${e.message}"
            }
        }
    }

    // Format currency amount for display
    fun formatAmount(amount: Double): String {
        return decimalFormat.format(amount)
    }
}
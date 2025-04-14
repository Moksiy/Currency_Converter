package com.example.currency_converter.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.currency_converter.data.CurrencyDatabase
import com.example.currency_converter.model.Currency
import com.example.currency_converter.network.RetrofitClient
import com.example.currency_converter.repository.CurrencyRepository
import kotlinx.coroutines.launch
import java.text.DecimalFormat

class CurrencyViewModel(application: Application) : AndroidViewModel(application) {
    // Initialize database, API service, and repository
    private val database = CurrencyDatabase.getDatabase(application)
    private val apiService = RetrofitClient.currencyApiService
    private val repository = CurrencyRepository(database, apiService)

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
        loadSelectedCurrencies()
        loadAllCurrencies()
        fetchLatestRates()
    }

    // Load user's selected currencies
    private fun loadSelectedCurrencies() {
        viewModelScope.launch {
            try {
                val currencies = repository.getSelectedCurrencies()
                _selectedCurrencies.value = currencies

                // Set active currency to first in the list if available
                if (currencies.isNotEmpty()) {
                    activeCurrencyCode = currencies[0].code
                }
            } catch (e: Exception) {
                _error.value = "Failed to load selected currencies: ${e.message}"
            }
        }
    }

    // Load all available currencies
    private fun loadAllCurrencies() {
        _availableCurrencies.value = repository.getAvailableCurrencies()
    }

    // Fetch latest exchange rates
    fun fetchLatestRates() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val needsUpdate = repository.needsRateUpdate()
                if (needsUpdate) {
                    repository.fetchLatestRates()
                        .onSuccess {
                            _isLoading.value = false
                        }
                        .onFailure {
                            _error.value = "Failed to update rates: ${it.message}"
                            _isLoading.value = false
                        }
                } else {
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = "Error checking rate update status: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    // Add a currency to selected list
    fun addCurrency(currencyCode: String) {
        viewModelScope.launch {
            try {
                repository.addCurrencyToSelected(currencyCode)
                loadSelectedCurrencies()
            } catch (e: Exception) {
                _error.value = "Failed to add currency: ${e.message}"
            }
        }
    }

    // Remove a currency from selected list
    fun removeCurrency(currencyCode: String) {
        viewModelScope.launch {
            try {
                repository.removeCurrencyFromSelected(currencyCode)
                loadSelectedCurrencies()
            } catch (e: Exception) {
                _error.value = "Failed to remove currency: ${e.message}"
            }
        }
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
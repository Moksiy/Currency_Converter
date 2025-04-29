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
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

/**
 * ViewModel для управления валютами, конвертацией и пользовательскими настройками.
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

    private val _lastUpdateTime = MutableLiveData<Long>()
    val lastUpdateTime: LiveData<Long> = _lastUpdateTime

    // Business logic state
    private val decimalFormat = (NumberFormat.getNumberInstance(Locale.getDefault()) as DecimalFormat).apply {
        applyPattern("#,##0.00")
    }

    private var activeCurrencyCode: String = DEFAULT_CURRENCY_CODE

    // Coroutine exception handler
    private val errorHandler = CoroutineExceptionHandler { _, exception ->
        Timber.e(exception, "Error in ViewModel coroutine")
        _error.postValue("Error: ${exception.localizedMessage ?: exception.message ?: "Unknown error"}")
        _isLoading.postValue(false)
    }

    init {
        Timber.d("CurrencyViewModel initialized")
        loadAvailableCurrencies()
        loadSelectedCurrencies()
    }

    /**
     * Очищает текущее сообщение об ошибке.
     */
    fun clearError() {
        _error.value = ""
    }

    /**
     * Возвращает код активной валюты.
     * @return Код активной валюты.
     */
    fun getActiveCurrencyCode(): String {
        return activeCurrencyCode
    }

    /**
     * Загружает список всех доступных валют.
     */
    private fun loadAvailableCurrencies() {
        launchWithErrorHandling {
            val allCurrencies = repository.getAvailableCurrencies()
            val selectedCodes = prefsManager.getSelectedCurrencies()

            val updatedCurrencies = allCurrencies.map { currency ->
                currency.copy(isSelected = currency.code in selectedCodes)
            }

            _availableCurrencies.postValue(updatedCurrencies)
            Timber.d("Loaded ${updatedCurrencies.size} available currencies")
        }
    }

    /**
     * Загружает выбранные пользователем валюты из настроек.
     */
    private fun loadSelectedCurrencies() {
        launchWithErrorHandling {
            val selectedCodes = prefsManager.getSelectedCurrencies()

            // Если нет выбранных валют, добавляем базовые
            if (selectedCodes.isEmpty()) {
                addDefaultCurrencies()
                return@launchWithErrorHandling
            }

            val allCurrencies = repository.getAvailableCurrencies()

            val selectedCurrencies = allCurrencies
                .filter { it.code in selectedCodes }
                .map { it.copy(isSelected = true) }

            _selectedCurrencies.postValue(selectedCurrencies)
            Timber.d("Loaded ${selectedCurrencies.size} selected currencies")

            // Устанавливаем первую валюту как активную, если есть
            if (selectedCurrencies.isNotEmpty()) {
                activeCurrencyCode = selectedCurrencies[0].code
                Timber.d("Set active currency to: $activeCurrencyCode")
            }
        }
    }

    /**
     * Добавляет базовые валюты по умолчанию, если у пользователя ничего не выбрано.
     */
    private fun addDefaultCurrencies() {
        val defaultCurrencies = listOf("USD", "EUR", "GBP")
        Timber.d("Adding default currencies: $defaultCurrencies")

        defaultCurrencies.forEach {
            prefsManager.addCurrency(it)
        }
        loadSelectedCurrencies()
    }

    /**
     * Загружает последние обменные курсы с API.
     */
    fun fetchLatestRates() {
        launchWithErrorHandling {
            _isLoading.postValue(true)
            Timber.d("Fetching latest rates")

            try {
                // Загружаем курсы из репозитория (в реальном приложении - из API)
                val result = repository.fetchLatestRates()

                if (result.isSuccess) {
                    Timber.d("Successfully fetched exchange rates")
                    _lastUpdateTime.postValue(System.currentTimeMillis())

                    // Если есть активная валюта с суммой, обновляем конвертацию
                    val currentCurrencies = _selectedCurrencies.value
                    val activeCurrency = currentCurrencies?.find { it.code == activeCurrencyCode }

                    if (activeCurrency != null && activeCurrency.amount > 0) {
                        Timber.d("Updating conversions for active currency: ${activeCurrency.code}, amount: ${activeCurrency.amount}")
                        updateCurrencyAmount(activeCurrency.amount)
                    }
                } else {
                    val exception = result.exceptionOrNull()
                    Timber.e(exception, "Failed to fetch exchange rates")
                    _error.postValue("Failed to update rates: ${exception?.message}")
                }
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    /**
     * Добавляет валюту в список выбранных.
     * @param currencyCode Код валюты для добавления.
     */
    fun addCurrency(currencyCode: String) {
        Timber.d("Adding currency: $currencyCode")
        prefsManager.addCurrency(currencyCode)
        refreshCurrencyLists()
    }

    /**
     * Удаляет валюту из списка выбранных.
     * @param currencyCode Код валюты для удаления.
     */
    fun removeCurrency(currencyCode: String) {
        // Не даем удалить последнюю валюту
        val selectedCodes = prefsManager.getSelectedCurrencies()
        if (selectedCodes.size <= 1) {
            _error.value = "Cannot remove the last currency"
            Timber.w("Attempted to remove last currency: $currencyCode")
            return
        }

        Timber.d("Removing currency: $currencyCode")

        // Если удаляем активную валюту, переключаемся на другую
        if (currencyCode == activeCurrencyCode) {
            val newActiveCurrency = selectedCodes.firstOrNull { it != currencyCode } ?: DEFAULT_CURRENCY_CODE
            Timber.d("Active currency removed. Switching from $activeCurrencyCode to $newActiveCurrency")
            activeCurrencyCode = newActiveCurrency
        }

        prefsManager.removeCurrency(currencyCode)
        refreshCurrencyLists()
    }

    /**
     * Проверяет, выбрана ли валюта пользователем.
     * @param currencyCode Код валюты для проверки.
     * @return true если валюта выбрана, иначе false.
     */
    fun isCurrencySelected(currencyCode: String): Boolean {
        return prefsManager.isCurrencySelected(currencyCode)
    }

    /**
     * Обновляет оба списка валют после изменения настроек.
     */
    private fun refreshCurrencyLists() {
        Timber.d("Refreshing currency lists")
        loadAvailableCurrencies()
        loadSelectedCurrencies()
    }

    /**
     * Устанавливает активную валюту для ввода.
     * @param currencyCode Код валюты для установки активной.
     */
    fun setActiveCurrency(currencyCode: String) {
        activeCurrencyCode = currencyCode
        Timber.d("Active currency set to: $currencyCode")
    }

    /**
     * Обновляет сумму для активной валюты и конвертирует для всех остальных.
     * @param amount Новая сумма для активной валюты.
     */
    fun updateCurrencyAmount(amount: Double) {
        launchWithErrorHandling {
            val currentCurrencies = _selectedCurrencies.value ?: return@launchWithErrorHandling

            Timber.d("Updating currencies. Active: $activeCurrencyCode, Amount: $amount")

            // Создаем новый список для обновления
            val updatedCurrencies = currentCurrencies.map { currency ->
                if (currency.code == activeCurrencyCode) {
                    // Для активной валюты используем введенную сумму напрямую
                    currency.copy(amount = amount)
                } else {
                    // Для других валют конвертируем из активной
                    val convertedAmount = repository.convertCurrency(
                        amount,
                        activeCurrencyCode,
                        currency.code
                    )
                    Timber.d("Converted ${currency.code}: $convertedAmount (from $activeCurrencyCode: $amount)")
                    currency.copy(amount = convertedAmount)
                }
            }

            // Обновляем LiveData с новым списком
            _selectedCurrencies.postValue(updatedCurrencies)
        }
    }

    /**
     * Форматирует сумму для отображения.
     * @param amount Сумма для форматирования.
     * @return Отформатированная строка с суммой.
     */
    fun formatAmount(amount: Double): String {
        return decimalFormat.format(amount)
    }

    /**
     * Вспомогательный метод для запуска корутин с обработкой ошибок.
     */
    private fun launchWithErrorHandling(block: suspend () -> Unit) {
        viewModelScope.launch(errorHandler) {
            try {
                block()
            } catch (e: Exception) {
                Timber.e(e, "Error in ViewModel coroutine")
                _error.postValue("Error: ${e.localizedMessage ?: e.message ?: "Unknown error"}")
            }
        }
    }

    companion object {
        private const val DEFAULT_CURRENCY_CODE = "USD"
    }
}
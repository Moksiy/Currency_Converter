package com.example.currency_converter.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Менеджер для работы с SharedPreferences, сохраняющий/загружающий выбранные валюты
 */
class CurrencyPreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    /**
     * Сохраняет список кодов выбранных валют
     */
    fun saveSelectedCurrencies(currencyCodes: List<String>) {
        val json = gson.toJson(currencyCodes)
        prefs.edit().putString(KEY_SELECTED_CURRENCIES, json).apply()
    }

    /**
     * Загружает список кодов выбранных валют
     */
    fun getSelectedCurrencies(): List<String> {
        val json = prefs.getString(KEY_SELECTED_CURRENCIES, null) ?: return DEFAULT_CURRENCIES

        val type = object : TypeToken<List<String>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            DEFAULT_CURRENCIES
        }
    }

    /**
     * Добавляет валюту в выбранные
     */
    fun addCurrency(currencyCode: String) {
        val currencyCodes = getSelectedCurrencies().toMutableList()
        if (!currencyCodes.contains(currencyCode)) {
            currencyCodes.add(currencyCode)
            saveSelectedCurrencies(currencyCodes)
        }
    }

    /**
     * Удаляет валюту из выбранных
     */
    fun removeCurrency(currencyCode: String) {
        val currencyCodes = getSelectedCurrencies().toMutableList()
        if (currencyCodes.contains(currencyCode)) {
            currencyCodes.remove(currencyCode)
            saveSelectedCurrencies(currencyCodes)
        }
    }

    /**
     * Проверяет, выбрана ли валюта
     */
    fun isCurrencySelected(currencyCode: String): Boolean {
        return getSelectedCurrencies().contains(currencyCode)
    }

    companion object {
        private const val PREFS_NAME = "currency_converter_prefs"
        private const val KEY_SELECTED_CURRENCIES = "selected_currencies"
        private val DEFAULT_CURRENCIES = listOf("USD", "EUR")
    }
}
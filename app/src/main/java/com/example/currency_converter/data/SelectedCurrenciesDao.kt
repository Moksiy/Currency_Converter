package com.example.currency_converter.data

import com.example.currency_converter.model.SelectedCurrencyEntity

// DAO for selected currencies
@androidx.room.Dao
interface SelectedCurrenciesDao {
    @androidx.room.Query("SELECT * FROM selected_currencies ORDER BY position ASC")
    suspend fun getSelectedCurrencies(): List<SelectedCurrencyEntity>

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertSelectedCurrency(currency: SelectedCurrencyEntity)

    @androidx.room.Delete
    suspend fun deleteSelectedCurrency(currency: SelectedCurrencyEntity)

    @androidx.room.Query("DELETE FROM selected_currencies WHERE currencyCode = :code")
    suspend fun deleteByCode(code: String)
}
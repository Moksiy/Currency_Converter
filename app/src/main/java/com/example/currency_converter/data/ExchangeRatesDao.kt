package com.example.currency_converter.data

import com.example.currency_converter.model.ExchangeRateEntity

// DAO for exchange rates
@androidx.room.Dao
interface ExchangeRatesDao {
    @androidx.room.Query("SELECT * FROM exchange_rates")
    suspend fun getAllRates(): List<ExchangeRateEntity>

    @androidx.room.Query("SELECT * FROM exchange_rates WHERE currencyCode = :code")
    suspend fun getRateForCurrency(code: String): ExchangeRateEntity?

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertRate(rate: ExchangeRateEntity)

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertAllRates(rates: List<ExchangeRateEntity>)

    @androidx.room.Query("SELECT MAX(lastUpdated) FROM exchange_rates")
    suspend fun getLastUpdateTime(): Long?
}
package com.example.currency_converter.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.currency_converter.model.ExchangeRateEntity
import com.example.currency_converter.model.SelectedCurrencyEntity

// Room database
@Database(entities = [ExchangeRateEntity::class, SelectedCurrencyEntity::class], version = 1)
abstract class CurrencyDatabase : RoomDatabase() {
    abstract fun exchangeRatesDao(): ExchangeRatesDao
    abstract fun selectedCurrenciesDao(): SelectedCurrenciesDao

    companion object {
        @Volatile
        private var INSTANCE: CurrencyDatabase? = null

        fun getDatabase(context: Context): CurrencyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CurrencyDatabase::class.java,
                    "currency_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
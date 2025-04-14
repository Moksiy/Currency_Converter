package com.example.currency_converter.model
import androidx.room.Entity
import androidx.room.PrimaryKey

// Room entity for storing user's selected currencies
@Entity(tableName = "selected_currencies")
data class SelectedCurrencyEntity(
    @PrimaryKey
    val currencyCode: String,
    val position: Int
)
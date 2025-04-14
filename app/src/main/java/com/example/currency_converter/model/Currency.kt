package com.example.currency_converter.model

// Represents a currency with its details
data class Currency(
    val code: String,          // e.g., USD, EUR
    val name: String,          // e.g., US Dollar
    val symbol: String,        // e.g., $
    val flagResId: Int,        // Resource ID for flag image
    var amount: Double = 0.0,  // The amount in this currency
    var isSelected: Boolean = false  // Whether this currency is in user's list
)
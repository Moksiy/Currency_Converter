package com.example.currency_converter.model

// API response model for exchange rates
data class ExchangeRatesResponse(
    val base: String,
    val date: String,
    val rates: Map<String, Double>
)
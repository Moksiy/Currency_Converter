package com.example.currency_converter.network

import com.example.currency_converter.model.ExchangeRatesResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// Retrofit interface for the currency API
interface CurrencyApiService {
    @GET("latest")
    suspend fun getLatestRates(
        @Query("base") baseCurrency: String = "USD"
    ): ExchangeRatesResponse
}

// For this example, we'll use https://exchangerate.host as our API
// You'll need to replace this with a real API key for production use
object RetrofitClient {
    private const val BASE_URL = "https://api.exchangerate.host/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val currencyApiService: CurrencyApiService = retrofit.create(CurrencyApiService::class.java)
}
package com.example.currency_converter.model

/**
 * Модель валюты для отображения и конвертации.
 *
 * @param code Код валюты (например, "USD", "EUR").
 * @param name Название валюты на английском.
 * @param symbol Символ валюты (например, "$", "€").
 * @param flagResId Ресурс изображения флага.
 * @param amount Текущая сумма в этой валюте.
 * @param isSelected Флаг, выбрана ли валюта пользователем.
 * @param position Позиция валюты в списке выбранных валют.
 */
data class Currency(
    val code: String,
    val name: String,
    val symbol: String,
    val flagResId: Int,
    val amount: Double = 0.0,
    val isSelected: Boolean = false,
    val position: Int = 0
)
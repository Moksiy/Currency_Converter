package com.example.currency_converter.adapter

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.currency_converter.R
import com.example.currency_converter.databinding.ItemCurrencyBinding
import com.example.currency_converter.model.Currency
import java.text.NumberFormat
import java.util.Currency as JavaCurrency
import java.util.Locale
import timber.log.Timber

/**
 * Адаптер для основного списка валют.
 * @param onItemClicked Callback вызываемый при выборе валюты.
 */
class CurrencyAdapter(
    private val onItemClicked: (Currency) -> Unit
) : ListAdapter<Currency, CurrencyAdapter.CurrencyViewHolder>(CurrencyDiffCallback) {

    // Код активной валюты для визуального выделения
    private var activeCurrencyCode: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CurrencyViewHolder {
        val binding = ItemCurrencyBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CurrencyViewHolder(binding, onItemClicked)
    }

    override fun onBindViewHolder(holder: CurrencyViewHolder, position: Int) {
        val currency = getItem(position)
        val isActive = currency.code == activeCurrencyCode
        holder.bind(currency, isActive)
    }

    /**
     * Устанавливает код активной валюты и обновляет UI.
     * @param currencyCode Код активной валюты.
     */
    fun setActiveCurrency(currencyCode: String) {
        val oldActiveCode = activeCurrencyCode
        activeCurrencyCode = currencyCode

        // Обновляем только изменившиеся элементы для оптимизации
        val currentList = currentList
        currentList.forEachIndexed { index, currency ->
            if (currency.code == oldActiveCode || currency.code == currencyCode) {
                notifyItemChanged(index)
            }
        }
    }

    /**
     * ViewHolder для элемента валюты в основном списке.
     */
    class CurrencyViewHolder(
        private val binding: ItemCurrencyBinding,
        private val onItemClicked: (Currency) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentCurrency: Currency? = null

        init {
            binding.root.setOnClickListener {
                currentCurrency?.let { onItemClicked(it) }
            }
        }

        /**
         * Привязывает данные валюты к элементам интерфейса.
         * @param currency Объект валюты для отображения.
         * @param isActive Флаг активности валюты для визуального выделения.
         */
        fun bind(currency: Currency, isActive: Boolean) {
            currentCurrency = currency

            with(binding) {
                // Установка изображения флага
                flagImageView.setImageResource(currency.flagResId)

                // Установка кода и названия валюты
                currencyCodeTextView.text = currency.code
                currencyNameTextView.text = currency.name

                // Форматирование суммы с символом валюты
                val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
                try {
                    formatter.currency = JavaCurrency.getInstance(currency.code)
                } catch (e: IllegalArgumentException) {
                    // Если код валюты не поддерживается, используем символ из нашей модели
                    Timber.e(e, "Unsupported currency code: ${currency.code}")
                }
                val formattedAmount = formatter.format(currency.amount)
                currencyAmountTextView.text = formattedAmount

                // Визуальное выделение активной валюты
                if (isActive) {
                    // Задаем фон для CardView
                    (root.parent as? com.google.android.material.card.MaterialCardView)?.apply {
                        setCardBackgroundColor(ContextCompat.getColor(root.context, R.color.selected_item_bg))
                        setStrokeWidth(2)
                        setStrokeColor(ContextCompat.getColor(root.context, R.color.primary))
                    }
                    currencyCodeTextView.setTypeface(null, Typeface.BOLD)
                    currencyAmountTextView.setTypeface(null, Typeface.BOLD)
                    currencyAmountTextView.setTextColor(ContextCompat.getColor(root.context, R.color.primary))
                } else {
                    // Сбрасываем фон для CardView
                    (root.parent as? com.google.android.material.card.MaterialCardView)?.apply {
                        setCardBackgroundColor(ContextCompat.getColor(root.context, R.color.white))
                        setStrokeWidth(0)
                    }
                    currencyCodeTextView.setTypeface(null, Typeface.NORMAL)
                    currencyAmountTextView.setTypeface(null, Typeface.NORMAL)
                    currencyAmountTextView.setTextColor(ContextCompat.getColor(root.context, R.color.black))
                }
            }
        }
    }

    /**
     * Обновляет конкретную валюту в списке.
     * @param updatedCurrency Обновленный объект валюты.
     */
    fun updateCurrency(updatedCurrency: Currency) {
        val currentItems = currentList.toMutableList()
        val position = currentItems.indexOfFirst { it.code == updatedCurrency.code }

        if (position != -1) {
            currentItems[position] = updatedCurrency
            submitList(currentItems)
        }
    }

    /**
     * DiffUtil для оптимизации обновлений списка.
     */
    object CurrencyDiffCallback : DiffUtil.ItemCallback<Currency>() {
        override fun areItemsTheSame(oldItem: Currency, newItem: Currency): Boolean {
            return oldItem.code == newItem.code
        }

        override fun areContentsTheSame(oldItem: Currency, newItem: Currency): Boolean {
            return oldItem.code == newItem.code &&
                    oldItem.amount == newItem.amount &&
                    oldItem.isSelected == newItem.isSelected
        }
    }
}
package com.example.currency_converter.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.currency_converter.databinding.ItemAddCurrencyBinding
import com.example.currency_converter.model.Currency

/**
 * Адаптер для диалога добавления/удаления валют.
 * @param onCheckChanged Callback вызываемый при изменении состояния чекбокса.
 */
class AddCurrencyAdapter(
    private val onCheckChanged: (Currency, Boolean) -> Unit
) : ListAdapter<Currency, AddCurrencyAdapter.AddCurrencyViewHolder>(AddCurrencyDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddCurrencyViewHolder {
        val binding = ItemAddCurrencyBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AddCurrencyViewHolder(binding, onCheckChanged)
    }

    override fun onBindViewHolder(holder: AddCurrencyViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder для элемента списка добавления валют.
     */
    class AddCurrencyViewHolder(
        private val binding: ItemAddCurrencyBinding,
        private val onCheckChanged: (Currency, Boolean) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentCurrency: Currency? = null

        init {
            // Обработка изменения состояния чекбокса
            binding.checkBox.setOnCheckedChangeListener { _, isChecked ->
                currentCurrency?.let { onCheckChanged(it, isChecked) }
            }

            // Обработка клика по всему элементу для переключения чекбокса
            binding.root.setOnClickListener {
                binding.checkBox.isChecked = !binding.checkBox.isChecked
            }
        }

        /**
         * Привязывает данные валюты к элементам интерфейса.
         */
        fun bind(currency: Currency) {
            currentCurrency = currency

            with(binding) {
                // Загрузка изображения флага
                flagImageView.setImageResource(currency.flagResId)

                // Установка текстовых полей
                currencyCodeTextView.text = currency.code
                currencyNameTextView.text = currency.name

                // Установка состояния чекбокса без вызова слушателя
                checkBox.setOnCheckedChangeListener(null)
                checkBox.isChecked = currency.isSelected
                checkBox.setOnCheckedChangeListener { _, isChecked ->
                    currentCurrency?.let { onCheckChanged(it, isChecked) }
                }
            }
        }
    }

    /**
     * DiffUtil для оптимизации обновлений списка.
     */
    object AddCurrencyDiffCallback : DiffUtil.ItemCallback<Currency>() {
        override fun areItemsTheSame(oldItem: Currency, newItem: Currency): Boolean {
            return oldItem.code == newItem.code
        }

        override fun areContentsTheSame(oldItem: Currency, newItem: Currency): Boolean {
            return oldItem == newItem
        }
    }
}
package com.example.currency_converter.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.currency_converter.databinding.ItemAddCurrencyBinding
import com.example.currency_converter.model.Currency

/**
 * Adapter for the currency selection dialog that handles adding and removing currencies.
 * Implements ListAdapter for efficient list updates using DiffUtil.
 * 
 * @param onCheckChanged Callback triggered when a currency's selection state changes.
 *        First parameter is the selected currency, second parameter is the new selection state.
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
     * ViewHolder class for currency items in the selection list.
     * Handles the UI binding and user interactions for individual currency items.
     */
    class AddCurrencyViewHolder(
        private val binding: ItemAddCurrencyBinding,
        private val onCheckChanged: (Currency, Boolean) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentCurrency: Currency? = null

        init {
            // Set up checkbox state change listener
            binding.checkBox.setOnCheckedChangeListener { _, isChecked ->
                currentCurrency?.let { onCheckChanged(it, isChecked) }
            }

            // Enable whole item click to toggle checkbox
            binding.root.setOnClickListener {
                binding.checkBox.isChecked = !binding.checkBox.isChecked
            }
        }

        /**
         * Binds currency data to the view elements.
         * Updates the UI with currency information and selection state.
         * 
         * @param currency The currency data to display
         */
        fun bind(currency: Currency) {
            currentCurrency = currency

            with(binding) {
                // Set currency flag image
                flagImageView.setImageResource(currency.flagResId)

                // Set currency information text
                currencyCodeTextView.text = currency.code
                currencyNameTextView.text = currency.name

                // Update checkbox state without triggering the listener
                checkBox.setOnCheckedChangeListener(null)
                checkBox.isChecked = currency.isSelected
                checkBox.setOnCheckedChangeListener { _, isChecked ->
                    currentCurrency?.let { onCheckChanged(it, isChecked) }
                }
            }
        }
    }

    /**
     * DiffUtil callback implementation for efficient list updates.
     * Determines how to handle changes in the currency list.
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
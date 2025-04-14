package com.example.currency_converter.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.currency_converter.R
import com.example.currency_converter.model.Currency

// Adapter for the add currency dialog
class AddCurrencyAdapter(
    private val onCheckChanged: (Currency, Boolean) -> Unit
) : ListAdapter<Currency, AddCurrencyAdapter.AddCurrencyViewHolder>(AddCurrencyDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddCurrencyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_add_currency, parent, false)
        return AddCurrencyViewHolder(view, onCheckChanged)
    }

    override fun onBindViewHolder(holder: AddCurrencyViewHolder, position: Int) {
        val currency = getItem(position)
        holder.bind(currency)
    }

    class AddCurrencyViewHolder(
        itemView: View,
        private val onCheckChanged: (Currency, Boolean) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val flagImageView: ImageView = itemView.findViewById(R.id.flagImageView)
        private val codeTextView: TextView = itemView.findViewById(R.id.currencyCodeTextView)
        private val nameTextView: TextView = itemView.findViewById(R.id.currencyNameTextView)
        private val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)

        private var currentCurrency: Currency? = null

        init {
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                currentCurrency?.let { onCheckChanged(it, isChecked) }
            }

            // Make the entire item clickable to toggle checkbox
            itemView.setOnClickListener {
                checkBox.isChecked = !checkBox.isChecked
            }
        }

        fun bind(currency: Currency) {
            currentCurrency = currency

            flagImageView.setImageResource(currency.flagResId)
            codeTextView.text = currency.code
            nameTextView.text = currency.name

            // Set checkbox state without triggering the listener
            checkBox.setOnCheckedChangeListener(null)
            checkBox.isChecked = currency.isSelected
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                currentCurrency?.let { onCheckChanged(it, isChecked) }
            }
        }
    }

    object AddCurrencyDiffCallback : DiffUtil.ItemCallback<Currency>() {
        override fun areItemsTheSame(oldItem: Currency, newItem: Currency): Boolean {
            return oldItem.code == newItem.code
        }

        override fun areContentsTheSame(oldItem: Currency, newItem: Currency): Boolean {
            return oldItem == newItem
        }
    }
}
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

// Adapter for the main currency list
class CurrencyAdapter(
    private val onItemClicked: (Currency) -> Unit
) : ListAdapter<Currency, CurrencyAdapter.CurrencyViewHolder>(CurrencyDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CurrencyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_currency, parent, false)
        return CurrencyViewHolder(view, onItemClicked)
    }

    override fun onBindViewHolder(holder: CurrencyViewHolder, position: Int) {
        val currency = getItem(position)
        holder.bind(currency)
    }

    class CurrencyViewHolder(
        itemView: View,
        private val onItemClicked: (Currency) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val flagImageView: ImageView = itemView.findViewById(R.id.flagImageView)
        private val codeTextView: TextView = itemView.findViewById(R.id.currencyCodeTextView)
        private val nameTextView: TextView = itemView.findViewById(R.id.currencyNameTextView)
        private val amountTextView: TextView = itemView.findViewById(R.id.currencyAmountTextView)

        private var currentCurrency: Currency? = null

        init {
            itemView.setOnClickListener {
                currentCurrency?.let { onItemClicked(it) }
            }
        }

        fun bind(currency: Currency) {
            currentCurrency = currency

            flagImageView.setImageResource(currency.flagResId)
            codeTextView.text = currency.code
            nameTextView.text = currency.name

            // Format amount with the currency symbol
            val formattedAmount = "${currency.symbol} ${String.format("%,.2f", currency.amount)}"
            amountTextView.text = formattedAmount
        }
    }

    object CurrencyDiffCallback : DiffUtil.ItemCallback<Currency>() {
        override fun areItemsTheSame(oldItem: Currency, newItem: Currency): Boolean {
            return oldItem.code == newItem.code
        }

        override fun areContentsTheSame(oldItem: Currency, newItem: Currency): Boolean {
            return oldItem == newItem
        }
    }
}
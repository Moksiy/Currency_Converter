package com.example.currency_converter

import android.app.Dialog
import android.os.Bundle
import android.view.WindowManager
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.currency_converter.adapter.AddCurrencyAdapter
import com.example.currency_converter.adapter.CurrencyAdapter
import com.example.currency_converter.databinding.ActivityMainBinding
import com.example.currency_converter.model.Calculator
import com.example.currency_converter.viewmodel.CurrencyViewModel
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Main activity that displays the currency converter and calculator.
 */
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: CurrencyViewModel by viewModels()
    private lateinit var currencyAdapter: CurrencyAdapter
    private val calculator = Calculator(onResultChanged = { result ->
        viewModel.updateCurrencyAmount(result)
    })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupCurrencyRecyclerView()
        setupCalculatorButtons()
        setupUIComponents()
        observeViewModel()
    }

    private fun setupCurrencyRecyclerView() {
        currencyAdapter = CurrencyAdapter { currency ->
            viewModel.setActiveCurrency(currency.code)
            calculator.setDisplay(currency.amount.toString())
        }

        binding.currencyRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = currencyAdapter
        }
    }

    private fun setupCalculatorButtons() {
        with(binding) {
            // Number buttons
            button0.setOnClickListener { calculator.appendDigit("0") }
            button1.setOnClickListener { calculator.appendDigit("1") }
            button2.setOnClickListener { calculator.appendDigit("2") }
            button3.setOnClickListener { calculator.appendDigit("3") }
            button4.setOnClickListener { calculator.appendDigit("4") }
            button5.setOnClickListener { calculator.appendDigit("5") }
            button6.setOnClickListener { calculator.appendDigit("6") }
            button7.setOnClickListener { calculator.appendDigit("7") }
            button8.setOnClickListener { calculator.appendDigit("8") }
            button9.setOnClickListener { calculator.appendDigit("9") }
            buttonDot.setOnClickListener { calculator.appendDot() }

            // Operation buttons
            buttonPlus.setOnClickListener { calculator.setOperation("+") }
            buttonMinus.setOnClickListener { calculator.setOperation("-") }
            buttonMultiply.setOnClickListener { calculator.setOperation("×") }
            buttonDivide.setOnClickListener { calculator.setOperation("÷") }
            buttonPercent.setOnClickListener { calculator.calculatePercent() }
            buttonEquals.setOnClickListener { calculator.calculateResult() }

            // Clear buttons
            buttonAC.setOnClickListener { calculator.clearAll() }
            buttonClear.setOnClickListener { calculator.clearLastDigit() }
        }
    }

    private fun setupUIComponents() {
        // Edit button to add/remove currencies
        binding.editButton.setOnClickListener {
            showAddCurrencyDialog()
        }

        // Settings button
        binding.settingsButton.setOnClickListener {
            Toast.makeText(this, getString(R.string.settings_coming_soon), Toast.LENGTH_SHORT).show()
        }

        // Update last update text
        val dateFormat = SimpleDateFormat(getString(R.string.date_format), Locale.getDefault())
        binding.lastUpdateTextView.text = getString(R.string.last_update_format, dateFormat.format(Date()))
    }

    private fun observeViewModel() {
        // Observe selected currencies
        viewModel.selectedCurrencies.observe(this) { currencies ->
            currencyAdapter.submitList(currencies)
        }

        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observe errors
        viewModel.error.observe(this) { errorMessage ->
            errorMessage?.takeIf { it.isNotEmpty() }?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    private fun showAddCurrencyDialog() {
        val dialog = createAddCurrencyDialog()
        setupAddCurrencyDialogViews(dialog)
        dialog.show()
    }

    private fun createAddCurrencyDialog(): Dialog {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_add_currency)

        // Set dialog width
        dialog.window?.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        return dialog
    }

    private fun setupAddCurrencyDialogViews(dialog: Dialog) {
        val recyclerView: RecyclerView = dialog.findViewById(R.id.addCurrencyRecyclerView)
        val searchView: SearchView = dialog.findViewById(R.id.searchView)
        val confirmButton: MaterialButton = dialog.findViewById(R.id.buttonConfirm)

        setupAddCurrencyRecyclerView(recyclerView)
        setupAddCurrencySearch(searchView, recyclerView.adapter as AddCurrencyAdapter)

        confirmButton.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun setupAddCurrencyRecyclerView(recyclerView: RecyclerView) {
        val allCurrencies = viewModel.availableCurrencies.value.orEmpty()

        val adapter = AddCurrencyAdapter { currency, isChecked ->
            if (isChecked) viewModel.addCurrency(currency.code)
            else viewModel.removeCurrency(currency.code)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Update selection state according to current selection
        val updatedCurrencies = allCurrencies.map { currency ->
            currency.copy(isSelected = viewModel.isCurrencySelected(currency.code))
        }
        adapter.submitList(updatedCurrencies)
    }

    private fun setupAddCurrencySearch(
        searchView: SearchView,
        adapter: AddCurrencyAdapter
    ) {
        val allCurrencies = viewModel.availableCurrencies.value.orEmpty().map { currency ->
            currency.copy(isSelected = viewModel.isCurrencySelected(currency.code))
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    adapter.submitList(allCurrencies)
                } else {
                    val filteredList = allCurrencies.filter { currency ->
                        currency.code.contains(newText, ignoreCase = true) ||
                                currency.name.contains(newText, ignoreCase = true)
                    }
                    adapter.submitList(filteredList)
                }
                return true
            }
        })
    }
}
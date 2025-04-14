package com.example.currency_converter

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.currency_converter.adapter.AddCurrencyAdapter
import com.example.currency_converter.adapter.CurrencyAdapter
import com.example.currency_converter.databinding.ActivityMainBinding
import com.example.currency_converter.model.Currency
import com.example.currency_converter.viewmodel.CurrencyViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: CurrencyViewModel by viewModels()
    private lateinit var currencyAdapter: CurrencyAdapter

    // Calculator variables
    private var currentInput = StringBuilder()
    private var currentOperation: String? = null
    private var firstOperand: Double = 0.0
    private var secondOperand: Double = 0.0
    private var isOperationClicked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up currency recycler view
        setupCurrencyRecyclerView()

        // Set up calculator buttons
        setupCalculatorButtons()

        // Set up other UI components
        setupUIComponents()

        // Observe LiveData from ViewModel
        observeViewModel()
    }

    private fun setupCurrencyRecyclerView() {
        currencyAdapter = CurrencyAdapter { currency ->
            // When a currency is clicked, set it as active for input
            viewModel.setActiveCurrency(currency.code)

            // Clear calculator display and set current value
            currentInput.clear()
            currentInput.append(currency.amount.toString())
            updateCalculatorDisplay()
        }

        binding.currencyRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = currencyAdapter
        }
    }

    private fun setupCalculatorButtons() {
        // Number buttons
        binding.button0.setOnClickListener { appendDigit("0") }
        binding.button1.setOnClickListener { appendDigit("1") }
        binding.button2.setOnClickListener { appendDigit("2") }
        binding.button3.setOnClickListener { appendDigit("3") }
        binding.button4.setOnClickListener { appendDigit("4") }
        binding.button5.setOnClickListener { appendDigit("5") }
        binding.button6.setOnClickListener { appendDigit("6") }
        binding.button7.setOnClickListener { appendDigit("7") }
        binding.button8.setOnClickListener { appendDigit("8") }
        binding.button9.setOnClickListener { appendDigit("9") }
        binding.buttonDot.setOnClickListener { appendDot() }

        // Operation buttons
        binding.buttonPlus.setOnClickListener { setOperation("+") }
        binding.buttonMinus.setOnClickListener { setOperation("-") }
        binding.buttonMultiply.setOnClickListener { setOperation("×") }
        binding.buttonDivide.setOnClickListener { setOperation("÷") }
        binding.buttonPercent.setOnClickListener { calculatePercent() }
        binding.buttonEquals.setOnClickListener { calculateResult() }

        // Clear buttons
        binding.buttonAC.setOnClickListener { clearAll() }
        binding.buttonClear.setOnClickListener { clearLastDigit() }
    }

    private fun setupUIComponents() {
        // Edit button to add/remove currencies
        binding.editButton.setOnClickListener {
            showAddCurrencyDialog()
        }

        // Settings button
        binding.settingsButton.setOnClickListener {
            Toast.makeText(this, "Settings coming soon!", Toast.LENGTH_SHORT).show()
        }

        // Update last update text
        val dateFormat = SimpleDateFormat("MM/dd/yyyy, h:mm:ss a", Locale.getDefault())
        binding.lastUpdateTextView.text = "Last update: ${dateFormat.format(Date())}"
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
            if (errorMessage.isNotEmpty()) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    // Calculator functions
    private fun appendDigit(digit: String) {
        if (isOperationClicked) {
            currentInput.clear()
            isOperationClicked = false
        }

        currentInput.append(digit)
        updateCalculatorDisplay()
    }

    private fun appendDot() {
        if (isOperationClicked) {
            currentInput.clear()
            currentInput.append("0")
            isOperationClicked = false
        }

        if (!currentInput.contains(".")) {
            if (currentInput.isEmpty()) {
                currentInput.append("0")
            }
            currentInput.append(".")
            updateCalculatorDisplay()
        }
    }

    private fun setOperation(operation: String) {
        if (currentInput.isNotEmpty()) {
            firstOperand = currentInput.toString().toDouble()
            currentOperation = operation
            isOperationClicked = true
        }
    }

    private fun calculatePercent() {
        if (currentInput.isNotEmpty()) {
            val currentValue = currentInput.toString().toDouble()
            val result = currentValue / 100
            currentInput.clear()
            currentInput.append(result.toString())
            updateCalculatorDisplay()
        }
    }

    private fun calculateResult() {
        if (currentInput.isNotEmpty() && currentOperation != null) {
            secondOperand = currentInput.toString().toDouble()

            val result = when (currentOperation) {
                "+" -> firstOperand + secondOperand
                "-" -> firstOperand - secondOperand
                "×" -> firstOperand * secondOperand
                "÷" -> if (secondOperand != 0.0) firstOperand / secondOperand else 0.0
                else -> secondOperand
            }

            currentInput.clear()
            currentInput.append(result.toString())
            updateCalculatorDisplay()

            // Update currency amounts
            viewModel.updateCurrencyAmount(result)

            // Reset operation
            currentOperation = null
        }
    }

    private fun clearAll() {
        currentInput.clear()
        currentOperation = null
        firstOperand = 0.0
        secondOperand = 0.0
        isOperationClicked = false
        updateCalculatorDisplay()

        // Also clear currency amounts
        viewModel.updateCurrencyAmount(0.0)
    }

    private fun clearLastDigit() {
        if (currentInput.isNotEmpty()) {
            currentInput.deleteCharAt(currentInput.length - 1)
            updateCalculatorDisplay()

            // If we still have a number, update currency
            if (currentInput.isNotEmpty()) {
                try {
                    val amount = currentInput.toString().toDouble()
                    viewModel.updateCurrencyAmount(amount)
                } catch (e: Exception) {
                    // If we can't parse it (e.g., just a decimal point), ignore
                }
            } else {
                // If input is now empty, set amount to 0
                viewModel.updateCurrencyAmount(0.0)
            }
        }
    }

    private fun updateCalculatorDisplay() {
        // We don't have a display in our calculator, but we update the amount
        // in the active currency instead
        if (currentInput.isNotEmpty()) {
            try {
                val amount = currentInput.toString().toDouble()
                viewModel.updateCurrencyAmount(amount)
            } catch (e: Exception) {
                // Handle parsing error
            }
        } else {
            viewModel.updateCurrencyAmount(0.0)
        }
    }

    // Add currency dialog
    private fun showAddCurrencyDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_add_currency)

        val recyclerView = dialog.findViewById<RecyclerView>(R.id.addCurrencyRecyclerView)
        val searchView = dialog.findViewById<SearchView>(R.id.searchView)

        // Get all available currencies
        val allCurrencies = viewModel.availableCurrencies.value ?: emptyList()

        // Create adapter for add currency dialog
        val adapter = AddCurrencyAdapter { currency, isChecked ->
            if (isChecked) {
                viewModel.addCurrency(currency.code)
            } else {
                viewModel.removeCurrency(currency.code)
            }
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Set initial data
        adapter.submitList(allCurrencies)

        // Set up search
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

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

        dialog.show()
    }
}
package com.example.currency_converter.model

/**
 * Calculator class that handles calculator operations for currency conversion.
 * @param onResultChanged Callback that will be invoked when the calculator result changes.
 */
class Calculator(private val onResultChanged: (Double) -> Unit) {
    private val currentInput = StringBuilder()
    private var currentOperation: String? = null
    private var firstOperand: Double = 0.0
    private var secondOperand: Double = 0.0
    private var isOperationClicked = false

    /**
     * Sets the display to the specified value.
     * @param value The value to display.
     */
    fun setDisplay(value: String) {
        currentInput.clear()
        currentInput.append(value)
        notifyResultChanged()
    }

    /**
     * Appends a digit to the current input.
     * @param digit The digit to append.
     */
    fun appendDigit(digit: String) {
        if (isOperationClicked) {
            currentInput.clear()
            isOperationClicked = false
        }

        currentInput.append(digit)
        notifyResultChanged()
    }

    /**
     * Appends a decimal point to the current input if not already present.
     */
    fun appendDot() {
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
            notifyResultChanged()
        }
    }

    /**
     * Sets the operation to be performed.
     * @param operation The operation symbol ("+", "-", "×", "÷").
     */
    fun setOperation(operation: String) {
        if (currentInput.isNotEmpty()) {
            firstOperand = currentInput.toString().toDoubleOrNull() ?: 0.0
            currentOperation = operation
            isOperationClicked = true
        }
    }

    /**
     * Calculates the percentage of the current value.
     */
    fun calculatePercent() {
        if (currentInput.isNotEmpty()) {
            val currentValue = currentInput.toString().toDoubleOrNull() ?: 0.0
            val result = currentValue / 100
            currentInput.clear()
            currentInput.append(result.toString())
            notifyResultChanged()
        }
    }

    /**
     * Calculates the result of the operation.
     */
    fun calculateResult() {
        if (currentInput.isNotEmpty() && currentOperation != null) {
            secondOperand = currentInput.toString().toDoubleOrNull() ?: 0.0

            val result = when (currentOperation) {
                "+" -> firstOperand + secondOperand
                "-" -> firstOperand - secondOperand
                "×" -> firstOperand * secondOperand
                "÷" -> if (secondOperand != 0.0) firstOperand / secondOperand else 0.0
                else -> secondOperand
            }

            currentInput.clear()
            currentInput.append(result.toString())
            notifyResultChanged()

            // Reset operation
            currentOperation = null
        }
    }

    /**
     * Clears all input and operations.
     */
    fun clearAll() {
        currentInput.clear()
        currentOperation = null
        firstOperand = 0.0
        secondOperand = 0.0
        isOperationClicked = false
        notifyResultChanged(0.0)
    }

    /**
     * Clears the last digit of the current input.
     */
    fun clearLastDigit() {
        if (currentInput.isNotEmpty()) {
            currentInput.deleteCharAt(currentInput.length - 1)

            if (currentInput.isNotEmpty()) {
                notifyResultChanged()
            } else {
                notifyResultChanged(0.0)
            }
        }
    }

    /**
     * Notifies that the result has changed based on the current input.
     */
    private fun notifyResultChanged() {
        try {
            val amount = currentInput.toString().toDoubleOrNull() ?: 0.0
            onResultChanged(amount)
        } catch (e: Exception) {
            onResultChanged(0.0)
        }
    }

    /**
     * Notifies that the result has changed to a specific value.
     * @param value The new result value.
     */
    private fun notifyResultChanged(value: Double) {
        onResultChanged(value)
    }
}
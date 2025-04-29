package com.example.currency_converter.model

import timber.log.Timber

/**
 * Класс калькулятора для выполнения арифметических операций с валютами.
 * @param onResultChanged Callback вызываемый при изменении результата расчета.
 */
class Calculator(private val onResultChanged: (Double) -> Unit) {
    private val currentInput = StringBuilder()
    private var currentOperation: String? = null
    private var firstOperand: Double = 0.0
    private var secondOperand: Double = 0.0
    private var isOperationClicked = false

    // Максимальное количество десятичных знаков
    private val MAX_DECIMAL_PLACES = 8

    // Максимальное количество цифр до десятичной точки
    private val MAX_INTEGER_DIGITS = 12

    /**
     * Устанавливает значение в дисплей калькулятора.
     * @param value Новое значение.
     */
    fun setDisplay(value: String) {
        Timber.d("Setting calculator display to: $value")
        currentInput.clear()
        currentInput.append(value)
        notifyResultChanged()
    }

    /**
     * Добавляет цифру к текущему вводу.
     * @param digit Добавляемая цифра (0-9).
     */
    fun appendDigit(digit: String) {
        // Если предыдущей операцией был клик по операции, очищаем ввод
        if (isOperationClicked) {
            currentInput.clear()
            isOperationClicked = false
        }

        // Проверка превышения максимального количества цифр
        val hasDot = currentInput.contains(".")
        val integerPartLength = if (hasDot) {
            currentInput.indexOf(".")
        } else {
            currentInput.length
        }

        if (integerPartLength >= MAX_INTEGER_DIGITS && !hasDot) {
            Timber.d("Maximum integer digits reached: $MAX_INTEGER_DIGITS")
            return
        }

        // Если уже есть десятичная точка, проверяем количество знаков после неё
        if (hasDot) {
            val decimalPlaces = currentInput.length - currentInput.indexOf(".") - 1
            if (decimalPlaces >= MAX_DECIMAL_PLACES) {
                Timber.d("Maximum decimal places reached: $MAX_DECIMAL_PLACES")
                return
            }
        }

        // Не позволяем начинать с нескольких нулей
        if (currentInput.isEmpty() && digit == "0") {
            currentInput.append(digit)
        } else if (currentInput.toString() == "0" && digit != ".") {
            // Если текущий ввод только "0", заменяем его на вводимую цифру
            currentInput.clear()
            currentInput.append(digit)
        } else {
            currentInput.append(digit)
        }

        notifyResultChanged()
    }

    /**
     * Добавляет десятичную точку к текущему вводу.
     */
    fun appendDot() {
        if (isOperationClicked) {
            currentInput.clear()
            currentInput.append("0")
            isOperationClicked = false
        }

        // Добавляем точку только если её еще нет
        if (!currentInput.contains(".")) {
            if (currentInput.isEmpty()) {
                currentInput.append("0")
            }
            currentInput.append(".")
            notifyResultChanged()
        }
    }

    /**
     * Устанавливает операцию для выполнения.
     * @param operation Символ операции ("+", "-", "×", "÷").
     */
    fun setOperation(operation: String) {
        if (currentInput.isNotEmpty()) {
            firstOperand = currentInput.toString().toDoubleOrNull() ?: 0.0
            currentOperation = operation
            isOperationClicked = true
            Timber.d("Operation set: $operation, First operand: $firstOperand")
        } else if (operation == "-" && currentInput.isEmpty()) {
            // Позволяем ввести отрицательное число
            currentInput.append("-")
            notifyResultChanged()
        }
    }

    /**
     * Вычисляет процент от текущего значения.
     */
    fun calculatePercent() {
        if (currentInput.isNotEmpty()) {
            val currentValue = currentInput.toString().toDoubleOrNull() ?: 0.0
            val result = currentValue / 100
            currentInput.clear()
            currentInput.append(formatResult(result))
            notifyResultChanged()
            Timber.d("Percent calculated: $currentValue -> $result")
        }
    }

    /**
     * Выполняет расчет в соответствии с текущей операцией.
     */
    fun calculateResult() {
        if (currentInput.isNotEmpty() && currentOperation != null) {
            secondOperand = currentInput.toString().toDoubleOrNull() ?: 0.0
            Timber.d("Calculating result: $firstOperand $currentOperation $secondOperand")

            val result = when (currentOperation) {
                "+" -> firstOperand + secondOperand
                "-" -> firstOperand - secondOperand
                "×" -> firstOperand * secondOperand
                "÷" -> {
                    if (secondOperand != 0.0) {
                        firstOperand / secondOperand
                    } else {
                        Timber.w("Division by zero attempted")
                        0.0
                    }
                }
                else -> secondOperand
            }

            currentInput.clear()
            currentInput.append(formatResult(result))
            notifyResultChanged()

            // Сбрасываем операцию
            currentOperation = null
        }
    }

    /**
     * Форматирует результат для отображения.
     * @param result Результат вычисления.
     * @return Отформатированная строка.
     */
    private fun formatResult(result: Double): String {
        return when {
            result == result.toLong().toDouble() -> result.toLong().toString()
            else -> {
                // Ограничиваем количество десятичных знаков
                val formattedResult = "%.${MAX_DECIMAL_PLACES}f".format(result)
                // Удаляем конечные нули
                formattedResult.replace(Regex("0+$"), "").replace(Regex("\\.$"), "")
            }
        }
    }

    /**
     * Полностью очищает калькулятор.
     */
    fun clearAll() {
        Timber.d("Clearing calculator")
        currentInput.clear()
        currentOperation = null
        firstOperand = 0.0
        secondOperand = 0.0
        isOperationClicked = false
        notifyResultChanged(0.0)
    }

    /**
     * Удаляет последний введенный символ.
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
     * Уведомляет о изменении результата на основе текущего ввода.
     */
    private fun notifyResultChanged() {
        try {
            val amount = currentInput.toString().toDoubleOrNull() ?: 0.0
            onResultChanged(amount)
        } catch (e: Exception) {
            Timber.e(e, "Error parsing calculator input: ${currentInput}")
            onResultChanged(0.0)
        }
    }

    /**
     * Уведомляет о изменении результата до конкретного значения.
     * @param value Новое значение результата.
     */
    private fun notifyResultChanged(value: Double) {
        onResultChanged(value)
    }
}
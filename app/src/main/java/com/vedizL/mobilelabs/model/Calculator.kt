package com.vedizL.mobilelabs.model

import com.vedizL.mobilelabs.utils.Constants
import java.util.Locale

class Calculator {
    // Public state
    var currentInput: String = Constants.INITIAL_DISPLAY_VALUE
        private set
    var isErrorState: Boolean = false
        private set

    // Private state
    private var previousInput: String? = null
    private var currentOperation: String? = null
    private var shouldResetInput: Boolean = false
    private var pendingOperation: String? = null
    private var lastExpression: String? = null
    // Log of the full expression tokens before '=' for logging on equals
    private var expressionLog: String = ""

    // Number input
    fun inputDigit(digit: String): Boolean {
        if (isErrorState) {
            clearError()
            return false
        }

        if (shouldResetInput) {
            currentInput = Constants.INITIAL_DISPLAY_VALUE
            shouldResetInput = false
        }

        if (currentInput.length >= Constants.MAX_INPUT_LENGTH) return false

        currentInput = when {
            currentInput == Constants.INITIAL_DISPLAY_VALUE && digit != "0" -> digit
            currentInput == Constants.INITIAL_DISPLAY_VALUE -> Constants.INITIAL_DISPLAY_VALUE
            else -> currentInput + digit
        }

        return true
    }

    fun inputDecimal(): Boolean {
        if (isErrorState) {
            clearError()
            return false
        }

        if (shouldResetInput) {
            currentInput = Constants.INITIAL_DISPLAY_VALUE
            shouldResetInput = false
        }

        if (!currentInput.contains(".")) {
            currentInput = if (currentInput.isEmpty() || currentInput == Constants.INITIAL_DISPLAY_VALUE) {
                "0."
            } else {
                "$currentInput."
            }
            return true
        }

        return false
    }

    // Operations
    fun performOperation(operation: String): Boolean {
        if (isErrorState) {
            clearError()
            return false
        }

        // Build expression logging tokens
        if (expressionLog.isEmpty()) {
            expressionLog = "$currentInput$operation"
        } else {
            expressionLog += "$currentInput$operation"
        }

        // If there is a pending calculation, evaluate it before chaining
        if (previousInput != null && currentOperation != null) {
            if (!calculateResult()) {
                return false
            }
        }

        if (isErrorState) return false

        previousInput = currentInput
        currentOperation = operation
        shouldResetInput = true
        return true
    }

    fun calculateResult(): Boolean {
        if (previousInput == null || (currentOperation == null && pendingOperation == null)) return false

        // Handle immediate operations (square, sqrt, factorial)
        if (pendingOperation != null) {
            if (!calculatePendingOperation()) {
                return false
            }
            pendingOperation = null
            previousInput = null
            return true
        }

        val prev = previousInput!!.toDoubleOrNull() ?: return false
        val current = currentInput.toDoubleOrNull() ?: return false

        val result = when (currentOperation) {
            Constants.OP_ADD -> {
                val res = prev + current
                rememberExpression(previousInput, currentOperation, currentInput, res)
                if (res.isInfinite() || res.isNaN()) {
                    showError("Overflow")
                    return false
                }
                res
            }
            Constants.OP_SUBTRACT -> {
                val res = prev - current
                rememberExpression(previousInput, currentOperation, currentInput, res)
                if (res.isInfinite() || res.isNaN()) {
                    showError("Overflow")
                    return false
                }
                res
            }
            Constants.OP_MULTIPLY -> {
                val res = prev * current
                rememberExpression(previousInput, currentOperation, currentInput, res)
                if (res.isInfinite() || res.isNaN()) {
                    showError("Overflow")
                    return false
                }
                res
            }
            Constants.OP_DIVIDE -> {
                if (current == 0.0) {
                    showError("Error")
                    return false
                }
                val res = prev / current
                rememberExpression(previousInput, currentOperation, currentInput, res)
                res
            }
            else -> return false
        }

        currentInput = formatNumber(result)
        previousInput = null
        currentOperation = null
        shouldResetInput = false
        // lastExpression is already set inside rememberExpression
        return true
    }

    private fun calculatePendingOperation(): Boolean {
        val value = previousInput!!.toDoubleOrNull() ?: return false
        val opSymbol = pendingOperation

        val result = when (pendingOperation) {
            Constants.OP_POWER -> {
                val exponent = currentInput.toDoubleOrNull() ?: return false
                val res = Math.pow(value, exponent)
                if (res.isInfinite() || res.isNaN()) {
                    showError("Overflow")
                    return false
                }
                res
            }
            Constants.OP_SQUARE -> {
                val res = value * value
                if (res.isInfinite() || res.isNaN()) {
                    showError("Overflow")
                    return false
                }
                res
            }
            Constants.OP_SQRT -> {
                if (value < 0) {
                    showError("Invalid")
                    return false
                }
                Math.sqrt(value)
            }
            Constants.OP_FACTORIAL -> {
                if (value < 0 || value != value.toLong().toDouble()) {
                    showError("Invalid")
                    return false
                }
                val n = value.toLong()
                if (n > 25) {
                    showError("Overflow")
                    return false
                }
                var fact = 1L
                for (i in 2..n) {
                    fact *= i
                }
                fact.toDouble()
            }
            else -> return false
        }

        // remember expression for equals logging
        rememberExpression(previousInput, opSymbol, currentInput, result)
        currentInput = formatNumber(result)
        pendingOperation = null
        previousInput = null
        shouldResetInput = false
        return true
    }

    // Logging helpers for equals and loading from history
    fun getExpressionLog(): String = expressionLog

    fun resetExpressionLog() {
        expressionLog = ""
    }

    fun finalizeExpressionBeforeEquals() {
        // Ensure the last entered value is included in the expression log for logging purposes
        expressionLog += currentInput
    }

    fun setCurrentInput(value: String) {
        currentInput = value
    }

    // store the last expression like "12+5=17" for logging on equals
    private fun rememberExpression(left: String?, op: String?, right: String?, result: Double) {
        val formatted = formatNumber(result)
        val l = left ?: ""
        val r = if (op == Constants.OP_SQRT || op == Constants.OP_FACTORIAL) "" else (right ?: "")
        val symbol = op ?: ""
        lastExpression = if (r.isNotEmpty()) "$l$symbol$r=${formatted}" else "${l}${symbol}=${formatted}"
    }

    fun getLastExpression(): String? = lastExpression

    fun applyPercent(): Boolean {
        if (isErrorState) {
            clearError()
            return false
        }

        val value = currentInput.toDoubleOrNull() ?: return false
        val result = value / 100

        if (result.isInfinite() || result.isNaN()) {
            showError("Overflow")
            return false
        }

        currentInput = formatNumber(result)
        return true
    }

    fun applySquare(): Boolean {
        if (isErrorState) {
            clearError()
            return false
        }

        previousInput = currentInput
        pendingOperation = Constants.OP_SQUARE
        shouldResetInput = true
        return calculatePendingOperation()
    }

    fun applySquareRoot(): Boolean {
        if (isErrorState) {
            clearError()
            return false
        }

        previousInput = currentInput
        pendingOperation = Constants.OP_SQRT
        shouldResetInput = true
        return calculatePendingOperation()
    }

    fun applyPower(): Boolean {
        if (isErrorState) {
            clearError()
            return false
        }

        if (previousInput != null && currentOperation != null && !shouldResetInput) {
            return calculateResult()
        }

        previousInput = currentInput
        pendingOperation = Constants.OP_POWER
        shouldResetInput = true
        return true
    }

    fun applyFactorial(): Boolean {
        if (isErrorState) {
            clearError()
            return false
        }

        previousInput = currentInput
        pendingOperation = Constants.OP_FACTORIAL
        shouldResetInput = true
        return calculatePendingOperation()
    }

    // Special functions
    fun clear() {
        clearError()
        currentInput = Constants.INITIAL_DISPLAY_VALUE
        previousInput = null
        currentOperation = null
        shouldResetInput = false
        pendingOperation = null
    }

    fun negate(): Boolean {
        if (isErrorState) {
            clearError()
            return false
        }

        if (currentInput == Constants.INITIAL_DISPLAY_VALUE || currentInput == "0.") return false

        if (currentInput.length >= Constants.MAX_INPUT_LENGTH) return false

        currentInput = if (currentInput.startsWith("-")) {
            currentInput.substring(1)
        } else {
            "-$currentInput"
        }

        return true
    }

    fun deleteLast(): String? {
        if (isErrorState) {
            clearError()
            return null
        }

        if (currentInput.length > 1) {
            var newInput = currentInput.dropLast(1)

            if (newInput.endsWith(".")) {
                newInput = newInput.dropLast(1)
            }

            if (newInput == "-" || newInput.isEmpty()) {
                newInput = Constants.INITIAL_DISPLAY_VALUE
            }

            val deletedChar = currentInput.last().toString()
            currentInput = newInput
            return deletedChar
        } else {
            val oldInput = currentInput
            currentInput = Constants.INITIAL_DISPLAY_VALUE
            return oldInput
        }
    }

    // Error handling
    private fun showError(message: String = Constants.ERROR_MESSAGE) {
        isErrorState = true
        currentInput = message
    }

    private fun clearError() {
        if (isErrorState) {
            isErrorState = false
            currentInput = Constants.INITIAL_DISPLAY_VALUE
        }
    }

    // Formatting
    private fun formatNumber(number: Double): String {
        // Check for integer
        if (number % 1 == 0.0) {
            return try {
                val longValue = number.toLong()
                if (longValue.toString().length > Constants.MAX_INPUT_LENGTH) {
                    showError("Overflow")
                    Constants.ERROR_MESSAGE
                } else {
                    longValue.toString()
                }
            } catch (e: Exception) {
                showError("Overflow")
                Constants.ERROR_MESSAGE
            }
        }

        // Format decimal number
        var formatted = try {
            String.format(Locale.US, "%.10f", number)
                .trimEnd('0')
                .trimEnd('.')
        } catch (e: Exception) {
            showError("Error")
            return Constants.ERROR_MESSAGE
        }

        // Limit length
        if (formatted.length > Constants.MAX_INPUT_LENGTH) {
            formatted = formatted.substring(0, Constants.MAX_INPUT_LENGTH).trimEnd('.')

            if (formatted.endsWith(".")) {
                formatted = formatted.dropLast(1)
            }
        }

        return formatted
    }

    // State saving/restoring
    fun saveState(): CalculatorState {
        return CalculatorState(
            currentInput = currentInput,
            previousInput = previousInput,
            currentOperation = currentOperation,
            shouldResetInput = shouldResetInput,
            isErrorState = isErrorState,
            pendingOperation = pendingOperation
        )
    }

    fun restoreState(state: CalculatorState) {
        currentInput = state.currentInput
        previousInput = state.previousInput
        currentOperation = state.currentOperation
        shouldResetInput = state.shouldResetInput
        isErrorState = state.isErrorState
        pendingOperation = state.pendingOperation
    }

    data class CalculatorState(
        val currentInput: String,
        val previousInput: String?,
        val currentOperation: String?,
        val shouldResetInput: Boolean,
        val isErrorState: Boolean,
        val pendingOperation: String? = null
    )
}
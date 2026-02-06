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

        // If we already have an operation and input should be reset, just change the operation
        if (previousInput != null && currentOperation != null && shouldResetInput) {
            currentOperation = operation
            return true
        }

        // Calculate previous operation if exists
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
        if (previousInput == null || currentOperation == null) return false

        val prev = previousInput!!.toDoubleOrNull() ?: return false
        val current = currentInput.toDoubleOrNull() ?: return false

        val result = when (currentOperation) {
            Constants.OP_ADD -> prev + current
            Constants.OP_SUBTRACT -> prev - current
            Constants.OP_MULTIPLY -> prev * current
            Constants.OP_DIVIDE -> {
                if (current == 0.0) {
                    showError()
                    return false
                }
                prev / current
            }
            else -> return false
        }

        currentInput = formatNumber(result)
        return true
    }

    fun applyPercent(): Boolean {
        if (isErrorState) {
            clearError()
            return false
        }

        val value = currentInput.toDoubleOrNull() ?: return false
        val result = value / 100
        currentInput = formatNumber(result)
        return true
    }

    // Special functions
    fun clear() {
        clearError()
        currentInput = Constants.INITIAL_DISPLAY_VALUE
        previousInput = null
        currentOperation = null
        shouldResetInput = false
    }

    fun negate(): Boolean {
        if (isErrorState) {
            clearError()
            return false
        }

        if (currentInput == Constants.INITIAL_DISPLAY_VALUE || currentInput == "0.") return false

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
            val longValue = number.toLong()
            return if (longValue.toString().length > Constants.MAX_INPUT_LENGTH) {
                showError("Overflow")
                Constants.ERROR_MESSAGE
            } else {
                longValue.toString()
            }
        }

        // Format decimal number
        var formatted = String.format(Locale.US, "%.10f", number)
            .trimEnd('0')
            .trimEnd('.')

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
            isErrorState = isErrorState
        )
    }

    fun restoreState(state: CalculatorState) {
        currentInput = state.currentInput
        previousInput = state.previousInput
        currentOperation = state.currentOperation
        shouldResetInput = state.shouldResetInput
        isErrorState = state.isErrorState
    }

    data class CalculatorState(
        val currentInput: String,
        val previousInput: String?,
        val currentOperation: String?,
        val shouldResetInput: Boolean,
        val isErrorState: Boolean
    )
}
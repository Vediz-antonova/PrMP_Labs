package com.vedizL.mobilelabs.model

import com.vedizL.mobilelabs.utils.Constants
import java.util.Locale
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.sqrt

class Calculator {
    // Public state
    var currentInput: String = Constants.INITIAL_DISPLAY_VALUE
        private set
    var isErrorState: Boolean = false
        private set

    // Private state
    private var previousInput: String? = null
    private var currentOperation: String? = null
    var shouldResetInput: Boolean = false
        private set
    private var pendingOperation: String? = null
    private var lastExpression: String? = null
    var displayExpression: String = ""
        private set

    fun hasPendingOperation(): Boolean = shouldResetInput

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
            (currentInput == Constants.INITIAL_DISPLAY_VALUE || currentInput.isEmpty()) && digit != "0" -> digit
            currentInput == Constants.INITIAL_DISPLAY_VALUE || currentInput.isEmpty() -> Constants.INITIAL_DISPLAY_VALUE
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

        // Build the expression string
        val opSymbol = getOperationSymbol(operation)
        
        // If user just changed operation (was entering second number), update the expression
        if (shouldResetInput && previousInput != null && currentOperation != null) {
            // User is changing operation - need to include the current number they already entered
            // First add the number they entered for the previous operation
            displayExpression += previousInput
            // Then replace the operation
            displayExpression = displayExpression.dropLast(getOperationSymbol(currentOperation).length)
            displayExpression += opSymbol
        } else {
            // Check if we're continuing from a previous result (expression has "=")
            if (displayExpression.contains("=")) {
                // Start fresh from the result
                displayExpression = currentInput
            } else {
                // Add current number to expression
                displayExpression += currentInput
            }
            // Add the operation symbol
            displayExpression += opSymbol
        }

        previousInput = currentInput
        currentOperation = operation
        shouldResetInput = true
        // Clear currentInput after adding to expression
        currentInput = ""
        return true
    }

    private fun getOperationSymbol(operation: String?): String {
        return when (operation) {
            Constants.OP_ADD -> "+"
            Constants.OP_SUBTRACT -> "-"
            Constants.OP_MULTIPLY -> "×"
            Constants.OP_DIVIDE -> "÷"
            Constants.OP_POWER -> "^"
            Constants.OP_SQUARE -> "²"
            Constants.OP_SQRT -> "√"
            Constants.OP_FACTORIAL -> "!"
            else -> operation ?: ""
        }
    }

    fun calculateResult(): Boolean {
        // Handle immediate operations (square, sqrt, factorial)
        if (pendingOperation != null) {
            if (!calculatePendingOperation()) {
                return false
            }
            pendingOperation = null
            previousInput = null
            return true
        }

        if (previousInput == null && currentInput.isEmpty()) return false

        // Build the full expression
        val inputToUse = if (currentInput.isEmpty()) previousInput ?: "" else currentInput
        val fullExpression = displayExpression + inputToUse
        
        // Evaluate with proper precedence
        val result = evaluateExpression(fullExpression)
        
        if (result == null || result.isNaN() || result.isInfinite()) {
            showError("Error")
            return false
        }

        val formattedResult = formatNumber(result)
        displayExpression = "$fullExpression=$formattedResult"
        currentInput = formattedResult
        previousInput = null
        currentOperation = null
        shouldResetInput = false
        return true
    }

    private fun evaluateExpression(expr: String): Double? {
        if (expr.isEmpty()) return null
        
        try {
            // Parse the expression and evaluate with proper precedence
            val values = mutableListOf<Double>()
            val ops = mutableListOf<String>()
            
            var currentNum = StringBuilder()
            var i = 0
            
            while (i < expr.length) {
                val c = expr[i]
                when {
                    c.isDigit() || c == '.' || (c == '-' && currentNum.isEmpty()) -> {
                        currentNum.append(c)
                    }
                    c == '-' && currentNum.isNotEmpty() && !currentNum.toString().endsWith("E") -> {
                        // This is an operator
                        if (currentNum.isNotEmpty()) {
                            values.add(currentNum.toString().toDouble())
                            currentNum = StringBuilder()
                        }
                        // Check for negative number
                        if (i + 1 < expr.length && expr[i + 1].isDigit()) {
                            currentNum.append(c)
                        } else if (ops.isNotEmpty() || values.isNotEmpty()) {
                            ops.add(c.toString())
                        }
                    }
                    c == '+' || c == '-' && currentNum.toString().isNotEmpty() && !currentNum.toString().endsWith("E") -> {
                        if (currentNum.isNotEmpty()) {
                            values.add(currentNum.toString().toDouble())
                            currentNum = StringBuilder()
                        }
                        ops.add(c.toString())
                    }
                    c == '×' || c == '÷' || c == '^' -> {
                        if (currentNum.isNotEmpty()) {
                            values.add(currentNum.toString().toDouble())
                            currentNum = StringBuilder()
                        }
                        ops.add(c.toString())
                    }
                    c == '²' -> {
                        // Square - apply to last value
                        if (values.isNotEmpty()) {
                            val lastIdx = values.size - 1
                            values[lastIdx] = values[lastIdx] * values[lastIdx]
                        }
                    }
                    c == '√' -> {
                        // Square root - will be handled specially
                    }
                    c == '!' -> {
                        // Factorial - apply to last value
                        if (values.isNotEmpty()) {
                            val v = values.last().toInt()
                            var fact = 1
                            for (j in 2..v) {
                                fact *= j
                            }
                            values[values.size - 1] = fact.toDouble()
                        }
                    }
                }
                i++
            }
            
            // Add last number
            if (currentNum.isNotEmpty()) {
                values.add(currentNum.toString().toDouble())
            }
            
            if (values.isEmpty()) return null
            if (values.size == 1) return values[0]
            
            // First pass: handle *, /, ^
            var idx = 0
            while (idx < ops.size) {
                val op = ops[idx]
                if (op == "×" || op == "÷" || op == "^") {
                    val a = values[idx]
                    val b = values[idx + 1]
                    val res = when (op) {
                        "×" -> a * b
                        "÷" -> if (b == 0.0) return null else a / b
                        "^" -> a.pow(b)
                        else -> b
                    }
                    values[idx] = res
                    values.removeAt(idx + 1)
                    ops.removeAt(idx)
                } else {
                    idx++
                }
            }
            
            // Second pass: handle +, -
            var result = values[0]
            for (j in ops.indices) {
                result = if (ops[j] == "+") {
                    result + values[j + 1]
                } else {
                    result - values[j + 1]
                }
            }
            
            return result
        } catch (e: Exception) {
            return null
        }
    }

    private fun calculatePendingOperation(): Boolean {
        val value = previousInput!!.toDoubleOrNull() ?: return false
        val opSymbol = pendingOperation

        val result = when (pendingOperation) {
            Constants.OP_POWER -> {
                val exponent = currentInput.toDoubleOrNull() ?: return false
                val res = value.pow(exponent)
                if (res.isInfinite() || res.isNaN()) {
                    showError("Overflow")
                    return false
                }
                currentInput = formatNumber(res)
                displayExpression = "$value^$exponent=$currentInput"
                res
            }
            Constants.OP_SQUARE -> {
                val res = value * value
                if (res.isInfinite() || res.isNaN()) {
                    showError("Overflow")
                    return false
                }
                currentInput = formatNumber(res)
                displayExpression = "$value²=$currentInput"
                res
            }
            Constants.OP_SQRT -> {
                if (value < 0) {
                    showError("Invalid")
                    return false
                }
                val res = sqrt(value)
                currentInput = formatNumber(res)
                displayExpression = "√$value=$currentInput"
                res
            }
            Constants.OP_FACTORIAL -> {
                if (value < 0 || value != value.toLong().toDouble()) {
                    showError("Invalid")
                    return false
                }
                val n = value.toInt()
                if (n > 10000) {
                    showError("Overflow")
                    return false
                }

                var fact = java.math.BigInteger.ONE
                for (i in 2..n) {
                    fact = fact.multiply(java.math.BigInteger.valueOf(i.toLong()))
                }
                currentInput = fact.toString()
                displayExpression = "$value!=$currentInput"
                pendingOperation = null
                previousInput = null
                shouldResetInput = true
                return true
            }
            else -> return false
        }

        pendingOperation = null
        previousInput = null
        shouldResetInput = true
        return true
    }

    fun getHistoryExpression(): String {
        return displayExpression
    }

    fun resetExpressionLog() {
        displayExpression = ""
    }

    fun finalizeExpressionBeforeEquals() {
        // Expression is already built in displayExpression
    }

    fun setCurrentInput(value: String) {
        currentInput = value
    }

    private fun rememberExpression(left: String?, op: String?, right: String?, result: Double) {
        val formatted = formatNumber(result)
        val l = left ?: ""
        val r = if (op == Constants.OP_SQRT || op == Constants.OP_FACTORIAL) "" else (right ?: "")
        val symbol = op ?: ""
        lastExpression = if (r.isNotEmpty()) "$l$symbol$r=${formatted}" else "${l}${symbol}=${formatted}"
    }

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

    fun applySin(): Boolean {
        if (isErrorState) {
            clearError()
            return false
        }

        val value = currentInput.toDoubleOrNull() ?: return false
        val radians = Math.toRadians(value)
        val result = sin(radians)

        if (result.isInfinite() || result.isNaN()) {
            showError("Error")
            return false
        }

        val formattedResult = formatNumber(result)
        displayExpression = "sin($currentInput)=$formattedResult"
        currentInput = formattedResult
        shouldResetInput = true
        return true
    }

    fun applyCos(): Boolean {
        if (isErrorState) {
            clearError()
            return false
        }

        val value = currentInput.toDoubleOrNull() ?: return false
        val radians = Math.toRadians(value)
        val result = cos(radians)

        if (result.isInfinite() || result.isNaN()) {
            showError("Error")
            return false
        }

        val formattedResult = formatNumber(result)
        displayExpression = "cos($currentInput)=$formattedResult"
        currentInput = formattedResult
        shouldResetInput = true
        return true
    }

    // Special functions
    fun clear() {
        clearError()
        currentInput = Constants.INITIAL_DISPLAY_VALUE
        previousInput = null
        currentOperation = null
        shouldResetInput = false
        pendingOperation = null
        displayExpression = ""
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

        // If we have characters in currentInput, delete from there
        if (currentInput.isNotEmpty()) {
            // If currentInput is a single negative number like "-5", delete the whole thing
            if (currentInput.startsWith("-") && currentInput.length == 2) {
                val oldInput = currentInput
                currentInput = Constants.INITIAL_DISPLAY_VALUE
                return oldInput
            }
            
            if (currentInput.length > 1) {
                var newInput = currentInput.dropLast(1)

                if (newInput.endsWith(".")) {
                    newInput = newInput.dropLast(1)
                }

                if (newInput == "-") {
                    return "-"
                }

                if (newInput.isEmpty()) {
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

        // currentInput is empty - user deleted the entire second number
        // Delete operation from displayExpression and go back to previousInput
        if (displayExpression.isNotEmpty()) {
            // Check if we're after equals
            if (displayExpression.contains("=")) {
                // After equals - use the result as current input
                currentInput = previousInput ?: Constants.INITIAL_DISPLAY_VALUE
                displayExpression = ""
                previousInput = null
                currentOperation = null
                shouldResetInput = false
                // Now delete from currentInput
                if (currentInput.length > 1) {
                    val newInput = currentInput.dropLast(1)
                    currentInput = newInput
                    return newInput.last().toString()
                } else {
                    val oldInput = currentInput
                    currentInput = Constants.INITIAL_DISPLAY_VALUE
                    return oldInput
                }
            }

            // Delete operation from displayExpression
            val deleted = displayExpression.last().toString()
            displayExpression = displayExpression.dropLast(1)
            
            // Go back to previousInput
            currentInput = previousInput ?: Constants.INITIAL_DISPLAY_VALUE
            previousInput = null
            currentOperation = null
            shouldResetInput = false
            return deleted
        }

        return null
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
        val formatted = try {
            String.format(Locale.US, "%.10f", number)
                .trimEnd('0')
                .trimEnd('.')
        } catch (e: Exception) {
            showError("Error")
            return Constants.ERROR_MESSAGE
        }

        if (formatted.length > Constants.MAX_INPUT_LENGTH) {
            showError("Overflow")
            return Constants.ERROR_MESSAGE
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
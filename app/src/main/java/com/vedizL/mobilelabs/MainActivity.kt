package com.vedizL.mobilelabs

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var tvDisplay: TextView
    private var currentInput = "0"
    private var previousInput: String? = null
    private var currentOperation: String? = null
    private var shouldResetInput = false
    private var isErrorState = false

    companion object {
        private const val MAX_INPUT_LENGTH = 15
        private const val ERROR_MESSAGE = "Error"
        private const val ERROR_DISPLAY_TIME = 1500L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvDisplay = findViewById(R.id.tvDisplay)

        setupButtonListeners()
    }

    private fun setupButtonListeners() {
        // Set up number buttons
        findViewById<Button>(R.id.btn0).setOnClickListener { onNumberClick("0") }
        findViewById<Button>(R.id.btn1).setOnClickListener { onNumberClick("1") }
        findViewById<Button>(R.id.btn2).setOnClickListener { onNumberClick("2") }
        findViewById<Button>(R.id.btn3).setOnClickListener { onNumberClick("3") }
        findViewById<Button>(R.id.btn4).setOnClickListener { onNumberClick("4") }
        findViewById<Button>(R.id.btn5).setOnClickListener { onNumberClick("5") }
        findViewById<Button>(R.id.btn6).setOnClickListener { onNumberClick("6") }
        findViewById<Button>(R.id.btn7).setOnClickListener { onNumberClick("7") }
        findViewById<Button>(R.id.btn8).setOnClickListener { onNumberClick("8") }
        findViewById<Button>(R.id.btn9).setOnClickListener { onNumberClick("9") }
        findViewById<Button>(R.id.btnDecimal).setOnClickListener { onDecimalClick() }

        // Operation buttons
        findViewById<Button>(R.id.btnAdd).setOnClickListener { onOperationClick("+") }
        findViewById<Button>(R.id.btnSubtract).setOnClickListener { onOperationClick("-") }
        findViewById<Button>(R.id.btnMultiply).setOnClickListener { onOperationClick("×") }
        findViewById<Button>(R.id.btnDivide).setOnClickListener { onOperationClick("÷") }
        findViewById<Button>(R.id.btnPercent).setOnClickListener { onPercentClick() }

        // Special buttons
        findViewById<Button>(R.id.btnClear).setOnClickListener { onClearClick() }
        findViewById<Button>(R.id.btnPlusMinus).setOnClickListener { onPlusMinusClick() }
        findViewById<Button>(R.id.btnEquals).setOnClickListener { onEqualsClick() }
    }

    private fun onNumberClick(number: String) {
        if (isErrorState) {
            clearError()
            return
        }

        if (shouldResetInput) {
            currentInput = "0"
            shouldResetInput = false
        }

        if (currentInput.length >= MAX_INPUT_LENGTH) return
        currentInput = if (currentInput == "0" && number != "0") {
            number
        } else if (currentInput == "0") {
            "0"
        } else {
            currentInput + number
        }

        updateDisplay()
    }

    private fun onDecimalClick() {
        if (isErrorState) {
            clearError()
            return
        }

        if (shouldResetInput) {
            currentInput = "0"
            shouldResetInput = false
        }

        if (!currentInput.contains(".")) {
            if (currentInput.isEmpty() || currentInput == "0") {
                currentInput = "0."
            } else {
                currentInput += "."
            }
        }

        updateDisplay()
    }

    private fun onOperationClick(operation: String) {
        if (isErrorState) {
            clearError()
            return
        }

        if (previousInput != null && currentOperation != null && shouldResetInput) {
            currentOperation = operation
            updateDisplay()
            return
        }

        if (previousInput != null && currentOperation != null && !shouldResetInput) {
            calculateResult()
        }

        if (isErrorState) return

        previousInput = currentInput
        currentOperation = operation
        shouldResetInput = true
        updateDisplay()
    }

    private fun onPercentClick() {
        if (isErrorState) {
            clearError()
            return
        }

        val value = currentInput.toDoubleOrNull() ?: return
        val result = value / 100

        currentInput = formatNumber(result)
        updateDisplay()
    }

    private fun onClearClick() {
        clearError()
        currentInput = "0"
        previousInput = null
        currentOperation = null
        shouldResetInput = false
        updateDisplay()
    }

    private fun onPlusMinusClick() {
        if (isErrorState) {
            clearError()
            return
        }

        if (currentInput == "0" || currentInput == "0.") return

        currentInput = if (currentInput.startsWith("-")) {
            currentInput.substring(1)
        } else {
            "-$currentInput"
        }

        updateDisplay()
    }

    private fun onEqualsClick() {
        if (isErrorState) {
            clearError()
            return
        }

        if (previousInput == null || currentOperation == null) return

        calculateResult()

        if (!isErrorState) {
            previousInput = null
            currentOperation = null
            shouldResetInput = true
            updateDisplay()
        }
    }

    private fun calculateResult() {
        val prev = previousInput?.toDoubleOrNull() ?: return
        val current = currentInput.toDoubleOrNull() ?: return

        val result = when (currentOperation) {
            "+" -> prev + current
            "-" -> prev - current
            "×" -> prev * current
            "÷" -> {
                if (current == 0.0) {
                    showError()
                    return
                }
                prev / current
            }
            else -> return
        }

        currentInput = formatNumber(result)
    }

    private fun formatNumber(number: Double): String {
        if (number % 1 == 0.0) {
            val longValue = number.toLong()
            return if (longValue.toString().length > MAX_INPUT_LENGTH) {
                showError("Overflow")
                ERROR_MESSAGE
            } else {
                longValue.toString()
            }
        }

        var formatted = String.format(Locale.US, "%.10f", number)
            .trimEnd('0')
            .trimEnd('.')

        if (formatted.length > MAX_INPUT_LENGTH) {
            formatted = formatted.substring(0, MAX_INPUT_LENGTH).trimEnd('.')

            if (formatted.endsWith(".")) {
                formatted = formatted.dropLast(1)
            }
        }

        return formatted
    }

    private fun showError(message: String = ERROR_MESSAGE) {
        isErrorState = true
        currentInput = message
        updateDisplay()

        Handler(Looper.getMainLooper()).postDelayed({
            onClearClick()
        }, ERROR_DISPLAY_TIME)
    }

    private fun clearError() {
        if (isErrorState) {
            isErrorState = false
            currentInput = "0"
        }
    }

    private fun updateDisplay() {
        tvDisplay.text = currentInput

        if (isErrorState) {
            tvDisplay.setTextColor(getColor(android.R.color.holo_red_dark))
        } else {
            tvDisplay.setTextColor(getColor(R.color.display_text))
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("currentInput", currentInput)
        outState.putString("previousInput", previousInput)
        outState.putString("currentOperation", currentOperation)
        outState.putBoolean("shouldResetInput", shouldResetInput)
        outState.putBoolean("isErrorState", isErrorState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        currentInput = savedInstanceState.getString("currentInput") ?: "0"
        previousInput = savedInstanceState.getString("previousInput")
        currentOperation = savedInstanceState.getString("currentOperation")
        shouldResetInput = savedInstanceState.getBoolean("shouldResetInput", false)
        isErrorState = savedInstanceState.getBoolean("isErrorState", false)
        updateDisplay()
    }
}
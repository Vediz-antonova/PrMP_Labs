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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvDisplay = findViewById(R.id.tvDisplay)

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
        if (shouldResetInput) {
            currentInput = "0"
            shouldResetInput = false
        }

        currentInput = if (currentInput == "0") {
            number
        } else {
            currentInput + number
        }

        updateDisplay()
    }

    private fun onDecimalClick() {
        if (shouldResetInput) {
            currentInput = "0"
            shouldResetInput = false
        }

        if (!currentInput.contains(".")) {
            currentInput += "."
        }

        updateDisplay()
    }

    private fun onOperationClick(operation: String) {
        if (previousInput != null && currentOperation != null && !shouldResetInput) {
            calculateResult()
        }

        previousInput = currentInput
        currentOperation = operation
        shouldResetInput = true
        updateDisplay()
    }

    private fun onPercentClick() {
        val value = currentInput.toDoubleOrNull() ?: return
        currentInput = (value / 100).toString()
        formatDisplay()
        updateDisplay()
    }

    private fun onClearClick() {
        currentInput = "0"
        previousInput = null
        currentOperation = null
        shouldResetInput = false
        updateDisplay()
    }

    private fun onPlusMinusClick() {
        if (currentInput == "0") return

        currentInput = if (currentInput.startsWith("-")) {
            currentInput.substring(1)
        } else {
            "-$currentInput"
        }

        updateDisplay()
    }

    private fun onEqualsClick() {
        if (previousInput == null || currentOperation == null) return

        calculateResult()
        previousInput = null
        currentOperation = null
        shouldResetInput = true
        updateDisplay()
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
                    currentInput = "Error"
                    updateDisplay()
                    // Reset after error
                    Handler(Looper.getMainLooper()).postDelayed({
                        onClearClick()
                    }, 1500)
                    return
                }
                prev / current
            }
            else -> return
        }

        currentInput = result.toString()
        formatDisplay()
    }

    private fun formatDisplay() {
        val value = currentInput.toDoubleOrNull() ?: return

        currentInput = if (value % 1 == 0.0) {
            value.toLong().toString()
        } else {
            // Limit to 10 decimal places to avoid overflow
            String.format(Locale.US, "%.10f", value).trimEnd('0').trimEnd('.')
        }
    }

    private fun updateDisplay() {
        tvDisplay.text = currentInput
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("currentInput", currentInput)
        outState.putString("previousInput", previousInput)
        outState.putString("currentOperation", currentOperation)
        outState.putBoolean("shouldResetInput", shouldResetInput)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        currentInput = savedInstanceState.getString("currentInput") ?: "0"
        previousInput = savedInstanceState.getString("previousInput")
        currentOperation = savedInstanceState.getString("currentOperation")
        shouldResetInput = savedInstanceState.getBoolean("shouldResetInput", false)
        updateDisplay()
    }
}
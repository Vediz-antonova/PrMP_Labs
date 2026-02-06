package com.vedizL.mobilelabs

import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.vedizL.mobilelabs.model.Calculator
import com.vedizL.mobilelabs.utils.Constants
import com.vedizL.mobilelabs.utils.GestureController

class MainActivity : AppCompatActivity() {
    private lateinit var tvDisplay: TextView
    private lateinit var calculator: Calculator
    private lateinit var gestureController: GestureController
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)

        // Show tutorial on first launch
        if (sharedPreferences.getBoolean(Constants.KEY_FIRST_LAUNCH, true)) {
            showGestureTutorial()
            sharedPreferences.edit().putBoolean(Constants.KEY_FIRST_LAUNCH, false).apply()
        }

        // Initialize views and components
        tvDisplay = findViewById(R.id.tvDisplay)
        calculator = Calculator()

        // Setup gesture controller
        gestureController = GestureController(
            context = this,
            calculator = calculator,
            displayView = tvDisplay,
            onDisplayUpdate = { updateDisplay() },
            onShowToast = { message -> showToast(message) }
        )

        // Setup button listeners
        setupButtonListeners()

        // Restore state if available
        if (savedInstanceState != null) {
            restoreCalculatorState(savedInstanceState)
        }

        // Initial display update
        updateDisplay()
    }

    private fun showGestureTutorial() {
        val message = """
            Welcome to Advanced Calculator!
            
            New Gesture Controls:
            
            • Swipe ← on display
              Delete last digit
            
            • Swipe → on display  
              Clear all input
            
            • Long press on display
              Quick clear
            
            • Double tap on display
              Copy result to clipboard
            
            Try it out!
        """.trimIndent()

        android.app.AlertDialog.Builder(this)
            .setTitle("Gesture Tutorial")
            .setMessage(message)
            .setPositiveButton("Got it!") { dialog, _ -> dialog.dismiss() }
            .setCancelable(false)
            .show()
    }

    private fun setupButtonListeners() {
        // Number buttons
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

        // Operation buttons
        findViewById<Button>(R.id.btnDecimal).setOnClickListener { onDecimalClick() }
        findViewById<Button>(R.id.btnAdd).setOnClickListener { onOperationClick(Constants.OP_ADD) }
        findViewById<Button>(R.id.btnSubtract).setOnClickListener { onOperationClick(Constants.OP_SUBTRACT) }
        findViewById<Button>(R.id.btnMultiply).setOnClickListener { onOperationClick(Constants.OP_MULTIPLY) }
        findViewById<Button>(R.id.btnDivide).setOnClickListener { onOperationClick(Constants.OP_DIVIDE) }
        findViewById<Button>(R.id.btnPercent).setOnClickListener { onPercentClick() }

        // Special buttons
        findViewById<Button>(R.id.btnClear).setOnClickListener { onClearClick() }
        findViewById<Button>(R.id.btnPlusMinus).setOnClickListener { onPlusMinusClick() }
        findViewById<Button>(R.id.btnEquals).setOnClickListener { onEqualsClick() }
    }

    private fun onNumberClick(number: String) {
        if (calculator.inputDigit(number)) {
            updateDisplay()
        } else if (calculator.isErrorState) {
            // Handle error state with delay
            Handler(Looper.getMainLooper()).postDelayed({
                calculator.clear()
                updateDisplay()
            }, Constants.ERROR_DISPLAY_TIME)
        }
    }

    private fun onDecimalClick() {
        if (calculator.inputDecimal()) {
            updateDisplay()
        }
    }

    private fun onOperationClick(operation: String) {
        if (calculator.performOperation(operation)) {
            updateDisplay()
        } else if (calculator.isErrorState) {
            showErrorState()
        }
    }

    private fun onPercentClick() {
        if (calculator.applyPercent()) {
            updateDisplay()
        }
    }

    private fun onClearClick() {
        calculator.clear()
        updateDisplay()
    }

    private fun onPlusMinusClick() {
        if (calculator.negate()) {
            updateDisplay()
        }
    }

    private fun onEqualsClick() {
        if (calculator.calculateResult()) {
            updateDisplay()
        } else if (calculator.isErrorState) {
            showErrorState()
        }
    }

    private fun updateDisplay() {
        tvDisplay.text = calculator.currentInput

        // Update text color based on error state
        if (calculator.isErrorState) {
            tvDisplay.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        } else {
            tvDisplay.setTextColor(ContextCompat.getColor(this, R.color.display_text))
        }
    }

    private fun showErrorState() {
        updateDisplay()

        // Clear error after delay
        Handler(Looper.getMainLooper()).postDelayed({
            calculator.clear()
            updateDisplay()
        }, Constants.ERROR_DISPLAY_TIME)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // State saving/restoring
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val state = calculator.saveState()
        outState.putString("currentInput", state.currentInput)
        outState.putString("previousInput", state.previousInput)
        outState.putString("currentOperation", state.currentOperation)
        outState.putBoolean("shouldResetInput", state.shouldResetInput)
        outState.putBoolean("isErrorState", state.isErrorState)
    }

    private fun restoreCalculatorState(savedInstanceState: Bundle) {
        val state = Calculator.CalculatorState(
            currentInput = savedInstanceState.getString("currentInput") ?: Constants.INITIAL_DISPLAY_VALUE,
            previousInput = savedInstanceState.getString("previousInput"),
            currentOperation = savedInstanceState.getString("currentOperation"),
            shouldResetInput = savedInstanceState.getBoolean("shouldResetInput", false),
            isErrorState = savedInstanceState.getBoolean("isErrorState", false)
        )
        calculator.restoreState(state)
    }
}
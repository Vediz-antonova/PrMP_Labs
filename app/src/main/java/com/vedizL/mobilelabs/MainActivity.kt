package com.vedizL.mobilelabs

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar
import com.vedizL.mobilelabs.data.preferences.ThemePreferences
import com.vedizL.mobilelabs.model.Calculator
import com.vedizL.mobilelabs.ui.settings.SettingsActivity
import com.vedizL.mobilelabs.ui.theme.ThemeManager
import com.vedizL.mobilelabs.utils.Constants
import com.vedizL.mobilelabs.utils.GestureController

class MainActivity : AppCompatActivity() {
    private lateinit var tvDisplay: TextView
    private lateinit var calculator: Calculator
    private lateinit var gestureController: GestureController
    private lateinit var prefs: ThemePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Preferences + Theme
        prefs = ThemePreferences(this)
        ThemeManager.applyCustomColors(this)

        // Toolbar
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Tutorial
        if (prefs.getBoolean(Constants.KEY_FIRST_LAUNCH, true)) {
            showGestureTutorial()
            prefs.setBoolean(Constants.KEY_FIRST_LAUNCH, false)
        }

        // Calculator UI
        tvDisplay = findViewById(R.id.tvDisplay)
        calculator = Calculator()

        gestureController = GestureController(
            context = this,
            calculator = calculator,
            displayView = tvDisplay,
            onDisplayUpdate = { updateDisplay() },
            onShowToast = { message -> showToast(message) }
        )

        setupButtonListeners()

        if (savedInstanceState != null) {
            restoreCalculatorState(savedInstanceState)
        }

        updateDisplay()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {

            R.id.action_theme -> {
                toggleTheme()
                true
            }

            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        ThemeManager.applyCustomColors(this)
    }

    private fun toggleTheme() {
        val current = prefs.getThemeMode()
        val newMode = if (current == "light") "dark" else "light"

        prefs.saveThemeMode(newMode)
        prefs.setCustomThemeEnabled(false)

        ThemeManager.applyDefaultTheme(newMode)
        recreate()
    }

    private fun showGestureTutorial() {
        val message = """
            Welcome to Advanced Calculator!

            New Gesture Controls:

            • Swipe ← on display — delete last digit
            • Swipe → on display — clear all input
            • Long press — quick clear
            • Double tap — copy result

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
        findViewById<Button>(R.id.btnAdd).setOnClickListener { onOperationClick(Constants.OP_ADD) }
        findViewById<Button>(R.id.btnSubtract).setOnClickListener { onOperationClick(Constants.OP_SUBTRACT) }
        findViewById<Button>(R.id.btnMultiply).setOnClickListener { onOperationClick(Constants.OP_MULTIPLY) }
        findViewById<Button>(R.id.btnDivide).setOnClickListener { onOperationClick(Constants.OP_DIVIDE) }
        findViewById<Button>(R.id.btnPercent).setOnClickListener { onPercentClick() }

        findViewById<Button>(R.id.btnClear).setOnClickListener { onClearClick() }
        findViewById<Button>(R.id.btnPlusMinus).setOnClickListener { onPlusMinusClick() }
        findViewById<Button>(R.id.btnEquals).setOnClickListener { onEqualsClick() }
    }

    private fun onNumberClick(number: String) {
        if (calculator.inputDigit(number)) updateDisplay()
        else if (calculator.isErrorState) {
            Handler(Looper.getMainLooper()).postDelayed({
                calculator.clear()
                updateDisplay()
            }, Constants.ERROR_DISPLAY_TIME)
        }
    }

    private fun onDecimalClick() {
        if (calculator.inputDecimal()) updateDisplay()
    }

    private fun onOperationClick(operation: String) {
        if (calculator.performOperation(operation)) updateDisplay()
        else if (calculator.isErrorState) showErrorState()
    }

    private fun onPercentClick() {
        if (calculator.applyPercent()) updateDisplay()
    }

    private fun onClearClick() {
        calculator.clear()
        updateDisplay()
    }

    private fun onPlusMinusClick() {
        if (calculator.negate()) updateDisplay()
    }

    private fun onEqualsClick() {
        if (calculator.calculateResult()) updateDisplay()
        else if (calculator.isErrorState) showErrorState()
    }

    private fun updateDisplay() {
        tvDisplay.text = calculator.currentInput
        tvDisplay.setTextColor(
            if (calculator.isErrorState)
                ContextCompat.getColor(this, android.R.color.holo_red_dark)
            else
                ContextCompat.getColor(this, R.color.display_text)
        )
    }

    private fun showErrorState() {
        updateDisplay()
        Handler(Looper.getMainLooper()).postDelayed({
            calculator.clear()
            updateDisplay()
        }, Constants.ERROR_DISPLAY_TIME)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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
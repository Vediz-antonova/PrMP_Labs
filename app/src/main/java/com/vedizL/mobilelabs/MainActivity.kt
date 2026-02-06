package com.vedizL.mobilelabs

import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var tvDisplay: TextView
    private var currentInput = "0"
    private var previousInput: String? = null
    private var currentOperation: String? = null
    private var shouldResetInput = false
    private var isErrorState = false

    private lateinit var gestureDetector: GestureDetector
    private lateinit var sharedPreferences: SharedPreferences
    private val PREFS_NAME = "calculator_prefs"
    private val KEY_FIRST_LAUNCH = "first_launch"

    companion object {
        private const val MAX_INPUT_LENGTH = 15
        private const val ERROR_MESSAGE = "Error"
        private const val ERROR_DISPLAY_TIME = 1500L
        private const val SWIPE_THRESHOLD = 100
        private const val SWIPE_VELOCITY_THRESHOLD = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        if (sharedPreferences.getBoolean(KEY_FIRST_LAUNCH, true)) {
            showGestureTutorial()
            sharedPreferences.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
        }

        tvDisplay = findViewById(R.id.tvDisplay)
        setupGestureDetector()

        setupButtonListeners()
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
            .setPositiveButton("Got it!") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun setupGestureDetector() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                try {
                    val diffY = e2.y - (e1?.y ?: 0f)
                    val diffX = e2.x - (e1?.x ?: 0f)

                    if (Math.abs(diffX) > Math.abs(diffY)) {
                        if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                            if (diffX > 0) {
                                // Swipe right - clear all
                                onSwipeRight()
                            } else {
                                // Swipe left - delete last character
                                onSwipeLeft()
                            }
                            return true
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return false
            }

            override fun onDown(e: MotionEvent): Boolean {
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                // Long press on display - clear all
                onClearClick()
                showToast("Display cleared")
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                copyToClipboard()
                return true
            }
        })

        // Set touch listener for display
        tvDisplay.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun copyToClipboard() {
        if (isErrorState) return

        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Calculator result", currentInput)
        clipboard.setPrimaryClip(clip)

        tvDisplay.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_blue_light))

        Handler(Looper.getMainLooper()).postDelayed({
            tvDisplay.setBackgroundResource(R.drawable.display_background)
            showToast("Result copied to clipboard")
        }, 200)
    }

    private fun onSwipeLeft() {
        if (isErrorState) {
            clearError()
            return
        }

        val swipeAnim = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.swipe_left)
        tvDisplay.startAnimation(swipeAnim)

        Handler(Looper.getMainLooper()).postDelayed({
            if (currentInput.length > 1) {
                currentInput = currentInput.dropLast(1)

                if (currentInput.endsWith(".")) {
                    currentInput = currentInput.dropLast(1)
                }

                if (currentInput == "-" || currentInput.isEmpty()) {
                    currentInput = "0"
                }

                showToast(getString(R.string.toast_delete_digit))
            } else {
                currentInput = "0"
                showToast(getString(R.string.toast_clear_display))
            }

            updateDisplay()

            val returnAnim = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.return_to_normal)
            tvDisplay.startAnimation(returnAnim)
        }, 200)
    }

    private fun onSwipeRight() {
        if (isErrorState) {
            clearError()
            return
        }

        val swipeAnim = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.swipe_right)
        tvDisplay.startAnimation(swipeAnim)

        Handler(Looper.getMainLooper()).postDelayed({
            onClearClick()
            showToast(getString(R.string.toast_clear_display))

            val returnAnim = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.return_to_normal)
            tvDisplay.startAnimation(returnAnim)
        }, 200)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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

        if (previousInput != null && currentOperation != null) {
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
            tvDisplay.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        } else {
            tvDisplay.setTextColor(ContextCompat.getColor(this, R.color.display_text))
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
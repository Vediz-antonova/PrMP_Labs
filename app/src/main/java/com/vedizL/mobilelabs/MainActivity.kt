package com.vedizL.mobilelabs

import android.content.Intent
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.vedizL.mobilelabs.data.preferences.ThemeKeys
import com.vedizL.mobilelabs.data.preferences.ThemePreferences
import com.vedizL.mobilelabs.data.theme.ThemeRepository
import com.vedizL.mobilelabs.model.Calculator
import com.vedizL.mobilelabs.ui.settings.SettingsActivity
import com.vedizL.mobilelabs.data.history.ActionEvent
import com.vedizL.mobilelabs.data.history.ActionHistoryStore
import com.vedizL.mobilelabs.ui.theme.ThemeManager
import com.vedizL.mobilelabs.utils.Constants
import com.vedizL.mobilelabs.utils.GestureController
import com.vedizL.mobilelabs.utils.NetworkReceiver
import com.vedizL.mobilelabs.utils.NotificationHelper
import com.vedizL.mobilelabs.data.auth.AuthManager
import com.vedizL.mobilelabs.data.auth.BiometricAuthManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var tvDisplay: TextView
    private lateinit var calculator: Calculator
    private lateinit var gestureController: GestureController
    private lateinit var prefs: ThemePreferences
    private lateinit var advancedRow: LinearLayout
    private lateinit var contentContainer: LinearLayout
    private var isAdvancedVisible = false
    private val themeRepo = ThemeRepository()
    private var themeListener: ListenerRegistration? = null
    private lateinit var historyLauncher: ActivityResultLauncher<Intent>
    private lateinit var networkReceiver: NetworkReceiver
    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = ThemePreferences(this)
        ThemeManager.applyDefaultTheme(prefs.getThemeMode())

        NotificationHelper.init(this)
        BiometricAuthManager.init(this)

        notificationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show()
            }
        }

        requestNotificationPermission()

        setContentView(R.layout.activity_main)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        tvDisplay = findViewById(R.id.tvDisplay)
        calculator = Calculator()
        advancedRow = findViewById(R.id.advancedRow)
        contentContainer = findViewById(R.id.contentContainer)

        gestureController = GestureController(
            context = this,
            calculator = calculator,
            displayView = tvDisplay,
            onDisplayUpdate = { updateDisplay() },
            onShowToast = { showToast(it) }
        )

        if (savedInstanceState != null) {
            restoreCalculatorState(savedInstanceState)
        }

        applyAdaptiveLayout(resources.configuration.orientation)

        ThemeManager.applyCustomColors(this)
        setupButtonListeners()

        if (!prefs.isTutorialShown()) {
            showGestureTutorial()
            prefs.setTutorialShown()
        }

        updateDisplay()

        historyLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                val value = data?.getStringExtra("selected_value")
                if (!value.isNullOrEmpty()) {
                    calculator.setCurrentInput(value)
                    updateDisplay()
                }
            }
        }

        startThemeListener()

        networkReceiver = NetworkReceiver()
        val filter = android.content.IntentFilter()
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE")
        registerReceiver(networkReceiver, filter)
    }

    private fun logEvent(type: String, details: String) {
        val e = ActionEvent(System.currentTimeMillis(), type, details)
        ActionHistoryStore.log(this, e)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(permission)
            }
        }
    }

    private fun startThemeListener() {
        if (AuthManager.isAnonymous()) {
            return
        }

        val uid = AuthManager.getUserId()

        themeListener = FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .collection("theme")
            .document("settings")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val data = snapshot.data ?: return@addSnapshotListener

                    val mode = data["mode"] as? String ?: "light"
                    prefs.saveThemeMode(mode)

                    val custom = data["custom_enabled"] as? Boolean ?: false
                    prefs.setCustomThemeEnabled(custom)

                    if (custom) {
                        prefs.saveColor(ThemeKeys.PRIMARY, (data["primary_color"] as? Long)?.toInt() ?: 0)
                        prefs.saveColor(ThemeKeys.BUTTON, (data["button_color"] as? Long)?.toInt() ?: 0)
                        prefs.saveColor(ThemeKeys.DISPLAY_BG, (data["display_bg"] as? Long)?.toInt() ?: 0)
                        prefs.saveColor(ThemeKeys.DISPLAY_TEXT, (data["display_text"] as? Long)?.toInt() ?: 0)
                    }

                    ThemeManager.applyDefaultTheme(mode)
                    ThemeManager.applyCustomColors(this)
                    updateDisplay()
                }
            }
    }

    override fun onResume() {
        super.onResume()
        ThemeManager.applyCustomColors(this)

        if (prefs.isCustomThemeEnabled() && !AuthManager.isAnonymous()) {
            lifecycleScope.launch {
                themeRepo.saveTheme(
                    mapOf(
                        "mode" to prefs.getThemeMode(),
                        "custom_enabled" to true,
                        "primary_color" to prefs.getColor(ThemeKeys.PRIMARY, 0),
                        "button_color" to prefs.getColor(ThemeKeys.BUTTON, 0),
                        "display_bg" to prefs.getColor(ThemeKeys.DISPLAY_BG, 0),
                        "display_text" to prefs.getColor(ThemeKeys.DISPLAY_TEXT, 0),
                        "source" to "custom"
                    )
                )
            }
        }
    }

    override fun onDestroy() {
        themeListener?.remove()
        unregisterReceiver(networkReceiver)
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        val passwordItem = menu.findItem(R.id.action_password_settings)
        val logoutItem = menu.findItem(R.id.action_logout)

        if (AuthManager.isAnonymous()) {
            passwordItem?.title = "Login"
            logoutItem?.isVisible = false
        } else {
            passwordItem?.title = "Change Password"
            logoutItem?.isVisible = true
        }

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
            R.id.action_history -> {
                openHistory()
                true
            }
            R.id.action_password_settings -> {
                if (AuthManager.isAnonymous()) {
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                } else {
                    showChangePasswordDialog()
                }
                true
            }
            R.id.action_logout -> {
                showLogoutConfirmation()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun openHistory() {
        val intent = Intent(this, com.vedizL.mobilelabs.ui.history.HistoryActivity::class.java)
        historyLauncher.launch(intent)
    }

    private fun showChangePasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)
        val etOldPassword = dialogView.findViewById<EditText>(R.id.etOldPassword)
        val etNewPassword = dialogView.findViewById<EditText>(R.id.etNewPassword)
        val etConfirmPassword = dialogView.findViewById<EditText>(R.id.etConfirmPassword)

        AlertDialog.Builder(this)
            .setTitle("Change Password")
            .setView(dialogView)
            .setPositiveButton("Change") { _, _ ->
                val oldPassword = etOldPassword.text.toString()
                val newPassword = etNewPassword.text.toString()
                val confirmPassword = etConfirmPassword.text.toString()

                if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                    Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newPassword.length < 6) {
                    Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newPassword != confirmPassword) {
                    Toast.makeText(this, "New passwords do not match", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                Toast.makeText(this, "Changing password...", Toast.LENGTH_SHORT).show()
                changePassword(oldPassword, newPassword)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun changePassword(oldPassword: String, newPassword: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: run {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val email = user.email ?: run {
            Toast.makeText(this, "Email not found", Toast.LENGTH_SHORT).show()
            return
        }

        val credential = EmailAuthProvider.getCredential(email, oldPassword)

        user.reauthenticate(credential)
            .addOnCompleteListener { reauthTask ->
                if (!reauthTask.isSuccessful) {
                    Toast.makeText(this, "Incorrect current password", Toast.LENGTH_SHORT).show()
                    return@addOnCompleteListener
                }

                user.updatePassword(newPassword)
                    .addOnCompleteListener { updateTask ->
                        if (updateTask.isSuccessful) {
                            Toast.makeText(this, "Password changed successfully", Toast.LENGTH_SHORT).show()

                            if (BiometricAuthManager.isBiometricEnabledForEmail(email)) {
                                val newToken = BiometricAuthManager.generateBiometricToken()
                                BiometricAuthManager.saveBiometricToken(email, newToken)
                            }
                        } else {
                            Toast.makeText(this, "Failed to change password: ${updateTask.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Logging out will make you anonymous. All data will be stored locally.")
            .setPositiveButton("Logout") { _, _ ->
                AuthManager.logout()
                Toast.makeText(this, "Logged out. Now in anonymous mode.", Toast.LENGTH_SHORT).show()
                invalidateOptionsMenu()
                recreate()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toggleTheme() {
        val current = prefs.getThemeMode()
        val newMode = if (current == "light") "dark" else "light"

        prefs.saveThemeMode(newMode)
        prefs.setCustomThemeEnabled(false)

        if (!AuthManager.isAnonymous()) {
            lifecycleScope.launch {
                themeRepo.saveTheme(
                    mapOf(
                        "mode" to newMode,
                        "custom_enabled" to false,
                        "source" to "default"
                    )
                )
            }
        }

        NotificationHelper.send(
            this,
            "Theme Changed",
            if (newMode == "dark") "Dark theme enabled." else "Light theme enabled.",
            2001
        )

        ThemeManager.applyDefaultTheme(newMode)
        recreate()
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

        findViewById<Button>(R.id.btnExpand).setOnClickListener { toggleAdvancedPanel() }

        findViewById<Button>(R.id.btnSqrtRow).setOnClickListener { onSqrtClick() }
        findViewById<Button>(R.id.btnSquareRow).setOnClickListener { onSquareClick() }
        findViewById<Button>(R.id.btnPowerRow).setOnClickListener { onPowerClick() }
        findViewById<Button>(R.id.btnFactorialRow).setOnClickListener { onFactorialClick() }
    }

    private fun toggleAdvancedPanel() {
        isAdvancedVisible = !isAdvancedVisible

        if (isAdvancedVisible) {
            contentContainer.weightSum = 7f
            advancedRow.layoutParams = (advancedRow.layoutParams as LinearLayout.LayoutParams).apply {
                weight = 1f
                height = 0
            }
            findViewById<LinearLayout>(R.id.glButtons).layoutParams = (findViewById<LinearLayout>(R.id.glButtons).layoutParams as LinearLayout.LayoutParams).apply {
                weight = 5f
            }
            advancedRow.visibility = View.VISIBLE
        } else {
            contentContainer.weightSum = 6f
            advancedRow.layoutParams = (advancedRow.layoutParams as LinearLayout.LayoutParams).apply {
                weight = 0f
                height = 0
            }
            findViewById<LinearLayout>(R.id.glButtons).layoutParams = (findViewById<LinearLayout>(R.id.glButtons).layoutParams as LinearLayout.LayoutParams).apply {
                weight = 5f
            }
            advancedRow.visibility = View.GONE
        }
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

        AlertDialog.Builder(this)
            .setTitle("Gesture Tutorial")
            .setMessage(message)
            .setPositiveButton("Got it!") { dialog, _ -> dialog.dismiss() }
            .setCancelable(false)
            .show()
    }

    private fun onNumberClick(number: String) {
        if (calculator.inputDigit(number)) {
            updateDisplay()
        }
        else if (calculator.isErrorState) {
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
        }
        else if (calculator.isErrorState) showErrorState()
    }

    private fun onPercentClick() {
        if (calculator.applyPercent()) {
            updateDisplay()
        }
    }

    private fun onSqrtClick() {
        if (calculator.applySquareRoot()) {
            updateDisplay()
        }
        else if (calculator.isErrorState) showErrorState()
    }

    private fun onSquareClick() {
        if (calculator.applySquare()) {
            updateDisplay()
        }
        else if (calculator.isErrorState) showErrorState()
    }

    private fun onPowerClick() {
        if (calculator.applyPower()) {
            updateDisplay()
            showToast("Enter exponent")
        } else if (calculator.isErrorState) {
            showErrorState()
        }
    }

    private fun onFactorialClick() {
        if (calculator.applyFactorial()) {
            updateDisplay()
        }
        else if (calculator.isErrorState) showErrorState()
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
        calculator.finalizeExpressionBeforeEquals()
        val logExpr = calculator.getExpressionLog()
        if (calculator.calculateResult()) {
            updateDisplay()
            val finalRes = calculator.currentInput

            logEvent("equals", "$logExpr=$finalRes")
            calculator.resetExpressionLog()

            NotificationHelper.send(
                this,
                "History Added",
                "$logExpr = $finalRes",
                3001
            )

        } else if (calculator.isErrorState) {
            showErrorState()
        }
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
            isErrorState = savedInstanceState.getBoolean("isErrorState", false),
            pendingOperation = savedInstanceState.getString("pendingOperation")
        )
        calculator.restoreState(state)
        isAdvancedVisible = savedInstanceState.getBoolean("isAdvancedVisible", false)
        advancedRow.visibility = if (isAdvancedVisible) View.VISIBLE else View.GONE
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("isAdvancedVisible", isAdvancedVisible)
        val calcState = calculator.saveState()
        outState.putString("currentInput", calcState.currentInput)
        outState.putString("previousInput", calcState.previousInput)
        outState.putString("currentOperation", calcState.currentOperation)
        outState.putBoolean("shouldResetInput", calcState.shouldResetInput)
        outState.putBoolean("isErrorState", calcState.isErrorState)
        outState.putString("pendingOperation", calcState.pendingOperation)
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        applyAdaptiveLayout(newConfig.orientation)
        advancedRow.visibility = if (isAdvancedVisible) View.VISIBLE else View.GONE
    }

    private fun applyAdaptiveLayout(orientation: Int) {
        val display = tvDisplay
        val buttons = listOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9,
            R.id.btnAdd, R.id.btnSubtract, R.id.btnMultiply, R.id.btnDivide,
            R.id.btnDecimal, R.id.btnPercent, R.id.btnClear,
            R.id.btnPlusMinus, R.id.btnEquals, R.id.btnExpand,
            R.id.btnSqrtRow, R.id.btnSquareRow, R.id.btnPowerRow,
            R.id.btnFactorialRow
        ).map { findViewById<Button>(it) }

        val isLandscape = orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val isPanelExpanded = isAdvancedVisible

        if (isLandscape) {
            display.textSize = if (isPanelExpanded) 8f else 12f
            val buttonSize = if (isPanelExpanded) 12f else 14f
            buttons.forEach { it.textSize = buttonSize }
        } else {
            display.textSize = 30f
            buttons.forEach { it.textSize = 22f }
        }
    }
}
package com.vedizL.mobilelabs.utils

object Constants {
    // Calculator constants
    const val MAX_INPUT_LENGTH = 15
    const val ERROR_MESSAGE = "Error"
    const val ERROR_DISPLAY_TIME = 1500L
    const val INITIAL_DISPLAY_VALUE = "0"

    // Gesture constants
    const val SWIPE_THRESHOLD = 100
    const val SWIPE_VELOCITY_THRESHOLD = 100
    const val COPY_FEEDBACK_DURATION = 200L

    // SharedPreferences keys
    const val PREFS_NAME = "calculator_prefs"
    const val KEY_FIRST_LAUNCH = "first_launch"

    // Animation resources
    const val ANIM_SWIPE_LEFT = "swipe_left"
    const val ANIM_SWIPE_RIGHT = "swipe_right"
    const val ANIM_RETURN_NORMAL = "return_to_normal"

    // Operation symbols
    const val OP_ADD = "+"
    const val OP_SUBTRACT = "-"
    const val OP_MULTIPLY = "×"
    const val OP_DIVIDE = "÷"
}
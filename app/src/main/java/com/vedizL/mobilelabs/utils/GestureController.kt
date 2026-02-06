package com.vedizL.mobilelabs.utils

import android.content.ClipboardManager
import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import com.vedizL.mobilelabs.R
import com.vedizL.mobilelabs.model.Calculator

class GestureController(
    private val context: Context,
    private val calculator: Calculator,
    private val displayView: View,
    private val onDisplayUpdate: () -> Unit,
    private val onShowToast: (String) -> Unit
) {
    private lateinit var gestureDetector: GestureDetector

    init {
        setupGestureDetector()
    }

    private fun setupGestureDetector() {
        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
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
                        if (Math.abs(diffX) > Constants.SWIPE_THRESHOLD &&
                            Math.abs(velocityX) > Constants.SWIPE_VELOCITY_THRESHOLD) {
                            if (diffX > 0) {
                                onSwipeRight()
                            } else {
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
                calculator.clear()
                onDisplayUpdate()
                onShowToast(context.getString(R.string.toast_clear_display))
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                copyToClipboard()
                return true
            }
        })

        displayView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun onSwipeLeft() {
        val deleted = calculator.deleteLast()
        if (deleted != null) {
            // Animation
            val swipeAnim = AnimationUtils.loadAnimation(context,
                context.resources.getIdentifier(
                    Constants.ANIM_SWIPE_LEFT,
                    "anim",
                    context.packageName
                )
            )
            displayView.startAnimation(swipeAnim)

            // Update display after animation
            displayView.postDelayed({
                onDisplayUpdate()
                onShowToast(context.getString(R.string.toast_delete_digit))

                // Return animation
                val returnAnim = AnimationUtils.loadAnimation(context,
                    context.resources.getIdentifier(
                        Constants.ANIM_RETURN_NORMAL,
                        "anim",
                        context.packageName
                    )
                )
                displayView.startAnimation(returnAnim)
            }, 200)
        }
    }

    private fun onSwipeRight() {
        // Animation
        val swipeAnim = AnimationUtils.loadAnimation(context,
            context.resources.getIdentifier(
                Constants.ANIM_SWIPE_RIGHT,
                "anim",
                context.packageName
            )
        )
        displayView.startAnimation(swipeAnim)

        // Clear after animation
        displayView.postDelayed({
            calculator.clear()
            onDisplayUpdate()
            onShowToast(context.getString(R.string.toast_clear_display))

            // Return animation
            val returnAnim = AnimationUtils.loadAnimation(context,
                context.resources.getIdentifier(
                    Constants.ANIM_RETURN_NORMAL,
                    "anim",
                    context.packageName
                )
            )
            displayView.startAnimation(returnAnim)
        }, 200)
    }

    private fun copyToClipboard() {
        if (calculator.isErrorState) return

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = android.content.ClipData.newPlainText("Calculator result", calculator.currentInput)
        clipboard.setPrimaryClip(clip)

        // Visual feedback
        displayView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_blue_light))

        displayView.postDelayed({
            displayView.setBackgroundResource(R.drawable.display_background)
            onShowToast(context.getString(R.string.toast_copied))
        }, Constants.COPY_FEEDBACK_DURATION)
    }
}
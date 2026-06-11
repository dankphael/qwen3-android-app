package com.example.qwen3chat.ui.chatdetail

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import com.example.qwen3chat.R

/**
 * Typing indicator view with 3 bouncing dots.
 * Pill-shaped background, 36dp height, 1200ms animation cycle.
 */
class TypingIndicatorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var dot1: View
    private var dot2: View
    private var dot3: View
    private var animatorSet: AnimatorSet? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.view_typing_indicator, this, true)
        dot1 = findViewById(R.id.dot1)
        dot2 = findViewById(R.id.dot2)
        dot3 = findViewById(R.id.dot3)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startAnimation()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }

    fun startAnimation() {
        if (animatorSet?.isRunning == true) return

        val bounceUp1 = ObjectAnimator.ofFloat(dot1, "translationY", 0f, -8f, 0f).apply {
            duration = 400
            interpolator = AccelerateDecelerateInterpolator()
            repeatCount = ObjectAnimator.INFINITE
        }
        val bounceUp2 = ObjectAnimator.ofFloat(dot2, "translationY", 0f, -8f, 0f).apply {
            duration = 400
            interpolator = AccelerateDecelerateInterpolator()
            startDelay = 200
            repeatCount = ObjectAnimator.INFINITE
        }
        val bounceUp3 = ObjectAnimator.ofFloat(dot3, "translationY", 0f, -8f, 0f).apply {
            duration = 400
            interpolator = AccelerateDecelerateInterpolator()
            startDelay = 400
            repeatCount = ObjectAnimator.INFINITE
        }

        animatorSet = AnimatorSet().apply {
            playTogether(bounceUp1, bounceUp2, bounceUp3)
            start()
        }
    }

    fun stopAnimation() {
        animatorSet?.let {
            it.removeAllListeners()
            it.cancel()
        }
        animatorSet = null
        dot1.translationY = 0f
        dot2.translationY = 0f
        dot3.translationY = 0f
    }
}
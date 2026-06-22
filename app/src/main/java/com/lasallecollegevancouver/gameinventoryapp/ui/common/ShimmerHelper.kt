package com.lasallecollegevancouver.gameinventoryapp.ui.common

import android.animation.ValueAnimator
import android.view.View

/**
 * ShimmerHelper drives the "premium" loading animation for skeleton screens.
 *
 * Rather than pulling in a third-party shimmer library, we get the same effect by gently
 * animating the opacity (alpha) of a whole skeleton card up and down forever. The gray blocks
 * inside the card therefore appear to "breathe", which reads as a polished loading state.
 *
 * Usage:
 *   - call [start] when a skeleton view appears on screen
 *   - call [stop]  when it is recycled or the real content arrives, to release the animator
 *
 * The running animator is stored on the view itself via setTag so each skeleton card owns its
 * own animation and we never leak one animator into the wrong recycled view.
 */
object ShimmerHelper {

    // A unique tag key (any unused resource-style id works) so our animator does not collide
    // with other libraries that might also call setTag on the same view.
    private const val ANIMATOR_TAG_KEY = 0x7F_5A_11_01

    // The opacity the card fades down to at the dimmest point of the pulse.
    private const val MINIMUM_ALPHA = 0.35f

    // Full opacity at the brightest point of the pulse.
    private const val MAXIMUM_ALPHA = 1.0f

    // One full dim-to-bright sweep in milliseconds. Reversing makes a complete pulse ~1.6s.
    private const val SWEEP_DURATION_MILLIS = 800L

    /** Start (or restart) the breathing shimmer animation on [view]. */
    fun start(view: View) {
        // If this view is already shimmering, leave the existing animation running.
        if (view.getTag(ANIMATOR_TAG_KEY) is ValueAnimator) return

        val pulseAnimator = ValueAnimator.ofFloat(MINIMUM_ALPHA, MAXIMUM_ALPHA).apply {
            duration = SWEEP_DURATION_MILLIS
            repeatMode = ValueAnimator.REVERSE        // bright -> dim -> bright, smoothly
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animation ->
                view.alpha = animation.animatedValue as Float
            }
            start()
        }

        view.setTag(ANIMATOR_TAG_KEY, pulseAnimator)
    }

    /** Stop the shimmer animation on [view] and reset it back to full opacity. */
    fun stop(view: View) {
        (view.getTag(ANIMATOR_TAG_KEY) as? ValueAnimator)?.cancel()
        view.setTag(ANIMATOR_TAG_KEY, null)
        view.alpha = MAXIMUM_ALPHA
    }
}

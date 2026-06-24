package de.maengelmelder.mainmodule.customviews

import android.content.Context
import androidx.viewpager.widget.ViewPager
import android.util.AttributeSet
import android.view.MotionEvent

/**
 * This viewpager can activate or deactivate its swipe function with [canSwipe] property
 */
internal class SwipeToggleableViewPager(c: Context, set: AttributeSet?) : androidx.viewpager.widget.ViewPager(c, set) {

    /**
     * Whether the viewpager can be swiped to change the page
     */
    private var bCanSwipe = true

    override fun onTouchEvent(ev: MotionEvent?): Boolean =
        if (bCanSwipe) super.onTouchEvent(ev) else false

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean =
        if (bCanSwipe) {
            try { super.onInterceptTouchEvent(ev) } catch (e: Exception) { false }
        } else {
            false
        }

    var canSwipe: Boolean
        get() = bCanSwipe
        set(value) { bCanSwipe = value }
}
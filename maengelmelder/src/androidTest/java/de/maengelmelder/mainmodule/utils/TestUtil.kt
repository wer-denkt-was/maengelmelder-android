package de.maengelmelder.mainmodule.utils

import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import androidx.test.espresso.Espresso
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.*
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Matcher

object TestUtil {

    fun waitInUI(delayMs: Long) {
        Espresso.onView(ViewMatchers.isRoot()).perform(waitForViewAction(2000))
    }

    fun tapXY(x: Int, y: Int): GeneralClickAction {
        return GeneralClickAction(Tap.SINGLE, CoordinatesProvider { v ->
            val screenPos = IntArray(2)
            v?.getLocationOnScreen(screenPos)
            val screenX = (screenPos[0] + x).toFloat()
            val screenY = (screenPos[1] + y).toFloat()
            floatArrayOf(screenX, screenY)
        }, Press.FINGER, InputDevice.SOURCE_MOUSE, MotionEvent.BUTTON_PRIMARY)
    }

    private fun waitForViewAction(delayMs: Long): ViewAction {
        return object: ViewAction {
            override fun getConstraints(): Matcher<View> = ViewMatchers.isRoot()
            override fun getDescription(): String = "delay for $delayMs milliseconds"
            override fun perform(uiController: UiController?, view: View?) {
                uiController?.loopMainThreadForAtLeast(delayMs)
            }
        }
    }

}
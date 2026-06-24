package de.maengelmelder.mainmodule.ui

import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.activities.SplashscreenActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class SplashscreenTest {

    // starts from Splashscreen Activity
    @get:Rule val activitySplashscreen = ActivityScenarioRule(SplashscreenActivity::class.java)

    @Test
    fun splashscreenImageShown() {
        // Makes sure that splashscreen image is shown fully on screen
        Espresso.onView(ViewMatchers.withId(R.id.splashscreen))
                .check(ViewAssertions.matches(ViewMatchers.isCompletelyDisplayed()))
    }
}
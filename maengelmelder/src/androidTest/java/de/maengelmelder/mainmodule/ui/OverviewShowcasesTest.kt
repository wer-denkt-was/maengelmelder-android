package de.maengelmelder.mainmodule.ui

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.activities.OverviewActivity
import de.maengelmelder.mainmodule.activities.TermsActivity
import de.maengelmelder.mainmodule.utils.TestUtil
import org.hamcrest.core.AllOf
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.jar.Manifest

@RunWith(AndroidJUnit4::class)
@LargeTest
class OverviewShowcasesTest {

    @get:Rule
    val activityOverview = ActivityScenarioRule(OverviewActivity::class.java)

    // Grant all permissions to skip dialog
    @get:Rule val permReadExtStorage = GrantPermissionRule.grant(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    @get:Rule val permWriteExtStorage = GrantPermissionRule.grant(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
    @get:Rule val permCamera = GrantPermissionRule.grant(android.Manifest.permission.CAMERA)
    @get:Rule val permLoc = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION)

    lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        // Reset preference for Showcases
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean("wdw.mm.tut_home.shown", false)
                .putBoolean("wdw.mm.tut_new_msg.shown", false)
                .commit()
    }

    @Test
    fun showcases() {
        // WARN: Showcase UI's text cannot be identified, but the OK button can be interacted

        // Wait for the showcase to load
        TestUtil.waitInUI(3000)

        // Perform click on "ok" button for 1st showcase
        Espresso.onView(ViewMatchers.withText("OK"))
                .perform(ViewActions.click())

        // Wait for next showcase to load
        TestUtil.waitInUI(3000)

        // Perform click on "ok" button on 2nd showcase
        Espresso
                .onView(AllOf.allOf(ViewMatchers.withText("OK"),
                        ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
                .perform(ViewActions.click())

        // Wait for next showcase to load
        TestUtil.waitInUI(3000)

        // Map should be fully displayed
        Espresso.onView(ViewMatchers.withId(R.id.map))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        // preference should be correctly saved
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val tut1Seen = pref.getBoolean("wdw.mm.tut_home.shown", false)
        val tut2Seen = pref.getBoolean("wdw.mm.tut_new_msg.shown", false)
        Assert.assertTrue("Tutorial will only be shown one time",
            tut1Seen && tut2Seen)
    }

}
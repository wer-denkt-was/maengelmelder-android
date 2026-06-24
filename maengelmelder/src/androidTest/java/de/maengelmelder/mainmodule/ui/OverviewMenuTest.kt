package de.maengelmelder.mainmodule.ui

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.activities.MessageProcessActivity
import de.maengelmelder.mainmodule.activities.OverviewActivity
import de.maengelmelder.mainmodule.database.MMDB
import de.maengelmelder.mainmodule.utils.TestUtil
import okhttp3.internal.wait
import org.hamcrest.CoreMatchers
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class OverviewMenuTest {
    @get:Rule
    val activityOverview = ActivityScenarioRule(OverviewActivity::class.java)

    // Grant all permissions to skip dialog
    @get:Rule
    val permReadExtStorage = GrantPermissionRule.grant(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    @get:Rule
    val permWriteExtStorage = GrantPermissionRule.grant(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
    @get:Rule
    val permCamera = GrantPermissionRule.grant(android.Manifest.permission.CAMERA)
    @get:Rule
    val permLoc = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION)

    lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        // For this test, we skip showcases
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean("wdw.mm.tut_home.shown", true)
                .putBoolean("wdw.mm.tut_new_msg.shown", true)
                .commit()
        // Clear all DB entries for messages
        val db = MMDB.instance(context)
        db.truncate(db.constants.TBL_MESSAGES)
    }

    @Test
    fun menuElements() {
        // Map should be fully displayed
        Espresso.onView(ViewMatchers.withId(R.id.map))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        // my location and new message are displayed
        Espresso.onView(ViewMatchers.withId(R.id.my_location))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withId(R.id.new_message))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        // Menu (home) button is visible
        Espresso.onView(ViewMatchers.withId(R.id.toolbar))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun mapInteract() {
        // perform swipe on map
        Espresso.onView(ViewMatchers.withId(R.id.map)).perform(ViewActions.swipeLeft())
        // wait a bit
        TestUtil.waitInUI(500)
        // It should try to load messages
        Espresso.onView(ViewMatchers.withId(R.id.status))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun slidingMenu() {
        // perform click on home button in overview (top left)
        Espresso.onView(ViewMatchers.withId(R.id.toolbar)).perform(TestUtil.tapXY(10, 10))

        // Wait for few ms
        TestUtil.waitInUI(500)

        // It should show sliding menu
        Espresso.onView(ViewMatchers.withId(R.id.slidingmenu_root))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        // pressing back should close the sliding menu
        Espresso.onView(ViewMatchers.isRoot()).perform(ViewActions.pressBack())

        // wait for few ms (closing animation)
        TestUtil.waitInUI(500)

        // Sliding menu should be gone
        Espresso.onView(ViewMatchers.withId(R.id.slidingmenu_root))
                .check(ViewAssertions.matches(CoreMatchers.not(ViewMatchers.isDisplayed())))
    }

    @Test
    fun newMessage() {
        Intents.init()

        // perform click on new message button
        Espresso.onView(ViewMatchers.withId(R.id.new_message)).perform(ViewActions.click())

        // wait for few ms (closing animation)
        TestUtil.waitInUI(500)

        // it should launch MessageProcessActivity
        Intents.intending(IntentMatchers.hasComponent(MessageProcessActivity::class.simpleName))

        // It should also create a new message
        val db = MMDB.instance(context)
        val msgs = db.getMessages(db.constants.COL_ORIGIN to db.constants.ORIGIN_SELF)
        Assert.assertTrue("a new message should be created", msgs.isNotEmpty())
    }

}
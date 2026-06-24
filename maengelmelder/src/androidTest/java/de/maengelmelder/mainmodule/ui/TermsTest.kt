package de.maengelmelder.mainmodule.ui

import android.content.Context
import android.util.Log
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
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.activities.OverviewActivity
import de.maengelmelder.mainmodule.activities.TermsActivity
import de.maengelmelder.mainmodule.utils.ResourceProxy
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class TermsTest {

    @get:Rule
    val activityTerms = ActivityScenarioRule(TermsActivity::class.java)

    lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        // Reset the terms and condition preference first
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(TermsActivity.PREF_POLICY_ACCEPTED, false)
                .putBoolean(TermsActivity.PREF_TERMS_ACCEPTED, false)
                .commit()
    }

    @Test
    fun termsInteraction() {
        Intents.init()

        // Initial testing
        Assert.assertTrue("Initially user has not accepted terms and policy",
                !ResourceProxy.hasUserAcceptedTermsAndCondition(context))

        val btnAccept = Espresso.onView(ViewMatchers.withId(R.id.accept))
        val btnReject = Espresso.onView(ViewMatchers.withId(R.id.reject))
        val webViewWelcome = Espresso.onView(ViewMatchers.withId(R.id.webview_for_welcometext))
        val webViewTerms = Espresso.onView(ViewMatchers.withId(R.id.webview_for_terms))
        val webViewPolicy = Espresso.onView(ViewMatchers.withId(R.id.webview_for_policy))

        // Makes sure that important views are shown fully on screen
        btnAccept.check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        btnReject.check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        webViewWelcome.check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        // After "accept" is clicked, it should move from welcome text to terms
        btnAccept.perform(ViewActions.click())
        webViewWelcome.check(ViewAssertions.matches(CoreMatchers.not(ViewMatchers.isDisplayed())))
        webViewTerms.check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        // After "accept" is clicked again, it should move from terms to policy
        btnAccept.perform(ViewActions.click())
        webViewTerms.check(ViewAssertions.matches(CoreMatchers.not(ViewMatchers.isDisplayed())))
        webViewPolicy.check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        // If "accept" is clicked again during Policy, it will start the Overview activity
        Intents.intending(IntentMatchers.hasComponent(OverviewActivity::class.simpleName))
        btnAccept.perform(ViewActions.click())
        // How do we prove that Overview activity is displayed? We can check if map is displayed
        Espresso.onView(ViewMatchers.withId(R.id.map))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        // User has accepted both terms and condition
        Assert.assertTrue("user has accepted terms and policy",
                ResourceProxy.hasUserAcceptedTermsAndCondition(context))
    }

}
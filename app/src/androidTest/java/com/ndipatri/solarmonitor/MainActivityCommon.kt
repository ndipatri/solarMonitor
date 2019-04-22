package com.ndipatri.solarmonitor

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.PositionAssertions.isAbove
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.ndipatri.solarmonitor.utils.Matchers.isTextMatchingPattern
import java.util.regex.Pattern

// The actual test assertions are common across Live and Mock testing
// scenarios
fun assertFindingPanelViews(expectedPanelId: String) {
    onView(withText("Click to find nearby solar panel.")).check(matches(isCompletelyDisplayed()))

    onView(withId(R.id.scanFAB)).check(matches(isDisplayed())).perform(click())

    onView(withText("Click to load solar output ...")).check(matches(isDisplayed())).check(isAbove(withText("solar panel ($expectedPanelId)")))
}

fun assertLoadingSolarOutputViews(expectedOutputPattern: String, expectedPanelId: String) {

    onView(withId(R.id.scanFAB)).check(matches(isDisplayed())).perform(click())

    // No need to wait for real hardware to scan for panel.. because our test thread is
    // blocked on app's background thread

    onView(withId(R.id.loadFAB)).check(matches(isDisplayed())).perform(click())

    // No need to wait for real network call to get solar output.. because our test thread is
    // blocked on app's background thread

    onView(withId(R.id.mainTextView)).check(matches(isTextMatchingPattern(expectedOutputPattern))).check(matches(isDisplayed())).check(isAbove(withText("solar panel ($expectedPanelId)")))
}

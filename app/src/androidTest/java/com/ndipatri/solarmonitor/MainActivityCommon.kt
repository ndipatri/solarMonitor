package com.ndipatri.solarmonitor

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.PositionAssertions.isAbove
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.*

fun assertFindingPanelViews(expectedPanelId: String) {
    onView(withText("Click to find nearby solar panel.")).check(matches(isCompletelyDisplayed()))

    // NJD TODO - first one fails.. don't know why.. not bug in test, but bug in app code
    onView(withId(R.id.scanFAB)).check(matches(isDisplayed())).perform(click())

    onView(withId(R.id.scanFAB)).check(matches(isDisplayed())).perform(click())

    onView(withText("Click to load solar output ...")).check(matches(isDisplayed())).check(isAbove(withText("solar panel ($expectedPanelId)")))
}

fun assertLoadingSolarOutputViews(expectedOutputMessage: String, expectedPanelId: String) {

    onView(withId(R.id.scanFAB)).check(matches(isDisplayed())).perform(click())

    // No need to wait for real hardware to scan for panel.. because our test thread is
    // blocked on app's background thread

    onView(withId(R.id.loadFAB)).check(matches(isDisplayed())).perform(click())

    // No need to wait for real network call to get solar output.. because our test thread is
    // blocked on app's background thread

    onView(withText(expectedOutputMessage)).check(matches(isDisplayed())).check(isAbove(withText("solar panel ($expectedPanelId)")))
}

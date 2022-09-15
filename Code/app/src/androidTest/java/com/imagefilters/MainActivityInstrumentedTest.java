package com.imagefilters;

import android.Manifest;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.not;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityInstrumentedTest {
    /**
     * Rule to use Espresso in these tests
     */
    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    /**
     * Rule to allow Espresso access to local storage
     */
    @Rule
    public GrantPermissionRule storageTestRule =
            GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);

    /**
     * Tests whether input Buttons block when user starts download from default URL
     */
    @Test
    public void defaultUrlLoadInterfaceChanged() {
        //Clicking button to start download
        onView(withId(R.id.download_url_default_button)).perform(click());

        //Checking if clickable buttons became disabled
        onView(withId(R.id.select_gallery_button)).check(matches(isNotEnabled()));
        onView(withId(R.id.download_url_default_button)).check(matches(isNotEnabled()));
        onView(withId(R.id.download_url_user_button)).check(matches(isNotEnabled()));

        //Checking if progress bar became visible
        onView(withId(R.id.progress_loader)).check(matches(isEnabled()));
    }

    /**
     * Tests whether input Buttons block when user starts download from their valid URL
     */
    @Test
    public void userUrlLoadInterfaceChanged() {
        //Inputting a valid URL
        onView(withId(R.id.download_url_value))
                .perform(replaceText("http://picsum.photos/100"));

        //Clicking button to start download
        onView(withId(R.id.download_url_user_button)).perform(click());

        //Checking if clickable buttons became disabled
        onView(withId(R.id.select_gallery_button)).check(matches(isNotEnabled()));
        onView(withId(R.id.download_url_default_button)).check(matches(isNotEnabled()));
        onView(withId(R.id.download_url_user_button)).check(matches(isNotEnabled()));

        //Checking if progress bar became visible
        onView(withId(R.id.progress_loader)).check(matches(isDisplayed()));
    }

    /**
     * Tests whether download doesn't start with invalid URL
     */
    @Test
    public void invalidUserUrlBlockDownload() {
        //Inputting an invalid URL
        onView(withId(R.id.download_url_value))
                .perform(replaceText("not a URL"));

        //Clicking button to start download
        onView(withId(R.id.download_url_user_button)).perform(click());

        //Checking if progress bar is still invisible
        onView(withId(R.id.progress_loader)).check(matches(not(isDisplayed())));
    }
}
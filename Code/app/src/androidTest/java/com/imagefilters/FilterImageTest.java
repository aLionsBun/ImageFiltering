package com.imagefilters;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static org.hamcrest.CoreMatchers.is;

import android.Manifest;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.widget.ImageView;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class FilterImageTest {
    /**
     * Rule to use Espresso in these tests
     */
    @Rule
    public ActivityScenarioRule<FilterImage> activityRule =
            new ActivityScenarioRule<>(FilterImage.class);

    /**
     * Rule to allow Espresso access to local storage
     */
    @Rule
    public GrantPermissionRule storageTestRule =
            GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);

    /**
     * Bitmap of source image used for testing
     */
    private Bitmap mSourceImage;

    /**
     * Resource ID of image used for testing, located at res/drawable/ducklings.jpg
     */
    private final int SOURCE_ID = R.drawable.ducklings;

    /**
     * View IDs of all filter buttons
     */
    private final int[] BUTTONS_IDS = { R.id.invert_button, R.id.grayscale_button,
        R.id.blur_button, R.id.sharp_button,
            R.id.change_gbr_button, R.id.change_brg_button };

    /**
     * Tests if any random filter works
     */
    @Test
    public void applyRandomFilter() {
        //Picking random filter
        int randIndex = getRandomFilter();

        //Applying random filter to alter image
        onView(withId(BUTTONS_IDS[randIndex])).perform(scrollTo());
        onView(withId(BUTTONS_IDS[randIndex])).perform(click());

        //Checking if filtered image doesn't match source image
        activityRule.getScenario().onActivity(activity -> {
            Resources resources = getApplicationContext().getResources();
            mSourceImage = BitmapFactory.decodeResource(resources, SOURCE_ID);
            ImageView sourceView = activity.findViewById(R.id.filter_source);
            Bitmap activityImage = ((BitmapDrawable)sourceView.getDrawable()).getBitmap();
            assertThat(mSourceImage.sameAs(activityImage), is(false));
        });
    }

    /**
     * Tests if Clear filters Button works
     */
    @Test
    public void testClearFiltersButton() {
        //Picking random filter
        int randIndex = getRandomFilter();

        //Applying random filter to alter image
        onView(withId(BUTTONS_IDS[randIndex])).perform(scrollTo());
        onView(withId(BUTTONS_IDS[randIndex])).perform(click());

        //Scrolling to and clicking Clear button
        onView(withId(R.id.filter_clear_button)).perform(scrollTo());
        onView(withId(R.id.filter_clear_button)).perform(click());

        //Checking if cleared image matches source image
        activityRule.getScenario().onActivity(activity -> {
            Resources resources = getApplicationContext().getResources();
            mSourceImage = BitmapFactory.decodeResource(resources, SOURCE_ID);
            ImageView sourceView = activity.findViewById(R.id.filter_source);
            Bitmap activityImage = ((BitmapDrawable)sourceView.getDrawable()).getBitmap();
            assertThat(mSourceImage.sameAs(activityImage), is(true));
        });
    }

    /**
     * Helper method to get random filter
     * @return Index of Button View that deploys filter
     *          applying process when clicked
     */
    private int getRandomFilter() {
        return ThreadLocalRandom.current().nextInt(0, BUTTONS_IDS.length);
    }
}
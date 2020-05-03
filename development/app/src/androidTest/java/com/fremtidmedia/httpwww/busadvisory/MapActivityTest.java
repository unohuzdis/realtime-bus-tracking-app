package com.fremtidmedia.httpwww.busadvisory;

import android.location.Location;
import android.location.LocationManager;

import androidx.test.espresso.ViewAssertion;
import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;
import androidx.test.runner.AndroidJUnitRunner;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.rule.ActivityTestRule;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isClickable;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withInputType;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4ClassRunner.class)
@LargeTest
public class MapActivityTest {


    @Rule
    public ActivityTestRule<MapActivity> mActivityRule =
            new ActivityTestRule<>(MapActivity.class);

//    @Test
//    public void mapExists() {
//        onView(withId(R.id.mapfragment)).check((ViewAssertion) isDisplayed());
//    }
//    @Test
//    public void mapTracks() {
//        onView(withId(R.id.BusList)).perform(click());
//        onData(allOf(is(instanceOf(String.class)), is("97 south"))).perform(click());
//        double busLocationv = 0;
//        double buslocationv1 = 0;
//        double buslocationv2 = 0;
//        assertEquals(busLocationv, 49.196261, 0);
//        assertEquals(buslocationv1, -123.004773, 0);
//        assertEquals(buslocationv2, 0.0, 0);
//
//    }
    @Test
    public void listShowsStuff() {
        onView(withId(R.id.BusList)).perform(click()).check(matches(withText("97")));
    }
    @Test
    public void listExpands() {
        onView(withId(R.id.BusList)).perform(click());
        onData(allOf(is(instanceOf(String.class)), is("97 south"))).perform(click());
        onView(withId(R.id.BusList))
                .check(matches(withText(containsString("97 south"))));

    }

    @Test
    public void longCorrect() {
        startTest();
        Double lon = startTest().getLongitude();
        assertTrue((119.4960) > (lon - 1.0) && (119.4960) < (lon + 1.0));
    }

    @Test
    public void latCorrect() {
        startTest();
        Double lat = startTest().getLatitude();
        assertTrue((49.8880) > (lat - 1.0) && (49.8880) < (lat + 1.0));
    }

    @Test
    public Location startTest() {
        MapActivity testMap = new MapActivity();
        Location loc = testMap.locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        return loc;

    }


}
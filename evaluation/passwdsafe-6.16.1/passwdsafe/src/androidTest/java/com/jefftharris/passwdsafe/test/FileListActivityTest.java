/*
 * Copyright (©) 2017 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.test;

import android.os.Environment;
import android.support.test.espresso.DataInteraction;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.test.runner.AndroidJUnit4;
import android.view.Gravity;

import com.jefftharris.passwdsafe.BuildConfig;
import com.jefftharris.passwdsafe.FileListActivity;
import com.jefftharris.passwdsafe.R;

import junit.framework.Assert;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.DrawerActions.open;
import static android.support.test.espresso.contrib.DrawerMatchers.isClosed;
import static android.support.test.espresso.contrib.NavigationViewActions.navigateTo;
import static android.support.test.espresso.intent.Checks.checkNotNull;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasData;
import static android.support.test.espresso.intent.matcher.IntentMatchers.toPackage;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.jefftharris.passwdsafe.test.util.TestUtils.withAdaptedData;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * UI tests for FileListActivity
 */
@RunWith(AndroidJUnit4.class)
public class FileListActivityTest
{
    public static final File DIR = Environment.getExternalStorageDirectory();

    @Rule
    public IntentsTestRule<FileListActivity> itsActivityRule =
            new IntentsTestRule<>(FileListActivity.class);

    @Test
    public void testFiles()
    {
        verifyDrawerClosed();

        onView(withId(android.R.id.list));
        onTestFile("test.psafe3").check(matches(anything()));

        onView(withId(android.R.id.list))
                .check(matches(withAdaptedData(withFileData("test.psafe3"))));
        onView(withId(android.R.id.list))
                .check(matches(not(withAdaptedData(withFileData("none.psafe3")))));
    }

    @Test
    public void testFileNav()
    {
        verifyDrawerClosed();

        Assert.assertTrue(
                DIR.equals(new File("/storage/emulated/0")) ||
                DIR.equals(new File("/mnt/sdcard")));
        onView(withId(R.id.current_group_label))
                .check(matches(withText(DIR.getPath())));
        onView(withId(R.id.current_group_label))
                .perform(click());
        onView(withId(R.id.current_group_label))
                .check(matches(withText(DIR.getParentFile().getPath())));
        onView(withId(R.id.home))
                .perform(click());
        onView(withId(R.id.current_group_label))
                .check(matches(withText(DIR.getPath())));
    }

    @Test
    public void testLaunchFileNew() throws MalformedURLException
    {
        verifyDrawerClosed();

        onView(withId(R.id.menu_file_new))
                .perform(click());

        intended(allOf(
                hasAction(equalTo("com.jefftharris.passwdsafe.action.NEW")),
                toPackage("com.jefftharris.passwdsafe"),
                hasData("file://" + DIR.getAbsolutePath())));
    }

    @Test
    public void testAbout()
    {
        onView(withId(R.id.drawer_layout))
                .check(matches(isClosed(Gravity.START)))
                .perform(open());

        onView(withId(R.id.navigation_drawer))
                .perform(navigateTo(R.id.menu_drawer_about));

        onView(withId(R.id.version))
                .check(matches(withText(BuildConfig.VERSION_NAME + " (DEBUG)")));
    }

    /**
     * Test on a file in the list
     */
    private static DataInteraction onTestFile(
            @SuppressWarnings("SameParameterValue") String fileTitle)
    {
        return onData(allOf(is(instanceOf(HashMap.class)),
                            withFileData(fileTitle)))
                .inAdapterView(withId(android.R.id.list));
    }

    /**
     * Match with a FileData entry matching title text
     */
    private static Matcher<Object> withFileData(String expectedText)
    {
        // use preconditions to fail fast when a test is creating an
        // invalid matcher.
        checkNotNull(expectedText);
        return withFileData(equalTo(expectedText));
    }

    /**
     * Match with a FileData entry matching a title
     */
    private static Matcher<Object> withFileData(
            final Matcher<String> titleMatcher)
    {
        // use preconditions to fail fast when a test is creating an
        // invalid matcher.
        checkNotNull(titleMatcher);
        return new BoundedMatcher<Object, Map>(Map.class)
        {
            @Override
            public boolean matchesSafely(Map data)
            {
                return hasEntry(equalTo("title"), hasToString(titleMatcher))
                        .matches(data);
            }
            @Override
            public void describeTo(Description description) {
                description.appendText("with file data: ");
                titleMatcher.describeTo(description);
            }
        };
    }

    /**
     * Verify the nav drawer is closed
     */
    private void verifyDrawerClosed()
    {
        onView(withId(R.id.drawer_layout))
                .check(matches(isClosed(Gravity.START)));
    }
}

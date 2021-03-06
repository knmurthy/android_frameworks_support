/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.support.v7.app;

import org.junit.Test;

import android.support.v7.appcompat.test.R;
import android.support.v7.testutils.BaseTestActivity;
import android.support.v7.view.ActionMode;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public abstract class BaseKeyEventsTestCase<A extends BaseTestActivity>
        extends BaseInstrumentationTestCase<A> {

    protected BaseKeyEventsTestCase(Class<A> activityClass) {
        super(activityClass);
    }

    @Test
    @SmallTest
    public void testBackDismissesActionMode() {
        final AtomicBoolean destroyed = new AtomicBoolean();

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getActivity().startSupportActionMode(new ActionMode.Callback() {
                    @Override
                    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                        mode.getMenuInflater().inflate(R.menu.sample_actions, menu);
                        return true;
                    }

                    @Override
                    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                        return false;
                    }

                    @Override
                    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                        return false;
                    }

                    @Override
                    public void onDestroyActionMode(ActionMode mode) {
                        destroyed.set(true);
                    }
                });
            }
        });

        getInstrumentation().waitForIdleSync();
        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
        getInstrumentation().waitForIdleSync();

        assertFalse("Activity was not finished", getActivity().isFinishing());
        assertTrue("ActionMode was destroyed", destroyed.get());
    }

    @Test
    @SmallTest
    public void testBackCollapsesSearchView() throws InterruptedException {
        // First expand the SearchView
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                assertTrue("SearchView expanded", getActivity().expandSearchView());
            }
        });
        getInstrumentation().waitForIdleSync();

        // Now send a back press
        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
        getInstrumentation().waitForIdleSync();

        if (getActivity().isSearchViewExpanded()) {
            // If the SearchView is still expanded, it's probably because it had focus and the
            // first back removed the focus. Send another.
            getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
            getInstrumentation().waitForIdleSync();
        }

        // Check that the Activity is still running and the SearchView is not expanded
        assertFalse("Activity was not finished", getActivity().isFinishing());
        assertFalse("SearchView was collapsed", getActivity().isSearchViewExpanded());
    }

    @Test
    @SmallTest
    public void testMenuPressInvokesPanelCallbacks() throws InterruptedException {
        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_MENU);
        getInstrumentation().waitForIdleSync();
        assertTrue("onMenuOpened called", getActivity().wasOnMenuOpenedCalled());

        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_MENU);
        getInstrumentation().waitForIdleSync();
        assertTrue("onPanelClosed called", getActivity().wasOnPanelClosedCalled());
    }

    @Test
    @SmallTest
    public void testBackPressWithMenuInvokesOnPanelClosed() throws InterruptedException {
        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_MENU);
        getInstrumentation().waitForIdleSync();

        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
        getInstrumentation().waitForIdleSync();
        assertTrue("onPanelClosed called", getActivity().wasOnPanelClosedCalled());
    }

    @Test
    @SmallTest
    public void testBackPressWithEmptyMenuFinishesActivity() throws InterruptedException {
        repopulateWithEmptyMenu();

        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_MENU);
        getInstrumentation().waitForIdleSync();

        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
        assertTrue(getActivity().isFinishing());
    }

    @Test
    @SmallTest
    public void testDelKeyEventReachesActivity() {
        // First send the event
        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DEL);
        getInstrumentation().waitForIdleSync();

        KeyEvent downEvent = getActivity().getInvokedKeyDownEvent();
        assertNotNull("onKeyDown called", downEvent);
        assertEquals("onKeyDown event matches", KeyEvent.KEYCODE_DEL, downEvent.getKeyCode());

        KeyEvent upEvent = getActivity().getInvokedKeyUpEvent();
        assertNotNull("onKeyUp called", upEvent);
        assertEquals("onKeyUp event matches", KeyEvent.KEYCODE_DEL, upEvent.getKeyCode());
    }

    @Test
    @SmallTest
    public void testMenuKeyEventReachesActivity() throws InterruptedException {
        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_MENU);
        getInstrumentation().waitForIdleSync();

        KeyEvent downEvent = getActivity().getInvokedKeyDownEvent();
        assertNotNull("onKeyDown called", downEvent);
        assertEquals("onKeyDown event matches", KeyEvent.KEYCODE_MENU, downEvent.getKeyCode());

        KeyEvent upEvent = getActivity().getInvokedKeyUpEvent();
        assertNotNull("onKeyUp called", upEvent);
        assertEquals("onKeyDown event matches", KeyEvent.KEYCODE_MENU, upEvent.getKeyCode());
    }

    private void repopulateWithEmptyMenu() throws InterruptedException {
        int count = 0;
        getActivity().setShouldPopulateOptionsMenu(false);
        while (count++ < 10) {
            Menu menu = getActivity().getMenu();
            if (menu == null || menu.size() != 0) {
                Thread.sleep(100);
            } else {
                return;
            }
        }
    }
}

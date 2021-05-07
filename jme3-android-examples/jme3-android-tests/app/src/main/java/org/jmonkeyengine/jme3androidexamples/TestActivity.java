/*
 * Copyright (c) 2009-2021 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.jmonkeyengine.jme3androidexamples;

import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.jme3.system.JmeSystem;
import androidx.appcompat.app.AppCompatActivity;

public class TestActivity extends AppCompatActivity {
    JmeFragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        fragment = new JmeFragment();

        // Supply index input as an argument.
        Bundle args = new Bundle();

        Bundle bundle = savedInstanceState;
        if (bundle == null) {
            bundle = getIntent().getExtras();
        }

        String appClass = bundle.getString(MainActivity.SELECTED_APP_CLASS);
        args.putString(MainActivity.SELECTED_APP_CLASS, appClass);
//        Log.d(TestActivity.class.getSimpleName(), "AppClass="+appClass);

        boolean mouseEnabled = bundle.getBoolean(MainActivity.ENABLE_MOUSE_EVENTS, true);
        args.putBoolean(MainActivity.ENABLE_MOUSE_EVENTS, mouseEnabled);
//        Log.d(TestActivity.class.getSimpleName(), "MouseEnabled="+mouseEnabled);

        boolean joystickEnabled = bundle.getBoolean(MainActivity.ENABLE_JOYSTICK_EVENTS, true);
        args.putBoolean(MainActivity.ENABLE_JOYSTICK_EVENTS, joystickEnabled);
//        Log.d(TestActivity.class.getSimpleName(), "JoystickEnabled="+joystickEnabled);

        boolean keyEnabled = bundle.getBoolean(MainActivity.ENABLE_KEY_EVENTS, true);
        args.putBoolean(MainActivity.ENABLE_KEY_EVENTS, keyEnabled);
//        Log.d(TestActivity.class.getSimpleName(), "KeyEnabled="+keyEnabled);

        boolean verboseLogging = bundle.getBoolean(MainActivity.VERBOSE_LOGGING, true);
        args.putBoolean(MainActivity.VERBOSE_LOGGING, verboseLogging);
//        Log.d(TestActivity.class.getSimpleName(), "VerboseLogging="+verboseLogging);

        fragment.setArguments(args);


        FragmentTransaction transaction = getFragmentManager().beginTransaction();

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack so the user can navigate back
        transaction.add(R.id.fragmentContainer, fragment);
        transaction.addToBackStack(null);

        // Commit the transaction
        transaction.commit();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.test_menu_items, menu);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {
        MenuItem item;

        item = menu.findItem(R.id.optionToggleKeyboard);
        if (item != null) {
//            Log.d(this.getClass().getSimpleName(), "Found ToggleKeyboard menu item");
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.optionToggleKeyboard:
                toggleKeyboard(true);
//                Log.d(this.getClass().getSimpleName(), "showing soft keyboard");
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;

    }

    private void toggleKeyboard(final boolean show) {
        fragment.getView().getHandler().post(new Runnable() {

            @Override
            public void run() {
                JmeSystem.showSoftKeyboard(show);
            }
        });

    }


}

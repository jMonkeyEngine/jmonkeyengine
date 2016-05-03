package org.jmonkeyengine.jme3androidexamples;

import android.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class TestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        JmeFragment fragment = new JmeFragment();
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
}

package org.jmonkeyengine.jme3androidexamples;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import com.jme3.system.JmeSystem;

public class TestActivity extends FragmentActivity {
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

        boolean verboseLogging = bundle.getBoolean(MainActivity.VERBOSE_LOGGING,
                MainActivity.DEFAULT_VERBOSE_LOGGING);
        args.putBoolean(MainActivity.VERBOSE_LOGGING, verboseLogging);
//        Log.d(TestActivity.class.getSimpleName(), "VerboseLogging="+verboseLogging);

        fragment.setArguments(args);


        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

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
        if (item.getItemId() == R.id.optionToggleKeyboard) {
            toggleKeyboard(true);
//            Log.d(this.getClass().getSimpleName(), "showing soft keyboard");
        } else {
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

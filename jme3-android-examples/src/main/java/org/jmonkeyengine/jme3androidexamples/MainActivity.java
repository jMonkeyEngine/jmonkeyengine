package org.jmonkeyengine.jme3androidexamples;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import com.jme3.app.Application;
import dalvik.system.DexFile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

//TODO:  Create onscreen virtual keypad for triggering normal mapped keys used by test apps or modify test apps for touch with onscreen keypad

/**
 * Main Activity started by the application.  Users select different jME3 test
 * applications that are started via TestsHarness Activity.
 * @author iwgeric
 */
public class MainActivity extends AppCompatActivity implements OnItemClickListener, View.OnClickListener, TextWatcher {
    private static final String TAG = "MainActivity";

    /**
     * Static String to pass the key for the selected test app to the
     * TestsHarness class to start the application. Also used to store the
     * current selection to the savedInstanceState Bundle.
     */
    public static final String SELECTED_APP_CLASS = "Selected_App_Class";

    /**
     * Static String to pass the key for the selected list position to the
     * savedInstanceState Bundle so the list position can be restored after
     * exiting the test application.
     */
    public static final String SELECTED_LIST_POSITION = "Selected_List_Position";

    /**
     * Static String to pass the key for the setting for enabling mouse events to the
     * savedInstanceState Bundle.
     */
    public static final String ENABLE_MOUSE_EVENTS = "Enable_Mouse_Events";

    /**
     * Static String to pass the key for the setting for enabling joystick events to the
     * savedInstanceState Bundle.
     */
    public static final String ENABLE_JOYSTICK_EVENTS = "Enable_Joystick_Events";

    /**
     * Static String to pass the key for the setting for enabling key events to the
     * savedInstanceState Bundle.
     */
    public static final String ENABLE_KEY_EVENTS = "Enable_Key_Events";

    /**
     * Static String to pass the key for the setting for verbose logging to the
     * savedInstanceState Bundle.
     */
    public static final String VERBOSE_LOGGING = "Verbose_Logging";

    /* Fields to contain the current position and display contents of the spinner */
    private int currentPosition = 0;
    private String currentSelection = "";
    private List<String> classNames = new ArrayList<>();
    private List<String> exclusions = new ArrayList<>();
    private String rootPackage;

    /* ListView that displays the test application class names. */
    private ListView listClasses;

    /* ArrayAdapter connects the spinner widget to array-based data. */
    private CustomArrayAdapter arrayAdapter;

    /* Buttons to start application or stop the activity. */
    private Button btnOK;
    private Button btnCancel;

    /* Filter Edit Box */
    EditText editFilterText;

    /* Custom settings for the test app */
    private boolean enableMouseEvents = true;
    private boolean enableJoystickEvents = false;
    private boolean enableKeyEvents = true;
    private boolean verboseLogging = false;


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            Log.d(TAG, "Restoring selections in onCreate: "
                    + "position: " + savedInstanceState.getInt(SELECTED_LIST_POSITION, 0)
                    + "class: " + savedInstanceState.getString(SELECTED_APP_CLASS)
            );
            currentPosition = savedInstanceState.getInt(SELECTED_LIST_POSITION, 0);
            currentSelection = savedInstanceState.getString(SELECTED_APP_CLASS);
            enableMouseEvents = savedInstanceState.getBoolean(ENABLE_MOUSE_EVENTS, true);
            enableJoystickEvents = savedInstanceState.getBoolean(ENABLE_JOYSTICK_EVENTS, false);
            enableKeyEvents = savedInstanceState.getBoolean(ENABLE_KEY_EVENTS, true);
            verboseLogging = savedInstanceState.getBoolean(VERBOSE_LOGGING, true);
        }


        /* Set content view and register views */
        setContentView(R.layout.test_chooser_layout);
        btnOK = (Button) findViewById(R.id.btnOK);
        btnCancel = (Button) findViewById(R.id.btnCancel);
        listClasses = (ListView) findViewById(R.id.listClasses);
        editFilterText = (EditText) findViewById(R.id.txtFilter);


        /* Define the root package to start with */
        rootPackage = "jme3test";

        /* Create an array of Strings to define which classes to exclude */
        exclusions.add("$");  // inner classes
        exclusions.add("TestChooser");  // Desktop test chooser class
        exclusions.add("awt");  // Desktop test chooser class

//        mExclusions.add("");

        /*
         * Read the class names from the dex file and filter based on
         * name and super class.
         */

        Log.d(TAG, "Composing Test list...");

        ApplicationInfo ai = this.getApplicationInfo();
        String classPath = ai.sourceDir;
        DexFile dex = null;
        Enumeration<String> apkClassNames = null;
        try {
            dex = new DexFile(classPath);
            apkClassNames = dex.entries();
            while (apkClassNames.hasMoreElements()) {
                String className = apkClassNames.nextElement();
                if (checkClassName(className) && checkClassType(className)) {
                    classNames.add(className);
                }
//            	classNames.add(className);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                dex.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /*
         * Create a backing Adapter for the List View from a list of the
         * classes. The list is defined by array of class names.
         */
        arrayAdapter = new CustomArrayAdapter(
                this,
                R.layout.test_chooser_row, // text view to display selection
                classNames // array of strings to display
        );

        /* Set the resource id for selected and non selected backgrounds */
        Log.d(TAG, "Setting Adapter Background Resource IDs");
        arrayAdapter.setSelectedBackgroundResource(R.drawable.selected);
        arrayAdapter.setNonSelectedBackgroundResource(R.drawable.nonselected);

        /* Attach the Adapter to the spinner */
        Log.d(TAG, "Setting ListView Adapter");
        listClasses.setAdapter(arrayAdapter);

        /* Set initial selection for the list */
        setSelection(currentPosition);

        /* Set Click and Text Changed listeners */
        listClasses.setOnItemClickListener(this);
        btnOK.setOnClickListener(this);
        btnCancel.setOnClickListener(this);
        editFilterText.addTextChangedListener(this);

    }

    /**
     * User selected an application.  Sets the current selection and redraws
     * the list view to highlight the selected item.
     * @param parent AdapterView tied to the list
     * @param view The ListView
     * @param position Selection position in the list of class names
     * @param id
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        setSelection(position);
    }

    /**
     * User clicked a view on the screen.  Check for the OK and Cancel buttons
     * and either start the application or exit.
     * @param view
     */
    public void onClick(View view) {
        if (view.equals(btnOK)) {
            /* Get selected class, pack it in the intent and start the test app */
            Log.d(TAG, "User selected OK for class: " + currentSelection);
            Intent intent = new Intent(this, TestActivity.class);
//            intent.putExtra(SELECTED_APP_CLASS, currentSelection);
//            intent.putExtra(ENABLE_MOUSE_EVENTS, enableMouseEvents);
//            intent.putExtra(ENABLE_JOYSTICK_EVENTS, enableJoystickEvents);
//            intent.putExtra(ENABLE_KEY_EVENTS, enableKeyEvents);

            Bundle args = new Bundle();

            args.putString(MainActivity.SELECTED_APP_CLASS, currentSelection);
//            Log.d(this.getClass().getSimpleName(), "AppClass="+currentSelection);

            args.putBoolean(MainActivity.ENABLE_MOUSE_EVENTS, enableMouseEvents);
//            Log.d(TestActivity.class.getSimpleName(), "MouseEnabled="+enableMouseEvents);

            args.putBoolean(MainActivity.ENABLE_JOYSTICK_EVENTS, enableJoystickEvents);
//            Log.d(TestActivity.class.getSimpleName(), "JoystickEnabled="+enableJoystickEvents);

            args.putBoolean(MainActivity.ENABLE_KEY_EVENTS, enableKeyEvents);
//            Log.d(TestActivity.class.getSimpleName(), "KeyEnabled="+enableKeyEvents);

            args.putBoolean(MainActivity.VERBOSE_LOGGING, verboseLogging);
//            Log.d(TestActivity.class.getSimpleName(), "VerboseLogging="+verboseLogging);

            intent.putExtras(args);

            startActivity(intent);
        } else if (view.equals(btnCancel)) {
            /* Exit */
            Log.d(TAG, "User selected Cancel");
            finish();
        }
    }

    /**
     * Check class name to see if the class is in the root package and if it
     * contains any of the exclusion strings
     * @param className Class name to check
     * @return true if the check passes, false otherwise
     */
    private boolean checkClassName(String className) {
        boolean include = true;
        /* check to see if the class in inside the rootPackage package */
        if (className.startsWith(rootPackage)) {
            /* check to see if the class contains any of the exclusion strings */
            for (int i = 0; i < exclusions.size(); i++) {
                if (className.contains(exclusions.get(i))) {
                    Log.d(TAG, "Skipping Class " + className + ". Includes exclusion string: " + exclusions.get(i) + ".");
                    include = false;
                    break;
                }
            }
        } else {
            include = false;
            Log.d(TAG, "Skipping Class " + className + ". Not in the root package: " + rootPackage + ".");
        }
        return include;
    }

    /**
     * Check to see if the class extends Application or SimpleApplication
     * @param className Class name to check
     * @return true if the check passes, false otherwise
     */
    private boolean checkClassType(String className) {
        boolean include = true;
        try {
            Class<?> clazz = Class.forName(className);
            if (Application.class.isAssignableFrom(clazz)) {
                Log.d(TAG, "Class " + className + " is a jME Application");
            } else {
                include = false;
                Log.d(TAG, "Skipping Class " + className + ". Not a jME Application");
            }

        } catch (NoClassDefFoundError ncdf) {
            include = false;
            Log.d(TAG, "Skipping Class " + className + ". No Class Def found.");
        } catch (ClassNotFoundException cnfe) {
            include = false;
            Log.d(TAG, "Skipping Class " + className + ". Class not found.");
        }
        return include;
    }

    private void setSelection(int position) {
        if (position == -1) {
            arrayAdapter.setSelectedPosition(-1);
            currentPosition = -1;
            currentSelection = "";
            btnOK.setEnabled(false);
            listClasses.invalidateViews();
        } else {
            arrayAdapter.setSelectedPosition(position);
            currentPosition = position;
            currentSelection = arrayAdapter.getItem(position);
            btnOK.setEnabled(true);
            listClasses.invalidateViews();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        Log.d(TAG, "Saving selections in onSaveInstanceState: "
                + "position: " + currentPosition + ", "
                + "class: " + currentSelection + ", "
                + "mouseEvents: " + enableMouseEvents + ", "
                + "joystickEvents: " + enableJoystickEvents + ", "
                + "keyEvents: " + enableKeyEvents + ", "
                + "VerboseLogging: " + verboseLogging + ", "
        );
        // Save current selections to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.
        savedInstanceState.putString(SELECTED_APP_CLASS, currentSelection);
        savedInstanceState.putInt(SELECTED_LIST_POSITION, currentPosition);
        savedInstanceState.putBoolean(ENABLE_MOUSE_EVENTS, enableMouseEvents);
        savedInstanceState.putBoolean(ENABLE_JOYSTICK_EVENTS, enableJoystickEvents);
        savedInstanceState.putBoolean(ENABLE_KEY_EVENTS, enableKeyEvents);
        savedInstanceState.putBoolean(VERBOSE_LOGGING, verboseLogging);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
//        Log.i(TAG, "Restoring selections in onRestoreInstanceState: "
//                + "position: " + savedInstanceState.getInt(SELECTED_LIST_POSITION, 0)
//                + "class: " + savedInstanceState.getString(SELECTED_APP_CLASS)
//                );
//        //Restore selections from the savedInstanceState.
//        // This bundle has also been passed to onCreate.
//        currentPosition = savedInstanceState.getInt(SELECTED_LIST_POSITION, 0);
//        currentSelection = savedInstanceState.getString(SELECTED_APP_CLASS);
    }

    public void beforeTextChanged(CharSequence cs, int i, int i1, int i2) {
    }

    public void onTextChanged(CharSequence cs, int startPos, int beforePos, int count) {
        Log.d(TAG, "onTextChanged with cs: " + cs + ", startPos: " + startPos + ", beforePos: " + beforePos + ", count: " + count);
        arrayAdapter.getFilter().filter(cs.toString());
        setSelection(-1);
    }

    public void afterTextChanged(Editable editable) {
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        editFilterText.removeTextChangedListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_items, menu);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {
        MenuItem item;

        item = menu.findItem(R.id.optionMouseEvents);
        if (item != null) {
            Log.d(TAG, "Found EnableMouseEvents menu item");
            if (enableMouseEvents) {
                item.setTitle(R.string.strOptionDisableMouseEventsTitle);
            } else {
                item.setTitle(R.string.strOptionEnableMouseEventsTitle);
            }
        }

        item = menu.findItem(R.id.optionJoystickEvents);
        if (item != null) {
            Log.d(TAG, "Found EnableJoystickEvents menu item");
            if (enableJoystickEvents) {
                item.setTitle(R.string.strOptionDisableJoystickEventsTitle);
            } else {
                item.setTitle(R.string.strOptionEnableJoystickEventsTitle);
            }
        }

        item = menu.findItem(R.id.optionKeyEvents);
        if (item != null) {
            Log.d(TAG, "Found EnableKeyEvents menu item");
            if (enableKeyEvents) {
                item.setTitle(R.string.strOptionDisableKeyEventsTitle);
            } else {
                item.setTitle(R.string.strOptionEnableKeyEventsTitle);
            }
        }

        item = menu.findItem(R.id.optionVerboseLogging);
        if (item != null) {
            Log.d(TAG, "Found EnableVerboseLogging menu item");
            if (verboseLogging) {
                item.setTitle(R.string.strOptionDisableVerboseLoggingTitle);
            } else {
                item.setTitle(R.string.strOptionEnableVerboseLoggingTitle);
            }
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.optionMouseEvents:
                enableMouseEvents = !enableMouseEvents;
                Log.d(TAG, "enableMouseEvents set to: " + enableMouseEvents);
                break;
            case R.id.optionJoystickEvents:
                enableJoystickEvents = !enableJoystickEvents;
                Log.d(TAG, "enableJoystickEvents set to: " + enableJoystickEvents);
                break;
            case R.id.optionKeyEvents:
                enableKeyEvents = !enableKeyEvents;
                Log.d(TAG, "enableKeyEvents set to: " + enableKeyEvents);
                break;
            case R.id.optionVerboseLogging:
                verboseLogging = !verboseLogging;
                Log.d(TAG, "verboseLogging set to: " + verboseLogging);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;

    }

}
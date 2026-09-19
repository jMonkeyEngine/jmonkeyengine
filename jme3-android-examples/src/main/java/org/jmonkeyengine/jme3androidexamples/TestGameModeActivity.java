package org.jmonkeyengine.jme3androidexamples;

import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import androidx.fragment.app.FragmentActivity;
import com.jme3.app.LegacyApplication;
import com.jme3.system.android.GameMode;
import com.jme3.system.android.OnGameModeChanged;
import com.jme3.view.surfaceview.JmeSurfaceView;

/**
 * Example and verification Activity reporting the Android Game Mode selected by the user.
 *
 * <p>It registers an {@link OnGameModeChanged} listener on the {@link JmeSurfaceView} and
 * logs every game mode change. The same listener is available on
 * {@code com.jme3.app.AndroidHarnessFragment}.</p>
 *
 * <p>The platform only reports a game mode on Android 12 and newer, and only for
 * applications it treats as games; everywhere else the listener is notified once with
 * {@link GameMode#UNSUPPORTED}. See the Android documentation for the
 * <a href="https://developer.android.com/games/gamemode/about-API-and-interventions">Game Mode API</a>.</p>
 *
 * <p>Launch it for example with:
 * {@code adb shell am start -n org.jmonkeyengine.jme3androidexamples/.TestGameModeActivity}
 * and watch the output with {@code adb logcat -s TestGameModeActivity}. Add
 * {@code --es Selected_App_Class <class>} to host a different jME application.</p>
 *
 * @see GameMode
 * @see OnGameModeChanged
 * @see JmeSurfaceView#setOnGameModeChanged(OnGameModeChanged)
 */
@SuppressWarnings("deprecation")
public class TestGameModeActivity extends FragmentActivity {

    /**
     * Key of the intent extra selecting the jME application to host. It mirrors
     * {@code MainActivity.SELECTED_APP_CLASS}.
     */
    private static final String SELECTED_APP_CLASS = "Selected_App_Class";

    private static final String TAG = "TestGameModeActivity";
    private static final String DEFAULT_APP_CLASS = "jme3test.android.TestAndroidSensors";

    private JmeSurfaceView jmeSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);

        jmeSurfaceView = new JmeSurfaceView(this);
        jmeSurfaceView.setOnGameModeChanged(new OnGameModeChanged() {
            @Override
            public void onGameModeChanged(GameMode gameMode) {
                /*
                 * The callback is invoked with the current mode as soon as the listener
                 * is registered and afterwards on every change made in the system game
                 * settings. Applications can implement their own logic here, for example
                 * altering the level of detail, loading lower-poly models, changing the
                 * frame rate or disabling filters.
                 */
                System.out.println("Game mode changed to: " + gameMode);
                Log.i(TAG, "Game mode changed to: " + gameMode);
                switch (gameMode) {
                    case PERFORMANCE:
                        // Favor visual quality, for example a higher frame rate.
                        break;
                    case BATTERY:
                        // Save power, for example a lower frame rate and no filters.
                        break;
                    case STANDARD:
                        // Use the regular, balanced settings.
                        break;
                    case UNSUPPORTED:
                    default:
                        // The Game Mode API is not available on this device.
                        break;
                }
            }
        });

        String appClass = DEFAULT_APP_CLASS;
        Bundle bundle = getIntent().getExtras();
        if (bundle != null && bundle.containsKey(SELECTED_APP_CLASS)) {
            appClass = bundle.getString(SELECTED_APP_CLASS);
        }

        try {
            Class<?> clazz = Class.forName(appClass);
            LegacyApplication app = (LegacyApplication) clazz.getDeclaredConstructor().newInstance();
            jmeSurfaceView.setLegacyApplication(app);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        getLifecycle().addObserver(jmeSurfaceView);

        RelativeLayout layout = new RelativeLayout(this);
        layout.addView(jmeSurfaceView);
        setContentView(layout);

        jmeSurfaceView.startRenderer(0);
    }
}

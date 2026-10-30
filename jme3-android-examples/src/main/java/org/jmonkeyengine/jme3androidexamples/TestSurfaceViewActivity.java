package org.jmonkeyengine.jme3androidexamples;

import android.os.Bundle;
import android.widget.RelativeLayout;
import androidx.fragment.app.FragmentActivity;
import com.jme3.app.LegacyApplication;
import com.jme3.system.AppSettings;
import com.jme3.view.surfaceview.JmeSurfaceView;
import com.jme3.view.surfaceview.OnExceptionThrown;
import com.jme3.view.surfaceview.OnLayoutDrawn;
import com.jme3.view.surfaceview.OnRendererCompleted;
import com.jme3.view.surfaceview.OnRendererStarted;

/**
 * Example and verification Activity embedding JmeSurfaceView.
 */
public class TestSurfaceViewActivity extends FragmentActivity {

    private JmeSurfaceView jmeSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
                android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);

        RelativeLayout layout = new RelativeLayout(this);
        jmeSurfaceView = new JmeSurfaceView(this);

        String appClass = "jme3test.android.TestAndroidSensors";
        Bundle bundle = getIntent().getExtras();
        if (bundle != null && bundle.containsKey(MainActivity.SELECTED_APP_CLASS)) {
            appClass = bundle.getString(MainActivity.SELECTED_APP_CLASS);
        }

        try {
            Class<?> clazz = Class.forName(appClass);
            LegacyApplication app = (LegacyApplication) clazz.getDeclaredConstructor().newInstance();
            jmeSurfaceView.setLegacyApplication(app);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        jmeSurfaceView.setOnRendererStarted(new OnRendererStarted() {
            @Override
            public void onRenderStart(LegacyApplication application, android.view.View layout) {
            }
        });

        jmeSurfaceView.setOnRendererCompleted(new OnRendererCompleted() {
            @Override
            public void onRenderCompletion(LegacyApplication application, AppSettings appSettings) {
            }
        });

        jmeSurfaceView.setOnLayoutDrawn(new OnLayoutDrawn() {
            @Override
            public void onLayoutDrawn(LegacyApplication application, android.view.View layout) {
            }
        });

        jmeSurfaceView.setOnExceptionThrown(new OnExceptionThrown() {
            @Override
            public void onExceptionThrown(Throwable e) {
            }
        });

        getLifecycle().addObserver(jmeSurfaceView);

        layout.addView(jmeSurfaceView);
        setContentView(layout);

        jmeSurfaceView.startRenderer(0);
    }
}

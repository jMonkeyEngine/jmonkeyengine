package org.jmonkeyengine.screenshottests.android.android;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.View;

@RunWith(AndroidJUnit4.class)
public class ScreenshotTest {

    @Rule
    public ActivityScenarioRule<AndroidLauncher> activityRule =
            new ActivityScenarioRule<>(AndroidLauncher.class);

    @Test
    public void takeScreenshot() {
        System.out.println("Starting test");

        // Wait a bit for the app to initialize and render the blue box
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        activityRule.getScenario().onActivity(new ActivityScenario.ActivityAction<AndroidLauncher>() {
            @Override
            public void perform(AndroidLauncher activity) {
                System.out.println("Within activity");
                View view = activity.getWindow().getDecorView().getRootView();
                view.setDrawingCacheEnabled(true);
                Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
                view.setDrawingCacheEnabled(false);

                File storageDir = activity.getExternalFilesDir(null);
                File screenshotFile = new File(storageDir, "screenshot.png");

                try (FileOutputStream out = new FileOutputStream(screenshotFile)) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                    System.out.println("Screenshot saved to: " + screenshotFile.getAbsolutePath());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                System.out.println("Storage dir: " + storageDir.getAbsolutePath());
                System.out.println("Screenshot file: " + screenshotFile.getAbsolutePath());
            }
        });
    }
}

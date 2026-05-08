package org.jmonkeyengine.screenshottests.android.android;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.graphics.Bitmap;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.PixelCopy;
import android.view.View;

@RunWith(AndroidJUnit4.class)
public class ScreenshotTest {

    @Rule
    public ActivityScenarioRule<AndroidLauncher> activityRule =
            new ActivityScenarioRule<>(AndroidLauncher.class);

    @Test
    public void takeScreenshot() {
        System.out.println("Starting test");
        Log.i("SCREENSHOT_TEST", "Starting test");
        // Wait a bit for the app to initialize and render the blue box
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        final CountDownLatch latch = new CountDownLatch(1);

        activityRule.getScenario().onActivity(new ActivityScenario.ActivityAction<AndroidLauncher>() {
            @Override
            public void perform(AndroidLauncher activity) {
                View view = activity.getWindow().getDecorView().getRootView();
                // Bitmap.createBitmap(view.getDrawingCache()) was deprecated
                Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);

                final HandlerThread handlerThread = new HandlerThread("PixelCopyThread");
                handlerThread.start();

                PixelCopy.request(activity.getWindow(), bitmap, new PixelCopy.OnPixelCopyFinishedListener() {
                    @Override
                    public void onPixelCopyFinished(int copyResult) {
                        try {
                            if (copyResult == PixelCopy.SUCCESS) {

                                File publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                                File screenshotFile = new File(publicDir, "screenshot.png");

                                Log.i("SCREENSHOT_TEST", "Storage dir: " + publicDir.getAbsolutePath());
                                Log.i("SCREENSHOT_TEST", "Screenshot file: " + screenshotFile.getAbsolutePath());

                                try (FileOutputStream out = new FileOutputStream(screenshotFile)) {
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                                    System.out.println("Screenshot saved to: " + screenshotFile.getAbsolutePath());
                                    Log.i("SCREENSHOT_TEST", "Screenshot saved to: " + screenshotFile.getAbsolutePath());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                System.err.println("PixelCopy failed with result: " + copyResult);
                                Log.i("SCREENSHOT_TEST", "PixelCopy failed with result: " + copyResult);
                            }
                        }finally {
                            handlerThread.quitSafely();
                            latch.countDown();
                        }
                    }
                }, new Handler(handlerThread.getLooper()));
            }
        });

        // Wait a bit for PixelCopy to finish since it's asynchronous
        boolean completed = false; // <-- Wait here
        try {
            completed = latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (!completed) {
            throw new RuntimeException("PixelCopy did not complete within 10 seconds");
        }
    }
}

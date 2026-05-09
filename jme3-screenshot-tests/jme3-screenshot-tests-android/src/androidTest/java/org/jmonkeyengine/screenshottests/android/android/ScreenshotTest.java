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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
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

        // Wait for the app to initialize
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        final CountDownLatch latch = new CountDownLatch(1);

        activityRule.getScenario().onActivity(new ActivityScenario.ActivityAction<AndroidLauncher>() {
            @Override
            public void perform(AndroidLauncher activity) {
                // Get the GLSurfaceView
                GLSurfaceView glSurfaceView = activity.getGLSurfaceView();
                if (glSurfaceView == null) {
                    Log.e("SCREENSHOT_TEST", "GLSurfaceView is null!");
                    latch.countDown();
                    return;
                }


                final int width = glSurfaceView.getWidth();
                final int height = glSurfaceView.getHeight();
                final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

                // Queue the screenshot capture on the GL thread
                glSurfaceView.queueEvent(() -> {
                    try {
                        // Ensure a frame is rendered
                        //glSurfaceView.requestRender();
                        //Thread.sleep(16); // Wait for a frame (~60fps)

                        // Allocate a buffer for the pixels
                        ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4);
                        buffer.order(ByteOrder.LITTLE_ENDIAN);

                        // Read pixels from the framebuffer
                        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
                        buffer.rewind();

                        // Copy the buffer to the bitmap
                        bitmap.copyPixelsFromBuffer(buffer);

                        // Save the bitmap
                        File saveDir = activity.getExternalFilesDir(null);
                        File screenshotFile = new File(saveDir, "screenshot.png");

                        try (FileOutputStream out = new FileOutputStream(screenshotFile)) {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                            Log.i("SCREENSHOT_TEST", "Screenshot saved to: " + screenshotFile.getAbsolutePath());
                        } catch (IOException e) {
                            Log.e("SCREENSHOT_TEST", "Failed to save screenshot", e);
                        }
                    } catch (Exception e) {
                        Log.e("SCREENSHOT_TEST", "Failed to capture GL content", e);
                    } finally {
                        latch.countDown();
                    }
                });
            }
        });

        // Wait for the snapshot to complete
        boolean completed;
        try {
            completed = latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (!completed) {
            throw new RuntimeException("Screenshot capture did not complete within 10 seconds");
        }
    }
}

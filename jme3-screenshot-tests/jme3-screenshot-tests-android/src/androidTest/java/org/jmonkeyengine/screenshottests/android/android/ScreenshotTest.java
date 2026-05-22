package org.jmonkeyengine.screenshottests.android.android;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

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
import android.util.Log;

@RunWith(AndroidJUnit4.class)
public class ScreenshotTest extends ScreenshotTestAndroidBase {

    @Test
    public void takeScreenshot() {
        System.out.println("Starting test");
        Log.i("SCREENSHOT_TEST", "Starting test");


        try (FragmentScenario<AndroidLauncher> scenario = FragmentScenario.launchInContainer(AndroidLauncher.class)) {
            // Wait for the app to initialize

            final CountDownLatch latch = new CountDownLatch(1);

            scenario.onFragment(fragment -> {
                // Get the GLSurfaceView from the fragment
                GLSurfaceView glSurfaceView = fragment.getGLSurfaceView();
                if (glSurfaceView == null) {
                    Log.e("SCREENSHOT_TEST", "GLSurfaceView is null!");
                    latch.countDown();
                    return;
                }

                // ... (rest of the screenshot logic remains similar)
                final int width = glSurfaceView.getWidth();
                final int height = glSurfaceView.getHeight();
                if (width <= 0 || height <= 0) {
                    latch.countDown();
                    return;
                }

                final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

                glSurfaceView.queueEvent(() -> {
                    try {
                        ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4);
                        buffer.order(ByteOrder.LITTLE_ENDIAN);
                        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
                        buffer.rewind();
                        bitmap.copyPixelsFromBuffer(buffer);

                        File saveDir = fragment.requireContext().getExternalFilesDir(null);
                        File screenshotFile = new File(saveDir, "screenshot.png");

                        try (FileOutputStream out = new FileOutputStream(screenshotFile)) {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                            Log.i("SCREENSHOT_TEST", "Screenshot saved to: " + screenshotFile.getAbsolutePath());
                        } catch (IOException e) {
                            Log.e("SCREENSHOT_TEST", "Failed to save screenshot", e);
                        }
                    } finally {
                        latch.countDown();
                    }
                });
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
}

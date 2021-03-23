package com.jme3.util;

import android.graphics.Bitmap;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

public final class AndroidScreenshots {

    private static final Logger logger = Logger.getLogger(AndroidScreenshots.class.getName());

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private AndroidScreenshots() {
    }

    /**
     * Convert OpenGL GLES20.GL_RGBA to Bitmap.Config.ARGB_8888 and store result
     * in a Bitmap
     *
     * @param buf ByteBuffer that has the pixel color data from OpenGL
     * @param bitmapImage Bitmap to be used after converting the data
     */
    public static void convertScreenShot(ByteBuffer buf, Bitmap bitmapImage) {
        int width = bitmapImage.getWidth();
        int height = bitmapImage.getHeight();
        int size = width * height;

        // Grab data from ByteBuffer as Int Array to manipulate data and send to image
        int[] data = new int[size];
        buf.asIntBuffer().get(data);

        // convert from GLES20.GL_RGBA to Bitmap.Config.ARGB_8888
        // ** need to swap RED and BLUE **
        for (int idx = 0; idx < data.length; idx++) {
            int initial = data[idx];
            int pb = (initial >> 16) & 0xff;
            int pr = (initial << 16) & 0x00ff0000;
            int pix1 = (initial & 0xff00ff00) | pr | pb;
            data[idx] = pix1;
        }

        // OpenGL and Bitmap have opposite starting points for Y axis (top vs bottom)
        // Need to write the data in the image from the bottom to the top
        // Use size-width to indicate start with last row and increment by -width for each row
        bitmapImage.setPixels(data, size - width, -width, 0, 0, width, height);
    }
}
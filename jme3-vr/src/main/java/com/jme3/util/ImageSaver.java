package com.jme3.util;

import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_PACK_ALIGNMENT;
import static org.lwjgl.opengl.GL11.glPixelStorei;
import static org.lwjgl.system.MemoryUtil.memAlloc;

public class ImageSaver {

    private int floatsPerPixel = 3;
    private int bytesPerFloat = 1;
    private int width = 1512;
    private int height = 1680;
    private int size = width * height * floatsPerPixel * bytesPerFloat;
    private ByteBuffer texSaveBuffer = memAlloc(size);
    private IntBuffer wBuf = BufferUtils.createIntBuffer(1);
    private IntBuffer hBuf = BufferUtils.createIntBuffer(1);

    public void saveTextureToFile(long textureID, String filename) {
        // TODO: Can we do without the cast?
        glBindTexture(GL_TEXTURE_2D, (int) textureID);
        glPixelStorei(GL_PACK_ALIGNMENT, 1);
        int level = 0;

        glGetTexLevelParameteriv(GL_TEXTURE_2D, level, GL_TEXTURE_WIDTH, wBuf);
        glGetTexLevelParameteriv(GL_TEXTURE_2D, level, GL_TEXTURE_HEIGHT, hBuf);

        int w = wBuf.get();
        int h = hBuf.get();
        assert (w == width);
        assert (h == height);

        glGetTexImage(GL_TEXTURE_2D, level, GL_RGB
                , GL_UNSIGNED_BYTE, texSaveBuffer);
        glPixelStorei(GL_PACK_ALIGNMENT, 4);

        DataBuffer imageData = new DataBufferByte(size);
        for (int i = 0; i < size; i++) {
            imageData.setElem(i, texSaveBuffer.get(i));
        }

        //3 bytes per pixel: red, green, blue
          WritableRaster raster = Raster.createInterleavedRaster(imageData, width, height, 3 * width, 3, new int[] {0, 1, 2}, (Point) null);
//        WritableRaster raster = Raster.createInterleavedRaster(imageData, width, height, bytesPerFloat * floatsPerPixel * width, 16, new int[]{0, 4, 8, 12}, (Point) null);
        ColorModel cm = new ComponentColorModel(ColorModel.getRGBdefault().getColorSpace(), false, true, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
        BufferedImage image = new BufferedImage(cm, raster, true, null);
        try {
            ImageIO.write(image, "png", new File(filename));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            hBuf.clear();
            wBuf.clear();
            texSaveBuffer.clear();
        }
    }
}

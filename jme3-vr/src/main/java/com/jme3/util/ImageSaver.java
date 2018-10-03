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
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_PACK_ALIGNMENT;
import static org.lwjgl.opengl.GL11.glPixelStorei;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_DEPTH;
import static org.lwjgl.opengl.GL13.GL_TEXTURE_COMPRESSED;
import static org.lwjgl.opengl.GL13.GL_TEXTURE_COMPRESSED_IMAGE_SIZE;
import static org.lwjgl.opengl.GL14.GL_TEXTURE_DEPTH_SIZE;
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

    IntBuffer paramBuffer = BufferUtils.createIntBuffer(1);
    public void getTextureInfo(long handle) {
        glBindTexture(GL_TEXTURE_2D, (int) handle);

        HashMap<String, Integer> h = new HashMap<String, Integer>();
        h.put("GL_TEXTURE_ALPHA_SIZE"            ,  GL_TEXTURE_ALPHA_SIZE);
        h.put("GL_TEXTURE_BLUE_SIZE"             ,  GL_TEXTURE_BLUE_SIZE);
        h.put("GL_TEXTURE_BORDER"                ,  GL_TEXTURE_BORDER);
        h.put("GL_TEXTURE_COMPRESSED"            ,  GL_TEXTURE_COMPRESSED);
        h.put("GL_TEXTURE_COMPRESSED_IMAGE_SIZE" ,  GL_TEXTURE_COMPRESSED_IMAGE_SIZE);
        h.put("GL_TEXTURE_DEPTH"                 ,  GL_TEXTURE_DEPTH);
        h.put("GL_TEXTURE_DEPTH_SIZE"            ,  GL_TEXTURE_DEPTH_SIZE);
        h.put("GL_TEXTURE_GREEN_SIZE"            ,  GL_TEXTURE_GREEN_SIZE);
        h.put("GL_TEXTURE_HEIGHT"                ,  GL_TEXTURE_HEIGHT);
        h.put("GL_TEXTURE_INTENSITY_SIZE"        ,  GL_TEXTURE_INTENSITY_SIZE);
        h.put("GL_TEXTURE_INTERNAL_FORMAT"       ,  GL_TEXTURE_INTERNAL_FORMAT);
        h.put("GL_TEXTURE_LUMINANCE_SIZE"        ,  GL_TEXTURE_LUMINANCE_SIZE);
        h.put("GL_TEXTURE_RED_SIZE"              ,  GL_TEXTURE_RED_SIZE);
        h.put("GL_TEXTURE_WIDTH"                 ,  GL_TEXTURE_WIDTH);

        for(Map.Entry<String, Integer> entry : h.entrySet()) {
            String name  = entry.getKey();
            int glAttrib = entry.getValue();
            glGetTexLevelParameteriv(GL_TEXTURE_2D, 0, glAttrib, paramBuffer);

            System.out.println(name + ": " + paramBuffer.get());

            paramBuffer.clear();
        }

        glBindTexture(GL_TEXTURE_2D, 0);
    }
}

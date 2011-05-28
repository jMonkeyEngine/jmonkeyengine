package com.jme3.terrain;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import org.novyon.noise.ShaderUtils;

public class MapUtils {

	public static FloatBuffer clip(FloatBuffer src, int origSize, int newSize, int offset) {
		FloatBuffer result = FloatBuffer.allocate(newSize * newSize);

		float[] orig = src.array();
		for (int i = offset; i < offset + newSize; i++) {
			result.put(orig, i * origSize + offset, newSize);
		}

		return result;
	}

	public static BufferedImage toGrayscale16Image(FloatBuffer buff, int size) {
		BufferedImage retval = new BufferedImage(size, size, BufferedImage.TYPE_USHORT_GRAY);
		buff.rewind();
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				short c = (short) (ShaderUtils.clamp(buff.get(), 0, 1) * 65532);
				retval.getRaster().setDataElements(x, y, new short[] { c });
			}
		}
		return retval;
	}

	public static BufferedImage toGrayscaleRGBImage(FloatBuffer buff, int size) {
		BufferedImage retval = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		buff.rewind();
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				int c = (int) (ShaderUtils.clamp(buff.get(), 0, 1) * 255);
				retval.setRGB(x, y, 0xFF000000 | c << 16 | c << 8 | c);
			}
		}
		return retval;
	}

	public static void saveImage(BufferedImage im, String file) {
		MapUtils.saveImage(im, new File(file));
	}

	public static void saveImage(BufferedImage im, File file) {
		try {
			ImageIO.write(im, "PNG", file);
			Logger.getLogger(MapUtils.class.getCanonicalName()).info("Saved image as : " + file.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

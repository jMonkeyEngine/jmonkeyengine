package com.jme3.scene.plugins.blender.textures;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.math.ColorRGBA;
import com.jme3.texture.Image.Format;

/*package*/ class TexturePixel implements Cloneable {
	private static final Logger LOGGER = Logger.getLogger(TexturePixel.class.getName());
	
	public float	intensity, red, green, blue, alpha;

	public void fromColor(ColorRGBA colorRGBA) {
		this.intensity = 0;
		this.red = colorRGBA.r;
		this.green = colorRGBA.g;
		this.blue = colorRGBA.b;
		this.alpha = colorRGBA.a;
	}
	
	public void fromImage(Format imageFormat, ByteBuffer data, int pixelIndex) {
		int firstByteIndex;
		byte pixelValue;
		switch(imageFormat) {
			case ABGR8:
				firstByteIndex = pixelIndex << 2;
				pixelValue = data.get(firstByteIndex);
				this.alpha = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - (~pixelValue) / 255.0f;
				pixelValue = data.get(firstByteIndex + 1);
				this.blue = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - (~pixelValue) / 255.0f;
				pixelValue = data.get(firstByteIndex + 2);
				this.green = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - (~pixelValue) / 255.0f;
				pixelValue = data.get(firstByteIndex + 3);
				this.red = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - (~pixelValue) / 255.0f;
				break;
			case RGBA8:
				firstByteIndex = pixelIndex << 2;
				pixelValue = data.get(firstByteIndex);
				this.red = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - (~pixelValue) / 255.0f;
				pixelValue = data.get(firstByteIndex + 1);
				this.green = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - (~pixelValue) / 255.0f;
				pixelValue = data.get(firstByteIndex + 2);
				this.blue = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - (~pixelValue) / 255.0f;
				pixelValue = data.get(firstByteIndex + 3);
				this.alpha = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - (~pixelValue) / 255.0f;
				break;
			case BGR8:
				firstByteIndex = pixelIndex * 3;
				pixelValue = data.get(firstByteIndex);
				this.blue = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - (~pixelValue) / 255.0f;
				pixelValue = data.get(firstByteIndex + 1);
				this.green = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - (~pixelValue) / 255.0f;
				pixelValue = data.get(firstByteIndex + 2);
				this.red = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - (~pixelValue) / 255.0f;
				this.alpha = 1.0f;
				break;
			case RGB8:
				firstByteIndex = pixelIndex * 3;
				pixelValue = data.get(firstByteIndex);
				this.red = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - (~pixelValue) / 255.0f;
				pixelValue = data.get(firstByteIndex + 1);
				this.green = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - (~pixelValue) / 255.0f;
				pixelValue = data.get(firstByteIndex + 2);
				this.blue = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - (~pixelValue) / 255.0f;
				this.alpha = 1.0f;
				break;
			case Luminance8:
				pixelValue = data.get(pixelIndex);
				this.intensity = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - (~pixelValue) / 255.0f;
				break;
			default:
				LOGGER.log(Level.FINEST, "Unknown type of texture: {0}. Black pixel used!", imageFormat);
				this.intensity = this.blue = this.red = this.green = this.alpha = 0.0f;
		}
	}
	
	public void merge(TexturePixel pixel) {
		float oneMinusAlpha = 1 - pixel.alpha;
		this.red = oneMinusAlpha * this.red + pixel.alpha*pixel.red;
		this.green = oneMinusAlpha * this.green + pixel.alpha*pixel.green;
		this.blue = oneMinusAlpha * this.blue + pixel.alpha*pixel.blue;
		//alpha should be always 1.0f as a result
	}
	
	public void clear() {
		this.intensity = this.blue = this.red = this.green = this.alpha = 0.0f;
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
}

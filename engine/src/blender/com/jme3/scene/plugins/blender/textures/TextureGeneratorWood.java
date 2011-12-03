/*
 *
 * $Id: noise.c 14611 2008-04-29 08:24:33Z campbellbarton $
 *
 * ***** BEGIN GPL LICENSE BLOCK *****
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * The Original Code is Copyright (C) 2001-2002 by NaN Holding BV.
 * All rights reserved.
 *
 * The Original Code is: all of this file.
 *
 * Contributor(s): none yet.
 *
 * ***** END GPL LICENSE BLOCK *****
 *
 */
package com.jme3.scene.plugins.blender.textures;

import com.jme3.math.FastMath;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture3D;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * This class generates the 'wood' texture.
 * @author Marcin Roguski (Kaelthas)
 */
public class TextureGeneratorWood extends TextureGenerator {
	// tex->noisebasis2
    protected static final int TEX_SIN = 0;
    protected static final int TEX_SAW = 1;
    protected static final int TEX_TRI = 2;
    
    // tex->stype
    protected static final int TEX_BAND = 0;
    protected static final int TEX_RING = 1;
    protected static final int TEX_BANDNOISE = 2;
    protected static final int TEX_RINGNOISE = 3;
    
    // tex->noisetype
    protected static final int TEX_NOISESOFT = 0;
    protected static final int TEX_NOISEPERL = 1;
    
	/**
	 * Constructor stores the given noise generator.
	 * @param noiseGenerator the noise generator
	 */
	public TextureGeneratorWood(NoiseGenerator noiseGenerator) {
		super(noiseGenerator);
	}

	@Override
	protected Texture generate(Structure tex, int width, int height, int depth, BlenderContext blenderContext) {
		float[] texvec = new float[] { 0, 0, 0 };
		TexturePixel texres = new TexturePixel();
		int halfW = width >> 1;
		int halfH = height >> 1;
		int halfD = depth >> 1;
		float wDelta = 1.0f / halfW, hDelta = 1.0f / halfH, dDelta = 1.0f / halfD;
		
		float[][] colorBand = this.computeColorband(tex, blenderContext);
		Format format = colorBand != null ? Format.RGBA8 : Format.Luminance8;
		int bytesPerPixel = colorBand != null ? 4 : 1;
		WoodIntensityData woodIntensityData = new WoodIntensityData(tex);
		BrightnessAndContrastData bacd = new BrightnessAndContrastData(tex);
		
		int index = 0;
		byte[] data = new byte[width * height * depth * bytesPerPixel];
		for (int i = -halfW; i < halfW; ++i) {
			texvec[0] = wDelta * i;
			for (int j = -halfH; j < halfH; ++j) {
				texvec[1] = hDelta * j;
				for(int k = -halfD; k < halfD; ++k) {
					texvec[2] = dDelta * k;
					texres.intensity = this.woodIntensity(woodIntensityData, texvec[0], texvec[1], texvec[2]);
					
					if (colorBand != null) {
						int colorbandIndex = (int) (texres.intensity * 1000.0f);
						texres.red = colorBand[colorbandIndex][0];
						texres.green = colorBand[colorbandIndex][1];
						texres.blue = colorBand[colorbandIndex][2];
						
						this.applyBrightnessAndContrast(bacd, texres);
						
						data[index++] = (byte) (texres.red * 255.0f);
						data[index++] = (byte) (texres.green * 255.0f);
						data[index++] = (byte) (texres.blue * 255.0f);
						data[index++] = (byte) (colorBand[colorbandIndex][3] * 255.0f);
					} else {
						this.applyBrightnessAndContrast(texres, bacd.contrast, bacd.brightness);
						data[index++] = (byte) (texres.intensity * 255.0f);
					}
				}
			}
		}
		
		ArrayList<ByteBuffer> dataArray = new ArrayList<ByteBuffer>(1);
		dataArray.add(BufferUtils.createByteBuffer(data));
		return new Texture3D(new Image(format, width, height, depth, dataArray));
	}
	
    protected static WaveForm[] waveformFunctions = new WaveForm[3];
    static {
        waveformFunctions[0] = new WaveForm() {// sinus (TEX_SIN)

            @Override
            public float execute(float x) {
                return 0.5f + 0.5f * (float) Math.sin(x);
            }
        };
        waveformFunctions[1] = new WaveForm() {// saw (TEX_SAW)

            @Override
            public float execute(float x) {
                int n = (int) (x * FastMath.INV_TWO_PI);
                x -= n * FastMath.TWO_PI;
                if (x < 0.0f) {
                    x += FastMath.TWO_PI;
                }
                return x * FastMath.INV_TWO_PI;
            }
        };
        waveformFunctions[2] = new WaveForm() {// triangle (TEX_TRI)

            @Override
            public float execute(float x) {
                return 1.0f - 2.0f * FastMath.abs((float) Math.floor(x * FastMath.INV_TWO_PI + 0.5f) - x * FastMath.INV_TWO_PI);
            }
        };
    }

    /**
     * Computes basic wood intensity value at x,y,z.
     * @param woodIntData
     * @param x X coordinate of the texture pixel
     * @param y Y coordinate of the texture pixel
     * @param z Z coordinate of the texture pixel
     * @return wood intensity at position [x, y, z]
     */
    public float woodIntensity(WoodIntensityData woodIntData, float x, float y, float z) {
        float result;

        switch(woodIntData.woodType) {
	        case TEX_BAND:
	        	result = woodIntData.waveformFunction.execute((x + y + z) * 10.0f);
	        	break;
	        case TEX_RING:
	        	result = woodIntData.waveformFunction.execute((float) Math.sqrt(x * x + y * y + z * z) * 20.0f);
	        	break;
	        case TEX_BANDNOISE:
	        	result = woodIntData.turbul * NoiseGenerator.NoiseFunctions.noise(x, y, z, woodIntData.noisesize, 0, woodIntData.noisebasis, woodIntData.isHard);
	            result = woodIntData.waveformFunction.execute((x + y + z) * 10.0f + result);
	        	break;
	        case TEX_RINGNOISE:
	        	result = woodIntData.turbul * NoiseGenerator.NoiseFunctions.noise(x, y, z, woodIntData.noisesize, 0, woodIntData.noisebasis, woodIntData.isHard);
	            result = woodIntData.waveformFunction.execute((float) Math.sqrt(x * x + y * y + z * z) * 20.0f + result);
	        	break;
        	default:
        		result = 0;
        }
        return result;
    }
    
    /**
     * A class that collects the data for wood intensity calculations.
     * @author Marcin Roguski (Kaelthas)
     */
    private static class WoodIntensityData {
    	public final WaveForm waveformFunction;
    	public final int noisebasis;
    	public final float noisesize;
    	public final float turbul;
    	public final int noiseType;
    	public final int woodType;
    	public final boolean isHard;
        
    	public WoodIntensityData(Structure tex) {
    		int waveform = ((Number) tex.getFieldValue("noisebasis2")).intValue();//wave form: TEX_SIN=0, TEX_SAW=1, TEX_TRI=2
    		if (waveform > TEX_TRI || waveform < TEX_SIN) {
                waveform = 0; // check to be sure noisebasis2 is initialized ahead of time
            }
    		waveformFunction = waveformFunctions[waveform];
    		noisebasis = ((Number) tex.getFieldValue("noisebasis")).intValue();
            woodType = ((Number) tex.getFieldValue("stype")).intValue();
            noisesize = ((Number) tex.getFieldValue("noisesize")).floatValue();
            turbul = ((Number) tex.getFieldValue("turbul")).floatValue();
            noiseType = ((Number) tex.getFieldValue("noisetype")).intValue();
            isHard = noiseType != TEX_NOISESOFT;
		}
    }
    
    protected static interface WaveForm {

        float execute(float x);
    }
}

/*
 * Copyright (c) 2009-2010 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.scene.plugins.blender.textures;

import com.jme3.math.FastMath;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.DynamicArray;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.texture.Texture;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is a base class for texture generators.
 * @author Marcin Roguski (Kaelthas)
 */
/* package */abstract class TextureGenerator {
	private static final Logger	LOGGER	= Logger.getLogger(TextureGenerator.class.getName());

	protected NoiseGenerator	noiseGenerator;
	
	public TextureGenerator(NoiseGenerator noiseGenerator) {
		this.noiseGenerator = noiseGenerator;
	}

	/**
	 * This method generates the texture.
	 * @param tex
	 *        texture's structure
	 * @param width
	 *        the width of the result texture
	 * @param height
	 *        the height of the result texture
	 * @param depth
	 *        the depth of the texture
	 * @param blenderContext
	 *        the blender context
	 * @return newly generated texture
	 */
	protected abstract Texture generate(Structure tex, int width, int height, int depth, BlenderContext blenderContext);

	/**
	 * This method reads the colorband data from the given texture structure.
	 * 
	 * @param tex
	 *        the texture structure
	 * @param blenderContext
	 *        the blender context
	 * @return read colorband or null if not present
	 */
	private ColorBand readColorband(Structure tex, BlenderContext blenderContext) {
		ColorBand result = null;
		int flag = ((Number) tex.getFieldValue("flag")).intValue();
		if ((flag & NoiseGenerator.TEX_COLORBAND) != 0) {
			Pointer pColorband = (Pointer) tex.getFieldValue("coba");
			Structure colorbandStructure;
			try {
				colorbandStructure = pColorband.fetchData(blenderContext.getInputStream()).get(0);
				result = new ColorBand(colorbandStructure);
			} catch (BlenderFileException e) {
				LOGGER.log(Level.WARNING, "Cannot fetch the colorband structure. The reason: {0}", e.getLocalizedMessage());
			}
		}
		return result;
	}
	
	protected float[][] computeColorband(Structure tex, BlenderContext blenderContext) {
		ColorBand colorBand = this.readColorband(tex, blenderContext);
		float[][] result = null;
		if(colorBand!=null) {
			result = new float[1001][4];//1001 - amount of possible cursor positions; 4 = [r, g, b, a]
			ColorBandData[] dataArray = colorBand.data;
			
			if(dataArray.length==1) {//special case; use only one color for all types of colorband interpolation
				for(int i=0;i<result.length;++i) {
					result[i][0] = dataArray[0].r;
					result[i][1] = dataArray[0].g;
					result[i][2] = dataArray[0].b;
					result[i][3] = dataArray[0].a;
				}
			} else {
				int currentCursor = 0;
				ColorBandData currentData = dataArray[0];
				ColorBandData nextData = dataArray[0];
				switch(colorBand.ipoType) {
					case ColorBand.IPO_LINEAR:
						float rDiff = 0, gDiff = 0, bDiff = 0, aDiff = 0, posDiff;
						for(int i=0;i<result.length;++i) {
							posDiff = i - currentData.pos;
							result[i][0] = currentData.r + rDiff * posDiff;
							result[i][1] = currentData.g + gDiff * posDiff;
							result[i][2] = currentData.b + bDiff * posDiff;
							result[i][3] = currentData.a + aDiff * posDiff;
							if(nextData.pos==i) {
								currentData = dataArray[currentCursor++];
								if(currentCursor < dataArray.length) {
									nextData = dataArray[currentCursor];
									//calculate differences
									int d = nextData.pos - currentData.pos;
									rDiff = (nextData.r - currentData.r)/d;
									gDiff = (nextData.g - currentData.g)/d;
									bDiff = (nextData.b - currentData.b)/d;
									aDiff = (nextData.a - currentData.a)/d;
								} else {
									rDiff = gDiff = bDiff = aDiff = 0;
								}							
							}
						}
						break;
					case ColorBand.IPO_BSPLINE:
					case ColorBand.IPO_CARDINAL:
						Map<Integer, ColorBandData> cbDataMap = new TreeMap<Integer, ColorBandData>();
						for(int i=0;i<colorBand.data.length;++i) {
							cbDataMap.put(Integer.valueOf(i), colorBand.data[i]);
						}
						
						if(colorBand.data[0].pos==0) {
							cbDataMap.put(Integer.valueOf(-1), colorBand.data[0]);
						} else {
							ColorBandData cbData = colorBand.data[0].clone();
							cbData.pos = 0;
							cbDataMap.put(Integer.valueOf(-1), cbData);
							cbDataMap.put(Integer.valueOf(-2), cbData);
						}
						
						if(colorBand.data[colorBand.data.length - 1].pos==1000) {
							cbDataMap.put(Integer.valueOf(colorBand.data.length), colorBand.data[colorBand.data.length - 1]);
						} else {
							ColorBandData cbData = colorBand.data[colorBand.data.length - 1].clone();
							cbData.pos = 1000;
							cbDataMap.put(Integer.valueOf(colorBand.data.length), cbData);
							cbDataMap.put(Integer.valueOf(colorBand.data.length + 1), cbData);
						}
						
						float[] ipoFactors = new float[4];
						float f;
						
						ColorBandData data0 = cbDataMap.get(currentCursor - 2);
						ColorBandData data1 = cbDataMap.get(currentCursor - 1);
						ColorBandData data2 = cbDataMap.get(currentCursor);
						ColorBandData data3 = cbDataMap.get(currentCursor + 1);
						
						for(int i=0;i<result.length;++i) {
							if (data2.pos != data1.pos) {
		                        f = (i - data2.pos) / (float)(data1.pos - data2.pos);
		                    } else {
		                        f = 0.0f;
		                    }
							
							f = FastMath.clamp(f, 0.0f, 1.0f);
							
							this.getIpoData(colorBand, f, ipoFactors);
							result[i][0] = ipoFactors[3] * data0.r + ipoFactors[2] * data1.r + ipoFactors[1] * data2.r + ipoFactors[0] * data3.r;
							result[i][1] = ipoFactors[3] * data0.g + ipoFactors[2] * data1.g + ipoFactors[1] * data2.g + ipoFactors[0] * data3.g;
							result[i][2] = ipoFactors[3] * data0.b + ipoFactors[2] * data1.b + ipoFactors[1] * data2.b + ipoFactors[0] * data3.b;
							result[i][3] = ipoFactors[3] * data0.a + ipoFactors[2] * data1.a + ipoFactors[1] * data2.a + ipoFactors[0] * data3.a;
							result[i][0] = FastMath.clamp(result[i][0], 0.0f, 1.0f);
							result[i][1] = FastMath.clamp(result[i][1], 0.0f, 1.0f);
							result[i][2] = FastMath.clamp(result[i][2], 0.0f, 1.0f);
							result[i][3] = FastMath.clamp(result[i][3], 0.0f, 1.0f);
							
							if(nextData.pos==i) {
								++currentCursor;
								data0 = cbDataMap.get(currentCursor - 2);
								data1 = cbDataMap.get(currentCursor - 1);
								data2 = cbDataMap.get(currentCursor);
								data3 = cbDataMap.get(currentCursor + 1);
							}
						}
						break;
					case ColorBand.IPO_EASE:
						float d, a, b, d2;
						for(int i=0;i<result.length;++i) {
							if(nextData.pos != currentData.pos) {
								d = (i - currentData.pos) / (float)(nextData.pos - currentData.pos);
								d2 = d * d;
								a = 3.0f * d2 - 2.0f * d * d2;
								b = 1.0f - a;
							} else {
								d = a = 0.0f;
								b = 1.0f;
							}
							
							result[i][0] = b * currentData.r + a * nextData.r;
							result[i][1] = b * currentData.g + a * nextData.g;
							result[i][2] = b * currentData.b + a * nextData.b;
							result[i][3] = b * currentData.a + a * nextData.a;
							if(nextData.pos==i) {
								currentData = dataArray[currentCursor++];
								if(currentCursor < dataArray.length) {
									nextData = dataArray[currentCursor];
								}						
							}
						}
						break;
					case ColorBand.IPO_CONSTANT:
						for(int i=0;i<result.length;++i) {
							result[i][0] = currentData.r;
							result[i][1] = currentData.g;
							result[i][2] = currentData.b;
							result[i][3] = currentData.a;
							if(nextData.pos==i) {
								currentData = dataArray[currentCursor++];
								if(currentCursor < dataArray.length) {
									nextData = dataArray[currentCursor];
								}
							}
						}
						break;
					default:
						throw new IllegalStateException("Unknown interpolation type: " + colorBand.ipoType);
				}
			}
		}
		return result;
	}
	
	/**
	 * This method returns the data for either B-spline of Cardinal interpolation.
	 * @param colorBand the color band
	 * @param d distance factor for the current intensity
	 * @param ipoFactors table to store the results (size of the table must be at least 4)
	 */
	private void getIpoData(ColorBand colorBand, float d, float[] ipoFactors) {
		float d2 = d * d;
		float d3 = d2 * d;
		if(colorBand.ipoType==ColorBand.IPO_BSPLINE) {
			ipoFactors[0] = -0.71f * d3 + 1.42f * d2 - 0.71f * d;
			ipoFactors[1] = 1.29f * d3 - 2.29f * d2 + 1.0f;
			ipoFactors[2] = -1.29f * d3 + 1.58f * d2 + 0.71f * d;
			ipoFactors[3] = 0.71f * d3 - 0.71f * d2;
		} else if(colorBand.ipoType==ColorBand.IPO_CARDINAL) {
			ipoFactors[0] = -0.16666666f * d3 + 0.5f * d2 - 0.5f * d + 0.16666666f;
			ipoFactors[1] = 0.5f * d3 - d2 + 0.6666666f;
			ipoFactors[2] = -0.5f * d3 + 0.5f * d2 + 0.5f * d + 0.16666666f;
			ipoFactors[3] = 0.16666666f * d3;
		} else {
			throw new IllegalStateException("Cannot get interpolation data for other colorband types than B-spline and Cardinal!");
		}
	}
	
	/**
	 * This method applies brightness and contrast for RGB textures.
	 * @param tex texture structure
	 * @param texres
	 */
	protected void applyBrightnessAndContrast(BrightnessAndContrastData bacd, TexturePixel texres) {
        texres.red = (texres.red - 0.5f) * bacd.contrast + bacd.brightness;
        if (texres.red < 0.0f) {
            texres.red = 0.0f;
        }
        texres.green =(texres.green - 0.5f) * bacd.contrast + bacd.brightness;
        if (texres.green < 0.0f) {
            texres.green = 0.0f;
        }
        texres.blue = (texres.blue - 0.5f) * bacd.contrast + bacd.brightness;
        if (texres.blue < 0.0f) {
            texres.blue = 0.0f;
        }
    }
	
	/**
	 * This method applies brightness and contrast for Luminance textures.
	 * @param texres
	 * @param contrast
	 * @param brightness
	 */
	protected void applyBrightnessAndContrast(TexturePixel texres, float contrast, float brightness) {
        texres.intensity = (texres.intensity - 0.5f) * contrast + brightness;
        if (texres.intensity < 0.0f) {
            texres.intensity = 0.0f;
        } else if (texres.intensity > 1.0f) {
            texres.intensity = 1.0f;
        }
    }
	
	/**
	 * A class constaining the colorband data.
	 * 
	 * @author Marcin Roguski (Kaelthas)
	 */
	protected static class ColorBand {
		//interpolation types
		public static final int IPO_LINEAR 		= 0;
		public static final int IPO_EASE 		= 1;
		public static final int IPO_BSPLINE 	= 2;
		public static final int IPO_CARDINAL 	= 3;
		public static final int IPO_CONSTANT 	= 4;

		public int		cursorsAmount, ipoType;
		public ColorBandData[]	data;

		/**
		 * Constructor. Loads the data from the given structure.
		 * 
		 * @param cbdataStructure
		 *        the colorband structure
		 */
		@SuppressWarnings("unchecked")
		public ColorBand(Structure colorbandStructure) {
			this.cursorsAmount = ((Number) colorbandStructure.getFieldValue("tot")).intValue();
			this.ipoType = ((Number) colorbandStructure.getFieldValue("ipotype")).intValue();
			this.data = new ColorBandData[this.cursorsAmount];
			DynamicArray<Structure> data = (DynamicArray<Structure>) colorbandStructure.getFieldValue("data");
			for (int i = 0; i < this.cursorsAmount; ++i) {
				this.data[i] = new ColorBandData(data.get(i));
			}
		}
	}

	/**
	 * Class to store the single colorband cursor data.
	 * 
	 * @author Marcin Roguski (Kaelthas)
	 */
	protected static class ColorBandData implements Cloneable {
		public final float	r, g, b, a;
		public int 	pos;

		/**
		 * Copy constructor.
		 */
		private ColorBandData(ColorBandData data) {
			this.r = data.r;
			this.g = data.g;
			this.b = data.b;
			this.a = data.a;
			this.pos = data.pos;
		}

		/**
		 * Constructor. Loads the data from the given structure.
		 * 
		 * @param cbdataStructure
		 *        the structure containing the CBData object
		 */
		public ColorBandData(Structure cbdataStructure) {
			this.r = ((Number) cbdataStructure.getFieldValue("r")).floatValue();
			this.g = ((Number) cbdataStructure.getFieldValue("g")).floatValue();
			this.b = ((Number) cbdataStructure.getFieldValue("b")).floatValue();
			this.a = ((Number) cbdataStructure.getFieldValue("a")).floatValue();
			this.pos = (int) (((Number) cbdataStructure.getFieldValue("pos")).floatValue() * 1000.0f);
		}

		@Override
		public ColorBandData clone() {
			try {
				return (ColorBandData) super.clone();
			} catch (CloneNotSupportedException e) {
				return new ColorBandData(this);
			}
		}

		@Override
		public String toString() {
			return "P: " + this.pos + " [" + this.r+", "+this.g+", "+this.b+", "+this.a+"]";
		}
	}
	
	/**
	 * This class contains brightness and contrast data.
	 * @author Marcin Roguski (Kaelthas)
	 */
	protected static class BrightnessAndContrastData {
		public final float contrast;
        public final float brightness;
        public final float rFactor;
        public final float gFactor;
        public final float bFactor;
        
        /**
         * Constructor reads the required data from the given structure.
         * @param tex texture structure
         */
		public BrightnessAndContrastData(Structure tex) {
			contrast = ((Number) tex.getFieldValue("contrast")).floatValue();
	        brightness = ((Number) tex.getFieldValue("bright")).floatValue() - 0.5f;
	        rFactor = ((Number) tex.getFieldValue("rfac")).floatValue();
	        gFactor = ((Number) tex.getFieldValue("gfac")).floatValue();
	        bFactor = ((Number) tex.getFieldValue("bfac")).floatValue();
		}
	}
}

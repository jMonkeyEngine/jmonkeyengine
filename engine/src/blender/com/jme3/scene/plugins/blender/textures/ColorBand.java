package com.jme3.scene.plugins.blender.textures;

import com.jme3.math.FastMath;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.DynamicArray;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class constaining the colorband data.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public class ColorBand {
	private static final Logger	LOGGER			= Logger.getLogger(ColorBand.class.getName());

	// interpolation types
	public static final int		IPO_LINEAR		= 0;
	public static final int		IPO_EASE		= 1;
	public static final int		IPO_BSPLINE		= 2;
	public static final int		IPO_CARDINAL	= 3;
	public static final int		IPO_CONSTANT	= 4;

	private int					cursorsAmount, ipoType;
	private ColorBandData[]		data;

	/**
	 * Constructor. Loads the data from the given structure.
         * @param tex
         * @param blenderContext 
         */
	@SuppressWarnings("unchecked")
	public ColorBand(Structure tex, BlenderContext blenderContext) {
		int flag = ((Number) tex.getFieldValue("flag")).intValue();
		if ((flag & GeneratedTexture.TEX_COLORBAND) != 0) {
			Pointer pColorband = (Pointer) tex.getFieldValue("coba");
			try {
				Structure colorbandStructure = pColorband.fetchData(blenderContext.getInputStream()).get(0);
				this.cursorsAmount = ((Number) colorbandStructure.getFieldValue("tot")).intValue();
				this.ipoType = ((Number) colorbandStructure.getFieldValue("ipotype")).intValue();
				this.data = new ColorBandData[this.cursorsAmount];
				DynamicArray<Structure> data = (DynamicArray<Structure>) colorbandStructure.getFieldValue("data");
				for (int i = 0; i < this.cursorsAmount; ++i) {
					this.data[i] = new ColorBandData(data.get(i));
				}
			} catch (BlenderFileException e) {
				LOGGER.log(Level.WARNING, "Cannot fetch the colorband structure. The reason: {0}", e.getLocalizedMessage());
			}
		}
	}

	/**
	 * This method determines if the colorband has any transparencies or is not
	 * transparent at all.
	 * 
	 * @return <b>true</b> if the colorband has transparencies and <b>false</b>
	 *         otherwise
	 */
	public boolean hasTransparencies() {
		if (data != null) {
			for (ColorBandData colorBandData : data) {
				if (colorBandData.a < 1.0f) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * This method computes the values of the colorband.
	 * 
	 * @return an array of 1001 elements and each element is float[4] object
	 *         containing rgba values
	 */
	public float[][] computeValues() {
		float[][] result = null;
		if (data != null) {
			result = new float[1001][4];// 1001 - amount of possible cursor
										// positions; 4 = [r, g, b, a]

			if (data.length == 1) {// special case; use only one color for all
									// types of colorband interpolation
				for (int i = 0; i < result.length; ++i) {
					result[i][0] = data[0].r;
					result[i][1] = data[0].g;
					result[i][2] = data[0].b;
					result[i][3] = data[0].a;
				}
			} else {
				int currentCursor = 0;
				ColorBandData currentData = data[0];
				ColorBandData nextData = data[0];
				switch (ipoType) {
					case ColorBand.IPO_LINEAR:
						float rDiff = 0,
						gDiff = 0,
						bDiff = 0,
						aDiff = 0,
						posDiff;
						for (int i = 0; i < result.length; ++i) {
							posDiff = i - currentData.pos;
							result[i][0] = currentData.r + rDiff * posDiff;
							result[i][1] = currentData.g + gDiff * posDiff;
							result[i][2] = currentData.b + bDiff * posDiff;
							result[i][3] = currentData.a + aDiff * posDiff;
							if (nextData.pos == i) {
								currentData = data[currentCursor++];
								if (currentCursor < data.length) {
									nextData = data[currentCursor];
									// calculate differences
									int d = nextData.pos - currentData.pos;
									rDiff = (nextData.r - currentData.r) / d;
									gDiff = (nextData.g - currentData.g) / d;
									bDiff = (nextData.b - currentData.b) / d;
									aDiff = (nextData.a - currentData.a) / d;
								} else {
									rDiff = gDiff = bDiff = aDiff = 0;
								}
							}
						}
						break;
					case ColorBand.IPO_BSPLINE:
					case ColorBand.IPO_CARDINAL:
						Map<Integer, ColorBandData> cbDataMap = new TreeMap<Integer, ColorBandData>();
						for (int i = 0; i < data.length; ++i) {
							cbDataMap.put(Integer.valueOf(i), data[i]);
						}

						if (data[0].pos == 0) {
							cbDataMap.put(Integer.valueOf(-1), data[0]);
						} else {
							ColorBandData cbData = data[0].clone();
							cbData.pos = 0;
							cbDataMap.put(Integer.valueOf(-1), cbData);
							cbDataMap.put(Integer.valueOf(-2), cbData);
						}

						if (data[data.length - 1].pos == 1000) {
							cbDataMap.put(Integer.valueOf(data.length), data[data.length - 1]);
						} else {
							ColorBandData cbData = data[data.length - 1].clone();
							cbData.pos = 1000;
							cbDataMap.put(Integer.valueOf(data.length), cbData);
							cbDataMap.put(Integer.valueOf(data.length + 1), cbData);
						}

						float[] ipoFactors = new float[4];
						float f;

						ColorBandData data0 = this.getColorbandData(currentCursor - 2, cbDataMap);
						ColorBandData data1 = this.getColorbandData(currentCursor - 1, cbDataMap);
						ColorBandData data2 = this.getColorbandData(currentCursor, cbDataMap);
						ColorBandData data3 = this.getColorbandData(currentCursor + 1, cbDataMap);

						for (int i = 0; i < result.length; ++i) {
							if (data2.pos != data1.pos) {
								f = (i - data2.pos) / (float) (data1.pos - data2.pos);
								f = FastMath.clamp(f, 0.0f, 1.0f);
							} else {
								f = 0.0f;
							}
							this.getIpoData(f, ipoFactors);
							result[i][0] = ipoFactors[3] * data0.r + ipoFactors[2] * data1.r + ipoFactors[1] * data2.r + ipoFactors[0] * data3.r;
							result[i][1] = ipoFactors[3] * data0.g + ipoFactors[2] * data1.g + ipoFactors[1] * data2.g + ipoFactors[0] * data3.g;
							result[i][2] = ipoFactors[3] * data0.b + ipoFactors[2] * data1.b + ipoFactors[1] * data2.b + ipoFactors[0] * data3.b;
							result[i][3] = ipoFactors[3] * data0.a + ipoFactors[2] * data1.a + ipoFactors[1] * data2.a + ipoFactors[0] * data3.a;
							result[i][0] = FastMath.clamp(result[i][0], 0.0f, 1.0f);
							result[i][1] = FastMath.clamp(result[i][1], 0.0f, 1.0f);
							result[i][2] = FastMath.clamp(result[i][2], 0.0f, 1.0f);
							result[i][3] = FastMath.clamp(result[i][3], 0.0f, 1.0f);

							if (nextData.pos == i) {
								++currentCursor;
								data0 = cbDataMap.get(currentCursor - 2);
								data1 = cbDataMap.get(currentCursor - 1);
								data2 = cbDataMap.get(currentCursor);
								data3 = cbDataMap.get(currentCursor + 1);
							}
						}
						break;
					case ColorBand.IPO_EASE:
						float d,
						a,
						b,
						d2;
						for (int i = 0; i < result.length; ++i) {
							if (nextData.pos != currentData.pos) {
								d = (i - currentData.pos) / (float) (nextData.pos - currentData.pos);
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
							if (nextData.pos == i) {
								currentData = data[currentCursor++];
								if (currentCursor < data.length) {
									nextData = data[currentCursor];
								}
							}
						}
						break;
					case ColorBand.IPO_CONSTANT:
						for (int i = 0; i < result.length; ++i) {
							result[i][0] = currentData.r;
							result[i][1] = currentData.g;
							result[i][2] = currentData.b;
							result[i][3] = currentData.a;
							if (nextData.pos == i) {
								currentData = data[currentCursor++];
								if (currentCursor < data.length) {
									nextData = data[currentCursor];
								}
							}
						}
						break;
					default:
						throw new IllegalStateException("Unknown interpolation type: " + ipoType);
				}
			}
		}
		return result;
	}
	
	private ColorBandData getColorbandData(int index, Map<Integer, ColorBandData> cbDataMap) {
		ColorBandData result = cbDataMap.get(index);
		if(result == null) {
			result = new ColorBandData();
		}
		return result;
	}

	/**
	 * This method returns the data for either B-spline of Cardinal
	 * interpolation.
	 * 
	 * @param d
	 *            distance factor for the current intensity
	 * @param ipoFactors
	 *            table to store the results (size of the table must be at least
	 *            4)
	 */
	private void getIpoData(float d, float[] ipoFactors) {
		float d2 = d * d;
		float d3 = d2 * d;
		if (ipoType == ColorBand.IPO_BSPLINE) {
			ipoFactors[0] = -0.71f * d3 + 1.42f * d2 - 0.71f * d;
			ipoFactors[1] = 1.29f * d3 - 2.29f * d2 + 1.0f;
			ipoFactors[2] = -1.29f * d3 + 1.58f * d2 + 0.71f * d;
			ipoFactors[3] = 0.71f * d3 - 0.71f * d2;
		} else if (ipoType == ColorBand.IPO_CARDINAL) {
			ipoFactors[0] = -0.16666666f * d3 + 0.5f * d2 - 0.5f * d + 0.16666666f;
			ipoFactors[1] = 0.5f * d3 - d2 + 0.6666666f;
			ipoFactors[2] = -0.5f * d3 + 0.5f * d2 + 0.5f * d + 0.16666666f;
			ipoFactors[3] = 0.16666666f * d3;
		} else {
			throw new IllegalStateException("Cannot get interpolation data for other colorband types than B-spline and Cardinal!");
		}
	}

	/**
	 * Class to store the single colorband cursor data.
	 * 
	 * @author Marcin Roguski (Kaelthas)
	 */
	private static class ColorBandData implements Cloneable {
		public final float	r, g, b, a;
		public int			pos;

		public ColorBandData() {
			r = g = b = 0;
			a = 1;
		}
		
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
		 *            the structure containing the CBData object
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
			return "P: " + this.pos + " [" + this.r + ", " + this.g + ", " + this.b + ", " + this.a + "]";
		}
	}
}

package com.jme3.scene.plugins.blender.animations;

import com.jme3.animation.BoneTrack;
import com.jme3.animation.SpatialTrack;
import com.jme3.animation.Track;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.plugins.blender.curves.BezierCurve;

/**
 * This class is used to calculate bezier curves value for the given frames. The
 * Ipo (interpolation object) consists of several b-spline curves (connected 3rd
 * degree bezier curves) of a different type.
 * 
 * @author Marcin Roguski
 */
public class Ipo {

	public static final int	AC_LOC_X	= 1;
	public static final int	AC_LOC_Y	= 2;
	public static final int	AC_LOC_Z	= 3;
	public static final int	OB_ROT_X	= 7;
	public static final int	OB_ROT_Y	= 8;
	public static final int	OB_ROT_Z	= 9;
	public static final int	AC_SIZE_X	= 13;
	public static final int	AC_SIZE_Y	= 14;
	public static final int	AC_SIZE_Z	= 15;
	public static final int	AC_QUAT_W	= 25;
	public static final int	AC_QUAT_X	= 26;
	public static final int	AC_QUAT_Y	= 27;
	public static final int	AC_QUAT_Z	= 28;

	/** A list of bezier curves for this interpolation object. */
	private BezierCurve[]	bezierCurves;
	/** Each ipo contains one bone track. */
	private Track			calculatedTrack;
	/** This variable indicates if the Y asxis is the UP axis or not. */
	protected boolean		fixUpAxis;

	/**
	 * Constructor. Stores the bezier curves.
	 * 
	 * @param bezierCurves
	 *            a table of bezier curves
	 */
	public Ipo(BezierCurve[] bezierCurves, boolean fixUpAxis) {
		this.bezierCurves = bezierCurves;
		this.fixUpAxis = fixUpAxis;
	}

	/**
	 * This method calculates the ipo value for the first curve.
	 * 
	 * @param frame
	 *            the frame for which the value is calculated
	 * @return calculated ipo value
	 */
	public float calculateValue(int frame) {
		return this.calculateValue(frame, 0);
	}

	/**
	 * This method calculates the ipo value for the curve of the specified
	 * index. Make sure you do not exceed the curves amount. Alway chech the
	 * amount of curves before calling this method.
	 * 
	 * @param frame
	 *            the frame for which the value is calculated
	 * @param curveIndex
	 *            the index of the curve
	 * @return calculated ipo value
	 */
	public float calculateValue(int frame, int curveIndex) {
		return bezierCurves[curveIndex].evaluate(frame, BezierCurve.Y_VALUE);
	}

	/**
	 * This method returns the curves amount.
	 * 
	 * @return the curves amount
	 */
	public int getCurvesAmount() {
		return bezierCurves.length;
	}

	/**
	 * This method returns the frame where last bezier triple center point of
	 * the specified bezier curve is located.
	 * 
	 * @return the frame number of the last defined bezier triple point for the
	 *         specified ipo
	 */
	public int getLastFrame() {
		int result = 1;
		for (int i = 0; i < bezierCurves.length; ++i) {
			int tempResult = bezierCurves[i].getLastFrame();
			if (tempResult > result) {
				result = tempResult;
			}
		}
		return result;
	}

	/**
	 * This method calculates the value of the curves as a bone track between
	 * the specified frames.
	 * 
	 * @param targetIndex
	 *            the index of the target for which the method calculates the
	 *            tracks IMPORTANT! Aet to -1 (or any negative number) if you
	 *            want to load spatial animation.
	 * @param startFrame
	 *            the firs frame of tracks (inclusive)
	 * @param stopFrame
	 *            the last frame of the tracks (inclusive)
	 * @param fps
	 *            frame rate (frames per second)
	 * @param spatialTrack
	 *            this flag indicates if the track belongs to a spatial or to a
	 *            bone; the diference is important because it appears that bones
	 *            in blender have the same type of coordinate system (Y as UP)
	 *            as jme while other features have different one (Z is UP)
	 * @return bone track for the specified bone
	 */
	public Track calculateTrack(int targetIndex, int startFrame, int stopFrame, int fps, boolean spatialTrack) {
		if (calculatedTrack == null) {
			// preparing data for track
			int framesAmount = stopFrame - startFrame;
			float start = (startFrame - 1.0f) / fps;
			float timeBetweenFrames = 1.0f / fps;

			float[] times = new float[framesAmount + 1];
			Vector3f[] translations = new Vector3f[framesAmount + 1];
			float[] translation = new float[3];
			Quaternion[] rotations = new Quaternion[framesAmount + 1];
			float[] quaternionRotation = new float[4];
			float[] objectRotation = new float[3];
			Vector3f[] scales = new Vector3f[framesAmount + 1];
			float[] scale = new float[] { 1.0f, 1.0f, 1.0f };
			float degreeToRadiansFactor = FastMath.DEG_TO_RAD * 10;// the values in blender are divided by 10, so we need to mult it here

			// calculating track data
			for (int frame = startFrame; frame <= stopFrame; ++frame) {
				int index = frame - startFrame;
				times[index] = start + (frame - 1) * timeBetweenFrames;
				for (int j = 0; j < bezierCurves.length; ++j) {
					double value = bezierCurves[j].evaluate(frame, BezierCurve.Y_VALUE);
					switch (bezierCurves[j].getType()) {
					// LOCATION
						case AC_LOC_X:
							translation[0] = (float) value;
							break;
						case AC_LOC_Y:
							if (fixUpAxis && spatialTrack) {
								translation[2] = (float) -value;
							} else {
								translation[1] = (float) value;
							}
							break;
						case AC_LOC_Z:
							translation[fixUpAxis && spatialTrack ? 1 : 2] = (float) value;
							break;

						// ROTATION (used with object animation)
						// the value here is in degrees divided by 10 (so in
						// example: 9 = PI/2)
						case OB_ROT_X:
							objectRotation[0] = (float) value * degreeToRadiansFactor;
							break;
						case OB_ROT_Y:
							if (fixUpAxis) {
								objectRotation[2] = (float) -value * degreeToRadiansFactor;
							} else {
								objectRotation[1] = (float) value * degreeToRadiansFactor;
							}
							break;
						case OB_ROT_Z:
							objectRotation[fixUpAxis ? 1 : 2] = (float) value * degreeToRadiansFactor;
							break;

						// SIZE
						case AC_SIZE_X:
							scale[0] = (float) value;
							break;
						case AC_SIZE_Y:
							if (fixUpAxis && spatialTrack) {
								scale[2] = (float) value;
							} else {
								scale[1] = (float) value;
							}
							break;
						case AC_SIZE_Z:
							scale[fixUpAxis && spatialTrack ? 1 : 2] = (float) value;
							break;

						// QUATERNION ROTATION (used with bone animation), dunno
						// why but here we shouldn't check the
						// spatialTrack flag value
						case AC_QUAT_W:
							quaternionRotation[3] = (float) value;
							break;
						case AC_QUAT_X:
							quaternionRotation[0] = (float) value;
							break;
						case AC_QUAT_Y:
							if (fixUpAxis) {
								quaternionRotation[2] = -(float) value;
							} else {
								quaternionRotation[1] = (float) value;
							}
							break;
						case AC_QUAT_Z:
							if (fixUpAxis) {
								quaternionRotation[1] = (float) value;
							} else {
								quaternionRotation[2] = (float) value;
							}
							break;
						default:
							throw new IllegalStateException("Unknown ipo curve type: " + bezierCurves[j].getType());
					}
				}
				translations[index] = new Vector3f(translation[0], translation[1], translation[2]);
				rotations[index] = spatialTrack ? new Quaternion().fromAngles(objectRotation) : new Quaternion(quaternionRotation[0], quaternionRotation[1], quaternionRotation[2], quaternionRotation[3]);
				scales[index] = new Vector3f(scale[0], scale[1], scale[2]);
			}
			if (spatialTrack) {
				calculatedTrack = new SpatialTrack(times, translations, rotations, scales);
			} else {
				calculatedTrack = new BoneTrack(targetIndex, times, translations, rotations, scales);
			}
		}
		return calculatedTrack;
	}
}
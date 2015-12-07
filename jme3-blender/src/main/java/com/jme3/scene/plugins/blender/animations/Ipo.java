package com.jme3.scene.plugins.blender.animations;

import java.util.logging.Level;
import java.util.logging.Logger;

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
    private static final Logger LOGGER    = Logger.getLogger(Ipo.class.getName());

    public static final int     AC_LOC_X  = 1;
    public static final int     AC_LOC_Y  = 2;
    public static final int     AC_LOC_Z  = 3;
    public static final int     OB_ROT_X  = 7;
    public static final int     OB_ROT_Y  = 8;
    public static final int     OB_ROT_Z  = 9;
    public static final int     AC_SIZE_X = 13;
    public static final int     AC_SIZE_Y = 14;
    public static final int     AC_SIZE_Z = 15;
    public static final int     AC_QUAT_W = 25;
    public static final int     AC_QUAT_X = 26;
    public static final int     AC_QUAT_Y = 27;
    public static final int     AC_QUAT_Z = 28;

    /** A list of bezier curves for this interpolation object. */
    private BezierCurve[]       bezierCurves;
    /** Each ipo contains one bone track. */
    private Track               calculatedTrack;
    /** This variable indicates if the Y asxis is the UP axis or not. */
    protected boolean           fixUpAxis;
    /**
     * Depending on the blender version rotations are stored in degrees or
     * radians so we need to know the version that is used.
     */
    protected final int         blenderVersion;

    /**
     * Constructor. Stores the bezier curves.
     * 
     * @param bezierCurves
     *            a table of bezier curves
     * @param fixUpAxis
     *            indicates if the Y is the up axis or not
     * @param blenderVersion
     *            the blender version that is currently used
     */
    public Ipo(BezierCurve[] bezierCurves, boolean fixUpAxis, int blenderVersion) {
        this.bezierCurves = bezierCurves;
        this.fixUpAxis = fixUpAxis;
        this.blenderVersion = blenderVersion;
    }

    /**
     * This method calculates the ipo value for the first curve.
     * 
     * @param frame
     *            the frame for which the value is calculated
     * @return calculated ipo value
     */
    public double calculateValue(int frame) {
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
    public double calculateValue(int frame, int curveIndex) {
        return bezierCurves[curveIndex].evaluate(frame, BezierCurve.Y_VALUE);
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
     * @param localTranslation
     *            the local translation of the object/bone that will be animated by
     *            the track
     * @param localRotation
     *            the local rotation of the object/bone that will be animated by
     *            the track
     * @param localScale
     *            the local scale of the object/bone that will be animated by
     *            the track
     * @param startFrame
     *            the first frame of tracks (inclusive)
     * @param stopFrame
     *            the last frame of the tracks (inclusive)
     * @param fps
     *            frame rate (frames per second)
     * @param spatialTrack
     *            this flag indicates if the track belongs to a spatial or to a
     *            bone; the difference is important because it appears that bones
     *            in blender have the same type of coordinate system (Y as UP)
     *            as jme while other features have different one (Z is UP)
     * @return bone track for the specified bone
     */
    public Track calculateTrack(int targetIndex, BoneContext boneContext, Vector3f localTranslation, Quaternion localRotation, Vector3f localScale, int startFrame, int stopFrame, int fps, boolean spatialTrack) {
        if (calculatedTrack == null) {
            // preparing data for track
            int framesAmount = stopFrame - startFrame;
            float timeBetweenFrames = 1.0f / fps;

            float[] times = new float[framesAmount + 1];
            Vector3f[] translations = new Vector3f[framesAmount + 1];
            float[] translation = new float[3];
            Quaternion[] rotations = new Quaternion[framesAmount + 1];
            float[] quaternionRotation = new float[] { localRotation.getX(), localRotation.getY(), localRotation.getZ(), localRotation.getW(), };
            float[] eulerRotation = localRotation.toAngles(null);
            Vector3f[] scales = new Vector3f[framesAmount + 1];
            float[] scale = new float[] { localScale.x, localScale.y, localScale.z };
            float degreeToRadiansFactor = 1;
            if (blenderVersion < 250) {// in blender earlier than 2.50 the values are stored in degrees
                degreeToRadiansFactor *= FastMath.DEG_TO_RAD * 10;// the values in blender are divided by 10, so we need to mult it here
            }
            int yIndex = 1, zIndex = 2;
            boolean swapAxes = spatialTrack && fixUpAxis;
            if (swapAxes) {
                yIndex = 2;
                zIndex = 1;
            }
            boolean eulerRotationUsed = false, queternionRotationUsed = false;

            // calculating track data
            for (int frame = startFrame; frame <= stopFrame; ++frame) {
                boolean translationSet = false;
                translation[0] = translation[1] = translation[2] = 0;
                int index = frame - startFrame;
                times[index] = index * timeBetweenFrames;// start + (frame - 1) * timeBetweenFrames;
                for (int j = 0; j < bezierCurves.length; ++j) {
                    double value = bezierCurves[j].evaluate(frame, BezierCurve.Y_VALUE);
                    switch (bezierCurves[j].getType()) {
                        // LOCATION
                        case AC_LOC_X:
                            translation[0] = (float) value;
                            translationSet = true;
                            break;
                        case AC_LOC_Y:
                            if (swapAxes && value != 0) {
                                value = -value;
                            }
                            translation[yIndex] = (float) value;
                            translationSet = true;
                            break;
                        case AC_LOC_Z:
                            translation[zIndex] = (float) value;
                            translationSet = true;
                            break;

                        // EULER ROTATION
                        case OB_ROT_X:
                            eulerRotationUsed = true;
                            eulerRotation[0] = (float) value * degreeToRadiansFactor;
                            break;
                        case OB_ROT_Y:
                            eulerRotationUsed = true;
                            if (swapAxes && value != 0) {
                                value = -value;
                            }
                            eulerRotation[yIndex] = (float) value * degreeToRadiansFactor;
                            break;
                        case OB_ROT_Z:
                            eulerRotationUsed = true;
                            eulerRotation[zIndex] = (float) value * degreeToRadiansFactor;
                            break;

                        // SIZE
                        case AC_SIZE_X:
                            scale[0] = (float) value;
                            break;
                        case AC_SIZE_Y:
                            scale[yIndex] = (float) value;
                            break;
                        case AC_SIZE_Z:
                            scale[zIndex] = (float) value;
                            break;

                        // QUATERNION ROTATION (used with bone animation)
                        case AC_QUAT_W:
                            queternionRotationUsed = true;
                            quaternionRotation[3] = (float) value;
                            break;
                        case AC_QUAT_X:
                            queternionRotationUsed = true;
                            quaternionRotation[0] = (float) value;
                            break;
                        case AC_QUAT_Y:
                            queternionRotationUsed = true;
                            if (swapAxes && value != 0) {
                                value = -value;
                            }
                            quaternionRotation[yIndex] = (float) value;
                            break;
                        case AC_QUAT_Z:
                            quaternionRotation[zIndex] = (float) value;
                            break;
                        default:
                            LOGGER.log(Level.WARNING, "Unknown ipo curve type: {0}.", bezierCurves[j].getType());
                    }
                }
                if(translationSet) {
                    translations[index] = localRotation.multLocal(new Vector3f(translation[0], translation[1], translation[2]));
                } else {
                    translations[index] = new Vector3f();
                }
                
                if(boneContext != null) {
                    if(boneContext.getBone().getParent() == null && boneContext.is(BoneContext.NO_LOCAL_LOCATION)) {
                        float temp = translations[index].z;
                        translations[index].z = -translations[index].y;
                        translations[index].y = temp;
                    }
                }
                
                if (queternionRotationUsed) {
                    rotations[index] = new Quaternion(quaternionRotation[0], quaternionRotation[1], quaternionRotation[2], quaternionRotation[3]);
                } else {
                    rotations[index] = new Quaternion().fromAngles(eulerRotation);
                }

                scales[index] = new Vector3f(scale[0], scale[1], scale[2]);
            }
            if (spatialTrack) {
                calculatedTrack = new SpatialTrack(times, translations, rotations, scales);
            } else {
                calculatedTrack = new BoneTrack(targetIndex, times, translations, rotations, scales);
            }

            if (queternionRotationUsed && eulerRotationUsed) {
                LOGGER.warning("Animation uses both euler and quaternion tracks for rotations. Quaternion rotation is applied. Make sure that this is what you wanted!");
            }
        }

        return calculatedTrack;
    }

    /**
     * Ipo constant curve. This is a curve with only one value and no specified
     * type. This type of ipo cannot be used to calculate tracks. It should only
     * be used to calculate single value for a given frame.
     * 
     * @author Marcin Roguski (Kaelthas)
     */
    /* package */static class ConstIpo extends Ipo {

        /** The constant value of this ipo. */
        private float constValue;

        /**
         * Constructor. Stores the constant value of this ipo.
         * 
         * @param constValue
         *            the constant value of this ipo
         */
        public ConstIpo(float constValue) {
            super(null, false, 0);// the version is not important here
            this.constValue = constValue;
        }

        @Override
        public double calculateValue(int frame) {
            return constValue;
        }

        @Override
        public double calculateValue(int frame, int curveIndex) {
            return constValue;
        }

        @Override
        public BoneTrack calculateTrack(int boneIndex, BoneContext boneContext, Vector3f localTranslation, Quaternion localRotation, Vector3f localScale, int startFrame, int stopFrame, int fps, boolean boneTrack) {
            throw new IllegalStateException("Constatnt ipo object cannot be used for calculating bone tracks!");
        }
    }
}

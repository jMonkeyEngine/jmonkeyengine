package com.jme3.scene.plugins.blender.constraints.definitions;

import java.io.IOException;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.SpatialTrack;
import com.jme3.animation.Track;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.animations.Ipo;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.util.TempVars;

public abstract class ConstraintDefinition {
    protected int flag;

    public ConstraintDefinition(Structure constraintData, BlenderContext blenderContext) {
        if (constraintData != null) {// Null constraint has no data
            Number flag = (Number) constraintData.getFieldValue("flag");
            if (flag != null) {
                this.flag = flag.intValue();
            }
        }
    }

    public void bake(Transform ownerTransform, Transform targetTransform, Track ownerTrack, Track targetTrack, Ipo influenceIpo) {
        TrackWrapper ownerWrapperTrack = ownerTrack != null ? new TrackWrapper(ownerTrack) : null;
        TrackWrapper targetWrapperTrack = targetTrack != null ? new TrackWrapper(targetTrack) : null;

        // uruchamiamy bake dla transformat zalenie od tego, ktre argumenty s nullami, a ktre - nie
        this.bake(ownerTransform, targetTransform, influenceIpo.calculateValue(0));
        if (ownerWrapperTrack != null) {
            float[] ownerTimes = ownerWrapperTrack.getTimes();
            Vector3f[] translations = ownerWrapperTrack.getTranslations();
            Quaternion[] rotations = ownerWrapperTrack.getRotations();
            Vector3f[] scales = ownerWrapperTrack.getScales();

            float[] targetTimes = targetWrapperTrack == null ? null : targetWrapperTrack.getTimes();
            Vector3f[] targetTranslations = targetWrapperTrack == null ? null : targetWrapperTrack.getTranslations();
            Quaternion[] targetRotations = targetWrapperTrack == null ? null : targetWrapperTrack.getRotations();
            Vector3f[] targetScales = targetWrapperTrack == null ? null : targetWrapperTrack.getScales();
            Vector3f translation = new Vector3f(), scale = new Vector3f();
            Quaternion rotation = new Quaternion();

            Transform ownerTemp = new Transform(), targetTemp = new Transform();
            for (int i = 0; i < ownerTimes.length; ++i) {
                float t = ownerTimes[i];
                ownerTemp.setTranslation(translations[i]);
                ownerTemp.setRotation(rotations[i]);
                ownerTemp.setScale(scales[i]);
                if (targetWrapperTrack == null) {
                    this.bake(ownerTemp, targetTransform, influenceIpo.calculateValue(i));
                } else {
                    // getting the values that are the interpolation of the target track for the time 't'
                    this.interpolate(targetTranslations, targetTimes, t, translation);
                    this.interpolate(targetRotations, targetTimes, t, rotation);
                    this.interpolate(targetScales, targetTimes, t, scale);

                    targetTemp.setTranslation(translation);
                    targetTemp.setRotation(rotation);
                    targetTemp.setScale(scale);

                    this.bake(ownerTemp, targetTemp, influenceIpo.calculateValue(i));
                }
                // need to clone here because each of the arrays will reference the same instance if they hold the same value in the compact array
                translations[i] = ownerTemp.getTranslation().clone();
                rotations[i] = ownerTemp.getRotation().clone();
                scales[i] = ownerTemp.getScale().clone();
            }
            ownerWrapperTrack.setKeyframes(ownerTimes, translations, rotations, scales);
        }
    }

    protected abstract void bake(Transform ownerTransform, Transform targetTransform, float influence);

    private void interpolate(Vector3f[] targetVectors, float[] targetTimes, float currentTime, Vector3f result) {
        int index = 0;
        for (int i = 1; i < targetTimes.length; ++i) {
            if (targetTimes[i] < currentTime) {
                ++index;
            } else {
                break;
            }
        }
        if (index >= targetTimes.length - 1) {
            result.set(targetVectors[targetTimes.length - 1]);
        } else {
            float delta = targetTimes[index + 1] - targetTimes[index];
            if (delta == 0.0f) {
                result.set(targetVectors[index + 1]);
            } else {
                float scale = (currentTime - targetTimes[index]) / (targetTimes[index + 1] - targetTimes[index]);
                FastMath.interpolateLinear(scale, targetVectors[index], targetVectors[index + 1], result);
            }
        }
    }

    private void interpolate(Quaternion[] targetQuaternions, float[] targetTimes, float currentTime, Quaternion result) {
        int index = 0;
        for (int i = 1; i < targetTimes.length; ++i) {
            if (targetTimes[i] < currentTime) {
                ++index;
            } else {
                break;
            }
        }
        if (index >= targetTimes.length - 1) {
            result.set(targetQuaternions[targetTimes.length - 1]);
        } else {
            float delta = targetTimes[index + 1] - targetTimes[index];
            if (delta == 0.0f) {
                result.set(targetQuaternions[index + 1]);
            } else {
                float scale = (currentTime - targetTimes[index]) / (targetTimes[index + 1] - targetTimes[index]);
                result.slerp(targetQuaternions[index], targetQuaternions[index + 1], scale);
            }
        }
    }

    /**
     * This class holds either the bone track or spatial track. Is made to improve
     * code readability.
     * 
     * @author Marcin Roguski (Kaelthas)
     */
    private static class TrackWrapper implements Track {
        /** The spatial track. */
        private SpatialTrack spatialTrack;
        /** The bone track. */
        private BoneTrack    boneTrack;

        /**
         * Constructs the object using the given track. The track must be of one of the types: <li>BoneTrack <li>SpatialTrack
         * 
         * @param track
         *            the animation track
         */
        public TrackWrapper(Track track) {
            if (track instanceof SpatialTrack) {
                this.spatialTrack = (SpatialTrack) track;
            } else if (track instanceof BoneTrack) {
                this.boneTrack = (BoneTrack) track;
            } else {
                throw new IllegalStateException("Unknown track type!");
            }
        }

        /**
         * @return the array of rotations of this track
         */
        public Quaternion[] getRotations() {
            if (boneTrack != null) {
                return boneTrack.getRotations();
            }
            return spatialTrack.getRotations();
        }

        /**
         * @return the array of scales for this track
         */
        public Vector3f[] getScales() {
            if (boneTrack != null) {
                return boneTrack.getScales();
            }
            return spatialTrack.getScales();
        }

        /**
         * @return the arrays of time for this track
         */
        public float[] getTimes() {
            if (boneTrack != null) {
                return boneTrack.getTimes();
            }
            return spatialTrack.getTimes();
        }

        /**
         * @return the array of translations of this track
         */
        public Vector3f[] getTranslations() {
            if (boneTrack != null) {
                return boneTrack.getTranslations();
            }
            return spatialTrack.getTranslations();
        }

        /**
         * Set the translations, rotations and scales for this bone track
         * 
         * @param times
         *            a float array with the time of each frame
         * @param translations
         *            the translation of the bone for each frame
         * @param rotations
         *            the rotation of the bone for each frame
         * @param scales
         *            the scale of the bone for each frame
         */
        public void setKeyframes(float[] times, Vector3f[] translations, Quaternion[] rotations, Vector3f[] scales) {
            if (boneTrack != null) {
                boneTrack.setKeyframes(times, translations, rotations, scales);
            } else {
                spatialTrack.setKeyframes(times, translations, rotations, scales);
            }
        }

        public void write(JmeExporter ex) throws IOException {
            // no need to implement this one (the TrackWrapper is used internally and never serialized)
        }

        public void read(JmeImporter im) throws IOException {
            // no need to implement this one (the TrackWrapper is used internally and never serialized)
        }

        public void setTime(float time, float weight, AnimControl control, AnimChannel channel, TempVars vars) {
            if (boneTrack != null) {
                boneTrack.setTime(time, weight, control, channel, vars);
            } else {
                spatialTrack.setTime(time, weight, control, channel, vars);
            }
        }

        public float getLength() {
            return spatialTrack == null ? boneTrack.getLength() : spatialTrack.getLength();
        }

        @Override
        public TrackWrapper clone() {
            if (boneTrack != null) {
                return new TrackWrapper(boneTrack.clone());
            }
            return new TrackWrapper(spatialTrack.clone());
        }
    }
}

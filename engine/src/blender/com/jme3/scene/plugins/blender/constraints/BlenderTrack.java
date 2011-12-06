package com.jme3.scene.plugins.blender.constraints;

import java.io.IOException;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.SpatialTrack;
import com.jme3.animation.Track;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.util.TempVars;

/**
 * This class holds either the bone track or spatial track. Is made to improve
 * code readability.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
/* package */final class BlenderTrack implements Track {
	/** The spatial track. */
	private SpatialTrack spatialTrack;
	/** The bone track. */
	private BoneTrack boneTrack;

	/**
	 * Constructs the object using spatial track (bone track is null).
	 * 
	 * @param spatialTrack
	 *            the spatial track
	 */
	public BlenderTrack(SpatialTrack spatialTrack) {
		this.spatialTrack = spatialTrack;
	}

	/**
	 * Constructs the object using bone track (spatial track is null).
	 * 
	 * @param spatialTrack
	 *            the spatial track
	 */
	public BlenderTrack(BoneTrack boneTrack) {
		this.boneTrack = boneTrack;
	}

	/**
	 * @return the stored track (either bone or spatial)
	 */
	public Track getTrack() {
		return boneTrack != null ? boneTrack : spatialTrack;
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
	public void setKeyframes(float[] times, Vector3f[] translations,
			Quaternion[] rotations, Vector3f[] scales) {
		if (boneTrack != null) {
			boneTrack.setKeyframes(times, translations, rotations, scales);
		} else {
			spatialTrack.setKeyframes(times, translations, rotations, scales);
		}
	}

	@Override
	public void write(JmeExporter ex) throws IOException {
	}

	@Override
	public void read(JmeImporter im) throws IOException {
	}

	@Override
	public void setTime(float time, float weight, AnimControl control,
			AnimChannel channel, TempVars vars) {
		if (boneTrack != null) {
			boneTrack.setTime(time, weight, control, channel, vars);
		} else {
			spatialTrack.setTime(time, weight, control, channel, vars);
		}
	}

	@Override
	public float getLength() {
		return spatialTrack == null ? boneTrack.getLength() : spatialTrack
				.getLength();
	}

	@Override
	public BlenderTrack clone() {
		if (boneTrack != null) {
			return new BlenderTrack(boneTrack.clone());
		}
		return new BlenderTrack(spatialTrack.clone());
	}
}

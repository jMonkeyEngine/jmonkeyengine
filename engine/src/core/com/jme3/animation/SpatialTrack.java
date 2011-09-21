package com.jme3.animation;

import java.io.IOException;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;

/**
 * This class represents the track for spatial animation.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public class SpatialTrack implements Savable {
	/** Translations of the track. */
	private CompactVector3Array translations;
	/** Rotations of the track. */
	private CompactQuaternionArray rotations;
	/** Scales of the track. */
	private CompactVector3Array scales;
	/** The times of the animations frames. */
	private float[] times;

	// temp vectors for interpolation
	private transient final Vector3f tempV = new Vector3f();
	private transient final Quaternion tempQ = new Quaternion();
	private transient final Vector3f tempS = new Vector3f();
	private transient final Vector3f tempV2 = new Vector3f();
	private transient final Quaternion tempQ2 = new Quaternion();
	private transient final Vector3f tempS2 = new Vector3f();

	public SpatialTrack() {
	}

	/**
	 * Creates a spatial track for the given track data.
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
	public SpatialTrack(float[] times, Vector3f[] translations,
						Quaternion[] rotations, Vector3f[] scales) {
		this.setKeyframes(times, translations, rotations, scales);
	}

	/**
	 * 
	 * Modify the spatial which this track modifies.
	 * 
	 * @param time
	 *            the current time of the animation
	 * @param spatial
	 *            the spatial that should be animated with this track
	 */
	public void setTime(float time, Spatial spatial) {
		int lastFrame = times.length - 1;
		if (time < 0 || lastFrame == 0) {
			rotations.get(0, tempQ);
			translations.get(0, tempV);
			if (scales != null) {
				scales.get(0, tempS);
			}
		} else if (time >= times[lastFrame]) {
			rotations.get(lastFrame, tempQ);
			translations.get(lastFrame, tempV);
			if (scales != null) {
				scales.get(lastFrame, tempS);
			}
		} else {
			int startFrame = 0;
			int endFrame = 1;
			// use lastFrame so we never overflow the array
			for (int i = 0; i < lastFrame && times[i] < time; ++i) {
				startFrame = i;
				endFrame = i + 1;
			}

			float blend = (time - times[startFrame]) / (times[endFrame] - times[startFrame]);

			rotations.get(startFrame, tempQ);
			translations.get(startFrame, tempV);
			if (scales != null) {
				scales.get(startFrame, tempS);
			}
			rotations.get(endFrame, tempQ2);
			translations.get(endFrame, tempV2);
			if (scales != null) {
				scales.get(endFrame, tempS2);
			}
			tempQ.nlerp(tempQ2, blend);
			tempV.interpolate(tempV2, blend);
			tempS.interpolate(tempS2, blend);
		}
		spatial.setLocalTranslation(tempV);
		spatial.setLocalRotation(tempQ);
		if (scales != null) {
			spatial.setLocalScale(tempS);
		}
	}

	/**
	 * Set the translations, rotations and scales for this track.
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
		if (times.length == 0) {
			throw new RuntimeException("BoneTrack with no keyframes!");
		}

		assert times.length == translations.length
				&& times.length == rotations.length;

		this.times = times;
		this.translations = new CompactVector3Array();
		this.translations.add(translations);
		this.translations.freeze();
		this.rotations = new CompactQuaternionArray();
		this.rotations.add(rotations);
		this.rotations.freeze();

		assert times.length == scales.length;

		if (scales != null) {
			this.scales = new CompactVector3Array();
			this.scales.add(scales);
			this.scales.freeze();
		}
	}

	/**
	 * @return the array of rotations of this track
	 */
	public Quaternion[] getRotations() {
		return rotations.toObjectArray();
	}

	/**
	 * @return the array of scales for this track
	 */
	public Vector3f[] getScales() {
		return scales == null ? null : scales.toObjectArray();
	}

	/**
	 * @return the arrays of time for this track
	 */
	public float[] getTimes() {
		return times;
	}

	/**
	 * @return the array of translations of this track
	 */
	public Vector3f[] getTranslations() {
		return translations.toObjectArray();
	}

	@Override
	public void write(JmeExporter ex) throws IOException {
		OutputCapsule oc = ex.getCapsule(this);
		oc.write(translations, "translations", null);
		oc.write(rotations, "rotations", null);
		oc.write(times, "times", null);
		oc.write(scales, "scales", null);
	}

	@Override
	public void read(JmeImporter im) throws IOException {
		InputCapsule ic = im.getCapsule(this);
		translations = (CompactVector3Array) ic.readSavable("translations", null);
		rotations = (CompactQuaternionArray) ic.readSavable("rotations", null);
		times = ic.readFloatArray("times", null);
		scales = (CompactVector3Array) ic.readSavable("scales", null);
	}
}

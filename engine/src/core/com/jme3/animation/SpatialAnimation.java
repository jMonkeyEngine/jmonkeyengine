package com.jme3.animation;

import java.io.IOException;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.scene.Spatial;

/**
 * This class animates the whole spatials. All spatial's children are also
 * affected by their parent's movement.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public class SpatialAnimation implements Animation {
	/** The name of the animation. */
	private String name;
	/** The length of the animation. */
	private float length;
	/** The track of the animation. */
	private SpatialTrack track;

	/**
	 * Constructor. Stores the name and length of the animation.
	 * @param name the name of the animation
	 * @param length the length of the animation
	 */
	public SpatialAnimation(String name, float length) {
		this.name = name;
		this.length = length;
	}

	@Override
	public void setTime(float time, float blendAmount, AnimControl control,
			AnimChannel channel) {
		Spatial spatial = control.getSpatial();
		if (spatial != null) {
			track.setTime(time, spatial);
		}
	}

	/**
	 * This method sets the animation track.
	 * @param track the animation track
	 */
	public void setTrack(SpatialTrack track) {
		this.track = track;
	}

	/**
	 * @return the animation track
	 */
	public SpatialTrack getTracks() {
		return track;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public float getLength() {
		return length;
	}

	@Override
	public String toString() {
		return "SpatialAnim[name=" + name + ", length=" + length + "]";
	}

	@Override
	public Animation clone() {
		try {
			return (Animation) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new AssertionError();
		}
	}

	@Override
	public void write(JmeExporter ex) throws IOException {
		OutputCapsule oc = ex.getCapsule(this);
		oc.write(name, "name", null);
		oc.write(length, "length", 0);
		oc.write(track, "track", null);
	}

	@Override
	public void read(JmeImporter im) throws IOException {
		InputCapsule in = im.getCapsule(this);
		name = in.readString("name", null);
		length = in.readFloat("length", 0);
		track = (SpatialTrack) in.readSavable("track", null);
	}
}

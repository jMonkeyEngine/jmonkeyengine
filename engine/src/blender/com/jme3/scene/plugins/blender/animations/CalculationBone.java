package com.jme3.scene.plugins.blender.animations;

import com.jme3.animation.Bone;
import com.jme3.animation.BoneTrack;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.Arrays;

/**
 * The purpose of this class is to imitate bone's movement when calculating inverse kinematics.
 * @author Marcin Roguski (Kaelthas)
 */
public class CalculationBone extends Node {
	private Bone bone;
	/** The bone's tracks. Will be altered at the end of calculation process. */
	private BoneTrack track;
	/** The starting position of the bone. */
	private Vector3f startTranslation;
	/** The starting rotation of the bone. */
	private Quaternion startRotation;
	/** The starting scale of the bone. */
	private Vector3f startScale;
	private Vector3f[] translations;
	private Quaternion[] rotations;
	private Vector3f[] scales;

	public CalculationBone(Bone bone, int boneFramesCount) {
		this.bone = bone;
		this.startRotation = bone.getModelSpaceRotation().clone();
		this.startTranslation = bone.getModelSpacePosition().clone();
		this.startScale = bone.getModelSpaceScale().clone();
		this.reset();
		if(boneFramesCount > 0) {
			this.translations = new Vector3f[boneFramesCount];
			this.rotations = new Quaternion[boneFramesCount];
			this.scales = new Vector3f[boneFramesCount];
			
			Arrays.fill(this.translations, 0, boneFramesCount, this.startTranslation);
			Arrays.fill(this.rotations, 0, boneFramesCount, this.startRotation);
			Arrays.fill(this.scales, 0, boneFramesCount, this.startScale);
		}
	}
	
	/**
	 * Constructor. Stores the track, starting transformation and sets the transformation to the starting positions.
	 * @param bone
	 *        the bone this class will imitate
	 * @param track
	 *        the bone's tracks
	 */
	public CalculationBone(Bone bone, BoneTrack track) {
		this(bone, 0);
		this.track = track;
		this.translations = track.getTranslations();
		this.rotations = track.getRotations();
		this.scales = track.getScales();
	}

	public int getBoneFramesCount() {
		return this.translations==null ? 0 : this.translations.length;
	}
	
	/**
	 * This method returns the end point of the bone. If the bone has parent it is calculated from the start point
	 * of parent to the start point of this bone. If the bone doesn't have a parent the end location is considered
	 * to be 1 point up along Y axis (scale is applied if set to != 1.0);
	 * @return the end point of this bone
	 */
	//TODO: set to Z axis if user defined it this way
	public Vector3f getEndPoint() {
		if (this.getParent() == null) {
			return new Vector3f(0, this.getLocalScale().y, 0);
		} else {
			Node parent = this.getParent();
			return parent.getWorldTranslation().subtract(this.getWorldTranslation()).multLocal(this.getWorldScale());
		}
	}

	/**
	 * This method resets the calculation bone to the starting position.
	 */
	public void reset() {
		this.setLocalTranslation(startTranslation);
		this.setLocalRotation(startRotation);
		this.setLocalScale(startScale);
	}

	@Override
	public int attachChild(Spatial child) {
		if (this.getChildren() != null && this.getChildren().size() > 1) {
			throw new IllegalStateException(this.getClass().getName() + " class instance can only have one child!");
		}
		return super.attachChild(child);
	}

	public Spatial rotate(Quaternion rot, int frame) {
		Spatial spatial = super.rotate(rot);
		this.updateWorldTransforms();
		if (this.getChildren() != null && this.getChildren().size() > 0) {
			CalculationBone child = (CalculationBone) this.getChild(0);
			child.updateWorldTransforms();
		}
		rotations[frame].set(this.getLocalRotation());
		translations[frame].set(this.getLocalTranslation());
		if (scales != null) {
			scales[frame].set(this.getLocalScale());
		}
		return spatial;
	}

	public void applyCalculatedTracks() {
		if(track != null) {
			track.setKeyframes(track.getTimes(), translations, rotations, scales);
		} else {
			bone.setUserControl(true);
			bone.setUserTransforms(translations[0], rotations[0], scales[0]);
			bone.setUserControl(false);
			bone.updateWorldVectors();
		}
	}

	@Override
	public String toString() {
		return bone.getName() + ": " + this.getLocalRotation() + " " + this.getLocalTranslation();
	}
}
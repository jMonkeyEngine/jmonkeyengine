package com.jme3.scene.plugins.blender.constraints;

import com.jme3.animation.Animation;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.animations.Ipo;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.ogre.AnimData;

/**
 * This class represents 'Rot like' constraint type in blender.
 * @author Marcin Roguski (Kaelthas)
 */
/*package*/ class ConstraintRotLike extends Constraint {
	private static final int ROTLIKE_X = 0x01;
	private static final int ROTLIKE_Y = 0x02;
	private static final int ROTLIKE_Z = 0x04;
	private static final int ROTLIKE_X_INVERT = 0x10;
    private static final int ROTLIKE_Y_INVERT = 0x20;
    private static final int ROTLIKE_Z_INVERT = 0x40;
    private static final int ROTLIKE_OFFSET = 0x80;
    
    protected int flag;
    
	/**
	 * This constructor creates the constraint instance.
	 * 
	 * @param constraintStructure
	 *            the constraint's structure (bConstraint clss in blender 2.49).
	 * @param ownerOMA
	 *            the old memory address of the constraint owner
	 * @param influenceIpo
	 *            the ipo curve of the influence factor
	 * @param blenderContext
	 *            the blender context
	 * @throws BlenderFileException
	 *             this exception is thrown when the blender file is somehow
	 *             corrupted
	 */
	public ConstraintRotLike(Structure constraintStructure, Long ownerOMA,
			Ipo influenceIpo, BlenderContext blenderContext) throws BlenderFileException {
		super(constraintStructure, ownerOMA, influenceIpo, blenderContext);
		
		flag = ((Number) data.getFieldValue("flag")).intValue();
	}

	@Override
	protected void bakeConstraint() {
		Object owner = this.owner.getObject();
		AnimData animData = blenderContext.getAnimData(this.owner.getOma());
		if(animData != null) {
			Transform targetTransform = this.target.getTransform();
			Quaternion targetRotation = targetTransform.getRotation();
			for(Animation animation : animData.anims) {
				BlenderTrack track = this.getTrack(owner, animData.skeleton, animation);
				float[] targetAngles = targetRotation.toAngles(null);
				Quaternion[] rotations = track.getRotations();
				int maxFrames = rotations.length;
				float[] angles = new float[3];
				for (int frame = 0; frame < maxFrames; ++frame) {
					rotations[frame].toAngles(angles);
					this.rotLike(rotations[frame], angles, targetAngles, ipo.calculateValue(frame));
				}
				track.setKeyframes(track.getTimes(), track.getTranslations(), rotations, track.getScales());
			}
		}
		
		if(owner instanceof Spatial) {
			Transform targetTransform = this.target.getTransform();
			Transform ownerTransform = this.owner.getTransform();
			Quaternion ownerRotation = ownerTransform.getRotation();
			this.rotLike(ownerRotation, ownerRotation.toAngles(null), targetTransform.getRotation().toAngles(null), ipo.calculateValue(0));
			this.owner.applyTransform(ownerTransform);
		}
	}
	
	private void rotLike(Quaternion ownerRotation, float[] ownerAngles, float[] targetAngles, float influence) {
		Quaternion startRotation = ownerRotation.clone();
		Quaternion offset = Quaternion.IDENTITY;
		if ((flag & ROTLIKE_OFFSET) != 0) {//we add the original rotation to the copied rotation
			offset = startRotation;
		}

		if ((flag & ROTLIKE_X) != 0) {
			ownerAngles[0] = targetAngles[0];
			if ((flag & ROTLIKE_X_INVERT) != 0) {
				ownerAngles[0] = -ownerAngles[0];
			}
		}
		if ((flag & ROTLIKE_Y) != 0) {
			ownerAngles[1] = targetAngles[1];
			if ((flag & ROTLIKE_Y_INVERT) != 0) {
				ownerAngles[1] = -ownerAngles[1];
			}
		}
		if ((flag & ROTLIKE_Z) != 0) {
			ownerAngles[2] = targetAngles[2];
			if ((flag & ROTLIKE_Z_INVERT) != 0) {
				ownerAngles[2] = -ownerAngles[2];
			}
		}
		ownerRotation.fromAngles(ownerAngles).multLocal(offset);

		if(influence < 1.0f) {
			
//			startLocation.subtractLocal(ownerLocation).normalizeLocal().mult(influence);
//			ownerLocation.addLocal(startLocation);
			//TODO
		}
	}
}

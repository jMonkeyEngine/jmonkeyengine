package com.jme3.scene.plugins.blender.constraints;

import com.jme3.animation.Animation;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.animations.Ipo;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.ogre.AnimData;

/**
 * This class represents 'Size like' constraint type in blender.
 * @author Marcin Roguski (Kaelthas)
 */
/*package*/ class ConstraintSizeLike extends Constraint {
	private static final int SIZELIKE_X = 0x01;
	private static final int SIZELIKE_Y = 0x02;
	private static final int SIZELIKE_Z = 0x04;
	private static final int LOCLIKE_OFFSET = 0x80;
    
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
	public ConstraintSizeLike(Structure constraintStructure, Long ownerOMA,
			Ipo influenceIpo, BlenderContext blenderContext) throws BlenderFileException {
		super(constraintStructure, ownerOMA, influenceIpo, blenderContext);
		
		flag = ((Number) data.getFieldValue("flag")).intValue();
		if(blenderContext.getBlenderKey().isFixUpAxis()) {
			//swapping Y and X limits flag in the bitwise flag
			int y = flag & SIZELIKE_Y;
			int z = flag & SIZELIKE_Z;
			flag &= SIZELIKE_X | LOCLIKE_OFFSET;//clear the other flags to swap them
			flag |= y << 1;
			flag |= z >> 1;
		}
	}

	@Override
	protected void bakeConstraint() {
		Object owner = this.owner.getObject();
		AnimData animData = blenderContext.getAnimData(this.owner.getOma());
		if(animData != null) {
			Transform targetTransform = this.target.getTransform();
			Vector3f targetScale = targetTransform.getScale();
			for(Animation animation : animData.anims) {
				BlenderTrack track = this.getTrack(owner, animData.skeleton, animation);
				Vector3f[] scales = track.getScales();
				int maxFrames = scales.length;
				for (int frame = 0; frame < maxFrames; ++frame) {
					this.sizeLike(scales[frame], targetScale, ipo.calculateValue(frame));
				}
				track.setKeyframes(track.getTimes(), track.getTranslations(), track.getRotations(), scales);
			}
		}
		
		if(owner instanceof Spatial) {
			Transform targetTransform = this.target.getTransform();
			Transform ownerTransform = this.owner.getTransform();
			this.sizeLike(ownerTransform.getScale(), targetTransform.getScale(), ipo.calculateValue(0));
			this.owner.applyTransform(ownerTransform);
		}
	}
	
	private void sizeLike(Vector3f ownerScale, Vector3f targetScale, float influence) {
		Vector3f offset = Vector3f.ZERO;
		if ((flag & LOCLIKE_OFFSET) != 0) {//we add the original scale to the copied scale
			offset = ownerScale.clone();
		}

		if ((flag & SIZELIKE_X) != 0) {
			ownerScale.x = targetScale.x * influence + (1.0f - influence) * ownerScale.x;
		}
		if ((flag & SIZELIKE_Y) != 0) {
			ownerScale.y = targetScale.y * influence + (1.0f - influence) * ownerScale.y;
		}
		if ((flag & SIZELIKE_Z) != 0) {
			ownerScale.z = targetScale.z * influence + (1.0f - influence) * ownerScale.z;
		}
		ownerScale.addLocal(offset);
	}
}

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
 * This class represents 'Loc like' constraint type in blender.
 * @author Marcin Roguski (Kaelthas)
 */
/*package*/ class ConstraintLocLike extends Constraint {
	private static final int LOCLIKE_X = 0x01;
	private static final int LOCLIKE_Y = 0x02;
	private static final int LOCLIKE_Z = 0x04;
    //protected static final int LOCLIKE_TIP = 0x08;//this is deprecated in blender
    private static final int LOCLIKE_X_INVERT = 0x10;
    private static final int LOCLIKE_Y_INVERT = 0x20;
    private static final int LOCLIKE_Z_INVERT = 0x40;
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
	public ConstraintLocLike(Structure constraintStructure, Long ownerOMA,
			Ipo influenceIpo, BlenderContext blenderContext) throws BlenderFileException {
		super(constraintStructure, ownerOMA, influenceIpo, blenderContext);
		
		flag = ((Number) data.getFieldValue("flag")).intValue();
		
		if(blenderContext.getBlenderKey().isFixUpAxis()) {
			//swapping Y and X limits flag in the bitwise flag
			int y = flag & LOCLIKE_Y;
			int invY = flag & LOCLIKE_Y_INVERT;
			int z = flag & LOCLIKE_Z;
			int invZ = flag & LOCLIKE_Z_INVERT;
			flag &= LOCLIKE_X | LOCLIKE_X_INVERT | LOCLIKE_OFFSET;//clear the other flags to swap them
			flag |= y << 2;
			flag |= invY << 2;
			flag |= z >> 2;
			flag |= invZ >> 2;
		}
	}

	@Override
	protected void bakeConstraint() {
		Object owner = this.owner.getObject();
		AnimData animData = blenderContext.getAnimData(this.owner.getOma());
		if(animData != null) {
			Transform targetTransform = this.target.getTransform();
			for(Animation animation : animData.anims) {
				BlenderTrack blenderTrack = this.getTrack(owner, animData.skeleton, animation);
				Vector3f[] translations = blenderTrack.getTranslations();
				int maxFrames = translations.length;
				for (int frame = 0; frame < maxFrames; ++frame) {
					this.locLike(translations[frame], targetTransform.getTranslation(), ipo.calculateValue(frame));
				}
				blenderTrack.setKeyframes(blenderTrack.getTimes(), translations, blenderTrack.getRotations(), blenderTrack.getScales());
			}
		}
		
		if(owner instanceof Spatial) {
			Transform targetTransform = this.target.getTransform();
			Transform ownerTransform = this.owner.getTransform();
			Vector3f ownerLocation = ownerTransform.getTranslation();
			this.locLike(ownerLocation, targetTransform.getTranslation(), ipo.calculateValue(0));
			this.owner.applyTransform(ownerTransform);
		}
	}
	
	private void locLike(Vector3f ownerLocation, Vector3f targetLocation, float influence) {
		Vector3f startLocation = ownerLocation.clone();
		Vector3f offset = Vector3f.ZERO;
		if ((flag & LOCLIKE_OFFSET) != 0) {//we add the original location to the copied location
			offset = startLocation;
		}

		if ((flag & LOCLIKE_X) != 0) {
			ownerLocation.x = targetLocation.x;
			if ((flag & LOCLIKE_X_INVERT) != 0) {
				ownerLocation.x = -ownerLocation.x;
			}
		}
		if ((flag & LOCLIKE_Y) != 0) {
			ownerLocation.y = targetLocation.y;
			if ((flag & LOCLIKE_Y_INVERT) != 0) {
				ownerLocation.y = -ownerLocation.y;
			}
		}
		if ((flag & LOCLIKE_Z) != 0) {
			ownerLocation.z = targetLocation.z;
			if ((flag & LOCLIKE_Z_INVERT) != 0) {
				ownerLocation.z = -ownerLocation.z;
			}
		}
		ownerLocation.addLocal(offset);
		
		if(influence < 1.0f) {
			startLocation.subtractLocal(ownerLocation).normalizeLocal().mult(influence);
			ownerLocation.addLocal(startLocation);
		}
	}
}

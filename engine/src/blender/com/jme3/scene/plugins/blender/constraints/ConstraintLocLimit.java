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
 * This class represents 'Loc limit' constraint type in blender.
 * @author Marcin Roguski (Kaelthas)
 */
/*package*/ class ConstraintLocLimit extends Constraint {
    private static final int LIMIT_XMIN = 0x01;
    private static final int LIMIT_XMAX = 0x02;
    private static final int LIMIT_YMIN = 0x04;
    private static final int LIMIT_YMAX = 0x08;
    private static final int LIMIT_ZMIN = 0x10;
    private static final int LIMIT_ZMAX = 0x20;
    
    protected float[][] limits = new float[3][2];
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
	public ConstraintLocLimit(Structure constraintStructure, Long ownerOMA,
			Ipo influenceIpo, BlenderContext blenderContext) throws BlenderFileException {
		super(constraintStructure, ownerOMA, influenceIpo, blenderContext);
		
		flag = ((Number) data.getFieldValue("flag")).intValue();
		if(blenderContext.getBlenderKey().isFixUpAxis()) {
			limits[0][0] = ((Number) data.getFieldValue("xmin")).floatValue();
			limits[0][1] = ((Number) data.getFieldValue("xmax")).floatValue();
			limits[2][0] = -((Number) data.getFieldValue("ymin")).floatValue();
			limits[2][1] = -((Number) data.getFieldValue("ymax")).floatValue();
			limits[1][0] = ((Number) data.getFieldValue("zmin")).floatValue();
			limits[1][1] = ((Number) data.getFieldValue("zmax")).floatValue();
			
			//swapping Y and X limits flag in the bitwise flag
			int ymin = flag & LIMIT_YMIN;
			int ymax = flag & LIMIT_YMAX;
			int zmin = flag & LIMIT_ZMIN;
			int zmax = flag & LIMIT_ZMAX;
			flag &= LIMIT_XMIN | LIMIT_XMAX;//clear the other flags to swap them
			flag |= ymin << 2;
			flag |= ymax << 2;
			flag |= zmin >> 2;
			flag |= zmax >> 2;
		} else {
			limits[0][0] = ((Number) data.getFieldValue("xmin")).floatValue();
			limits[0][1] = ((Number) data.getFieldValue("xmax")).floatValue();
			limits[1][0] = ((Number) data.getFieldValue("ymin")).floatValue();
			limits[1][1] = ((Number) data.getFieldValue("ymax")).floatValue();
			limits[2][0] = ((Number) data.getFieldValue("zmin")).floatValue();
			limits[2][1] = ((Number) data.getFieldValue("zmax")).floatValue();
		}
	}

	@Override
	protected void bakeConstraint() {
		Object owner = this.owner.getObject();
		AnimData animData = blenderContext.getAnimData(this.owner.getOma());
		if(animData != null) {
			for(Animation animation : animData.anims) {
				BlenderTrack track = this.getTrack(owner, animData.skeleton, animation);
				Vector3f[] translations = track.getTranslations();
				int maxFrames = translations.length;
				for (int frame = 0; frame < maxFrames; ++frame) {
					this.locLimit(translations[frame], ipo.calculateValue(frame));
				}
				track.setKeyframes(track.getTimes(), translations, track.getRotations(), track.getScales());
			}
		}
		
		if(owner instanceof Spatial) {
			Transform ownerTransform = this.owner.getTransform();
			Vector3f ownerLocation = ownerTransform.getTranslation();
			this.locLimit(ownerLocation, ipo.calculateValue(0));
			this.owner.applyTransform(ownerTransform);
		}
	}
	
	/**
	 * This method modifies the given translation.
	 * @param translation the translation to be modified.
	 * @param influence the influence value
	 */
	private void locLimit(Vector3f translation, float influence) {
		if ((flag & LIMIT_XMIN) != 0) {
			if (translation.x < limits[0][0]) {
				translation.x -= (translation.x - limits[0][0]) * influence;
			}
		}
		if ((flag & LIMIT_XMAX) != 0) {
			if (translation.x > limits[0][1]) {
				translation.x -= (translation.x - limits[0][1]) * influence;
			}
		}
		if ((flag & LIMIT_YMIN) != 0) {
			if (translation.y < limits[1][0]) {
				translation.y -= (translation.y - limits[1][0]) * influence;
			}
		}
		if ((flag & LIMIT_YMAX) != 0) {
			if (translation.y > limits[1][1]) {
				translation.y -= (translation.y - limits[1][1]) * influence;
			}
		}
		if ((flag & LIMIT_ZMIN) != 0) {
			if (translation.z < limits[2][0]) {
				translation.z -= (translation.z - limits[2][0]) * influence;
			}
		}
		if ((flag & LIMIT_ZMAX) != 0) {
			if (translation.z > limits[2][1]) {
				translation.z -= (translation.z - limits[2][1]) * influence;
			}
		}
	}
}

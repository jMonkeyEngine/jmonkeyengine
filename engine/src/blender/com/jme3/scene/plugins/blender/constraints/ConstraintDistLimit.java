package com.jme3.scene.plugins.blender.constraints;

import com.jme3.animation.Animation;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Track;
import com.jme3.math.Vector3f;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.animations.Ipo;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Structure;

/**
 * This class represents 'Dist limit' constraint type in blender.
 * @author Marcin Roguski (Kaelthas)
 */
/*package*/ class ConstraintDistLimit extends Constraint {
	private static final int LIMITDIST_INSIDE = 0;
	private static final int LIMITDIST_OUTSIDE = 1;
	private static final int LIMITDIST_ONSURFACE = 2;
    
    /**
	 * This constructor creates the constraint instance.
	 * 
	 * @param constraintStructure
	 *            the constraint's structure (bConstraint clss in blender 2.49).
	 * @param boneOMA
	 *            the old memory address of the constraint owner
	 * @param influenceIpo
	 *            the ipo curve of the influence factor
	 * @param blenderContext
	 *            the blender context
	 * @throws BlenderFileException
	 *             this exception is thrown when the blender file is somehow
	 *             corrupted
	 */
	public ConstraintDistLimit(Structure constraintStructure, Long boneOMA,
			Ipo influenceIpo, BlenderContext blenderContext) throws BlenderFileException {
		super(constraintStructure, boneOMA, influenceIpo, blenderContext);
	}

	@Override
	public void affectAnimation(Animation animation, int targetIndex) {
		Vector3f targetLocation = this.getTargetLocation();
		BoneTrack boneTrack = (BoneTrack) this.getTrack(animation, targetIndex);
		if (boneTrack != null) {
			//TODO: target vertex group !!!
			float dist = ((Number) data.getFieldValue("dist")).floatValue();
			int mode = ((Number) data.getFieldValue("mode")).intValue();

			int maxFrames = boneTrack.getTimes().length;
			Vector3f[] translations = boneTrack.getTranslations();
			for (int frame = 0; frame < maxFrames; ++frame) {
				Vector3f v = translations[frame].subtract(targetLocation);
				float currentDistance = v.length();
				float influence = ipo.calculateValue(frame);
				float modifier = 0.0f;
				switch (mode) {
					case LIMITDIST_INSIDE:
						if (currentDistance >= dist) {
							modifier = (dist - currentDistance) / currentDistance;
						}
						break;
					case LIMITDIST_ONSURFACE:
						modifier = (dist - currentDistance) / currentDistance;
						break;
					case LIMITDIST_OUTSIDE:
						if (currentDistance <= dist) {
							modifier = (dist - currentDistance) / currentDistance;
						}
						break;
					default:
						throw new IllegalStateException("Unknown distance limit constraint mode: " + mode);
				}
				translations[frame].addLocal(v.multLocal(modifier * influence));
			}
			boneTrack.setKeyframes(boneTrack.getTimes(), translations, boneTrack.getRotations(), boneTrack.getScales());
		}
	}
	
	@Override
	public ConstraintType getType() {
		return ConstraintType.CONSTRAINT_TYPE_DISTLIMIT;
	}
}

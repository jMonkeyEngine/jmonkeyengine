package com.jme3.scene.plugins.blender.constraints;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import com.jme3.animation.Animation;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.animations.Ipo;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.ogre.AnimData;

/**
 * This class represents 'Shrink wrap' constraint type in blender.
 * @author Marcin Roguski (Kaelthas)
 */
/*package*/ class ConstraintShrinkWrap extends Constraint {
	
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
	public ConstraintShrinkWrap(Structure constraintStructure, Long ownerOMA,
			Ipo influenceIpo, BlenderContext blenderContext) throws BlenderFileException {
		super(constraintStructure, ownerOMA, influenceIpo, blenderContext);
	}

	@Override
	protected void bakeConstraint() {
		//loading mesh points (blender ensures that the target is a mesh-object)
		List<Vector3f> pts = new ArrayList<Vector3f>();
		Node target = (Node) this.target.getObject();
		for(Spatial spatial : target.getChildren()) {
			if(spatial instanceof Geometry) {
				Mesh mesh = ((Geometry) spatial).getMesh();
				FloatBuffer floatBuffer = mesh.getFloatBuffer(Type.Position);
				for(int i=0;i<floatBuffer.limit();i+=3) {
					pts.add(new Vector3f(floatBuffer.get(i), floatBuffer.get(i + 1), floatBuffer.get(i + 2)));
				}
			}
		}
		
		AnimData animData = blenderContext.getAnimData(this.owner.getOma());
		if(animData != null) {
			Object owner = this.owner.getObject();
			for(Animation animation : animData.anims) {
				BlenderTrack track = this.getTrack(owner, animData.skeleton, animation);
				Vector3f[] translations = track.getTranslations();
				Quaternion[] rotations = track.getRotations();
				int maxFrames = translations.length;
				for (int frame = 0; frame < maxFrames; ++frame) {
					Vector3f currentTranslation = translations[frame];
					
					//looking for minimum distanced point
					Vector3f minDistancePoint = null;
					float distance = Float.MAX_VALUE;
					for(Vector3f p : pts) {
						float temp = currentTranslation.distance(p);
						if(temp < distance) {
							distance = temp;
							minDistancePoint = p;
						}
					}
					translations[frame] = minDistancePoint.clone();
				}
				
				track.setKeyframes(track.getTimes(), translations, rotations, track.getScales());
			}
		}
		
		//TODO: static constraint for spatials
	}
}

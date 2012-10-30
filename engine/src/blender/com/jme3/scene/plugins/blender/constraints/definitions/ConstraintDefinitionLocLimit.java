package com.jme3.scene.plugins.blender.constraints.definitions;

import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.file.Structure;

/**
 * This class represents 'Loc limit' constraint type in blender.
 * @author Marcin Roguski (Kaelthas)
 */
/*package*/ class ConstraintDefinitionLocLimit extends ConstraintDefinition {
	private static final int LIMIT_XMIN = 0x01;
    private static final int LIMIT_XMAX = 0x02;
    private static final int LIMIT_YMIN = 0x04;
    private static final int LIMIT_YMAX = 0x08;
    private static final int LIMIT_ZMIN = 0x10;
    private static final int LIMIT_ZMAX = 0x20;
    
    protected float[][] limits = new float[3][2];
    
    public ConstraintDefinitionLocLimit(Structure constraintData, BlenderContext blenderContext) {
		super(constraintData, blenderContext);
		if(blenderContext.getBlenderKey().isFixUpAxis()) {
			limits[0][0] = ((Number) constraintData.getFieldValue("xmin")).floatValue();
			limits[0][1] = ((Number) constraintData.getFieldValue("xmax")).floatValue();
			limits[2][0] = -((Number) constraintData.getFieldValue("ymin")).floatValue();
			limits[2][1] = -((Number) constraintData.getFieldValue("ymax")).floatValue();
			limits[1][0] = ((Number) constraintData.getFieldValue("zmin")).floatValue();
			limits[1][1] = ((Number) constraintData.getFieldValue("zmax")).floatValue();
			
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
			limits[0][0] = ((Number) constraintData.getFieldValue("xmin")).floatValue();
			limits[0][1] = ((Number) constraintData.getFieldValue("xmax")).floatValue();
			limits[1][0] = ((Number) constraintData.getFieldValue("ymin")).floatValue();
			limits[1][1] = ((Number) constraintData.getFieldValue("ymax")).floatValue();
			limits[2][0] = ((Number) constraintData.getFieldValue("zmin")).floatValue();
			limits[2][1] = ((Number) constraintData.getFieldValue("zmax")).floatValue();
		}
	}
	
	@Override
	public void bake(Transform ownerTransform, Transform targetTransform, float influence) {
		Vector3f translation = ownerTransform.getTranslation();
		
		if ((flag & LIMIT_XMIN) != 0 && translation.x < limits[0][0]) {
			translation.x -= (translation.x - limits[0][0]) * influence;
		}
		if ((flag & LIMIT_XMAX) != 0 && translation.x > limits[0][1]) {
			translation.x -= (translation.x - limits[0][1]) * influence;
		}
		if ((flag & LIMIT_YMIN) != 0 && translation.y < limits[1][0]) {
			translation.y -= (translation.y - limits[1][0]) * influence;
		}
		if ((flag & LIMIT_YMAX) != 0 && translation.y > limits[1][1]) {
			translation.y -= (translation.y - limits[1][1]) * influence;
		}
		if ((flag & LIMIT_ZMIN) != 0 && translation.z < limits[2][0]) {
			translation.z -= (translation.z - limits[2][0]) * influence;
		}
		if ((flag & LIMIT_ZMAX) != 0 && translation.z > limits[2][1]) {
			translation.z -= (translation.z - limits[2][1]) * influence;
		}
	}
}

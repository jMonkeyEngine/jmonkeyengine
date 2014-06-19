package com.jme3.scene.plugins.blender.constraints.definitions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.jme3.animation.Bone;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.animations.BoneContext;
import com.jme3.scene.plugins.blender.constraints.ConstraintHelper;
import com.jme3.scene.plugins.blender.constraints.ConstraintHelper.Space;
import com.jme3.scene.plugins.blender.file.Structure;

public class ConstraintDefinitionIK extends ConstraintDefinition {

    private static final int FLAG_POSITION = 0x20;

    /** The number of affected bones. Zero means that all parent bones of the current bone should take part in baking. */
    private int              bonesAffected;
    private float            chainLength;
    private BoneContext[]    bones;
    private boolean          needToCompute = true;

    public ConstraintDefinitionIK(Structure constraintData, Long ownerOMA, BlenderContext blenderContext) {
        super(constraintData, ownerOMA, blenderContext);
        bonesAffected = ((Number) constraintData.getFieldValue("rootbone")).intValue();

        if ((flag & FLAG_POSITION) == 0) {
            needToCompute = false;
        }

        if (needToCompute) {
            alteredOmas = new HashSet<Long>();
        }
    }
    
    @Override
    public void bake(Space ownerSpace, Space targetSpace, Transform targetTransform, float influence) {
        if (needToCompute && influence != 0) {
            ConstraintHelper constraintHelper = blenderContext.getHelper(ConstraintHelper.class);
            BoneContext[] boneContexts = this.getBones();
            float b = chainLength;
            Quaternion boneWorldRotation = new Quaternion();

            for (int i = 0; i < boneContexts.length; ++i) {
                Bone bone = boneContexts[i].getBone();

                bone.updateModelTransforms();
                Transform boneWorldTransform = constraintHelper.getTransform(boneContexts[i].getArmatureObjectOMA(), bone.getName(), Space.CONSTRAINT_SPACE_WORLD);

                Vector3f head = boneWorldTransform.getTranslation();
                Vector3f tail = head.add(bone.getModelSpaceRotation().mult(Vector3f.UNIT_Y.mult(boneContexts[i].getLength())));

                Vector3f vectorA = tail.subtract(head);
                float a = vectorA.length();
                vectorA.normalizeLocal();

                Vector3f vectorC = targetTransform.getTranslation().subtract(head);
                float c = vectorC.length();
                vectorC.normalizeLocal();

                b -= a;
                float theta = 0;

                if (c >= a + b) {
                    theta = vectorA.angleBetween(vectorC);
                } else if (c <= FastMath.abs(a - b) && i < boneContexts.length - 1) {
                    theta = vectorA.angleBetween(vectorC) - FastMath.HALF_PI;
                } else {
                    theta = vectorA.angleBetween(vectorC) - FastMath.acos(-(b * b - a * a - c * c) / (2 * a * c));
                }
                
                theta *= influence;

                if (theta != 0) {
                    Vector3f vectorR = vectorA.cross(vectorC);
                    boneWorldRotation.fromAngleAxis(theta, vectorR);
                    boneWorldTransform.getRotation().multLocal(boneWorldRotation);
                    constraintHelper.applyTransform(boneContexts[i].getArmatureObjectOMA(), bone.getName(), Space.CONSTRAINT_SPACE_WORLD, boneWorldTransform);
                }

                bone.updateModelTransforms();
                alteredOmas.add(boneContexts[i].getBoneOma());
            }
        }
    }

    @Override
    public String getConstraintTypeName() {
        return "Inverse kinematics";
    }

    /**
     * @return the bone contexts of all bones that will be used in this constraint computations
     */
    private BoneContext[] getBones() {
        if (bones == null) {
            List<BoneContext> bones = new ArrayList<BoneContext>();
            Bone bone = (Bone) this.getOwner();
            while (bone != null) {
                BoneContext boneContext = blenderContext.getBoneContext(bone);
                bones.add(0, boneContext);
                chainLength += boneContext.getLength();
                if (bonesAffected != 0 && bones.size() >= bonesAffected) {
                    break;
                }
                bone = bone.getParent();
            }
            this.bones = bones.toArray(new BoneContext[bones.size()]);
        }
        return bones;
    }
}

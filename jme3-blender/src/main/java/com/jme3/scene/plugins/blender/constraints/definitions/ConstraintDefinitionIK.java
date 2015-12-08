package com.jme3.scene.plugins.blender.constraints.definitions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.ejml.simple.SimpleMatrix;

import com.jme3.animation.Bone;
import com.jme3.math.Transform;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.animations.BoneContext;
import com.jme3.scene.plugins.blender.constraints.ConstraintHelper;
import com.jme3.scene.plugins.blender.constraints.ConstraintHelper.Space;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.math.DQuaternion;
import com.jme3.scene.plugins.blender.math.DTransform;
import com.jme3.scene.plugins.blender.math.Matrix;
import com.jme3.scene.plugins.blender.math.Vector3d;

/**
 * A definiotion of a Inverse Kinematics constraint. This implementation uses Jacobian pseudoinverse algorithm.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public class ConstraintDefinitionIK extends ConstraintDefinition {
    private static final float MIN_DISTANCE     = 0.001f;
    private static final float MIN_ANGLE_CHANGE = 0.001f;
    private static final int   FLAG_USE_TAIL    = 0x01;
    private static final int   FLAG_POSITION    = 0x20;

    private BonesChain bones;
    /** The number of affected bones. Zero means that all parent bones of the current bone should take part in baking. */
    private int        bonesAffected;
    /** Indicates if the tail of the bone should be used or not. */
    private boolean    useTail;
    /** The amount of iterations of the algorithm. */
    private int        iterations;
    /** The count of bones' chain. */
    private int        bonesCount = -1;

    public ConstraintDefinitionIK(Structure constraintData, Long ownerOMA, BlenderContext blenderContext) {
        super(constraintData, ownerOMA, blenderContext);
        bonesAffected = ((Number) constraintData.getFieldValue("rootbone")).intValue();
        iterations = ((Number) constraintData.getFieldValue("iterations")).intValue();
        useTail = (flag & FLAG_USE_TAIL) != 0;

        if ((flag & FLAG_POSITION) == 0) {
            trackToBeChanged = false;
        }

        if (trackToBeChanged) {
            alteredOmas = new HashSet<Long>();
        }
    }

    /**
     * Below are the variables that only need to be allocated once for IK constraint instance.
     */
    /** Temporal quaternion. */
    private DQuaternion tempDQuaternion = new DQuaternion();
    /** Temporal matrix column. */
    private Vector3d    col             = new Vector3d();
    /** Effector's position change. */
    private Matrix      deltaP          = new Matrix(3, 1);
    /** The current target position. */
    private Vector3d    target          = new Vector3d();
    /** Rotation vectors for each joint (allocated when we know the size of a bones' chain. */
    private Vector3d[]  rotationVectors;
    /** The Jacobian matrix. Allocated when the bones' chain size is known. */
    private Matrix      J;

    @Override
    public void bake(Space ownerSpace, Space targetSpace, Transform targetTransform, float influence) {
        if (influence == 0 || !trackToBeChanged || targetTransform == null || bonesCount == 0) {
            return;// no need to do anything
        }

        if (bones == null) {
            bones = new BonesChain((Bone) this.getOwner(), useTail, bonesAffected, alteredOmas, blenderContext);
        }
        if (bones.size() == 0) {
            bonesCount = 0;
            return;// no need to do anything
        }
        double distanceFromTarget = Double.MAX_VALUE;
        target.set(targetTransform.getTranslation().x, targetTransform.getTranslation().y, targetTransform.getTranslation().z);

        if (bonesCount < 0) {
            bonesCount = bones.size();
            rotationVectors = new Vector3d[bonesCount];
            for (int i = 0; i < bonesCount; ++i) {
                rotationVectors[i] = new Vector3d();
            }
            J = new Matrix(3, bonesCount);
        }

        BoneContext topBone = bones.get(0);
        for (int i = 0; i < iterations; ++i) {
            DTransform topBoneTransform = bones.getWorldTransform(topBone);
            Vector3d e = topBoneTransform.getTranslation().add(topBoneTransform.getRotation().mult(Vector3d.UNIT_Y).multLocal(topBone.getLength()));// effector
            distanceFromTarget = e.distance(target);
            if (distanceFromTarget <= MIN_DISTANCE) {
                break;
            }

            deltaP.setColumn(0, 0, target.x - e.x, target.y - e.y, target.z - e.z);
            int column = 0;
            for (BoneContext boneContext : bones) {
                DTransform boneWorldTransform = bones.getWorldTransform(boneContext);
                Vector3d j = boneWorldTransform.getTranslation(); // current join position
                Vector3d vectorFromJointToEffector = e.subtract(j);
                vectorFromJointToEffector.cross(target.subtract(j), rotationVectors[column]).normalizeLocal();
                rotationVectors[column].cross(vectorFromJointToEffector, col);
                J.setColumn(col, column++);
            }
            Matrix J_1 = J.pseudoinverse();

            SimpleMatrix deltaThetas = J_1.mult(deltaP);
            if (deltaThetas.elementMaxAbs() < MIN_ANGLE_CHANGE) {
                break;
            }
            for (int j = 0; j < deltaThetas.numRows(); ++j) {
                double angle = deltaThetas.get(j, 0);
                Vector3d rotationVector = rotationVectors[j];

                tempDQuaternion.fromAngleAxis(angle, rotationVector);
                BoneContext boneContext = bones.get(j);
                Bone bone = boneContext.getBone();
                if (bone.equals(this.getOwner())) {
                    if (boneContext.isLockX()) {
                        tempDQuaternion.set(0, tempDQuaternion.getY(), tempDQuaternion.getZ(), tempDQuaternion.getW());
                    }
                    if (boneContext.isLockY()) {
                        tempDQuaternion.set(tempDQuaternion.getX(), 0, tempDQuaternion.getZ(), tempDQuaternion.getW());
                    }
                    if (boneContext.isLockZ()) {
                        tempDQuaternion.set(tempDQuaternion.getX(), tempDQuaternion.getY(), 0, tempDQuaternion.getW());
                    }
                }

                DTransform boneTransform = bones.getWorldTransform(boneContext);
                boneTransform.getRotation().set(tempDQuaternion.mult(boneTransform.getRotation()));
                bones.setWorldTransform(boneContext, boneTransform);
            }
        }

        // applying the results
        for (int i = bonesCount - 1; i >= 0; --i) {
            BoneContext boneContext = bones.get(i);
            DTransform transform = bones.getWorldTransform(boneContext);
            constraintHelper.applyTransform(boneContext.getArmatureObjectOMA(), boneContext.getBone().getName(), Space.CONSTRAINT_SPACE_WORLD, transform.toTransform());
        }
        bones = null;// need to reload them again
    }

    @Override
    public String getConstraintTypeName() {
        return "Inverse kinematics";
    }

    @Override
    public boolean isTargetRequired() {
        return true;
    }

    /**
     * Loaded bones' chain. This class allows to operate on transform matrices that use double precision in computations.
     * Only the final result is being transformed to single precision numbers.
     * 
     * @author Marcin Roguski (Kaelthas)
     */
    private static class BonesChain extends ArrayList<BoneContext> {
        private static final long serialVersionUID = -1850524345643600718L;

        private List<Matrix> localBonesMatrices = new ArrayList<Matrix>();

        public BonesChain(Bone bone, boolean useTail, int bonesAffected, Collection<Long> alteredOmas, BlenderContext blenderContext) {
            if (bone != null) {
                ConstraintHelper constraintHelper = blenderContext.getHelper(ConstraintHelper.class);
                if (!useTail) {
                    bone = bone.getParent();
                }
                while (bone != null && (bonesAffected <= 0 || this.size() < bonesAffected)) {
                    BoneContext boneContext = blenderContext.getBoneContext(bone);
                    this.add(boneContext);
                    alteredOmas.add(boneContext.getBoneOma());

                    Transform transform = constraintHelper.getTransform(boneContext.getArmatureObjectOMA(), boneContext.getBone().getName(), Space.CONSTRAINT_SPACE_WORLD);
                    localBonesMatrices.add(new DTransform(transform).toMatrix());

                    bone = bone.getParent();
                }
                
                if(localBonesMatrices.size() > 0) {
                	// making the matrices describe the local transformation
                    Matrix parentWorldMatrix = localBonesMatrices.get(localBonesMatrices.size() - 1);
                    for(int i=localBonesMatrices.size() - 2;i>=0;--i) {
                    	SimpleMatrix m = parentWorldMatrix.invert().mult(localBonesMatrices.get(i));
                    	parentWorldMatrix = localBonesMatrices.get(i);
                    	localBonesMatrices.set(i, new Matrix(m));
                    }
                }
            }
        }

        public DTransform getWorldTransform(BoneContext bone) {
            int index = this.indexOf(bone);
            return this.getWorldMatrix(index).toTransform();
        }

        public void setWorldTransform(BoneContext bone, DTransform transform) {
            int index = this.indexOf(bone);
            Matrix boneMatrix = transform.toMatrix();

            if (index < this.size() - 1) {
                // computing the current bone local transform
                Matrix parentWorldMatrix = this.getWorldMatrix(index + 1);
                SimpleMatrix m = parentWorldMatrix.invert().mult(boneMatrix);
                boneMatrix = new Matrix(m);
            }
            localBonesMatrices.set(index, boneMatrix);
        }

        public Matrix getWorldMatrix(int index) {
            if (index == this.size() - 1) {
                return new Matrix(localBonesMatrices.get(this.size() - 1));
            }

            SimpleMatrix result = this.getWorldMatrix(index + 1);
            result = result.mult(localBonesMatrices.get(index));
            return new Matrix(result);
        }
    }
}

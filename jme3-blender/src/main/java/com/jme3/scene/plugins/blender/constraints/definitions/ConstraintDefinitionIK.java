package com.jme3.scene.plugins.blender.constraints.definitions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

public class ConstraintDefinitionIK extends ConstraintDefinition {
    private static final float MIN_DISTANCE  = 0.001f;
    private static final int   FLAG_USE_TAIL = 0x01;
    private static final int   FLAG_POSITION = 0x20;

    private BonesChain         bones;
    /** The number of affected bones. Zero means that all parent bones of the current bone should take part in baking. */
    private int                bonesAffected;
    /** Indicates if the tail of the bone should be used or not. */
    private boolean            useTail;
    /** The amount of iterations of the algorithm. */
    private int                iterations;

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

    @Override
    public Set<Long> getAlteredOmas() {
        return bones.alteredOmas;
    }

    @Override
    public void bake(Space ownerSpace, Space targetSpace, Transform targetTransform, float influence) {
        if (influence == 0 || !trackToBeChanged || targetTransform == null) {
            return;// no need to do anything
        }

        DQuaternion q = new DQuaternion();
        Vector3d t = new Vector3d(targetTransform.getTranslation());
        bones = new BonesChain((Bone) this.getOwner(), useTail, bonesAffected, blenderContext);
        if (bones.size() == 0) {
            return;// no need to do anything
        }
        double distanceFromTarget = Double.MAX_VALUE;

        Vector3d target = new Vector3d(targetTransform.getTranslation());
        Vector3d[] rotationVectors = new Vector3d[bones.size()];
        BoneContext topBone = bones.get(0);
        for (int i = 1; i <= iterations; ++i) {
            DTransform topBoneTransform = bones.getWorldTransform(topBone);
            Vector3d e = topBoneTransform.getTranslation().add(topBoneTransform.getRotation().mult(Vector3d.UNIT_Y).multLocal(topBone.getLength()));// effector
            distanceFromTarget = e.distance(t);
            if (distanceFromTarget <= MIN_DISTANCE) {
                break;
            }

            Matrix deltaP = new Matrix(3, 1);
            deltaP.setColumn(target.subtract(e), 0);

            Matrix J = new Matrix(3, bones.size());
            int column = 0;
            for (BoneContext boneContext : bones) {
                DTransform boneWorldTransform = bones.getWorldTransform(boneContext);
                Vector3d j = boneWorldTransform.getTranslation(); // current join position
                Vector3d vectorFromJointToEffector = e.subtract(j);
                rotationVectors[column] = vectorFromJointToEffector.cross(target.subtract(j)).normalize();
                Vector3d col = rotationVectors[column].cross(vectorFromJointToEffector);
                J.setColumn(col, column++);
            }
            Matrix J_1 = J.pseudoinverse();

            SimpleMatrix deltaThetas = J_1.mult(deltaP);

            for (int j = 0; j < deltaThetas.numRows(); ++j) {
                double angle = deltaThetas.get(j, 0);
                Vector3d rotationVector = rotationVectors[j];

                q.fromAngleAxis(angle, rotationVector);
                BoneContext boneContext = bones.get(j);
                Bone bone = boneContext.getBone();
                if (bone.equals(this.getOwner())) {
                    if (boneContext.isLockX()) {
                        q.set(0, q.getY(), q.getZ(), q.getW());
                    }
                    if (boneContext.isLockY()) {
                        q.set(q.getX(), 0, q.getZ(), q.getW());
                    }
                    if (boneContext.isLockZ()) {
                        q.set(q.getX(), q.getY(), 0, q.getW());
                    }
                }

                DTransform boneTransform = bones.getWorldTransform(boneContext);
                boneTransform.getRotation().set(q.mult(boneTransform.getRotation()));
                bones.setWorldTransform(boneContext, boneTransform);
            }
        }

        // applying the results
        for (int i = bones.size() - 1; i >= 0; --i) {
            BoneContext boneContext = bones.get(i);
            DTransform transform = bones.getWorldTransform(boneContext);
            constraintHelper.applyTransform(boneContext.getArmatureObjectOMA(), boneContext.getBone().getName(), Space.CONSTRAINT_SPACE_WORLD, transform.toTransform());
        }
        bones.reset();
    }

    @Override
    public String getConstraintTypeName() {
        return "Inverse kinematics";
    }

    @Override
    public boolean isTrackToBeChanged() {
        if (trackToBeChanged) {
            // need to check the bone structure too (when constructor was called not all of the bones might have been loaded yet)
            // that is why it is also checked here
            bones = new BonesChain((Bone) this.getOwner(), useTail, bonesAffected, blenderContext);
            trackToBeChanged = bones.size() > 0;
        }
        return trackToBeChanged;
    }

    @Override
    public boolean isTargetRequired() {
        return true;
    }

    private static class BonesChain extends ArrayList<BoneContext> {
        private static final long serialVersionUID      = -1850524345643600718L;

        private Set<Long>         alteredOmas           = new HashSet<Long>();
        private List<Matrix>    originalBonesMatrices = new ArrayList<Matrix>();
        private List<Matrix>    bonesMatrices         = new ArrayList<Matrix>();

        public BonesChain(Bone bone, boolean useTail, int bonesAffected, BlenderContext blenderContext) {
            if (bone != null) {
                ConstraintHelper constraintHelper = blenderContext.getHelper(ConstraintHelper.class);
                if (!useTail) {
                    bone = bone.getParent();
                }
                while (bone != null && this.size() < bonesAffected) {
                    BoneContext boneContext = blenderContext.getBoneContext(bone);
                    this.add(boneContext);
                    alteredOmas.add(boneContext.getBoneOma());

                    Space space = this.size() < bonesAffected ? Space.CONSTRAINT_SPACE_LOCAL : Space.CONSTRAINT_SPACE_WORLD;
                    Transform transform = constraintHelper.getTransform(boneContext.getArmatureObjectOMA(), boneContext.getBone().getName(), space);
                    originalBonesMatrices.add(new DTransform(transform).toMatrix());

                    bone = bone.getParent();
                }
                this.reset();
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
            bonesMatrices.set(index, boneMatrix);
        }

        public Matrix getWorldMatrix(int index) {
            if (index == this.size() - 1) {
                return new Matrix(bonesMatrices.get(this.size() - 1));
            }

            SimpleMatrix result = this.getWorldMatrix(index + 1);
            result = result.mult(bonesMatrices.get(index));
            return new Matrix(result);
        }

        public void reset() {
            bonesMatrices.clear();
            for (Matrix m : originalBonesMatrices) {
                bonesMatrices.add(new Matrix(m));
            }
        }
    }
}

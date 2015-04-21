package com.jme3.scene.plugins.blender.constraints.definitions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import com.jme3.animation.Bone;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.animations.BoneContext;
import com.jme3.scene.plugins.blender.constraints.ConstraintHelper.Space;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.math.DQuaternion;
import com.jme3.scene.plugins.blender.math.DTransform;
import com.jme3.scene.plugins.blender.math.Vector3d;

/**
 * The Inverse Kinematics constraint.
 * 
 * @author Wesley Shillingford (wezrule)
 * @author Marcin Roguski (Kaelthas)
 */
public class ConstraintDefinitionIK extends ConstraintDefinition {
    private static final float MIN_DISTANCE  = 0.0001f;
    private static final int   FLAG_USE_TAIL = 0x01;
    private static final int   FLAG_POSITION = 0x20;

    /** The number of affected bones. Zero means that all parent bones of the current bone should take part in baking. */
    private int                bonesAffected;
    /** The total length of the bone chain. Useful for optimisation of computations speed in some cases. */
    private double              chainLength;
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
    public void bake(Space ownerSpace, Space targetSpace, Transform targetTransform, float influence) {
        if (influence == 0 || !trackToBeChanged || targetTransform == null) {
            return;// no need to do anything
        }
        DQuaternion q = new DQuaternion();
        Vector3d t = new Vector3d(targetTransform.getTranslation());
        List<BoneContext> bones = this.loadBones();
        if (bones.size() == 0) {
            return;// no need to do anything
        }
        double distanceFromTarget = Double.MAX_VALUE;

        int iterations = this.iterations;
        if (bones.size() == 1) {
            iterations = 1;// if only one bone is in the chain then only one iteration that will properly rotate it will be needed
        } else {
            // if the target cannot be rached by the bones' chain then the chain will become straight and point towards the target
            // in this case only one iteration will be needed, computed from the root to top bone
            BoneContext rootBone = bones.get(bones.size() - 1);
            Transform rootBoneTransform = constraintHelper.getTransform(rootBone.getArmatureObjectOMA(), rootBone.getBone().getName(), Space.CONSTRAINT_SPACE_WORLD);
            if (t.distance(new Vector3d(rootBoneTransform.getTranslation())) >= chainLength) {
                Collections.reverse(bones);

                for (BoneContext boneContext : bones) {
                    Bone bone = boneContext.getBone();
                    DTransform boneTransform = new DTransform(constraintHelper.getTransform(boneContext.getArmatureObjectOMA(), bone.getName(), Space.CONSTRAINT_SPACE_WORLD));

                    Vector3d e = boneTransform.getTranslation().add(boneTransform.getRotation().mult(Vector3d.UNIT_Y).multLocal(boneContext.getLength()));// effector
                    Vector3d j = boneTransform.getTranslation(); // current join position

                    Vector3d currentDir = e.subtractLocal(j).normalizeLocal();
                    Vector3d target = t.subtract(j).normalizeLocal();
                    double angle = currentDir.angleBetween(target);
                    if (angle != 0) {
                        Vector3d cross = currentDir.crossLocal(target).normalizeLocal();
                        q.fromAngleAxis(angle, cross);
                        
                        if(bone.equals(this.getOwner())) {
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

                        boneTransform.getRotation().set(q.multLocal(boneTransform.getRotation()));
                        constraintHelper.applyTransform(boneContext.getArmatureObjectOMA(), bone.getName(), Space.CONSTRAINT_SPACE_WORLD, boneTransform.toTransform());
                    }
                }

                iterations = 0;
            }
        }

        List<Transform> bestSolution = new ArrayList<Transform>(bones.size());
        double bestSolutionDistance = Double.MAX_VALUE;
        BoneContext topBone = bones.get(0);
        for (int i = 0; i < iterations && distanceFromTarget > MIN_DISTANCE; ++i) {
            for (BoneContext boneContext : bones) {
                Bone bone = boneContext.getBone();
                DTransform topBoneTransform = new DTransform(constraintHelper.getTransform(topBone.getArmatureObjectOMA(), topBone.getBone().getName(), Space.CONSTRAINT_SPACE_WORLD));
                DTransform boneWorldTransform = new DTransform(constraintHelper.getTransform(boneContext.getArmatureObjectOMA(), bone.getName(), Space.CONSTRAINT_SPACE_WORLD));

                Vector3d e = topBoneTransform.getTranslation().addLocal(topBoneTransform.getRotation().mult(Vector3d.UNIT_Y).multLocal(topBone.getLength()));// effector
                Vector3d j = boneWorldTransform.getTranslation(); // current join position

                Vector3d currentDir = e.subtractLocal(j).normalizeLocal();
                Vector3d target = t.subtract(j).normalizeLocal();
                double angle = currentDir.angleBetween(target);
                if (angle != 0) {
                    Vector3d cross = currentDir.crossLocal(target).normalizeLocal();
                    q.fromAngleAxis(angle, cross);

                    if(bone.equals(this.getOwner())) {
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

                    boneWorldTransform.getRotation().set(q.multLocal(boneWorldTransform.getRotation()));
                    constraintHelper.applyTransform(boneContext.getArmatureObjectOMA(), bone.getName(), Space.CONSTRAINT_SPACE_WORLD, boneWorldTransform.toTransform());
                } else {
                    iterations = 0;
                    break;
                }
            }

            DTransform topBoneTransform = new DTransform(constraintHelper.getTransform(topBone.getArmatureObjectOMA(), topBone.getBone().getName(), Space.CONSTRAINT_SPACE_WORLD));
            Vector3d e = topBoneTransform.getTranslation().addLocal(topBoneTransform.getRotation().mult(Vector3d.UNIT_Y).multLocal(topBone.getLength()));// effector
            distanceFromTarget = e.distance(t);
            
            if(distanceFromTarget < bestSolutionDistance) {
                bestSolutionDistance = distanceFromTarget;
                bestSolution.clear();
                for(BoneContext boneContext : bones) {
                    bestSolution.add(constraintHelper.getTransform(boneContext.getArmatureObjectOMA(), boneContext.getBone().getName(), Space.CONSTRAINT_SPACE_WORLD));
                }
            }
        }
        
        // applying best solution
        for(int i=0;i<bestSolution.size();++i) {
            BoneContext boneContext = bones.get(i);
            constraintHelper.applyTransform(boneContext.getArmatureObjectOMA(), boneContext.getBone().getName(), Space.CONSTRAINT_SPACE_WORLD, bestSolution.get(i));
        }
    }

    @Override
    public String getConstraintTypeName() {
        return "Inverse kinematics";
    }

    /**
     * @return the bone contexts of all bones that will be used in this constraint computations
     */
    private List<BoneContext> loadBones() {
        List<BoneContext> bones = new ArrayList<BoneContext>();
        Bone bone = (Bone) this.getOwner();
        if (bone == null) {
            return bones;
        }
        if (!useTail) {
            bone = bone.getParent();
        }
        chainLength = 0;
        while (bone != null) {
            BoneContext boneContext = blenderContext.getBoneContext(bone);
            chainLength += boneContext.getLength();
            bones.add(boneContext);
            alteredOmas.add(boneContext.getBoneOma());
            if (bonesAffected != 0 && bones.size() >= bonesAffected) {
                break;
            }
            // need to add spaces between bones to the chain length
            Transform boneWorldTransform = constraintHelper.getTransform(boneContext.getArmatureObjectOMA(), boneContext.getBone().getName(), Space.CONSTRAINT_SPACE_WORLD);
            Vector3f boneWorldTranslation = boneWorldTransform.getTranslation();

            bone = bone.getParent();

            if (bone != null) {
                boneContext = blenderContext.getBoneContext(bone);
                Transform parentWorldTransform = constraintHelper.getTransform(boneContext.getArmatureObjectOMA(), boneContext.getBone().getName(), Space.CONSTRAINT_SPACE_WORLD);
                Vector3f parentWorldTranslation = parentWorldTransform.getTranslation();
                chainLength += boneWorldTranslation.distance(parentWorldTranslation);
            }
        }
        return bones;
    }

    @Override
    public boolean isTrackToBeChanged() {
        if (trackToBeChanged) {
            // need to check the bone structure too (when constructor was called not all of the bones might have been loaded yet)
            // that is why it is also checked here
            List<BoneContext> bones = this.loadBones();
            trackToBeChanged = bones.size() > 0;
        }
        return trackToBeChanged;
    }

    @Override
    public boolean isTargetRequired() {
        return true;
    }
}

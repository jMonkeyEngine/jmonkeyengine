package com.jme3.scene.plugins.blender.modifiers;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.scene.Node;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.animations.AnimationHelper;
import com.jme3.scene.plugins.blender.animations.BoneContext;
import com.jme3.scene.plugins.blender.file.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.meshes.TemporalMesh;

/**
 * This modifier allows to add bone animation to the object.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
/* package */class ArmatureModifier extends Modifier {
    private static final Logger LOGGER              = Logger.getLogger(ArmatureModifier.class.getName());

    private static final int    FLAG_VERTEX_GROUPS  = 0x01;
    private static final int    FLAG_BONE_ENVELOPES = 0x02;

    private Skeleton            skeleton;

    /**
     * This constructor reads animation data from the object structore. The
     * stored data is the AnimData and additional data is armature's OMA.
     * 
     * @param objectStructure
     *            the structure of the object
     * @param modifierStructure
     *            the structure of the modifier
     * @param blenderContext
     *            the blender context
     * @throws BlenderFileException
     *             this exception is thrown when the blender file is somehow
     *             corrupted
     */
    public ArmatureModifier(Structure objectStructure, Structure modifierStructure, BlenderContext blenderContext) throws BlenderFileException {
        if (this.validate(modifierStructure, blenderContext)) {
            Pointer pArmatureObject = (Pointer) modifierStructure.getFieldValue("object");
            if (pArmatureObject.isNotNull()) {
                int deformflag = ((Number) modifierStructure.getFieldValue("deformflag")).intValue();
                boolean useVertexGroups = (deformflag & FLAG_VERTEX_GROUPS) != 0;
                boolean useBoneEnvelopes = (deformflag & FLAG_BONE_ENVELOPES) != 0;
                modifying = useBoneEnvelopes || useVertexGroups;
                if (modifying) {// if neither option is used the modifier will not modify anything anyway
                    Structure armatureObject = pArmatureObject.fetchData().get(0);
                    if(blenderContext.getSkeleton(armatureObject.getOldMemoryAddress()) == null) {
                        LOGGER.fine("Creating new skeleton for armature modifier.");
                        Structure armatureStructure = ((Pointer) armatureObject.getFieldValue("data")).fetchData().get(0);
                        List<Structure> bonebase = ((Structure) armatureStructure.getFieldValue("bonebase")).evaluateListBase();
                        List<Bone> bonesList = new ArrayList<Bone>();
                        for (int i = 0; i < bonebase.size(); ++i) {
                            this.buildBones(armatureObject.getOldMemoryAddress(), bonebase.get(i), null, bonesList, objectStructure.getOldMemoryAddress(), blenderContext);
                        }
                        bonesList.add(0, new Bone(""));
                        Bone[] bones = bonesList.toArray(new Bone[bonesList.size()]);
                        skeleton = new Skeleton(bones);
                        blenderContext.setSkeleton(armatureObject.getOldMemoryAddress(), skeleton);
                    } else {
                        skeleton = blenderContext.getSkeleton(armatureObject.getOldMemoryAddress());
                    }                    
                }
            } else {
                modifying = false;
            }
        }
    }

    private void buildBones(Long armatureObjectOMA, Structure boneStructure, Bone parent, List<Bone> result, Long spatialOMA, BlenderContext blenderContext) throws BlenderFileException {
        BoneContext bc = new BoneContext(armatureObjectOMA, boneStructure, blenderContext);
        bc.buildBone(result, spatialOMA, blenderContext);
    }
    
    @Override
    public void postMeshCreationApply(Node node, BlenderContext blenderContext) {
        LOGGER.fine("Applying armature modifier after mesh has been created.");
        AnimationHelper animationHelper = blenderContext.getHelper(AnimationHelper.class);
        animationHelper.applyAnimations(node, skeleton, blenderContext.getBlenderKey().getAnimationMatchMethod());
        node.updateModelBound();
    }

    @Override
    public void apply(Node node, BlenderContext blenderContext) {
        if (invalid) {
            LOGGER.log(Level.WARNING, "Armature modifier is invalid! Cannot be applied to: {0}", node.getName());
        }

        if (modifying) {
            TemporalMesh temporalMesh = this.getTemporalMesh(node);
            if (temporalMesh != null) {
                LOGGER.log(Level.FINE, "Applying armature modifier to: {0}", temporalMesh);
                
                LOGGER.fine("Creating map between bone name and its index.");
                for (int i = 0; i < skeleton.getBoneCount(); ++i) {
                    Bone bone = skeleton.getBone(i);
                    temporalMesh.addBoneIndex(bone.getName(), i);
                }
                temporalMesh.applyAfterMeshCreate(this);
            } else {
                LOGGER.log(Level.WARNING, "Cannot find temporal mesh for node: {0}. The modifier will NOT be applied!", node);
            }
        }
    }
}

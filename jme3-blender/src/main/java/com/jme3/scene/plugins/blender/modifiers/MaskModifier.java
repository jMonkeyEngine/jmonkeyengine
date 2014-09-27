package com.jme3.scene.plugins.blender.modifiers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.scene.Node;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.animations.BoneContext;
import com.jme3.scene.plugins.blender.file.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.meshes.TemporalMesh;

/**
 * This modifier allows to use mask modifier on the object.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
/* package */class MaskModifier extends Modifier {
    private static final Logger LOGGER             = Logger.getLogger(MaskModifier.class.getName());

    private static final int    FLAG_INVERT_MASK   = 0x01;

    private static final int    MODE_VERTEX_GROUP  = 0;
    private static final int    MODE_ARMATURE      = 1;

    private Pointer             pArmatureObject;
    private String              vertexGroupName;
    private boolean             invertMask;

    public MaskModifier(Structure modifierStructure, BlenderContext blenderContext) {
        if (this.validate(modifierStructure, blenderContext)) {
            int flag = ((Number) modifierStructure.getFieldValue("flag")).intValue();
            invertMask = (flag & FLAG_INVERT_MASK) != 0;
            
            int mode = ((Number) modifierStructure.getFieldValue("mode")).intValue();
            if (mode == MODE_VERTEX_GROUP) {
                vertexGroupName = modifierStructure.getFieldValue("vgroup").toString();
                if (vertexGroupName != null && vertexGroupName.length() == 0) {
                    vertexGroupName = null;
                }
            } else if (mode == MODE_ARMATURE) {
                pArmatureObject = (Pointer) modifierStructure.getFieldValue("ob_arm");
            } else {
                LOGGER.log(Level.SEVERE, "Unknown mode type: {0}. Cannot apply modifier: {1}.", new Object[] { mode, modifierStructure.getName() });
                invalid = true;
            }
        }
    }

    @Override
    public void apply(Node node, BlenderContext blenderContext) {
        if (invalid) {
            LOGGER.log(Level.WARNING, "Mirror modifier is invalid! Cannot be applied to: {0}", node.getName());
        } else {
            TemporalMesh temporalMesh = this.getTemporalMesh(node);
            if (temporalMesh != null) {
                List<String> vertexGroupsToRemove = new ArrayList<String>();
                if (vertexGroupName != null) {
                    vertexGroupsToRemove.add(vertexGroupName);
                } else if (pArmatureObject != null && pArmatureObject.isNotNull()) {
                    try {
                        Structure armatureObject = pArmatureObject.fetchData().get(0);

                        Structure armatureStructure = ((Pointer) armatureObject.getFieldValue("data")).fetchData().get(0);
                        List<Structure> bonebase = ((Structure) armatureStructure.getFieldValue("bonebase")).evaluateListBase();
                        vertexGroupsToRemove.addAll(this.readBoneNames(bonebase));
                    } catch (BlenderFileException e) {
                        LOGGER.log(Level.SEVERE, "Cannot load armature object for the mask modifier. Cause: {0}", e.getLocalizedMessage());
                        LOGGER.log(Level.SEVERE, "Mask modifier will NOT be applied to node named: {0}", node.getName());
                    }
                } else {
                    // if the mesh has no vertex groups then remove all verts
                    // if the mesh has at least one vertex group - then do nothing
                    // I have no idea why we should do that, but blender works this way
                    Collection<String> vertexGroupNames = temporalMesh.getVertexGroupNames();
                    if (vertexGroupNames.size() == 0 && !invertMask || vertexGroupNames.size() > 0 && invertMask) {
                        temporalMesh.clear();
                    }
                }

                if (vertexGroupsToRemove.size() > 0) {
                    List<Integer> vertsToBeRemoved = new ArrayList<Integer>();
                    for (int i = 0; i < temporalMesh.getVertexCount(); ++i) {
                        Map<String, Float> vertexGroups = temporalMesh.getVertexGroups(i);
                        boolean hasVertexGroup = false;
                        if(vertexGroups != null) {
                            for (String groupName : vertexGroupsToRemove) {
                                Float weight = vertexGroups.get(groupName);
                                if (weight != null && weight > 0) {
                                    hasVertexGroup = true;
                                    break;
                                }
                            }
                        }

                        if (!hasVertexGroup && !invertMask || hasVertexGroup && invertMask) {
                            vertsToBeRemoved.add(i);
                        }
                    }

                    Collections.reverse(vertsToBeRemoved);
                    for (Integer vertexIndex : vertsToBeRemoved) {
                        temporalMesh.removeVertexAt(vertexIndex);
                    }
                }
            } else {
                LOGGER.log(Level.WARNING, "Cannot find temporal mesh for node: {0}. The modifier will NOT be applied!", node);
            }
        }
    }

    /**
     * Reads the names of the bones from the given bone base.
     * @param boneBase
     *            the list of bone base structures
     * @return a list of bones' names
     * @throws BlenderFileException
     *             is thrown if problems with reading the child bones' bases occur
     */
    private List<String> readBoneNames(List<Structure> boneBase) throws BlenderFileException {
        List<String> result = new ArrayList<String>();
        for (Structure boneStructure : boneBase) {
            int flag = ((Number) boneStructure.getFieldValue("flag")).intValue();
            if ((flag & BoneContext.SELECTED) != 0) {
                result.add(boneStructure.getFieldValue("name").toString());
            }
            List<Structure> childbase = ((Structure) boneStructure.getFieldValue("childbase")).evaluateListBase();
            result.addAll(this.readBoneNames(childbase));
        }
        return result;
    }
}

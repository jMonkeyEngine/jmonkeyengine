package com.jme3.scene.plugins.blender.modifiers;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.scene.Node;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.file.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.meshes.TemporalMesh;

/**
 * The triangulation modifier. It does not take any settings into account so if the result is different than
 * in blender then please apply the modifier before importing.
 * 
 * @author Marcin Roguski
 */
public class TriangulateModifier extends Modifier {
    private static final Logger LOGGER = Logger.getLogger(TriangulateModifier.class.getName());

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
    public TriangulateModifier(Structure objectStructure, Structure modifierStructure, BlenderContext blenderContext) throws BlenderFileException {
        if (this.validate(modifierStructure, blenderContext)) {
            LOGGER.warning("Triangulation modifier does not take modifier options into account. If triangulation result is different" + " than the model in blender please apply the modifier before importing!");
        }
    }

    @Override
    public void apply(Node node, BlenderContext blenderContext) {
        if (invalid) {
            LOGGER.log(Level.WARNING, "Triangulate modifier is invalid! Cannot be applied to: {0}", node.getName());
        }
        TemporalMesh temporalMesh = this.getTemporalMesh(node);
        if (temporalMesh != null) {
            LOGGER.log(Level.FINE, "Applying triangulation modifier to: {0}", temporalMesh);
            temporalMesh.triangulate();
        } else {
            LOGGER.log(Level.WARNING, "Cannot find temporal mesh for node: {0}. The modifier will NOT be applied!", node);
        }
    }
}

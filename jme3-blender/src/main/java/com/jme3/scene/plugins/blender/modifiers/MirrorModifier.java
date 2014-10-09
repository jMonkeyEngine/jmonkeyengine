package com.jme3.scene.plugins.blender.modifiers;

import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.math.Matrix4f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.file.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.meshes.Edge;
import com.jme3.scene.plugins.blender.meshes.Face;
import com.jme3.scene.plugins.blender.meshes.TemporalMesh;
import com.jme3.scene.plugins.blender.objects.ObjectHelper;

/**
 * This modifier allows to array modifier to the object.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
/* package */class MirrorModifier extends Modifier {
    private static final Logger LOGGER                   = Logger.getLogger(MirrorModifier.class.getName());

    private static final int    FLAG_MIRROR_X            = 0x08;
    private static final int    FLAG_MIRROR_Y            = 0x10;
    private static final int    FLAG_MIRROR_Z            = 0x20;
    private static final int    FLAG_MIRROR_U            = 0x02;
    private static final int    FLAG_MIRROR_V            = 0x04;
    private static final int    FLAG_MIRROR_VERTEX_GROUP = 0x40;
    private static final int    FLAG_MIRROR_MERGE        = 0x80;

    private boolean[]           isMirrored;
    private boolean             mirrorU, mirrorV;
    private boolean             merge;
    private float               tolerance;
    private Pointer             pMirrorObject;
    private boolean             mirrorVGroup;

    /**
     * This constructor reads mirror data from the modifier structure. The
     * stored data is a map of parameters for mirror modifier. No additional data
     * is loaded.
     * When the modifier is applied it is necessary to get the newly created node.
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
    public MirrorModifier(Structure modifierStructure, BlenderContext blenderContext) {
        if (this.validate(modifierStructure, blenderContext)) {
            int flag = ((Number) modifierStructure.getFieldValue("flag")).intValue();

            isMirrored = new boolean[] { (flag & FLAG_MIRROR_X) != 0, (flag & FLAG_MIRROR_Y) != 0, (flag & FLAG_MIRROR_Z) != 0 };
            if (blenderContext.getBlenderKey().isFixUpAxis()) {
                boolean temp = isMirrored[1];
                isMirrored[1] = isMirrored[2];
                isMirrored[2] = temp;
            }
            mirrorU = (flag & FLAG_MIRROR_U) != 0;
            mirrorV = (flag & FLAG_MIRROR_V) != 0;
            mirrorVGroup = (flag & FLAG_MIRROR_VERTEX_GROUP) != 0;
            merge = (flag & FLAG_MIRROR_MERGE) == 0;// in this case we use == instead of != (this is not a mistake)

            tolerance = ((Number) modifierStructure.getFieldValue("tolerance")).floatValue();
            pMirrorObject = (Pointer) modifierStructure.getFieldValue("mirror_ob");

            if (mirrorVGroup) {
                LOGGER.warning("Mirroring vertex groups is currently not supported.");
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
                LOGGER.log(Level.FINE, "Applying mirror modifier to: {0}", temporalMesh);
                Vector3f mirrorPlaneCenter = new Vector3f();
                if (pMirrorObject.isNotNull()) {
                    Structure objectStructure;
                    try {
                        objectStructure = pMirrorObject.fetchData().get(0);
                        ObjectHelper objectHelper = blenderContext.getHelper(ObjectHelper.class);
                        Node object = (Node) objectHelper.toObject(objectStructure, blenderContext);
                        if (object != null) {
                            // compute the mirror object coordinates in node's local space
                            mirrorPlaneCenter = this.getWorldMatrix(node).invertLocal().mult(object.getWorldTranslation());
                        }
                    } catch (BlenderFileException e) {
                        LOGGER.log(Level.SEVERE, "Cannot load mirror''s reference object. Cause: {0}", e.getLocalizedMessage());
                        LOGGER.log(Level.SEVERE, "Mirror modifier will not be applied to node named: {0}", node.getName());
                        return;
                    }
                }

                LOGGER.finest("Allocating temporal variables.");
                float d;
                Vector3f mirrorPlaneNormal = new Vector3f();
                Vector3f shiftVector = new Vector3f();

                LOGGER.fine("Mirroring mesh.");
                for (int mirrorIndex = 0; mirrorIndex < 3; ++mirrorIndex) {
                    if (isMirrored[mirrorIndex]) {
                        boolean mirrorAtPoint0 = mirrorPlaneCenter.get(mirrorIndex) == 0;
                        if (!mirrorAtPoint0) {// compute mirror's plane normal vector in node's space
                            mirrorPlaneNormal.set(0, 0, 0).set(mirrorIndex, Math.signum(mirrorPlaneCenter.get(mirrorIndex)));
                        }

                        TemporalMesh mirror = temporalMesh.clone();
                        for (int i = 0; i < mirror.getVertexCount(); ++i) {
                            Vector3f vertex = mirror.getVertices().get(i);
                            Vector3f normal = mirror.getNormals().get(i);

                            if (mirrorAtPoint0) {
                                d = Math.abs(vertex.get(mirrorIndex));
                                shiftVector.set(0, 0, 0).set(mirrorIndex, -vertex.get(mirrorIndex));
                            } else {
                                d = this.computeDistanceFromPlane(vertex, mirrorPlaneCenter, mirrorPlaneNormal);
                                mirrorPlaneNormal.mult(d, shiftVector);
                            }

                            if (merge && d <= tolerance) {
                                vertex.addLocal(shiftVector);
                                normal.set(mirrorIndex, 0);
                                temporalMesh.getVertices().get(i).addLocal(shiftVector);
                                temporalMesh.getNormals().get(i).set(mirrorIndex, 0);
                            } else {
                                vertex.addLocal(shiftVector.multLocal(2));
                                normal.set(mirrorIndex, -normal.get(mirrorIndex));
                            }
                        }

                        // flipping the indexes
                        for (Face face : mirror.getFaces()) {
                            face.flipIndexes();
                        }
                        for (Edge edge : mirror.getEdges()) {
                            edge.flipIndexes();
                        }
                        Collections.reverse(mirror.getPoints());

                        if (mirrorU || mirrorV) {
                            for (Face face : mirror.getFaces()) {
                                face.flipUV(mirrorU, mirrorV);
                            }
                        }

                        temporalMesh.append(mirror);
                    }
                }
            } else {
                LOGGER.log(Level.WARNING, "Cannot find temporal mesh for node: {0}. The modifier will NOT be applied!", node);
            }
        }
    }

    /**
     * Fetches the world matrix transformation of the given node.
     * @param node
     *            the node
     * @return the node's world transformation matrix
     */
    private Matrix4f getWorldMatrix(Node node) {
        Matrix4f result = new Matrix4f();
        result.setTranslation(node.getWorldTranslation());
        result.setRotationQuaternion(node.getWorldRotation());
        result.setScale(node.getWorldScale());
        return result;
    }

    /**
     * The method computes the distance between a point and a plane (described by point in space and normal vector).
     * @param p
     *            the point in the space
     * @param c
     *            mirror's plane center
     * @param n
     *            mirror's plane normal (should be normalized)
     * @return the minimum distance from point to plane
     */
    private float computeDistanceFromPlane(Vector3f p, Vector3f c, Vector3f n) {
        return Math.abs(n.dot(p) - c.dot(n));
    }
}

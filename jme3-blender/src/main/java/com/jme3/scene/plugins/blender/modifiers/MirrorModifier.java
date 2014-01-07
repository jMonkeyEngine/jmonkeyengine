package com.jme3.scene.plugins.blender.modifiers;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.math.Matrix4f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.file.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.objects.ObjectHelper;

/**
 * This modifier allows to array modifier to the object.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
/* package */class MirrorModifier extends Modifier {
    private static final Logger LOGGER            = Logger.getLogger(MirrorModifier.class.getName());

    private static final int    FLAG_MIRROR_X     = 0x08;
    private static final int    FLAG_MIRROR_Y     = 0x10;
    private static final int    FLAG_MIRROR_Z     = 0x20;
    private static final int    FLAG_MIRROR_U     = 0x02;
    private static final int    FLAG_MIRROR_V     = 0x04;
    // private static final int FLAG_MIRROR_VERTEX_GROUP = 0x40;
    private static final int    FLAG_MIRROR_MERGE = 0x80;

    private boolean[]           isMirrored;
    private boolean             mirrorU, mirrorV;
    private boolean             merge;
    private float               tolerance;
    private Pointer             pMirrorObject;

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
            // boolean mirrorVGroup = (flag & FLAG_MIRROR_VERTEX_GROUP) != 0;
            merge = (flag & FLAG_MIRROR_MERGE) == 0;// in this case we use == instead of != (this is not a mistake)

            tolerance = ((Number) modifierStructure.getFieldValue("tolerance")).floatValue();
            pMirrorObject = (Pointer) modifierStructure.getFieldValue("mirror_ob");
        }
    }

    @Override
    public void apply(Node node, BlenderContext blenderContext) {
        if (invalid) {
            LOGGER.log(Level.WARNING, "Mirror modifier is invalid! Cannot be applied to: {0}", node.getName());
        } else {
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
            Vector3f point = new Vector3f();
            Vector3f normal = new Vector3f();
            Set<Integer> modifiedIndexes = new HashSet<Integer>();
            List<Geometry> geometriesToAdd = new ArrayList<Geometry>();
            final char[] mirrorNames = new char[] { 'X', 'Y', 'Z' };

            LOGGER.fine("Mirroring mesh.");
            for (int mirrorIndex = 0; mirrorIndex < 3; ++mirrorIndex) {
                if (isMirrored[mirrorIndex]) {
                    boolean mirrorAtPoint0 = mirrorPlaneCenter.get(mirrorIndex) == 0;
                    if (!mirrorAtPoint0) {// compute mirror's plane normal vector in node's space
                        mirrorPlaneNormal.set(0, 0, 0).set(mirrorIndex, Math.signum(mirrorPlaneCenter.get(mirrorIndex)));
                    }

                    for (Spatial spatial : node.getChildren()) {
                        if (spatial instanceof Geometry) {
                            Mesh mesh = ((Geometry) spatial).getMesh();
                            Mesh clone = mesh.deepClone();

                            LOGGER.log(Level.FINEST, "Fetching buffers of cloned spatial: {0}", spatial.getName());
                            FloatBuffer position = mesh.getFloatBuffer(Type.Position);
                            FloatBuffer bindPosePosition = mesh.getFloatBuffer(Type.BindPosePosition);

                            FloatBuffer clonePosition = clone.getFloatBuffer(Type.Position);
                            FloatBuffer cloneBindPosePosition = clone.getFloatBuffer(Type.BindPosePosition);
                            FloatBuffer cloneNormals = clone.getFloatBuffer(Type.Normal);
                            FloatBuffer cloneBindPoseNormals = clone.getFloatBuffer(Type.BindPoseNormal);
                            Buffer cloneIndexes = clone.getBuffer(Type.Index).getData();

                            for (int i = 0; i < cloneIndexes.limit(); ++i) {
                                int index = cloneIndexes instanceof ShortBuffer ? ((ShortBuffer) cloneIndexes).get(i) : ((IntBuffer) cloneIndexes).get(i);
                                if (!modifiedIndexes.contains(index)) {
                                    modifiedIndexes.add(index);

                                    this.get(clonePosition, index, point);
                                    if (mirrorAtPoint0) {
                                        d = Math.abs(point.get(mirrorIndex));
                                        shiftVector.set(0, 0, 0).set(mirrorIndex, -point.get(mirrorIndex));
                                    } else {
                                        d = this.computeDistanceFromPlane(point, mirrorPlaneCenter, mirrorPlaneNormal);
                                        mirrorPlaneNormal.mult(d, shiftVector);
                                    }

                                    if (merge && d <= tolerance) {
                                        point.addLocal(shiftVector);

                                        this.set(index, point, clonePosition, cloneBindPosePosition, position, bindPosePosition);
                                        if (cloneNormals != null) {
                                            this.get(cloneNormals, index, normal);
                                            normal.set(mirrorIndex, 0);
                                            this.set(index, normal, cloneNormals, cloneBindPoseNormals);
                                        }
                                    } else {
                                        point.addLocal(shiftVector.multLocal(2));

                                        this.set(index, point, clonePosition, cloneBindPosePosition);
                                        if (cloneNormals != null) {
                                            this.get(cloneNormals, index, normal);
                                            normal.set(mirrorIndex, -normal.get(mirrorIndex));
                                            this.set(index, normal, cloneNormals, cloneBindPoseNormals);
                                        }
                                    }
                                }
                            }
                            modifiedIndexes.clear();

                            LOGGER.finer("Flipping index order.");
                            switch (mesh.getMode()) {
                                case Points:
                                    cloneIndexes.flip();
                                    break;
                                case Lines:
                                    for (int i = 0; i < cloneIndexes.limit(); i += 2) {
                                        if (cloneIndexes instanceof ShortBuffer) {
                                            short index = ((ShortBuffer) cloneIndexes).get(i + 1);
                                            ((ShortBuffer) cloneIndexes).put(i + 1, ((ShortBuffer) cloneIndexes).get(i));
                                            ((ShortBuffer) cloneIndexes).put(i, index);
                                        } else {
                                            int index = ((IntBuffer) cloneIndexes).get(i + 1);
                                            ((IntBuffer) cloneIndexes).put(i + 1, ((IntBuffer) cloneIndexes).get(i));
                                            ((IntBuffer) cloneIndexes).put(i, index);
                                        }
                                    }
                                    break;
                                case Triangles:
                                    for (int i = 0; i < cloneIndexes.limit(); i += 3) {
                                        if (cloneIndexes instanceof ShortBuffer) {
                                            short index = ((ShortBuffer) cloneIndexes).get(i + 2);
                                            ((ShortBuffer) cloneIndexes).put(i + 2, ((ShortBuffer) cloneIndexes).get(i + 1));
                                            ((ShortBuffer) cloneIndexes).put(i + 1, index);
                                        } else {
                                            int index = ((IntBuffer) cloneIndexes).get(i + 2);
                                            ((IntBuffer) cloneIndexes).put(i + 2, ((IntBuffer) cloneIndexes).get(i + 1));
                                            ((IntBuffer) cloneIndexes).put(i + 1, index);
                                        }
                                    }
                                    break;
                                default:
                                    throw new IllegalStateException("Invalid mesh mode: " + mesh.getMode());
                            }

                            if (mirrorU && clone.getBuffer(Type.TexCoord) != null) {
                                LOGGER.finer("Mirroring U coordinates.");
                                FloatBuffer cloneUVs = (FloatBuffer) clone.getBuffer(Type.TexCoord).getData();
                                for (int i = 0; i < cloneUVs.limit(); i += 2) {
                                    cloneUVs.put(i, 1.0f - cloneUVs.get(i));
                                }
                            }
                            if (mirrorV && clone.getBuffer(Type.TexCoord) != null) {
                                LOGGER.finer("Mirroring V coordinates.");
                                FloatBuffer cloneUVs = (FloatBuffer) clone.getBuffer(Type.TexCoord).getData();
                                for (int i = 1; i < cloneUVs.limit(); i += 2) {
                                    cloneUVs.put(i, 1.0f - cloneUVs.get(i));
                                }
                            }

                            Geometry geometry = new Geometry(spatial.getName() + " - mirror " + mirrorNames[mirrorIndex], clone);
                            geometry.setMaterial(((Geometry) spatial).getMaterial());
                            geometriesToAdd.add(geometry);
                        }
                    }

                    LOGGER.log(Level.FINE, "Adding {0} geometries to current node.", geometriesToAdd.size());
                    for (Geometry geometry : geometriesToAdd) {
                        node.attachChild(geometry);
                    }
                    geometriesToAdd.clear();
                }
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

    /**
     * Sets the given value (v) into every of the buffers at the given index.
     * The index is cosidered to be an index of a vertex of the mesh.
     * @param index
     *            the index of vertex of the mesh
     * @param value
     *            the value to be set
     * @param buffers
     *            the buffers where the value will be set
     */
    private void set(int index, Vector3f value, FloatBuffer... buffers) {
        index *= 3;
        for (FloatBuffer buffer : buffers) {
            if (buffer != null) {
                buffer.put(index, value.x);
                buffer.put(index + 1, value.y);
                buffer.put(index + 2, value.z);
            }
        }
    }

    /**
     * Fetches the vector's value from the given buffer at specified index.
     * @param buffer
     *            the buffer we get the data from
     * @param index
     *            the index of vertex of the mesh
     * @param store
     *            the vector where the result will be set
     */
    private void get(FloatBuffer buffer, int index, Vector3f store) {
        index *= 3;
        store.x = buffer.get(index);
        store.y = buffer.get(index + 1);
        store.z = buffer.get(index + 2);
    }
}

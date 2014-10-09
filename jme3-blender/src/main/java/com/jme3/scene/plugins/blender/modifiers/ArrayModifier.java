package com.jme3.scene.plugins.blender.modifiers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.bounding.BoundingVolume;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.file.BlenderFileException;
import com.jme3.scene.plugins.blender.file.DynamicArray;
import com.jme3.scene.plugins.blender.file.FileBlockHeader;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.meshes.MeshHelper;
import com.jme3.scene.plugins.blender.meshes.TemporalMesh;
import com.jme3.scene.plugins.blender.objects.ObjectHelper;
import com.jme3.scene.shape.Curve;

/**
 * This modifier allows to array modifier to the object.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
/* package */class ArrayModifier extends Modifier {
    private static final Logger LOGGER = Logger.getLogger(ArrayModifier.class.getName());

    private int                 fittype;
    private int                 count;
    private float               length;
    private float[]             offset;
    private float[]             scale;
    private Pointer             pOffsetObject;
    private Pointer             pStartCap;
    private Pointer             pEndCap;

    /**
     * This constructor reads array data from the modifier structure. The
     * stored data is a map of parameters for array modifier. No additional data
     * is loaded.
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
    @SuppressWarnings("unchecked")
    public ArrayModifier(Structure modifierStructure, BlenderContext blenderContext) throws BlenderFileException {
        if (this.validate(modifierStructure, blenderContext)) {
            fittype = ((Number) modifierStructure.getFieldValue("fit_type")).intValue();
            switch (fittype) {
                case 0:// FIXED COUNT
                    count = ((Number) modifierStructure.getFieldValue("count")).intValue();
                    break;
                case 1:// FIXED LENGTH
                    length = ((Number) modifierStructure.getFieldValue("length")).floatValue();
                    break;
                case 2:// FITCURVE
                    Pointer pCurveOb = (Pointer) modifierStructure.getFieldValue("curve_ob");
                    if (pCurveOb.isNotNull()) {
                        Structure curveStructure = pCurveOb.fetchData().get(0);
                        ObjectHelper objectHelper = blenderContext.getHelper(ObjectHelper.class);
                        Node curveObject = (Node) objectHelper.toObject(curveStructure, blenderContext);
                        Set<Number> referencesToCurveLengths = new HashSet<Number>(curveObject.getChildren().size());
                        for (Spatial spatial : curveObject.getChildren()) {
                            if (spatial instanceof Geometry) {
                                Mesh mesh = ((Geometry) spatial).getMesh();
                                if (mesh instanceof Curve) {
                                    length += ((Curve) mesh).getLength();
                                } else {
                                    // if bevel object has several parts then each mesh will have the same reference
                                    // to length value (and we should use only one)
                                    Number curveLength = spatial.getUserData("curveLength");
                                    if (curveLength != null && !referencesToCurveLengths.contains(curveLength)) {
                                        length += curveLength.floatValue();
                                        referencesToCurveLengths.add(curveLength);
                                    }
                                }
                            }
                        }
                    }
                    fittype = 1;// treat it like FIXED LENGTH
                    break;
                default:
                    assert false : "Unknown array modifier fit type: " + fittype;
            }

            // offset parameters
            int offsettype = ((Number) modifierStructure.getFieldValue("offset_type")).intValue();
            if ((offsettype & 0x01) != 0) {// Constant offset
                DynamicArray<Number> offsetArray = (DynamicArray<Number>) modifierStructure.getFieldValue("offset");
                offset = new float[] { offsetArray.get(0).floatValue(), offsetArray.get(1).floatValue(), offsetArray.get(2).floatValue() };
            }
            if ((offsettype & 0x02) != 0) {// Relative offset
                DynamicArray<Number> scaleArray = (DynamicArray<Number>) modifierStructure.getFieldValue("scale");
                scale = new float[] { scaleArray.get(0).floatValue(), scaleArray.get(1).floatValue(), scaleArray.get(2).floatValue() };
            }
            if ((offsettype & 0x04) != 0) {// Object offset
                pOffsetObject = (Pointer) modifierStructure.getFieldValue("offset_ob");
            }

            // start cap and end cap
            pStartCap = (Pointer) modifierStructure.getFieldValue("start_cap");
            pEndCap = (Pointer) modifierStructure.getFieldValue("end_cap");
        }
    }

    @Override
    public void apply(Node node, BlenderContext blenderContext) {
        if (invalid) {
            LOGGER.log(Level.WARNING, "Array modifier is invalid! Cannot be applied to: {0}", node.getName());
        } else {
            TemporalMesh temporalMesh = this.getTemporalMesh(node);
            if (temporalMesh != null) {
                LOGGER.log(Level.FINE, "Applying array modifier to: {0}", temporalMesh);
                if (offset == null) {// the node will be repeated several times in the same place
                    offset = new float[] { 0.0f, 0.0f, 0.0f };
                }
                if (scale == null) {// the node will be repeated several times in the same place
                    scale = new float[] { 0.0f, 0.0f, 0.0f };
                } else {
                    // getting bounding box
                    temporalMesh.updateModelBound();
                    BoundingVolume boundingVolume = temporalMesh.getWorldBound();
                    if (boundingVolume instanceof BoundingBox) {
                        scale[0] *= ((BoundingBox) boundingVolume).getXExtent() * 2.0f;
                        scale[1] *= ((BoundingBox) boundingVolume).getYExtent() * 2.0f;
                        scale[2] *= ((BoundingBox) boundingVolume).getZExtent() * 2.0f;
                    } else if (boundingVolume instanceof BoundingSphere) {
                        float radius = ((BoundingSphere) boundingVolume).getRadius();
                        scale[0] *= radius * 2.0f;
                        scale[1] *= radius * 2.0f;
                        scale[2] *= radius * 2.0f;
                    } else {
                        throw new IllegalStateException("Unknown bounding volume type: " + boundingVolume.getClass().getName());
                    }
                }

                // adding object's offset
                float[] objectOffset = new float[] { 0.0f, 0.0f, 0.0f };
                if (pOffsetObject != null && pOffsetObject.isNotNull()) {
                    FileBlockHeader offsetObjectBlock = blenderContext.getFileBlock(pOffsetObject.getOldMemoryAddress());
                    ObjectHelper objectHelper = blenderContext.getHelper(ObjectHelper.class);
                    try {// we take the structure in case the object was not yet loaded
                        Structure offsetStructure = offsetObjectBlock.getStructure(blenderContext);
                        Vector3f translation = objectHelper.getTransformation(offsetStructure, blenderContext).getTranslation();
                        objectOffset[0] = translation.x;
                        objectOffset[1] = translation.y;
                        objectOffset[2] = translation.z;
                    } catch (BlenderFileException e) {
                        LOGGER.log(Level.WARNING, "Problems in blender file structure! Object offset cannot be applied! The problem: {0}", e.getMessage());
                    }
                }

                // getting start and end caps
                MeshHelper meshHelper = blenderContext.getHelper(MeshHelper.class);
                TemporalMesh[] caps = new TemporalMesh[] { null, null };
                Pointer[] pCaps = new Pointer[] { pStartCap, pEndCap };
                for (int i = 0; i < pCaps.length; ++i) {
                    if (pCaps[i].isNotNull()) {
                        FileBlockHeader capBlock = blenderContext.getFileBlock(pCaps[i].getOldMemoryAddress());
                        try {// we take the structure in case the object was not yet loaded
                            Structure capStructure = capBlock.getStructure(blenderContext);
                            Pointer pMesh = (Pointer) capStructure.getFieldValue("data");
                            List<Structure> meshesArray = pMesh.fetchData();
                            caps[i] = meshHelper.toTemporalMesh(meshesArray.get(0), blenderContext);
                        } catch (BlenderFileException e) {
                            LOGGER.log(Level.WARNING, "Problems in blender file structure! Cap object cannot be applied! The problem: {0}", e.getMessage());
                        }
                    }
                }

                Vector3f translationVector = new Vector3f(offset[0] + scale[0] + objectOffset[0], offset[1] + scale[1] + objectOffset[1], offset[2] + scale[2] + objectOffset[2]);
                if (blenderContext.getBlenderKey().isFixUpAxis()) {
                    float y = translationVector.y;
                    translationVector.y = translationVector.z;
                    translationVector.z = y == 0 ? 0 : -y;
                }

                // getting/calculating repeats amount
                int count = 0;
                if (fittype == 0) {// Fixed count
                    count = this.count - 1;
                } else if (fittype == 1) {// Fixed length
                    float length = this.length;
                    if (translationVector.length() > 0.0f) {
                        count = (int) (length / translationVector.length()) - 1;
                    }
                } else if (fittype == 2) {// Fit curve
                    throw new IllegalStateException("Fit curve should be transformed to Fixed Length array type!");
                } else {
                    throw new IllegalStateException("Unknown fit type: " + fittype);
                }

                // adding translated nodes and caps
                Vector3f totalTranslation = new Vector3f(translationVector);
                if (count > 0) {
                    TemporalMesh originalMesh = temporalMesh.clone();
                    for (int i = 0; i < count; ++i) {
                        TemporalMesh clone = originalMesh.clone();
                        for (Vector3f v : clone.getVertices()) {
                            v.addLocal(totalTranslation);
                        }
                        temporalMesh.append(clone);
                        totalTranslation.addLocal(translationVector);
                    }
                }
                if (caps[0] != null) {
                    translationVector.multLocal(-1);
                    TemporalMesh capsClone = caps[0].clone();
                    for (Vector3f v : capsClone.getVertices()) {
                        v.addLocal(translationVector);
                    }
                    temporalMesh.append(capsClone);
                }
                if (caps[1] != null) {
                    TemporalMesh capsClone = caps[1].clone();
                    for (Vector3f v : capsClone.getVertices()) {
                        v.addLocal(totalTranslation);
                    }
                    temporalMesh.append(capsClone);
                }
            } else {
                LOGGER.log(Level.WARNING, "Cannot find temporal mesh for node: {0}. The modifier will NOT be applied!", node);
            }
        }
    }
}

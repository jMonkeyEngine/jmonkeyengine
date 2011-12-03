package com.jme3.scene.plugins.blender.modifiers;

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.bounding.BoundingVolume;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.BlenderContext.LoadedFeatureDataType;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.DynamicArray;
import com.jme3.scene.plugins.blender.file.FileBlockHeader;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.objects.ObjectHelper;
import com.jme3.scene.shape.Curve;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This modifier allows to array modifier to the object.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
/*package*/ class ArrayModifier extends Modifier {
	private static final Logger LOGGER = Logger.getLogger(ArrayModifier.class.getName());
	
	/** Parameters of the modifier. */
	private Map<String, Object> modifierData = new HashMap<String, Object>();
	
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
		if(this.validate(modifierStructure, blenderContext)) {
	        Number fittype = (Number) modifierStructure.getFieldValue("fit_type");
	        modifierData.put("fittype", fittype);
	        switch (fittype.intValue()) {
	            case 0:// FIXED COUNT
	            	modifierData.put("count", modifierStructure.getFieldValue("count"));
	                break;
	            case 1:// FIXED LENGTH
	            	modifierData.put("length", modifierStructure.getFieldValue("length"));
	                break;
	            case 2:// FITCURVE
	                Pointer pCurveOb = (Pointer) modifierStructure.getFieldValue("curve_ob");
	                float length = 0;
	                if (pCurveOb.isNotNull()) {
	                    Structure curveStructure = pCurveOb.fetchData(blenderContext.getInputStream()).get(0);
	                    ObjectHelper objectHelper = blenderContext.getHelper(ObjectHelper.class);
	                    Node curveObject = (Node) objectHelper.toObject(curveStructure, blenderContext);
	                    Set<Number> referencesToCurveLengths = new HashSet<Number>(curveObject.getChildren().size());
	                    for (Spatial spatial : curveObject.getChildren()) {
	                        if (spatial instanceof Geometry) {
	                            Mesh mesh = ((Geometry) spatial).getMesh();
	                            if (mesh instanceof Curve) {
	                                length += ((Curve) mesh).getLength();
	                            } else {
	                                //if bevel object has several parts then each mesh will have the same reference
	                                //to length value (and we should use only one)
	                                Number curveLength = spatial.getUserData("curveLength");
	                                if (curveLength != null && !referencesToCurveLengths.contains(curveLength)) {
	                                    length += curveLength.floatValue();
	                                    referencesToCurveLengths.add(curveLength);
	                                }
	                            }
	                        }
	                    }
	                }
	                modifierData.put("length", Float.valueOf(length));
	                modifierData.put("fittype", Integer.valueOf(1));// treat it like FIXED LENGTH
	                break;
	            default:
	                assert false : "Unknown array modifier fit type: " + fittype;
	        }
	
	        // offset parameters
	        int offsettype = ((Number) modifierStructure.getFieldValue("offset_type")).intValue();
	        if ((offsettype & 0x01) != 0) {// Constant offset
	            DynamicArray<Number> offsetArray = (DynamicArray<Number>) modifierStructure.getFieldValue("offset");
	            float[] offset = new float[]{offsetArray.get(0).floatValue(), offsetArray.get(1).floatValue(), offsetArray.get(2).floatValue()};
	            modifierData.put("offset", offset);
	        }
	        if ((offsettype & 0x02) != 0) {// Relative offset
	            DynamicArray<Number> scaleArray = (DynamicArray<Number>) modifierStructure.getFieldValue("scale");
	            float[] scale = new float[]{scaleArray.get(0).floatValue(), scaleArray.get(1).floatValue(), scaleArray.get(2).floatValue()};
	            modifierData.put("scale", scale);
	        }
	        if ((offsettype & 0x04) != 0) {// Object offset
	            Pointer pOffsetObject = (Pointer) modifierStructure.getFieldValue("offset_ob");
	            if (pOffsetObject.isNotNull()) {
	            	modifierData.put("offsetob", pOffsetObject);
	            }
	        }
	
	        // start cap and end cap
	        Pointer pStartCap = (Pointer) modifierStructure.getFieldValue("start_cap");
	        if (pStartCap.isNotNull()) {
	        	modifierData.put("startcap", pStartCap);
	        }
	        Pointer pEndCap = (Pointer) modifierStructure.getFieldValue("end_cap");
	        if (pEndCap.isNotNull()) {
	        	modifierData.put("endcap", pEndCap);
	        }
		}
	}
	
	@Override
	public Node apply(Node node, BlenderContext blenderContext) {
		if(invalid) {
			LOGGER.log(Level.WARNING, "Array modifier is invalid! Cannot be applied to: {0}", node.getName());
			return node;
		}
        int fittype = ((Number) modifierData.get("fittype")).intValue();
        float[] offset = (float[]) modifierData.get("offset");
        if (offset == null) {// the node will be repeated several times in the same place
            offset = new float[]{0.0f, 0.0f, 0.0f};
        }
        float[] scale = (float[]) modifierData.get("scale");
        if (scale == null) {// the node will be repeated several times in the same place
            scale = new float[]{0.0f, 0.0f, 0.0f};
        } else {
            // getting bounding box
            node.updateModelBound();
            BoundingVolume boundingVolume = node.getWorldBound();
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
        float[] objectOffset = new float[]{0.0f, 0.0f, 0.0f};
        Pointer pOffsetObject = (Pointer) modifierData.get("offsetob");
        if (pOffsetObject != null) {
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
        Node[] caps = new Node[]{null, null};
        Pointer[] pCaps = new Pointer[]{(Pointer) modifierData.get("startcap"), (Pointer) modifierData.get("endcap")};
        for (int i = 0; i < pCaps.length; ++i) {
            if (pCaps[i] != null) {
                caps[i] = (Node) blenderContext.getLoadedFeature(pCaps[i].getOldMemoryAddress(), LoadedFeatureDataType.LOADED_FEATURE);
                if (caps[i] != null) {
                    caps[i] = (Node) caps[i].clone();
                } else {
                    FileBlockHeader capBlock = blenderContext.getFileBlock(pOffsetObject.getOldMemoryAddress());
                    try {// we take the structure in case the object was not yet loaded
                        Structure capStructure = capBlock.getStructure(blenderContext);
                        ObjectHelper objectHelper = blenderContext.getHelper(ObjectHelper.class);
                        caps[i] = (Node) objectHelper.toObject(capStructure, blenderContext);
                        if (caps[i] == null) {
                            LOGGER.log(Level.WARNING, "Cap object ''{0}'' couldn''t be loaded!", capStructure.getName());
                        }
                    } catch (BlenderFileException e) {
                        LOGGER.log(Level.WARNING, "Problems in blender file structure! Cap object cannot be applied! The problem: {0}", e.getMessage());
                    }
                }
            }
        }

        Vector3f translationVector = new Vector3f(offset[0] + scale[0] + objectOffset[0], offset[1] + scale[1] + objectOffset[1], offset[2] + scale[2] + objectOffset[2]);

        // getting/calculating repeats amount
        int count = 0;
        if (fittype == 0) {// Fixed count
            count = ((Number) modifierData.get("count")).intValue() - 1;
        } else if (fittype == 1) {// Fixed length
            float length = ((Number) modifierData.get("length")).floatValue();
            if (translationVector.length() > 0.0f) {
                count = (int) (length / translationVector.length()) - 1;
            }
        } else if (fittype == 2) {// Fit curve
            throw new IllegalStateException("Fit curve should be transformed to Fixed Length array type!");
        } else {
            throw new IllegalStateException("Unknown fit type: " + fittype);
        }

        // adding translated nodes and caps
        if (count > 0) {
            Node[] arrayNodes = new Node[count];
            Vector3f newTranslation = new Vector3f();
            for (int i = 0; i < count; ++i) {
                newTranslation.addLocal(translationVector);
                Node nodeClone = (Node) node.clone();
                nodeClone.setLocalTranslation(newTranslation);
                arrayNodes[i] = nodeClone;
            }
            for (Node nodeClone : arrayNodes) {
                node.attachChild(nodeClone);
            }
            if (caps[0] != null) {
                caps[0].getLocalTranslation().set(node.getLocalTranslation()).subtractLocal(translationVector);
                node.attachChild(caps[0]);
            }
            if (caps[1] != null) {
                caps[1].getLocalTranslation().set(newTranslation).addLocal(translationVector);
                node.attachChild(caps[1]);
            }
        }
        return node;
	}
	
	@Override
	public String getType() {
		return ARRAY_MODIFIER_DATA;
	}
}

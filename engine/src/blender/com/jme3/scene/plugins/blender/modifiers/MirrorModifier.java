package com.jme3.scene.plugins.blender.modifiers;

import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.objects.ObjectHelper;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This modifier allows to array modifier to the object.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
/*package*/ class MirrorModifier extends Modifier {
	private static final Logger LOGGER = Logger.getLogger(MirrorModifier.class.getName());
	
	/** Parameters of the modifier. */
	private Map<String, Object> modifierData = new HashMap<String, Object>();
	
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
		if(this.validate(modifierStructure, blenderContext)) {
			modifierData.put("flag", modifierStructure.getFieldValue("flag"));
			modifierData.put("tolerance", modifierStructure.getFieldValue("tolerance"));
	        Pointer pMirrorOb = (Pointer) modifierStructure.getFieldValue("mirror_ob");
	        if (pMirrorOb.isNotNull()) {
	        	modifierData.put("mirrorob", pMirrorOb);
	        }
		}
	}
	
	@Override
	public Node apply(Node node, BlenderContext blenderContext) {
		if(invalid) {
			LOGGER.log(Level.WARNING, "Mirror modifier is invalid! Cannot be applied to: {0}", node.getName());
			return node;
		}
		
        int flag = ((Number) modifierData.get("flag")).intValue();
        float[] mirrorFactor = new float[]{
            (flag & 0x08) != 0 ? -1.0f : 1.0f,
            (flag & 0x10) != 0 ? -1.0f : 1.0f,
            (flag & 0x20) != 0 ? -1.0f : 1.0f
        };
        float[] center = new float[]{0.0f, 0.0f, 0.0f};
        Pointer pObject = (Pointer) modifierData.get("mirrorob");
        if (pObject != null) {
            Structure objectStructure;
            try {
                objectStructure = pObject.fetchData(blenderContext.getInputStream()).get(0);
                ObjectHelper objectHelper = blenderContext.getHelper(ObjectHelper.class);
                Node object = (Node) objectHelper.toObject(objectStructure, blenderContext);
                if (object != null) {
                    Vector3f translation = object.getWorldTranslation();
                    center[0] = translation.x;
                    center[1] = translation.y;
                    center[2] = translation.z;
                }
            } catch (BlenderFileException e) {
                LOGGER.log(Level.SEVERE, "Cannot load mirror''s reference object. Cause: {0}", e.getLocalizedMessage());
            }
        }
        float tolerance = ((Number) modifierData.get("tolerance")).floatValue();
        boolean mirrorU = (flag & 0x01) != 0;
        boolean mirrorV = (flag & 0x02) != 0;
//		boolean mirrorVGroup = (flag & 0x20) != 0;

        List<Geometry> geometriesToAdd = new ArrayList<Geometry>();
        for (int mirrorIndex = 0; mirrorIndex < 3; ++mirrorIndex) {
            if (mirrorFactor[mirrorIndex] == -1.0f) {
                for (Spatial spatial : node.getChildren()) {
                    if (spatial instanceof Geometry) {
                        Mesh mesh = ((Geometry) spatial).getMesh();
                        Mesh clone = mesh.deepClone();

                        // getting buffers
                        FloatBuffer position = mesh.getFloatBuffer(Type.Position);
                        FloatBuffer bindPosePosition = mesh.getFloatBuffer(Type.BindPosePosition);

                        FloatBuffer clonePosition = clone.getFloatBuffer(Type.Position);
                        FloatBuffer cloneBindPosePosition = clone.getFloatBuffer(Type.BindPosePosition);
                        FloatBuffer cloneNormals = clone.getFloatBuffer(Type.Normal);
                        FloatBuffer cloneBindPoseNormals = clone.getFloatBuffer(Type.BindPoseNormal);
                        IntBuffer cloneIndexes = (IntBuffer) clone.getBuffer(Type.Index).getData();

                        // modyfying data
                        for (int i = mirrorIndex; i < clonePosition.limit(); i += 3) {
                            float value = clonePosition.get(i);
                            float d = center[mirrorIndex] - value;

                            if (Math.abs(d) <= tolerance) {
                                clonePosition.put(i, center[mirrorIndex]);
                                cloneBindPosePosition.put(i, center[mirrorIndex]);
                                position.put(i, center[mirrorIndex]);
                                bindPosePosition.put(i, center[mirrorIndex]);
                            } else {
                                clonePosition.put(i, value + 2.0f * d);
                                cloneBindPosePosition.put(i, value + 2.0f * d);
                            }
                            cloneNormals.put(i, -cloneNormals.get(i));
                            cloneBindPoseNormals.put(i, -cloneNormals.get(i));

                            //modifying clone indexes
                            int vertexIndex = (i - mirrorIndex) / 3;
                            if (vertexIndex % 3 == 0 && vertexIndex<cloneIndexes.limit()) {
                                int index = cloneIndexes.get(vertexIndex + 2);
                                cloneIndexes.put(vertexIndex + 2, cloneIndexes.get(vertexIndex + 1));
                                cloneIndexes.put(vertexIndex + 1, index);
                            }
                        }

                        if (mirrorU) {
                            FloatBuffer cloneUVs = (FloatBuffer) clone.getBuffer(Type.TexCoord).getData();
                            for (int i = 0; i < cloneUVs.limit(); i += 2) {
                                cloneUVs.put(i, 1.0f - cloneUVs.get(i));
                            }
                        }
                        if (mirrorV) {
                            FloatBuffer cloneUVs = (FloatBuffer) clone.getBuffer(Type.TexCoord).getData();
                            for (int i = 1; i < cloneUVs.limit(); i += 2) {
                                cloneUVs.put(i, 1.0f - cloneUVs.get(i));
                            }
                        }

                        Geometry geometry = new Geometry(null, clone);
                        geometry.setMaterial(((Geometry) spatial).getMaterial());
                        geometriesToAdd.add(geometry);
                    }
                }

                // adding meshes to node
                for (Geometry geometry : geometriesToAdd) {
                    node.attachChild(geometry);
                }
                geometriesToAdd.clear();
            }
        }
        return node;
	}
	
	@Override
	public String getType() {
		return Modifier.MIRROR_MODIFIER_DATA;
	}
}

/*
 * Copyright (c) 2009-2012 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.scene.plugins.blender.objects;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.asset.BlenderKey.FeaturesToLoad;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix4f;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh.Mode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.plugins.blender.AbstractBlenderHelper;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.BlenderContext.LoadedDataType;
import com.jme3.scene.plugins.blender.animations.AnimationHelper;
import com.jme3.scene.plugins.blender.cameras.CameraHelper;
import com.jme3.scene.plugins.blender.constraints.ConstraintHelper;
import com.jme3.scene.plugins.blender.curves.CurvesHelper;
import com.jme3.scene.plugins.blender.file.BlenderFileException;
import com.jme3.scene.plugins.blender.file.DynamicArray;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.lights.LightHelper;
import com.jme3.scene.plugins.blender.meshes.MeshHelper;
import com.jme3.scene.plugins.blender.meshes.TemporalMesh;
import com.jme3.scene.plugins.blender.modifiers.Modifier;
import com.jme3.scene.plugins.blender.modifiers.ModifierHelper;
import com.jme3.util.TempVars;

/**
 * A class that is used in object calculations.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public class ObjectHelper extends AbstractBlenderHelper {
    private static final Logger LOGGER               = Logger.getLogger(ObjectHelper.class.getName());

    public static final String  OMA_MARKER           = "oma";
    public static final String  ARMATURE_NODE_MARKER = "armature-node";

    /**
     * This constructor parses the given blender version and stores the result.
     * Some functionalities may differ in different blender versions.
     * 
     * @param blenderVersion
     *            the version read from the blend file
     * @param blenderContext
     *            the blender context
     */
    public ObjectHelper(String blenderVersion, BlenderContext blenderContext) {
        super(blenderVersion, blenderContext);
    }

    /**
     * This method reads the given structure and createn an object that
     * represents the data.
     * 
     * @param objectStructure
     *            the object's structure
     * @param blenderContext
     *            the blender context
     * @return blener's object representation or null if its type is excluded from loading
     * @throws BlenderFileException
     *             an exception is thrown when the given data is inapropriate
     */
    public Object toObject(Structure objectStructure, BlenderContext blenderContext) throws BlenderFileException {
        LOGGER.fine("Loading blender object.");

        int type = ((Number) objectStructure.getFieldValue("type")).intValue();
        ObjectType objectType = ObjectType.valueOf(type);
        LOGGER.log(Level.FINE, "Type of the object: {0}.", objectType);
        if (objectType == ObjectType.LAMP && !blenderContext.getBlenderKey().shouldLoad(FeaturesToLoad.LIGHTS)) {
            LOGGER.fine("Lamps are not included in loading.");
            return null;
        }
        if (objectType == ObjectType.CAMERA && !blenderContext.getBlenderKey().shouldLoad(FeaturesToLoad.CAMERAS)) {
            LOGGER.fine("Cameras are not included in loading.");
            return null;
        }
        if (!blenderContext.getBlenderKey().shouldLoad(FeaturesToLoad.OBJECTS)) {
            LOGGER.fine("Objects are not included in loading.");
            return null;
        }
        int lay = ((Number) objectStructure.getFieldValue("lay")).intValue();
        if ((lay & blenderContext.getBlenderKey().getLayersToLoad()) == 0) {
            LOGGER.fine("The layer this object is located in is not included in loading.");
            return null;
        }

        LOGGER.fine("Checking if the object has not been already loaded.");
        Object loadedResult = blenderContext.getLoadedFeature(objectStructure.getOldMemoryAddress(), LoadedDataType.FEATURE);
        if (loadedResult != null) {
            return loadedResult;
        }

        blenderContext.pushParent(objectStructure);
        String name = objectStructure.getName();
        LOGGER.log(Level.FINE, "Loading obejct: {0}", name);
        
        int restrictflag = ((Number) objectStructure.getFieldValue("restrictflag")).intValue();
        boolean visible = (restrictflag & 0x01) != 0;

        Pointer pParent = (Pointer) objectStructure.getFieldValue("parent");
        Object parent = blenderContext.getLoadedFeature(pParent.getOldMemoryAddress(), LoadedDataType.FEATURE);
        if (parent == null && pParent.isNotNull()) {
            Structure parentStructure = pParent.fetchData().get(0);
            parent = this.toObject(parentStructure, blenderContext);
        }

        Transform t = this.getTransformation(objectStructure, blenderContext);
        LOGGER.log(Level.FINE, "Importing object of type: {0}", objectType);
        Node result = null;
        try {
            switch (objectType) {
                case LATTICE:
                case METABALL:
                case TEXT:
                case WAVE:
                    LOGGER.log(Level.WARNING, "{0} type is not supported but the node will be returned in order to keep parent - child relationship.", objectType);
                case EMPTY:
                case ARMATURE:
                    // need to use an empty node to properly create
                    // parent-children relationships between nodes
                    result = new Node(name);
                    break;
                case MESH:
                    result = new Node(name);
                    MeshHelper meshHelper = blenderContext.getHelper(MeshHelper.class);
                    Pointer pMesh = (Pointer) objectStructure.getFieldValue("data");
                    List<Structure> meshesArray = pMesh.fetchData();
                    TemporalMesh temporalMesh = meshHelper.toTemporalMesh(meshesArray.get(0), blenderContext);
                    if(temporalMesh != null) {
                        result.attachChild(temporalMesh);
                    }
                    break;
                case SURF:
                case CURVE:
                    result = new Node(name);
                    Pointer pCurve = (Pointer) objectStructure.getFieldValue("data");
                    if (pCurve.isNotNull()) {
                        CurvesHelper curvesHelper = blenderContext.getHelper(CurvesHelper.class);
                        Structure curveData = pCurve.fetchData().get(0);
                        List<Geometry> curves = curvesHelper.toCurve(curveData, blenderContext);
                        for (Geometry curve : curves) {
                            result.attachChild(curve);
                        }
                    }
                    break;
                case LAMP:
                    Pointer pLamp = (Pointer) objectStructure.getFieldValue("data");
                    if (pLamp.isNotNull()) {
                        LightHelper lightHelper = blenderContext.getHelper(LightHelper.class);
                        List<Structure> lampsArray = pLamp.fetchData();
                        result = lightHelper.toLight(lampsArray.get(0), blenderContext);
                        if (result == null) {
                            // probably some light type is not supported, just create a node so that we can maintain child-parent relationship for nodes
                            result = new Node(name);
                        }
                    }
                    break;
                case CAMERA:
                    Pointer pCamera = (Pointer) objectStructure.getFieldValue("data");
                    if (pCamera.isNotNull()) {
                        CameraHelper cameraHelper = blenderContext.getHelper(CameraHelper.class);
                        List<Structure> camerasArray = pCamera.fetchData();
                        result = cameraHelper.toCamera(camerasArray.get(0), blenderContext);
                    }
                    break;
                default:
                    LOGGER.log(Level.WARNING, "Unsupported object type: {0}", type);
            }
            
            if (result != null) {
                LOGGER.fine("Storing loaded feature in blender context and applying markers (those will be removed before the final result is released).");
                Long oma = objectStructure.getOldMemoryAddress();
                blenderContext.addLoadedFeatures(oma, LoadedDataType.STRUCTURE, objectStructure);
                blenderContext.addLoadedFeatures(oma, LoadedDataType.FEATURE, result);
                
                blenderContext.addMarker(OMA_MARKER, result, objectStructure.getOldMemoryAddress());
                if (objectType == ObjectType.ARMATURE) {
                    blenderContext.addMarker(ARMATURE_NODE_MARKER, result, Boolean.TRUE);
                }

                result.setLocalTransform(t);
                result.setCullHint(visible ? CullHint.Always : CullHint.Inherit);
                if (parent instanceof Node) {
                    ((Node) parent).attachChild(result);
                }

                LOGGER.fine("Reading and applying object's modifiers.");
                ModifierHelper modifierHelper = blenderContext.getHelper(ModifierHelper.class);
                Collection<Modifier> modifiers = modifierHelper.readModifiers(objectStructure, blenderContext);
                for (Modifier modifier : modifiers) {
                    modifier.apply(result, blenderContext);
                }
                
                if (result.getChildren() != null && result.getChildren().size() > 0) {
                    if(result.getChildren().size() == 1 && result.getChild(0) instanceof TemporalMesh) {
                        LOGGER.fine("Converting temporal mesh into jme geometries.");
                        ((TemporalMesh)result.getChild(0)).toGeometries();
                    }
                    
                    LOGGER.fine("Applying proper scale to the geometries.");
                    for (Spatial child : result.getChildren()) {
                        if (child instanceof Geometry) {
                            this.flipMeshIfRequired((Geometry) child, child.getWorldScale());
                        }
                    }
                }

                // I prefer do compute bounding box here than read it from the file
                result.updateModelBound();

                LOGGER.fine("Applying animations to the object if such are defined.");
                AnimationHelper animationHelper = blenderContext.getHelper(AnimationHelper.class);
                animationHelper.applyAnimations(result, blenderContext.getBlenderKey().getAnimationMatchMethod());

                LOGGER.fine("Loading constraints connected with this object.");
                ConstraintHelper constraintHelper = blenderContext.getHelper(ConstraintHelper.class);
                constraintHelper.loadConstraints(objectStructure, blenderContext);

                LOGGER.fine("Loading custom properties.");
                if (blenderContext.getBlenderKey().isLoadObjectProperties()) {
                    Properties properties = this.loadProperties(objectStructure, blenderContext);
                    // the loaded property is a group property, so we need to get
                    // each value and set it to Spatial
                    if (properties != null && properties.getValue() != null) {
                        this.applyProperties(result, properties);
                    }
                }
            }
        } finally {
            blenderContext.popParent();
        }
        return result;
    }

    /**
     * The method flips the mesh if the scale is mirroring it. Mirroring scale has either 1 or all 3 factors negative.
     * If two factors are negative then there is no mirroring because a rotation and translation can be found that will
     * lead to the same transform when all scales are positive.
     * 
     * @param geometry
     *            the geometry that is being flipped if necessary
     * @param scale
     *            the scale vector of the given geometry
     */
    private void flipMeshIfRequired(Geometry geometry, Vector3f scale) {
        float s = scale.x * scale.y * scale.z;

        if (s < 0 && geometry.getMesh() != null) {// negative s means that the scale is mirroring the object
            FloatBuffer normals = geometry.getMesh().getFloatBuffer(Type.Normal);
            if (normals != null) {
                for (int i = 0; i < normals.limit(); i += 3) {
                    if (scale.x < 0) {
                        normals.put(i, -normals.get(i));
                    }
                    if (scale.y < 0) {
                        normals.put(i + 1, -normals.get(i + 1));
                    }
                    if (scale.z < 0) {
                        normals.put(i + 2, -normals.get(i + 2));
                    }
                }
            }

            if (geometry.getMesh().getMode() == Mode.Triangles) {// there is no need to flip the indexes for lines and points
                LOGGER.finer("Flipping index order in triangle mesh.");
                Buffer indexBuffer = geometry.getMesh().getBuffer(Type.Index).getData();
                for (int i = 0; i < indexBuffer.limit(); i += 3) {
                    if (indexBuffer instanceof ShortBuffer) {
                        short index = ((ShortBuffer) indexBuffer).get(i + 1);
                        ((ShortBuffer) indexBuffer).put(i + 1, ((ShortBuffer) indexBuffer).get(i + 2));
                        ((ShortBuffer) indexBuffer).put(i + 2, index);
                    } else {
                        int index = ((IntBuffer) indexBuffer).get(i + 1);
                        ((IntBuffer) indexBuffer).put(i + 1, ((IntBuffer) indexBuffer).get(i + 2));
                        ((IntBuffer) indexBuffer).put(i + 2, index);
                    }
                }
            }
        }
    }

    /**
     * Checks if the first given OMA points to a parent of the second one.
     * The parent need not to be the direct one. This method should be called when we are sure
     * that both of the features are alred loaded because it does not check it.
     * The OMA's should point to a spatials, otherwise the function will throw ClassCastException.
     * @param supposedParentOMA
     *            the OMA of the node that we suppose might be a parent of the second one
     * @param spatialOMA
     *            the OMA of the scene's node
     * @return <b>true</b> if the first given OMA points to a parent of the second one and <b>false</b> otherwise
     */
    public boolean isParent(Long supposedParentOMA, Long spatialOMA) {
        Spatial supposedParent = (Spatial) blenderContext.getLoadedFeature(supposedParentOMA, LoadedDataType.FEATURE);
        Spatial spatial = (Spatial) blenderContext.getLoadedFeature(spatialOMA, LoadedDataType.FEATURE);

        Spatial parent = spatial.getParent();
        while (parent != null) {
            if (parent.equals(supposedParent)) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    /**
     * This method calculates local transformation for the object. Parentage is
     * taken under consideration.
     * 
     * @param objectStructure
     *            the object's structure
     * @return objects transformation relative to its parent
     */
    public Transform getTransformation(Structure objectStructure, BlenderContext blenderContext) {
        TempVars tempVars = TempVars.get();

        Matrix4f parentInv = tempVars.tempMat4;
        Pointer pParent = (Pointer) objectStructure.getFieldValue("parent");
        if (pParent.isNotNull()) {
            Structure parentObjectStructure = (Structure) blenderContext.getLoadedFeature(pParent.getOldMemoryAddress(), LoadedDataType.STRUCTURE);
            this.getMatrix(parentObjectStructure, "obmat", fixUpAxis, parentInv).invertLocal();
        } else {
            parentInv.loadIdentity();
        }

        Matrix4f globalMatrix = this.getMatrix(objectStructure, "obmat", fixUpAxis, tempVars.tempMat42);
        Matrix4f localMatrix = parentInv.multLocal(globalMatrix);

        this.getSizeSignums(objectStructure, tempVars.vect1);

        localMatrix.toTranslationVector(tempVars.vect2);
        localMatrix.toRotationQuat(tempVars.quat1);
        localMatrix.toScaleVector(tempVars.vect3);

        Transform t = new Transform(tempVars.vect2, tempVars.quat1.normalizeLocal(), tempVars.vect3.multLocal(tempVars.vect1));
        tempVars.release();
        return t;
    }

    /**
     * The method gets the signs of the scale factors and stores them properly in the given vector.
     * @param objectStructure
     *            the object's structure
     * @param store
     *            the vector where the result will be stored
     */
    @SuppressWarnings("unchecked")
    private void getSizeSignums(Structure objectStructure, Vector3f store) {
        DynamicArray<Number> size = (DynamicArray<Number>) objectStructure.getFieldValue("size");
        if (fixUpAxis) {
            store.x = Math.signum(size.get(0).floatValue());
            store.y = Math.signum(size.get(2).floatValue());
            store.z = Math.signum(size.get(1).floatValue());
        } else {
            store.x = Math.signum(size.get(0).floatValue());
            store.y = Math.signum(size.get(1).floatValue());
            store.z = Math.signum(size.get(2).floatValue());
        }
    }

    /**
     * This method returns the matrix of a given name for the given structure.
     * It takes up axis into consideration.
     * 
     * The method that moves the matrix from Z-up axis to Y-up axis space is as follows:
     * - load the matrix directly from blender (it has the Z-up axis orientation)
     * - switch the second and third rows in the matrix
     * - switch the second and third column in the matrix
     * - multiply the values in the third row by -1
     * - multiply the values in the third column by -1
     * 
     * The result matrix is now in Y-up axis orientation.
     * The procedure was discovered by experimenting but it looks like it's working :)
     * The previous procedure transformet the loaded matrix into component (loc, rot, scale),
     * switched several values and pu the back into the matrix.
     * It worked fine until models with negative scale are used.
     * The current method is not touched by that flaw.
     * 
     * @param structure
     *            the structure with matrix data
     * @param matrixName
     *            the name of the matrix
     * @param fixUpAxis
     *            tells if the Y axis is a UP axis
     * @param store
     *            the matrix where the result will pe placed
     * @return the required matrix
     */
    @SuppressWarnings("unchecked")
    private Matrix4f getMatrix(Structure structure, String matrixName, boolean fixUpAxis, Matrix4f store) {
        DynamicArray<Number> obmat = (DynamicArray<Number>) structure.getFieldValue(matrixName);
        // the matrix must be square
        int rowAndColumnSize = Math.abs((int) Math.sqrt(obmat.getTotalSize()));
        for (int i = 0; i < rowAndColumnSize; ++i) {
            for (int j = 0; j < rowAndColumnSize; ++j) {
                float value = obmat.get(j, i).floatValue();
                if (Math.abs(value) <= FastMath.FLT_EPSILON) {
                    value = 0;
                }
                store.set(i, j, value);
            }
        }
        if (fixUpAxis) {
            // first switch the second and third row
            for (int i = 0; i < 4; ++i) {
                float temp = store.get(1, i);
                store.set(1, i, store.get(2, i));
                store.set(2, i, temp);
            }

            // then switch the second and third column
            for (int i = 0; i < 4; ++i) {
                float temp = store.get(i, 1);
                store.set(i, 1, store.get(i, 2));
                store.set(i, 2, temp);
            }

            // multiply the values in the third row by -1
            store.m20 *= -1;
            store.m21 *= -1;
            store.m22 *= -1;
            store.m23 *= -1;

            // multiply the values in the third column by -1
            store.m02 *= -1;
            store.m12 *= -1;
            store.m22 *= -1;
            store.m32 *= -1;
        }

        return store;
    }

    /**
     * This method returns the matrix of a given name for the given structure.
     * It takes up axis into consideration.
     * 
     * @param structure
     *            the structure with matrix data
     * @param matrixName
     *            the name of the matrix
     * @param fixUpAxis
     *            tells if the Y axis is a UP axis
     * @return the required matrix
     */
    public Matrix4f getMatrix(Structure structure, String matrixName, boolean fixUpAxis) {
        return this.getMatrix(structure, matrixName, fixUpAxis, new Matrix4f());
    }

    private static enum ObjectType {
        EMPTY(0), MESH(1), CURVE(2), SURF(3), TEXT(4), METABALL(5), LAMP(10), CAMERA(11), WAVE(21), LATTICE(22), ARMATURE(25);

        private int blenderTypeValue;

        private ObjectType(int blenderTypeValue) {
            this.blenderTypeValue = blenderTypeValue;
        }

        public static ObjectType valueOf(int blenderTypeValue) throws BlenderFileException {
            for (ObjectType type : ObjectType.values()) {
                if (type.blenderTypeValue == blenderTypeValue) {
                    return type;
                }
            }
            throw new BlenderFileException("Unknown type value: " + blenderTypeValue);
        }
    }
}

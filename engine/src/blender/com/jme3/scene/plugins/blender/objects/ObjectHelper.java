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

import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.asset.BlenderKey.FeaturesToLoad;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.plugins.blender.AbstractBlenderHelper;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.BlenderContext.LoadedFeatureDataType;
import com.jme3.scene.plugins.blender.animations.ArmatureHelper;
import com.jme3.scene.plugins.blender.cameras.CameraHelper;
import com.jme3.scene.plugins.blender.constraints.ConstraintHelper;
import com.jme3.scene.plugins.blender.curves.CurvesHelper;
import com.jme3.scene.plugins.blender.file.BlenderFileException;
import com.jme3.scene.plugins.blender.file.DynamicArray;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.lights.LightHelper;
import com.jme3.scene.plugins.blender.meshes.MeshHelper;
import com.jme3.scene.plugins.blender.modifiers.Modifier;
import com.jme3.scene.plugins.blender.modifiers.ModifierHelper;

/**
 * A class that is used in object calculations.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public class ObjectHelper extends AbstractBlenderHelper {
    private static final Logger LOGGER     = Logger.getLogger(ObjectHelper.class.getName());

    public static final String  OMA_MARKER = "oma";

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
        Object loadedResult = blenderContext.getLoadedFeature(objectStructure.getOldMemoryAddress(), LoadedFeatureDataType.LOADED_FEATURE);
        if (loadedResult != null) {
            return loadedResult;
        }

        blenderContext.pushParent(objectStructure);
        String name = objectStructure.getName();
        LOGGER.log(Level.FINE, "Loading obejct: {0}", name);

        int restrictflag = ((Number) objectStructure.getFieldValue("restrictflag")).intValue();
        boolean visible = (restrictflag & 0x01) != 0;

        Pointer pParent = (Pointer) objectStructure.getFieldValue("parent");
        Object parent = blenderContext.getLoadedFeature(pParent.getOldMemoryAddress(), LoadedFeatureDataType.LOADED_FEATURE);
        if (parent == null && pParent.isNotNull()) {
            Structure parentStructure = pParent.fetchData(blenderContext.getInputStream()).get(0);
            parent = this.toObject(parentStructure, blenderContext);
        }

        Transform t = this.getTransformation(objectStructure, blenderContext);
        LOGGER.log(Level.FINE, "Importing object of type: {0}", objectType);
        Node result = null;
        try {
            switch (objectType) {
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
                    List<Structure> meshesArray = pMesh.fetchData(blenderContext.getInputStream());
                    List<Geometry> geometries = meshHelper.toMesh(meshesArray.get(0), blenderContext);
                    if (geometries != null) {
                        for (Geometry geometry : geometries) {
                            result.attachChild(geometry);
                        }
                    }
                    break;
                case SURF:
                case CURVE:
                    result = new Node(name);
                    Pointer pCurve = (Pointer) objectStructure.getFieldValue("data");
                    if (pCurve.isNotNull()) {
                        CurvesHelper curvesHelper = blenderContext.getHelper(CurvesHelper.class);
                        Structure curveData = pCurve.fetchData(blenderContext.getInputStream()).get(0);
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
                        List<Structure> lampsArray = pLamp.fetchData(blenderContext.getInputStream());
                        result = lightHelper.toLight(lampsArray.get(0), blenderContext);
                    }
                    break;
                case CAMERA:
                    Pointer pCamera = (Pointer) objectStructure.getFieldValue("data");
                    if (pCamera.isNotNull()) {
                        CameraHelper cameraHelper = blenderContext.getHelper(CameraHelper.class);
                        List<Structure> camerasArray = pCamera.fetchData(blenderContext.getInputStream());
                        result = cameraHelper.toCamera(camerasArray.get(0), blenderContext);
                    }
                    break;
                default:
                    LOGGER.log(Level.WARNING, "Unsupported object type: {0}", type);
            }
        } finally {
            blenderContext.popParent();
        }

        if (result != null) {
            blenderContext.addLoadedFeatures(objectStructure.getOldMemoryAddress(), name, objectStructure, result);

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

            // I prefer do compute bounding box here than read it from the file
            result.updateModelBound();

            LOGGER.fine("Applying markers (those will be removed before the final result is released).");
            blenderContext.addMarker(OMA_MARKER, result, objectStructure.getOldMemoryAddress());
            if (objectType == ObjectType.ARMATURE) {
                blenderContext.addMarker(ArmatureHelper.ARMATURE_NODE_MARKER, result, Boolean.TRUE);
            }

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
        return result;
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
        Spatial supposedParent = (Spatial) blenderContext.getLoadedFeature(supposedParentOMA, LoadedFeatureDataType.LOADED_FEATURE);
        Spatial spatial = (Spatial) blenderContext.getLoadedFeature(spatialOMA, LoadedFeatureDataType.LOADED_FEATURE);

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
        Matrix4f parentInv = Matrix4f.IDENTITY;
        Pointer pParent = (Pointer) objectStructure.getFieldValue("parent");
        if(pParent.isNotNull()) {
            Structure parentObjectStructure = (Structure) blenderContext.getLoadedFeature(pParent.getOldMemoryAddress(), LoadedFeatureDataType.LOADED_STRUCTURE);
            parentInv = this.getMatrix(parentObjectStructure, "obmat", fixUpAxis).invertLocal();
        }

        Matrix4f globalMatrix = this.getMatrix(objectStructure, "obmat", fixUpAxis);
        Matrix4f localMatrix = parentInv.multLocal(globalMatrix);
        return new Transform(localMatrix.toTranslationVector(), localMatrix.toRotationQuat(), localMatrix.toScaleVector());
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
    @SuppressWarnings("unchecked")
    public Matrix4f getMatrix(Structure structure, String matrixName, boolean fixUpAxis) {
        Matrix4f result = new Matrix4f();
        DynamicArray<Number> obmat = (DynamicArray<Number>) structure.getFieldValue(matrixName);
        // the matrix must be square
        int rowAndColumnSize = Math.abs((int) Math.sqrt(obmat.getTotalSize()));
        for (int i = 0; i < rowAndColumnSize; ++i) {
            for (int j = 0; j < rowAndColumnSize; ++j) {
                result.set(i, j, obmat.get(j, i).floatValue());
            }
        }
        if (fixUpAxis) {
            Vector3f translation = result.toTranslationVector();
            Quaternion rotation = result.toRotationQuat();
            Vector3f scale = result.toScaleVector();

            float y = translation.y;
            translation.y = translation.z;
            translation.z = y == 0 ? 0 : -y;

            y = rotation.getY();
            float z = rotation.getZ();
            rotation.set(rotation.getX(), z, y == 0 ? 0 : -y, rotation.getW());

            y = scale.y;
            scale.y = scale.z;
            scale.z = y;

            result.loadIdentity();
            result.setTranslation(translation);
            result.setRotationQuaternion(rotation);
            result.setScale(scale);
        }

        for (int i = 0; i < 4; ++i) {
            for (int j = 0; j < 4; ++j) {
                float value = result.get(i, j);
                if (Math.abs(value) <= FastMath.FLT_EPSILON) {
                    result.set(i, j, 0);
                }
            }
        }

        return result;
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

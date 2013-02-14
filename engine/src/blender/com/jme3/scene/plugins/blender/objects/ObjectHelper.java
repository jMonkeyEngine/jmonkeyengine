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
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.LightNode;
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
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.DynamicArray;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.lights.LightHelper;
import com.jme3.scene.plugins.blender.meshes.MeshHelper;
import com.jme3.scene.plugins.blender.modifiers.Modifier;
import com.jme3.scene.plugins.blender.modifiers.ModifierHelper;

/**
 * A class that is used in object calculations.
 * @author Marcin Roguski (Kaelthas)
 */
public class ObjectHelper extends AbstractBlenderHelper {
    private static final Logger LOGGER               = Logger.getLogger(ObjectHelper.class.getName());

    protected static final int  OBJECT_TYPE_EMPTY    = 0;
    protected static final int  OBJECT_TYPE_MESH     = 1;
    protected static final int  OBJECT_TYPE_CURVE    = 2;
    protected static final int  OBJECT_TYPE_SURF     = 3;
    protected static final int  OBJECT_TYPE_TEXT     = 4;
    protected static final int  OBJECT_TYPE_METABALL = 5;
    protected static final int  OBJECT_TYPE_LAMP     = 10;
    protected static final int  OBJECT_TYPE_CAMERA   = 11;
    protected static final int  OBJECT_TYPE_WAVE     = 21;
    protected static final int  OBJECT_TYPE_LATTICE  = 22;
    protected static final int  OBJECT_TYPE_ARMATURE = 25;

    /**
     * This constructor parses the given blender version and stores the result. Some functionalities may differ in
     * different blender versions.
     * @param blenderVersion
     *            the version read from the blend file
     * @param fixUpAxis
     *            a variable that indicates if the Y asxis is the UP axis or not
     */
    public ObjectHelper(String blenderVersion, boolean fixUpAxis) {
        super(blenderVersion, fixUpAxis);
    }

    /**
     * This method reads the given structure and createn an object that represents the data.
     * @param objectStructure
     *            the object's structure
     * @param blenderContext
     *            the blender context
     * @return blener's object representation
     * @throws BlenderFileException
     *             an exception is thrown when the given data is inapropriate
     */
    public Object toObject(Structure objectStructure, BlenderContext blenderContext) throws BlenderFileException {
        Object loadedResult = blenderContext.getLoadedFeature(objectStructure.getOldMemoryAddress(), LoadedFeatureDataType.LOADED_FEATURE);
        if (loadedResult != null) {
            return loadedResult;
        }

        blenderContext.pushParent(objectStructure);

        // get object data
        int type = ((Number) objectStructure.getFieldValue("type")).intValue();
        String name = objectStructure.getName();
        LOGGER.log(Level.FINE, "Loading obejct: {0}", name);

        int restrictflag = ((Number) objectStructure.getFieldValue("restrictflag")).intValue();
        boolean visible = (restrictflag & 0x01) != 0;
        Node result = null;

        Pointer pParent = (Pointer) objectStructure.getFieldValue("parent");
        Object parent = blenderContext.getLoadedFeature(pParent.getOldMemoryAddress(), LoadedFeatureDataType.LOADED_FEATURE);
        if (parent == null && pParent.isNotNull()) {
            Structure parentStructure = pParent.fetchData(blenderContext.getInputStream()).get(0);
            parent = this.toObject(parentStructure, blenderContext);
        }

        Transform t = this.getTransformation(objectStructure, blenderContext);

        try {
            switch (type) {
                case OBJECT_TYPE_EMPTY:
                    LOGGER.log(Level.FINE, "Importing empty.");
                    Node empty = new Node(name);
                    empty.setLocalTransform(t);
                    if (parent instanceof Node) {
                        ((Node) parent).attachChild(empty);
                    }
                    result = empty;
                    break;
                case OBJECT_TYPE_MESH:
                    LOGGER.log(Level.FINE, "Importing mesh.");
                    Node node = new Node(name);
                    node.setCullHint(visible ? CullHint.Always : CullHint.Inherit);

                    // reading mesh
                    MeshHelper meshHelper = blenderContext.getHelper(MeshHelper.class);
                    Pointer pMesh = (Pointer) objectStructure.getFieldValue("data");
                    List<Structure> meshesArray = pMesh.fetchData(blenderContext.getInputStream());
                    List<Geometry> geometries = meshHelper.toMesh(meshesArray.get(0), blenderContext);
                    if (geometries != null) {
                        for (Geometry geometry : geometries) {
                            node.attachChild(geometry);
                        }
                    }
                    node.setLocalTransform(t);

                    // setting the parent
                    if (parent instanceof Node) {
                        ((Node) parent).attachChild(node);
                    }
                    result = node;
                    break;
                case OBJECT_TYPE_SURF:
                case OBJECT_TYPE_CURVE:
                    LOGGER.log(Level.FINE, "Importing curve/nurb.");
                    Pointer pCurve = (Pointer) objectStructure.getFieldValue("data");
                    if (pCurve.isNotNull()) {
                        CurvesHelper curvesHelper = blenderContext.getHelper(CurvesHelper.class);
                        Structure curveData = pCurve.fetchData(blenderContext.getInputStream()).get(0);
                        List<Geometry> curves = curvesHelper.toCurve(curveData, blenderContext);
                        result = new Node(name);
                        for (Geometry curve : curves) {
                            ((Node) result).attachChild(curve);
                        }
                        ((Node) result).setLocalTransform(t);
                    }
                    break;
                case OBJECT_TYPE_LAMP:
                    LOGGER.log(Level.FINE, "Importing lamp.");
                    Pointer pLamp = (Pointer) objectStructure.getFieldValue("data");
                    if (pLamp.isNotNull()) {
                        LightHelper lightHelper = blenderContext.getHelper(LightHelper.class);
                        List<Structure> lampsArray = pLamp.fetchData(blenderContext.getInputStream());
                        LightNode light = lightHelper.toLight(lampsArray.get(0), blenderContext);
                        if (light != null) {
                            light.setName(name);
                            light.setLocalTransform(t);
                        }
                        result = light;
                    }
                    break;
                case OBJECT_TYPE_CAMERA:
                    Pointer pCamera = (Pointer) objectStructure.getFieldValue("data");
                    if (pCamera.isNotNull()) {
                        CameraHelper cameraHelper = blenderContext.getHelper(CameraHelper.class);
                        List<Structure> camerasArray = pCamera.fetchData(blenderContext.getInputStream());
                        CameraNode camera = cameraHelper.toCamera(camerasArray.get(0), blenderContext);
                        camera.setName(name);
                        camera.setLocalTransform(t);
                        result = camera;
                    }
                    break;
                case OBJECT_TYPE_ARMATURE:
                    // need to create an empty node to properly create parent-children relationships between nodes
                    Node armature = new Node(name);
                    armature.setLocalTransform(t);
                    armature.setUserData(ArmatureHelper.ARMETURE_NODE_MARKER, Boolean.TRUE);

                    if (parent instanceof Node) {
                        ((Node) parent).attachChild(armature);
                    }
                    result = armature;
                    break;
                default:
                    LOGGER.log(Level.WARNING, "Unknown object type: {0}", type);
            }
        } finally {
            blenderContext.popParent();
        }

        if (result != null) {
            result.updateModelBound();// I prefer do compute bounding box here than read it from the file

            blenderContext.addLoadedFeatures(objectStructure.getOldMemoryAddress(), name, objectStructure, result);

            // applying modifiers
            LOGGER.log(Level.FINE, "Reading and applying object's modifiers.");
            ModifierHelper modifierHelper = blenderContext.getHelper(ModifierHelper.class);
            Collection<Modifier> modifiers = modifierHelper.readModifiers(objectStructure, blenderContext);
            for (Modifier modifier : modifiers) {
                modifier.apply(result, blenderContext);
            }

            // loading constraints connected with this object
            ConstraintHelper constraintHelper = blenderContext.getHelper(ConstraintHelper.class);
            constraintHelper.loadConstraints(objectStructure, blenderContext);

            // reading custom properties
            if (blenderContext.getBlenderKey().isLoadObjectProperties()) {
                Properties properties = this.loadProperties(objectStructure, blenderContext);
                // the loaded property is a group property, so we need to get each value and set it to Spatial
                if (result instanceof Spatial && properties != null && properties.getValue() != null) {
                    this.applyProperties((Spatial) result, properties);
                }
            }
        }
        return result;
    }

    /**
     * This method calculates local transformation for the object. Parentage is taken under consideration.
     * @param objectStructure
     *            the object's structure
     * @return objects transformation relative to its parent
     */
    @SuppressWarnings("unchecked")
    public Transform getTransformation(Structure objectStructure, BlenderContext blenderContext) {
        // these are transformations in global space
        DynamicArray<Number> loc = (DynamicArray<Number>) objectStructure.getFieldValue("loc");
        DynamicArray<Number> size = (DynamicArray<Number>) objectStructure.getFieldValue("size");
        DynamicArray<Number> rot = (DynamicArray<Number>) objectStructure.getFieldValue("rot");

        // load parent inverse matrix
        Pointer pParent = (Pointer) objectStructure.getFieldValue("parent");
        Matrix4f parentInv = pParent.isNull() ? Matrix4f.IDENTITY : this.getMatrix(objectStructure, "parentinv");

        // create the global matrix (without the scale)
        Matrix4f globalMatrix = new Matrix4f();
        globalMatrix.setTranslation(loc.get(0).floatValue(), loc.get(1).floatValue(), loc.get(2).floatValue());
        globalMatrix.setRotationQuaternion(new Quaternion().fromAngles(rot.get(0).floatValue(), rot.get(1).floatValue(), rot.get(2).floatValue()));
        // compute local matrix
        Matrix4f localMatrix = parentInv.mult(globalMatrix);

        Vector3f translation = localMatrix.toTranslationVector();
        Quaternion rotation = localMatrix.toRotationQuat();
        Vector3f scale = this.getScale(parentInv).multLocal(size.get(0).floatValue(), size.get(1).floatValue(), size.get(2).floatValue());

        if (fixUpAxis) {
            float y = translation.y;
            translation.y = translation.z;
            translation.z = -y;

            y = rotation.getY();
            float z = rotation.getZ();
            rotation.set(rotation.getX(), z, -y, rotation.getW());

            y = scale.y;
            scale.y = scale.z;
            scale.z = y;
        }

        // create the result
        Transform t = new Transform(translation, rotation);
        t.setScale(scale);
        return t;
    }

    /**
     * This method returns the matrix of a given name for the given structure.
     * The matrix is NOT transformed if Y axis is up - the raw data is loaded from the blender file.
     * @param structure
     *            the structure with matrix data
     * @param matrixName
     *            the name of the matrix
     * @return the required matrix
     */
    public Matrix4f getMatrix(Structure structure, String matrixName) {
        return this.getMatrix(structure, matrixName, false);
    }

    /**
     * This method returns the matrix of a given name for the given structure.
     * It takes up axis into consideration.
     * @param structure
     *            the structure with matrix data
     * @param matrixName
     *            the name of the matrix
     * @return the required matrix
     */
    @SuppressWarnings("unchecked")
    public Matrix4f getMatrix(Structure structure, String matrixName, boolean applyFixUpAxis) {
        Matrix4f result = new Matrix4f();
        DynamicArray<Number> obmat = (DynamicArray<Number>) structure.getFieldValue(matrixName);
        int rowAndColumnSize = Math.abs((int) Math.sqrt(obmat.getTotalSize()));// the matrix must be square
        for (int i = 0; i < rowAndColumnSize; ++i) {
            for (int j = 0; j < rowAndColumnSize; ++j) {
                result.set(i, j, obmat.get(j, i).floatValue());
            }
        }
        if (applyFixUpAxis && fixUpAxis) {
            Vector3f translation = result.toTranslationVector();
            Quaternion rotation = result.toRotationQuat();
            Vector3f scale = this.getScale(result);

            float y = translation.y;
            translation.y = translation.z;
            translation.z = -y;

            y = rotation.getY();
            float z = rotation.getZ();
            rotation.set(rotation.getX(), z, -y, rotation.getW());

            y = scale.y;
            scale.y = scale.z;
            scale.z = y;

            result.loadIdentity();
            result.setTranslation(translation);
            result.setRotationQuaternion(rotation);
            result.setScale(scale);
        }
        return result;
    }

    /**
     * This method returns the scale from the given matrix.
     * 
     * @param matrix
     *            the transformation matrix
     * @return the scale from the given matrix
     */
    public Vector3f getScale(Matrix4f matrix) {
        float scaleX = (float) Math.sqrt(matrix.m00 * matrix.m00 + matrix.m10 * matrix.m10 + matrix.m20 * matrix.m20);
        float scaleY = (float) Math.sqrt(matrix.m01 * matrix.m01 + matrix.m11 * matrix.m11 + matrix.m21 * matrix.m21);
        float scaleZ = (float) Math.sqrt(matrix.m02 * matrix.m02 + matrix.m12 * matrix.m12 + matrix.m22 * matrix.m22);
        return new Vector3f(scaleX, scaleY, scaleZ);
    }

    @Override
    public void clearState() {
        fixUpAxis = false;
    }

    @Override
    public boolean shouldBeLoaded(Structure structure, BlenderContext blenderContext) {
        int lay = ((Number) structure.getFieldValue("lay")).intValue();
        return (lay & blenderContext.getBlenderKey().getLayersToLoad()) != 0 && (blenderContext.getBlenderKey().getFeaturesToLoad() & FeaturesToLoad.OBJECTS) != 0;
    }
}

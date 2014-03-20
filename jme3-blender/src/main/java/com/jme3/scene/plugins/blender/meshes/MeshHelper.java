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
package com.jme3.scene.plugins.blender.meshes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.asset.BlenderKey.FeaturesToLoad;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Mesh.Mode;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Format;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.scene.plugins.blender.AbstractBlenderHelper;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.BlenderContext.LoadedFeatureDataType;
import com.jme3.scene.plugins.blender.file.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.materials.MaterialContext;
import com.jme3.scene.plugins.blender.materials.MaterialHelper;
import com.jme3.scene.plugins.blender.meshes.builders.MeshBuilder;
import com.jme3.scene.plugins.blender.objects.Properties;
import com.jme3.scene.plugins.blender.textures.TextureHelper;
import com.jme3.util.BufferUtils;

/**
 * A class that is used in mesh calculations.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public class MeshHelper extends AbstractBlenderHelper {
    private static final Logger LOGGER                   = Logger.getLogger(MeshHelper.class.getName());

    /** A type of UV data layer in traditional faced mesh (triangles or quads). */
    public static final int     UV_DATA_LAYER_TYPE_FMESH = 5;
    /** A type of UV data layer in bmesh type. */
    public static final int     UV_DATA_LAYER_TYPE_BMESH = 16;
    /** A material used for single lines and points. */
    private Material            blackUnshadedMaterial;

    /**
     * This constructor parses the given blender version and stores the result. Some functionalities may differ in different blender
     * versions.
     * 
     * @param blenderVersion
     *            the version read from the blend file
     * @param blenderContext
     *            the blender context
     */
    public MeshHelper(String blenderVersion, BlenderContext blenderContext) {
        super(blenderVersion, blenderContext);
    }

    /**
     * This method reads converts the given structure into mesh. The given structure needs to be filled with the appropriate data.
     * 
     * @param structure
     *            the structure we read the mesh from
     * @return the mesh feature
     * @throws BlenderFileException
     */
    @SuppressWarnings("unchecked")
    public List<Geometry> toMesh(Structure structure, BlenderContext blenderContext) throws BlenderFileException {
        List<Geometry> geometries = (List<Geometry>) blenderContext.getLoadedFeature(structure.getOldMemoryAddress(), LoadedFeatureDataType.LOADED_FEATURE);
        if (geometries != null) {
            List<Geometry> copiedGeometries = new ArrayList<Geometry>(geometries.size());
            for (Geometry geometry : geometries) {
                copiedGeometries.add(geometry.clone());
            }
            return copiedGeometries;
        }

        String name = structure.getName();
        MeshContext meshContext = new MeshContext();
        LOGGER.log(Level.FINE, "Reading mesh: {0}.", name);

        LOGGER.fine("Loading materials.");
        MaterialHelper materialHelper = blenderContext.getHelper(MaterialHelper.class);
        MaterialContext[] materials = null;
        if ((blenderContext.getBlenderKey().getFeaturesToLoad() & FeaturesToLoad.MATERIALS) != 0) {
            materials = materialHelper.getMaterials(structure, blenderContext);
        }

        LOGGER.fine("Reading vertices.");
        MeshBuilder meshBuilder = new MeshBuilder(structure, materials, blenderContext);
        if (meshBuilder.isEmpty()) {
            LOGGER.fine("The geometry is empty.");
            geometries = new ArrayList<Geometry>(0);
            blenderContext.addLoadedFeatures(structure.getOldMemoryAddress(), structure.getName(), structure, geometries);
            blenderContext.setMeshContext(structure.getOldMemoryAddress(), meshContext);
            return geometries;
        }

        meshContext.setVertexReferenceMap(meshBuilder.getVertexReferenceMap());

        LOGGER.fine("Reading vertices groups (from the Object structure).");
        Structure parent = blenderContext.peekParent();
        Structure defbase = (Structure) parent.getFieldValue("defbase");
        List<Structure> defs = defbase.evaluateListBase();
        String[] verticesGroups = new String[defs.size()];
        int defIndex = 0;
        for (Structure def : defs) {
            verticesGroups[defIndex++] = def.getFieldValue("name").toString();
        }

        LOGGER.fine("Reading custom properties.");
        Properties properties = this.loadProperties(structure, blenderContext);

        LOGGER.fine("Generating meshes.");
        Map<Integer, List<Mesh>> meshes = meshBuilder.buildMeshes();
        geometries = new ArrayList<Geometry>(meshes.size());
        for (Entry<Integer, List<Mesh>> meshEntry : meshes.entrySet()) {
            int materialIndex = meshEntry.getKey();
            for (Mesh mesh : meshEntry.getValue()) {
                LOGGER.fine("Preparing the result part.");
                Geometry geometry = new Geometry(name + (geometries.size() + 1), mesh);
                if (properties != null && properties.getValue() != null) {
                    this.applyProperties(geometry, properties);
                }
                geometries.add(geometry);
                meshContext.putGeometry(materialIndex, geometry);
            }
        }

        // store the data in blender context before applying the material
        blenderContext.addLoadedFeatures(structure.getOldMemoryAddress(), structure.getName(), structure, geometries);
        blenderContext.setMeshContext(structure.getOldMemoryAddress(), meshContext);

        // apply materials only when all geometries are in place
        if (materials != null) {
            for (Geometry geometry : geometries) {
                int materialNumber = meshContext.getMaterialIndex(geometry);
                if (materialNumber < 0) {
                    geometry.setMaterial(this.getBlackUnshadedMaterial(blenderContext));
                } else if (materials[materialNumber] != null) {
                    LinkedHashMap<String, List<Vector2f>> uvCoordinates = meshBuilder.getUVCoordinates(materialNumber);
                    MaterialContext materialContext = materials[materialNumber];
                    materialContext.applyMaterial(geometry, structure.getOldMemoryAddress(), uvCoordinates, blenderContext);
                } else {
                    geometry.setMaterial(blenderContext.getDefaultMaterial());
                    LOGGER.warning("The importer came accross mesh that points to a null material. Default material is used to prevent loader from crashing, " + "but the model might look not the way it should. Sometimes blender does not assign materials properly. " + "Enter the edit mode and assign materials once more to your faces.");
                }
            }
        } else {
            // add UV coordinates if they are defined even if the material is not applied to the model
            List<VertexBuffer> uvCoordsBuffer = null;
            if (meshBuilder.hasUVCoordinates()) {
                Map<String, List<Vector2f>> uvs = meshBuilder.getUVCoordinates(0);
                if (uvs != null && uvs.size() > 0) {
                    uvCoordsBuffer = new ArrayList<VertexBuffer>(uvs.size());
                    int uvIndex = 0;
                    for (Entry<String, List<Vector2f>> entry : uvs.entrySet()) {
                        VertexBuffer buffer = new VertexBuffer(TextureHelper.TEXCOORD_TYPES[uvIndex++]);
                        buffer.setupData(Usage.Static, 2, Format.Float, BufferUtils.createFloatBuffer(entry.getValue().toArray(new Vector2f[uvs.size()])));
                        uvCoordsBuffer.add(buffer);
                    }
                }
            }

            for (Geometry geometry : geometries) {
                Mode mode = geometry.getMesh().getMode();
                if (mode != Mode.Triangles && mode != Mode.TriangleFan && mode != Mode.TriangleStrip) {
                    geometry.setMaterial(this.getBlackUnshadedMaterial(blenderContext));
                } else {
                    Material defaultMaterial = blenderContext.getDefaultMaterial();
                    if(geometry.getMesh().getBuffer(Type.Color) != null) {
                        defaultMaterial = defaultMaterial.clone();
                        defaultMaterial.setBoolean("VertexColor", true);
                    }
                    geometry.setMaterial(defaultMaterial);
                }
                if (uvCoordsBuffer != null) {
                    for (VertexBuffer buffer : uvCoordsBuffer) {
                        geometry.getMesh().setBuffer(buffer);
                    }
                }
            }
        }

        return geometries;
    }

    /**
     * Tells if the given mesh structure supports BMesh.
     * 
     * @param meshStructure
     *            the mesh structure
     * @return <b>true</b> if BMesh is supported and <b>false</b> otherwise
     */
    public boolean isBMeshCompatible(Structure meshStructure) {
        Pointer pMLoop = (Pointer) meshStructure.getFieldValue("mloop");
        Pointer pMPoly = (Pointer) meshStructure.getFieldValue("mpoly");
        return pMLoop != null && pMPoly != null && pMLoop.isNotNull() && pMPoly.isNotNull();
    }

    private synchronized Material getBlackUnshadedMaterial(BlenderContext blenderContext) {
        if (blackUnshadedMaterial == null) {
            blackUnshadedMaterial = new Material(blenderContext.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            blackUnshadedMaterial.setColor("Color", ColorRGBA.Black);
        }
        return blackUnshadedMaterial;
    }
}

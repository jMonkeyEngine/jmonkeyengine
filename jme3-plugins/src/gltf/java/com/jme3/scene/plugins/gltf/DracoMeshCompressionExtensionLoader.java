/*
 * Copyright (c) 2009-2026 jMonkeyEngine
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
package com.jme3.scene.plugins.gltf;

import static com.jme3.scene.plugins.gltf.GltfUtils.assertNotNull;
import static com.jme3.scene.plugins.gltf.GltfUtils.getAsInt;
import static com.jme3.scene.plugins.gltf.GltfUtils.getAsInteger;
import static com.jme3.scene.plugins.gltf.GltfUtils.getAsString;
import static com.jme3.scene.plugins.gltf.GltfUtils.getNumberOfComponents;
import static com.jme3.scene.plugins.gltf.GltfUtils.getVertexBufferFormat;
import static com.jme3.scene.plugins.gltf.GltfUtils.getVertexBufferType;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.asset.AssetLoadException;
import com.jme3.plugins.json.JsonElement;
import com.jme3.plugins.json.JsonObject;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;

import dev.fileformat.drako.Draco;
import dev.fileformat.drako.DracoMesh;
import dev.fileformat.drako.DrakoException;
import dev.fileformat.drako.PointAttribute;

/**
 * A class for handling the <code>KHR_draco_mesh_compression</code> extension when loading a glTF asset.
 * 
 * It is registered as the handler for this extension in the glTF {@link CustomContentManager}. In the
 * {@link GltfLoader#readMeshPrimitives(int)} method, the custom content handler will be called for each mesh
 * primitive, and handle the <code>KHR_draco_mesh_compression</code> of the primitive by calling the
 * {@link #handleExtension} method of this class.
 * 
 * TODO_DRACO Strictly speaking, the loader should ignore any attribute definitions when the draco extension
 * is present. Right now, this is called after the mesh was already filled with the vertex buffers that have
 * been created by the default loading process. See the check for "bufferViewIndex == null" in
 * VertexBufferPopulator.
 */
public class DracoMeshCompressionExtensionLoader implements ExtensionLoader {

    /**
     * The logger used in this class
     */
    private final static Logger logger = Logger
            .getLogger(DracoMeshCompressionExtensionLoader.class.getName());

    /**
     * The default log level
     */
    private static final Level level = Level.INFO;

    /**
     * <ul>
     * <li>The <code>parentName</code> will be <code>"primitive"</code></li>
     * <li>The <code>parent</code>" will be the JSON element that represents the mesh primitive from the glTF
     * JSON.</li>
     * <li>The <code>extension</code> will be the JSON element that represents the
     * <code>KHR_draco_mesh_compression</code> extension object.</li>
     * </ul>
     * 
     * {@inheritDoc}
     */
    @Override
    public Object handleExtension(GltfLoader loader, String parentName, JsonElement parent,
            JsonElement extension, Object input) throws IOException {

        logger.log(level, "Decoding draco data");

        JsonObject meshPrimitiveObject = parent.getAsJsonObject();
        JsonObject extensionObject = extension.getAsJsonObject();
        Mesh mesh = (Mesh) input;

        DracoMesh dracoMesh = readDracoMesh(loader, extension);

        // Fetch the indices, convert them into a vertex buffer,
        // and replace the index vertex buffer of the mesh with
        // the newly created buffer.
        logger.log(level, "Decoding draco indices");
        int indices[] = dracoMesh.getIndices().toArray();
        int indicesAccessorIndex = getAsInt(meshPrimitiveObject, "mesh primitive", "indices");
        JsonObject indicesAccessor = loader.getAccessor(indicesAccessorIndex);
        int indicesComponentType = getAsInt(indicesAccessor, "accessor " + indicesAccessorIndex,
                "componentType");
        VertexBuffer indicesVertexBuffer = createIndicesVertexBuffer(loader, indicesComponentType, indices);
        mesh.clearBuffer(VertexBuffer.Type.Index);
        mesh.setBuffer(indicesVertexBuffer);

        // Iterate over all attributes that are found in the
        // "attributes" dictionary of the extension object.
        // According to the specification, these must be
        // a subset of the attributes of the mesh primitive.
        JsonObject attributes = extensionObject.get("attributes").getAsJsonObject();
        JsonObject parentAttributes = meshPrimitiveObject.get("attributes").getAsJsonObject();
        for (Entry<String, JsonElement> entry : attributes.entrySet()) {
            String attributeName = entry.getKey();
            logger.log(level, "Decoding draco attribute " + attributeName);

            // The extension object stores the attribute ID, which
            // is an identifier for the attribute in the decoded
            // draco data. It is NOT an accessor index!
            int attributeId = entry.getValue().getAsInt();
            PointAttribute pointAttribute = getAttribute(dracoMesh, attributeName, attributeId);

            logger.log(level, "attribute " + attributeName);
            logger.log(level, "attributeId " + attributeId);
            logger.log(level, "pointAttribute " + pointAttribute);

            // The mesh primitive stores the accessor index for
            // each attribute
            int attributeAccessorIndex = getAsInt(parentAttributes, attributeName + " attribute",
                    attributeName);
            JsonObject accessor = loader.getAccessor(attributeAccessorIndex);

            logger.log(level, "attributeAccessorIndex " + attributeAccessorIndex);
            logger.log(level, "accessor " + accessor);

            // Replace the buffer in the mesh with a buffer that was
            // created from the data that was fetched from the
            // decoded draco PointAttribute
            Type bufferType = getVertexBufferType(attributeName);
            VertexBuffer attributeVertexBuffer = createAttributeVertexBuffer(attributeName, accessor,
                    pointAttribute, indices);
            mesh.clearBuffer(bufferType);
            mesh.setBuffer(attributeVertexBuffer);
        }

        logger.log(level, "Decoding draco data DONE");
        return mesh;
    }

    /**
     * Read the draco data from the given extension, using <code>openize-drako-java</code>.
     * 
     * @param loader
     *            The glTF loader
     * @param extension
     *            The draco extension object that was found in a mesh primitive
     * @return The Draco mesh
     * @throws IOException
     *             If attempting to load the underlying buffer causes an IO error
     */
    private static DracoMesh readDracoMesh(GltfLoader loader, JsonElement extension) throws IOException {
        logger.log(level, "Decoding draco mesh");

        JsonObject jsonObject = extension.getAsJsonObject();
        int bufferViewIndex = getAsInt(jsonObject, "Draco extension object", "bufferView");

        ByteBuffer bufferViewData = obtainBufferViewData(loader, bufferViewIndex);

        byte bufferViewDataArray[] = new byte[bufferViewData.remaining()];
        bufferViewData.slice().get(bufferViewDataArray);
        DracoMesh dracoMesh = null;
        try {
            dracoMesh = (DracoMesh) Draco.decode(bufferViewDataArray);
        } catch (DrakoException e) {
            throw new AssetLoadException("Could not decode Draco mesh from buffer view " + bufferViewIndex,
                    e);
        }

        logger.log(level, "Decoding draco mesh DONE");
        return dracoMesh;
    }

    /**
     * Create the indices vertex buffer that should go into the mesh, based on the given Draco-decoded indices
     * 
     * @param loader
     *            The glTF loader
     * @param accessorIndex
     *            The accessor index of the vertices
     * @param indices
     *            The Draco-decoded indices
     * @return The indices vertex buffer
     * @throws AssetLoadException
     *             If the given component type is not <code>GL_UNSIGNED_BYTE</code>,
     *             <code>GL_UNSIGNED_SHORT</code>, or <code>GL_UNSIGNED_INT</code>
     */
    VertexBuffer createIndicesVertexBuffer(GltfLoader loader, int componentType, int indices[]) {
        Buffer data = null;
        if (componentType == GltfConstants.GL_UNSIGNED_BYTE) {
            data = createByteBuffer(indices);
        } else if (componentType == GltfConstants.GL_UNSIGNED_SHORT) {
            data = createShortBuffer(indices);
        } else if (componentType == GltfConstants.GL_UNSIGNED_INT) {
            data = BufferUtils.createIntBuffer(indices);
        } else {
            throw new AssetLoadException("The indices accessor must have a component type of "
                    + GltfConstants.GL_UNSIGNED_BYTE + ", " + GltfConstants.GL_UNSIGNED_SHORT + ", or "
                    + GltfConstants.GL_UNSIGNED_INT + ", but has " + componentType);
        }
        VertexBuffer vb = new VertexBuffer(VertexBuffer.Type.Index);
        VertexBuffer.Format format = getVertexBufferFormat(componentType);
        int numComponents = 3;
        vb.setupData(VertexBuffer.Usage.Dynamic, numComponents, format, data);
        return vb;
    }

    // TODO_DRACO Could go into GltfUtils
    /**
     * Determines the number of components per element for the given accessor, based on its <code>type</code>
     * 
     * @param accessor
     *            The accessor
     * @return The number of components
     * @throws AssetLoadException
     *             If the accessor does not have a valid <code>type</code> property
     */
    private static int getAccessorComponentCount(JsonObject accessor) {
        String type = getAsString(accessor, "type");
        assertNotNull(type, "No type attribute defined for accessor");
        return getNumberOfComponents(type);
    }

    // TODO_DRACO Could go into BufferUtils
    /**
     * Create a byte buffer containing the given values, cast to <code>byte</code>
     * 
     * @param array
     *            The array
     * @return The buffer
     */
    private static Buffer createByteBuffer(int[] array) {
        ByteBuffer buffer = BufferUtils.createByteBuffer(array.length);
        for (int i = 0; i < array.length; i++) {
            buffer.put(i, (byte) array[i]);
        }
        return buffer;
    }

    // TODO_DRACO Could go into BufferUtils
    /**
     * Create a short buffer containing the given values, cast to <code>short</code>
     * 
     * @param array
     *            The array
     * @return The buffer
     */
    private static Buffer createShortBuffer(int[] array) {
        ShortBuffer buffer = BufferUtils.createShortBuffer(array.length);
        for (int i = 0; i < array.length; i++) {
            buffer.put(i, (short) array[i]);
        }
        return buffer;
    }

    // TODO_DRACO Could fit into GltfLoader
    /**
     * Obtain the data for the specified buffer view of the given loader.
     * 
     * This will return a slice of the data of the underlying buffer. Callers may not modify the returned
     * data.
     * 
     * @param loader
     *            The loader
     * @param bufferViewIndex
     *            The buffer view index
     * @return The buffer view data
     * @throws IOException
     *             If attempting to load the underlying buffer causes an IO error
     * @throws AssetLoadException
     *             If the specified index is not valid, or the buffer view did not define a valid buffer index
     *             or byte length
     */
    private static ByteBuffer obtainBufferViewData(GltfLoader loader, int bufferViewIndex)
            throws IOException {
        JsonObject bufferView = loader.getBufferView(bufferViewIndex);
        int bufferIndex = getAsInt(bufferView, "bufferView", "buffer");
        assertNotNull(bufferIndex, "No buffer defined for bufferView " + bufferViewIndex);

        int byteOffset = getAsInteger(bufferView, "byteOffset", 0);
        int byteLength = getAsInt(bufferView, "bufferView " + bufferViewIndex, "byteLength");

        ByteBuffer bufferData = loader.readData(bufferIndex);
        ByteBuffer bufferViewData = bufferData.slice();
        bufferViewData.limit(byteOffset + byteLength);
        bufferViewData.position(byteOffset);
        return bufferViewData;
    }

    /**
     * Obtains the point attribute with the given ID from the given draco mesh.
     * 
     * @param dracoMesh
     *            The draco mesh
     * @param gltfAttribute
     *            The glTF attribute name, like <code>"POSITION"</code> (only used for error messages)
     * @param id
     *            The unique ID of the attribute, i.e. the value that was stored as the
     *            <code>"POSITION": id</code> in the draco extension JSON object.
     * @return The point attribute
     * @throws AssetLoadException
     *             If the attribute with the given ID cannot be found
     */
    private static PointAttribute getAttribute(DracoMesh dracoMesh, String gltfAttribute, int id) {
        for (int i = 0; i < dracoMesh.getNumAttributes(); i++) {
            PointAttribute attribute = dracoMesh.attribute(i);
            if (attribute.getUniqueId() == id) {
                return attribute;
            }
        }
        throw new AssetLoadException("Could not obtain attribute " + gltfAttribute + " with unique ID " + id
                + " from decoded Draco mesh");
    }

    /**
     * Creates a vertex buffer for the specified attribute, according to the structure that is described by
     * the given accessor JSON object, using the data that is obtained from the given Draco-decoded point
     * attribute
     * 
     * @param attributeName
     *            The attribute name
     * @param accessor
     *            The accessor JSON object
     * @param pointAttribute
     *            The Draco-decoded point attribute
     * @param indices
     *            The indices, obtained from the draco mesh
     * @return The vertex buffer
     * @throws AssetLoadException
     *             If the given accessor does not have a component type that is valid for a vertex attribute
     */
    private static VertexBuffer createAttributeVertexBuffer(String attributeName, JsonObject accessor,
            PointAttribute pointAttribute, int indices[]) {
        int count = getAsInt(accessor, "accessor", "count");
        int componentType = getAsInt(accessor, "accessor", "componentType");
        int componentCount = getAccessorComponentCount(accessor);
        Type bufferType = getVertexBufferType(attributeName);

        if (componentType == GltfConstants.GL_BYTE || componentType == GltfConstants.GL_UNSIGNED_BYTE) {
            ByteBuffer attributeData = readByteDracoAttribute(pointAttribute, indices, count, componentCount);
            VertexBuffer attributeVertexBuffer = createByteAttributeVertexBuffer(accessor, bufferType,
                    attributeData);
            return attributeVertexBuffer;
        }
        if (componentType == GltfConstants.GL_SHORT || componentType == GltfConstants.GL_UNSIGNED_SHORT) {
            ShortBuffer attributeData = readShortDracoAttribute(pointAttribute, indices, count,
                    componentCount);
            VertexBuffer attributeVertexBuffer = createShortAttributeVertexBuffer(accessor, bufferType,
                    attributeData);
            return attributeVertexBuffer;
        }
        if (componentType == GltfConstants.GL_FLOAT) {
            FloatBuffer attributeData = readFloatDracoAttribute(pointAttribute, indices, count,
                    componentCount);
            VertexBuffer attributeVertexBuffer = createFloatAttributeVertexBuffer(accessor, bufferType,
                    attributeData);
            return attributeVertexBuffer;
        }
        throw new AssetLoadException(
                "The accessor for attribute " + attributeName + " must have a component type of "
                        + GltfConstants.GL_BYTE + ", " + GltfConstants.GL_UNSIGNED_BYTE + ", "
                        + GltfConstants.GL_SHORT + ", " + GltfConstants.GL_UNSIGNED_SHORT + ", " + "or "
                        + GltfConstants.GL_FLOAT + ", but has " + componentType);
    }

    /**
     * Read the data from the given point attribute, as <code>byte</code> values
     * 
     * @param pointAttribute
     *            The Draco-decoded point attribute
     * @param indices
     *            The indices, obtained from the draco mesh
     * @param count
     *            The count, obtained from the accessor for this attribute
     * @param componentCount
     *            The component count (number of components per element), obtained from the accessor type
     * @return The resulting data, as a byte buffer
     */
    private static ByteBuffer readByteDracoAttribute(PointAttribute pointAttribute, int indices[], int count,
            int componentCount) {
        int numFaces = indices.length / 3;
        byte p[] = new byte[componentCount];
        ByteBuffer attributeData = BufferUtils.createByteBuffer(count * componentCount);
        for (int i = 0; i < numFaces; i++) {
            int j0 = indices[i * 3 + 0];
            int j1 = indices[i * 3 + 1];
            int j2 = indices[i * 3 + 2];

            int mj0 = pointAttribute.mappedIndex(j0);
            int mj1 = pointAttribute.mappedIndex(j1);
            int mj2 = pointAttribute.mappedIndex(j2);

            pointAttribute.getValue(mj0, p);
            int offset0 = j0 * componentCount;
            for (int c = 0; c < componentCount; c++) {
                attributeData.put(offset0 + c, p[c]);
            }
            pointAttribute.getValue(mj1, p);
            int offset1 = j1 * componentCount;
            for (int c = 0; c < componentCount; c++) {
                attributeData.put(offset1 + c, p[c]);
            }
            pointAttribute.getValue(mj2, p);
            int offset2 = j2 * componentCount;
            for (int c = 0; c < componentCount; c++) {
                attributeData.put(offset2 + c, p[c]);
            }
        }
        return attributeData;
    }

    /**
     * Read the data from the given point attribute, as <code>short</code> values
     * 
     * @param pointAttribute
     *            The Draco-decoded point attribute
     * @param indices
     *            The indices, obtained from the draco mesh
     * @param count
     *            The count, obtained from the accessor for this attribute
     * @param componentCount
     *            The component count (number of components per element), obtained from the accessor type
     * @return The resulting data, as a short buffer
     */
    private static ShortBuffer readShortDracoAttribute(PointAttribute pointAttribute, int indices[],
            int count, int componentCount) {
        int numFaces = indices.length / 3;
        short p[] = new short[componentCount];
        ShortBuffer attributeData = BufferUtils.createShortBuffer(count * componentCount);
        for (int i = 0; i < numFaces; i++) {
            int j0 = indices[i * 3 + 0];
            int j1 = indices[i * 3 + 1];
            int j2 = indices[i * 3 + 2];

            int mj0 = pointAttribute.mappedIndex(j0);
            int mj1 = pointAttribute.mappedIndex(j1);
            int mj2 = pointAttribute.mappedIndex(j2);

            pointAttribute.getValue(mj0, p);
            int offset0 = j0 * componentCount;
            for (int c = 0; c < componentCount; c++) {
                attributeData.put(offset0 + c, p[c]);
            }
            pointAttribute.getValue(mj1, p);
            int offset1 = j1 * componentCount;
            for (int c = 0; c < componentCount; c++) {
                attributeData.put(offset1 + c, p[c]);
            }
            pointAttribute.getValue(mj2, p);
            int offset2 = j2 * componentCount;
            for (int c = 0; c < componentCount; c++) {
                attributeData.put(offset2 + c, p[c]);
            }
        }
        return attributeData;
    }

    /**
     * Read the data from the given point attribute, as <code>float</code> values
     * 
     * @param pointAttribute
     *            The Draco-decoded point attribute
     * @param indices
     *            The indices, obtained from the draco mesh
     * @param count
     *            The count, obtained from the accessor for this attribute
     * @param componentCount
     *            The component count (number of components per element), obtained from the accessor type
     * @return The resulting data, as a float buffer
     */
    private static FloatBuffer readFloatDracoAttribute(PointAttribute pointAttribute, int indices[],
            int count, int componentCount) {
        int numFaces = indices.length / 3;
        float p[] = new float[componentCount];
        FloatBuffer attributeData = BufferUtils.createFloatBuffer(count * componentCount);
        for (int i = 0; i < numFaces; i++) {
            int j0 = indices[i * 3 + 0];
            int j1 = indices[i * 3 + 1];
            int j2 = indices[i * 3 + 2];

            int mj0 = pointAttribute.mappedIndex(j0);
            int mj1 = pointAttribute.mappedIndex(j1);
            int mj2 = pointAttribute.mappedIndex(j2);

            pointAttribute.getValue(mj0, p);
            int offset0 = j0 * componentCount;
            for (int c = 0; c < componentCount; c++) {
                attributeData.put(offset0 + c, p[c]);
            }
            pointAttribute.getValue(mj1, p);
            int offset1 = j1 * componentCount;
            for (int c = 0; c < componentCount; c++) {
                attributeData.put(offset1 + c, p[c]);
            }
            pointAttribute.getValue(mj2, p);
            int offset2 = j2 * componentCount;
            for (int c = 0; c < componentCount; c++) {
                attributeData.put(offset2 + c, p[c]);
            }
        }
        return attributeData;
    }

    /**
     * Create the vertex buffer for the given <code>byte</code> attribute data
     * 
     * @param accessor
     *            The accessor that describes the component type and type
     * 
     * @param bufferType
     *            The buffer type
     * @param attributeData
     *            The attribute data
     * @return The vertex buffer
     */
    private static VertexBuffer createByteAttributeVertexBuffer(JsonObject accessor,
            VertexBuffer.Type bufferType, ByteBuffer attributeData) {
        int componentType = getAsInt(accessor, "accessor", "componentType");
        VertexBuffer vb = new VertexBuffer(bufferType);
        VertexBuffer.Format format = getVertexBufferFormat(componentType);
        int numComponents = getAccessorComponentCount(accessor);
        vb.setupData(VertexBuffer.Usage.Dynamic, numComponents, format, attributeData);
        return vb;
    }

    /**
     * Create the vertex buffer for the given <code>short</code> attribute data
     * 
     * @param accessor
     *            The accessor that describes the component type and type
     * 
     * @param bufferType
     *            The buffer type
     * @param attributeData
     *            The attribute data
     * @return The vertex buffer
     */
    private static VertexBuffer createShortAttributeVertexBuffer(JsonObject accessor,
            VertexBuffer.Type bufferType, ShortBuffer attributeData) {
        int componentType = getAsInt(accessor, "accessor", "componentType");
        VertexBuffer vb = new VertexBuffer(bufferType);
        VertexBuffer.Format format = getVertexBufferFormat(componentType);
        int numComponents = getAccessorComponentCount(accessor);
        vb.setupData(VertexBuffer.Usage.Dynamic, numComponents, format, attributeData);
        return vb;
    }

    /**
     * Create the vertex buffer for the given <code>float</code> attribute data
     * 
     * @param accessor
     *            The accessor that describes the component type and type
     * 
     * @param bufferType
     *            The buffer type
     * @param attributeData
     *            The attribute data
     * @return The vertex buffer
     */
    private static VertexBuffer createFloatAttributeVertexBuffer(JsonObject accessor,
            VertexBuffer.Type bufferType, FloatBuffer attributeData) {
        int componentType = getAsInt(accessor, "accessor", "componentType");
        VertexBuffer vb = new VertexBuffer(bufferType);
        VertexBuffer.Format format = getVertexBufferFormat(componentType);
        int numComponents = getAccessorComponentCount(accessor);
        vb.setupData(VertexBuffer.Usage.Dynamic, numComponents, format, attributeData);
        return vb;
    }

}

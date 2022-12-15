/*
 * Copyright (c) 2009-2022 jMonkeyEngine
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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jme3.asset.AssetLoadException;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import static com.jme3.scene.plugins.gltf.GltfUtils.getAsInteger;
import static com.jme3.scene.plugins.gltf.GltfUtils.getVertexBufferType;
import com.jme3.texture.Texture2D;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thread-safe extension loader for KHR_texture_transform. 
 * It allows for UV coordinates to be scaled/rotated/translated  
 * based on transformation properties from textures in the glTF model.
 *
 * See spec at https://github.com/KhronosGroup/glTF/tree/main/extensions/2.0/Khronos/KHR_texture_transform
 *
 * @author manuelrmo - Created on 11/20/2022
 */
public class TextureTransformExtensionLoader implements ExtensionLoader {
    
    private final static Logger logger = Logger.getLogger(TextureTransformExtensionLoader.class.getName());
        
    /**
     * Scale/rotate/translate UV coordinates based on a transformation matrix.
     * Code adapted from scaleTextureCoordinates(Vector2f) in jme3-core/src/main/java/com/jme3/scene/Mesh.java
     * @param mesh The mesh holding the UV coordinates
     * @param transform The matrix containing the scale/rotate/translate transformations
     * @param verType The vertex buffer type from which to retrieve the UV coordinates
     */    
    private void uvTransform(Mesh mesh, Matrix3f transform, VertexBuffer.Type verType) {
        if (!transform.isIdentity()) { // if transform is the identity matrix, there's nothing to do
            VertexBuffer tc = mesh.getBuffer(verType);
            if (tc == null) {
                throw new IllegalStateException("The mesh has no texture coordinates");
            }
            if (tc.getFormat() != VertexBuffer.Format.Float) {
                throw new UnsupportedOperationException("Only float texture coord format is supported");
            }
            if (tc.getNumComponents() != 2) {
                throw new UnsupportedOperationException("Only 2D texture coords are supported");
            }
            FloatBuffer fb = (FloatBuffer) tc.getData();
            fb.clear();
            for (int i = 0; i < fb.limit() / 2; i++) {
                float x = fb.get();
                float y = fb.get();
                fb.position(fb.position() - 2);
                Vector3f v = transform.mult(new Vector3f(x, y, 1));
                fb.put(v.getX()).put(v.getY());
            }
            fb.clear();
            tc.updateData(fb);   
        }
    }
    
    // The algorithm relies on the fact that the GltfLoader.class object 
    // loads all textures of a given mesh before doing so for the next mesh.    
    @Override
    public Object handleExtension(GltfLoader loader, String parentName, JsonElement parent, JsonElement extension, Object input) throws IOException {
        if (!(input instanceof Texture2D)) {
            logger.log(Level.WARNING, "KHR_texture_transform extension added on an unsupported element, the loaded scene result will be unexpected.");
        }
        Mesh mesh = loader.fetchFromCache("mesh", 0, Mesh.class);
        if (mesh != null) {
            Matrix3f translation = new Matrix3f();
            Matrix3f rotation = new Matrix3f();
            Matrix3f scale = new Matrix3f();
            Integer texCoord = getAsInteger(parent.getAsJsonObject(), "texCoord");
            texCoord = texCoord != null ? texCoord : 0;
            JsonObject jsonObject = extension.getAsJsonObject();
            if (jsonObject.has("offset")) {
                JsonArray jsonArray = jsonObject.getAsJsonArray("offset");
                translation.set(0, 2, jsonArray.get(0).getAsFloat());
                translation.set(1, 2, jsonArray.get(1).getAsFloat());                    
            }
            if (jsonObject.has("rotation")) {
                float rad = jsonObject.get("rotation").getAsFloat();
                rotation.set(0, 0, (float) Math.cos(rad));
                rotation.set(0, 1, (float) Math.sin(rad));
                rotation.set(1, 0, (float) -Math.sin(rad));
                rotation.set(1, 1, (float) Math.cos(rad));
            }                
            if (jsonObject.has("scale")) {
                JsonArray jsonArray = jsonObject.getAsJsonArray("scale");
                scale.set(0, 0, jsonArray.get(0).getAsFloat());
                scale.set(1, 1, jsonArray.get(1).getAsFloat());
            }     
            if (jsonObject.has("texCoord")) {
                texCoord = jsonObject.get("texCoord").getAsInt(); // it overrides the parent's texCoord value
            }                 
            Matrix3f transform = translation.mult(rotation).mult(scale);
            Mesh meshLast = loader.fetchFromCache("textureTransformData", 0, Mesh.class);
            Map<Integer, Matrix3f> transformMap = loader.fetchFromCache("textureTransformData", 1, HashMap.class);
            if (mesh != meshLast || (transformMap != null && transformMap.get(texCoord) == null)) {
                // at this point, we're processing a new mesh or the same mesh as before but for a different UV set
                if (mesh != meshLast) { // it's a new mesh
                    loader.addToCache("textureTransformData", 0, mesh, 2);
                    if (transformMap == null) {
                        transformMap = new HashMap<>(); // initialize transformMap
                        loader.addToCache("textureTransformData", 1, transformMap, 2);
                    } else {
                        transformMap.clear(); // reset transformMap
                    }
                }
                transformMap.put(texCoord, transform); // store the transformation matrix applied to this UV set
                uvTransform(mesh, transform, getVertexBufferType("TEXCOORD_" + texCoord));
                logger.log(Level.FINE, "KHR_texture_transform extension successfully applied."); 
            }
            else {
                // at this point, we're processing the same mesh as before for an already transformed UV set
                Matrix3f transformLast = transformMap.get(texCoord);
                if (!transform.equals(transformLast)) {
                    logger.log(Level.WARNING, "KHR_texture_transform extension: use of different texture transforms for the same mesh's UVs is not supported, the loaded scene result will be unexpected.");
                }
            }
            return input;
        }
        else {
            throw new AssetLoadException("KHR_texture_transform extension applied to a null mesh.");
        }
    }
}

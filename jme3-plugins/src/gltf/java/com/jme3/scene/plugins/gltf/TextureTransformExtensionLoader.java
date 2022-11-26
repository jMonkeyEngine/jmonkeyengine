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
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.texture.Texture2D;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Extension loader for KHR_texture_transform, which allows for
 * UV coordinates to be scaled/rotated/translated based on 
 * transformation properties from textures in the glTF model.
 *
 * See spec at https://github.com/KhronosGroup/glTF/tree/main/extensions/2.0/Khronos/KHR_texture_transform
 *
 * @author manuelrmo - Created on 11/20/2022
 */
public class TextureTransformExtensionLoader implements ExtensionLoader {
    
    private final static Logger logger = Logger.getLogger(TextureTransformExtensionLoader.class.getName());
    
    private Geometry geomLast = null; // Last geometry created by the GltfLoader.class object
    private Matrix3f mInvLast = null; // Last transformation matrix (inverted) that was applied
        
    /**
     * Scale/rotate/translate UV coordinates based on a transformation matrix.
     * Code adapted from scaleTextureCoordinates(Vector2f) in jme3-core/src/main/java/com/jme3/scene/Mesh.java
     * @param mesh The mesh holding the UV coordinates
     * @param m The matrix containing the scale/rotate/translate transformations
     */    
    private void uvTransform(Mesh mesh, Matrix3f m) {
        VertexBuffer tc = mesh.getBuffer(VertexBuffer.Type.TexCoord);
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
            Vector3f v = m.mult(new Vector3f(x, y, 1));
            fb.put(v.getX()).put(v.getY());
        }
        fb.clear();
        tc.updateData(fb);        
    }
    
    @Override
    public Object handleExtension(GltfLoader loader, String parentName, JsonElement parent, JsonElement extension, Object input) throws IOException {
        if (input instanceof Texture2D) {
            Geometry geom = loader.getLastCreatedGeom();
            if (geom != null) {
                Matrix3f t = new Matrix3f();
                Matrix3f r = new Matrix3f();
                Matrix3f s = new Matrix3f();
                JsonObject jsonObject = extension.getAsJsonObject();
                if (jsonObject.has("offset")) {
                    JsonArray jsonArray = jsonObject.getAsJsonArray("offset");
                    t.set(2, 0, jsonArray.get(0).getAsFloat());
                    t.set(2, 1, jsonArray.get(1).getAsFloat());                    
                }
                if (jsonObject.has("rotation")) {
                    float rad = jsonObject.get("rotation").getAsFloat();
                    r.set(0, 0, (float) Math.cos(rad));
                    r.set(0, 1, (float) Math.sin(rad));
                    r.set(1, 0, (float) -Math.sin(rad));
                    r.set(1, 1, (float) Math.cos(rad));
                }                
                if (jsonObject.has("scale")) {
                    JsonArray jsonArray = jsonObject.getAsJsonArray("scale");
                    s.set(0, 0, jsonArray.get(0).getAsFloat());
                    s.set(1, 1, jsonArray.get(1).getAsFloat());
                }     
                if (jsonObject.has("texCoord")) {
                    logger.log(Level.WARNING, "KHR_texture_transform extension: the texCoord property is not supported");                
                }                 
                Matrix3f m = t.mult(r).mult(s);
                if (geom != geomLast) {
                    geomLast = geom;
                    mInvLast = m.invert();
                    uvTransform(geom.getMesh(), m);
                    logger.log(Level.FINE, "KHR_texture_transform extension successfully applied"); 
                }
                else {
                    if (!m.mult(mInvLast).isIdentity()) {
                        logger.log(Level.WARNING, "KHR_texture_transform extension: use of different texture transforms for the same geometry is not supported");
                    }
                }
                return input;
            }
            else {
                throw new AssetLoadException("KHR_texture_transform extension applied to a null geometry");
            }
        } 
        else {
            throw new AssetLoadException("KHR_texture_transform extension added on an unsupported element");
        }        
    }
}

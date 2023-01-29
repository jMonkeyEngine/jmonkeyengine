/*
 * Copyright (c) 2009-2023 jMonkeyEngine
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
package com.jme3.scene.plugins.fbx.mesh;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.plugins.fbx.file.FbxElement;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FbxLayerElement {
    
    private static final Logger logger = Logger.getLogger(FbxLayerElement.class.getName());
    
    public enum Type {
        Position,      // Vector3f (isn't actually defined in FBX)
        BoneIndex,     // List<Integer> (isn't actually defined in FBX)
        BoneWeight,    // List<Float> (isn't actually defined in FBX)
        Normal,        // Vector3f
        Binormal,      // Vector3f
        Tangent,       // Vector3f
        UV,            // Vector2f
        TransparentUV, // Vector2f
        Color,         // ColorRGBA
        Material,      // Integer
        Smoothing,     // Integer
        Visibility,    // Integer
        Texture,      // ??? (FBX 6.x)
        PolygonGroup, // ??? (FBX 6.x)
        NormalMapTextures, // ??? (FBX 6.x)
        SpecularFactorUV, // ??? (FBX 6.x)
        NormalMapUV, // ??? (FBX 6.x)
        SpecularFactorTextures, // ??? (FBX 6.x)
        
    }
    
    public enum MappingInformationType {
        NoMappingInformation,
        AllSame,
        ByPolygonVertex,
        ByVertex,
        ByPolygon,
        ByEdge;
    }
    
    public enum ReferenceInformationType {
        Direct,
        IndexToDirect;
    }
    
    public enum TextureBlendMode {
        Translucent;
    }
    
    private static final Set<String> indexTypes = new HashSet<String>();
    
    static {
        indexTypes.add("UVIndex");
        indexTypes.add("NormalsIndex");
        indexTypes.add("TangentsIndex");
        indexTypes.add("BinormalsIndex");
        indexTypes.add("Smoothing");
        indexTypes.add("Materials");
        indexTypes.add("TextureId");
        indexTypes.add("ColorIndex");
        indexTypes.add("PolygonGroup");
    }
    
    int index;
    Type type;
    ReferenceInformationType refInfoType;
    MappingInformationType mapInfoType;
    String name = "";
    Object[] data;
    int[] dataIndices;

    private FbxLayerElement() { }
    
    @Override
    public String toString() {
        return "LayerElement[type=" + type + ", layer=" + index +
               ", mapInfoType=" + mapInfoType + ", refInfoType=" + refInfoType + "]";
    }
    
    private Object getVertexDataIndexToDirect(int polygonIndex, int polygonVertexIndex, 
                                              int positionIndex, int edgeIndex) {
        switch (mapInfoType) {
            case AllSame:           return data[dataIndices[0]];
            case ByPolygon:         return data[dataIndices[polygonIndex]];
            case ByPolygonVertex:   return data[dataIndices[polygonVertexIndex]];
            case ByVertex:          return data[dataIndices[positionIndex]];
            case ByEdge:            return data[dataIndices[edgeIndex]];
            default:                throw new UnsupportedOperationException();
        }
    }
    
    private Object getVertexDataDirect(int polygonIndex, int polygonVertexIndex, 
                                              int positionIndex, int edgeIndex) {
        switch (mapInfoType) {
            case AllSame:           return data[0];
            case ByPolygon:         return data[polygonIndex];
            case ByPolygonVertex:   return data[polygonVertexIndex];
            case ByVertex:          return data[positionIndex];
            case ByEdge:            return data[edgeIndex];
            default:                throw new UnsupportedOperationException();
        }
    }
    
    public Object getVertexData(int polygonIndex, int polygonVertexIndex, int positionIndex, int edgeIndex) {
        switch (refInfoType) {
            case Direct:        return getVertexDataDirect(polygonIndex, polygonVertexIndex, positionIndex, edgeIndex);
            case IndexToDirect: return getVertexDataIndexToDirect(polygonIndex, polygonVertexIndex, positionIndex, edgeIndex);
            default:            return null;
        }
    }
    
    public static FbxLayerElement fromPositions(double[] positionData) {
        FbxLayerElement layerElement = new FbxLayerElement();
        layerElement.index = -1;
        layerElement.name = "";
        layerElement.type = Type.Position;
        layerElement.mapInfoType = MappingInformationType.ByVertex;
        layerElement.refInfoType = ReferenceInformationType.Direct;
        layerElement.data = toVector3(positionData);
        layerElement.dataIndices = null;
        return layerElement;
    }
    
    public static FbxLayerElement fromElement(FbxElement element) {
        FbxLayerElement layerElement = new FbxLayerElement();
        if (!element.id.startsWith("LayerElement")) {
            throw new IllegalArgumentException("Not a layer element");
        }
        layerElement.index = (Integer)element.properties.get(0);
        
        String elementType = element.id.substring("LayerElement".length());
        try {
            layerElement.type = Type.valueOf(elementType);
        } catch (IllegalArgumentException ex) {
            logger.log(Level.WARNING, "Unsupported layer element: {0}. Ignoring.", elementType);
        }
        for (FbxElement child : element.children) {
            if (child.id.equals("MappingInformationType")) {
                String mapInfoTypeVal = (String) child.properties.get(0);
                if (mapInfoTypeVal.equals("ByVertice")) {
                    mapInfoTypeVal = "ByVertex";
                }
                layerElement.mapInfoType = MappingInformationType.valueOf(mapInfoTypeVal);
            } else if (child.id.equals("ReferenceInformationType")) {
                String refInfoTypeVal = (String) child.properties.get(0);
                if (refInfoTypeVal.equals("Index")) {
                    refInfoTypeVal = "IndexToDirect";
                }
                layerElement.refInfoType = ReferenceInformationType.valueOf(refInfoTypeVal);
            } else if (child.id.equals("Normals") || child.id.equals("Tangents") || child.id.equals("Binormals")) {
                layerElement.data = toVector3(FbxMeshUtil.getDoubleArray(child));
            } else if (child.id.equals("Colors")) {
                layerElement.data = toColorRGBA(FbxMeshUtil.getDoubleArray(child));
            } else if (child.id.equals("UV")) {
                layerElement.data = toVector2(FbxMeshUtil.getDoubleArray(child));
            } else if (indexTypes.contains(child.id)) {
                layerElement.dataIndices = FbxMeshUtil.getIntArray(child);
            } else if (child.id.equals("Name")) {
                layerElement.name = (String) child.properties.get(0);
            }
        }
        if (layerElement.data == null && layerElement.dataIndices != null) {
            // For Smoothing / Materials, data = dataIndices
            layerElement.refInfoType = ReferenceInformationType.Direct;
            layerElement.data = new Integer[layerElement.dataIndices.length];
            for (int i = 0; i < layerElement.data.length; i++) {
                layerElement.data[i] = layerElement.dataIndices[i];
            }
            layerElement.dataIndices = null;
        }
        return layerElement;
    }
    
    static Vector3f[] toVector3(double[] data) {
        Vector3f[] vectors = new Vector3f[data.length / 3];
        for (int i = 0; i < vectors.length; i++) {
            float x = (float) data[i * 3];
            float y = (float) data[i * 3 + 1];
            float z = (float) data[i * 3 + 2];
            vectors[i] = new Vector3f(x, y, z);
        }
        return vectors;
    }

    static Vector2f[] toVector2(double[] data) {
        Vector2f[] vectors = new Vector2f[data.length / 2];
        for (int i = 0; i < vectors.length; i++) {
            float x = (float) data[i * 2];
            float y = (float) data[i * 2 + 1];
            vectors[i] = new Vector2f(x, y);
        }
        return vectors;
    }
    
    static ColorRGBA[] toColorRGBA(double[] data) {
        ColorRGBA[] colors = new ColorRGBA[data.length / 4];
        for (int i = 0; i < colors.length; i++) {
            float r = (float) data[i * 4];
            float g = (float) data[i * 4 + 1];
            float b = (float) data[i * 4 + 2];
            float a = (float) data[i * 4 + 3];
            colors[i] = new ColorRGBA(r, g, b, a);
        }
        return colors;
    }
}


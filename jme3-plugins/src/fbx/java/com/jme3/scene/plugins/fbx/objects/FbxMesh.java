package com.jme3.scene.plugins.fbx.objects;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.jme3.asset.AssetLoadException;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.plugins.fbx.SceneLoader;
import com.jme3.scene.plugins.fbx.file.FbxElement;
import com.jme3.scene.Mesh.Mode;
import com.jme3.scene.Node;
import com.jme3.util.BufferUtils;
import com.jme3.util.IntMap;
import com.jme3.util.IntMap.Entry;

public class FbxMesh extends FbxObject {

    public double[] vertices;
    public int[] indices;
    public int[] edges;
    public String normalsMapping;
    public String normalsReference;
    public double[] normals;
    public String tangentsMapping;
    public String tangentsReference;
    public double[] tangents;
    public String binormalsMapping;
    public String binormalsReference;
    public double[] binormals;
    public String uvMapping;
    public String uvReference;
    public double[] uv;
    public int[] uvIndex;
    public List<int[]> uvIndexes = new ArrayList<>();
    public List<double[]> uvs = new ArrayList<>();
    public String smoothingMapping;
    public String smoothingReference;
    public int[] smoothing;
    public String materialsMapping;
    public String materialsReference;
    public int[] materials;
    // Build helping data
    public int iCount;
    public int vCount;
    public int srcVertexCount;
    public List<Integer> vertexMap; // Target vertex -> source vertex
    public List<List<Integer>> reverseVertexMap; // source vertex -> list of target vertices
    public List<Integer> indexMap; // Target vertex -> source index

    public List<Geometry> geometries; // One mesh can be split in two geometries in case of by-polygon material mapping
    public FbxNode parent;
    public int lastMaterialId = 0;

    public FbxMesh(SceneLoader scene, FbxElement element) throws IOException {
        super(scene, element);
        if(type.equals("Mesh")) {
            data: for(FbxElement e : element.children) {
                switch(e.id) {
                case "Vertices":
                    vertices = (double[]) e.properties.get(0);
                    break;
                case "PolygonVertexIndex":
                    indices = (int[]) e.properties.get(0);
                    break;
                // TODO edges are not used now
                /*case "Edges":
                    edges = (int[]) e.properties.get(0);
                    break;*/
                case "LayerElementNormal":
                    for(FbxElement e2 : e.children) {
                        switch(e2.id) {
                        case "MappingInformationType":
                            normalsMapping = (String) e2.properties.get(0);
                            if(!normalsMapping.equals("ByVertice") && !normalsMapping.equals("ByPolygonVertex")) {
                                if(SceneLoader.WARN_IGNORED_ATTRIBUTES)
                                    scene.warning("Ignored LayerElementNormal.MappingInformationType attribute (" + normalsReference + ")");
                                continue data;
                            }
                            break;
                        case "ReferenceInformationType":
                            normalsReference = (String) e2.properties.get(0);
                            if(!normalsReference.equals("Direct")) {
                                if(SceneLoader.WARN_IGNORED_ATTRIBUTES)
                                    scene.warning("Ignored LayerElementNormal.ReferenceInformationType attribute (" + normalsReference + ")");
                                continue data;
                            }
                            break;
                        case "Normals":
                            normals = (double[]) e2.properties.get(0);
                            break;
                        }
                    }
                    break;
                case "LayerElementTangent":
                    for(FbxElement e2 : e.children) {
                        switch(e2.id) {
                        case "MappingInformationType":
                            tangentsMapping = (String) e2.properties.get(0);
                            if(!tangentsMapping.equals("ByVertice") && !tangentsMapping.equals("ByPolygonVertex")) {
                                if(SceneLoader.WARN_IGNORED_ATTRIBUTES)
                                    scene.warning("Ignored LayerElementTangent.MappingInformationType attribute (" + tangentsMapping + ")");
                                continue data;
                            }
                            break;
                        case "ReferenceInformationType":
                            tangentsReference = (String) e2.properties.get(0);
                            if(!tangentsReference.equals("Direct")) {
                                if(SceneLoader.WARN_IGNORED_ATTRIBUTES)
                                    scene.warning("Ignored LayerElementTangent.ReferenceInformationType attribute (" + tangentsReference + ")");
                                continue data;
                            }
                            break;
                        case "Tangents":
                            tangents = (double[]) e2.properties.get(0);
                            break;
                        }
                    }
                    break;
                case "LayerElementBinormal":
                    for(FbxElement e2 : e.children) {
                        switch(e2.id) {
                        case "MappingInformationType":
                            binormalsMapping = (String) e2.properties.get(0);
                            if(!binormalsMapping.equals("ByVertice") && !binormalsMapping.equals("ByPolygonVertex")) {
                                if(SceneLoader.WARN_IGNORED_ATTRIBUTES)
                                    scene.warning("Ignored LayerElementBinormal.MappingInformationType attribute (" + binormalsMapping + ")");
                                continue data;
                            }
                            break;
                        case "ReferenceInformationType":
                            binormalsReference = (String) e2.properties.get(0);
                            if(!binormalsReference.equals("Direct")) {
                                if(SceneLoader.WARN_IGNORED_ATTRIBUTES)
                                    scene.warning("Ignored LayerElementBinormal.ReferenceInformationType attribute (" + binormalsReference + ")");
                                continue data;
                            }
                            break;
                        case "Tangents":
                            binormals = (double[]) e2.properties.get(0);
                            break;
                        }
                    }
                    break;
                case "LayerElementUV":
                    for(FbxElement e2 : e.children) {
                        switch(e2.id) {
                        case "MappingInformationType":
                            uvMapping = (String) e2.properties.get(0);
                            if(!uvMapping.equals("ByPolygonVertex")) {
                                if(SceneLoader.WARN_IGNORED_ATTRIBUTES)
                                    scene.warning("Ignored LayerElementUV.MappingInformationType attribute (" + uvMapping + ")");
                                continue data;
                            }
                            break;
                        case "ReferenceInformationType":
                            uvReference = (String) e2.properties.get(0);
                            if(!uvReference.equals("IndexToDirect")) {
                                if(SceneLoader.WARN_IGNORED_ATTRIBUTES)
                                    scene.warning("Ignored LayerElementUV.ReferenceInformationType attribute (" + uvReference + ")");
                                continue data;
                            }
                            break;
                        case "UV":
                            uv = (double[]) e2.properties.get(0);
                            uvs.add(uv);
                            break;
                        case "UVIndex":
                            uvIndex = (int[]) e2.properties.get(0);
                            uvIndexes.add(uvIndex);
                            break;
                        }
                    }
                    break;
                // TODO smoothing is not used now
                /*case "LayerElementSmoothing":
                    for(FBXElement e2 : e.children) {
                        switch(e2.id) {
                        case "MappingInformationType":
                            smoothingMapping = (String) e2.properties.get(0);
                            if(!smoothingMapping.equals("ByEdge"))
                                throw new AssetLoadException("Not supported LayerElementSmoothing.MappingInformationType = " + smoothingMapping);
                            break;
                        case "ReferenceInformationType":
                            smoothingReference = (String) e2.properties.get(0);
                            if(!smoothingReference.equals("Direct"))
                                throw new AssetLoadException("Not supported LayerElementSmoothing.ReferenceInformationType = " + smoothingReference);
                            break;
                        case "Smoothing":
                            smoothing = (int[]) e2.properties.get(0);
                            break;
                        }
                    }
                    break;*/
                case "LayerElementMaterial":
                    for(FbxElement e2 : e.children) {
                        switch(e2.id) {
                        case "MappingInformationType":
                            materialsMapping = (String) e2.properties.get(0);
                            if(!materialsMapping.equals("AllSame") && !materialsMapping.equals("ByPolygon")) {
                                if(SceneLoader.WARN_IGNORED_ATTRIBUTES)
                                    scene.warning("Ignored LayerElementMaterial.MappingInformationType attribute (" + materialsMapping + ")");
                                continue data;
                            }
                            break;
                        case "ReferenceInformationType":
                            materialsReference = (String) e2.properties.get(0);
                            if(!materialsReference.equals("IndexToDirect")) {
                                if(SceneLoader.WARN_IGNORED_ATTRIBUTES)
                                    scene.warning("Ignored LayerElementMaterial.ReferenceInformationType attribute (" + materialsReference + ")");
                                continue data;
                            }
                            break;
                        case "Materials":
                            materials = (int[]) e2.properties.get(0);
                            break;
                        }
                    }
                    break;
                }
            }
            geometries = createGeometries();
        }
    }

    public void setParent(Node node) {
        for(int i = 0; i < geometries.size(); ++i) {
            Geometry geom = geometries.get(i);
            geom.setName(node.getName() + (i > 0 ? "-" + i : ""));
            geom.updateModelBound();
            node.attachChild(geom);
        }
    }

    @Override
    public void linkToZero() {
        setParent(scene.sceneNode);
    }

    public void clearMaterials() {
        for(Geometry g : geometries) {
            if(g.getUserData("FBXMaterial") != null)
                g.setUserData("FBXMaterial", null);
        }
    }

    @Override
    public void link(FbxObject otherObject) {
        if(otherObject instanceof FbxSkin) {
            FbxSkin skin = (FbxSkin) otherObject;
            skin.toSkin.add(this);
        }
    }

    private List<Geometry> createGeometries() throws IOException {
        Mesh mesh = new Mesh();
        mesh.setMode(Mode.Triangles);
        // Since each vertex should contain unique texcoord and normal we should unroll vertex indexing
        // So we don't use VertexBuffer.Type.Index for elements drawing
        // Moreover quads should be triangulated (this increases number of vertices)
        if(indices != null) {
            iCount = indices.length;
            srcVertexCount = vertices.length / 3;
            // Indices contain negative numbers to define polygon last index.
            // Check index strides to be sure we have triangles or quads.
            vCount = 0;
            // Count number of vertices to be produced
            int polyVertCount = 0;
            for(int i = 0; i < iCount; ++i) {
                int index = indices[i];
                polyVertCount++;
                if(index < 0) {
                    if(polyVertCount == 3) {
                        vCount += 3; // A triangle
                    } else if(polyVertCount == 4) {
                        vCount += 6; // A quad produce two triangles
                    } else {
                        throw new AssetLoadException("Unsupported PolygonVertexIndex stride");
                    }
                    polyVertCount = 0;
                }
            }
            // Unroll index array into vertex mapping
            vertexMap = new ArrayList<>(vCount);
            indexMap = new ArrayList<>(vCount);
            polyVertCount = 0;
            for(int i = 0; i < iCount; ++i) {
                int index = indices[i];
                polyVertCount++;
                if(index < 0) {
                    int lastIndex = -(index + 1);
                    if(polyVertCount == 3) {
                        vertexMap.add(indices[i - 2]);
                        vertexMap.add(indices[i - 1]);
                        vertexMap.add(lastIndex);
                        indexMap.add(i - 2);
                        indexMap.add(i - 1);
                        indexMap.add(i - 0);
                    } else if(polyVertCount == 4) {
                        vertexMap.add(indices[i - 3]);
                        vertexMap.add(indices[i - 2]);
                        vertexMap.add(indices[i - 1]);
                        vertexMap.add(indices[i - 3]);
                        vertexMap.add(indices[i - 1]);
                        vertexMap.add(lastIndex);
                        indexMap.add(i - 3);
                        indexMap.add(i - 2);
                        indexMap.add(i - 1);
                        indexMap.add(i - 3);
                        indexMap.add(i - 1);
                        indexMap.add(i - 0);
                    }
                    polyVertCount = 0;
                }
            }
            // Build reverse vertex mapping
            reverseVertexMap = new ArrayList<>(srcVertexCount);
            for(int i = 0; i < srcVertexCount; ++i)
                reverseVertexMap.add(new ArrayList<Integer>());
            for(int i = 0; i < vCount; ++i) {
                int index = vertexMap.get(i);
                reverseVertexMap.get(index).add(i);
            }
        } else {
            // Stub for no vertex indexing (direct mapping)
            iCount = vCount = srcVertexCount;
            vertexMap = new ArrayList<>(vCount);
            indexMap = new ArrayList<>(vCount);
            reverseVertexMap = new ArrayList<>(vCount);
            for(int i = 0; i < vCount; ++i) {
                vertexMap.set(i, i);
                indexMap.set(i, i);
                List<Integer> l = new ArrayList<>(1);
                l.add(i);
                reverseVertexMap.add(l);
            }
        }
        if(vertices != null) {
            // Unroll vertices data array
            FloatBuffer positionBuffer = BufferUtils.createFloatBuffer(vCount * 3);
            mesh.setBuffer(VertexBuffer.Type.Position, 3, positionBuffer);
            int srcCount = vertices.length / 3;
            for(int i = 0; i < vCount; ++i) {
                int index = vertexMap.get(i);
                if(index > srcCount)
                    throw new AssetLoadException("Invalid vertex mapping. Unexpected lookup vertex " + index + " from " + srcCount);
                float x = (float) vertices[3 * index + 0] / scene.unitSize * scene.xAxis; // XXX Why we should scale by unit size?
                float y = (float) vertices[3 * index + 1] / scene.unitSize * scene.yAxis;
                float z = (float) vertices[3 * index + 2] / scene.unitSize * scene.zAxis;
                positionBuffer.put(x).put(y).put(z);
            }
        }
        if(normals != null) {
            // Unroll normals data array
            FloatBuffer normalBuffer = BufferUtils.createFloatBuffer(vCount * 3);
            mesh.setBuffer(VertexBuffer.Type.Normal, 3, normalBuffer);
            List<Integer> mapping = null;
            if(normalsMapping.equals("ByVertice"))
                mapping = vertexMap;
            else if(normalsMapping.equals("ByPolygonVertex"))
                mapping = indexMap;
            else
                throw new IOException("Unknown normals mapping type: " + normalsMapping);
            int srcCount = normals.length / 3;
            for(int i = 0; i < vCount; ++i) {
                int index = mapping.get(i);
                if(index > srcCount)
                    throw new AssetLoadException("Invalid normal mapping. Unexpected lookup normal " + index + " from " + srcCount);
                float x = (float) normals[3 * index + 0] * scene.xAxis;
                float y = (float) normals[3 * index + 1] * scene.yAxis;
                float z = (float) normals[3 * index + 2] * scene.zAxis;
                normalBuffer.put(x).put(y).put(z);
            }
        }
        if(tangents != null) {
            // Unroll normals data array
            FloatBuffer tangentBuffer = BufferUtils.createFloatBuffer(vCount * 4);
            mesh.setBuffer(VertexBuffer.Type.Tangent, 4, tangentBuffer);
            List<Integer> mapping = null;
            if(tangentsMapping.equals("ByVertice"))
                mapping = vertexMap;
            else if(tangentsMapping.equals("ByPolygonVertex"))
                mapping = indexMap;
            else
                throw new IOException("Unknown tangents mapping type: " + tangentsMapping);
            int srcCount = tangents.length / 3;
            for(int i = 0; i < vCount; ++i) {
                int index = mapping.get(i);
                if(index > srcCount)
                    throw new AssetLoadException("Invalid tangent mapping. Unexpected lookup tangent " + index + " from " + srcCount);
                float x = (float) tangents[3 * index + 0] * scene.xAxis;
                float y = (float) tangents[3 * index + 1] * scene.yAxis;
                float z = (float) tangents[3 * index + 2] * scene.zAxis;
                tangentBuffer.put(x).put(y).put(z).put(-1.0f);
            }
        }
        if(binormals != null) {
            // Unroll normals data array
            FloatBuffer binormalBuffer = BufferUtils.createFloatBuffer(vCount * 3);
            mesh.setBuffer(VertexBuffer.Type.Binormal, 3, binormalBuffer);
            List<Integer> mapping = null;
            if(binormalsMapping.equals("ByVertice"))
                mapping = vertexMap;
            else if(binormalsMapping.equals("ByPolygonVertex"))
                mapping = indexMap;
            else
                throw new IOException("Unknown binormals mapping type: " + binormalsMapping);
            int srcCount = binormals.length / 3;
            for(int i = 0; i < vCount; ++i) {
                int index = mapping.get(i);
                if(index > srcCount)
                    throw new AssetLoadException("Invalid binormal mapping. Unexpected lookup binormal " + index + " from " + srcCount);
                float x = (float) binormals[3 * index + 0] * scene.xAxis;
                float y = (float) binormals[3 * index + 1] * scene.yAxis;
                float z = (float) binormals[3 * index + 2] * scene.zAxis;
                binormalBuffer.put(x).put(y).put(z);
            }
        }
        for(int uvLayer = 0; uvLayer < uvs.size(); ++uvLayer) {
            double[] uv = uvs.get(uvLayer);
            int[] uvIndex = uvIndexes.size() > uvLayer ? uvIndexes.get(uvLayer) : null;
            List<Integer> unIndexMap = vertexMap;
            if(uvIndex != null) {
                int uvIndexSrcCount = uvIndex.length;
                if(uvIndexSrcCount != iCount)
                    throw new AssetLoadException("Invalid number of texcoord index data " + uvIndexSrcCount + " expected " + iCount);
                // Unroll UV index array
                unIndexMap = new ArrayList<>(vCount);
                int polyVertCount = 0;
                for(int i = 0; i < iCount; ++i) {
                    int index = indices[i];
                    polyVertCount++;
                    if(index < 0) {
                        if(polyVertCount == 3) {
                            unIndexMap.add(uvIndex[i - 2]);
                            unIndexMap.add(uvIndex[i - 1]);
                            unIndexMap.add(uvIndex[i - 0]);
                        } else if(polyVertCount == 4) {
                            unIndexMap.add(uvIndex[i - 3]);
                            unIndexMap.add(uvIndex[i - 2]);
                            unIndexMap.add(uvIndex[i - 1]);
                            unIndexMap.add(uvIndex[i - 3]);
                            unIndexMap.add(uvIndex[i - 1]);
                            unIndexMap.add(uvIndex[i - 0]);
                        }
                        polyVertCount = 0;
                    }
                }
            }
            // Unroll UV data array
            FloatBuffer uvBuffer = BufferUtils.createFloatBuffer(vCount * 2);
            VertexBuffer.Type type = VertexBuffer.Type.TexCoord;
            switch(uvLayer) {
            case 1:
                type = VertexBuffer.Type.TexCoord2;
                break;
            case 2:
                type = VertexBuffer.Type.TexCoord3;
                break;
            case 3:
                type = VertexBuffer.Type.TexCoord4;
                break;
            case 4:
                type = VertexBuffer.Type.TexCoord5;
                break;
            case 5:
                type = VertexBuffer.Type.TexCoord6;
                break;
            case 6:
                type = VertexBuffer.Type.TexCoord7;
                break;
            case 7:
                type = VertexBuffer.Type.TexCoord8;
                break;
            }
            mesh.setBuffer(type, 2, uvBuffer);
            int srcCount = uv.length / 2;
            for(int i = 0; i < vCount; ++i) {
                int index = unIndexMap.get(i);
                if(index > srcCount)
                    throw new AssetLoadException("Invalid texcoord mapping. Unexpected lookup texcoord " + index + " from " + srcCount);
                float u = (index >= 0) ? (float) uv[2 * index + 0] : 0;
                float v = (index >= 0) ? (float) uv[2 * index + 1] : 0;
                uvBuffer.put(u).put(v);
            }
        }
        List<Geometry> geometries = new ArrayList<>();
        if(materialsReference.equals("IndexToDirect") && materialsMapping.equals("ByPolygon")) {
            IntMap<List<Integer>> indexBuffers = new IntMap<>();
            for(int polygon = 0; polygon < materials.length; ++polygon) {
                int material = materials[polygon];
                List<Integer> list = indexBuffers.get(material);
                if(list == null) {
                    list = new ArrayList<>();
                    indexBuffers.put(material, list);
                }
                list.add(polygon * 3 + 0);
                list.add(polygon * 3 + 1);
                list.add(polygon * 3 + 2);
            }
            Iterator<Entry<List<Integer>>> iterator = indexBuffers.iterator();
            while(iterator.hasNext()) {
                Entry<List<Integer>> e = iterator.next();
                int materialId = e.getKey();
                List<Integer> indexes = e.getValue();
                Mesh newMesh = mesh.clone();
                newMesh.setBuffer(VertexBuffer.Type.Index, 3, toArray(indexes.toArray(new Integer[indexes.size()])));
                newMesh.setStatic();
                newMesh.updateBound();
                newMesh.updateCounts();
                Geometry geom = new Geometry();
                geom.setMesh(newMesh);
                geometries.add(geom);
                geom.setUserData("FBXMaterial", materialId);
            }
        } else {
            mesh.setStatic();
            mesh.updateBound();
            mesh.updateCounts();
            Geometry geom = new Geometry();
            geom.setMesh(mesh);
            geometries.add(geom);
        }
        return geometries;
    }

    private static int[] toArray(Integer[] arr) {
        int[] ret = new int[arr.length];
        for(int i = 0; i < arr.length; ++i)
            ret[i] = arr[i].intValue();
        return ret;
    }
}

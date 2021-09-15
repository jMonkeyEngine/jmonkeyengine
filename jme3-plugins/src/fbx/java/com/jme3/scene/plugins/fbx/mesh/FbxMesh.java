/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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

import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.asset.AssetManager;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.plugins.IrUtils;
import com.jme3.scene.plugins.IrBoneWeightIndex;
import com.jme3.scene.plugins.IrMesh;
import com.jme3.scene.plugins.IrPolygon;
import com.jme3.scene.plugins.IrVertex;
import com.jme3.scene.plugins.fbx.anim.FbxCluster;
import com.jme3.scene.plugins.fbx.anim.FbxLimbNode;
import com.jme3.scene.plugins.fbx.anim.FbxSkinDeformer;
import com.jme3.scene.plugins.fbx.file.FbxElement;
import com.jme3.scene.plugins.fbx.obj.FbxObject;
import com.jme3.scene.plugins.fbx.node.FbxNodeAttribute;
import com.jme3.util.IntMap;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class FbxMesh extends FbxNodeAttribute<IntMap<Mesh>> {

    private static final Logger logger = Logger.getLogger(FbxMesh.class.getName());
    
    private FbxPolygon[] polygons;
    private FbxLayerElement[] layerElements;
    private Vector3f[] positions;
    private FbxLayer[] layers;

    private ArrayList<Integer>[] boneIndices;
    private ArrayList<Float>[] boneWeights;
    
    private FbxSkinDeformer skinDeformer;
    
    public FbxMesh(AssetManager assetManager, String sceneFolderName) {
        super(assetManager, sceneFolderName);
    }
    
    @Override
    public void fromElement(FbxElement element) {
        super.fromElement(element);
        
        List<FbxLayerElement> layerElementsList = new ArrayList<>();
        List<FbxLayer> layersList = new ArrayList<>();
        
        for (FbxElement e : element.children) {
            if (e.id.equals("Vertices")) {
                setPositions(FbxMeshUtil.getDoubleArray(e));
            } else if (e.id.equals("PolygonVertexIndex")) {
                setPolygonVertexIndices(FbxMeshUtil.getIntArray(e));
            } else if (e.id.equals("Edges")) {
                setEdges(FbxMeshUtil.getIntArray(e));
            } else if (e.id.startsWith("LayerElement")) {
                layerElementsList.add(FbxLayerElement.fromElement(e));
            } else if (e.id.equals("Layer")) {
                layersList.add(FbxLayer.fromElement(e));
            }
        }
        
        for (FbxLayer layer : layersList) {
            layer.setLayerElements(layerElementsList);
        }
        
        layerElements = new FbxLayerElement[layerElementsList.size()];
        layerElementsList.toArray(layerElements);
        
        layers = new FbxLayer[layersList.size()];
        layersList.toArray(layers);
    }

    public FbxSkinDeformer getSkinDeformer() {
        return skinDeformer;
    }
    
    @SuppressWarnings("unchecked")
    public void applyCluster(FbxCluster cluster) {
        if (boneIndices == null) {
            boneIndices = new ArrayList[positions.length];
            boneWeights = new ArrayList[positions.length];
        }
        
        FbxLimbNode limb = cluster.getLimb();
        Bone bone = limb.getJmeBone();
        Skeleton skeleton = limb.getSkeletonHolder().getJmeSkeleton();
        int boneIndex = skeleton.getBoneIndex(bone);
        
        int[] positionIndices = cluster.getVertexIndices();
        double[] weights = cluster.getWeights();
        
        for (int i = 0; i < positionIndices.length; i++) {
            int positionIndex = positionIndices[i];
            float boneWeight = (float)weights[i];
            
            ArrayList<Integer> boneIndicesForVertex = boneIndices[positionIndex];
            ArrayList<Float>  boneWeightsForVertex = boneWeights[positionIndex];
            
            if (boneIndicesForVertex == null) {
                boneIndicesForVertex = new ArrayList<Integer>();
                boneWeightsForVertex = new ArrayList<Float>();
                boneIndices[positionIndex] = boneIndicesForVertex;
                boneWeights[positionIndex] = boneWeightsForVertex;
            }
            
            boneIndicesForVertex.add(boneIndex);
            boneWeightsForVertex.add(boneWeight);
        }
    }
    
    @Override
    public void connectObject(FbxObject object) {
        if (object instanceof FbxSkinDeformer) {
            if (skinDeformer != null) {
                logger.log(Level.WARNING, "This mesh already has a skin deformer attached. Ignoring.");
                return;
            }
            skinDeformer = (FbxSkinDeformer) object;
        } else {
            unsupportedConnectObject(object);
        }
    }

    @Override
    public void connectObjectProperty(FbxObject object, String property) {
        unsupportedConnectObjectProperty(object, property);
    }
    
    private void setPositions(double[] positions) {
        this.positions = FbxLayerElement.toVector3(positions);
    }
    
    private void setEdges(int[] edges) {
        // TODO: ...
    }
    
    private void setPolygonVertexIndices(int[] polygonVertexIndices) {
        List<FbxPolygon> polygonList = new ArrayList<>();

        boolean finishPolygon = false;
        List<Integer> vertexIndices = new ArrayList<>();

        for (int i = 0; i < polygonVertexIndices.length; i++) {
            int vertexIndex = polygonVertexIndices[i];
            
            if (vertexIndex < 0) {
                vertexIndex ^= -1;
                finishPolygon = true;
            }
            
            vertexIndices.add(vertexIndex);

            if (finishPolygon) {
                finishPolygon = false;
                polygonList.add(FbxPolygon.fromIndices(vertexIndices));
                vertexIndices.clear();
            }
        }
        
        polygons = new FbxPolygon[polygonList.size()];
        polygonList.toArray(polygons);
    }
    
    private static IrBoneWeightIndex[] toBoneWeightIndices(List<Integer> boneIndices, List<Float> boneWeights) {
        IrBoneWeightIndex[] boneWeightIndices = new IrBoneWeightIndex[boneIndices.size()];
        for (int i = 0; i < boneIndices.size(); i++) {
            boneWeightIndices[i] = new IrBoneWeightIndex(boneIndices.get(i), boneWeights.get(i));
        }
        return boneWeightIndices;
    }
    
    @Override
    protected IntMap<Mesh> toJmeObject() {
        // Load clusters from SkinDeformer
        if (skinDeformer != null) {
            for (FbxCluster cluster : skinDeformer.getJmeObject()) {
                applyCluster(cluster);
            }
        }
        
        IrMesh irMesh = toIRMesh();
        
        // Trim bone weights to 4 weights per vertex.
        IrUtils.trimBoneWeights(irMesh);
        
        // Convert tangents / binormals to tangents with parity.
        IrUtils.toTangentsWithParity(irMesh);
        
        // Triangulate quads.
        IrUtils.triangulate(irMesh);
        
        // Split meshes by material indices.
        IntMap<IrMesh> irMeshes = IrUtils.splitByMaterial(irMesh);
        
        // Create a jME3 Mesh for each material index.
        IntMap<Mesh> jmeMeshes = new IntMap<>();
        for (IntMap.Entry<IrMesh> irMeshEntry : irMeshes) {
            Mesh jmeMesh = IrUtils.convertIrMeshToJmeMesh(irMeshEntry.getValue());
            jmeMeshes.put(irMeshEntry.getKey(), jmeMesh);
        }
       
        if (jmeMeshes.size() == 0) {
            // When will this actually happen? Not sure.
            logger.log(Level.WARNING, "Empty FBX mesh found (unusual).");
        }
        
        // IMPORTANT: If we have a -1 entry, those are triangles
        // with no material indices. 
        // It makes sense only if the mesh uses a single material!
        if (jmeMeshes.containsKey(-1) && jmeMeshes.size() > 1) {
            logger.log(Level.WARNING, "Mesh has polygons with no material "
                                    + "indices (unusual) - they will use material index 0.");
        }
        
        return jmeMeshes;
    }
    
    /**
     * Convert FBXMesh to IRMesh.
     *
     * @return a new IrMesh
     */
    public IrMesh toIRMesh() {
        IrMesh newMesh = new IrMesh();
        newMesh.polygons = new IrPolygon[polygons.length];
        
        int polygonVertexIndex = 0;
        int positionIndex = 0;
        
        FbxLayer layer0 = layers[0];
        FbxLayer layer1 = layers.length > 1 ? layers[1] : null;
        
        for (int i = 0; i < polygons.length; i++) {
            FbxPolygon polygon = polygons[i];
            IrPolygon irPolygon = new IrPolygon();
            irPolygon.vertices = new IrVertex[polygon.indices.length];
            
            for (int j = 0; j < polygon.indices.length; j++) {
                positionIndex = polygon.indices[j];
                
                IrVertex irVertex = new IrVertex();
                irVertex.pos = positions[positionIndex];
                
                if (layer0 != null) {
                    irVertex.norm      = (Vector3f)  layer0.getVertexData(FbxLayerElement.Type.Normal,   i, polygonVertexIndex, positionIndex, 0);
                    irVertex.tang      = (Vector3f)  layer0.getVertexData(FbxLayerElement.Type.Tangent,  i, polygonVertexIndex, positionIndex, 0);
                    irVertex.bitang    = (Vector3f)  layer0.getVertexData(FbxLayerElement.Type.Binormal,  i, polygonVertexIndex, positionIndex, 0);
                    irVertex.uv0       = (Vector2f)  layer0.getVertexData(FbxLayerElement.Type.UV,       i, polygonVertexIndex, positionIndex, 0);
                    irVertex.color     = (ColorRGBA) layer0.getVertexData(FbxLayerElement.Type.Color,    i, polygonVertexIndex, positionIndex, 0);
                    irVertex.material  = (Integer)   layer0.getVertexData(FbxLayerElement.Type.Material,  i, polygonVertexIndex, positionIndex, 0);
                    irVertex.smoothing = (Integer)   layer0.getVertexData(FbxLayerElement.Type.Smoothing, i, polygonVertexIndex, positionIndex, 0);
                }
                
                if (layer1 != null) {
                    irVertex.uv1 = (Vector2f) layer1.getVertexData(FbxLayerElement.Type.UV, i, 
                                                                   polygonVertexIndex, positionIndex, 0);
                }
                
                if (boneIndices != null) {
                    ArrayList<Integer> boneIndicesForVertex = boneIndices[positionIndex];
                    ArrayList<Float>   boneWeightsForVertex = boneWeights[positionIndex];
                    if (boneIndicesForVertex != null) {
                        irVertex.boneWeightsIndices = toBoneWeightIndices(boneIndicesForVertex, boneWeightsForVertex);
                    }
                }
                
                irPolygon.vertices[j] = irVertex;

                polygonVertexIndex++;
            }
            
            newMesh.polygons[i] = irPolygon;
        }
        
        // Ensure "inspection vertex" specifies that mesh has bone indices / weights
        if (boneIndices != null && newMesh.polygons[0].vertices[0] == null) {
            newMesh.polygons[0].vertices[0].boneWeightsIndices = new IrBoneWeightIndex[0];
        }
        
        return newMesh;
    }
}

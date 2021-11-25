package com.jme3.scene.plugins.fbx.objects;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import com.jme3.asset.AssetLoadException;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.util.BufferUtils;
import com.jme3.scene.plugins.fbx.SceneLoader;
import com.jme3.scene.plugins.fbx.file.FbxElement;

public class FbxSkin extends FbxObject {

    public String skinningType;
    public List<FbxMesh> toSkin = new ArrayList<>();
    public List<FbxNode> bones = new ArrayList<>();

    public FbxSkin(SceneLoader scene, FbxElement element) {
        super(scene, element);
        for(FbxElement e : element.children) {
            switch(e.id) {
            case "SkinningType":
                skinningType = (String) e.properties.get(0);
                break;
            }
        }
    }
    
    @Override
    public void link(FbxObject otherObject) {
        if(otherObject instanceof FbxCluster) {
            FbxCluster cluster = ((FbxCluster) otherObject);
            cluster.skin = this;
        }
    }
    
    public void generateSkinning() {
        for(FbxMesh fbxMesh : toSkin) {
            if(fbxMesh.geometries == null)
                continue;
            Mesh firstMesh = fbxMesh.geometries.get(0).getMesh();
            int maxWeightsPerVert = generateBoneData(firstMesh, fbxMesh);
            for(int i = 0; i < fbxMesh.geometries.size(); ++i) {
                Mesh mesh = fbxMesh.geometries.get(i).getMesh();
                if(mesh != firstMesh) {
                    mesh.setBuffer(firstMesh.getBuffer(VertexBuffer.Type.BoneWeight));
                    mesh.setBuffer(firstMesh.getBuffer(VertexBuffer.Type.BoneIndex));
                    mesh.setBuffer(firstMesh.getBuffer(VertexBuffer.Type.HWBoneWeight));
                    mesh.setBuffer(firstMesh.getBuffer(VertexBuffer.Type.HWBoneIndex));
                }
                mesh.setMaxNumWeights(maxWeightsPerVert);
                mesh.generateBindPose(true);
            }
        }
    }
    
    private int generateBoneData(Mesh mesh, FbxMesh fbxMesh) {
        // Create bone buffers
        FloatBuffer boneWeightData = BufferUtils.createFloatBuffer(fbxMesh.vCount * 4);
        ByteBuffer boneIndicesData = BufferUtils.createByteBuffer(fbxMesh.vCount * 4);
        mesh.setBuffer(VertexBuffer.Type.BoneWeight, 4, boneWeightData);
        mesh.setBuffer(VertexBuffer.Type.BoneIndex, 4, boneIndicesData);
        mesh.getBuffer(VertexBuffer.Type.BoneWeight).setUsage(Usage.CpuOnly);
        mesh.getBuffer(VertexBuffer.Type.BoneIndex).setUsage(Usage.CpuOnly);
        VertexBuffer weightsHW = new VertexBuffer(Type.HWBoneWeight);
        VertexBuffer indicesHW = new VertexBuffer(Type.HWBoneIndex);
        indicesHW.setUsage(Usage.CpuOnly); // Setting usage to CpuOnly so that the buffer is not send empty to the GPU
        weightsHW.setUsage(Usage.CpuOnly);
        mesh.setBuffer(weightsHW);
        mesh.setBuffer(indicesHW);
        int bonesLimitExceeded = 0;
        // Accumulate skin bones influence into mesh buffers
        for(FbxNode limb : bones) {
            FbxCluster cluster = limb.skinToCluster.get(id);
            if(cluster == null || cluster.indexes == null || cluster.weights == null || cluster.indexes.length != cluster.weights.length)
                continue;
            if(limb.boneIndex > 255)
                throw new AssetLoadException("Bone index can't be packed into byte");
            for(int i = 0; i < cluster.indexes.length; ++i) {
                int vertexIndex = cluster.indexes[i];
                if(vertexIndex >= fbxMesh.reverseVertexMap.size())
                    throw new AssetLoadException("Invalid skinning vertex index. Unexpected index lookup " + vertexIndex + " from " + fbxMesh.reverseVertexMap.size());
                List<Integer> dstVertices = fbxMesh.reverseVertexMap.get(vertexIndex);
                for(int j = 0; j < dstVertices.size(); ++j) {
                    int v = dstVertices.get(j);
                    // Append bone index and weight to vertex
                    int offset;
                    int smallestOffset = 0;
                    float w = 0;
                    float smallestW = Float.MAX_VALUE;
                    for(offset = v * 4; offset < v * 4 + 4; ++offset) {
                        w = boneWeightData.get(offset);
                        if(w == 0)
                            break;
                        if(w < smallestW) {
                            smallestW = w;
                            smallestOffset = offset;
                        }
                    }
                    if(w == 0) {
                        boneWeightData.put(offset, (float) cluster.weights[i]);
                        boneIndicesData.put(offset, (byte) limb.boneIndex);
                    } else {
                        if((float) cluster.weights[i] > smallestW) { // If current weight more than smallest, discard smallest
                            boneWeightData.put(smallestOffset, (float) cluster.weights[i]);
                            boneIndicesData.put(smallestOffset, (byte) limb.boneIndex);
                        }
                        bonesLimitExceeded++;
                    }
                }
            }
        }
        if(bonesLimitExceeded > 0)
            scene.warning("Skinning support max 4 bone per vertex. Exceeding data of " + bonesLimitExceeded + " weights in mesh bones will be discarded");
        // Postprocess bones weights
        int maxWeightsPerVert = 0;
        boneWeightData.rewind();
        for(int v = 0; v < fbxMesh.vCount; v++) {
            float w0 = boneWeightData.get();
            float w1 = boneWeightData.get();
            float w2 = boneWeightData.get();
            float w3 = boneWeightData.get();
            if(w3 != 0) {
                maxWeightsPerVert = Math.max(maxWeightsPerVert, 4);
            } else if(w2 != 0) {
                maxWeightsPerVert = Math.max(maxWeightsPerVert, 3);
            } else if(w1 != 0) {
                maxWeightsPerVert = Math.max(maxWeightsPerVert, 2);
            } else if(w0 != 0) {
                maxWeightsPerVert = Math.max(maxWeightsPerVert, 1);
            }
            float sum = w0 + w1 + w2 + w3;
            if(sum != 1f) {
                // normalize weights
                float mult = (sum != 0) ? (1f / sum) : 0;
                boneWeightData.position(v * 4);
                boneWeightData.put(w0 * mult);
                boneWeightData.put(w1 * mult);
                boneWeightData.put(w2 * mult);
                boneWeightData.put(w3 * mult);
            }
        }
        return maxWeightsPerVert;
    }
}

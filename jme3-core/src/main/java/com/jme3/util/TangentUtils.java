package com.jme3.util;

import com.jme3.scene.*;

/**
 * Created by Nehon on 03/10/2016.
 */
public class TangentUtils {

    public static void generateBindPoseTangentsIfNecessary(Mesh mesh){
        if (mesh.getBuffer(VertexBuffer.Type.BindPosePosition) != null) {

            VertexBuffer tangents = mesh.getBuffer(VertexBuffer.Type.Tangent);
            if (tangents != null) {
                VertexBuffer bindTangents = new VertexBuffer(VertexBuffer.Type.BindPoseTangent);
                bindTangents.setupData(VertexBuffer.Usage.CpuOnly,
                        4,
                        VertexBuffer.Format.Float,
                        BufferUtils.clone(tangents.getData()));

                if (mesh.getBuffer(VertexBuffer.Type.BindPoseTangent) != null) {
                    mesh.clearBuffer(VertexBuffer.Type.BindPoseTangent);
                }
                mesh.setBuffer(bindTangents);
                tangents.setUsage(VertexBuffer.Usage.Stream);
            }
        }
    }
}

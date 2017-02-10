/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jmevr.util;

import com.jme3.app.VRApplication;
import com.jme3.input.vr.VRAPI;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.system.jopenvr.DistortionCoordinates_t;
import com.jme3.system.jopenvr.JOpenVRLibrary;
import com.jme3.system.jopenvr.VR_IVRSystem_FnTable;

/**
 *
 * @author reden
 */
public class MeshUtil {

    public static Mesh setupDistortionMesh(int eye, VRAPI api) {
        Mesh distortionMesh = new Mesh();
        float m_iLensGridSegmentCountH = 43, m_iLensGridSegmentCountV = 43;
        
        float w = 1f / (m_iLensGridSegmentCountH - 1f);
        float h = 1f / (m_iLensGridSegmentCountV - 1f);

        float u, v;

        float verts[] = new float[(int) (m_iLensGridSegmentCountV * m_iLensGridSegmentCountH) * 3];

        float texcoordR[] = new float[(int) (m_iLensGridSegmentCountV * m_iLensGridSegmentCountH) * 2];
        float texcoordG[] = new float[(int) (m_iLensGridSegmentCountV * m_iLensGridSegmentCountH) * 2];
        float texcoordB[] = new float[(int) (m_iLensGridSegmentCountV * m_iLensGridSegmentCountH) * 2];

        int vertPos = 0, coordPos = 0;
        
        float Xoffset = eye == JOpenVRLibrary.EVREye.EVREye_Eye_Left ? -1f : 0;
        for (int y = 0; y < m_iLensGridSegmentCountV; y++) {
            for (int x = 0; x < m_iLensGridSegmentCountH; x++) {
                u = x * w;
                v = 1 - y * h;
                verts[vertPos] = Xoffset + u; // x
                verts[vertPos + 1] = -1 + 2 * y * h; // y
                verts[vertPos + 2] = 0f; // z
                vertPos += 3;

                DistortionCoordinates_t dc0 = new DistortionCoordinates_t();
                if( api.getVRSystem() == null ) {
                    // default to no distortion
                    texcoordR[coordPos] = u;
                    texcoordR[coordPos + 1] = 1 - v;
                    texcoordG[coordPos] = u;
                    texcoordG[coordPos + 1] = 1 - v;
                    texcoordB[coordPos] = u;
                    texcoordB[coordPos + 1] = 1 - v;                    
                } else {
                    ((VR_IVRSystem_FnTable)api.getVRSystem()).ComputeDistortion.apply(eye, u, v, dc0);
                    
                    texcoordR[coordPos] = dc0.rfRed[0];
                    texcoordR[coordPos + 1] = 1 - dc0.rfRed[1];
                    texcoordG[coordPos] = dc0.rfGreen[0];
                    texcoordG[coordPos + 1] = 1 - dc0.rfGreen[1];
                    texcoordB[coordPos] = dc0.rfBlue[0];
                    texcoordB[coordPos + 1] = 1 - dc0.rfBlue[1];
                }                
                
                coordPos += 2;
            }
        }

        // have UV coordinates & positions, now to setup indices

        int[] indices = new int[(int) ((m_iLensGridSegmentCountV - 1) * (m_iLensGridSegmentCountH - 1)) * 6];
        int indexPos = 0;
        int a, b, c, d;

        int offset = 0;
        for (int y = 0; y < m_iLensGridSegmentCountV - 1; y++) {
            for (int x = 0; x < m_iLensGridSegmentCountH - 1; x++) {
                a = (int) (m_iLensGridSegmentCountH * y + x + offset);
                b = (int) (m_iLensGridSegmentCountH * y + x + 1 + offset);
                c = (int) ((y + 1) * m_iLensGridSegmentCountH + x + 1 + offset);
                d = (int) ((y + 1) * m_iLensGridSegmentCountH + x + offset);
                
                indices[indexPos] = a;
                indices[indexPos + 1] = b;
                indices[indexPos + 2] = c;

                indices[indexPos + 3] = a;
                indices[indexPos + 4] = c;
                indices[indexPos + 5] = d;

                indexPos += 6;
            }
        }
        
        // OK, create the mesh        
        distortionMesh.setBuffer(VertexBuffer.Type.Position, 3, verts);
        distortionMesh.setBuffer(VertexBuffer.Type.Index, 1, indices);
        distortionMesh.setBuffer(VertexBuffer.Type.TexCoord, 2, texcoordR);
        distortionMesh.setBuffer(VertexBuffer.Type.TexCoord2, 2, texcoordG);
        distortionMesh.setBuffer(VertexBuffer.Type.TexCoord3, 2, texcoordB);
        distortionMesh.setStatic();
        return distortionMesh;
    }
}

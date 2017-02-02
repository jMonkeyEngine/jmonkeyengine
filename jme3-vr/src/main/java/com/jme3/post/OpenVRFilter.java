/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.post;

import com.jme3.app.VRApplication;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.post.Filter;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.system.jopenvr.DistortionCoordinates_t;
import com.jme3.system.jopenvr.JOpenVRLibrary;
import com.jme3.system.jopenvr.VR_IVRSystem_FnTable;
import com.jme3.texture.FrameBuffer;

/**
 * DO NOT USE
 * @author phr00t
 * @deprecated DO NOT USE
 */
@Deprecated
public class OpenVRFilter extends Filter {

    private Mesh distortionMesh;
    
    private VRApplication application = null;
    
    /**
     * DO NOT USE
     * @param application the VR application.
     */
    public OpenVRFilter(VRApplication application) {
        this.application = application;
    }
    
    /**
     * DO NOT USE
     * @return the distortion mesh.
     */
    public Mesh getDistortionMesh() {
        return distortionMesh;
    }

    @Override
    protected void initFilter(AssetManager manager, RenderManager renderManager, ViewPort vp, int w, int h) {
        material = new Material(manager, "Common/MatDefs/VR/OpenVR.j3md");
        configureDistortionMesh();
    }

    @Override
    protected Material getMaterial() {
        return material;

    }
    
    @Override
    protected void preFrame(float tpf) {
        super.preFrame(tpf);
    }

    @Override
    protected void postFrame(RenderManager renderManager, ViewPort viewPort, FrameBuffer prevFilterBuffer, FrameBuffer sceneBuffer) {
        super.postFrame(renderManager, viewPort, prevFilterBuffer, sceneBuffer);
    }

    @Override
    protected void postFilter(Renderer r, FrameBuffer buffer) {
        super.postFilter(r, buffer);
    }
    
    /*
        function converted from:
        https://github.com/ValveSoftware/openvr/blob/master/samples/hellovr_opengl/hellovr_opengl_main.cpp#L1335
    */
    private void configureDistortionMesh() {        
        float m_iLensGridSegmentCountH = 43, m_iLensGridSegmentCountV = 43;

	float w = 1f / m_iLensGridSegmentCountH - 1f;
	float h = 1f / m_iLensGridSegmentCountV - 1f;

	float u, v;

        distortionMesh = new Mesh();
        float verts[] = new float[(int)(m_iLensGridSegmentCountV * m_iLensGridSegmentCountH) * 3];
        
        float texcoordR[] = new float[(int)(m_iLensGridSegmentCountV * m_iLensGridSegmentCountH) * 2];
        float texcoordG[] = new float[(int)(m_iLensGridSegmentCountV * m_iLensGridSegmentCountH) * 2];
        float texcoordB[] = new float[(int)(m_iLensGridSegmentCountV * m_iLensGridSegmentCountH) * 2];        
        
        int vertPos = 0, coordPos = 0;

	//left eye distortion verts
	float Xoffset = -1f;
	for( int y=0; y<m_iLensGridSegmentCountV; y++ )
	{
		for( int x=0; x<m_iLensGridSegmentCountH; x++ )
		{
			u = x*w; v = 1-y*h;
			verts[vertPos] = Xoffset+u; // x
                        verts[vertPos+1] = -1+2*y*h; // y
                        verts[vertPos+2] = 0f; // z
                        vertPos += 3;

			DistortionCoordinates_t dc0 = new DistortionCoordinates_t();
		    ((VR_IVRSystem_FnTable)application.getVRHardware().getVRSystem()).ComputeDistortion.apply(JOpenVRLibrary.EVREye.EVREye_Eye_Left, u, v, dc0);

			texcoordR[coordPos]   = dc0.rfRed[0];
                        texcoordR[coordPos+1] = 1 - dc0.rfRed[1];
			texcoordG[coordPos]   = dc0.rfGreen[0];
                        texcoordG[coordPos+1] = 1 - dc0.rfGreen[1];
			texcoordB[coordPos]   = dc0.rfBlue[0];
                        texcoordB[coordPos+1] = 1 - dc0.rfBlue[1];                        
                        coordPos+=2;                        
		}
	}

	//right eye distortion verts
	Xoffset = 0;
	for( int y=0; y<m_iLensGridSegmentCountV; y++ )
	{
		for( int x=0; x<m_iLensGridSegmentCountH; x++ )
		{
			u = x*w; v = 1-y*h;
			verts[vertPos] = Xoffset+u; // x
                        verts[vertPos+1] = -1+2*y*h; // y
                        verts[vertPos+2] = 0f; // z
                        vertPos += 3;

            DistortionCoordinates_t dc0 = new DistortionCoordinates_t();
			((VR_IVRSystem_FnTable)application.getVRHardware().getVRSystem()).ComputeDistortion.apply(JOpenVRLibrary.EVREye.EVREye_Eye_Right, u, v, dc0);

			texcoordR[coordPos]   = dc0.rfRed[0];
                        texcoordR[coordPos+1] = 1 - dc0.rfRed[1];
			texcoordG[coordPos]   = dc0.rfGreen[0];
                        texcoordG[coordPos+1] = 1 - dc0.rfGreen[1];
			texcoordB[coordPos]   = dc0.rfBlue[0];
                        texcoordB[coordPos+1] = 1 - dc0.rfBlue[1];                        
                        coordPos+=2;                        
		}
	}
        
        // have UV coordinates & positions, now to setup indices
        
	//std::vector<GLushort> vIndices;
        int[] indices = new int[(int)((m_iLensGridSegmentCountV - 1) * (m_iLensGridSegmentCountH - 1)) * 6];
        int indexPos = 0;
	int a,b,c,d;

	int offset = 0;
	for( int y=0; y<m_iLensGridSegmentCountV-1; y++ )
	{
		for( int x=0; x<m_iLensGridSegmentCountH-1; x++ )
		{
			a = (int)(m_iLensGridSegmentCountH*y+x +offset);
			b = (int)(m_iLensGridSegmentCountH*y+x+1 +offset);
			c = (int)((y+1)*m_iLensGridSegmentCountH+x+1 +offset);
			d = (int)((y+1)*m_iLensGridSegmentCountH+x +offset);
                        
                        indices[indexPos] = a;
                        indices[indexPos+1] = b;
                        indices[indexPos+2] = c;
                        
                        indices[indexPos+3] = a;
                        indices[indexPos+4] = c;
                        indices[indexPos+5] = d;
                        
                        indexPos += 6;
		}
	}

	offset = (int)(m_iLensGridSegmentCountH * m_iLensGridSegmentCountV);
	for( int y=0; y<m_iLensGridSegmentCountV-1; y++ )
	{
		for( int x=0; x<m_iLensGridSegmentCountH-1; x++ )
		{
			a = (int)(m_iLensGridSegmentCountH*y+x +offset);
			b = (int)(m_iLensGridSegmentCountH*y+x+1 +offset);
			c = (int)((y+1)*m_iLensGridSegmentCountH+x+1 +offset);
			d = (int)((y+1)*m_iLensGridSegmentCountH+x +offset);
                        
                        indices[indexPos] = a;
                        indices[indexPos+1] = b;
                        indices[indexPos+2] = c;
                        
                        indices[indexPos+3] = a;
                        indices[indexPos+4] = c;
                        indices[indexPos+5] = d;
                        
                        indexPos += 6;
		}
	}        
        
        // OK, create the mesh        
        distortionMesh.setBuffer(VertexBuffer.Type.Position,  3, verts);
        distortionMesh.setBuffer(VertexBuffer.Type.Index,     1, indices);
        distortionMesh.setBuffer(VertexBuffer.Type.TexCoord,  2, texcoordR);
        
        // TODO: are TexCoord2 & TexCoord3 even implemented in jME3?
        distortionMesh.setBuffer(VertexBuffer.Type.TexCoord2, 2, texcoordG);
        distortionMesh.setBuffer(VertexBuffer.Type.TexCoord3, 2, texcoordB);
        
        // TODO: make sure this distortion mesh is used instead of the fullscreen quad
        // when filter gets rendered.. might require changes to jME3 core..?
    }    
}

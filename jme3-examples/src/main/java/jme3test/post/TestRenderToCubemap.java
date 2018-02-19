/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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

package jme3test.post;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.TextureCubeMap;
import com.jme3.util.SkyFactory;
import com.jme3.util.SkyFactory.EnvMapType;

/**
 * Renders a rotating box to a cubemap texture, then applies the cubemap
 * texture as a sky.
 */
public class TestRenderToCubemap  extends SimpleApplication {
 
    private Geometry offBox;
    private float angle = 0;
    private ViewPort offView;
 
    public static void main(String[] args){
        TestRenderToCubemap app = new TestRenderToCubemap();
        app.start();
    }
 
    public Texture setupOffscreenView(){
        Camera offCamera = new Camera(512, 512);
 
        offView = renderManager.createPreView("Offscreen View", offCamera);
        offView.setClearFlags(true, true, true);
        offView.setBackgroundColor(ColorRGBA.DarkGray);
 
        // create offscreen framebuffer
        FrameBuffer offBuffer = new FrameBuffer(512, 512, 1);
 
        //setup framebuffer's cam
        offCamera.setFrustumPerspective(45f, 1f, 1f, 1000f);
        offCamera.setLocation(new Vector3f(0f, 0f, -5f));
        offCamera.lookAt(new Vector3f(0f, 0f, 0f), Vector3f.UNIT_Y);
 
        //setup framebuffer's texture
        TextureCubeMap offTex = new TextureCubeMap(512, 512, Format.RGBA8);
        offTex.setMinFilter(Texture.MinFilter.Trilinear);
        offTex.setMagFilter(Texture.MagFilter.Bilinear);
 
        //setup framebuffer to use texture
        offBuffer.setDepthBuffer(Format.Depth);
        offBuffer.setMultiTarget(true);
        offBuffer.addColorTexture(offTex, TextureCubeMap.Face.NegativeX);
        offBuffer.addColorTexture(offTex, TextureCubeMap.Face.PositiveX);
        offBuffer.addColorTexture(offTex, TextureCubeMap.Face.NegativeY);
        offBuffer.addColorTexture(offTex, TextureCubeMap.Face.PositiveY);
        offBuffer.addColorTexture(offTex, TextureCubeMap.Face.NegativeZ);
        offBuffer.addColorTexture(offTex, TextureCubeMap.Face.PositiveZ);
        
        //set viewport to render to offscreen framebuffer
        offView.setOutputFrameBuffer(offBuffer);
 
        // setup framebuffer's scene
        Box boxMesh = new Box( 1,1,1);
        Material material = assetManager.loadMaterial("Interface/Logo/Logo.j3m");
        offBox = new Geometry("box", boxMesh);
        offBox.setMaterial(material);
 
        // attach the scene to the viewport to be rendered
        offView.attachScene(offBox);
 
        return offTex;
    }
 
    @Override
    public void simpleInitApp() {
        cam.setLocation(new Vector3f(3, 3, 3));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
 
        Texture offTex = setupOffscreenView();
        Spatial sky = SkyFactory.createSky(assetManager, offTex, 
                EnvMapType.CubeMap);
        rootNode.attachChild(sky);
    }
 
    @Override
    public void simpleUpdate(float tpf){
        Quaternion q = new Quaternion();
 
        angle += tpf;
        angle %= FastMath.TWO_PI;
        q.fromAngles(angle, 0, angle);

        offBox.setLocalRotation(q);
        offBox.updateLogicalState(tpf);
        offBox.updateGeometricState();
    }
}
/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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
import com.jme3.math.Vector3f;
import com.jme3.post.HDRRenderer;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.ui.Picture;

public class TestHDR extends SimpleApplication {

    private HDRRenderer hdrRender;
    private Picture dispQuad;

    public static void main(String[] args){
        TestHDR app = new TestHDR();
        app.start();
    }

    public Geometry createHDRBox(){
        Box boxMesh = new Box(Vector3f.ZERO, 1, 1, 1);
        Geometry box = new Geometry("Box", boxMesh);

//        Material mat = assetManager.loadMaterial("Textures/HdrTest/Memorial.j3m");
//        box.setMaterial(mat);

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setTexture("ColorMap", assetManager.loadTexture("Textures/HdrTest/Memorial.hdr"));
        box.setMaterial(mat);

        return box;
    }

//    private Material disp;
    
    @Override
    public void simpleInitApp() {
        hdrRender = new HDRRenderer(assetManager, renderer);
        hdrRender.setSamples(0);
        hdrRender.setMaxIterations(20);
        hdrRender.setExposure(0.87f);
        hdrRender.setThrottle(0.33f);

        viewPort.addProcessor(hdrRender);
        
//        config.setVisible(true);

        rootNode.attachChild(createHDRBox());
    }

    public void simpleUpdate(float tpf){
        if (hdrRender.isInitialized() && dispQuad == null){
            dispQuad = hdrRender.createDisplayQuad();
            dispQuad.setWidth(128);
            dispQuad.setHeight(128);
            dispQuad.setPosition(30, cam.getHeight() - 128 - 30);
            guiNode.attachChild(dispQuad);
        }
    }

//    public void displayAvg(Renderer r){
//        r.setFrameBuffer(null);
//        disp = prepare(-1, -1, settings.getWidth(), settings.getHeight(), 3, -1, scene64, disp);
//        r.clearBuffers(true, true, true);
//        r.renderGeometry(pic);
//    }

}

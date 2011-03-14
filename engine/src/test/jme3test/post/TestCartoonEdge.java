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
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.CartoonEdgeFilter;
import com.jme3.renderer.Caps;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.texture.Texture;

public class TestCartoonEdge extends SimpleApplication {

    private FilterPostProcessor fpp;

    public static void main(String[] args){
        TestCartoonEdge app = new TestCartoonEdge();
        app.start();
    }

    public void setupFilters(){
        if (renderer.getCaps().contains(Caps.GLSL100)){
            fpp=new FilterPostProcessor(assetManager);
            //fpp.setNumSamples(4);
            CartoonEdgeFilter toon=new CartoonEdgeFilter();
            toon.setEdgeColor(ColorRGBA.Yellow);
            fpp.addFilter(toon);
            viewPort.addProcessor(fpp);
        }
    }

    public void makeToonish(Spatial spatial){
        if (spatial instanceof Node){
            Node n = (Node) spatial;
            for (Spatial child : n.getChildren())
                makeToonish(child);
        }else if (spatial instanceof Geometry){
            Geometry g = (Geometry) spatial;
            Material m = g.getMaterial();
            if (m.getMaterialDef().getName().equals("Phong Lighting")){
                Texture t = assetManager.loadTexture("Textures/ColorRamp/toon.png");
//                t.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
//                t.setMagFilter(Texture.MagFilter.Nearest);
                m.setTexture("ColorRamp", t);
                m.setBoolean("UseMaterialColors", true);
                m.setColor("Specular", ColorRGBA.Black);
                m.setColor("Diffuse", ColorRGBA.White);
                m.setBoolean("VertexLighting", true);
            }
        }
    }

    public void setupLighting(){
   
        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-1, -1, 1).normalizeLocal());
        dl.setColor(new ColorRGBA(2,2,2,1));

        rootNode.addLight(dl);
    }

    public void setupModel(){
        Spatial model = assetManager.loadModel("Models/MonkeyHead/MonkeyHead.mesh.xml");
        makeToonish(model);
        model.rotate(0, FastMath.PI, 0);
//        signpost.setLocalTranslation(12, 3.5f, 30);
//        model.scale(0.10f);
//        signpost.setShadowMode(ShadowMode.CastAndReceive);
        rootNode.attachChild(model);
    }

    @Override
    public void simpleInitApp() {
        viewPort.setBackgroundColor(ColorRGBA.Gray);

        cam.setLocation(new Vector3f(-5.6310086f, 5.0892987f, -13.000479f));
        cam.setRotation(new Quaternion(0.1779095f, 0.20036356f, -0.03702727f, 0.96272093f));
        cam.update();

        cam.setFrustumFar(300);
        flyCam.setMoveSpeed(30);

        rootNode.setCullHint(CullHint.Never);

        setupLighting();
        setupModel();
        setupFilters();
    }

}

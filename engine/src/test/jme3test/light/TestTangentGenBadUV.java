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

package jme3test.light;

import com.jme3.app.SimpleApplication;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Sphere;
import com.jme3.util.TangentBinormalGenerator;

public class TestTangentGenBadUV extends SimpleApplication {

    float angle;
    PointLight pl;
    Geometry lightMdl;

    public static void main(String[] args){
        TestTangentGenBadUV app = new TestTangentGenBadUV();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        Spatial teapot = assetManager.loadModel("Models/Teapot/Teapot.obj");
        if (teapot instanceof Geometry){
            Geometry g = (Geometry) teapot;
            TangentBinormalGenerator.generate(g.getMesh());
        }else{
            throw new RuntimeException();
        }
        teapot.setLocalScale(2f);
        Material mat = assetManager.loadMaterial("Textures/BumpMapTest/Tangent.j3m");
        teapot.setMaterial(mat);
        rootNode.attachChild(teapot);

        Geometry debug = new Geometry(
                "Debug Teapot",
                TangentBinormalGenerator.genTbnLines(((Geometry) teapot).getMesh(), 0.03f)
        );
        Material debugMat = assetManager.loadMaterial("Common/Materials/VertexColor.j3m");
        debug.setMaterial(debugMat);
        debug.setCullHint(Spatial.CullHint.Never);
        debug.getLocalTranslation().set(teapot.getLocalTranslation());
        debug.getLocalScale().set(teapot.getLocalScale());
        rootNode.attachChild(debug);


        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(1,-1,-1).normalizeLocal());
        dl.setColor(ColorRGBA.White);
        rootNode.addLight(dl);

        lightMdl = new Geometry("Light", new Sphere(10, 10, 0.1f));
        lightMdl.setMaterial(assetManager.loadMaterial("Common/Materials/RedColor.j3m"));
        lightMdl.getMesh().setStatic();
        rootNode.attachChild(lightMdl);

        pl = new PointLight();
        pl.setColor(ColorRGBA.White);
        //pl.setRadius(3f);
        rootNode.addLight(pl);
    }

    @Override
    public void simpleUpdate(float tpf){
        angle += tpf;
        angle %= FastMath.TWO_PI;
        
        pl.setPosition(new Vector3f(FastMath.cos(angle) * 2f, 0.5f, FastMath.sin(angle) * 2f));
        lightMdl.setLocalTranslation(pl.getPosition());
    }

}

/*
 * Copyright (c) 2009-2019 jMonkeyEngine
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

package jme3test.export;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.MaterialKey;
import com.jme3.asset.ModelKey;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.export.binary.BinaryImporter;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.AssetLinkNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Sphere;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TestAssetLinkNode extends SimpleApplication {

    float angle;
    PointLight pl;
    Spatial lightMdl;

    public static void main(String[] args){
        TestAssetLinkNode app = new TestAssetLinkNode();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        AssetLinkNode loaderNode=new AssetLinkNode();
        loaderNode.addLinkedChild(new ModelKey("Models/MonkeyHead/MonkeyHead.mesh.xml"));
        //load/attach the children (happens automatically on load)
//        loaderNode.attachLinkedChildren(assetManager);
//        rootNode.attachChild(loaderNode);

        //save and load the loaderNode
        try {
            //export to byte array
            ByteArrayOutputStream bout=new ByteArrayOutputStream();
            BinaryExporter.getInstance().save(loaderNode, bout);
            //import from byte array, automatically loads the monkeyhead from file
            ByteArrayInputStream bin=new ByteArrayInputStream(bout.toByteArray());
            BinaryImporter imp=BinaryImporter.getInstance();
            imp.setAssetManager(assetManager);
            Node newLoaderNode=(Node)imp.load(bin);
            //attach to rootNode
            rootNode.attachChild(newLoaderNode);
        } catch (IOException ex) {
            Logger.getLogger(TestAssetLinkNode.class.getName()).log(Level.SEVERE, null, ex);
        }


        rootNode.attachChild(loaderNode);

        lightMdl = new Geometry("Light", new Sphere(10, 10, 0.1f));
        lightMdl.setMaterial( (Material) assetManager.loadAsset(new MaterialKey("Common/Materials/RedColor.j3m")));
        rootNode.attachChild(lightMdl);

        // flourescent main light
        pl = new PointLight();
        pl.setColor(new ColorRGBA(0.88f, 0.92f, 0.95f, 1.0f));
        rootNode.addLight(pl);

        // sunset light
        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-0.1f,-0.7f,1).normalizeLocal());
        dl.setColor(new ColorRGBA(0.44f, 0.30f, 0.20f, 1.0f));
        rootNode.addLight(dl);

        // skylight
        dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-0.6f,-1,-0.6f).normalizeLocal());
        dl.setColor(new ColorRGBA(0.10f, 0.22f, 0.44f, 1.0f));
        rootNode.addLight(dl);

        // white ambient light
        dl = new DirectionalLight();
        dl.setDirection(new Vector3f(1, -0.5f,-0.1f).normalizeLocal());
        dl.setColor(new ColorRGBA(0.50f, 0.40f, 0.50f, 1.0f));
        rootNode.addLight(dl);
    }

    @Override
    public void simpleUpdate(float tpf){
        angle += tpf * 0.25f;
        angle %= FastMath.TWO_PI;

        pl.setPosition(new Vector3f(FastMath.cos(angle) * 6f, 3f, FastMath.sin(angle) * 6f));
        lightMdl.setLocalTranslation(pl.getPosition());
    }

}

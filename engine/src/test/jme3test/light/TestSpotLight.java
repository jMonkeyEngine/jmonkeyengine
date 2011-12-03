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
import com.jme3.light.AmbientLight;
import com.jme3.light.SpotLight;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.util.TangentBinormalGenerator;

public class TestSpotLight extends SimpleApplication {

    private Vector3f lightTarget = new Vector3f(12, 3.5f, 30);

    public static void main(String[] args){
        TestSpotLight app = new TestSpotLight();
        app.start();
    }

 SpotLight spot;
    Geometry lightMdl;
    public void setupLighting(){
      AmbientLight al=new AmbientLight();
      al.setColor(ColorRGBA.White.mult(0.8f));
      rootNode.addLight(al);
        
      spot=new SpotLight();
      
      spot.setSpotRange(1000);
      spot.setSpotInnerAngle(5*FastMath.DEG_TO_RAD);
      spot.setSpotOuterAngle(10*FastMath.DEG_TO_RAD);
      spot.setPosition(new Vector3f(77.70334f, 34.013165f, 27.1017f));
      spot.setDirection(lightTarget.subtract(spot.getPosition()));     
      spot.setColor(ColorRGBA.White.mult(2));
      rootNode.addLight(spot);
      
      
//        PointLight pl=new PointLight();
//      pl.setPosition(new Vector3f(77.70334f, 34.013165f, 27.1017f));
//      pl.setRadius(1000);     
//      pl.setColor(ColorRGBA.White.mult(2));
//      rootNode.addLight(pl);
       lightMdl = new Geometry("Light", new Sphere(10, 10, 0.1f));
      lightMdl.setMaterial(assetManager.loadMaterial("Common/Materials/RedColor.j3m"));
      lightMdl.setLocalTranslation(new Vector3f(77.70334f, 34.013165f, 27.1017f));
      lightMdl.setLocalScale(5);
      rootNode.attachChild(lightMdl);
        
//        DirectionalLight dl = new DirectionalLight();
//        dl.setDirection(lightTarget.subtract(new Vector3f(77.70334f, 34.013165f, 27.1017f)));
//        dl.setColor(ColorRGBA.White.mult(2));
//        rootNode.addLight(dl);
      
      
    }

    public void setupFloor(){
        Material mat = assetManager.loadMaterial("Textures/Terrain/Pond/Pond.j3m");
        mat.getTextureParam("DiffuseMap").getTextureValue().setWrap(WrapMode.Repeat);
        mat.getTextureParam("NormalMap").getTextureValue().setWrap(WrapMode.Repeat);
       // mat.getTextureParam("ParallaxMap").getTextureValue().setWrap(WrapMode.Repeat);
        mat.setFloat("Shininess",3);
      //  mat.setBoolean("VertexLighting", true);
        
        
        Box floor = new Box(Vector3f.ZERO, 50, 1f, 50);
        TangentBinormalGenerator.generate(floor);
        floor.scaleTextureCoordinates(new Vector2f(5, 5));
        Geometry floorGeom = new Geometry("Floor", floor);
        floorGeom.setMaterial(mat);
        floorGeom.setShadowMode(ShadowMode.Receive);
        rootNode.attachChild(floorGeom);
    }



    public void setupSignpost(){
        Spatial signpost = assetManager.loadModel("Models/Sign Post/Sign Post.mesh.xml");
        Material mat = assetManager.loadMaterial("Models/Sign Post/Sign Post.j3m");
      //   mat.setBoolean("VertexLighting", true);
        signpost.setMaterial(mat);
        signpost.rotate(0, FastMath.HALF_PI, 0);
        signpost.setLocalTranslation(12, 3.5f, 30);
        signpost.setLocalScale(4);
        signpost.setShadowMode(ShadowMode.CastAndReceive);
        TangentBinormalGenerator.generate(signpost);
        rootNode.attachChild(signpost);
    }

    @Override
    public void simpleInitApp() {
        cam.setLocation(new Vector3f(27.492603f, 29.138166f, -13.232513f));
        cam.setRotation(new Quaternion(0.25168246f, -0.10547892f, 0.02760565f, 0.96164864f));
        flyCam.setMoveSpeed(30);
     
        setupLighting();
        setupFloor();
        setupSignpost();

        
    }
    
    float angle;    

    @Override
    public void simpleUpdate(float tpf) {
        super.simpleUpdate(tpf);
        angle += tpf;
        angle %= FastMath.TWO_PI;

        spot.setPosition(new Vector3f(FastMath.cos(angle) * 30f, 34.013165f, FastMath.sin(angle) * 30f));
        lightMdl.setLocalTranslation(spot.getPosition());
        spot.setDirection(lightTarget.subtract(spot.getPosition()));     
    }
    
    

}

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

package jme3test.helloworld;

import com.jme3.app.SimpleApplication;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.texture.Texture;
import com.jme3.util.TangentBinormalGenerator;

/** Sample 6 - how to give an object's surface a material and texture.
 * How to make objects transparent. How to make bumpy and shiny surfaces.  */
public class HelloMaterial extends SimpleApplication {

  public static void main(String[] args) {
    HelloMaterial app = new HelloMaterial();
    app.start();
  }

  @Override
  public void simpleInitApp() {

    /** A simple textured cube -- in good MIP map quality. */
    Box cube1Mesh = new Box( 1f,1f,1f);
    Geometry cube1Geo = new Geometry("My Textured Box", cube1Mesh);
    cube1Geo.setLocalTranslation(new Vector3f(-3f,1.1f,0f));
    Material cube1Mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    Texture cube1Tex = assetManager.loadTexture("Interface/Logo/Monkey.jpg");
    cube1Mat.setTexture("ColorMap", cube1Tex);
    cube1Geo.setMaterial(cube1Mat);
    rootNode.attachChild(cube1Geo);

    /** A translucent/transparent texture, similar to a window frame. */
    Box cube2Mesh = new Box( 1f,1f,0.01f);
    Geometry cube2Geo = new Geometry("window frame", cube2Mesh);
    Material cube2Mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    cube2Mat.setTexture("ColorMap", assetManager.loadTexture("Textures/ColoredTex/Monkey.png"));
    cube2Mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);  // activate transparency
    cube2Geo.setQueueBucket(Bucket.Transparent);
    cube2Geo.setMaterial(cube2Mat);
    rootNode.attachChild(cube2Geo);

    /** A bumpy rock with a shiny light effect. To make bumpy objects you must create a NormalMap. */
    Sphere sphereMesh = new Sphere(32,32, 2f);
    Geometry sphereGeo = new Geometry("Shiny rock", sphereMesh);
    sphereMesh.setTextureMode(Sphere.TextureMode.Projected); // better quality on spheres
    TangentBinormalGenerator.generate(sphereMesh);           // for lighting effect
    Material sphereMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
    sphereMat.setTexture("DiffuseMap", assetManager.loadTexture("Textures/Terrain/Pond/Pond.jpg"));
    sphereMat.setTexture("NormalMap", assetManager.loadTexture("Textures/Terrain/Pond/Pond_normal.png"));
    sphereMat.setBoolean("UseMaterialColors",true);    
    sphereMat.setColor("Diffuse",ColorRGBA.White);
    sphereMat.setColor("Specular",ColorRGBA.White);
    sphereMat.setFloat("Shininess", 64f); // [0,128]
    sphereGeo.setMaterial(sphereMat);
    //sphereGeo.setMaterial((Material) assetManager.loadMaterial("Materials/MyCustomMaterial.j3m"));
    sphereGeo.setLocalTranslation(0,2,-2); // Move it a bit
    sphereGeo.rotate(1.6f, 0, 0);          // Rotate it a bit
    rootNode.attachChild(sphereGeo);
    
    /** Must add a light to make the lit object visible! */
    DirectionalLight sun = new DirectionalLight();
    sun.setDirection(new Vector3f(1,0,-2).normalizeLocal());
    sun.setColor(ColorRGBA.White);
    rootNode.addLight(sun);
  }
}

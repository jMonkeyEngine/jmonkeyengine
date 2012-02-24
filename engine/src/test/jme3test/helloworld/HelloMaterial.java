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
 * How to make objects transparent, or let colors "leak" through partially
 * transparent textures. How to make bumpy and shiny surfaces.  */
public class HelloMaterial extends SimpleApplication {

  public static void main(String[] args) {
    HelloMaterial app = new HelloMaterial();
    app.start();
  }

  @Override
  public void simpleInitApp() {

    /** A simple textured cube -- in good MIP map quality. */
    Box boxshape1 = new Box(new Vector3f(-3f,1.1f,0f), 1f,1f,1f);
    Geometry cube = new Geometry("My Textured Box", boxshape1);
    Material mat_stl = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    Texture tex_ml = assetManager.loadTexture("Interface/Logo/Monkey.jpg");
    mat_stl.setTexture("ColorMap", tex_ml);
    cube.setMaterial(mat_stl);
    rootNode.attachChild(cube);

    /** A translucent/transparent texture, similar to a window frame. */
    Box boxshape3 = new Box(new Vector3f(0f,0f,0f), 1f,1f,0.01f);
    Geometry window_frame = new Geometry("window frame", boxshape3);
    Material mat_tt = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    mat_tt.setTexture("ColorMap", assetManager.loadTexture("Textures/ColoredTex/Monkey.png"));
    mat_tt.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);  // activate transparency
    window_frame.setQueueBucket(Bucket.Transparent);
    window_frame.setMaterial(mat_tt);
    rootNode.attachChild(window_frame);

    /** A cube with its base color "leaking" through a partially transparent texture */
    Box boxshape4 = new Box(new Vector3f(3f,-1f,0f), 1f,1f,1f);
    Geometry cube_leak = new Geometry("Leak-through color cube", boxshape4);
    Material mat_tl = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    mat_tl.setTexture("ColorMap", assetManager.loadTexture("Textures/ColoredTex/Monkey.png"));
    mat_tl.setColor("Color", new ColorRGBA(1f,0f,1f, 1f)); // purple
    cube_leak.setMaterial(mat_tl);
    rootNode.attachChild(cube_leak);
    // cube_leak.setMaterial((Material) assetManager.loadAsset( "Materials/LeakThrough.j3m"));

    /** A bumpy rock with a shiny light effect. To make bumpy objects you must create a NormalMap. */
    Sphere rock = new Sphere(32,32, 2f);
    Geometry shiny_rock = new Geometry("Shiny rock", rock);
    rock.setTextureMode(Sphere.TextureMode.Projected); // better quality on spheres
    TangentBinormalGenerator.generate(rock);           // for lighting effect
    Material mat_lit = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
    mat_lit.setTexture("DiffuseMap", assetManager.loadTexture("Textures/Terrain/Pond/Pond.jpg"));
    mat_lit.setTexture("NormalMap", assetManager.loadTexture("Textures/Terrain/Pond/Pond_normal.png"));
    mat_lit.setBoolean("UseMaterialColors",true);    
    mat_lit.setColor("Specular",ColorRGBA.White);
    mat_lit.setColor("Diffuse",ColorRGBA.White);
    mat_lit.setFloat("Shininess", 5f); // [0,128]
    shiny_rock.setMaterial(mat_lit);
    shiny_rock.setLocalTranslation(0,2,-2); // Move it a bit
    shiny_rock.rotate(1.6f, 0, 0);          // Rotate it a bit
    rootNode.attachChild(shiny_rock);
    
    /** Must add a light to make the lit object visible! */
    DirectionalLight sun = new DirectionalLight();
    sun.setDirection(new Vector3f(1,0,-2).normalizeLocal());
    sun.setColor(ColorRGBA.White);
    rootNode.addLight(sun);

  }
}

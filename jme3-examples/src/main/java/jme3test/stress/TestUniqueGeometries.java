/*
 * Copyright (c) 2009-2015 jMonkeyEngine
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
package jme3test.stress;

import com.jme3.app.SimpleApplication;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;

// Let's see if we can render 2500 batches in 60 fps.
// We'll use 50 materials with various combinations of textures and colors
// to make things wild.
public class TestUniqueGeometries extends SimpleApplication {

    private Material[] randomMaterials = new Material[50];
    
    private String[] textureList = new String[] {
        "Blender/2.4x/textures/Concrete_Wall.PNG", 
        "Blender/2.4x/textures/Grass_256.png", 
        "Blender/2.4x/textures/SandDesert_StartTower.png", 
        "Blender/2.4x/textures/Tar_Cracked.png", 
        "Blender/2.4x/textures/WarningStrip.png", 
        "Blender/2.4x/WoodCrate_lighter.png",
        "Interface/Logo/Monkey.jpg", 
        "Interface/Logo/Monkey.png", 
        "Models/Boat/boat.png", 
        "Models/Ninja/Ninja.jpg",
        "Models/Tree/BarkColor.jpg", 
        "Textures/Terrain/BrickWall/BrickWall.jpg", 
        "Textures/Terrain/Pond/Pond.jpg", 
        "Textures/Terrain/Pond/Pond_normal.png", 
        "Textures/Terrain/Rock/Rock.PNG",
        "Textures/Terrain/Rock/Rock_normal.png",
        "Textures/Terrain/Rock2/rock.jpg",
        "Textures/Terrain/Rocky/RockyNormals.jpg",
        "Textures/Terrain/Rocky/RockyTexture.jpg",
        "Textures/Terrain/splat/alpha1.png", 
        "Textures/Terrain/splat/alpha2.png", 
        "Textures/Terrain/splat/alphamap.png", 
        "Textures/Terrain/splat/alphamap2.png", 
        "Textures/Terrain/splat/dirt.jpg", 
        "Textures/Terrain/splat/dirt_normal.png", 
        "Textures/Terrain/splat/fortress512.png", 
        "Textures/Terrain/splat/grass.jpg", 
        "Textures/Terrain/splat/grass_normal.jpg",
        "Textures/Terrain/splat/mountains128.png",
        "Textures/Terrain/splat/road.jpg",
        "Textures/Terrain/splat/road_normal.png",
    };
    
    public static void main(String[] args) {
        TestUniqueGeometries app = new TestUniqueGeometries();
        AppSettings settings = new AppSettings(true);
        settings.putBoolean("GraphicsTrace", false);
        settings.putBoolean("GraphicsTiming", true);
        app.setSettings(settings);
        app.start();
    }
    
    private void loadRandomMaterials() {
        for (int i = 0; i < randomMaterials.length; i++) {
            Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
            mat.setBoolean("VertexLighting", true);
            mat.setBoolean("UseMaterialColors", true);
            mat.setColor("Ambient", ColorRGBA.Black);
            mat.setColor("Diffuse", ColorRGBA.White);
            mat.setColor("Specular", ColorRGBA.White);
            mat.setFloat("Shininess", 32);
            mat.setTexture("DiffuseMap", assetManager.loadTexture(textureList[i % textureList.length]));
            randomMaterials[i] = mat;
        }
    }
    
    @Override
    public void simpleInitApp() {
        flyCam.setDragToRotate(true);
        
        cam.setLocation(new Vector3f(22.717342f, 18.366547f, 22.043106f));
        cam.setRotation(new Quaternion(-0.11630201f, 0.8794429f, -0.27703872f, -0.36919326f));
        
        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-1, -1, -1).normalizeLocal());
        rootNode.addLight(dl);
        
        flyCam.setMoveSpeed(5);
        
        loadRandomMaterials();
        
        // Box box = new Box(1,1,1);
        
        for (int y = -25; y < 25; y++) {
            for (int x = -25; x < 25; x++) {
                Material mat = randomMaterials[0]; // randomMaterials[FastMath.nextRandomInt(0, randomMaterials.length - 1)];
        
                Box box = new Box(1,1,1);
                Geometry boxClone = new Geometry("box", box);
                boxClone.setMaterial(mat);
                
                boxClone.setLocalTranslation(x * .5f, 0, y * .5f);
                boxClone.setLocalScale(.15f);
                boxClone.setMaterial(mat);
                rootNode.attachChild(boxClone);
            }
        }
    }
}

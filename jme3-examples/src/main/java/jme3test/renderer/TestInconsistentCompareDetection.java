/*
 * Copyright (c) 2009-2020 jMonkeyEngine
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
package jme3test.renderer;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.texture.Texture;

/**
 * Changes a material's texture from another thread while it is rendered.
 * This should trigger the sorting function's inconsistent compare detection.
 * 
 * @author Kirill Vainer
 */
public class TestInconsistentCompareDetection extends SimpleApplication {

    private static Texture t1, t2;
    
    public static void main(String[] args){
        TestInconsistentCompareDetection app = new TestInconsistentCompareDetection();
        app.start();
    }
    
    @Override
    public void simpleInitApp() {
        cam.setLocation(new Vector3f(-11.674385f, 7.892636f, 33.133106f));
        cam.setRotation(new Quaternion(0.06426433f, 0.90940624f, -0.15329266f, 0.38125014f));
        
        Material m = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        m.setColor("Color", ColorRGBA.White);
        
        t1 = assetManager.loadTexture("Textures/Terrain/BrickWall/BrickWall.jpg");
        t2 = assetManager.loadTexture("Textures/Terrain/Pond/Pond.jpg");
        
        Box b = new Box(1, 1, 1);

        for (int x = 0; x < 12; x++) {
            for (int y = 0; y < 12; y++) {
                Geometry g = new Geometry("g_" + x + "_" + y, b);
                Node monkey = new Node("n_" + x + "_" + y);
                monkey.attachChild(g);
                monkey.move(x * 2, 0, y * 2);

                Material newMat = m.clone();
                g.setMaterial(newMat);

                if (FastMath.rand.nextBoolean()) {
                    newMat.setTexture("ColorMap", t1);
                } else {
                    newMat.setTexture("ColorMap", t2);
                }

                rootNode.attachChild(monkey);
            }
        }
        
        Thread evilThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                }
                
                // begin randomly changing textures after 1 sec.
                while (true) {
                    for (Spatial child : rootNode.getChildren()) {
                        Geometry g = (Geometry) (((Node)child).getChild(0));
                        Material m = g.getMaterial();
                        Texture curTex = m.getTextureParam("ColorMap").getTextureValue();
                        if (curTex == t1) {
                            m.setTexture("ColorMap", t2);
                        } else {
                            m.setTexture("ColorMap", t1);
                        }
                    }
                }
            }
        });
        evilThread.setDaemon(true);
        evilThread.start();
    }
}


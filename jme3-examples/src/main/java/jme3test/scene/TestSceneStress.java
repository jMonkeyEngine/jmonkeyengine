/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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

package jme3test.scene;

import com.jme3.app.BasicProfilerState;
import com.jme3.app.DebugKeysAppState;
import com.jme3.app.FlyCamAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsAppState;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.shape.Box;
import java.util.Random;


/**
 *  Tests a deep scene with an unrecommended amount of objects.
 *
 *  @author    Paul Speed
 */
public class TestSceneStress extends SimpleApplication {
 
    final private static Box BOX = new Box(2f, 0.5f, 0.5f);
 
    private Material mat;
    final private Random random = new Random(0);
 
    private int totalNodes = 0;
    private int totalGeometry = 0;
    private int totalControls = 0;
    
    public static void main( String... args ) {
        
        TestSceneStress test = new TestSceneStress();
        test.start();
    }
    
    public TestSceneStress() {
        super(new StatsAppState(), new DebugKeysAppState(), new BasicProfilerState(false),
              new FlyCamAppState(),
              new ScreenshotAppState("", System.currentTimeMillis())); 
    }
    
    @Override
    public void simpleInitApp() {
 
        stateManager.getState(FlyCamAppState.class).getCamera().setMoveSpeed(10);
    
        mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Blue);

        // Create a deep, mostly static scene        
        Spatial oct = createOctSplit("root", 500, 5);
        
        rootNode.attachChild(oct);
 
        // Position to see most of it       
        cam.setLocation(new Vector3f(400.8009f, 370.16455f, -408.17984f));
        cam.setRotation(new Quaternion(0.24906662f, -0.3756747f, 0.105560325f, 0.88639235f));
        
        System.out.println("Total nodes:" + totalNodes + "  Total Geometry:" + totalGeometry + "  Total controls:" + totalControls );        
    }
    
    protected Spatial createOctSplit( String name, int size, int depth ) {
        
        if( depth == 0 ) {
            // Done splitting
            Geometry geom = new Geometry(name, BOX);
            totalGeometry++;
            geom.setMaterial(mat);
            
            if( random.nextFloat() < 0.01 ) {
                RotatorControl control = new RotatorControl(random.nextFloat(), random.nextFloat(), random.nextFloat());
                geom.addControl(control);
                totalControls++;
            }
            
            return geom;
        }
        
        Node root = new Node(name);
        totalNodes++;
 
        int half = size / 2;
        float quarter = half * 0.5f;
 
        for( int i = 0; i < 2; i++ ) {
            float x = i * half - quarter;
            for( int j = 0; j < 2; j++ ) {
                float y = j * half - quarter;
                for( int k = 0; k < 2; k++ ) {
                    float z = k * half - quarter;
                    
                    Spatial child = createOctSplit(name + "(" + i + ", " + j + ", " + k + ")", 
                                                   half, depth - 1);
                    child.setLocalTranslation(x, y, z);                                                   
                    root.attachChild(child);   
                }
            }
        }
       
        return root;        
    }
    
    private class RotatorControl extends AbstractControl {
        final private float[] rotate;
        
        public RotatorControl( float... rotate ) {
            this.rotate = rotate;
        }

        @Override
        protected void controlUpdate( float tpf ) {
            if( spatial != null ) {
                spatial.rotate(rotate[0] * tpf, rotate[1] * tpf, rotate[2] * tpf);
            }
        }

        @Override
        protected void controlRender( RenderManager rm, ViewPort vp ) {
        }        
    }  
}

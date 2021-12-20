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
package jme3test.model;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.ModelKey;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.font.Rectangle;
import com.jme3.light.AmbientLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;

public class TestObjGroupsLoading extends SimpleApplication {
    
    public static void main(String[] args) {
        TestObjGroupsLoading app = new TestObjGroupsLoading();
        app.start();
    }
    
    private BitmapText pointerDisplay;
    
    @Override
    public void simpleInitApp() {
    
        // load scene with following structure:
        // Chair 1 (just mesh without name) and named groups: Chair 2, Pillow 2, Podium
        Spatial scene = assetManager.loadModel(new ModelKey("OBJLoaderTest/TwoChairs.obj"));
        // add light to make it visible
        scene.addLight(new AmbientLight(ColorRGBA.White));
        // attach scene to the root
        rootNode.attachChild(scene);
        
        // configure camera for best scene viewing
        cam.setLocation(new Vector3f(-3, 4, 3));
        cam.lookAtDirection(new Vector3f(0, -0.5f, -1), Vector3f.UNIT_Y);
        flyCam.setMoveSpeed(10);
        
        // create display to indicate pointed geometry name
        pointerDisplay = new BitmapText(guiFont);
        pointerDisplay.setBox(new Rectangle(0, settings.getHeight(), settings.getWidth(), settings.getHeight()/2));
        pointerDisplay.setAlignment(BitmapFont.Align.Center);
        pointerDisplay.setVerticalAlignment(BitmapFont.VAlign.Center);
        guiNode.attachChild(pointerDisplay);
        
        initCrossHairs();
    }
    
    @Override
    public void simpleUpdate(final float tpf) {
        
        // ray to the center of the screen from the camera
        Ray ray = new Ray(cam.getLocation(), cam.getDirection());
        
        // find object at the center of the screen
    
        final CollisionResults results = new CollisionResults();
        rootNode.collideWith(ray, results);
        
        CollisionResult result = results.getClosestCollision();
        if (result == null) {
            pointerDisplay.setText("");
        } else {
            // display pointed geometry and it's parents names
            StringBuilder sb = new StringBuilder();
            for (Spatial node = result.getGeometry(); node != null; node = node.getParent()) {
                if (sb.length() > 0) {
                    sb.append(" < ");
                }
                sb.append(node.getName());
            }
            pointerDisplay.setText(sb);
        }
    }
    
    private void initCrossHairs() {
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        BitmapText ch = new BitmapText(guiFont);
        ch.setSize(guiFont.getCharSet().getRenderedSize() * 2);
        ch.setText("+"); // crosshairs
        ch.setLocalTranslation( // center
            settings.getWidth() / 2 - guiFont.getCharSet().getRenderedSize() / 3 * 2,
            settings.getHeight() / 2 + ch.getLineHeight() / 2, 0);
        guiNode.attachChild(ch);
    }
}

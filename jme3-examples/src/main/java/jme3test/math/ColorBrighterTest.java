/*
 * Copyright (c) 2009-2024 jMonkeyEngine
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
package jme3test.math;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;

/**
 * Class where both the brightness and darkness of colors are tested.
 * @author wil
 */
public class ColorBrighterTest extends SimpleApplication {
    
    public static void main(String[] args) {
        ColorBrighterTest app = new ColorBrighterTest();
        
        AppSettings settings = new AppSettings(true);
        settings.setResolution(1204, 756);
        settings.setGammaCorrection(false);
        
        app.setShowSettings(false);
        app.setSettings(settings);
        app.start();
    }
    
    @Override
    public void simpleInitApp() {
        ColorRGBA color = ColorRGBA.randomColor();

        Geometry normal = getInstanceBox(color);
        normal.addControl(new ColorControl(Vector3f.UNIT_X));
        rootNode.attachChild(normal);

        Geometry darker = getInstanceBox(ColorRGBA.darker(color));
        darker.move(4, 0, 0);
        darker.addControl(new ColorControl(Vector3f.UNIT_Y));
        rootNode.attachChild(darker);

        Geometry brighter = getInstanceBox(ColorRGBA.brighter(color));
        brighter.move(-4, 0, 0);
        brighter.addControl(new ColorControl(Vector3f.UNIT_Z));
        rootNode.attachChild(brighter);
    }

    private Geometry getInstanceBox(ColorRGBA color) {
        Box b = new Box(1, 1, 1);
        Geometry geom = new Geometry("Box", b);

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", color);
        geom.setMaterial(mat);
        return geom;
    }

    class ColorControl extends AbstractControl {

        Vector3f dir;
        float angle = 0;

        public ColorControl(Vector3f dir) {
            this.dir = dir;
        }
        
        @Override
        protected void controlUpdate(float tpf) {
            spatial.setLocalRotation(new Quaternion().fromAngleAxis(angle, dir));
            angle += 5f * tpf;
        }

        @Override
        protected void controlRender(RenderManager rm, ViewPort vp) { }        
    }
}

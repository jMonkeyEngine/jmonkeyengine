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

package jme3test.model.shape;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.debug.Arrow;
import com.jme3.scene.debug.Grid;
import com.jme3.scene.debug.WireBox;
import com.jme3.scene.debug.WireSphere;

public class TestDebugShapes extends SimpleApplication {

    public static void main(String[] args){
        TestDebugShapes app = new TestDebugShapes();
        app.start();
    }

    public Geometry putShape(Mesh shape, ColorRGBA color){
        Geometry g = new Geometry("shape", shape);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.getAdditionalRenderState().setWireframe(true);
        mat.setColor("Color", color);
        g.setMaterial(mat);
        rootNode.attachChild(g);
        return g;
    }

    public void putArrow(Vector3f pos, Vector3f dir, ColorRGBA color){
        Arrow arrow = new Arrow(dir);
        arrow.setLineWidth(4); // make arrow thicker
        putShape(arrow, color).setLocalTranslation(pos);
    }

    public void putBox(Vector3f pos, float size, ColorRGBA color){
        putShape(new WireBox(size, size, size), color).setLocalTranslation(pos);
    }

    public void putGrid(Vector3f pos, ColorRGBA color){
        putShape(new Grid(6, 6, 0.2f), color).center().move(pos);
    }

    public void putSphere(Vector3f pos, ColorRGBA color){
        putShape(new WireSphere(1), color).setLocalTranslation(pos);
    }

    @Override
    public void simpleInitApp() {
        cam.setLocation(new Vector3f(2,1.5f,2));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);

        putArrow(Vector3f.ZERO, Vector3f.UNIT_X, ColorRGBA.Red);
        putArrow(Vector3f.ZERO, Vector3f.UNIT_Y, ColorRGBA.Green);
        putArrow(Vector3f.ZERO, Vector3f.UNIT_Z, ColorRGBA.Blue);

        putBox(new Vector3f(2, 0, 0), 0.5f, ColorRGBA.Yellow);
        putGrid(new Vector3f(3.5f, 0, 0), ColorRGBA.White);
        putSphere(new Vector3f(4.5f, 0, 0), ColorRGBA.Magenta);
    }

}

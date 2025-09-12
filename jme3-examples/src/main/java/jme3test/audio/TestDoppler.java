/*
 * Copyright (c) 2009-2025 jMonkeyEngine
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

package jme3test.audio;

import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioNode;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.debug.Grid;
import com.jme3.scene.shape.Sphere;

import java.util.Locale;

/**
 * Test Doppler Effect
 */
public class TestDoppler extends SimpleApplication {

    private float pos = -5;
    private float vel = 5;
    private AudioNode ufoNode;
    private BitmapText bmp;

    public static void main(String[] args) {
        TestDoppler app = new TestDoppler();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        configureCamera();
        bmp = createLabelText(10, 20, "<placeholder>");

        ufoNode = new AudioNode(assetManager, "Sound/Effects/Beep.ogg", AudioData.DataType.Buffer);
        ufoNode.setLooping(true);
        ufoNode.setPitch(0.5f);
        ufoNode.setRefDistance(1);
        ufoNode.setMaxDistance(100000000);
        ufoNode.setVelocityFromTranslation(true);
        rootNode.attachChild(ufoNode);

        Geometry ball = makeShape("Beeper", new Sphere(10, 10, .5f), ColorRGBA.Red);
        ufoNode.attachChild(ball);

        Geometry grid = makeShape("DebugGrid", new Grid(21, 21, 2), ColorRGBA.Gray);
        grid.center().move(0, 0, 0);
        rootNode.attachChild(grid);

        ufoNode.play();
    }

    private void configureCamera() {
        flyCam.setMoveSpeed(15f);
        flyCam.setDragToRotate(true);

        cam.setLocation(Vector3f.UNIT_XYZ.mult(12));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
    }

    @Override
    public void simpleUpdate(float tpf) {
        pos += tpf * vel;
        if (pos < -10f || pos > 10f) {
            vel *= -1;
        }
        ufoNode.setLocalTranslation(pos, 0f, 0f);
        bmp.setText(String.format(Locale.ENGLISH, "Audio Position: (%.2f, %.1f, %.1f)", pos, 0f, 0f));
    }

    private BitmapText createLabelText(int x, int y, String text) {
        BitmapText bmp = new BitmapText(guiFont);
        bmp.setText(text);
        bmp.setLocalTranslation(x, settings.getHeight() - y, 0);
        bmp.setColor(ColorRGBA.Red);
        guiNode.attachChild(bmp);
        return bmp;
    }

    private Geometry makeShape(String name, Mesh mesh, ColorRGBA color) {
        Geometry geo = new Geometry(name, mesh);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", color);
        geo.setMaterial(mat);
        return geo;
    }
}

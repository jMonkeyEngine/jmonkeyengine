/*
 * Copyright (c) 2021 jMonkeyEngine
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
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.material.Materials;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.shape.Line;
import com.jme3.system.AppSettings;

/**
 * Display the renderer's maximum line width.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TestLineWidth extends SimpleApplication {

    public static void main(String... args) {
        TestLineWidth app = new TestLineWidth();
        AppSettings set = new AppSettings(true);
        set.setRenderer(AppSettings.LWJGL_OPENGL2);
        app.setSettings(set);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        /*
         * Generate a message to report (1) which renderer is selected
         * and (2) the maximum line width.
         */
        String rendererName = settings.getRenderer();
        float maxWidth = renderer.getMaxLineWidth();
        String message = String.format(
                "using %s renderer%nmaximum line width = %.1f pixel%s",
                rendererName, maxWidth, (maxWidth == 1f) ? "" : "s");
        /*
         * Display the message, centered near the top of the display.
         */
        BitmapFont font = assetManager.loadFont("Interface/Fonts/Default.fnt");
        BitmapText text = new BitmapText(font);
        text.setSize(font.getCharSet().getRenderedSize());
        text.setText(message);
        float leftX = (cam.getWidth() - text.getLineWidth()) / 2;
        float topY = cam.getHeight();
        text.setLocalTranslation(leftX, topY, 0f);
        guiNode.attachChild(text);
        /*
         * Display a vertical green line on the left side of the display.
         */
        float lineWidth = Math.min(maxWidth, leftX);
        drawVerticalLine(lineWidth, leftX / 2, ColorRGBA.Green);
    }

    private void drawVerticalLine(float lineWidth, float x, ColorRGBA color) {
        Material material = new Material(assetManager, Materials.UNSHADED);
        material.setColor("Color", color.clone());
        material.getAdditionalRenderState().setLineWidth(lineWidth);

        float viewportHeight = cam.getHeight();
        Vector3f startLocation = new Vector3f(x, 0.1f * viewportHeight, 0f);
        Vector3f endLocation = new Vector3f(x, 0.9f * viewportHeight, 0f);
        Mesh wireMesh = new Line(startLocation, endLocation);
        Geometry wire = new Geometry("wire", wireMesh);
        wire.setMaterial(material);
        guiNode.attachChild(wire);
    }
}

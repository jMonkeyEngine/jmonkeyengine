/*
 * Copyright (c) 2009-2019 jMonkeyEngine
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

package jme3test.gui;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.font.Rectangle;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;

public class TestBitmapFontAlignment extends SimpleApplication {

    public static void main(String[] args) {
        TestBitmapFontAlignment test = new TestBitmapFontAlignment();
        test.start();
    }

    @Override
    public void simpleInitApp() {
        int width = getCamera().getWidth();
        int height = getCamera().getHeight();

        // VAlign.Top
        BitmapText labelAlignTop = guiFont.createLabel("This text has VAlign.Top.");
        Rectangle textboxAlignTop = new Rectangle(width * 0.2f, height * 0.7f, 120, 120);
        labelAlignTop.setBox(textboxAlignTop);
        labelAlignTop.setVerticalAlignment(BitmapFont.VAlign.Top);
        getGuiNode().attachChild(labelAlignTop);

        Geometry backgroundBoxAlignTop = new Geometry("", new Quad(textboxAlignTop.width, -textboxAlignTop.height));
        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", ColorRGBA.Blue);
        backgroundBoxAlignTop.setMaterial(material);
        backgroundBoxAlignTop.setLocalTranslation(textboxAlignTop.x, textboxAlignTop.y, -1);
        getGuiNode().attachChild(backgroundBoxAlignTop);

        // VAlign.Center
        BitmapText labelAlignCenter = guiFont.createLabel("This text has VAlign.Center");
        Rectangle textboxAlignCenter = new Rectangle(width * 0.4f, height * 0.7f, 120, 120);
        labelAlignCenter.setBox(textboxAlignCenter);
        labelAlignCenter.setVerticalAlignment(BitmapFont.VAlign.Center);
        getGuiNode().attachChild(labelAlignCenter);

        Geometry backgroundBoxAlignCenter = backgroundBoxAlignTop.clone(false);
        backgroundBoxAlignCenter.setLocalTranslation(textboxAlignCenter.x, textboxAlignCenter.y, -1);
        getGuiNode().attachChild(backgroundBoxAlignCenter);

        // VAlign.Bottom
        BitmapText labelAlignBottom = guiFont.createLabel("This text has VAlign.Bottom");
        Rectangle textboxAlignBottom = new Rectangle(width * 0.6f, height * 0.7f, 120, 120);
        labelAlignBottom.setBox(textboxAlignBottom);
        labelAlignBottom.setVerticalAlignment(BitmapFont.VAlign.Bottom);
        getGuiNode().attachChild(labelAlignBottom);

        Geometry backgroundBoxAlignBottom = backgroundBoxAlignTop.clone(false);
        backgroundBoxAlignBottom.setLocalTranslation(textboxAlignBottom.x, textboxAlignBottom.y, -1);
        getGuiNode().attachChild(backgroundBoxAlignBottom);

        // VAlign.Top + Align.Right
        BitmapText labelAlignTopRight = guiFont.createLabel("This text has VAlign.Top and Align.Right");
        Rectangle textboxAlignTopRight = new Rectangle(width * 0.2f, height * 0.3f, 120, 120);
        labelAlignTopRight.setBox(textboxAlignTopRight);
        labelAlignTopRight.setVerticalAlignment(BitmapFont.VAlign.Top);
        labelAlignTopRight.setAlignment(BitmapFont.Align.Right);
        getGuiNode().attachChild(labelAlignTopRight);

        Geometry backgroundBoxAlignTopRight = backgroundBoxAlignTop.clone(false);
        backgroundBoxAlignTopRight.setLocalTranslation(textboxAlignTopRight.x, textboxAlignTopRight.y, -1);
        getGuiNode().attachChild(backgroundBoxAlignTopRight);

        // VAlign.Center + Align.Center
        BitmapText labelAlignCenterCenter = guiFont.createLabel("This text has VAlign.Center and Align.Center");
        Rectangle textboxAlignCenterCenter = new Rectangle(width * 0.4f, height * 0.3f, 120, 120);
        labelAlignCenterCenter.setBox(textboxAlignCenterCenter);
        labelAlignCenterCenter.setVerticalAlignment(BitmapFont.VAlign.Center);
        labelAlignCenterCenter.setAlignment(BitmapFont.Align.Center);
        getGuiNode().attachChild(labelAlignCenterCenter);

        Geometry backgroundBoxAlignCenterCenter = backgroundBoxAlignCenter.clone(false);
        backgroundBoxAlignCenterCenter.setLocalTranslation(textboxAlignCenterCenter.x, textboxAlignCenterCenter.y, -1);
        getGuiNode().attachChild(backgroundBoxAlignCenterCenter);

        // VAlign.Bottom + Align.Left
        BitmapText labelAlignBottomLeft = guiFont.createLabel("This text has VAlign.Bottom and Align.Left");
        Rectangle textboxAlignBottomLeft = new Rectangle(width * 0.6f, height * 0.3f, 120, 120);
        labelAlignBottomLeft.setBox(textboxAlignBottomLeft);
        labelAlignBottomLeft.setVerticalAlignment(BitmapFont.VAlign.Bottom);
        labelAlignBottomLeft.setAlignment(BitmapFont.Align.Left);
        getGuiNode().attachChild(labelAlignBottomLeft);

        Geometry backgroundBoxAlignBottomLeft = backgroundBoxAlignBottom.clone(false);
        backgroundBoxAlignBottomLeft.setLocalTranslation(textboxAlignBottomLeft.x, textboxAlignBottomLeft.y, -1);
        getGuiNode().attachChild(backgroundBoxAlignBottomLeft);

        // Large quad with VAlign.Center and Align.Center
        BitmapText label = guiFont.createLabel("This text is centered, both horizontally and vertically.");
        Rectangle box = new Rectangle(width * 0.05f, height * 0.95f, width * 0.9f, height * 0.1f);
        label.setBox(box);
        label.setAlignment(BitmapFont.Align.Center);
        label.setVerticalAlignment(BitmapFont.VAlign.Center);
        getGuiNode().attachChild(label);

        Geometry background = new Geometry("background", new Quad(box.width, -box.height));
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Green);
        background.setMaterial(mat);
        background.setLocalTranslation(box.x, box.y, -1);
        getGuiNode().attachChild(background);
    }

}

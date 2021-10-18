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
package jme3test.gui;

import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsAppState;
import com.jme3.font.*;
import com.jme3.math.ColorRGBA;

/**
 * Test case for JME issue #1158: BitmapText right to left line wrapping not work
 */
public class TestRtlBitmapText extends SimpleApplication {

    // A right to left text.
    private String text = " check check check linelenght all for  possible .text left to right test a is This";
    String text2 = "to right test a is This text left.";
    BitmapFont fnt;


    public static void main(String[] args) {
        TestRtlBitmapText app = new TestRtlBitmapText();
        app.start();
    }
    BitmapText txt;
    @Override
    public void simpleInitApp() {
        float H = 500;
        float X = 400;
        getStateManager().detach(stateManager.getState(StatsAppState.class));
        fnt = assetManager.loadFont("Interface/Fonts/Default.fnt");

        // A right to left BitmapText

        txt = new BitmapText(fnt, true);
        txt.setBox(new Rectangle(0, 0, 150, 0));
        txt.setLineWrapMode(LineWrapMode.Character);
        txt.setAlignment(BitmapFont.Align.Right);
        txt.setText(text);

        txt.setLocalTranslation(X, H, 0);
        txt.setColor(new ColorRGBA(ColorRGBA.Blue));
        guiNode.attachChild(txt);

        BitmapText txt2 = new BitmapText(fnt);
        txt2.setBox(new Rectangle(0, 0, 150, 0));
        txt2.setLineWrapMode(LineWrapMode.Word);
        txt2.setAlignment(BitmapFont.Align.Left);
    //    txt2.setVerticalAlignment(BitmapFont.VAlign.Top);

        txt2.setText(text2);
        txt2.setLocalTranslation(X,H + 200, 0);

        guiNode.attachChild(txt2);


    }
    int i =35;
    float d = 0;
    @Override
    public void simpleUpdate(float tpf) {
 //       BitmapCharacter c = fnt.getCharSet().getCharacter(i);
 //       c.getKerning(i+1);
 //       i++;
  //      txt.setLocalTranslation(d ,300,0);
  //      d += + 1.01f*tpf;
   //     System.out.println(cam.getHeight() / 2);
    }
}

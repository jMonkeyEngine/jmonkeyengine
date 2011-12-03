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

package jme3test.gui;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.font.LineWrapMode;
import com.jme3.font.Rectangle;
import com.jme3.input.KeyInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.event.*;

public class TestBitmapFont extends SimpleApplication {

    private String txtB =
    "ABCDEFGHIKLMNOPQRSTUVWXYZ1234567 890`~!@#$%^&*()-=_+[]\\;',./{}|:<>?";
    
    private BitmapText txt;
    private BitmapText txt2;
    private BitmapText txt3;

    public static void main(String[] args){
        TestBitmapFont app = new TestBitmapFont();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        inputManager.addMapping("WordWrap", new KeyTrigger(KeyInput.KEY_TAB));
        inputManager.addListener(keyListener, "WordWrap");
        inputManager.addRawInputListener(textListener);
        
        BitmapFont fnt = assetManager.loadFont("Interface/Fonts/Default.fnt");
        txt = new BitmapText(fnt, false);
        txt.setBox(new Rectangle(0, 0, settings.getWidth(), settings.getHeight()));
        txt.setSize(fnt.getPreferredSize() * 2f);
        txt.setText(txtB);
        txt.setLocalTranslation(0, txt.getHeight(), 0);
        guiNode.attachChild(txt);

        txt2 = new BitmapText(fnt, false);
        txt2.setSize(fnt.getPreferredSize() * 1.2f);
        txt2.setText("Text without restriction. \nText without restriction. Text without restriction. Text without restriction");
        txt2.setLocalTranslation(0, txt2.getHeight(), 0);
        guiNode.attachChild(txt2);
        
        txt3 = new BitmapText(fnt, false);
        txt3.setBox(new Rectangle(0, 0, settings.getWidth(), 0));
        txt3.setText("Press Tab to toggle word-wrap. type text and enter to input text");
        txt3.setLocalTranslation(0, settings.getHeight()/2, 0);
        guiNode.attachChild(txt3);
    }
    
    private ActionListener keyListener = new ActionListener() {
        @Override
        public void onAction(String name, boolean isPressed, float tpf) {
            if (name.equals("WordWrap") && !isPressed) {
                txt.setLineWrapMode( txt.getLineWrapMode() == LineWrapMode.Word ?
                                        LineWrapMode.NoWrap : LineWrapMode.Word );
            }            
        }
    };
    
    private RawInputListener textListener = new RawInputListener() {
        private StringBuilder str = new StringBuilder();
        
        @Override
        public void onMouseMotionEvent(MouseMotionEvent evt) { } 
        
        @Override
        public void onMouseButtonEvent(MouseButtonEvent evt) { } 
        
        @Override
        public void onKeyEvent(KeyInputEvent evt) {
            if (evt.isReleased())
                return;
            if (evt.getKeyChar() == '\n' || evt.getKeyChar() == '\r') {
                txt3.setText(str.toString());
                str.setLength(0);
            } else {
                str.append(evt.getKeyChar());
            }
        }
        
        @Override
        public void onJoyButtonEvent(JoyButtonEvent evt) { }
        
        @Override
        public void onJoyAxisEvent(JoyAxisEvent evt) { }
        
        @Override
        public void endInput() { }
        
        @Override
        public void beginInput() { }

        @Override
        public void onTouchEvent(TouchEvent evt) { }
    };

}

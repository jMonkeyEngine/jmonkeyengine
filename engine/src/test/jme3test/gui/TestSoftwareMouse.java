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
import com.jme3.input.RawInputListener;
import com.jme3.input.event.*;
import com.jme3.math.FastMath;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;

public class TestSoftwareMouse extends SimpleApplication {

    private Picture cursor;

    private RawInputListener inputListener = new RawInputListener() {

        private float x = 0, y = 0;

        public void beginInput() {
        }
        public void endInput() {
        }
        public void onJoyAxisEvent(JoyAxisEvent evt) {
        }
        public void onJoyButtonEvent(JoyButtonEvent evt) {
        }
        public void onMouseMotionEvent(MouseMotionEvent evt) {
            x += evt.getDX();
            y += evt.getDY();

            // Prevent mouse from leaving screen
            AppSettings settings = TestSoftwareMouse.this.settings;
            x = FastMath.clamp(x, 0, settings.getWidth());
            y = FastMath.clamp(y, 0, settings.getHeight());

            // adjust for hotspot
            cursor.setPosition(x, y - 64);
        }
        public void onMouseButtonEvent(MouseButtonEvent evt) {
        }
        public void onKeyEvent(KeyInputEvent evt) {
        }
        public void onTouchEvent(TouchEvent evt) {             
        }
    };

    public static void main(String[] args){
        TestSoftwareMouse app = new TestSoftwareMouse();

//        AppSettings settings = new AppSettings(true);
//        settings.setFrameRate(60);
//        app.setSettings(settings);

        app.start();
    }

    @Override
    public void simpleInitApp() {
        flyCam.setEnabled(false);
//        inputManager.setCursorVisible(false);

        Texture tex = assetManager.loadTexture("Interface/Logo/Cursor.png");
        
        cursor = new Picture("cursor");
        cursor.setTexture(assetManager, (Texture2D) tex, true);
        cursor.setWidth(64);
        cursor.setHeight(64);
        guiNode.attachChild(cursor);

        inputManager.addRawInputListener(inputListener);

//        Image img = tex.getImage();
//        ByteBuffer data = img.getData(0);
//        IntBuffer image = BufferUtils.createIntBuffer(64 * 64);
//        for (int y = 0; y < 64; y++){
//            for (int x = 0; x < 64; x++){
//                int rgba = data.getInt();
//                image.put(rgba);
//            }
//        }
//        image.clear();
//
//        try {
//            Cursor cur = new Cursor(64, 64, 2, 62, 1, image, null);
//            Mouse.setNativeCursor(cur);
//        } catch (LWJGLException ex) {
//            Logger.getLogger(TestSoftwareMouse.class.getName()).log(Level.SEVERE, null, ex);
//        }
    }
}

/*
 * Copyright (c) 2009-2026 jMonkeyEngine
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

import java.util.ArrayList;
import java.util.Arrays;

import com.jme3.app.SimpleApplication;
import com.jme3.cursors.plugins.CursorConverter;
import com.jme3.cursors.plugins.JmeCursor;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;

/**
 * This test class demonstrate how to change cursor in jME3.
 * 
 * The different methods of {@link com.jme3.cursors.plugins.CursorConverter} will be tested.
 * Also, note that {@link com.jme3.cursors.plugins.CursorConverter} will be configured
 * to match LWJGL cursor requirements.
 *
 * Cursor test
 * @author MadJack, mondogo24
 */
public class TestCursor extends SimpleApplication {

    final private ArrayList<JmeCursor> cursors = new ArrayList<>();
    private long sysTime;
    private int count = 0;

    public static void main(String[] args){
        TestCursor app = new TestCursor();

        app.setShowSettings(false);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        flyCam.setEnabled(false);
        // We need the cursor to be visible. If it is not visible the cursor
        // will still be "used" and loaded, you just won't see it on the screen.
        inputManager.setCursorVisible(true);

        // Note that vertical flip might be needed depending of the original
        // image's format and the consumer of the resulting {@link com.jme3.cursors.plugins.JmeCursor}.
        CursorConverter cursorConverter = new CursorConverter(
            CursorConverter.CursorFormat.ARGB,
            true
        );

        /*
         * To make jME3 use a custom cursor it is as simple as putting the
         * image file in an asset directory. Here we use
         * "Textures/GUI/Cursors".
         *
         * For the purpose of this demonstration we load 3 different cursors and
         * switch cursor every 5 seconds, applying differents configurations with
         * {@link com.jme3.cursors.plugins.CursorConverter}.
         *
         * At date of 2026/06/01:
         * 
         * The nyan cat cursor has been made by Sirea. Under the Attribution Required (CC by) license:
         * http://www.rw-designer.com/icon-set/nyan-cat
         *
         * The meme face cursor has been made by Virum64. Released to Public Domain.
         * http://www.rw-designer.com/cursor-set/memes-faces-v64
         *
         * The animated monkey cursor has been made by Pointer Adic. Released to Public Domain:
         * http://www.rw-designer.com/cursor-set/monkey
         * 
         * The three cursor examples have been converted to png format.
         * Checking and following the license restrictions in the process.
         */

        Image memeImage = ((Image) assetManager.loadAsset("Textures/Cursors/meme.png"));
        Texture2D memeTexture = new Texture2D(memeImage);

        Image nyancatImage = ((Image) assetManager.loadAsset("Textures/Cursors/nyancat.png"));
        Texture2D nyancatTexture = new Texture2D(nyancatImage);

        //Shows a cursor with default hot spot.
        cursors.add(cursorConverter.fromTexture(memeTexture));

        //Shows a cursor with custom hot spot.
        cursors.add(cursorConverter.fromTexture(nyancatTexture, 10, 10));

        /*
         * For animated cursors. Each frame must be loaded.
         * 
         * Differents configuration with the monkey animated cursor will be
         * showed by changing the frame delays.
         */
        String[] monkeyFramePaths = {
            "Textures/Cursors/monkey/frame_0001.png",
            "Textures/Cursors/monkey/frame_0002.png",
            "Textures/Cursors/monkey/frame_0003.png",
            "Textures/Cursors/monkey/frame_0004.png",
            "Textures/Cursors/monkey/frame_0005.png",
            "Textures/Cursors/monkey/frame_0006.png"
        };

        Texture2D[] monkeyFrames = Arrays.stream(monkeyFramePaths)
          .map(framePath -> ((Image) assetManager.loadAsset(framePath)))
          .map(frameImage -> new Texture2D(frameImage))
          .toArray(Texture2D[]::new);
        
        //Shows monkey cursor with same frame delay for each frame and default hot spot.
        cursors.add(cursorConverter.fromTextureFrames(monkeyFrames, 60));
        
        //Shows monkey cursor with custom frame delay for each frame and default hot spot.
        cursors.add(cursorConverter.fromTextureFrames(monkeyFrames, new int[]{40, 40, 40, 200, 200, 200}));

        //Shows monkey cursor with same frame delay for each frame and custom hot spot.
        cursors.add(cursorConverter.fromTextureFrames(monkeyFrames, 150, 5, 7));

        //Shows monkey cursor with custom frame delay for each frame and hot spot.
        cursors.add(cursorConverter.fromTextureFrames(monkeyFrames, new int[]{40, 200, 40, 200, 40, 200}, 10, 20));

        sysTime = System.currentTimeMillis();
        inputManager.setMouseCursor(cursors.get(count));
    }

    @Override
    public void simpleUpdate(float tpf) {
        long currentTime = System.currentTimeMillis();

        if (currentTime - sysTime > 5000) {
            count++;
            if (count >= cursors.size()) {
                count = 0;
            }
            sysTime = currentTime;
            // Enough time have passed,
            // tell jME3 to switch to a different cursor.
            inputManager.setMouseCursor(cursors.get(count));
        }

    }
}


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

package jme3test.asset;

import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.audio.AudioData;
import com.jme3.audio.plugins.WAVLoader;
import com.jme3.texture.Texture;
import com.jme3.texture.plugins.AWTLoader;

public class TestAbsoluteLocators {
    public static void main(String[] args){
        AssetManager am = new DesktopAssetManager();

        am.registerLoader(AWTLoader.class.getName(), "jpg");
        am.registerLoader(WAVLoader.class.getName(), "wav");

        // register absolute locator
        am.registerLocator("/",  ClasspathLocator.class.getName());

        // find a sound
        AudioData audio = am.loadAudio("Sound/Effects/Gun.wav");

        // find a texture
        Texture tex = am.loadTexture("Textures/Terrain/Pond/Pond.jpg");

        if (audio == null)
            throw new RuntimeException("Cannot find audio!");
        else
            System.out.println("Audio loaded from Sounds/Effects/Gun.wav");

        if (tex == null)
            throw new RuntimeException("Cannot find texture!");
        else
            System.out.println("Texture loaded from Textures/Terrain/Pond/Pond.jpg");

        System.out.println("Success!");
    }
}

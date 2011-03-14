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

package jme3test.audio;

import com.jme3.audio.AudioRenderer;
import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.audio.Listener;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeSystem;

public class AudioApp {

    private static final float UPDATE_RATE = 0.01f;

    protected AssetManager manager;
    protected Listener listener;
    protected AudioRenderer ar;

    public AudioApp(){
        AppSettings settings = new AppSettings(true);
        settings.setRenderer(null); // force dummy renderer (?)
        settings.setAudioRenderer(AppSettings.LWJGL_OPENAL);
        ar = JmeSystem.newAudioRenderer(settings);
        ar.initialize();
        manager = new DesktopAssetManager(true);

        listener = new Listener();
        ar.setListener(listener);
    }

    public void initAudioApp(){
    }

    public void updateAudioApp(float tpf){
    }

    public void start(){
        initAudioApp();

        while (true){
            updateAudioApp(UPDATE_RATE);
            ar.update(UPDATE_RATE);

            try{
                Thread.sleep((int) (UPDATE_RATE * 1000f));
            }catch (InterruptedException ex){
                ex.printStackTrace();
            }
        }
    }
}

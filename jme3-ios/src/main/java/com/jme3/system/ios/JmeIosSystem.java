/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
package com.jme3.system.ios;

import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.system.JmeSystemDelegate;
import com.jme3.system.NullContext;
import com.jme3.util.functional.VoidFunction;
import com.jme3.audio.AudioRenderer;
import com.jme3.audio.ios.IosAL;
import com.jme3.audio.ios.IosALC;
//import com.jme3.audio.ios.IosEFX;
import com.jme3.audio.openal.AL;
import com.jme3.audio.openal.ALAudioRenderer;
import com.jme3.audio.openal.ALC;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

/**
 *
 * @author normenhansen
 */
public class JmeIosSystem extends JmeSystemDelegate {

    public JmeIosSystem() {
        setErrorMessageHandler((message) -> {
            showDialog(message);
            System.err.println("JME APPLICATION ERROR:" + message);
        });       
    }

    @Override
    public URL getPlatformAssetConfigURL() {
        return Thread.currentThread().getContextClassLoader().getResource("com/jme3/asset/IOS.cfg");
    }
    
    @Override
    public void writeImageFile(OutputStream outStream, String format, ByteBuffer imageData, int width, int height) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

   

    private native void showDialog(String message);

    

    @Override
    public JmeContext newContext(AppSettings settings, JmeContext.Type contextType) {
        initialize(settings);
        JmeContext ctx = null;
        if (settings.getRenderer() == null
                || settings.getRenderer().equals("NULL")
                || contextType == JmeContext.Type.Headless) {
            ctx = new NullContext();
            ctx.setSettings(settings);
        } else {
            ctx = new IGLESContext();
            ctx.setSettings(settings);
        }
        return ctx;
    }

    @Override
    public AudioRenderer newAudioRenderer(AppSettings settings) {
        ALC alc = new IosALC();
        AL al = new IosAL();
        //EFX efx = new IosEFX();
        return new ALAudioRenderer(al, alc, null);
     }

    @Override
    public void initialize(AppSettings settings) {
        Logger.getLogger("").addHandler(new IosLogHandler());
//                throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void showSoftKeyboard(boolean show) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
/*
 * Copyright (c) 2009-2024 jMonkeyEngine
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

import com.jme3.audio.AudioRenderer;
import com.jme3.audio.ios.IosAL;
import com.jme3.audio.ios.IosALC;
import com.jme3.audio.ios.IosEFX;
import com.jme3.audio.openal.ALAudioRenderer;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.system.JmeSystemDelegate;
import com.jme3.system.NullContext;
import com.jme3.system.Platform;
import com.jme3.util.res.Resources;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.logging.Logger;
import org.ngengine.libjglios.core.LibJGLIOSDeviceBridge;
import org.ngengine.libjglios.core.LibJGLIOSKeyboardBridge;
import org.ngengine.libjglios.core.LibJGLIOSLifecycleBridge;

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
        return Resources.getResource("com/jme3/asset/IOS.cfg");
    }
    
    @Override
    public void writeImageFile(OutputStream outStream, String format, ByteBuffer imageData, int width, int height) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

   

    private void showDialog(String message) {
        System.err.println(message);
        try {
            LibJGLIOSLifecycleBridge.showError("jME iOS Error", message);
        } catch (UnsatisfiedLinkError | NoClassDefFoundError | RuntimeException ignored) {
            // Keep stderr as the fallback when the native iOS launcher is not active.
        }
    }

    

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
        return new ALAudioRenderer(new IosAL(), new IosALC(), new IosEFX());
     }

    @Override
    public void initialize(AppSettings settings) {
        Logger.getLogger("").addHandler(new IosLogHandler());
//                throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Platform getPlatform() {
        String arch = System.getProperty("os.arch").toLowerCase();
        if (arch.contains("arm") || arch.contains("aarch")) {
            return Platform.iOS_ARM;
        } else if (arch.contains("x86_64") || arch.contains("amd64")) {
            return Platform.iOS_X86;
        } else {
            throw new UnsupportedOperationException("Unsupported iOS architecture: " + arch);
        }
    }

    @Override
    public void showSoftKeyboard(boolean show) {
        try {
            LibJGLIOSKeyboardBridge.setSoftwareKeyboardVisible(show);
        } catch (UnsatisfiedLinkError | NoClassDefFoundError ignored) {
            // Desktop/unit-test runs may load this delegate without the iOS launcher.
        }
    }

    @Override
    public boolean isDeviceRumbleSupported() {
        try {
            return LibJGLIOSDeviceBridge.isRumbleSupported();
        } catch (UnsatisfiedLinkError | NoClassDefFoundError ignored) {
            return false;
        }
    }

    @Override
    public void rumble(float amountHigh, float amountLow, float duration) {
        if (duration <= 0f) {
            stopRumble();
            return;
        }
        try {
            LibJGLIOSDeviceBridge.rumble(amountHigh, amountLow, duration);
        } catch (UnsatisfiedLinkError | NoClassDefFoundError ignored) {
            // Desktop/unit-test runs may load this delegate without the iOS launcher.
        }
    }

    @Override
    public void stopRumble() {
        try {
            LibJGLIOSDeviceBridge.stopRumble();
        } catch (UnsatisfiedLinkError | NoClassDefFoundError ignored) {
            // Desktop/unit-test runs may load this delegate without the iOS launcher.
        }
    }
}

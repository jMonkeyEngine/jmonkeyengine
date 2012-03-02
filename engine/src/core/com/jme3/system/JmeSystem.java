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
package com.jme3.system;

import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioRenderer;
import com.jme3.input.SoftTextDialogInput;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JmeSystem {

    private static JmeSystemDelegate systemDelegate;

    public static void setSystemDelegate(JmeSystemDelegate systemDelegate) {
        JmeSystem.systemDelegate = systemDelegate;
    }
    
    public static synchronized File getStorageFolder() {
        checkDelegate();
        return systemDelegate.getStorageFolder();
    }

    public static String getFullName() {
        checkDelegate();
        return systemDelegate.getFullName();
    }

    public static InputStream getResourceAsStream(String name) {
        checkDelegate();
        return systemDelegate.getResourceAsStream(name);
    }

    public static URL getResource(String name) {
        checkDelegate();
        return systemDelegate.getResource(name);
    }

    public static boolean trackDirectMemory() {
        checkDelegate();
        return systemDelegate.trackDirectMemory();
    }

    public static void setLowPermissions(boolean lowPerm) {
        checkDelegate();
        systemDelegate.setLowPermissions(lowPerm);
    }

    public static boolean isLowPermissions() {
        checkDelegate();
        return systemDelegate.isLowPermissions();
    }

    public static void setSoftTextDialogInput(SoftTextDialogInput input) {
        checkDelegate();
        systemDelegate.setSoftTextDialogInput(input);
    }

    public static SoftTextDialogInput getSoftTextDialogInput() {
        checkDelegate();
        return systemDelegate.getSoftTextDialogInput();
    }

    public static AssetManager newAssetManager(URL configFile) {
        checkDelegate();
        return systemDelegate.newAssetManager(configFile);
    }

    public static AssetManager newAssetManager() {
        checkDelegate();
        return systemDelegate.newAssetManager();
    }

    public static boolean showSettingsDialog(AppSettings sourceSettings, final boolean loadFromRegistry) {
        checkDelegate();
        return systemDelegate.showSettingsDialog(sourceSettings, loadFromRegistry);
    }

    public static Platform getPlatform() {
        checkDelegate();
        return systemDelegate.getPlatform();
    }

    public static JmeContext newContext(AppSettings settings, JmeContext.Type contextType) {
        checkDelegate();
        return systemDelegate.newContext(settings, contextType);
    }

    public static AudioRenderer newAudioRenderer(AppSettings settings) {
        checkDelegate();
        return systemDelegate.newAudioRenderer(settings);
    }

    public static void initialize(AppSettings settings) {
        checkDelegate();
        systemDelegate.initialize(settings);
    }

    @SuppressWarnings("unchecked")
    private static void checkDelegate() {
        if (systemDelegate == null) {
            Class<JmeSystemDelegate> systemDelegateClass;
            try {
                systemDelegateClass = (Class<JmeSystemDelegate>) Class.forName("com.jme3.system.JmeDesktopSystem");
                systemDelegate = systemDelegateClass.newInstance();
            } catch (InstantiationException ex) {
                Logger.getLogger(JmeSystem.class.getName()).log(Level.SEVERE, "No JmeSystemDelegate specified, cannot instantiate default JmeDesktopSystem:\n{0}", ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(JmeSystem.class.getName()).log(Level.SEVERE, "No JmeSystemDelegate specified, cannot instantiate default JmeDesktopSystem:\n{0}", ex);
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(JmeSystem.class.getName()).log(Level.SEVERE, "No JmeSystemDelegate specified, cannot instantiate default JmeDesktopSystem:\n{0}", ex);
            }
        }
    }
}

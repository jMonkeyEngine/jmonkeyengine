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
package com.jme3.system;

import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioRenderer;
import com.jme3.input.SoftTextDialogInput;
import com.jme3.texture.Image;
import com.jme3.texture.image.DefaultImageRaster;
import com.jme3.texture.image.ImageRaster;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JmeSystem {

    private static final Logger logger = Logger.getLogger(JmeSystem.class.getName());
    public static enum StorageFolderType {
        Internal,
        External,
    }

    private static JmeSystemDelegate systemDelegate;

    public static void setSystemDelegate(JmeSystemDelegate systemDelegate) {
        JmeSystem.systemDelegate = systemDelegate;
    }

    public static synchronized File getStorageFolder() {
        return getStorageFolder(StorageFolderType.External);
    }

    public static synchronized File getStorageFolder(StorageFolderType type) {
        checkDelegate();
        return systemDelegate.getStorageFolder(type);
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

    /**
     * Displays or hides the onscreen soft keyboard
     * @param show If true, the keyboard is displayed, if false, the screen is hidden.
     */
    public static void showSoftKeyboard(boolean show) {
        checkDelegate();
        systemDelegate.showSoftKeyboard(show);
    }

    public static SoftTextDialogInput getSoftTextDialogInput() {
        checkDelegate();
        return systemDelegate.getSoftTextDialogInput();
    }

    /**
     * Compresses a raw image into a stream.
     * 
     * The encoding is performed via system libraries. On desktop, the encoding
     * is performed via ImageIO, whereas on Android, is is done via the 
     * Bitmap class.
     * 
     * @param outStream The stream where to write the image data.
     * @param format The format to use, either "png" or "jpg".
     * @param imageData The image data in {@link Image.Format#RGBA8} format.
     * @param width The width of the image.
     * @param height The height of the image.
     * @throws IOException If outStream throws an exception while writing.
     */
    public static void writeImageFile(OutputStream outStream, String format, ByteBuffer imageData, int width, int height) throws IOException {
        checkDelegate();
        systemDelegate.writeImageFile(outStream, format, imageData, width, height);
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

    public static URL getPlatformAssetConfigURL() {
        checkDelegate();
        return systemDelegate.getPlatformAssetConfigURL();
    }
    
    /**
     * Displays an error message to the user in whichever way the context
     * feels is appropriate. If this is a headless or an offscreen surface
     * context, this method should do nothing.
     *
     * @param message The error message to display. May contain new line
     * characters.
     */
    public static void showErrorDialog(String message){
        checkDelegate();
        systemDelegate.showErrorDialog(message);
    }

    public static void initialize(AppSettings settings) {
        checkDelegate();
        systemDelegate.initialize(settings);
    }

    private static JmeSystemDelegate tryLoadDelegate(String className) throws InstantiationException, IllegalAccessException {
        try {
            return (JmeSystemDelegate) Class.forName(className).newInstance();
        } catch (ClassNotFoundException ex) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static void checkDelegate() {
        if (systemDelegate == null) {
            try {
                systemDelegate = tryLoadDelegate("com.jme3.system.JmeDesktopSystem");
                if (systemDelegate == null) {
                    systemDelegate = tryLoadDelegate("com.jme3.system.android.JmeAndroidSystem");
                    if (systemDelegate == null) {
                        systemDelegate = tryLoadDelegate("com.jme3.system.ios.JmeIosSystem");
                        if (systemDelegate == null) {
                            // None of the system delegates were found ..
                            Logger.getLogger(JmeSystem.class.getName()).log(Level.SEVERE,
                                    "Failed to find a JmeSystem delegate!\n"
                                    + "Ensure either desktop or android jME3 jar is in the classpath.");
                        }
                    }
                }
            } catch (InstantiationException ex) {
                Logger.getLogger(JmeSystem.class.getName()).log(Level.SEVERE, "Failed to create JmeSystem delegate:\n{0}", ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(JmeSystem.class.getName()).log(Level.SEVERE, "Failed to create JmeSystem delegate:\n{0}", ex);
            }
        }
    }
}

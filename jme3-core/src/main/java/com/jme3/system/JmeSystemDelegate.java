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
package com.jme3.system;

import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.audio.AudioRenderer;
import com.jme3.input.SoftTextDialogInput;
import com.jme3.util.res.Resources;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Kirill Vainer, normenhansen
 */
public abstract class JmeSystemDelegate {

    protected final Logger logger = Logger.getLogger(JmeSystem.class.getName());
    protected boolean initialized = false;
    protected boolean lowPermissions = false;
    protected Map<JmeSystem.StorageFolderType, File> storageFolders = new EnumMap<>(JmeSystem.StorageFolderType.class);
    protected SoftTextDialogInput softTextDialogInput = null;

    protected Consumer<String> errorMessageHandler = (message) -> {
        JmeDialogsFactory dialogFactory = null;
        try {
             dialogFactory = (JmeDialogsFactory)Class.forName("com.jme3.system.JmeDialogsFactoryImpl").getConstructor().newInstance();
        } catch(ClassNotFoundException e){
            logger.warning("JmeDialogsFactory implementation not found.");    
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
        }
        if(dialogFactory != null) dialogFactory.showErrorDialog(message);
        else System.err.println(message);
    };

    protected BiFunction<AppSettings,Boolean,Boolean> settingsHandler = (settings,loadFromRegistry) -> {
        JmeDialogsFactory dialogFactory = null;
        try {
            dialogFactory = (JmeDialogsFactory)Class.forName("com.jme3.system.JmeDialogsFactoryImpl").getConstructor().newInstance();
        } catch(ClassNotFoundException e){
            logger.warning("JmeDialogsFactory implementation not found.");    
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
        }
        if(dialogFactory != null) return dialogFactory.showSettingsDialog(settings, loadFromRegistry);
        return true;
    };

    public synchronized File getStorageFolder(JmeSystem.StorageFolderType type) {
        File storageFolder = null;

        switch (type) {
            // Internal and External are currently the same folder
            case Internal:
            case External:
                if (lowPermissions) {
                    throw new UnsupportedOperationException("File system access restricted");
                }
                storageFolder = storageFolders.get(type);
                if (storageFolder == null) {
                    // Initialize storage folder
                    storageFolder = new File(System.getProperty("user.home"), ".jme3");
                    if (!storageFolder.exists()) {
                        storageFolder.mkdir();
                    }
                    storageFolders.put(type, storageFolder);
                }
                break;
            default:
                break;
        }
        if (storageFolder != null) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Storage Folder Path: {0}", storageFolder.getAbsolutePath());
            }
        } else {
            logger.log(Level.FINE, "Storage Folder not found!");
        }
        return storageFolder;
    }

    public String getFullName() {
        return JmeVersion.FULL_NAME;
    }

    public InputStream getResourceAsStream(String name) {
        return Resources.getResourceAsStream(name,this.getClass());
    }

    public URL getResource(String name) {
        return Resources.getResource(name,this.getClass());
    }

    public boolean trackDirectMemory() {
        return false;
    }

    public void setLowPermissions(boolean lowPerm) {
        lowPermissions = lowPerm;
    }

    public boolean isLowPermissions() {
        return lowPermissions;
    }

    public void setSoftTextDialogInput(SoftTextDialogInput input) {
        softTextDialogInput = input;
    }
    
    public SoftTextDialogInput getSoftTextDialogInput() {
        return softTextDialogInput;
    }

    public final AssetManager newAssetManager(URL configFile) {
        return new DesktopAssetManager(configFile);
    }

    public final AssetManager newAssetManager() {
        return new DesktopAssetManager(null);
    }
    
    public abstract void writeImageFile(OutputStream outStream, String format, ByteBuffer imageData, int width, int height) throws IOException;

    /**
     * Set function to handle errors. 
     * The default implementation show a dialog if available.
     * @param handler Consumer to which the error is passed as String
     */
    public void setErrorMessageHandler(Consumer<String> handler){
        errorMessageHandler = handler;
    }

    /**
     * Internal use only: submit an error to the error message handler
     */
    public void handleErrorMessage(String message){
        if(errorMessageHandler != null) errorMessageHandler.accept(message);
    }

    /**
     * Set a function to handler app settings. 
     * The default implementation shows a settings dialog if available.
     * @param handler handler function that accepts as argument an instance of AppSettings 
     * to transform and a boolean with the value of true if the settings are expected to be loaded from 
     * the user registry. The handler function returns false if the configuration is interrupted (eg.the the dialog was closed)
     * or true otherwise.
     */
    public void setSettingsHandler(BiFunction<AppSettings,Boolean, Boolean> handler){
        settingsHandler = handler;
    }

    /**
     * Internal use only: summon the settings handler
     */
    public boolean handleSettings(AppSettings settings, boolean loadFromRegistry){
        if(settingsHandler != null) return settingsHandler.apply(settings,loadFromRegistry);
        return true;
    }

    /**
     * @deprecated Use JmeSystemDelegate.handleErrorMessage(String) instead
     * @param message
     */
    @Deprecated
    public void showErrorDialog(String message){
        handleErrorMessage(message);
    }

    @Deprecated
    public boolean showSettingsDialog(AppSettings settings, boolean loadFromRegistry){
        return handleSettings(settings, loadFromRegistry);
    }


    private boolean is64Bit(String arch) {
        switch (arch) {
            case "amd64":
            case "x86_64":
            case "aarch64":
            case "arm64":
            case "ppc64":
            case "universal":
                return true;
            case "x86":
            case "i386":
            case "i686":
            case "aarch32":
            case "arm":
            case "armv7":
            case "armv7l":
                return false;
            default:
                throw new UnsupportedOperationException("Unsupported architecture: " + arch);
        }
    }

    private boolean isArmArchitecture(String arch) {
        return arch.startsWith("arm") || arch.startsWith("aarch");
    }

    private boolean isX86Architecture(String arch) {
        return arch.equals("x86")
                || arch.equals("amd64")
                || arch.equals("x86_64")
                || arch.equals("i386")
                || arch.equals("i686")
                || arch.equals("universal");
    }

    private UnsupportedOperationException unsupported32Bit(String osName) {
        return new UnsupportedOperationException("32-bit " + osName + " is not supported.");
    }

    private Platform getWindowsPlatform(String arch, boolean is64) {
        if (!is64) {
            // no 32-bit version
            throw unsupported32Bit("Windows");
        }
        if (isArmArchitecture(arch)) return Platform.Windows_ARM64;
        if (isX86Architecture(arch)) return Platform.Windows64;
        throw new UnsupportedOperationException("Unsupported architecture: " + arch);
    }

    private Platform getLinuxPlatform(String arch, boolean is64) {
        if (!is64) {
            // no 32-bit version
            throw unsupported32Bit("Linux");
        }
        if (isArmArchitecture(arch)) return Platform.Linux_ARM64;
        if (isX86Architecture(arch)) return Platform.Linux64;
        throw new UnsupportedOperationException("Unsupported architecture: " + arch);
    }

    private Platform getMacPlatform(String arch, boolean is64) {
        if (!is64) {
            // no 32-bit version
            throw unsupported32Bit("macOS");
        }
        if (isArmArchitecture(arch)) return Platform.MacOSX_ARM64;
        if (isX86Architecture(arch)) return Platform.MacOSX64;
        throw new UnsupportedOperationException("Unsupported architecture: " + arch);
    }

    public Platform getPlatform() {
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();
        boolean is64 = is64Bit(arch);
        if (os.contains("windows")) {
            return getWindowsPlatform(arch, is64);
        } else if (os.contains("linux") || os.contains("freebsd") 
                || os.contains("sunos") || os.contains("unix")) {
            return getLinuxPlatform(arch, is64);
        } else if (os.contains("mac os x") || os.contains("darwin")) {
            return getMacPlatform(arch, is64);
        } else {
            throw new UnsupportedOperationException("The specified platform: " + os + " is not supported.");
        }
    }

    public String getBuildInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Running on ").append(getFullName()).append("\n");
        sb.append(" * Branch: ").append(JmeVersion.BRANCH_NAME).append("\n");
        sb.append(" * Git Hash: ").append(JmeVersion.GIT_SHORT_HASH).append("\n");
        sb.append(" * Build Date: ").append(JmeVersion.BUILD_DATE);
        return sb.toString();
    }
    
    public abstract URL getPlatformAssetConfigURL();
    
    public abstract JmeContext newContext(AppSettings settings, JmeContext.Type contextType);

    public abstract AudioRenderer newAudioRenderer(AppSettings settings);

    public abstract void initialize(AppSettings settings);

    public abstract void showSoftKeyboard(boolean show);
}

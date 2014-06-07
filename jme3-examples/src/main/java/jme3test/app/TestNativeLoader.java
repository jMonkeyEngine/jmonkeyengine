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
package jme3test.app;

import com.jme3.system.NativeLibraryLoader;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Try to load some natives.
 * 
 * @author Kirill Vainer
 */
public class TestNativeLoader {
    
    private static final File WORKING_FOLDER = new File(System.getProperty("user.dir"));
    
    private static void tryLoadLwjgl() {
        NativeLibraryLoader.loadNativeLibrary("lwjgl", true);
        System.out.println("Succeeded in loading LWJGL.\n\tVersion: " + 
                           org.lwjgl.Sys.getVersion());
    }
    
    private static void tryLoadJinput() {
        NativeLibraryLoader.loadNativeLibrary("jinput", true);
        NativeLibraryLoader.loadNativeLibrary("jinput-dx8", true);
        
        net.java.games.input.ControllerEnvironment ce =
            net.java.games.input.ControllerEnvironment.getDefaultEnvironment();
        if (ce.isSupported()) {
            net.java.games.input.Controller[] c =
                    ce.getControllers();
            
            System.out.println("Succeeded in loading JInput.\n\tVersion: " + 
                            net.java.games.util.Version.getVersion());
        }
    }
    
    private static void tryLoadOpenAL() {
        NativeLibraryLoader.loadNativeLibrary("openal", true);
        
        try {
            org.lwjgl.openal.AL.create();
            String renderer = org.lwjgl.openal.AL10.alGetString(org.lwjgl.openal.AL10.AL_RENDERER);
            String vendor = org.lwjgl.openal.AL10.alGetString(org.lwjgl.openal.AL10.AL_VENDOR);
            String version = org.lwjgl.openal.AL10.alGetString(org.lwjgl.openal.AL10.AL_VERSION);
            System.out.println("Succeeded in loading OpenAL.");
            System.out.println("\tVersion: " + version);
        } catch (org.lwjgl.LWJGLException ex) {
            throw new RuntimeException(ex);
        } finally {
            if (org.lwjgl.openal.AL.isCreated()) {
                org.lwjgl.openal.AL.destroy();
            }
        }
    }
    
    private static void tryLoadOpenGL() {
        org.lwjgl.opengl.Pbuffer pb = null;
        try {
            pb = new org.lwjgl.opengl.Pbuffer(1, 1, new org.lwjgl.opengl.PixelFormat(0, 0, 0), null);
            pb.makeCurrent();
            String version = org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_VERSION);
            System.out.println("Succeeded in loading OpenGL.\n\tVersion: " + version);
        } catch (org.lwjgl.LWJGLException ex) {
            throw new RuntimeException(ex);
        } finally {
            if (pb != null) {
                pb.destroy();
            }
        }
    }
    
    private static void tryLoadBulletJme() {
        if (NativeLibraryLoader.isUsingNativeBullet()) {
            NativeLibraryLoader.loadNativeLibrary("bulletjme", true);

            com.jme3.bullet.PhysicsSpace physSpace = new com.jme3.bullet.PhysicsSpace();

            System.out.println("Succeeded in loading BulletJme.");
        } else {
            System.out.println("Native bullet not included. Cannot test loading.");
        }
    }
    
    private static void cleanupNativesFolder(File folder) {
        for (File file : folder.listFiles()) {
            String lowerCaseName = file.getName().toLowerCase();
            if (lowerCaseName.contains("lwjgl") ||
                lowerCaseName.contains("jinput") ||
                lowerCaseName.contains("openal") ||
                lowerCaseName.contains("bulletjme")) {
                file.delete();
            }
        }
    }
    
    public static void main(String[] args) {
        Logger.getLogger("").getHandlers()[0].setLevel(Level.WARNING);
        Logger.getLogger(NativeLibraryLoader.class.getName()).setLevel(Level.ALL);
        
        // Get a bit more output from LWJGL about issues.
        // System.setProperty("org.lwjgl.util.Debug", "true");
        
        // Extracting to working folder is no brainer. 
        // Choose some random path, then load LWJGL.
        File customNativesFolder = new File("CustomNativesFolder");
        customNativesFolder.mkdirs();
        
        if (!customNativesFolder.isDirectory()) {
            throw new IllegalStateException("Failed to make custom natives folder");
        }
        
        // Let's cleanup our folders first.
        cleanupNativesFolder(WORKING_FOLDER);
        cleanupNativesFolder(customNativesFolder);
        
        NativeLibraryLoader.setCustomExtractionFolder(customNativesFolder.getAbsolutePath());
        
        tryLoadLwjgl();
        tryLoadOpenGL();
        tryLoadOpenAL();
        tryLoadJinput();
        tryLoadBulletJme();
    }
}

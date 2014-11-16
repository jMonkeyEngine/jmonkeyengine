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

import com.jme3.app.SettingsDialog;
import com.jme3.app.SettingsDialog.SelectionListener;
import com.jme3.asset.AssetManager;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.audio.AudioRenderer;
import com.jme3.audio.openal.AL;
import com.jme3.audio.openal.ALAudioRenderer;
import com.jme3.audio.openal.ALC;
import com.jme3.audio.openal.EFX;
import com.jme3.system.JmeContext.Type;
import com.jme3.texture.Image;
import com.jme3.texture.image.DefaultImageRaster;
import com.jme3.texture.image.ImageRaster;
import com.jme3.util.Screenshots;
import java.awt.EventQueue;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/**
 *
 * @author Kirill Vainer, normenhansen
 */
public class JmeDesktopSystem extends JmeSystemDelegate {

    @Override
    public AssetManager newAssetManager(URL configFile) {
        return new DesktopAssetManager(configFile);
    }
    
    @Override
    public void writeImageFile(OutputStream outStream, String format, ByteBuffer imageData, int width, int height) throws IOException {
        BufferedImage awtImage = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        Screenshots.convertScreenShot(imageData, awtImage);
        ImageIO.write(awtImage, format, outStream);
    }

    @Override
    public ImageRaster createImageRaster(Image image, int slice) {
        assert image.getEfficentData() == null;
        return new DefaultImageRaster(image, slice);
    }

    @Override
    public AssetManager newAssetManager() {
        return new DesktopAssetManager(null);
    }

    @Override
    public void showErrorDialog(String message) {
        final String msg = message;
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                ErrorDialog.showDialog(msg);
            }
        });
    }
    
    @Override
    public boolean showSettingsDialog(AppSettings sourceSettings, final boolean loadFromRegistry) {
        if (SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Cannot run from EDT");
        }

        final AppSettings settings = new AppSettings(false);
        settings.copyFrom(sourceSettings);
        String iconPath = sourceSettings.getSettingsDialogImage();        
        if(iconPath == null){
            iconPath = "";
        }
        final URL iconUrl = JmeSystem.class.getResource(iconPath.startsWith("/") ? iconPath : "/" + iconPath);
        if (iconUrl == null) {
            throw new AssetNotFoundException(sourceSettings.getSettingsDialogImage());
        }

        final AtomicBoolean done = new AtomicBoolean();
        final AtomicInteger result = new AtomicInteger();
        final Object lock = new Object();

        final SelectionListener selectionListener = new SelectionListener() {

            public void onSelection(int selection) {
                synchronized (lock) {
                    done.set(true);
                    result.set(selection);
                    lock.notifyAll();
                }
            }
        };
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                synchronized (lock) {
                    SettingsDialog dialog = new SettingsDialog(settings, iconUrl, loadFromRegistry);
                    dialog.setSelectionListener(selectionListener);
                    dialog.showDialog();
                }
            }
        });

        synchronized (lock) {
            while (!done.get()) {
                try {
                    lock.wait();
                } catch (InterruptedException ex) {
                }
            }
        }

        sourceSettings.copyFrom(settings);

        return result.get() == SettingsDialog.APPROVE_SELECTION;
    }

    private JmeContext newContextLwjgl(AppSettings settings, JmeContext.Type type) {
        try {
            Class<? extends JmeContext> ctxClazz = null;
            switch (type) {
                case Canvas:
                    ctxClazz = (Class<? extends JmeContext>) Class.forName("com.jme3.system.lwjgl.LwjglCanvas");
                    break;
                case Display:
                    ctxClazz = (Class<? extends JmeContext>) Class.forName("com.jme3.system.lwjgl.LwjglDisplay");
                    break;
                case OffscreenSurface:
                    ctxClazz = (Class<? extends JmeContext>) Class.forName("com.jme3.system.lwjgl.LwjglOffscreenBuffer");
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported context type " + type);
            }

            return ctxClazz.newInstance();
        } catch (InstantiationException ex) {
            logger.log(Level.SEVERE, "Failed to create context", ex);
        } catch (IllegalAccessException ex) {
            logger.log(Level.SEVERE, "Failed to create context", ex);
        } catch (ClassNotFoundException ex) {
            logger.log(Level.SEVERE, "CRITICAL ERROR: Context class is missing!\n"
                    + "Make sure jme3_lwjgl-ogl is on the classpath.", ex);
        }

        return null;
    }

    private JmeContext newContextJogl(AppSettings settings, JmeContext.Type type) {
        try {
            Class<? extends JmeContext> ctxClazz = null;
            switch (type) {
                case Display:
                    ctxClazz = (Class<? extends JmeContext>) Class.forName("com.jme3.system.jogl.JoglNewtDisplay");
                    break;
                case Canvas:
                    ctxClazz = (Class<? extends JmeContext>) Class.forName("com.jme3.system.jogl.JoglNewtCanvas");
                    break;
                case OffscreenSurface:
                    ctxClazz = (Class<? extends JmeContext>) Class.forName("com.jme3.system.jogl.JoglOffscreenBuffer");
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported context type " + type);
            }

            return ctxClazz.newInstance();
        } catch (InstantiationException ex) {
            logger.log(Level.SEVERE, "Failed to create context", ex);
        } catch (IllegalAccessException ex) {
            logger.log(Level.SEVERE, "Failed to create context", ex);
        } catch (ClassNotFoundException ex) {
            logger.log(Level.SEVERE, "CRITICAL ERROR: Context class is missing!\n"
                    + "Make sure jme3_jogl is on the classpath.", ex);
        }

        return null;
    }

    private JmeContext newContextCustom(AppSettings settings, JmeContext.Type type) {
        try {
            String className = settings.getRenderer().substring("CUSTOM".length());

            Class<? extends JmeContext> ctxClazz = null;
            ctxClazz = (Class<? extends JmeContext>) Class.forName(className);
            return ctxClazz.newInstance();
        } catch (InstantiationException ex) {
            logger.log(Level.SEVERE, "Failed to create context", ex);
        } catch (IllegalAccessException ex) {
            logger.log(Level.SEVERE, "Failed to create context", ex);
        } catch (ClassNotFoundException ex) {
            logger.log(Level.SEVERE, "CRITICAL ERROR: Context class is missing!", ex);
        }

        return null;
    }

    @Override
    public JmeContext newContext(AppSettings settings, Type contextType) {
        initialize(settings);
        JmeContext ctx;
        if (settings.getRenderer() == null
                || settings.getRenderer().equals("NULL")
                || contextType == JmeContext.Type.Headless) {
            ctx = new NullContext();
            ctx.setSettings(settings);
        } else if (settings.getRenderer().startsWith("LWJGL")) {
            ctx = newContextLwjgl(settings, contextType);
            ctx.setSettings(settings);
        } else if (settings.getRenderer().startsWith("JOGL")) {
            ctx = newContextJogl(settings, contextType);
            ctx.setSettings(settings);
        } else if (settings.getRenderer().startsWith("CUSTOM")) {
            ctx = newContextCustom(settings, contextType);
            ctx.setSettings(settings);
        } else {
            throw new UnsupportedOperationException(
                    "Unrecognizable renderer specified: "
                    + settings.getRenderer());
        }
        return ctx;
    }

    private <T> T newObject(String className) {
        try {
            Class<T> clazz = (Class<T>) Class.forName(className);
            return clazz.newInstance();
        } catch (ClassNotFoundException ex) {
            logger.log(Level.SEVERE, "CRITICAL ERROR: Audio implementation class is missing!\n"
                                   + "Make sure jme3_lwjgl-oal or jm3_joal is on the classpath.", ex);
        } catch (IllegalAccessException ex) {
            logger.log(Level.SEVERE, "Failed to create context", ex);
        } catch (InstantiationException ex) {
            logger.log(Level.SEVERE, "Failed to create context", ex);
        }
        
        return null;
    }
    
    @Override
    public AudioRenderer newAudioRenderer(AppSettings settings) {
        initialize(settings);

        AL al;
        ALC alc;
        EFX efx;
        if (settings.getAudioRenderer().startsWith("LWJGL")) {
            al = newObject("com.jme3.audio.lwjgl.LwjglAL");
            alc = newObject("com.jme3.audio.lwjgl.LwjglALC");
            efx = newObject("com.jme3.audio.lwjgl.LwjglEFX");
        } else if (settings.getAudioRenderer().startsWith("JOAL")) {
            al = newObject("com.jme3.audio.joal.JoalAL");
            alc = newObject("com.jme3.audio.joal.JoalALC");
            efx = newObject("com.jme3.audio.joal.JoalEFX");
        } else {
            throw new UnsupportedOperationException(
                    "Unrecognizable audio renderer specified: "
                    + settings.getAudioRenderer());
        }

        if (al == null || alc == null || efx == null) {
            return null;
        }

        return new ALAudioRenderer(al, alc, efx);
    }

    @Override
    public void initialize(AppSettings settings) {
        if (initialized) {
            return;
        }

        initialized = true;
        try {
            if (!lowPermissions) {
                // can only modify logging settings
                // if permissions are available
//                JmeFormatter formatter = new JmeFormatter();
//                Handler fileHandler = new FileHandler("jme.log");
//                fileHandler.setFormatter(formatter);
//                Logger.getLogger("").addHandler(fileHandler);
//                Handler consoleHandler = new ConsoleHandler();
//                consoleHandler.setFormatter(formatter);
//                Logger.getLogger("").removeHandler(Logger.getLogger("").getHandlers()[0]);
//                Logger.getLogger("").addHandler(consoleHandler);
            }
//        } catch (IOException ex){
//            logger.log(Level.SEVERE, "I/O Error while creating log file", ex);
        } catch (SecurityException ex) {
            logger.log(Level.SEVERE, "Security error in creating log file", ex);
        }
        logger.log(Level.INFO, "Running on {0}", getFullName());
    }
}

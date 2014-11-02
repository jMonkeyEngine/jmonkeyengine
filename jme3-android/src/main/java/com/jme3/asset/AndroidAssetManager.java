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
package com.jme3.asset;

import com.jme3.asset.plugins.AndroidLocator;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.audio.plugins.AndroidAudioLoader;
import com.jme3.audio.plugins.WAVLoader;
import com.jme3.system.AppSettings;
import com.jme3.system.android.JmeAndroidSystem;
import com.jme3.texture.plugins.AndroidBufferImageLoader;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <code>AndroidAssetManager</code> is an implementation of DesktopAssetManager for Android
 *
 * @author larynx
 */
public class AndroidAssetManager extends DesktopAssetManager {

    private static final Logger logger = Logger.getLogger(AndroidAssetManager.class.getName());

    public AndroidAssetManager() {
        this(null);
    }

    @Deprecated
    public AndroidAssetManager(boolean loadDefaults) {
        //this(Thread.currentThread().getContextClassLoader().getResource("com/jme3/asset/Android.cfg"));
        this(null);
    }

    private void registerLoaderSafe(String loaderClass, String ... extensions) {
        try {
            Class<? extends AssetLoader> loader = (Class<? extends AssetLoader>) Class.forName(loaderClass);
            registerLoader(loader, extensions);
        } catch (Exception e){
            logger.log(Level.WARNING, "Failed to load AssetLoader", e);
        }
    }

    /**
     * AndroidAssetManager constructor
     * If URL == null then a default list of locators and loaders for android is set
     * @param configFile
     */
    public AndroidAssetManager(URL configFile) {
        System.setProperty("org.xml.sax.driver", "org.xmlpull.v1.sax2.Driver");

        // Set Default Android config
        registerLocator("", AndroidLocator.class);
        registerLocator("", ClasspathLocator.class);

        registerLoader(AndroidBufferImageLoader.class, "jpg", "bmp", "jpeg");
        registerLoader(AndroidBufferImageLoader.class, "gif", "png");
        
        if (JmeAndroidSystem.getAudioRendererType().equals(AppSettings.ANDROID_MEDIAPLAYER)) {
            registerLoader(AndroidAudioLoader.class, "ogg", "mp3", "wav");
        } else if (JmeAndroidSystem.getAudioRendererType().equals(AppSettings.ANDROID_OPENAL_SOFT)) {
            registerLoader(WAVLoader.class, "wav");
            // TODO jogg is not in core, need to add some other way to get around compile errors, or not.
//            registerLoader(com.jme3.audio.plugins.OGGLoader.class, "ogg");
            registerLoaderSafe("com.jme3.audio.plugins.OGGLoader", "ogg");
        } else {
            throw new IllegalStateException("No Audio Renderer Type defined!");
        }

        registerLoader(com.jme3.material.plugins.J3MLoader.class, "j3m");
        registerLoader(com.jme3.material.plugins.J3MLoader.class, "j3md");
        registerLoader(com.jme3.material.plugins.ShaderNodeDefinitionLoader.class, "j3sn");
        registerLoader(com.jme3.shader.plugins.GLSLLoader.class, "vert", "frag", "glsl", "glsllib");
        registerLoader(com.jme3.export.binary.BinaryImporter.class, "j3o");
        registerLoader(com.jme3.font.plugins.BitmapFontLoader.class, "fnt");

        // Less common loaders (especially on Android)
        registerLoaderSafe("com.jme3.texture.plugins.DDSLoader", "dds");
        registerLoaderSafe("com.jme3.texture.plugins.PFMLoader", "pfm");
        registerLoaderSafe("com.jme3.texture.plugins.HDRLoader", "hdr");
        registerLoaderSafe("com.jme3.texture.plugins.TGALoader", "tga");
        registerLoaderSafe("com.jme3.scene.plugins.OBJLoader", "obj");
        registerLoaderSafe("com.jme3.scene.plugins.MTLLoader", "mtl");
        registerLoaderSafe("com.jme3.scene.plugins.ogre.MeshLoader", "mesh.xml");
        registerLoaderSafe("com.jme3.scene.plugins.ogre.SkeletonLoader", "skeleton.xml");
        registerLoaderSafe("com.jme3.scene.plugins.ogre.MaterialLoader", "material");
        registerLoaderSafe("com.jme3.scene.plugins.ogre.SceneLoader", "scene");


        logger.fine("AndroidAssetManager created.");
    }

}

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
package com.jme3.asset;

import com.jme3.texture.Texture;
import com.jme3.texture.plugins.AndroidImageLoader;
import java.net.URL;
import java.util.logging.Logger;

import com.jme3.asset.plugins.AndroidLocator;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.audio.plugins.AndroidAudioLoader;

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

    /**
     * AndroidAssetManager constructor
     * If URL == null then a default list of locators and loaders for android is set
     * @param configFile
     */
    public AndroidAssetManager(URL configFile) {

        System.setProperty("org.xml.sax.driver", "org.xmlpull.v1.sax2.Driver");


        // Set Default Android config        	       
        this.registerLocator("", AndroidLocator.class);
        this.registerLocator("", ClasspathLocator.class);
        this.registerLoader(AndroidImageLoader.class, "jpg", "bmp", "gif", "png", "jpeg");
        this.registerLoader(AndroidAudioLoader.class, "ogg", "mp3");
        this.registerLoader(com.jme3.material.plugins.J3MLoader.class, "j3m");
        this.registerLoader(com.jme3.material.plugins.J3MLoader.class, "j3md");
        this.registerLoader(com.jme3.font.plugins.BitmapFontLoader.class, "fnt");
        this.registerLoader(com.jme3.texture.plugins.DDSLoader.class, "dds");
        this.registerLoader(com.jme3.texture.plugins.PFMLoader.class, "pfm");
        this.registerLoader(com.jme3.texture.plugins.HDRLoader.class, "hdr");
        this.registerLoader(com.jme3.texture.plugins.TGALoader.class, "tga");
        this.registerLoader(com.jme3.export.binary.BinaryImporter.class, "j3o");
        this.registerLoader(com.jme3.scene.plugins.OBJLoader.class, "obj");
        this.registerLoader(com.jme3.scene.plugins.MTLLoader.class, "mtl");
        this.registerLoader(com.jme3.scene.plugins.ogre.MeshLoader.class, "meshxml", "mesh.xml");
        this.registerLoader(com.jme3.scene.plugins.ogre.SkeletonLoader.class, "skeletonxml", "skeleton.xml");
        this.registerLoader(com.jme3.scene.plugins.ogre.MaterialLoader.class, "material");
        this.registerLoader(com.jme3.scene.plugins.ogre.SceneLoader.class, "scene");
        this.registerLoader(com.jme3.shader.plugins.GLSLLoader.class, "vert", "frag", "glsl", "glsllib");


        logger.info("AndroidAssetManager created.");
    }

    /**
     * Loads a texture. 
     *
     * @return
     */
    @Override
    public Texture loadTexture(TextureKey key) {
        Texture tex = (Texture) loadAsset(key);

        // Needed for Android
        tex.setMagFilter(Texture.MagFilter.Nearest);
        tex.setAnisotropicFilter(0);
        if (tex.getMinFilter().usesMipMapLevels()) {
            tex.setMinFilter(Texture.MinFilter.NearestNearestMipMap);
        } else {
            tex.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        }
        return tex;
    }
}

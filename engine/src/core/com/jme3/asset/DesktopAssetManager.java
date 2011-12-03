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

import com.jme3.asset.AssetCache.SmartAssetInfo;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioKey;
import com.jme3.font.BitmapFont;
import com.jme3.material.Material;
import com.jme3.scene.Spatial;
import com.jme3.shader.Shader;
import com.jme3.shader.ShaderKey;
import com.jme3.texture.Texture;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <code>AssetManager</code> is the primary method for managing and loading
 * assets inside jME.
 *
 * @author Kirill Vainer
 */
public class DesktopAssetManager implements AssetManager {

    private static final Logger logger = Logger.getLogger(AssetManager.class.getName());

    private final AssetCache cache = new AssetCache();
    private final ImplHandler handler = new ImplHandler(this);

    private AssetEventListener eventListener = null;
    private List<ClassLoader> classLoaders;

//    private final ThreadingManager threadingMan = new ThreadingManager(this);
//    private final Set<AssetKey> alreadyLoadingSet = new HashSet<AssetKey>();

    public DesktopAssetManager(){
        this(null);
    }

    @Deprecated
    public DesktopAssetManager(boolean loadDefaults){
        this(Thread.currentThread().getContextClassLoader().getResource("com/jme3/asset/Desktop.cfg"));
    }

    public DesktopAssetManager(URL configFile){
        if (configFile != null){
            InputStream stream = null;
            try{
                AssetConfig cfg = new AssetConfig(this);
                stream = configFile.openStream();
                cfg.loadText(stream);
            }catch (IOException ex){
                logger.log(Level.SEVERE, "Failed to load asset config", ex);
            }finally{
                if (stream != null)
                    try{
                        stream.close();
                    }catch (IOException ex){
                    }
            }
        }
        logger.info("DesktopAssetManager created.");
    }

    public void addClassLoader(ClassLoader loader){
        if(classLoaders == null)
            classLoaders = Collections.synchronizedList(new ArrayList<ClassLoader>());
        synchronized(classLoaders) {
            classLoaders.add(loader);
        }
    }
    
    public void removeClassLoader(ClassLoader loader){
        if(classLoaders != null) synchronized(classLoaders) {
                classLoaders.remove(loader);
            }
    }

    public List<ClassLoader> getClassLoaders(){
        return classLoaders;
    }
    
    public void setAssetEventListener(AssetEventListener listener){
        eventListener = listener;
    }

    public void registerLoader(Class<? extends AssetLoader> loader, String ... extensions){
        handler.addLoader(loader, extensions);
        if (logger.isLoggable(Level.FINER)){
            logger.log(Level.FINER, "Registered loader: {0} for extensions {1}",
              new Object[]{loader.getSimpleName(), Arrays.toString(extensions)});
        }
    }

    public void registerLoader(String clsName, String ... extensions){
        Class<? extends AssetLoader> clazz = null;
        try{
            clazz = (Class<? extends AssetLoader>) Class.forName(clsName);
        }catch (ClassNotFoundException ex){
            logger.log(Level.WARNING, "Failed to find loader: "+clsName, ex);
        }catch (NoClassDefFoundError ex){
            logger.log(Level.WARNING, "Failed to find loader: "+clsName, ex);
        }
        if (clazz != null){
            registerLoader(clazz, extensions);
        }
    }

    public void registerLocator(String rootPath, Class<? extends AssetLocator> locatorClass){
        handler.addLocator(locatorClass, rootPath);
        if (logger.isLoggable(Level.FINER)){
            logger.log(Level.FINER, "Registered locator: {0}",
                    locatorClass.getSimpleName());
        }
    }

    public void registerLocator(String rootPath, String clsName){
        Class<? extends AssetLocator> clazz = null;
        try{
            clazz = (Class<? extends AssetLocator>) Class.forName(clsName);
        }catch (ClassNotFoundException ex){
            logger.log(Level.WARNING, "Failed to find locator: "+clsName, ex);
        }catch (NoClassDefFoundError ex){
            logger.log(Level.WARNING, "Failed to find loader: "+clsName, ex);
        }
        if (clazz != null){
            registerLocator(rootPath, clazz);
        }
    }
    
    public void unregisterLocator(String rootPath, Class<? extends AssetLocator> clazz){
        handler.removeLocator(clazz, rootPath);
        if (logger.isLoggable(Level.FINER)){
            logger.log(Level.FINER, "Unregistered locator: {0}",
                    clazz.getSimpleName());
        }
    }

    public void clearCache(){
        cache.deleteAllAssets();
    }

    /**
     * Delete an asset from the cache, returns true if it was deleted
     * successfully.
     * <br/><br/>
     * <font color="red">Thread-safe.</font>
     */
    public boolean deleteFromCache(AssetKey key){
        return cache.deleteFromCache(key);
    }

    /**
     * Adds a resource to the cache.
     * <br/><br/>
     * <font color="red">Thread-safe.</font>
     */
    public void addToCache(AssetKey key, Object asset){
        cache.addToCache(key, asset);
    }

    public AssetInfo locateAsset(AssetKey<?> key){
        if (handler.getLocatorCount() == 0){
            logger.warning("There are no locators currently"+
                           " registered. Use AssetManager."+
                           "registerLocator() to register a"+
                           " locator.");
            return null;
        }

        AssetInfo info = handler.tryLocate(key);
        if (info == null){
            logger.log(Level.WARNING, "Cannot locate resource: {0}", key);
        }

        return info;
    }

    /**
     * <font color="red">Thread-safe.</font>
     *
     * @param <T>
     * @param key
     * @return
     */
      public <T> T loadAsset(AssetKey<T> key){
        if (key == null)
            throw new IllegalArgumentException("key cannot be null");
        
        if (eventListener != null)
            eventListener.assetRequested(key);

        AssetKey smartKey = null;
        Object o = null;
        if (key.shouldCache()){
            if (key.useSmartCache()){
                SmartAssetInfo smartInfo = cache.getFromSmartCache(key);
                if (smartInfo != null){
                    smartKey = smartInfo.smartKey.get();
                    if (smartKey != null){
                        o = smartInfo.asset;
                    }
                }
            }else{
                o = cache.getFromCache(key);
            }
        }
        if (o == null){
            AssetLoader loader = handler.aquireLoader(key);
            if (loader == null){
                throw new IllegalStateException("No loader registered for type \"" +
                                                key.getExtension() + "\"");
            }

            if (handler.getLocatorCount() == 0){
                throw new IllegalStateException("There are no locators currently"+
                                                " registered. Use AssetManager."+
                                                "registerLocator() to register a"+
                                                " locator.");
            }

            AssetInfo info = handler.tryLocate(key);
            if (info == null){
                if (handler.getParentKey() != null && eventListener != null){
                    // Inform event listener that an asset has failed to load.
                    // If the parent AssetLoader chooses not to propagate
                    // the exception, this is the only means of finding
                    // that something went wrong.
                    eventListener.assetDependencyNotFound(handler.getParentKey(), key);
                }
                throw new AssetNotFoundException(key.toString());
            }

            try {
                handler.establishParentKey(key);
                o = loader.load(info);
            } catch (IOException ex) {
                throw new AssetLoadException("An exception has occured while loading asset: " + key, ex);
            } finally {
                handler.releaseParentKey(key);
            }
            if (o == null){
                throw new AssetLoadException("Error occured while loading asset \"" + key + "\" using" + loader.getClass().getSimpleName());
            }else{
                if (logger.isLoggable(Level.FINER)){
                    logger.log(Level.FINER, "Loaded {0} with {1}",
                            new Object[]{key, loader.getClass().getSimpleName()});
                }
                
                // do processing on asset before caching
                o = key.postProcess(o);

                if (key.shouldCache())
                    cache.addToCache(key, o);

                if (eventListener != null)
                    eventListener.assetLoaded(key);
            }
        }

        // object o is the asset
        // create an instance for user
        T clone = (T) key.createClonedInstance(o);

        if (key.useSmartCache()){
            if (smartKey != null){
                // smart asset was already cached, use original key
                ((Asset)clone).setKey(smartKey);
            }else{
                // smart asset was cached on this call, use our key
                ((Asset)clone).setKey(key);
            }
        }
        
        return clone;
    }

    public Object loadAsset(String name){
        return loadAsset(new AssetKey(name));
    }

    /**
     * Loads a texture.
     *
     * @return
     */
    public Texture loadTexture(TextureKey key){
        return (Texture) loadAsset(key);
    }

    public Material loadMaterial(String name){
        return (Material) loadAsset(new MaterialKey(name));
    }

    /**
     * Loads a texture.
     *
     * @param name
     * @param generateMipmaps Enable if applying texture to 3D objects, disable
     * for GUI/HUD elements.
     * @return
     */
    public Texture loadTexture(String name, boolean generateMipmaps){
        TextureKey key = new TextureKey(name, true);
        key.setGenerateMips(generateMipmaps);
        key.setAsCube(false);
        return loadTexture(key);
    }

    public Texture loadTexture(String name, boolean generateMipmaps, boolean flipY, boolean asCube, int aniso){
        TextureKey key = new TextureKey(name, flipY);
        key.setGenerateMips(generateMipmaps);
        key.setAsCube(asCube);
        key.setAnisotropy(aniso);
        return loadTexture(key);
    }

    public Texture loadTexture(String name){
        return loadTexture(name, true);
    }

    public AudioData loadAudio(AudioKey key){
        return (AudioData) loadAsset(key);
    }

    public AudioData loadAudio(String name){
        return loadAudio(new AudioKey(name, false));
    }

    /**
     * Loads a bitmap font with the given name.
     *
     * @param name
     * @return
     */
    public BitmapFont loadFont(String name){
        return (BitmapFont) loadAsset(new AssetKey(name));
    }

    public InputStream loadGLSLLibrary(AssetKey key){
        return (InputStream) loadAsset(key);
    }

    /**
     * Load a vertex/fragment shader combo.
     *
     * @param key
     * @return
     */
    public Shader loadShader(ShaderKey key){
        // cache abuse in method
        // that doesn't use loaders/locators
        Shader s = (Shader) cache.getFromCache(key);
        if (s == null){
            String vertName = key.getVertName();
            String fragName = key.getFragName();

            String vertSource = (String) loadAsset(new AssetKey(vertName));
            String fragSource = (String) loadAsset(new AssetKey(fragName));

            s = new Shader(key.getLanguage());
            s.addSource(Shader.ShaderType.Vertex,   vertName, vertSource, key.getDefines().getCompiled());
            s.addSource(Shader.ShaderType.Fragment, fragName, fragSource, key.getDefines().getCompiled());

            cache.addToCache(key, s);
        }
        return s;
    }

    public Spatial loadModel(ModelKey key){
        return (Spatial) loadAsset(key);
    }

    /**
     * Load a model.
     *
     * @param name
     * @return
     */
    public Spatial loadModel(String name){
        return loadModel(new ModelKey(name));
    }
    
}

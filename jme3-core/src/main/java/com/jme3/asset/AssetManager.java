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
package com.jme3.asset;

import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioKey;
import com.jme3.font.BitmapFont;
import com.jme3.material.Material;
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.Caps;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.OBJLoader;
import com.jme3.shader.ShaderGenerator;
import com.jme3.texture.Texture;
import com.jme3.texture.plugins.TGALoader;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.List;

/**
 * <code>AssetManager</code> provides an interface for managing the data assets
 * of a jME3 application.
 * <p>
 * The asset manager provides a means to register {@link AssetLocator}s,
 * which are used to find asset data on disk, network, or other file system.
 * The asset locators are invoked in order of addition to find the asset data.
 * Use the {@link #registerLocator(java.lang.String, java.lang.Class) } method
 * to add new {@link AssetLocator}s. 
 * Some examples of locators:
 * <ul>
 * <li>{@link FileLocator} - Used to find assets on the local file system.</li>
 * <li>{@link ClasspathLocator} - Used to find assets in the Java classpath</li>
 * </ul>
 * <p>
 * The asset data is represented by the {@link AssetInfo} class, this
 * data is passed into the registered {@link AssetLoader}s in order to 
 * convert the data into a usable object. Use the
 * {@link #registerLoader(java.lang.Class, java.lang.String[]) } method
 * to add loaders.
 * Some examples of loaders:
 * <ul>
 * <li>{@link OBJLoader} - Used to load Wavefront .OBJ model files</li>
 * <li>{@link TGALoader} - Used to load Targa image files</li>
 * </ul>
 * <p>
 * Once the asset has been loaded, it will be 
 * {@link AssetProcessor#postProcess(com.jme3.asset.AssetKey, java.lang.Object) 
 * post-processed} by the {@link AssetKey#getProcessorType() key's processor}.
 * If the key specifies a {@link AssetKey#getCacheType() cache type}, the asset
 * will be cached in the specified cache. Next, the {@link AssetProcessor}
 * will be requested to {@link AssetProcessor#createClone(java.lang.Object) } 
 * generate a clone for the asset. Some assets do not require cloning,
 * such as immutable or shared assets. Others, like models, must be cloned
 * so that modifications to one instance do not leak onto others.
 */
public interface AssetManager {

    /**
     * Adds a {@link ClassLoader} that is used to load {@link Class classes}
     * that are needed for finding and loading Assets. 
     * This does <strong>not</strong> allow loading assets from that classpath, 
     * use registerLocator for that.
     * 
     * @param loader A ClassLoader that Classes in asset files can be loaded from.
     */
    public void addClassLoader(ClassLoader loader);

    /**
     * Remove a {@link ClassLoader} from the list of registered ClassLoaders
     * 
     * @param loader the ClassLoader to be removed
     */
    public void removeClassLoader(ClassLoader loader);

    /**
     * Retrieve the list of registered ClassLoaders that are used for loading 
     * {@link Class classes} from asset files.
     * 
     * @return an unmodifiable list
     */
    public List<ClassLoader> getClassLoaders();
    
    /**
     * Register an {@link AssetLoader} by using a class object.
     * 
     * @param loaderClass The loader class to register.
     * @param extensions Which extensions this loader is responsible for loading,
     * if there are already other loaders registered for that extension, they
     * will be overridden - there should only be one loader for each extension.
     */
    public void registerLoader(Class<? extends AssetLoader> loaderClass, String ... extensions);
    
    /**
     * Unregister a {@link AssetLoader} from loading its assigned extensions.
     * This undoes the effect of calling 
     * {@link #registerLoader(java.lang.Class, java.lang.String[]) }.
     * 
     * @param loaderClass The loader class to unregister.
     * @see #registerLoader(java.lang.Class, java.lang.String[]) 
     */
    public void unregisterLoader(Class<? extends AssetLoader> loaderClass);

    /**
     * Registers the given locator class for locating assets with this
     * <code>AssetManager</code>. {@link AssetLocator}s are invoked in the order
     * they were registered, to locate the asset by the {@link AssetKey}.
     * Once an {@link AssetLocator} returns a non-null AssetInfo, it is sent
     * to the {@link AssetLoader} to load the asset.
     * Once a locator is registered, it can be removed via
     * {@link #unregisterLocator(java.lang.String, java.lang.Class) }.
     * 
     * @param rootPath Specifies the root path from which to locate assets
     * for the given {@link AssetLocator}. The purpose of this parameter
     * depends on the type of the {@link AssetLocator}.
     * @param locatorClass The class type of the {@link AssetLocator} to register.
     *
     * @see AssetLocator#setRootPath(java.lang.String)
     * @see AssetLocator#locate(com.jme3.asset.AssetManager, com.jme3.asset.AssetKey) 
     * @see #unregisterLocator(java.lang.String, java.lang.Class) 
     */
    public void registerLocator(String rootPath, Class<? extends AssetLocator> locatorClass);

    /**
     * Unregisters the given locator class. This essentially undoes the operation
     * done by {@link #registerLocator(java.lang.String, java.lang.Class) }.
     * 
     * @param rootPath Should be the same as the root path specified in {@link
     * #registerLocator(java.lang.String, java.lang.Class) }.
     * @param locatorClass The locator class to unregister
     * 
     * @see #registerLocator(java.lang.String, java.lang.Class) 
     */
    public void unregisterLocator(String rootPath, Class<? extends AssetLocator> locatorClass);
    
    /**
     * Add an {@link AssetEventListener} to receive events from this
     * <code>AssetManager</code>. 
     * @param listener The asset event listener to add
     */
    public void addAssetEventListener(AssetEventListener listener);
    
    /**
     * Remove an {@link AssetEventListener} from receiving events from this
     * <code>AssetManager</code>
     * @param listener The asset event listener to remove
     */
    public void removeAssetEventListener(AssetEventListener listener);
    
    /**
     * Removes all asset event listeners.
     * 
     * @see #addAssetEventListener(com.jme3.asset.AssetEventListener) 
     */
    public void clearAssetEventListeners();
    
    /**
     * Manually locates an asset with the given {@link AssetKey}. 
     * This method should be used for debugging or internal uses.
     * <br>
     * The call will attempt to locate the asset by invoking the
     * {@link AssetLocator} that are registered with this <code>AssetManager</code>,
     * in the same way that the {@link AssetManager#loadAsset(com.jme3.asset.AssetKey) }
     * method locates assets.
     *
     * @param key The {@link AssetKey} to locate.
     * @return The {@link AssetInfo} object returned from the {@link AssetLocator}
     * that located the asset, or null if the asset cannot be located.
     */
    public AssetInfo locateAsset(AssetKey<?> key);

    /**
     * Load an asset from an {@link InputStream}. 
     * In some cases it may be required to load an asset from memory 
     * or arbitrary streams so that registering a custom locator and key
     * type is not necessary. 
     * 
     * @param <T> The object type that will be loaded from the AssetKey instance.
     * @param key The AssetKey. Note that the asset will not be cached - 
     * following the same behavior as if {@link AssetKey#getCacheType()} returned null.
     * @param inputStream The input stream from which the asset shall be loaded.
     * @return The loaded asset.
     * 
     * @throws AssetLoadException If the {@link AssetLoader} has failed
     * to load the asset due to an {@link IOException} or another error.
     */
    public <T> T loadAssetFromStream(AssetKey<T> key, InputStream inputStream);
    
    /**
     * Load an asset from a key, the asset will be located
     * by one of the {@link AssetLocator} implementations provided in the
     * {@link AssetManager#registerLocator(java.lang.String, java.lang.Class) }
     * call. If located successfully, it will be loaded via the appropriate
     * {@link AssetLoader} implementation based on the file's extension, as
     * specified in the call 
     * {@link AssetManager#registerLoader(java.lang.Class, java.lang.String[]) }.
     *
     * @param <T> The object type that will be loaded from the AssetKey instance.
     * @param key The AssetKey
     * @return The loaded asset.
     * 
     * @throws AssetNotFoundException If all registered locators have failed 
     * to locate the asset.
     * @throws AssetLoadException If the {@link AssetLoader} has failed
     * to load the asset due to an {@link IOException} or another error.
     */
    public <T> T loadAsset(AssetKey<T> key);

    /**
     * Load an asset by name, calling this method is the same as calling
     * <code>loadAsset(new AssetKey(name))</code>.
     *
     * @param name The name of the asset to load.
     * @return The loaded asset, or null if failed to be loaded.
     *
     * @see AssetManager#loadAsset(com.jme3.asset.AssetKey)
     */
    public Object loadAsset(String name);

    /**
     * Loads texture file, supported types are BMP, JPG, PNG, GIF,
     * TGA, DDS, PFM, and HDR.
     *
     * @param key The {@link TextureKey} to use for loading.
     * @return The loaded texture, or null if failed to be loaded.
     *
     * @see AssetManager#loadAsset(com.jme3.asset.AssetKey)
     */
    public Texture loadTexture(TextureKey key);

    /**
     * Loads texture file, supported types are BMP, JPG, PNG, GIF,
     * TGA, DDS, PFM, and HDR.
     *
     * The texture will be loaded with mip-mapping enabled. 
     * 
     * @param name The name of the texture to load.
     * @return The texture that was loaded
     *
     * @see AssetManager#loadAsset(com.jme3.asset.AssetKey)
     */
    public Texture loadTexture(String name);

    /**
     * Load audio file, supported types are WAV or OGG.
     * @param key Asset key of the audio file to load
     * @return The audio data loaded
     *
     * @see AssetManager#loadAsset(com.jme3.asset.AssetKey)
     */
    public AudioData loadAudio(AudioKey key);

    /**
     * Load audio file, supported types are WAV or OGG.
     * The file is loaded without stream-mode.
     * @param name Asset name of the audio file to load
     * @return The audio data loaded
     *
     * @see AssetManager#loadAsset(com.jme3.asset.AssetKey)
     */
    public AudioData loadAudio(String name);

    /**
     * Loads a 3D model with a ModelKey. 
     * Models can be jME3 object files (J3O), OgreXML (mesh.xml), BLEND, FBX 
     * and OBJ files.
     * @param key Asset key of the model to load
     * @return The model that was loaded
     *
     * @see AssetManager#loadAsset(com.jme3.asset.AssetKey)
     */
    public Spatial loadModel(ModelKey key);

    /**
     * Loads a 3D model. Models can be jME3 object files (J3O),
     * OgreXML (mesh.xml), BLEND, FBX and OBJ files.
     * 
     * @param name Asset name of the model to load
     * @return The model that was loaded
     *
     * @see AssetManager#loadAsset(com.jme3.asset.AssetKey)
     */
    public Spatial loadModel(String name);

    /**
     * Load a material instance (J3M) file.
     * @param name Asset name of the material to load
     * @return The material that was loaded
     *
     * @see AssetManager#loadAsset(com.jme3.asset.AssetKey)
     */
    public Material loadMaterial(String name);

    /**
     * Load a font file. Font files are in AngelCode text format,
     * and are with the extension "fnt".
     *
     * @param name Asset name of the font to load
     * @return The font loaded
     *
     * @see AssetManager#loadAsset(com.jme3.asset.AssetKey) 
     */
    public BitmapFont loadFont(String name);
    
    /**
     * Loads a filter *.j3f file with a FilterKey.
     * @param key Asset key of the filter file to load
     * @return The filter that was loaded
     *
     * @see AssetManager#loadAsset(com.jme3.asset.AssetKey)
     */
    public FilterPostProcessor loadFilter(FilterKey key);

    /**
     * Loads a filter *.j3f file with a FilterKey.
     * @param name Asset name of the filter file to load
     * @return The filter that was loaded
     *
     * @see AssetManager#loadAsset(com.jme3.asset.AssetKey)
     */
    public FilterPostProcessor loadFilter(String name);
    
    /**
     * Sets the shaderGenerator to generate shaders based on shaderNodes.
     * @param generator the shaderGenerator 
     */    
    public void setShaderGenerator(ShaderGenerator generator);
    
    /**
     * Returns the shaderGenerator responsible for generating the shaders
     *
     * @param caps a set of required capabilities
     * @return the shaderGenerator 
     */
    public ShaderGenerator getShaderGenerator(EnumSet<Caps> caps);
    
    /**
     * Retrieve an asset from the asset cache.
     * 
     * <b>NOTE:</b> Do <em>not</em> modify the returned asset! 
     * It is the same reference as what is stored in the cache, therefore any 
     * modifications to it will leak onto assets loaded from the same key in the future.
     * 
     * @param <T> The object type that will be retrieved from the AssetKey instance.
     * @param key The AssetKey to get from the cache.
     * @return The cached asset, if found. Otherwise, <code>null</code>.
     * 
     * @throws IllegalArgumentException If {@link AssetKey#getCacheType() caching}
     * is disabled for the key.
     */
    public <T> T getFromCache(AssetKey<T> key);
    
    /**
     * Inject an asset into the asset cache.
     * 
     * <b>NOTE:</b> Do <em>not</em> modify the cached asset after storing!
     * It is the same reference as what is stored in the cache, therefore any 
     * modifications to it will leak onto assets loaded from the same key in the future.
     * 
     * @param <T> The object type of the asset.
     * @param key The key where the asset shall be stored.
     * @param asset The asset to inject into the cache.
     * 
     * @throws IllegalArgumentException If {@link AssetKey#getCacheType() caching}
     * is disabled for the key.
     */
    public <T> void addToCache(AssetKey<T> key, T asset);
    
    /**
     * Delete an asset from the asset cache.
     * 
     * @param <T> The object type of the AssetKey instance.
     * @param key The asset key to remove from the cache.
     * @return True if the asset key was found in the cache and was removed
     * successfully. False if the asset key was not present in the cache.
     * 
     * @throws IllegalArgumentException If {@link AssetKey#getCacheType() caching}
     * is disabled for the key.
     */
    public <T> boolean deleteFromCache(AssetKey<T> key);
    
    /**
     * Clears the asset cache.
     */
    public void clearCache();
}

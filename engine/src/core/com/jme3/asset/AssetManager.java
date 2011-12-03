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

import com.jme3.audio.AudioData;
import com.jme3.audio.AudioKey;
import com.jme3.font.BitmapFont;
import com.jme3.material.Material;
import com.jme3.scene.Spatial;
import com.jme3.shader.Shader;
import com.jme3.shader.ShaderKey;
import com.jme3.texture.Texture;
import java.util.List;

/**
 * <code>AssetManager</code> provides an interface for managing the data assets
 * of a jME3 application.
 */
public interface AssetManager {

    /**
     * Adds a ClassLoader that is used to load *Classes* that are needed for Assets like j3o models.
     * This does *not* allow loading assets from that classpath, use registerLocator for that.
     * @param loader A ClassLoader that Classes in asset files can be loaded from
     */
    public void addClassLoader(ClassLoader loader);

    /**
     * Remove a ClassLoader from the list of registered ClassLoaders
     */
    public void removeClassLoader(ClassLoader loader);

    /**
     * Retrieve the list of registered ClassLoaders that are used for loading Classes from
     * asset files.
     */
    public List<ClassLoader> getClassLoaders();
    
    /**
     * Registers a loader for the given extensions.
     * @param loaderClassName
     * @param extensions
     */
    public void registerLoader(String loaderClassName, String ... extensions);

    /**
     * Registers an {@link AssetLocator} by using a class name, instead of 
     * a class instance. See the {@link AssetManager#registerLocator(java.lang.String, java.lang.Class) }
     * method for more information.
     *
     * @param rootPath The root path from which to locate assets, implementation
     * dependent.
     * @param locatorClassName The full class name of the {@link AssetLocator}
     * implementation.
     */
    public void registerLocator(String rootPath, String locatorClassName);

    /**
     *
     * @param loaderClass
     * @param extensions
     */
    public void registerLoader(Class<? extends AssetLoader> loaderClass, String ... extensions);

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
     */
    public void unregisterLocator(String rootPath, Class<? extends AssetLocator> locatorClass);
    
    /**
     * Set an {@link AssetEventListener} to receive events from this
     * <code>AssetManager</code>. There can only be one {@link  AssetEventListener}
     * associated with an <code>AssetManager</code>
     * 
     * @param listener
     */
    public void setAssetEventListener(AssetEventListener listener);

    /**
     * Manually locates an asset with the given {@link AssetKey}. This method
     * should be used for debugging or internal uses. <br/>
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
     * Load an asset from a key, the asset will be located
     * by one of the {@link AssetLocator} implementations provided in the
     * {@link AssetManager#registerLocator(java.lang.String, java.lang.Class) }
     * call. If located successfully, it will be loaded via the the appropriate
     * {@link AssetLoader} implementation based on the file's extension, as
     * specified in the call 
     * {@link AssetManager#registerLoader(java.lang.Class, java.lang.String[]) }.
     *
     * @param <T> The object type that will be loaded from the AssetKey instance.
     * @param key The AssetKey
     * @return The loaded asset, or null if it was failed to be located
     * or loaded.
     */
    public <T> T loadAsset(AssetKey<T> key);

    /**
     * Load a named asset by name, calling this method
     * is the same as calling
     * <code>
     * loadAsset(new AssetKey(name)).
     * </code>
     *
     * @param name The name of the asset to load.
     * @return The loaded asset, or null if failed to be loaded.
     *
     * @see AssetManager#loadAsset(com.jme3.asset.AssetKey)
     */
    public Object loadAsset(String name);

    /**
     * Loads texture file, supported types are BMP, JPG, PNG, GIF,
     * TGA and DDS.
     *
     * @param key The {@link TextureKey} to use for loading.
     * @return The loaded texture, or null if failed to be loaded.
     *
     * @see AssetManager#loadAsset(com.jme3.asset.AssetKey)
     */
    public Texture loadTexture(TextureKey key);

    /**
     * Loads texture file, supported types are BMP, JPG, PNG, GIF,
     * TGA and DDS.
     *
     * @param name The name of the texture to load.
     * @return The texture that was loaded
     *
     * @see AssetManager#loadAsset(com.jme3.asset.AssetKey)
     */
    public Texture loadTexture(String name);

    /**
     * Load audio file, supported types are WAV or OGG.
     * @param key
     * @return The audio data loaded
     *
     * @see AssetManager#loadAsset(com.jme3.asset.AssetKey)
     */
    public AudioData loadAudio(AudioKey key);

    /**
     * Load audio file, supported types are WAV or OGG.
     * The file is loaded without stream-mode.
     * @param name
     * @return The audio data loaded
     *
     * @see AssetManager#loadAsset(com.jme3.asset.AssetKey)
     */
    public AudioData loadAudio(String name);

    /**
     * Loads a named model. Models can be jME3 object files (J3O) or
     * OgreXML/OBJ files.
     * @param key
     * @return The model that was loaded
     *
     * @see AssetManager#loadAsset(com.jme3.asset.AssetKey)
     */
    public Spatial loadModel(ModelKey key);

    /**
     * Loads a named model. Models can be jME3 object files (J3O) or
     * OgreXML/OBJ files.
     * @param name
     * @return The model that was loaded
     *
     * @see AssetManager#loadAsset(com.jme3.asset.AssetKey)
     */
    public Spatial loadModel(String name);

    /**
     * Load a material (J3M) file.
     * @param name
     * @return The material that was loaded
     *
     * @see AssetManager#loadAsset(com.jme3.asset.AssetKey)
     */
    public Material loadMaterial(String name);

    /**
     * Loads shader file(s), shouldn't be used by end-user in most cases.
     *
     * @see AssetManager#loadAsset(com.jme3.asset.AssetKey)
     */
    public Shader loadShader(ShaderKey key);

    /**
     * Load a font file. Font files are in AngelCode text format,
     * and are with the extension "fnt".
     *
     * @param name
     * @return The font loaded
     *
     * @see AssetManager#loadAsset(com.jme3.asset.AssetKey) 
     */
    public BitmapFont loadFont(String name);
}

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
import com.jme3.shader.Shader;
import com.jme3.shader.ShaderGenerator;
import com.jme3.shader.ShaderKey;
import com.jme3.texture.Texture;
import com.jme3.texture.plugins.TGALoader;
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
 * Once the asset has been loaded, 
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
     */
    public void removeClassLoader(ClassLoader loader);

    /**
     * Retrieve the list of registered ClassLoaders that are used for loading 
     * {@link Class classes} from asset files.
     */
    public List<ClassLoader> getClassLoaders();
    
    /**
     * Registers a loader for the given extensions.
     * 
     * @param loaderClassName
     * @param extensions
     * 
     * @deprecated Please use {@link #registerLoader(java.lang.Class, java.lang.String[]) }
     * together with {@link Class#forName(java.lang.String) } to find a class
     * and then register it.
     * 
     * @deprecated Please use {@link #registerLoader(java.lang.Class, java.lang.String[]) }
     * with {@link Class#forName(java.lang.String) } instead.
     */
    @Deprecated
    public void registerLoader(String loaderClassName, String ... extensions);

    /**
     * Registers an {@link AssetLocator} by using a class name. 
     * See the {@link AssetManager#registerLocator(java.lang.String, java.lang.Class) }
     * method for more information.
     *
     * @param rootPath The root path from which to locate assets, this 
     * depends on the implementation of the asset locator. 
     * A URL based locator will expect a url folder such as "http://www.example.com/"
     * while a File based locator will expect a file path (OS dependent).
     * @param locatorClassName The full class name of the {@link AssetLocator}
     * implementation.
     * 
     * @deprecated Please use {@link #registerLocator(java.lang.String, java.lang.Class)  }
     * together with {@link Class#forName(java.lang.String) } to find a class
     * and then register it.
     */
    @Deprecated
    public void registerLocator(String rootPath, String locatorClassName);

    /**
     * Register an {@link AssetLoader} by using a class object.
     * 
     * @param loaderClass
     * @param extensions
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
     * Set an {@link AssetEventListener} to receive events from this
     * <code>AssetManager</code>. Any currently added listeners are
     * cleared and then the given listener is added.
     * 
     * @param listener The listener to set
     * @deprecated Please use {@link #addAssetEventListener(com.jme3.asset.AssetEventListener) }
     * to listen for asset events.
     */
    @Deprecated
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
     * Load an asset by name, calling this method
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
     * Models can be jME3 object files (J3O) or OgreXML/OBJ files.
     * @param key Asset key of the model to load
     * @return The model that was loaded
     *
     * @see AssetManager#loadAsset(com.jme3.asset.AssetKey)
     */
    public Spatial loadModel(ModelKey key);

    /**
     * Loads a 3D model. Models can be jME3 object files (J3O) or
     * OgreXML/OBJ files.
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
     * Loads shader file(s), shouldn't be used by end-user in most cases.
     *
     * @see AssetManager#loadAsset(com.jme3.asset.AssetKey)
     */
    public Shader loadShader(ShaderKey key);

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
     * @return the shaderGenerator 
     */
    public ShaderGenerator getShaderGenerator(EnumSet<Caps> caps);
    
}

/*
 * Copyright (c) 2009-2023 jMonkeyEngine
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
package com.jme3.scene.plugins.gltf;

import com.jme3.asset.ModelKey;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * An optional key to use when loading a glTF file
 * It allows you to specify custom data loader, replacing the default ones.
 *
 * MaterialAdapters: Allows you to map glTF standard material model to a non-stock material.
 * ExtensionLoaders: Allows you to provide or override a loader for a given glTF extension.
 * ExtrasLoader: Allows you to load any extras, application specific data of the glTF file.
 *
 * For more information, please see glTF 2.0 specifications
 * https://github.com/KhronosGroup/glTF/blob/master/specification/2.0/README.md
 *
 * Created by Nehon on 08/08/2017.
 */
public class GltfModelKey extends ModelKey {

    private Map<String, MaterialAdapter> materialAdapters = new HashMap<>();
    private static Map<String, ExtensionLoader> extensionLoaders = new HashMap<>();
    private boolean keepSkeletonPose = false;
    private ExtrasLoader extrasLoader;

    /**
     * The flag indicating whether the loader should perform stricter consistency checks of the supported glTF
     * extensions.
     * 
     * When this is <code>true</code>, then the loader will cause an <code>AssetLoadException</code> when it
     * encounters an asset that contains an extension in its <code>extensionsRequired</code> declaration that
     * is not supported.
     */
    private boolean strictExtensionCheck = true;

    public GltfModelKey(String name) {
        super(name);
    }

    public GltfModelKey() {
    }
    
    /**
     * Set whether the loader should perform stricter consistency checks when loading a model. Details are not
     * specified for now.
     * 
     * The default value is <code>true</code>.
     * 
     * @param strict
     *            The flag
     */
    public void setStrict(boolean strict) {
        this.strictExtensionCheck = strict;
    }

    /**
     * Returns whether the loader should perform stricter consistency checks when loading a model. Details are
     * not specified for now.
     * 
     * @return The flag
     */
    public boolean isStrict() {
        return strictExtensionCheck;
    }

    /**
     * Registers a MaterialAdapter for the given materialName.
     * The materialName must be "pbrMetallicRoughness" or any name from KHR_materials glTF Extension (for example "pbrSpecularGlossiness" for "KHR_materials_pbrSpecularGlossiness" extension)
     *
     * @param gltfMaterialName the name of the gltf material
     * @param adapter          the material adapter
     */
    public void registerMaterialAdapter(String gltfMaterialName, MaterialAdapter adapter) {
        materialAdapters.put(gltfMaterialName, adapter);
    }

    /**
     * Registers an extension loader for the given extension name.
     * For more information on extension please see glTF 2.0 extensions registry
     * https://github.com/KhronosGroup/glTF/blob/master/extensions/README.md
     *
     * @param extensionName the name of the extension
     * @param loader        the Extension loader
     */
    public void registerExtensionLoader(String extensionName, ExtensionLoader loader) {
        extensionLoaders.put(extensionName, loader);
    }

    public MaterialAdapter getAdapterForMaterial(String gltfMaterialName) {
        return materialAdapters.get(gltfMaterialName);
    }

    public ExtensionLoader getExtensionLoader(String extensionName) {
        return extensionLoaders.get(extensionName);
    }

    public boolean isKeepSkeletonPose() {
        return keepSkeletonPose;
    }

    public void setKeepSkeletonPose(boolean keepSkeletonPose) {
        this.keepSkeletonPose = keepSkeletonPose;
    }

    public ExtrasLoader getExtrasLoader() {
        return extrasLoader;
    }

    /**
     * Sets the ExtrasLoader for reading any extra information from the gltf file.
     *
     * @param extrasLoader the desired loader
     */
    public void setExtrasLoader(ExtrasLoader extrasLoader) {
        this.extrasLoader = extrasLoader;
    }
    
    @Override
    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }
        if (getClass() != object.getClass()) {
            return false;
        }
        final GltfModelKey other = (GltfModelKey)object;
        if (!super.equals(other)) {
            return false;
        }
        if (!Objects.equals(materialAdapters, other.materialAdapters)
                || !Objects.equals(extrasLoader, other.extrasLoader)) {
            return false;
        }
        return keepSkeletonPose == other.keepSkeletonPose;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 37 * hash + materialAdapters.hashCode();
        hash = 37 * hash + Objects.hashCode(this.extrasLoader);
        hash = 37 * hash + (this.keepSkeletonPose ? 1 : 0);
        return hash;
    }
    
}

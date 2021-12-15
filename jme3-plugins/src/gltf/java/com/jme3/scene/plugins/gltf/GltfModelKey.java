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
package com.jme3.scene.plugins.gltf;

import com.jme3.asset.ModelKey;

import java.util.HashMap;
import java.util.Map;

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
    private ExtrasLoader extrasLoader;
    private boolean keepSkeletonPose = false;

    public GltfModelKey(String name) {
        super(name);
    }

    public GltfModelKey() {
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
}

/*
 * Copyright (c) 2009-2026 jMonkeyEngine
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

import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;

/**
 * A material factory creates {@link Material}s based on the data of a single material from a GLTF file.
 * <p>
 *   All material factories have bo be registered with the {@link GltfLoader} by using one of its
 *   static register methods.
 * </p>
 */
public interface GltfMaterialFactory {

	/**
	 * Checks, if the factory is able to create a new material from the given material data.
	 * If it accepts the material data, the {@link #createMaterial(AssetManager, AssetKey, GltfMaterialData)} method
	 * can be used to create a new material.
	 *
	 * @param assetKey The {@link AssetKey} used for loading the GLTF model.
	 * @param gltfMaterialData The {@link GltfMaterialData} containing all available GLTF material data.
	 * @return true if the factory is able to create a material from the given material data, otherwise false.
	 */
	boolean accepts(AssetKey<?> assetKey, GltfMaterialData gltfMaterialData);

	/**
	 * Creates a new material from the given material data.
	 *
	 * @param assetManager     The {@link AssetManager} instance.
	 * @param assetKey The {@link AssetKey} used for loading the GLTF model.
	 * @param gltfMaterialData The {@link GltfMaterialData} containing all available GLTF material data.
	 * @return The new created {@link Material}.
	 */
	Material createMaterial(AssetManager assetManager, AssetKey<?> assetKey, GltfMaterialData gltfMaterialData);

}

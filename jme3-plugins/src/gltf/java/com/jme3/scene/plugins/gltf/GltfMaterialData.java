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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Stores all data of a single material from a GLTF file.
 * This data can then be used by a {@link GltfMaterialFactory} to create a new material.
 *  <h2>Parameter naming convention</h2>
 *  <ul>
 *    <li>
 *      Parameter names should use single dots '.' as a separator.
 *    </li>
 *    <li>
 *      All standard parameter names are directly taken from the
 *      <a href="https://registry.khronos.org/glTF/specs/2.0/glTF-2.0.html#reference-material">GLTF specs document</a>
 *      (§ 5.19. Material and following).
 *    </li>
 *    <li>
 *      All additional parameter names from GLTF extensions should start with {@link #MATERIAL_EXTENSION_PARAM_PREFIX}
 *      followed by the name of the extension (e.g. "KHR_materials_unlit") and finished by the parameter name.
 *    </li>
 *    <li>
 *      All additional parameter names from GLTF extras should start with {@link #MATERIAL_EXTRA_PARAM_PREFIX}.
 *    </li>
 *  </ul>
 */
public class GltfMaterialData {

	public static final String MATERIAL_NAME_PARAM = "material.name";

	public static final String BASE_COLOR_PARAM = "material.pbrMetallicRoughness.baseColorFactor";

	public static final String BASE_COLOR_TEXTURE_PARAM = "material.pbrMetallicRoughness.baseColorTexture";

	public static final String METALLIC_FACTOR_PARAM = "material.pbrMetallicRoughness.metallicFactor";

	public static final String ROUGHNESS_FACTOR_PARAM = "material.pbrMetallicRoughness.roughnessFactor";

	public static final String METALLIC_ROUGHNESS_TEXTURE_PARAM = "material.pbrMetallicRoughness.metallicRoughnessTexture";

	public static final String NORMAL_TEXTURE_PARAM = "material.normalTexture";

	public static final String NORMAL_SCALE_PARAM = "material.normalTextureInfo.scale";

	public static final String OCCLUSION_TEXTURE_PARAM = "material.occlusionTexture";

	public static final String OCCLUSION_TEXTURE_STRENGTH_PARAM = "material.occlusionTextureInfo.strength";

	public static final String EMISSIVE_TEXTURE_PARAM = "material.emissiveTexture";

	public static final String EMISSIVE_COLOR_PARAM = "material.emissiveFactor";

	public static final String ALPHA_MODE_PARAM = "material.alphaMode";

	public static final String ALPHA_CUTOFF_PARAM = "material.alphaCutoff";

	public static final String DOUBLE_SIDED_PARAM = "material.doubleSided";

	public static final String MATERIAL_EXTENSION_PARAM_PREFIX = "material.extension.";

	public static final String MATERIAL_EXTRA_PARAM_PREFIX = "material.extra.";


	private Map<String, Object> gltfParamMap = new HashMap<>();

	private Set<String> gltfExtensions = new HashSet<>();

	/**
	 * Indicates the existence of a vertex color buffer.
	 */
	private boolean hasVertexColors;


	/**
	 * Checks if the material provides the given GLTF extension.
	 *
	 * @param gltfExtension The GLTF extension name.
	 * @return true if the material provides the given GLTF extension, otherwise false.
	 */
	public boolean hasGltfExtension(String gltfExtension) {
		return gltfExtensions.contains(gltfExtension);
	}

	/**
	 * Adds the given GLTF extension name.
	 *
	 * @param gltfExtension The GLTF extension name.
	 */
	public void addGltfExtension(String gltfExtension) {
		gltfExtensions.add(gltfExtension);
	}

	/**
	 * Removes the given GLTF extension name.
	 *
	 * @param gltfExtension The GLTF extension name.
	 */
	public void removeGltfExtension(String gltfExtension) {
		gltfExtensions.remove(gltfExtension);
	}


	/**
	 * Checks if the material provides a material parameter with the given name.
	 *
	 * @param gltfParamName The GLTF parameter name.
	 * @return true if the material provides a material parameter with the given name, otherwise false.
	 */
	public boolean containsGltfParam(String gltfParamName) {
		return gltfParamMap.containsKey(gltfParamName);
	}

	/**
	 * Gets the material parameter with the given name.
	 *
	 * @param gltfParamName The GLTF parameter name.
	 * @return The value of the material parameter with the given name, or null if no such parameter exists.
	 */
	public Object getGltfParam(String gltfParamName) {
		return gltfParamMap.get(gltfParamName);
	}

	/**
	 * Adds a material parameter with the given name and value.
	 *
	 * @param gltfParamName The GLTF parameter name.
	 * @param value         The value of the material parameter. Does nothing, if value is null.
	 */
	public void setGltfParam(String gltfParamName, Object value) {
		if (value != null) {
			gltfParamMap.put(gltfParamName, value);
		}
	}

	/**
	 * Removes the material parameter with the given name.
	 *
	 * @param gltfParamName The GLTF parameter name.
	 * @return The previous value of the material parameter, or null if there was no such parameter.
	 */
	public Object removeGltfParam(String gltfParamName) {
		return gltfParamMap.remove(gltfParamName);
	}


	/**
	 * @return Indicates the existence of a vertex color buffer.
	 */
	public boolean hasVertexColors() {
		return hasVertexColors;
	}

	/**
	 * Sets the vertex color flag.
	 *
	 * @param hasVertexColors The value to set.
	 */
	public void setHasVertexColors(boolean hasVertexColors) {
		this.hasVertexColors = hasVertexColors;
	}

}

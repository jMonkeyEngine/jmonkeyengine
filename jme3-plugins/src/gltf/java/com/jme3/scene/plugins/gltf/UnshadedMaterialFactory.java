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
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;

import static com.jme3.scene.plugins.gltf.GltfMaterialData.*;

/**
 * This material factory creates jME3's standard "Unshaded" materials.
 */
public class UnshadedMaterialFactory implements GltfMaterialFactory {

	@Override
	public boolean accepts(AssetKey<?> assetKey, GltfMaterialData gltfMaterialData) {
		return gltfMaterialData.hasGltfExtension(UnlitExtensionLoader.EXTENSION_NAME);
	}

	@Override
	public Material createMaterial(AssetManager assetManager, AssetKey<?> assetKey, GltfMaterialData gltfMaterialData) {
		Material material = new Material(assetManager,getMaterialDefPath());
		material.setName((String) gltfMaterialData.getGltfParam(MATERIAL_NAME_PARAM));

		setParam(material, "Color", gltfMaterialData.getGltfParam(BASE_COLOR_PARAM), ColorRGBA.White);
		setParam(material, "ColorMap", gltfMaterialData.getGltfParam(BASE_COLOR_TEXTURE_PARAM));
		setParam(material, "GlowColor", gltfMaterialData.getGltfParam(EMISSIV_COLOR_PARAM), ColorRGBA.Black);
		setParam(material, "GlowMap", gltfMaterialData.getGltfParam(EMISSIV_TEXTURE_PARAM));

		if (gltfMaterialData.containsGltfParam(ALPHA_MODE_PARAM)) {
			String alphaMode = (String) gltfMaterialData.getGltfParam(ALPHA_MODE_PARAM);
			switch (alphaMode) {
				case "MASK":
					// "MASK" -> BlendMode.Off
					setParam(material, "AlphaDiscardThreshold", gltfMaterialData.getGltfParam(ALPHA_CUTOFF_PARAM), 0.5f);
					break;
				case "BLEND":
					material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
					break;
			}
		}

		if (gltfMaterialData.containsGltfParam(DOUBLE_SIDED_PARAM)) {
			boolean doubleSided = (boolean) gltfMaterialData.getGltfParam(DOUBLE_SIDED_PARAM);
			if (doubleSided) {
				//Note that this is not completely right as normals on the back side will be in the wrong direction.
				material.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
			}
		}

		return material;
	}

	protected String getMaterialDefPath() {
		return "Common/MatDefs/Misc/Unshaded.j3md";
	}

	protected void setParam(Material material, String paramName, Object value) {
		if (value != null) {
			material.setParam(paramName, value);
		}
	}

	protected void setParam(Material material, String paramName, Object value, Object defaultValue) {
		setParam(material, paramName, value != null ? value : defaultValue);
	}

}

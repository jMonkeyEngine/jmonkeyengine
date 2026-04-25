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
import com.jme3.texture.Texture;

import static com.jme3.scene.plugins.gltf.GltfMaterialData.*;
import static com.jme3.scene.plugins.gltf.PBREmissiveStrengthExtensionLoader.EMISSIVE_STRENGTH_PARAM;
import static com.jme3.scene.plugins.gltf.PBRSpecGlossExtensionLoader.*;

/**
 * This material factory creates jME3's standard "PBRLighting" materials.
 */
public class PBRLightingMaterialFactory implements GltfMaterialFactory {

	@Override
	public boolean accepts(AssetKey<?> assetKey, GltfMaterialData gltfMaterialData) {
		// Since PBRLighting is the default material, it accepts all material data,
		// making any subsequent material factories effectively unreachable.
		return true;
	}

	@Override
	public Material createMaterial(AssetManager assetManager, AssetKey<?> assetKey, GltfMaterialData gltfMaterialData) {
		Material material = new Material(assetManager, getMaterialDefPath());
		material.setName((String) gltfMaterialData.getGltfParam(MATERIAL_NAME_PARAM));

		setStandardParams(material, gltfMaterialData);

		if (gltfMaterialData.hasGltfExtension(PBRSpecGlossExtensionLoader.EXTENSION_NAME)) {
			setSpecularGlossinessParams(material, gltfMaterialData);

		} else {
			setMetallicRoughnessParams(material, gltfMaterialData);
		}

		return material;
	}

	protected String getMaterialDefPath() {
		return "Common/MatDefs/Light/PBRLighting.j3md";
	}

	protected void setStandardParams(Material material, GltfMaterialData gltfMaterialData) {
		if (gltfMaterialData.containsGltfParam(NORMAL_TEXTURE_PARAM)) {
			setParam(material, "NormalMap", gltfMaterialData.getGltfParam(NORMAL_TEXTURE_PARAM));
			setParam(material, "NormalScale", gltfMaterialData.getGltfParam(NORMAL_SCALE_PARAM));
			material.setFloat("NormalType", 1f);
		}

		if (gltfMaterialData.containsGltfParam(OCCLUSION_TEXTURE_PARAM)) {
			// Gltf only supports AO maps (gray scales and only the r channel must be read)
			material.setBoolean("LightMapAsAOMap", true);
			setParam(material, "LightMap", gltfMaterialData.getGltfParam(OCCLUSION_TEXTURE_PARAM));
			setParam(material, "AoStrength", gltfMaterialData.getGltfParam(OCCLUSION_TEXTURE_STRENGTH_PARAM));

			// Check if the occlusion texture is actually the same instance as the metallic-roughness texture.
			boolean isAoPackedInMRMap = false;
			if (gltfMaterialData.containsGltfParam(METALLIC_ROUGHNESS_TEXTURE_PARAM)) {
				Texture occlusionTexture = (Texture) gltfMaterialData.getGltfParam(OCCLUSION_TEXTURE_PARAM);
				Texture metallicRoughnessTexture = (Texture) gltfMaterialData.getGltfParam(METALLIC_ROUGHNESS_TEXTURE_PARAM);
				isAoPackedInMRMap = occlusionTexture == metallicRoughnessTexture;
			}
			material.setBoolean("AoPackedInMRMap", isAoPackedInMRMap);
		}

		setParam(material, "EmissiveMap", gltfMaterialData.getGltfParam(EMISSIV_TEXTURE_PARAM));
		setParam(material, "Emissive", gltfMaterialData.getGltfParam(EMISSIV_COLOR_PARAM), ColorRGBA.Black);
		setParam(material, "EmissiveIntensity", gltfMaterialData.getGltfParam(EMISSIVE_STRENGTH_PARAM));

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

		setParam(material, "UseVertexColor", gltfMaterialData.hasVertexColors());
	}

	protected void setMetallicRoughnessParams(Material material, GltfMaterialData gltfMaterialData) {
		setParam(material, "BaseColor", gltfMaterialData.getGltfParam(BASE_COLOR_PARAM), ColorRGBA.White);
		setParam(material, "BaseColorMap", gltfMaterialData.getGltfParam(BASE_COLOR_TEXTURE_PARAM));
		setParam(material, "Metallic", gltfMaterialData.getGltfParam(METALLIC_FACTOR_PARAM), 1f);
		setParam(material, "Roughness", gltfMaterialData.getGltfParam(ROUGHNESS_FACTOR_PARAM), 1f);
		setParam(material, "MetallicRoughnessMap", gltfMaterialData.getGltfParam(METALLIC_ROUGHNESS_TEXTURE_PARAM));
	}

	protected void setSpecularGlossinessParams(Material material, GltfMaterialData gltfMaterialData) {
		material.setBoolean("UseSpecGloss", true);
		setParam(material, "BaseColor", gltfMaterialData.getGltfParam(DIFFUSE_COLOR_PARAM));
		setParam(material, "BaseColorMap", gltfMaterialData.getGltfParam(DIFFUSE_TEXTURE_PARAM));
		setParam(material, "Specular", gltfMaterialData.getGltfParam(SPECULAR_COLOR_PARAM));
		setParam(material, "Glossiness", gltfMaterialData.getGltfParam(GLOSSINESS_FACTOR_PARAM));
		setParam(material, "SpecularGlossinessMap", gltfMaterialData.getGltfParam(SPECULAR_GLOSSINESS_TEXTURE_PARAM));
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

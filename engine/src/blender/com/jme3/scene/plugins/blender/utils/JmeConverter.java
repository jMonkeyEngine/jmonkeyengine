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
package com.jme3.scene.plugins.blender.utils;

import java.util.List;
import java.util.logging.Logger;

import com.jme3.asset.BlenderKey.FeaturesToLoad;
import com.jme3.asset.BlenderKey.WorldData;
import com.jme3.light.AmbientLight;
import com.jme3.light.Light;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.plugins.blender.data.Structure;
import com.jme3.scene.plugins.blender.exception.BlenderFileException;
import com.jme3.scene.plugins.blender.helpers.CameraHelper;
import com.jme3.scene.plugins.blender.helpers.LightHelper;
import com.jme3.scene.plugins.blender.helpers.MaterialHelper;
import com.jme3.scene.plugins.blender.helpers.MeshHelper;
import com.jme3.scene.plugins.blender.helpers.ObjectHelper;

/**
 * This class converts blender file blocks into jMonkeyEngine data structures.
 * @author Marcin Roguski
 */
public class JmeConverter implements IBlenderConverter<Node, Camera, Light, Object, List<Geometry>, Material> {
	private static final Logger		LOGGER						= Logger.getLogger(JmeConverter.class.getName());

	private final DataRepository	dataRepository;

	/**
	 * Constructor. Creates the loader and checks if the given data is correct.
	 * @param dataRepository
	 *        the data repository; it should have the following field set: - asset manager - blender key - dna block
	 *        data - blender input stream Otherwise IllegalArgumentException will be thrown.
	 * @param featuresToLoad
	 *        bitwise flag describing what features are to be loaded
	 * @see FeaturesToLoad FeaturesToLoad
	 */
	public JmeConverter(DataRepository dataRepository) {
		//validating the given data first
		if(dataRepository.getAssetManager() == null) {
			throw new IllegalArgumentException("Cannot find asset manager!");
		}
		if(dataRepository.getBlenderKey() == null) {
			throw new IllegalArgumentException("Cannot find blender key!");
		}
		if(dataRepository.getDnaBlockData() == null) {
			throw new IllegalArgumentException("Cannot find dna block!");
		}
		if(dataRepository.getInputStream() == null) {
			throw new IllegalArgumentException("Cannot find blender file stream!");
		}
		this.dataRepository = dataRepository;
	}

	@Override
	public Node toScene(Structure structure) {//TODO: poprawny import sceny
		if((dataRepository.getBlenderKey().getFeaturesToLoad() & FeaturesToLoad.SCENES) == 0) {
			return null;
		}
		Structure id = (Structure)structure.getFieldValue("id");
		String sceneName = id.getFieldValue("name").toString();
		return new Node(sceneName);
	}

	@Override
	public Camera toCamera(Structure structure) throws BlenderFileException {
		if((dataRepository.getBlenderKey().getFeaturesToLoad() & FeaturesToLoad.CAMERAS) == 0) {
			return null;
		}
		CameraHelper cameraHelper = dataRepository.getHelper(CameraHelper.class);
		return cameraHelper.toCamera(structure);
	}

	@Override
	public Light toLight(Structure structure) throws BlenderFileException {
		if((dataRepository.getBlenderKey().getFeaturesToLoad() & FeaturesToLoad.LIGHTS) == 0) {
			return null;
		}
		LightHelper lightHelper = dataRepository.getHelper(LightHelper.class);
		return lightHelper.toLight(structure, dataRepository);
	}

	@Override
	public Object toObject(Structure structure) throws BlenderFileException {
		if((dataRepository.getBlenderKey().getFeaturesToLoad() & FeaturesToLoad.OBJECTS) == 0) {
			return null;
		}
		ObjectHelper objectHelper = dataRepository.getHelper(ObjectHelper.class);
		return objectHelper.toObject(structure, dataRepository);
	}

	@Override
	public List<Geometry> toMesh(Structure structure) throws BlenderFileException {
		MeshHelper meshHelper = dataRepository.getHelper(MeshHelper.class);
		return meshHelper.toMesh(structure, dataRepository);
	}

	@Override
	public Material toMaterial(Structure structure) throws BlenderFileException {
		if((dataRepository.getBlenderKey().getFeaturesToLoad() & FeaturesToLoad.MATERIALS) == 0) {
			return null;
		}
		MaterialHelper materialHelper = dataRepository.getHelper(MaterialHelper.class);
		return materialHelper.toMaterial(structure, dataRepository);
	}

	/**
	 * This method returns the data read from the WORLD file block. The block contains data that can be stored as
	 * separate jme features and therefore cannot be returned as a single jME scene feature.
	 * @param structure
	 *        the structure with WORLD block data
	 * @return data read from the WORLD block that can be added to the scene
	 */
	public WorldData toWorldData(Structure structure) {
		WorldData result = new WorldData();

		//reading ambient light
		AmbientLight ambientLight = new AmbientLight();
		float ambr = ((Number)structure.getFieldValue("ambr")).floatValue();
		float ambg = ((Number)structure.getFieldValue("ambg")).floatValue();
		float ambb = ((Number)structure.getFieldValue("ambb")).floatValue();
		ambientLight.setColor(new ColorRGBA(ambr, ambg, ambb, 0.0f));
		result.setAmbientLight(ambientLight);

		return result;
	}
}

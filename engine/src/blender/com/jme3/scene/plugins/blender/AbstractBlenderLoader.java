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
package com.jme3.scene.plugins.blender;

import com.jme3.asset.AssetLoader;
import com.jme3.asset.BlenderKey.FeaturesToLoad;
import com.jme3.asset.BlenderKey.WorldData;
import com.jme3.light.AmbientLight;
import com.jme3.light.Light;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.blender.cameras.CameraHelper;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.lights.LightHelper;
import com.jme3.scene.plugins.blender.materials.MaterialHelper;
import com.jme3.scene.plugins.blender.meshes.MeshHelper;
import com.jme3.scene.plugins.blender.objects.ObjectHelper;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class converts blender file blocks into jMonkeyEngine data structures.
 * @author Marcin Roguski (Kaelthas)
 */
/* package */ abstract class AbstractBlenderLoader implements AssetLoader {
	private static final Logger LOGGER = Logger.getLogger(AbstractBlenderLoader.class.getName());
	
	protected BlenderContext	blenderContext;

	/**
	 * This method converts the given structure to a scene node.
	 * @param structure
	 *        structure of a scene
	 * @return scene's node
	 */
	public Node toScene(Structure structure) {
		if ((blenderContext.getBlenderKey().getFeaturesToLoad() & FeaturesToLoad.SCENES) == 0) {
			return null;
		}
		Node result = new Node(structure.getName());
		try {
			List<Structure> base = ((Structure)structure.getFieldValue("base")).evaluateListBase(blenderContext);
			for(Structure b : base) {
				Pointer pObject = (Pointer) b.getFieldValue("object");
				if(pObject.isNotNull()) {
					Structure objectStructure = pObject.fetchData(blenderContext.getInputStream()).get(0);
					Object object = this.toObject(objectStructure);
					if(object instanceof Spatial && ((Spatial) object).getParent()==null) {
						result.attachChild((Spatial) object);
					} else if(object instanceof Light) {
						result.addLight((Light)object);
					}
				}
			}
		} catch (BlenderFileException e) {
			LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
		return result;
	}

	/**
	 * This method converts the given structure to a camera.
	 * @param structure
	 *        structure of a camera
	 * @return camera's node
	 */
	public Camera toCamera(Structure structure) throws BlenderFileException {
		CameraHelper cameraHelper = blenderContext.getHelper(CameraHelper.class);
		if (cameraHelper.shouldBeLoaded(structure, blenderContext)) {
			return cameraHelper.toCamera(structure);
		}
		return null;
	}

	/**
	 * This method converts the given structure to a light.
	 * @param structure
	 *        structure of a light
	 * @return light's node
	 */
	public Light toLight(Structure structure) throws BlenderFileException {
		LightHelper lightHelper = blenderContext.getHelper(LightHelper.class);
		if (lightHelper.shouldBeLoaded(structure, blenderContext)) {
			return lightHelper.toLight(structure, blenderContext);
		}
		return null;
	}

	/**
	 * This method converts the given structure to a node.
	 * @param structure
	 *        structure of an object
	 * @return object's node
	 */
	public Object toObject(Structure structure) throws BlenderFileException {
		ObjectHelper objectHelper = blenderContext.getHelper(ObjectHelper.class);
		if (objectHelper.shouldBeLoaded(structure, blenderContext)) {
			return objectHelper.toObject(structure, blenderContext);
		}
		return null;
	}

	/**
	 * This method converts the given structure to a list of geometries.
	 * @param structure
	 *        structure of a mesh
	 * @return list of geometries
	 */
	public List<Geometry> toMesh(Structure structure) throws BlenderFileException {
		MeshHelper meshHelper = blenderContext.getHelper(MeshHelper.class);
		if (meshHelper.shouldBeLoaded(structure, blenderContext)) {
			return meshHelper.toMesh(structure, blenderContext);
		}
		return null;
	}

	/**
	 * This method converts the given structure to a material.
	 * @param structure
	 *        structure of a material
	 * @return material's node
	 */
	public Material toMaterial(Structure structure) throws BlenderFileException {
		MaterialHelper materialHelper = blenderContext.getHelper(MaterialHelper.class);
		if (materialHelper.shouldBeLoaded(structure, blenderContext)) {
			return materialHelper.toMaterial(structure, blenderContext);
		}
		return null;
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

		// reading ambient light
		AmbientLight ambientLight = new AmbientLight();
		float ambr = ((Number) structure.getFieldValue("ambr")).floatValue();
		float ambg = ((Number) structure.getFieldValue("ambg")).floatValue();
		float ambb = ((Number) structure.getFieldValue("ambb")).floatValue();
		ambientLight.setColor(new ColorRGBA(ambr, ambg, ambb, 0.0f));
		result.setAmbientLight(ambientLight);

		return result;
	}
}

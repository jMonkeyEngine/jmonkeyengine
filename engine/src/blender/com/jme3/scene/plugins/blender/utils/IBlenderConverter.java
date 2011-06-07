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

import com.jme3.scene.plugins.blender.data.Structure;
import com.jme3.scene.plugins.blender.exception.BlenderFileException;

/**
 * This interface provides an abstraction to converting loaded blender structures into data structures. The data
 * structures can vary and therefore one can use the loader for different kind of engines.
 * @author Marcin Roguski
 * @param <NodeType>
 *        the type of the scene node element
 * @param <CameraType>
 *        the type of camera element
 * @param <LightType>
 *        the type of light element
 * @param <ObjectType>
 *        the type of object element
 * @param <MeshType>
 *        the type of mesh element
 * @param <MaterialType>
 *        the type of material element
 */
//TODO: ujednolicić wyrzucane wyjątki
public interface IBlenderConverter<NodeType, CameraType, LightType, ObjectType, MeshType, MaterialType> {
	/**
	 * This method reads converts the given structure into scene. The given structure needs to be filled with the
	 * appropriate data.
	 * @param structure
	 *        the structure we read the scene from
	 * @return the scene feature
	 */
	NodeType toScene(Structure structure);

	/**
	 * This method reads converts the given structure into camera. The given structure needs to be filled with the
	 * appropriate data.
	 * @param structure
	 *        the structure we read the camera from
	 * @return the camera feature
	 */
	CameraType toCamera(Structure structure) throws BlenderFileException;

	/**
	 * This method reads converts the given structure into light. The given structure needs to be filled with the
	 * appropriate data.
	 * @param structure
	 *        the structure we read the light from
	 * @return the light feature
	 */
	LightType toLight(Structure structure) throws BlenderFileException;

	/**
	 * This method reads converts the given structure into objct. The given structure needs to be filled with the
	 * appropriate data.
	 * @param structure
	 *        the structure we read the object from
	 * @return the object feature
	 */
	ObjectType toObject(Structure structure) throws BlenderFileException;

	/**
	 * This method reads converts the given structure into mesh. The given structure needs to be filled with the
	 * appropriate data.
	 * @param structure
	 *        the structure we read the mesh from
	 * @return the mesh feature
	 */
	MeshType toMesh(Structure structure) throws BlenderFileException;

	/**
	 * This method reads converts the given structure into material. The given structure needs to be filled with the
	 * appropriate data.
	 * @param structure
	 *        the structure we read the material from
	 * @return the material feature
	 */
	MaterialType toMaterial(Structure structure) throws BlenderFileException;
}
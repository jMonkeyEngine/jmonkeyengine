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
package com.jme3.scene.plugins.blender.modifiers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.scene.plugins.blender.AbstractBlenderHelper;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.animations.Ipo;
import com.jme3.scene.plugins.blender.animations.IpoHelper;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;

/**
 * A class that is used in modifiers calculations.
 * 
 * @author Marcin Roguski
 */
public class ModifierHelper extends AbstractBlenderHelper {

	private static final Logger	LOGGER	= Logger.getLogger(ModifierHelper.class.getName());

	/**
	 * This constructor parses the given blender version and stores the result.
	 * Some functionalities may differ in different blender versions.
	 * 
	 * @param blenderVersion
	 *            the version read from the blend file
	 * @param fixUpAxis
	 *            a variable that indicates if the Y asxis is the UP axis or not
	 */
	public ModifierHelper(String blenderVersion, boolean fixUpAxis) {
		super(blenderVersion, fixUpAxis);
	}

	/**
	 * This method reads the given object's modifiers.
	 * 
	 * @param objectStructure
	 *            the object structure
	 * @param blenderContext
	 *            the blender context
	 * @throws BlenderFileException
	 *             this exception is thrown when the blender file is somehow
	 *             corrupted
	 */
	public Collection<Modifier> readModifiers(Structure objectStructure, BlenderContext blenderContext) throws BlenderFileException {
		Collection<Modifier> result = new ArrayList<Modifier>();
		Structure modifiersListBase = (Structure) objectStructure.getFieldValue("modifiers");
		List<Structure> modifiers = modifiersListBase.evaluateListBase(blenderContext);
		for (Structure modifierStructure : modifiers) {
			Modifier modifier = null;
			if (Modifier.ARRAY_MODIFIER_DATA.equals(modifierStructure.getType())) {
				modifier = new ArrayModifier(modifierStructure, blenderContext);
			} else if (Modifier.MIRROR_MODIFIER_DATA.equals(modifierStructure.getType())) {
				modifier = new MirrorModifier(modifierStructure, blenderContext);
			} else if (Modifier.ARMATURE_MODIFIER_DATA.equals(modifierStructure.getType())) {
				modifier = new ArmatureModifier(objectStructure, modifierStructure, blenderContext);
			} else if (Modifier.PARTICLE_MODIFIER_DATA.equals(modifierStructure.getType())) {
				modifier = new ParticlesModifier(modifierStructure, blenderContext);
			}

			if (modifier != null) {
				result.add(modifier);
				blenderContext.addModifier(objectStructure.getOldMemoryAddress(), modifier);
			} else {
				LOGGER.log(Level.WARNING, "Unsupported modifier type: {0}", modifierStructure.getType());
			}
		}

		// at the end read object's animation modifier (object animation is
		// either described by action or by ipo of the object)
		Modifier modifier;
		if (blenderVersion <= 249) {
			modifier = this.readAnimationModifier249(objectStructure, blenderContext);
		} else {
			modifier = this.readAnimationModifier250(objectStructure, blenderContext);
		}
		if (modifier != null) {
			result.add(modifier);
		}
		return result;
	}

	@Override
	public boolean shouldBeLoaded(Structure structure, BlenderContext blenderContext) {
		return true;
	}

	/**
	 * This method reads the object's animation modifier for blender version
	 * 2.49 and lower.
	 * 
	 * @param objectStructure
	 *            the object's structure
	 * @param blenderContext
	 *            the blender context
	 * @return loaded modifier
	 * @throws BlenderFileException
	 *             this exception is thrown when the blender file is somehow
	 *             corrupted
	 */
	private Modifier readAnimationModifier249(Structure objectStructure, BlenderContext blenderContext) throws BlenderFileException {
		Modifier result = null;
		Pointer pAction = (Pointer) objectStructure.getFieldValue("action");
		IpoHelper ipoHelper = blenderContext.getHelper(IpoHelper.class);
		if (pAction.isNotNull()) {
			Structure action = pAction.fetchData(blenderContext.getInputStream()).get(0);
			List<Structure> actionChannels = ((Structure) action.getFieldValue("chanbase")).evaluateListBase(blenderContext);
			if (actionChannels.size() == 1) {// object's animtion action has
												// only one channel
				Pointer pChannelIpo = (Pointer) actionChannels.get(0).getFieldValue("ipo");
				Structure ipoStructure = pChannelIpo.fetchData(blenderContext.getInputStream()).get(0);
				Ipo ipo = ipoHelper.fromIpoStructure(ipoStructure, blenderContext);
				result = new ObjectAnimationModifier(ipo, action.getName(), objectStructure.getOldMemoryAddress(), blenderContext);
				blenderContext.addModifier(objectStructure.getOldMemoryAddress(), result);
			} else {
				throw new IllegalStateException("Object's action cannot have more than one channel!");
			}
		} else {
			Pointer pIpo = (Pointer) objectStructure.getFieldValue("ipo");
			if (pIpo.isNotNull()) {
				Structure ipoStructure = pIpo.fetchData(blenderContext.getInputStream()).get(0);
				Ipo ipo = ipoHelper.fromIpoStructure(ipoStructure, blenderContext);
				result = new ObjectAnimationModifier(ipo, objectStructure.getName(), objectStructure.getOldMemoryAddress(), blenderContext);
				blenderContext.addModifier(objectStructure.getOldMemoryAddress(), result);
			}
		}
		return result;
	}

	/**
	 * This method reads the object's animation modifier for blender version
	 * 2.50 and higher.
	 * 
	 * @param objectStructure
	 *            the object's structure
	 * @param blenderContext
	 *            the blender context
	 * @return loaded modifier
	 * @throws BlenderFileException
	 *             this exception is thrown when the blender file is somehow
	 *             corrupted
	 */
	private Modifier readAnimationModifier250(Structure objectStructure, BlenderContext blenderContext) throws BlenderFileException {
		Modifier result = null;
		Pointer pAnimData = (Pointer) objectStructure.getFieldValue("adt");
		if (pAnimData.isNotNull()) {
			Structure animData = pAnimData.fetchData(blenderContext.getInputStream()).get(0);
			Pointer pAction = (Pointer) animData.getFieldValue("action");
			if (pAction.isNotNull()) {
				Structure actionStructure = pAction.fetchData(blenderContext.getInputStream()).get(0);
				IpoHelper ipoHelper = blenderContext.getHelper(IpoHelper.class);
				Ipo ipo = ipoHelper.fromAction(actionStructure, blenderContext);
				result = new ObjectAnimationModifier(ipo, actionStructure.getName(), objectStructure.getOldMemoryAddress(), blenderContext);
				blenderContext.addModifier(objectStructure.getOldMemoryAddress(), result);
			}
		}
		return result;
	}
}

package com.jme3.scene.plugins.blender.modifiers;

import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.shapes.EmitterMeshVertexShape;
import com.jme3.effect.shapes.EmitterShape;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.materials.MaterialHelper;
import com.jme3.scene.plugins.blender.particles.ParticlesHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This modifier allows to add particles to the object.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
/* package */class ParticlesModifier extends Modifier {
	private static final Logger LOGGER = Logger.getLogger(MirrorModifier.class.getName());
	
	/** Loaded particles emitter. */
	private ParticleEmitter particleEmitter;
	
	/**
	 * This constructor reads the particles system structure and stores it in
	 * order to apply it later to the node.
	 * 
	 * @param modifierStructure
	 *            the structure of the modifier
	 * @param blenderContext
	 *            the blender context
	 * @throws BlenderFileException
	 *             an exception is throw wneh there are problems with the
	 *             blender file
	 */
	public ParticlesModifier(Structure modifierStructure, BlenderContext blenderContext) throws BlenderFileException {
		if(this.validate(modifierStructure, blenderContext)) {
			Pointer pParticleSystem = (Pointer) modifierStructure.getFieldValue("psys");
			if (pParticleSystem.isNotNull()) {
				ParticlesHelper particlesHelper = blenderContext.getHelper(ParticlesHelper.class);
				Structure particleSystem = pParticleSystem.fetchData(blenderContext.getInputStream()).get(0);
				particleEmitter = particlesHelper.toParticleEmitter(particleSystem, blenderContext);
			}
		}
	}

	@Override
	public Node apply(Node node, BlenderContext blenderContext) {
		if(invalid) {
			LOGGER.log(Level.WARNING, "Particles modifier is invalid! Cannot be applied to: {0}", node.getName());
			return node;
		}
		
		MaterialHelper materialHelper = blenderContext.getHelper(MaterialHelper.class);
		ParticleEmitter emitter = particleEmitter.clone();

		// veryfying the alpha function for particles' texture
		Integer alphaFunction = MaterialHelper.ALPHA_MASK_HYPERBOLE;
		char nameSuffix = emitter.getName().charAt(emitter.getName().length() - 1);
		if (nameSuffix == 'B' || nameSuffix == 'N') {
			alphaFunction = MaterialHelper.ALPHA_MASK_NONE;
		}
		// removing the type suffix from the name
		emitter.setName(emitter.getName().substring(0, emitter.getName().length() - 1));

		// applying emitter shape
		EmitterShape emitterShape = emitter.getShape();
		List<Mesh> meshes = new ArrayList<Mesh>();
		for (Spatial spatial : node.getChildren()) {
			if (spatial instanceof Geometry) {
				Mesh mesh = ((Geometry) spatial).getMesh();
				if (mesh != null) {
					meshes.add(mesh);
					Material material = materialHelper.getParticlesMaterial(
							((Geometry) spatial).getMaterial(), alphaFunction, blenderContext);
					emitter.setMaterial(material);// TODO: divide into several pieces
				}
			}
		}
		if (meshes.size() > 0 && emitterShape instanceof EmitterMeshVertexShape) {
			((EmitterMeshVertexShape) emitterShape).setMeshes(meshes);
		}

		node.attachChild(emitter);
		return node;
	}

	@Override
	public String getType() {
		return Modifier.PARTICLE_MODIFIER_DATA;
	}
}

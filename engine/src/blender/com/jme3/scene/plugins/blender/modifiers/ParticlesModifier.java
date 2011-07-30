package com.jme3.scene.plugins.blender.modifiers;

import java.util.ArrayList;
import java.util.List;

import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.shapes.EmitterMeshVertexShape;
import com.jme3.effect.shapes.EmitterShape;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.blender.DataRepository;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.materials.MaterialHelper;
import com.jme3.scene.plugins.blender.particles.ParticlesHelper;

/**
 * This modifier allows to add particles to the object.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
/* package */class ParticlesModifier extends Modifier {

	/**
	 * This constructor reads the particles system structure and stores it in
	 * order to apply it later to the node.
	 * 
	 * @param modifier
	 *            the structure of the modifier
	 * @param dataRepository
	 *            the data repository
	 * @throws BlenderFileException
	 *             an exception is throw wneh there are problems with the
	 *             blender file
	 */
	public ParticlesModifier(Structure modifier, DataRepository dataRepository)
			throws BlenderFileException {
		Pointer pParticleSystem = (Pointer) modifier.getFieldValue("psys");
		if (pParticleSystem.isNotNull()) {
			ParticlesHelper particlesHelper = dataRepository
					.getHelper(ParticlesHelper.class);
			Structure particleSystem = pParticleSystem.fetchData(
					dataRepository.getInputStream()).get(0);
			jmeModifierRepresentation = particlesHelper.toParticleEmitter(
					particleSystem, dataRepository);
		}
	}

	@Override
	public Node apply(Node node, DataRepository dataRepository) {
		MaterialHelper materialHelper = dataRepository
				.getHelper(MaterialHelper.class);
		ParticleEmitter emitter = (ParticleEmitter) jmeModifierRepresentation;
		emitter = emitter.clone();

		// veryfying the alpha function for particles' texture
		Integer alphaFunction = MaterialHelper.ALPHA_MASK_HYPERBOLE;
		char nameSuffix = emitter.getName().charAt(
				emitter.getName().length() - 1);
		if (nameSuffix == 'B' || nameSuffix == 'N') {
			alphaFunction = MaterialHelper.ALPHA_MASK_NONE;
		}
		// removing the type suffix from the name
		emitter.setName(emitter.getName().substring(0,
				emitter.getName().length() - 1));

		// applying emitter shape
		EmitterShape emitterShape = emitter.getShape();
		List<Mesh> meshes = new ArrayList<Mesh>();
		for (Spatial spatial : node.getChildren()) {
			if (spatial instanceof Geometry) {
				Mesh mesh = ((Geometry) spatial).getMesh();
				if (mesh != null) {
					meshes.add(mesh);
					Material material = materialHelper.getParticlesMaterial(
							((Geometry) spatial).getMaterial(), alphaFunction,
							dataRepository);
					emitter.setMaterial(material);// TODO: divide into several
													// pieces
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

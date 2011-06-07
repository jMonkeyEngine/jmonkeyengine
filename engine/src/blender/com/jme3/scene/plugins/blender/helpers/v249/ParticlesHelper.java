package com.jme3.scene.plugins.blender.helpers.v249;

import java.util.logging.Logger;

import com.jme3.effect.EmitterMeshConvexHullShape;
import com.jme3.effect.EmitterMeshFaceShape;
import com.jme3.effect.EmitterMeshVertexShape;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh.Type;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.plugins.blender.data.Structure;
import com.jme3.scene.plugins.blender.exception.BlenderFileException;
import com.jme3.scene.plugins.blender.utils.AbstractBlenderHelper;
import com.jme3.scene.plugins.blender.utils.DataRepository;
import com.jme3.scene.plugins.blender.utils.DynamicArray;
import com.jme3.scene.plugins.blender.utils.Pointer;

public class ParticlesHelper extends AbstractBlenderHelper {
	private static final Logger			LOGGER		= Logger.getLogger(ParticlesHelper.class.getName());
	
	// part->type
	public static final int PART_EMITTER	=	0;
	public static final int PART_REACTOR	=	1;
	public static final int PART_HAIR		=	2;
	public static final int PART_FLUID		=	3;
	
	// part->flag
	public static final int PART_REACT_STA_END	=1;
	public static final int PART_REACT_MULTIPLE	=2;
	public static final int PART_LOOP			=4;
	//public static final int PART_LOOP_INSTANT	=8;
	public static final int PART_HAIR_GEOMETRY	=16;
	public static final int PART_UNBORN			=32;		//show unborn particles
	public static final int PART_DIED			=64;		//show died particles
	public static final int PART_TRAND			=128;	
	public static final int PART_EDISTR			=256;		// particle/face from face areas
	public static final int PART_STICKY			=512;		//collided particles can stick to collider
	public static final int PART_DIE_ON_COL		=1<<12;
	public static final int PART_SIZE_DEFL		=1<<13; 	// swept sphere deflections
	public static final int PART_ROT_DYN		=1<<14;	// dynamic rotation
	public static final int PART_SIZEMASS		=1<<16;
	public static final int PART_ABS_LENGTH		=1<<15;
	public static final int PART_ABS_TIME		=1<<17;
	public static final int PART_GLOB_TIME		=1<<18;
	public static final int PART_BOIDS_2D		=1<<19;
	public static final int PART_BRANCHING		=1<<20;
	public static final int PART_ANIM_BRANCHING	=1<<21;
	public static final int PART_SELF_EFFECT	=1<<22;
	public static final int PART_SYMM_BRANCHING	=1<<24;
	public static final int PART_HAIR_BSPLINE	=1024;
	public static final int PART_GRID_INVERT	=1<<26;
	public static final int PART_CHILD_EFFECT	=1<<27;
	public static final int PART_CHILD_SEAMS	=1<<28;
	public static final int PART_CHILD_RENDER	=1<<29;
	public static final int PART_CHILD_GUIDE	=1<<30;
	
	// part->from
	public static final int PART_FROM_VERT		=0;
	public static final int PART_FROM_FACE		=1;
	public static final int PART_FROM_VOLUME	=2;
	public static final int PART_FROM_PARTICLE	=3;
	public static final int PART_FROM_CHILD		=4;
	
	/**
	 * This constructor parses the given blender version and stores the result. Some functionalities may differ in
	 * different blender versions.
	 * @param blenderVersion
	 *        the version read from the blend file
	 */
	public ParticlesHelper(String blenderVersion) {
		super(blenderVersion);
	}

	@SuppressWarnings("unchecked")
	public ParticleEmitter toParticleEmitter(Structure particleSystem, DataRepository dataRepository) throws BlenderFileException {
		ParticleEmitter result = null;
		Pointer pParticleSettings = (Pointer) particleSystem.getFieldValue("part");
		if(!pParticleSettings.isNull()) {
			Structure particleSettings = pParticleSettings.fetchData(dataRepository.getInputStream()).get(0);
			int totPart = ((Number) particleSettings.getFieldValue("totpart")).intValue();
			result = new ParticleEmitter(particleSettings.getName(), Type.Triangle, totPart);
			
			//setting the emitters shape (the shapes meshes will be set later during modifier applying operation)
			int from = ((Number)particleSettings.getFieldValue("from")).intValue();
			switch(from) {
				case PART_FROM_VERT:
					result.setShape(new EmitterMeshVertexShape());
					break;
				case PART_FROM_FACE:
					result.setShape(new EmitterMeshFaceShape());
					break;
				case PART_FROM_VOLUME:
					result.setShape(new EmitterMeshConvexHullShape());
					break;
				default:
					LOGGER.warning("Default shape used! Unknown emitter shape value ('from' parameter): " + from);
			}
			
			//reading acceleration
			DynamicArray<Number> acc = (DynamicArray<Number>) particleSettings.getFieldValue("acc");
			result.setInitialVelocity(new Vector3f(acc.get(0).floatValue(), acc.get(1).floatValue(), acc.get(2).floatValue()));
			result.setGravity(0);//by default gravity is set to 0.1f so we need to disable it completely
			// 2x2 texture animation
			result.setImagesX(2);
			result.setImagesY(2);
			result.setEndColor(new ColorRGBA(1f, 0f, 0f, 1f));   // red
			result.setStartColor(new ColorRGBA(1f, 1f, 0f, 0.5f)); // yellow
			result.setStartSize(1.5f);
			result.setEndSize(0.1f);
			
			result.setLowLife(0.5f);
		    result.setHighLife(3f);
		    result.setVelocityVariation(0.3f);
		}
		return result;
	}
}

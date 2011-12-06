package com.jme3.scene.plugins.blender.particles;

import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh.Type;
import com.jme3.effect.influencers.EmptyParticleInfluencer;
import com.jme3.effect.influencers.NewtonianParticleInfluencer;
import com.jme3.effect.influencers.ParticleInfluencer;
import com.jme3.effect.shapes.EmitterMeshConvexHullShape;
import com.jme3.effect.shapes.EmitterMeshFaceShape;
import com.jme3.effect.shapes.EmitterMeshVertexShape;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.plugins.blender.AbstractBlenderHelper;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.DynamicArray;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import java.util.logging.Logger;

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
	
	// part->phystype
	public static final int PART_PHYS_NO	=	0;
	public static final int PART_PHYS_NEWTON=	1;
	public static final int PART_PHYS_KEYED	=	2;
	public static final int PART_PHYS_BOIDS	=	3;
	
	// part->draw_as
	public static final int PART_DRAW_NOT	=	0;
	public static final int PART_DRAW_DOT	=	1;
	public static final int PART_DRAW_CIRC	=	2;
	public static final int PART_DRAW_CROSS	=	3;
	public static final int PART_DRAW_AXIS	=	4;
	public static final int PART_DRAW_LINE	=	5;
	public static final int PART_DRAW_PATH	=	6;
	public static final int PART_DRAW_OB	=	7;
	public static final int PART_DRAW_GR	=	8;
	public static final int PART_DRAW_BB	=	9;
	
	/**
	 * This constructor parses the given blender version and stores the result. Some functionalities may differ in
	 * different blender versions.
	 * @param blenderVersion
	 *        the version read from the blend file
	 * @param fixUpAxis
     *        a variable that indicates if the Y asxis is the UP axis or not
	 */
	public ParticlesHelper(String blenderVersion, boolean fixUpAxis) {
		super(blenderVersion, fixUpAxis);
	}

	@SuppressWarnings("unchecked")
	public ParticleEmitter toParticleEmitter(Structure particleSystem, BlenderContext blenderContext) throws BlenderFileException {
		ParticleEmitter result = null;
		Pointer pParticleSettings = (Pointer) particleSystem.getFieldValue("part");
		if(pParticleSettings.isNotNull()) {
			Structure particleSettings = pParticleSettings.fetchData(blenderContext.getInputStream()).get(0);
			
			int totPart = ((Number) particleSettings.getFieldValue("totpart")).intValue();
			
			//draw type will be stored temporarily in the name (it is used during modifier applying operation)
			int drawAs = ((Number)particleSettings.getFieldValue("draw_as")).intValue();
			char nameSuffix;//P - point, L - line, N - None, B - Bilboard
			switch(drawAs) {
				case PART_DRAW_NOT:
					nameSuffix = 'N';
					totPart = 0;//no need to generate particles in this case
					break;
				case PART_DRAW_BB:
					nameSuffix = 'B';
					break;
				case PART_DRAW_OB:
				case PART_DRAW_GR:
					nameSuffix = 'P';
					LOGGER.warning("Neither object nor group particles supported yet! Using point representation instead!");//TODO: support groups and aobjects
					break;
				case PART_DRAW_LINE:
					nameSuffix = 'L';
					LOGGER.warning("Lines not yet supported! Using point representation instead!");//TODO: support lines
				default://all others are rendered as points in blender
					nameSuffix = 'P';
			}
			result = new ParticleEmitter(particleSettings.getName()+nameSuffix, Type.Triangle, totPart);
			if(nameSuffix=='N') {
				return result;//no need to set anything else
			}
			
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
					LOGGER.warning("Default shape used! Unknown emitter shape value ('from' parameter: " + from + ')');
			}
			
			//reading acceleration
			DynamicArray<Number> acc = (DynamicArray<Number>) particleSettings.getFieldValue("acc");
			result.setGravity(-acc.get(0).floatValue(), -acc.get(1).floatValue(), -acc.get(2).floatValue());
			
			//setting the colors
			result.setEndColor(new ColorRGBA(1f, 1f, 1f, 1f));
			result.setStartColor(new ColorRGBA(1f, 1f, 1f, 1f));
			
			//reading size
			float sizeFactor = nameSuffix=='B' ? 1.0f : 0.3f;
			float size = ((Number)particleSettings.getFieldValue("size")).floatValue() * sizeFactor;
			result.setStartSize(size);
			result.setEndSize(size);
			
			//reading lifetime
			int fps = blenderContext.getBlenderKey().getFps();
			float lifetime = ((Number)particleSettings.getFieldValue("lifetime")).floatValue() / fps;
			float randlife = ((Number)particleSettings.getFieldValue("randlife")).floatValue() / fps;
			result.setLowLife(lifetime * (1.0f - randlife));
		    result.setHighLife(lifetime);
		    
		    //preparing influencer
		    ParticleInfluencer influencer;
		    int phystype = ((Number)particleSettings.getFieldValue("phystype")).intValue();
		    switch(phystype) {
		    	case PART_PHYS_NEWTON:
		    		influencer = new NewtonianParticleInfluencer();
		    		((NewtonianParticleInfluencer)influencer).setNormalVelocity(((Number)particleSettings.getFieldValue("normfac")).floatValue());
		    		((NewtonianParticleInfluencer)influencer).setVelocityVariation(((Number)particleSettings.getFieldValue("randfac")).floatValue());
		    		((NewtonianParticleInfluencer)influencer).setSurfaceTangentFactor(((Number)particleSettings.getFieldValue("tanfac")).floatValue());
		    		((NewtonianParticleInfluencer)influencer).setSurfaceTangentRotation(((Number)particleSettings.getFieldValue("tanphase")).floatValue());
		    		break;
		    	case PART_PHYS_BOIDS:
		    	case PART_PHYS_KEYED://TODO: support other influencers
		    		LOGGER.warning("Boids and Keyed particles physic not yet supported! Empty influencer used!");
		    	case PART_PHYS_NO:
	    		default:
	    			influencer = new EmptyParticleInfluencer();
		    }
		    result.setParticleInfluencer(influencer);
		}
		return result;
	}
	
	@Override
	public boolean shouldBeLoaded(Structure structure, BlenderContext blenderContext) {
		return true;
	}
}

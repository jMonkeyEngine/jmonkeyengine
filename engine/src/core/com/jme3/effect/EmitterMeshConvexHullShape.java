package com.jme3.effect;

import java.util.List;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;

/**
 * This emiter shape emits the particles from the given shape's interior constrained by its convex hull
 * (a geometry that tightly wraps the mesh). So in case of multiple meshes some vertices may appear
 * in a space between them.
 * @author Marcin Roguski (Kaelthas)
 */
public class EmitterMeshConvexHullShape extends EmitterMeshFaceShape {
	/**
	 * Empty constructor. Sets nothing.
	 */
	public EmitterMeshConvexHullShape() {}
	
	/**
	 * Constructor. It stores a copy of vertex list of all meshes.
	 * @param meshes a list of meshes that will form the emitter's shape
	 */
	public EmitterMeshConvexHullShape(List<Mesh> meshes) {
		super(meshes);
	}

	@Override
	public void getRandomPoint(Vector3f store) {
		super.getRandomPoint(store);
		//now move the point from the meshe's face towards the center of the mesh
		//the center is in (0, 0, 0) in the local coordinates
		store.multLocal(FastMath.nextRandomFloat());
	}
}

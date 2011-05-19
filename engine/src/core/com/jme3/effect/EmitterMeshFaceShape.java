package com.jme3.effect;

import java.util.List;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;

/**
 * This emiter shape emits the particles from the given shape's faces.
 * @author Marcin Roguski (Kaelthas)
 */
public class EmitterMeshFaceShape extends EmitterMeshVertexShape {
	/**
	 * Empty constructor. Sets nothing.
	 */
	public EmitterMeshFaceShape() {}
	
	/**
	 * Constructor. It stores a copy of vertex list of all meshes.
	 * @param meshes a list of meshes that will form the emitter's shape
	 */
	public EmitterMeshFaceShape(List<Mesh> meshes) {
		super(meshes);
	}
	
	@Override
	public void getRandomPoint(Vector3f store) {
		int meshIndex = FastMath.nextRandomInt(0, vertices.length-1);
		//the index of the first vertex of a face (must be dividable by 9)
		int vertIndex = FastMath.nextRandomInt(0, vertices[meshIndex].length / 9 - 1) * 9;
		//put the point somewhere between the first and the second vertex of a face
		float moveFactor = FastMath.nextRandomFloat();
		store.set(vertices[meshIndex][vertIndex] + (vertices[meshIndex][vertIndex + 3] - vertices[meshIndex][vertIndex]) * moveFactor, 
				  vertices[meshIndex][vertIndex + 1] + (vertices[meshIndex][vertIndex + 4] - vertices[meshIndex][vertIndex + 1]) * moveFactor, 
				  vertices[meshIndex][vertIndex + 2] + (vertices[meshIndex][vertIndex + 5] - vertices[meshIndex][vertIndex + 2]) * moveFactor);
		//move the result towards the last face vertex
		moveFactor = FastMath.nextRandomFloat();
		store.addLocal((vertices[meshIndex][vertIndex + 6] - store.x) * moveFactor,
			  	  (vertices[meshIndex][vertIndex + 7] - store.y) * moveFactor,
				  (vertices[meshIndex][vertIndex + 8] - store.z) * moveFactor);
	}
}

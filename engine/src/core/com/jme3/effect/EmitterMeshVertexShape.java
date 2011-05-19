package com.jme3.effect;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.List;

import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;

/**
 * This emiter shape emits the particles from the given shape's vertices
 * @author Marcin Roguski (Kaelthas)
 */
public class EmitterMeshVertexShape implements EmitterShape {
	protected float[][] vertices;
	
	/**
	 * Empty constructor. Sets nothing.
	 */
	public EmitterMeshVertexShape() {}
	
	/**
	 * Constructor. It stores a copy of vertex list of all meshes.
	 * @param meshes a list of meshes that will form the emitter's shape
	 */
	public EmitterMeshVertexShape(List<Mesh> meshes) {
		this.setMeshes(meshes);
	}
	
	/**
	 * This method sets the meshes that will form the emiter's shape.
	 * @param meshes a list of meshes that will form the emitter's shape
	 */
	public void setMeshes(List<Mesh> meshes) {
		this.vertices = new float[meshes.size()][];
		int i=0;
		for(Mesh mesh : meshes) {
			FloatBuffer floatBuffer = mesh.getFloatBuffer(Type.Position);
			vertices[i++] = BufferUtils.getFloatArray(floatBuffer);
		}
	}
	
	@Override
	public void getRandomPoint(Vector3f store) {
		int meshIndex = FastMath.nextRandomInt(0, vertices.length-1);
		int vertIndex = FastMath.nextRandomInt(0, vertices[meshIndex].length / 3 - 1) * 3;
		store.set(vertices[meshIndex][vertIndex], vertices[meshIndex][vertIndex + 1], vertices[meshIndex][vertIndex + 2]);
	}

	@Override
	public EmitterShape deepClone() {
		try {
			EmitterMeshVertexShape clone = (EmitterMeshVertexShape) super.clone();
            clone.vertices = vertices==null ? null : vertices.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
	}
	
	@Override
	public void write(JmeExporter ex) throws IOException {
		OutputCapsule oc = ex.getCapsule(this);
        oc.write(vertices, "vertices", null);
	}

	@Override
	public void read(JmeImporter im) throws IOException {
		this.vertices = im.getCapsule(this).readFloatArray2D("vertices", null);
	}
}

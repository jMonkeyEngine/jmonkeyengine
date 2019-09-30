/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.particles.particle;

import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.mesh.IndexBuffer;

import java.nio.FloatBuffer;

/**
 *
 * @author t0neg0d
 */
public final class MeshUtils {
	
	public static FloatBuffer getPositionBuffer(Mesh mesh) {
		return mesh.getFloatBuffer(VertexBuffer.Type.Position);
	}
	
	public static IndexBuffer getIndexBuffer(Mesh mesh) {
		return mesh.getIndexBuffer();
	}
	
	public static FloatBuffer getTexCoordBuffer(Mesh mesh) {
		return mesh.getFloatBuffer(VertexBuffer.Type.TexCoord);
	}
  
	public static VertexBuffer getColorBuffer(Mesh mesh) {
		return mesh.getBuffer(VertexBuffer.Type.Color);
	}
	
	public static FloatBuffer getNormalsBuffer(Mesh mesh) {
		return mesh.getFloatBuffer(VertexBuffer.Type.Normal);
	}
}

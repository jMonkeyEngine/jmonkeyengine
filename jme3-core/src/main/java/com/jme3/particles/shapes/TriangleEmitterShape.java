/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.particles.shapes;

import com.jme3.math.Triangle;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 *
 * @author t0neg0d
 */
public class TriangleEmitterShape extends Mesh {
	private FloatBuffer verts = BufferUtils.createFloatBuffer(9);
	private ShortBuffer indexes = BufferUtils.createShortBuffer(3);
	private FloatBuffer normals = BufferUtils.createFloatBuffer(9);
	Vector3f p1 = new Vector3f(), p2 = new Vector3f(), p3 = new Vector3f();
	Triangle t = new Triangle();
	float size;
	
	public TriangleEmitterShape(float size) {
		this.size = size;
		
		p1.set(-(size/2),0,(size/2));
		p2.set((size/2),0,-(size/2));
		p3.set(-(size/2),0,-(size/2));
		
		t.set(p1, p2, p3);
		t.calculateCenter();
		
		p1.subtractLocal(t.getCenter());
		p2.subtractLocal(t.getCenter());
		p3.subtractLocal(t.getCenter());
		
		verts.put(p1.getX());
		verts.put(p1.getY());
		verts.put(p1.getZ());
		verts.put(p2.getX());
		verts.put(p2.getY());
		verts.put(p2.getZ());
		verts.put(p3.getX());
		verts.put(p3.getY());
		verts.put(p3.getZ());
		
		normals.put(0);
		normals.put(1);
		normals.put(0);
		normals.put(0);
		normals.put(1);
		normals.put(0);
		normals.put(0);
		normals.put(1);
		normals.put(0);
		
		indexes.put((short)0);
		indexes.put((short)1);
		indexes.put((short)2);
		
		
		this.clearBuffer(VertexBuffer.Type.Position);
		this.setBuffer(VertexBuffer.Type.Position, 3, verts);
		this.clearBuffer(VertexBuffer.Type.Index);
		this.setBuffer(VertexBuffer.Type.Index, 3, indexes);
		this.clearBuffer(VertexBuffer.Type.Normal);
		this.setBuffer(VertexBuffer.Type.Normal, 3, normals);
		
		createCollisionData();
		updateBound();
	}
}

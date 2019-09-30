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
package com.jme3.particles.emittershapes;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Triangle;
import com.jme3.math.Vector3f;
import com.jme3.particles.EmitterShape;
import com.jme3.particles.shapes.TriangleEmitterShape;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Sphere;

import java.io.IOException;

/**
 * EmitterMesh
 *
 * @author t0neg0d
 */
public class EmitterMesh extends EmitterShape {
	private Mesh mesh;
	private int triangleIndex;
	private Triangle triStore = new Triangle();
	Vector3f p1 = new Vector3f();
	Vector3f p2 = new Vector3f();
	Vector3f p3 = new Vector3f();
	Vector3f a = new Vector3f();
	Vector3f b = new Vector3f();
	Vector3f result = new Vector3f();
	private int triCount;

	public EmitterMesh() {

	}

	public EmitterMesh(Mesh mesh) {
		setShape(mesh);
	}

	@Override
	public Spatial getDebugShape(Material mat, boolean ignoreTransforms) {
		Geometry geometry = new Geometry("DebugShape", mesh);
		geometry.setMaterial(mat);
		//geometry.setIgnoreTransform(ignoreTransforms);
		return geometry;
	}

	/**
	 * Sets the mesh to use as the particles shape
	 * @param mesh The mesh to use as the particles shape
	 */
	public final void setShape(Mesh mesh) {
		this.mesh = mesh;
		triCount = mesh.getTriangleCount();
	}
	
	/**
	 * Returns the mesh used as the particle particles shape
	 * @return The particle particles shape mesh
	 */
	public Mesh getMesh() {
		return this.mesh;
	}
	
	/**
	 * Selects a random face as the next particle emission point
	 */
	public void setNext() {
		triangleIndex = FastMath.rand.nextInt(triCount);
		mesh.getTriangle(triangleIndex, triStore);
		triStore.calculateCenter();
		triStore.calculateNormal();
	}
	
	/**
	 * Set the current particle emission face to the specified faces index
	 * @param triangleIndex The index of the face to set as the particle emission point
	 */
	public void setNext(int triangleIndex) {
		mesh.getTriangle(triangleIndex, triStore);
		triStore.calculateCenter();
		triStore.calculateNormal();
	}
	
	/**
	 * Returns the index of the current face being used as the particle emission point
	 * @return 
	 */
	public int getIndex() {
		return triangleIndex;
	}
	
	/**
	 * Returns the local position of the center of the selected face
	 * @return A Vector3f representing the local translation of the selected emission point
	 */
	public Vector3f getNextTranslation(){
		return triStore.getCenter();
	}
	
	public Vector3f getRandomTranslation() {
		p1.set(triStore.get1().subtract(triStore.getCenter()));
		p2.set(triStore.get2().subtract(triStore.getCenter()));
		p3.set(triStore.get3().subtract(triStore.getCenter()));
		
		a.interpolateLocal(p1, p2, 1f- FastMath.rand.nextFloat());
		b.interpolateLocal(p1, p3, 1f- FastMath.rand.nextFloat());
		result.interpolateLocal(a,b, FastMath.rand.nextFloat());
		
		return result;
		/*
		return (p1.interpolate(p2, FastMath.rand.nextFloat()))
			.addLocal(p4.interpolate(p3, FastMath.rand.nextFloat()))
			.divideLocal(2f);
		*/
	}
	
	/**
	 * Returns the normal of the selected emission point
	 * @return A Vector3f containing the normal of the selected emission point
	 */
	public Vector3f getNextDirection(){
		return triStore.getNormal();
	}

	@Override
	public void write(JmeExporter ex) throws IOException {
		super.write(ex);
		OutputCapsule oc = ex.getCapsule(this);
		oc.write(mesh, "mesh", new TriangleEmitterShape(1));
	}

	@Override
	public void read(JmeImporter im) throws IOException {
		super.read(im);
		InputCapsule ic = im.getCapsule(this);
		mesh = (Mesh)ic.readSavable("mesh", new TriangleEmitterShape(1));
		triCount = mesh.getTriangleCount();

	}


	public boolean equals(Object o) {
		if (!super.equals(o)) return false;
		if (!(o instanceof EmitterMesh)) return false;

		EmitterMesh check = (EmitterMesh)o;

		if (mesh != null && !mesh.equals(check.mesh) || mesh == null && check.mesh != null) return false;

		return true;
	}
}

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
package com.jme3.bullet.collision.shapes;

import com.bulletphysics.dom.HeightfieldTerrainShape;
import com.jme3.bullet.util.Converter;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import java.io.IOException;

/**
 * Uses Bullet Physics Heightfield terrain collision system. This is MUCH faster
 * than using a regular mesh.
 * There are a couple tricks though:
 *	-No rotation or translation is supported.
 *	-The collision bbox must be centered around 0,0,0 with the height above and below the y-axis being
 *	equal on either side. If not, the whole collision box is shifted vertically and things don't collide
 *	as they should.
 * 
 * @author Brent Owens
 */
public class HeightfieldCollisionShape extends CollisionShape {

	//protected HeightfieldTerrainShape heightfieldShape;
	protected int heightStickWidth;
	protected int heightStickLength;
	protected float[] heightfieldData;
	protected float heightScale;
	protected float minHeight;
	protected float maxHeight;
	protected int upAxis;
	protected boolean flipQuadEdges;

	public HeightfieldCollisionShape() {

	}

	public HeightfieldCollisionShape(float[] heightmap) {
		createCollisionHeightfield(heightmap, Vector3f.UNIT_XYZ);
	}

	public HeightfieldCollisionShape(float[] heightmap, Vector3f scale) {
		createCollisionHeightfield(heightmap, scale);
	}

	protected void createCollisionHeightfield(float[] heightmap, Vector3f worldScale) {
		this.scale = worldScale;
		this.heightScale = 1;//don't change away from 1, we use worldScale instead to scale
		
		this.heightfieldData = heightmap;

		float min = heightfieldData[0];
		float max = heightfieldData[0];
		// calculate min and max height
		for (int i=0; i<heightfieldData.length; i++) {
			if (heightfieldData[i] < min)
				min = heightfieldData[i];
			if (heightfieldData[i] > max)
				max = heightfieldData[i];
		}
		// we need to center the terrain collision box at 0,0,0 for BulletPhysics. And to do that we need to set the
		// min and max height to be equal on either side of the y axis, otherwise it gets shifted and collision is incorrect.
		if (max < 0)
			max = -min;
		else {
			if (Math.abs(max) > Math.abs(min))
				min = -max;
			else
				max = -min;
		}
		this.minHeight = min;
		this.maxHeight = max;

		this.upAxis = HeightfieldTerrainShape.YAXIS;
		this.flipQuadEdges = false;

		heightStickWidth = (int) FastMath.sqrt(heightfieldData.length);
		heightStickLength = heightStickWidth;


		createShape();
	}
	
	protected void createShape() {

		HeightfieldTerrainShape shape = new HeightfieldTerrainShape(heightStickWidth, heightStickLength, heightfieldData, heightScale, minHeight, maxHeight, upAxis, flipQuadEdges);
		shape.setLocalScaling(new javax.vecmath.Vector3f(scale.x, scale.y, scale.z));
		cShape = shape;
		cShape.setLocalScaling(Converter.convert(getScale()));
                cShape.setMargin(margin);
	}

	public Mesh createJmeMesh(){
        //TODO return Converter.convert(bulletMesh);
		return null;
    }

    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(heightStickWidth, "heightStickWidth", 0);
        capsule.write(heightStickLength, "heightStickLength", 0);
        capsule.write(heightScale, "heightScale", 0);
        capsule.write(minHeight, "minHeight", 0);
        capsule.write(maxHeight, "maxHeight", 0);
        capsule.write(upAxis, "upAxis", 1);
        capsule.write(heightfieldData, "heightfieldData", new float[0]);
        capsule.write(flipQuadEdges, "flipQuadEdges", false);
    }

    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule capsule = im.getCapsule(this);
        heightStickWidth = capsule.readInt("heightStickWidth", 0);
        heightStickLength = capsule.readInt("heightStickLength", 0);
        heightScale = capsule.readFloat("heightScale", 0);
        minHeight = capsule.readFloat("minHeight", 0);
        maxHeight = capsule.readFloat("maxHeight", 0);
        upAxis = capsule.readInt("upAxis", 1);
        heightfieldData = capsule.readFloatArray("heightfieldData", new float[0]);
        flipQuadEdges = capsule.readBoolean("flipQuadEdges", false);
        createShape();
    }

}

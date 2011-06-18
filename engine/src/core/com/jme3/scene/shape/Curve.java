/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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
package com.jme3.scene.shape;

import com.jme3.math.Spline;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import java.util.Iterator;
import java.util.List;

/**
 * A <code>Curve</code> is a visual, line-based representation of a {@link Spline}.
 * The underlying Spline will be sampled N times where N is the number of 
 * segments as specified in the constructor. Each segment will represent
 * one line in the generated mesh.
 * 
 * @author Nehon
 */
public class Curve extends Mesh {

    private Spline spline;
    private Vector3f temp = new Vector3f();

    /**
     * Serialization only. Do not use.
     */
    public Curve(){
    }
    
    /**
     * Create a curve mesh.
     * Use a CatmullRom spline model that does not cycle.
     * 
     * @param controlPoints the control points to use to create this curve
     * @param nbSubSegments the number of subsegments between the control points
     */
    public Curve(Vector3f[] controlPoints, int nbSubSegments) {
        this(new Spline(Spline.SplineType.CatmullRom, controlPoints, 10, false), nbSubSegments);
    }

    /**
     * Create a curve mesh from a Spline
     * 
     * @param spline the spline to use
     * @param nbSubSegments the number of subsegments between the control points
     */
    public Curve(Spline spline, int nbSubSegments) {
        super();
        this.spline = spline;
        switch (spline.getType()) {
            case CatmullRom:
            	this.createCatmullRomMesh(nbSubSegments);
                break;
            case Bezier:
            	this.createBezierMesh(nbSubSegments);
            	break;
            case Nurb:
            	this.createNurbMesh(nbSubSegments);
            	break;
            case Linear:
            default:
            	this.createLinearMesh();
                break;
        }
    }

    private void createCatmullRomMesh(int nbSubSegments) {
        float[] array = new float[((spline.getControlPoints().size() - 1) * nbSubSegments + 1) * 3];
        short[] indices = new short[(spline.getControlPoints().size() - 1) * nbSubSegments * 2];
        int i = 0;
        int cptCP = 0;
        for (Iterator<Vector3f> it = spline.getControlPoints().iterator(); it.hasNext();) {
            Vector3f vector3f = it.next();
            array[i] = vector3f.x;
            i++;
            array[i] = vector3f.y;
            i++;
            array[i] = vector3f.z;
            i++;
            if (it.hasNext()) {
                for (int j = 1; j < nbSubSegments; j++) {
                    spline.interpolate((float) j / nbSubSegments, cptCP, temp);
                    array[i] = temp.getX();
                    i++;
                    array[i] = temp.getY();
                    i++;
                    array[i] = temp.getZ();
                    i++;
                }
            }
            cptCP++;
        }

        i = 0;
        int k = 0;
        for (int j = 0; j < (spline.getControlPoints().size() - 1) * nbSubSegments; j++) {
            k = j;
            indices[i] = (short) k;
            i++;
            k++;
            indices[i] = (short) k;
            i++;
        }

        this.setMode(Mesh.Mode.Lines);
        this.setBuffer(VertexBuffer.Type.Position, 3, array);
        this.setBuffer(VertexBuffer.Type.Index, 2, indices);//(spline.getControlPoints().size() - 1) * nbSubSegments * 2
        this.updateBound();
        this.updateCounts();
    }

    /**
	 * This method creates the Bezier path for this curve.
	 * 
	 * @param nbSubSegments
	 *            amount of subsegments between position control points
	 */
	private void createBezierMesh(int nbSubSegments) {
		if(nbSubSegments==0) {
			nbSubSegments = 1;
		}
		int centerPointsAmount = (spline.getControlPoints().size() + 2) / 3;
		
		//calculating vertices
		float[] array = new float[((centerPointsAmount - 1) * nbSubSegments + 1) * 3];
		int currentControlPoint = 0;
		List<Vector3f> controlPoints = spline.getControlPoints();
		int lineIndex = 0;
		for (int i = 0; i < centerPointsAmount - 1; ++i) {
			Vector3f vector3f = controlPoints.get(currentControlPoint);
			array[lineIndex++] = vector3f.x;
			array[lineIndex++] = vector3f.y;
			array[lineIndex++] = vector3f.z;
			for (int j = 1; j < nbSubSegments; ++j) {
				spline.interpolate((float) j / nbSubSegments, currentControlPoint, temp);
				array[lineIndex++] = temp.getX();
				array[lineIndex++] = temp.getY();
				array[lineIndex++] = temp.getZ();
			}
			currentControlPoint += 3;
		}
		Vector3f vector3f = controlPoints.get(currentControlPoint);
		array[lineIndex++] = vector3f.x;
		array[lineIndex++] = vector3f.y;
		array[lineIndex++] = vector3f.z;

		//calculating indexes
		int i = 0, k = 0;
		short[] indices = new short[(centerPointsAmount - 1) * nbSubSegments << 1];
		for (int j = 0; j < (centerPointsAmount - 1) * nbSubSegments; ++j) {
			k = j;
			indices[i++] = (short) k;
			++k;
			indices[i++] = (short) k;
		}

		this.setMode(Mesh.Mode.Lines);
		this.setBuffer(VertexBuffer.Type.Position, 3, array);
		this.setBuffer(VertexBuffer.Type.Index, 2, indices);
		this.updateBound();
		this.updateCounts();
	}
	
	/**
	 * This method creates the Nurb path for this curve.
	 * @param nbSubSegments
	 *            amount of subsegments between position control points
	 */
	private void createNurbMesh(int nbSubSegments) {
		float minKnot = spline.getMinNurbKnot();
		float maxKnot = spline.getMaxNurbKnot();
		float deltaU = (maxKnot - minKnot)/nbSubSegments;
		
		float[] array = new float[(nbSubSegments + 1) * 3];
		
		float u = minKnot;
		Vector3f interpolationResult = new Vector3f();
		for(int i=0;i<array.length;i+=3) {
			spline.interpolate(u, 0, interpolationResult);
			array[i] = interpolationResult.x;
			array[i + 1] = interpolationResult.y;
			array[i + 2] = interpolationResult.z;
			u += deltaU;
		}
		
		//calculating indexes
		int i = 0;
		short[] indices = new short[nbSubSegments << 1];
		for (int j = 0; j < nbSubSegments; ++j) {
			indices[i++] = (short) j;
			indices[i++] = (short) (j + 1);
		}

		this.setMode(Mesh.Mode.Lines);
		this.setBuffer(VertexBuffer.Type.Position, 3, array);
		this.setBuffer(VertexBuffer.Type.Index, 2, indices);
		this.updateBound();
		this.updateCounts();
	}
    
    private void createLinearMesh() {
        float[] array = new float[spline.getControlPoints().size() * 3];
        short[] indices = new short[(spline.getControlPoints().size() - 1) * 2];
        int i = 0;
        int cpt = 0;
        int k = 0;
        int j = 0;
        for (Iterator<Vector3f> it = spline.getControlPoints().iterator(); it.hasNext();) {
            Vector3f vector3f = it.next();
            array[i] = vector3f.getX();
            i++;
            array[i] = vector3f.getY();
            i++;
            array[i] = vector3f.getZ();
            i++;
            if (it.hasNext()) {
                k = j;
                indices[cpt] = (short) k;
                cpt++;
                k++;
                indices[cpt] = (short) k;
                cpt++;
                j++;
            }
        }

        this.setMode(Mesh.Mode.Lines);
        this.setBuffer(VertexBuffer.Type.Position, 3, array);
        this.setBuffer(VertexBuffer.Type.Index, 2, indices);
        this.updateBound();
        this.updateCounts();
    }
    
    /**
     * This method returns the length of the curve.
     * @return the length of the curve
     */
    public float getLength() {
    	return spline.getTotalLength();
    }
}

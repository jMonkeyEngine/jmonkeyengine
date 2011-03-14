/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.scene.shape;

import com.jme3.math.Spline;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author Nehon
 */
public class Curve extends Mesh {

    private Spline spline;
    private Vector3f temp = new Vector3f();

    /**
     * Create a curve mesh
     * Use a CatmullRom spline model that does not cycle.
     * @param controlPoints the control points to use to create this curve
     * @param nbSubSegments the number of subsegments between the control points
     */
    public Curve(Vector3f[] controlPoints, int nbSubSegments) {
        this(new Spline(Spline.SplineType.CatmullRom, controlPoints, 10, false), nbSubSegments);
    }

    /**
     * Create a curve mesh from a Spline
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
            case Linear:
            default:
            	this.createLinearMesh();
                break;
        }
    }

    private void createCatmullRomMesh(int nbSubSegments) {
        float[] array = new float[(((spline.getControlPoints().size() - 1) * nbSubSegments) + 1) * 3];
        short[] indices = new short[((spline.getControlPoints().size() - 1) * nbSubSegments) * 2];
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
        for (int j = 0; j < ((spline.getControlPoints().size() - 1) * nbSubSegments); j++) {
            k = j;
            indices[i] = (short) k;
            i++;
            k++;
            indices[i] = (short) k;
            i++;
        }

        setMode(Mesh.Mode.Lines);
        setBuffer(VertexBuffer.Type.Position, 3, array);
        setBuffer(VertexBuffer.Type.Index, ((spline.getControlPoints().size() - 1) * nbSubSegments) * 2, indices);
        updateBound();
        updateCounts();
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

        setMode(Mesh.Mode.Lines);
        setBuffer(VertexBuffer.Type.Position, 3, array);
        setBuffer(VertexBuffer.Type.Index, (spline.getControlPoints().size() - 1) * 2, indices);
        updateBound();
        updateCounts();
    }
}

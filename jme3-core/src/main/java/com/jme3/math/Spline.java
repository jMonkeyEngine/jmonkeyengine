/*
 * Copyright (c) 2009-2020 jMonkeyEngine
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
package com.jme3.math;

import com.jme3.export.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author Nehon
 */
public class Spline implements Savable {

    public enum SplineType {
        Linear,
        CatmullRom,
        Bezier,
        Nurb
    }
    
    private List<Vector3f> controlPoints = new ArrayList<Vector3f>();
    private List<Float> knots;				//knots of NURBS spline
    private float[] weights;				//weights of NURBS spline
    private int basisFunctionDegree;		//degree of NURBS spline basis function (computed automatically)
    private boolean cycle;
    private List<Float> segmentsLength;
    private float totalLength;
    private List<Vector3f> CRcontrolPoints;
    private float curveTension = 0.5f;
    private SplineType type = SplineType.CatmullRom;

    public Spline() {
    }

    /**
     * Create a spline
     * @param splineType the type of the spline @see {SplineType}
     * @param controlPoints an array of vector to use as control points of the spline
     * If the type of the curve is Bezier curve the control points should be provided
     * in the appropriate way. Each point 'p' describing control position in the scene
     * should be surrounded by two handler points. This applies to every point except
     * for the border points of the curve, who should have only one handle point.
     * The pattern should be as follows:
     * P0 - H0  :  H1 - P1 - H1  :  ...  :  Hn - Pn
     * 
     * n is the amount of 'P' - points.
     * @param curveTension the tension of the spline
     * @param cycle true if the spline cycle.
     */
    public Spline(SplineType splineType, Vector3f[] controlPoints, float curveTension, boolean cycle) {
    	if(splineType==SplineType.Nurb) {
    		throw new IllegalArgumentException("To create NURBS spline use: 'public Spline(Vector3f[] controlPoints, float[] weights, float[] nurbKnots)' constructor!");
    	}
        for (int i = 0; i < controlPoints.length; i++) {
            Vector3f vector3f = controlPoints[i];
            this.controlPoints.add(vector3f);
        }
        type = splineType;
        this.curveTension = curveTension;
        this.cycle = cycle;
        this.computeTotalLength();
    }

    /**
     * Create a spline
     * @param splineType the type of the spline @see {SplineType}
     * @param controlPoints a list of vector to use as control points of the spline
     * If the type of the curve is Bezier curve the control points should be provided
     * in the appropriate way. Each point 'p' describing control position in the scene
     * should be surrounded by two handler points. This applies to every point except
     * for the border points of the curve, who should have only one handle point.
     * The pattern should be as follows:
     * P0 - H0  :  H1 - P1 - H1  :  ...  :  Hn - Pn
     * 
     * n is the amount of 'P' - points.
     * @param curveTension the tension of the spline
     * @param cycle true if the spline cycle.
     */
    public Spline(SplineType splineType, List<Vector3f> controlPoints, float curveTension, boolean cycle) {
    	if(splineType==SplineType.Nurb) {
    		throw new IllegalArgumentException("To create NURBS spline use: 'public Spline(Vector3f[] controlPoints, float[] weights, float[] nurbKnots)' constructor!");
    	}
        type = splineType;
        this.controlPoints.addAll(controlPoints);
        this.curveTension = curveTension;
        this.cycle = cycle;
        this.computeTotalLength();
    }
    
    /**
     * Create a NURBS spline. A spline type is automatically set to SplineType.Nurb.
     * The cycle is set to <b>false</b> by default.
     * @param controlPoints a list of vector to use as control points of the spline
	 * @param nurbKnots the nurb's spline knots
     */
    public Spline(List<Vector4f> controlPoints, List<Float> nurbKnots) {
    	//input data control
    	for(int i=0;i<nurbKnots.size()-1;++i) {
    		if(nurbKnots.get(i)>nurbKnots.get(i+1)) {
    			throw new IllegalArgumentException("The knots values cannot decrease!");
    		}
    	}

    	//storing the data
        type = SplineType.Nurb;
        this.weights = new float[controlPoints.size()];
        this.knots = nurbKnots;
        this.basisFunctionDegree = nurbKnots.size() - weights.length;
        for(int i=0;i<controlPoints.size();++i) {
        	Vector4f controlPoint = controlPoints.get(i);
        	this.controlPoints.add(new Vector3f(controlPoint.x, controlPoint.y, controlPoint.z));
        	this.weights[i] = controlPoint.w;
        }
        CurveAndSurfaceMath.prepareNurbsKnots(knots, basisFunctionDegree);
        this.computeTotalLength();
    }

    private void initCatmullRomWayPoints(List<Vector3f> list) {
        if (CRcontrolPoints == null) {
            CRcontrolPoints = new ArrayList<Vector3f>();
        } else {
            CRcontrolPoints.clear();
        }
        int nb = list.size() - 1;

        if (cycle) {
            CRcontrolPoints.add(list.get(list.size() - 2));
        } else {
            CRcontrolPoints.add(list.get(0).subtract(list.get(1).subtract(list.get(0))));
        }

        for (Iterator<Vector3f> it = list.iterator(); it.hasNext();) {
            Vector3f vector3f = it.next();
            CRcontrolPoints.add(vector3f);
        }
        if (cycle) {
            CRcontrolPoints.add(list.get(1));
        } else {
            CRcontrolPoints.add(list.get(nb).add(list.get(nb).subtract(list.get(nb - 1))));
        }

    }

    /**
     * Adds a controlPoint to the spline
     * @param controlPoint a position in world space
     */
    public void addControlPoint(Vector3f controlPoint) {
        if (controlPoints.size() > 2 && this.cycle) {
            controlPoints.remove(controlPoints.size() - 1);
        }
        controlPoints.add(controlPoint.clone());
        if (controlPoints.size() >= 2 && this.cycle) {
            controlPoints.add(controlPoints.get(0).clone());
        }
        if (controlPoints.size() > 1) {
            this.computeTotalLength();
        }
    }

    /**
     * remove the controlPoint from the spline
     * @param controlPoint the controlPoint to remove
     */
    public void removeControlPoint(Vector3f controlPoint) {
        controlPoints.remove(controlPoint);
        if (controlPoints.size() > 1) {
            this.computeTotalLength();
        }
    }
    
    public void clearControlPoints(){
        controlPoints.clear();
        totalLength = 0;
    }

    /**
     * This method computes the total length of the curve.
     */
    private void computeTotalLength() {
        totalLength = 0;
        float l = 0;
        if (segmentsLength == null) {
            segmentsLength = new ArrayList<Float>();
        } else {
            segmentsLength.clear();
        }
        if (type == SplineType.Linear) {
            if (controlPoints.size() > 1) {
                for (int i = 0; i < controlPoints.size() - 1; i++) {
                    l = controlPoints.get(i + 1).subtract(controlPoints.get(i)).length();
                    segmentsLength.add(l);
                    totalLength += l;
                }
            }
        } else if(type == SplineType.Bezier) { 
        	this.computeBezierLength();
        } else if(type == SplineType.Nurb) {
        	this.computeNurbLength();
        } else {
            this.initCatmullRomWayPoints(controlPoints);
            this.computeCatmulLength();
        }
    }

    /**
     * This method computes the Catmull Rom curve length.
     */
    private void computeCatmulLength() {
        float l = 0;
        if (controlPoints.size() > 1) {
            for (int i = 0; i < controlPoints.size() - 1; i++) {
                l = FastMath.getCatmullRomP1toP2Length(CRcontrolPoints.get(i),
                        CRcontrolPoints.get(i + 1), CRcontrolPoints.get(i + 2), CRcontrolPoints.get(i + 3), 0, 1, curveTension);
                segmentsLength.add(l);
                totalLength += l;
            }
        }
    }
    
    /**
     * This method calculates the Bezier curve length.
     */
    private void computeBezierLength() {
    	float l = 0;
        if (controlPoints.size() > 1) {
            for (int i = 0; i < controlPoints.size() - 1; i+=3) {
                l = FastMath.getBezierP1toP2Length(controlPoints.get(i),
                		controlPoints.get(i + 1), controlPoints.get(i + 2), controlPoints.get(i + 3));
                segmentsLength.add(l);
                totalLength += l;
            }
        }
    }
    
    /**
     * This method calculates the NURB curve length.
     */
    private void computeNurbLength() {
    	//TODO: implement
    }

    /**
     * Interpolate a position on the spline
     * @param value a value from 0 to 1 that represent the position between the current control point and the next one
     * @param currentControlPoint the current control point
     * @param store a vector to store the result (use null to create a new one that will be returned by the method)
     * @return the position
     */
    public Vector3f interpolate(float value, int currentControlPoint, Vector3f store) {
        if (store == null) {
            store = new Vector3f();
        }
        switch (type) {
            case CatmullRom:
                FastMath.interpolateCatmullRom(value, curveTension, CRcontrolPoints.get(currentControlPoint), CRcontrolPoints.get(currentControlPoint + 1), CRcontrolPoints.get(currentControlPoint + 2), CRcontrolPoints.get(currentControlPoint + 3), store);
                break;
            case Linear:
                FastMath.interpolateLinear(value, controlPoints.get(currentControlPoint), controlPoints.get(currentControlPoint + 1), store);
                break;
            case Bezier:
            	FastMath.interpolateBezier(value, controlPoints.get(currentControlPoint), controlPoints.get(currentControlPoint + 1), controlPoints.get(currentControlPoint + 2), controlPoints.get(currentControlPoint + 3), store);
            	break;
            case Nurb:
            	CurveAndSurfaceMath.interpolateNurbs(value, this, store);
            	break;
            default:
                break;
        }
        return store;
    }

    /**
     * returns the curve tension
     */
    public float getCurveTension() {
        return curveTension;
    }

    /**
     * sets the curve tension
     *
     * @param curveTension the tension
     */
    public void setCurveTension(float curveTension) {
        this.curveTension = curveTension;
        if(type==SplineType.CatmullRom && !getControlPoints().isEmpty()) {            
        	this.computeTotalLength();
        }
    }

    /**
     * returns true if the spline cycle
     */
    public boolean isCycle() {
        return cycle;
    }

    /**
     * set to true to make the spline cycle
     * @param cycle
     */
    public void setCycle(boolean cycle) {
    	if(type!=SplineType.Nurb) {
    		if (controlPoints.size() >= 2) {
    			if (this.cycle && !cycle) {
    				controlPoints.remove(controlPoints.size() - 1);
    			}
    			if (!this.cycle && cycle) {
    				controlPoints.add(controlPoints.get(0));
    			}
    			this.cycle = cycle;
    			this.computeTotalLength();
    		} else {
    			this.cycle = cycle;
    		}
    	}
    }

    /**
     * return the total length of the spline
     */
    public float getTotalLength() {
        return totalLength;
    }

    /**
     * return the type of the spline
     */
    public SplineType getType() {
        return type;
    }

    /**
     * Sets the type of the spline
     * @param type
     */
    public void setType(SplineType type) {
        this.type = type;
        this.computeTotalLength();
    }

    /**
     * returns this spline control points
     */
    public List<Vector3f> getControlPoints() {
        return controlPoints;
    }

    /**
     * returns a list of float representing the segments length
     */
    public List<Float> getSegmentsLength() {
        return segmentsLength;
    }
    
    //////////// NURBS getters /////////////////////
    
	/**
	 * This method returns the minimum nurb curve knot value. Check the nurb type before calling this method. It the curve is not of a Nurb
	 * type - NPE will be thrown.
	 * @return the minimum nurb curve knot value
	 */
    public float getMinNurbKnot() {
    	return knots.get(basisFunctionDegree - 1);
    }
    
    /**
	 * This method returns the maximum nurb curve knot value. Check the nurb type before calling this method. It the curve is not of a Nurb
	 * type - NPE will be thrown.
	 * @return the maximum nurb curve knot value
	 */
    public float getMaxNurbKnot() {
    	return knots.get(weights.length);
    }
    
    /**
     * This method returns NURBS' spline knots.
     * @return NURBS' spline knots
     */
    public List<Float> getKnots() {
		return knots;
	}
    
    /**
     * This method returns NURBS' spline weights.
     * @return NURBS' spline weights
     */
    public float[] getWeights() {
		return weights;
	}
    
    /**
     * This method returns NURBS' spline basis function degree.
     * @return NURBS' spline basis function degree
     */
    public int getBasisFunctionDegree() {
		return basisFunctionDegree;
	}

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.writeSavableArrayList((ArrayList) controlPoints, "controlPoints", null);
        oc.write(type, "type", SplineType.CatmullRom);
        
        float list[] = null;
        if (segmentsLength != null) {
            list = new float[segmentsLength.size()];
            for (int i = 0; i < segmentsLength.size(); i++) {
                list[i] = segmentsLength.get(i);
            }
        }
        oc.write(list, "segmentsLength", null);

        oc.write(totalLength, "totalLength", 0);
        oc.writeSavableArrayList((ArrayList) CRcontrolPoints, "CRControlPoints", null);
        oc.write(curveTension, "curveTension", 0.5f);
        oc.write(cycle, "cycle", false);
        oc.writeSavableArrayList((ArrayList<Float>)knots, "knots", null);
        oc.write(weights, "weights", null);
        oc.write(basisFunctionDegree, "basisFunctionDegree", 0);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);

        controlPoints = in.readSavableArrayList("controlPoints", new ArrayList<>()); /* Empty List as default, prevents null pointers */
        float list[] = in.readFloatArray("segmentsLength", null);
        if (list != null) {
            segmentsLength = new ArrayList<Float>();
            for (int i = 0; i < list.length; i++) {
                segmentsLength.add(new Float(list[i]));
            }
        }
        type = in.readEnum("pathSplineType", SplineType.class, SplineType.CatmullRom);
        totalLength = in.readFloat("totalLength", 0);
        CRcontrolPoints = in.readSavableArrayList("CRControlPoints", null);
        curveTension = in.readFloat("curveTension", 0.5f);
        cycle = in.readBoolean("cycle", false);
        knots = in.readSavableArrayList("knots", null);
        weights = in.readFloatArray("weights", null);
        basisFunctionDegree = in.readInt("basisFunctionDegree", 0);
    }
}

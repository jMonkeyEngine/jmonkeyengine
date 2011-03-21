/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.math;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
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
        Bezier
    }
    private List<Vector3f> controlPoints = new ArrayList<Vector3f>();
    private boolean cycle = false;
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
        for (int i = 0; i < controlPoints.length; i++) {
            Vector3f vector3f = controlPoints[i];
            this.controlPoints.add(vector3f);
        }
        type = splineType;
        this.curveTension = curveTension;
        this.cycle = cycle;
        computeTotalLentgh();
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
        type = splineType;
        this.controlPoints.addAll(controlPoints);
        this.curveTension = curveTension;
        this.cycle = cycle;
        computeTotalLentgh();
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
        controlPoints.add(controlPoint);
        if (controlPoints.size() >= 2 && this.cycle) {
            controlPoints.add(controlPoints.get(0));
        }
        if (controlPoints.size() > 1) {
            computeTotalLentgh();
        }
    }

    /**
     * remove the controlPoint from the spline
     * @param controlPoint the controlPoint to remove
     */
    public void removeControlPoint(Vector3f controlPoint) {
        controlPoints.remove(controlPoint);
        if (controlPoints.size() > 1) {
            computeTotalLentgh();
        }
    }

    private void computeTotalLentgh() {
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
        } else {
            initCatmullRomWayPoints(controlPoints);
            computeCatmulLength();
        }
    }

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
                		controlPoints.get(i + 1), controlPoints.get(i + 2), controlPoints.get(i + 3), 0, 1);
                segmentsLength.add(l);
                totalLength += l;
            }
        }
    }

    /**
     * Iterpolate a position on the spline
     * @param value a value from 0 to 1 that represent the postion between the curent control point and the next one
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
            default:
                break;
        }
        return store;
    }

    /**
     * returns the curve tension
     * @return
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
        computeTotalLentgh();
    }

    /**
     * returns true if the spline cycle
     * @return
     */
    public boolean isCycle() {
        return cycle;
    }

    /**
     * set to true to make the spline cycle
     * @param cycle
     */
    public void setCycle(boolean cycle) {
        if (controlPoints.size() >= 2) {
            if (this.cycle && !cycle) {
                controlPoints.remove(controlPoints.size() - 1);
            }
            if (!this.cycle && cycle) {
                controlPoints.add(controlPoints.get(0));
            }
            this.cycle = cycle;
            computeTotalLentgh();
        } else {
            this.cycle = cycle;
        }
    }

    /**
     * return the total lenght of the spline
     * @return
     */
    public float getTotalLength() {
        return totalLength;
    }

    /**
     * return the type of the spline
     * @return
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
        computeTotalLentgh();
    }

    /**
     * returns this spline control points
     * @return
     */
    public List<Vector3f> getControlPoints() {
        return controlPoints;
    }

    /**
     * returns a list of float representing the segments lenght
     * @return
     */
    public List<Float> getSegmentsLength() {
        return segmentsLength;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.writeSavableArrayList((ArrayList) controlPoints, "controlPoints", null);
        oc.write(type, "type", SplineType.CatmullRom);
        float list[] = new float[segmentsLength.size()];
        for (int i = 0; i < segmentsLength.size(); i++) {
            list[i] = segmentsLength.get(i);
        }
        oc.write(list, "segmentsLength", null);

        oc.write(totalLength, "totalLength", 0);
        oc.writeSavableArrayList((ArrayList) CRcontrolPoints, "CRControlPoints", null);
        oc.write(curveTension, "curveTension", 0.5f);
        oc.write(cycle, "cycle", false);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);

        controlPoints = (ArrayList<Vector3f>) in.readSavableArrayList("wayPoints", null);
        float list[] = in.readFloatArray("segmentsLength", null);
        if (list != null) {
            segmentsLength = new ArrayList<Float>();
            for (int i = 0; i < list.length; i++) {
                segmentsLength.add(new Float(list[i]));
            }
        }
        type = in.readEnum("pathSplineType", SplineType.class, SplineType.CatmullRom);
        totalLength = in.readFloat("totalLength", 0);
        CRcontrolPoints = (ArrayList<Vector3f>) in.readSavableArrayList("CRControlPoints", null);
        curveTension = in.readFloat("curveTension", 0.5f);
        cycle = in.readBoolean("cycle", false);
    }
}

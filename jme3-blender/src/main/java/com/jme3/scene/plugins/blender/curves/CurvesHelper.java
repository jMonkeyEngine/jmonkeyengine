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
package com.jme3.scene.plugins.blender.curves;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Logger;

import com.jme3.material.Material;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Spline;
import com.jme3.math.Spline.SplineType;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.scene.plugins.blender.AbstractBlenderHelper;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.file.BlenderFileException;
import com.jme3.scene.plugins.blender.file.BlenderInputStream;
import com.jme3.scene.plugins.blender.file.DynamicArray;
import com.jme3.scene.plugins.blender.file.FileBlockHeader;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.materials.MaterialContext;
import com.jme3.scene.plugins.blender.materials.MaterialHelper;
import com.jme3.scene.plugins.blender.objects.Properties;
import com.jme3.scene.shape.Curve;
import com.jme3.scene.shape.Surface;
import com.jme3.util.BufferUtils;

/**
 * A class that is used in mesh calculations.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public class CurvesHelper extends AbstractBlenderHelper {
    private static final Logger LOGGER                      = Logger.getLogger(CurvesHelper.class.getName());

    /** Minimum basis U function degree for NURBS curves and surfaces. */
    protected int               minimumBasisUFunctionDegree = 4;
    /** Minimum basis V function degree for NURBS curves and surfaces. */
    protected int               minimumBasisVFunctionDegree = 4;

    /**
     * This constructor parses the given blender version and stores the result. Some functionalities may differ in
     * different blender versions.
     * @param blenderVersion
     *            the version read from the blend file
     * @param blenderContext
     *            the blender context
     */
    public CurvesHelper(String blenderVersion, BlenderContext blenderContext) {
        super(blenderVersion, blenderContext);
    }

    /**
     * This method converts given curve structure into a list of geometries representing the curve. The list is used here because on object
     * can have several separate curves.
     * @param curveStructure
     *            the curve structure
     * @param blenderContext
     *            the blender context
     * @return a list of geometries repreenting a single curve object
     * @throws BlenderFileException
     */
    public List<Geometry> toCurve(Structure curveStructure, BlenderContext blenderContext) throws BlenderFileException {
        String name = curveStructure.getName();
        int flag = ((Number) curveStructure.getFieldValue("flag")).intValue();
        boolean is3D = (flag & 0x01) != 0;
        boolean isFront = (flag & 0x02) != 0 && !is3D;
        boolean isBack = (flag & 0x04) != 0 && !is3D;
        if (isFront) {
            LOGGER.warning("No front face in curve implemented yet!");// TODO: implement front face
        }
        if (isBack) {
            LOGGER.warning("No back face in curve implemented yet!");// TODO: implement back face
        }

        // reading nurbs (and sorting them by material)
        List<Structure> nurbStructures = ((Structure) curveStructure.getFieldValue("nurb")).evaluateListBase();
        Map<Number, List<Structure>> nurbs = new HashMap<Number, List<Structure>>();
        for (Structure nurb : nurbStructures) {
            Number matNumber = (Number) nurb.getFieldValue("mat_nr");
            List<Structure> nurbList = nurbs.get(matNumber);
            if (nurbList == null) {
                nurbList = new ArrayList<Structure>();
                nurbs.put(matNumber, nurbList);
            }
            nurbList.add(nurb);
        }

        // getting materials
        MaterialHelper materialHelper = blenderContext.getHelper(MaterialHelper.class);
        MaterialContext[] materialContexts = materialHelper.getMaterials(curveStructure, blenderContext);
        Material defaultMaterial = null;
        if (materialContexts != null) {
            for (MaterialContext materialContext : materialContexts) {
                materialContext.setFaceCullMode(FaceCullMode.Off);
            }
        } else {
            defaultMaterial = blenderContext.getDefaultMaterial().clone();
            defaultMaterial.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
        }

        // getting or creating bevel object
        List<Geometry> bevelObject = null;
        Pointer pBevelObject = (Pointer) curveStructure.getFieldValue("bevobj");
        if (pBevelObject.isNotNull()) {
            Pointer pBevelStructure = (Pointer) pBevelObject.fetchData().get(0).getFieldValue("data");
            Structure bevelStructure = pBevelStructure.fetchData().get(0);
            bevelObject = this.toCurve(bevelStructure, blenderContext);
        } else {
            int bevResol = ((Number) curveStructure.getFieldValue("bevresol")).intValue();
            float extrude = ((Number) curveStructure.getFieldValue("ext1")).floatValue();
            float bevelDepth = ((Number) curveStructure.getFieldValue("ext2")).floatValue();
            if (bevelDepth > 0.0f) {
                float handlerLength = bevelDepth / 2.0f;

                List<Vector3f> conrtolPoints = new ArrayList<Vector3f>(extrude > 0.0f ? 19 : 13);
                if (extrude > 0.0f) {
                    conrtolPoints.add(new Vector3f(-bevelDepth, 0, extrude));
                    conrtolPoints.add(new Vector3f(-bevelDepth, 0, -handlerLength + extrude));
                    conrtolPoints.add(new Vector3f(-bevelDepth, 0, handlerLength - extrude));
                }

                conrtolPoints.add(new Vector3f(-bevelDepth, 0, -extrude));
                conrtolPoints.add(new Vector3f(-bevelDepth, 0, -handlerLength - extrude));

                conrtolPoints.add(new Vector3f(-handlerLength, 0, -bevelDepth - extrude));
                conrtolPoints.add(new Vector3f(0, 0, -bevelDepth - extrude));
                conrtolPoints.add(new Vector3f(handlerLength, 0, -bevelDepth - extrude));

                if (extrude > 0.0f) {
                    conrtolPoints.add(new Vector3f(bevelDepth, 0, -extrude - handlerLength));
                    conrtolPoints.add(new Vector3f(bevelDepth, 0, -extrude));
                    conrtolPoints.add(new Vector3f(bevelDepth, 0, -extrude + handlerLength));
                }

                conrtolPoints.add(new Vector3f(bevelDepth, 0, extrude - handlerLength));
                conrtolPoints.add(new Vector3f(bevelDepth, 0, extrude));
                conrtolPoints.add(new Vector3f(bevelDepth, 0, extrude + handlerLength));

                conrtolPoints.add(new Vector3f(handlerLength, 0, bevelDepth + extrude));
                conrtolPoints.add(new Vector3f(0, 0, bevelDepth + extrude));
                conrtolPoints.add(new Vector3f(-handlerLength, 0, bevelDepth + extrude));

                conrtolPoints.add(new Vector3f(-bevelDepth, 0, handlerLength + extrude));
                conrtolPoints.add(new Vector3f(-bevelDepth, 0, extrude));

                Spline bevelSpline = new Spline(SplineType.Bezier, conrtolPoints, 0, false);
                Curve bevelCurve = new Curve(bevelSpline, bevResol);
                bevelObject = new ArrayList<Geometry>(1);
                bevelObject.add(new Geometry("", bevelCurve));
            } else if (extrude > 0.0f) {
                Spline bevelSpline = new Spline(SplineType.Linear, new Vector3f[] { new Vector3f(0, 0, -extrude), new Vector3f(0, 0, extrude) }, 1, false);
                Curve bevelCurve = new Curve(bevelSpline, bevResol);
                bevelObject = new ArrayList<Geometry>(1);
                bevelObject.add(new Geometry("", bevelCurve));
            }
        }

        // getting taper object
        Spline taperObject = null;
        Pointer pTaperObject = (Pointer) curveStructure.getFieldValue("taperobj");
        if (bevelObject != null && pTaperObject.isNotNull()) {
            Pointer pTaperStructure = (Pointer) pTaperObject.fetchData().get(0).getFieldValue("data");
            Structure taperStructure = pTaperStructure.fetchData().get(0);
            taperObject = this.loadTaperObject(taperStructure);
        }

        Vector3f loc = this.getLoc(curveStructure);
        // creating the result curves
        List<Geometry> result = new ArrayList<Geometry>(nurbs.size());
        for (Entry<Number, List<Structure>> nurbEntry : nurbs.entrySet()) {
            for (Structure nurb : nurbEntry.getValue()) {
                int type = ((Number) nurb.getFieldValue("type")).intValue();
                List<Geometry> nurbGeoms = null;
                if ((type & 0x01) != 0) {// Bezier curve
                    nurbGeoms = this.loadBezierCurve(loc, nurb, bevelObject, taperObject, blenderContext);
                } else if ((type & 0x04) != 0) {// NURBS
                    nurbGeoms = this.loadNurb(loc, nurb, bevelObject, taperObject, blenderContext);
                }
                if (nurbGeoms != null) {// setting the name and assigning materials
                    for (Geometry nurbGeom : nurbGeoms) {
                        if (materialContexts != null) {
                            materialContexts[nurbEntry.getKey().intValue()].applyMaterial(nurbGeom, curveStructure.getOldMemoryAddress(), null, blenderContext);
                        } else {
                            nurbGeom.setMaterial(defaultMaterial);
                        }
                        nurbGeom.setName(name);
                        result.add(nurbGeom);
                    }
                }
            }
        }

        // reading custom properties
        if (blenderContext.getBlenderKey().isLoadObjectProperties() && result.size() > 0) {
            Properties properties = this.loadProperties(curveStructure, blenderContext);
            // the loaded property is a group property, so we need to get each value and set it to every geometry of the curve
            if (properties != null && properties.getValue() != null) {
                for(Geometry geom : result) {
                    this.applyProperties(geom, properties);
                }
            }
        }

        return result;
    }

    /**
     * This method loads the bezier curve.
     * @param loc
     *            the translation of the curve
     * @param nurb
     *            the nurb structure
     * @param bevelObject
     *            the bevel object
     * @param taperObject
     *            the taper object
     * @param blenderContext
     *            the blender context
     * @return a list of geometries representing the curves
     * @throws BlenderFileException
     *             an exception is thrown when there are problems with the blender file
     */
    protected List<Geometry> loadBezierCurve(Vector3f loc, Structure nurb, List<Geometry> bevelObject, Spline taperObject, BlenderContext blenderContext) throws BlenderFileException {
        Pointer pBezierTriple = (Pointer) nurb.getFieldValue("bezt");
        List<Geometry> result = new ArrayList<Geometry>();
        if (pBezierTriple.isNotNull()) {
            boolean smooth = (((Number) nurb.getFlatFieldValue("flag")).intValue() & 0x01) != 0;
            int resolution = ((Number) nurb.getFieldValue("resolu")).intValue();
            boolean cyclic = (((Number) nurb.getFieldValue("flagu")).intValue() & 0x01) != 0;

            // creating the curve object
            BezierCurve bezierCurve = new BezierCurve(0, pBezierTriple.fetchData(), 3);
            List<Vector3f> controlPoints = bezierCurve.getControlPoints();
            if (fixUpAxis) {
                for (Vector3f v : controlPoints) {
                    float y = v.y;
                    v.y = v.z;
                    v.z = -y;
                }
            }

            if (bevelObject != null && taperObject == null) {// create taper object using the scales of the bezier triple
                int triplesCount = controlPoints.size() / 3;
                List<Vector3f> taperControlPoints = new ArrayList<Vector3f>(triplesCount);
                for (int i = 0; i < triplesCount; ++i) {
                    taperControlPoints.add(new Vector3f(controlPoints.get(i * 3 + 1).x, (float)bezierCurve.getRadius(i), 0));
                }
                taperObject = new Spline(SplineType.Linear, taperControlPoints, 0, false);
            }

            if (cyclic) {
                // copy the first three points at the end
                for (int i = 0; i < 3; ++i) {
                    controlPoints.add(controlPoints.get(i));
                }
            }
            // removing the first and last handles
            controlPoints.remove(0);
            controlPoints.remove(controlPoints.size() - 1);

            // creating curve
            Spline spline = new Spline(SplineType.Bezier, controlPoints, 0, false);
            Curve curve = new Curve(spline, resolution);
            if (bevelObject == null) {// creating a normal curve
                Geometry curveGeometry = new Geometry(null, curve);
                result.add(curveGeometry);
                // TODO: use front and back flags; surface excluding algorithm for bezier circles should be added
            } else {// creating curve with bevel and taper shape
                result = this.applyBevelAndTaper(curve, bevelObject, taperObject, smooth, blenderContext);
            }
        }
        return result;
    }

    /**
     * This method loads the NURBS curve or surface.
     * @param loc
     *            object's location
     * @param nurb
     *            the NURBS data structure
     * @param bevelObject
     *            the bevel object to be applied
     * @param taperObject
     *            the taper object to be applied
     * @param blenderContext
     *            the blender context
     * @return a list of geometries that represents the loaded NURBS curve or surface
     * @throws BlenderFileException
     *             an exception is throw when problems with blender loaded data occurs
     */
    @SuppressWarnings("unchecked")
    protected List<Geometry> loadNurb(Vector3f loc, Structure nurb, List<Geometry> bevelObject, Spline taperObject, BlenderContext blenderContext) throws BlenderFileException {
        // loading the knots
        List<Float>[] knots = new List[2];
        Pointer[] pKnots = new Pointer[] { (Pointer) nurb.getFieldValue("knotsu"), (Pointer) nurb.getFieldValue("knotsv") };
        for (int i = 0; i < knots.length; ++i) {
            if (pKnots[i].isNotNull()) {
                FileBlockHeader fileBlockHeader = blenderContext.getFileBlock(pKnots[i].getOldMemoryAddress());
                BlenderInputStream blenderInputStream = blenderContext.getInputStream();
                blenderInputStream.setPosition(fileBlockHeader.getBlockPosition());
                int knotsAmount = fileBlockHeader.getCount() * fileBlockHeader.getSize() / 4;
                knots[i] = new ArrayList<Float>(knotsAmount);
                for (int j = 0; j < knotsAmount; ++j) {
                    knots[i].add(Float.valueOf(blenderInputStream.readFloat()));
                }
            }
        }

        // loading the flags and orders (basis functions degrees)
        int flagU = ((Number) nurb.getFieldValue("flagu")).intValue();
        int flagV = ((Number) nurb.getFieldValue("flagv")).intValue();
        int orderU = ((Number) nurb.getFieldValue("orderu")).intValue();
        int orderV = ((Number) nurb.getFieldValue("orderv")).intValue();

        // loading control points and their weights
        int pntsU = ((Number) nurb.getFieldValue("pntsu")).intValue();
        int pntsV = ((Number) nurb.getFieldValue("pntsv")).intValue();
        List<Structure> bPoints = ((Pointer) nurb.getFieldValue("bp")).fetchData();
        List<List<Vector4f>> controlPoints = new ArrayList<List<Vector4f>>(pntsV);
        for (int i = 0; i < pntsV; ++i) {
            List<Vector4f> uControlPoints = new ArrayList<Vector4f>(pntsU);
            for (int j = 0; j < pntsU; ++j) {
                DynamicArray<Float> vec = (DynamicArray<Float>) bPoints.get(j + i * pntsU).getFieldValue("vec");
                if (fixUpAxis) {
                    uControlPoints.add(new Vector4f(vec.get(0).floatValue(), vec.get(2).floatValue(), -vec.get(1).floatValue(), vec.get(3).floatValue()));
                } else {
                    uControlPoints.add(new Vector4f(vec.get(0).floatValue(), vec.get(1).floatValue(), vec.get(2).floatValue(), vec.get(3).floatValue()));
                }
            }
            if ((flagU & 0x01) != 0) {
                for (int k = 0; k < orderU - 1; ++k) {
                    uControlPoints.add(uControlPoints.get(k));
                }
            }
            controlPoints.add(uControlPoints);
        }
        if ((flagV & 0x01) != 0) {
            for (int k = 0; k < orderV - 1; ++k) {
                controlPoints.add(controlPoints.get(k));
            }
        }

        int resolu = ((Number) nurb.getFieldValue("resolu")).intValue() + 1;
        List<Geometry> result;
        if (knots[1] == null) {// creating the curve
            Spline nurbSpline = new Spline(controlPoints.get(0), knots[0]);
            Curve nurbCurve = new Curve(nurbSpline, resolu);
            if (bevelObject != null) {
                result = this.applyBevelAndTaper(nurbCurve, bevelObject, taperObject, true, blenderContext);// TODO: smooth
            } else {
                result = new ArrayList<Geometry>(1);
                Geometry nurbGeometry = new Geometry("", nurbCurve);
                result.add(nurbGeometry);
            }
        } else {// creating the nurb surface
            int resolv = ((Number) nurb.getFieldValue("resolv")).intValue() + 1;
            Surface nurbSurface = Surface.createNurbsSurface(controlPoints, knots, resolu, resolv, orderU, orderV);
            Geometry nurbGeometry = new Geometry("", nurbSurface);
            result = new ArrayList<Geometry>(1);
            result.add(nurbGeometry);
        }
        return result;
    }

    /**
     * The method computes the taper scale on the given point on the curve.
     * 
     * @param taper
     *            the taper object that defines the scale
     * @param percent
     *            the percent of the 'road' along the curve
     * @return scale on the pointed place along the curve
     */
    protected float getTaperScale(Spline taper, float percent) {
        if (taper == null) {
            return 1;// return scale = 1 if no taper is applied
        }
        percent = FastMath.clamp(percent, 0, 1);
        List<Float> segmentLengths = taper.getSegmentsLength();
        float percentLength = taper.getTotalLength() * percent;
        float partLength = 0;
        int i;
        for (i = 0; i < segmentLengths.size(); ++i) {
            partLength += segmentLengths.get(i);
            if (partLength > percentLength) {
                partLength -= segmentLengths.get(i);
                percentLength -= partLength;
                percent = percentLength / segmentLengths.get(i);
                break;
            }
        }
        // do not cross the line :)
        if (percent >= 1) {
            percent = 1;
            --i;
        }
        if (taper.getType() == SplineType.Bezier) {
            i *= 3;
        }
        return taper.interpolate(percent, i, null).y;
    }

    /**
     * This method applies bevel and taper objects to the curve.
     * @param curve
     *            the curve we apply the objects to
     * @param bevelObject
     *            the bevel object
     * @param taperObject
     *            the taper object
     * @param smooth
     *            the smooth flag
     * @param blenderContext
     *            the blender context
     * @return a list of geometries representing the beveled and/or tapered curve
     */
    protected List<Geometry> applyBevelAndTaper(Curve curve, List<Geometry> bevelObject, Spline taperObject, boolean smooth, BlenderContext blenderContext) {
        Vector3f[] curvePoints = BufferUtils.getVector3Array(curve.getFloatBuffer(Type.Position));
        Vector3f subtractResult = new Vector3f();
        float curveLength = curve.getLength();

        FloatBuffer[] vertexBuffers = new FloatBuffer[bevelObject.size()];
        FloatBuffer[] normalBuffers = new FloatBuffer[bevelObject.size()];
        IndexBuffer[] indexBuffers = new IndexBuffer[bevelObject.size()];
        for (int geomIndex = 0; geomIndex < bevelObject.size(); ++geomIndex) {
            Mesh mesh = bevelObject.get(geomIndex).getMesh();
            Vector3f[] positions = BufferUtils.getVector3Array(mesh.getFloatBuffer(Type.Position));
            Vector3f[] bevelPoints = this.transformToFirstLineOfBevelPoints(positions, curvePoints[0], curvePoints[1]);

            List<Vector3f[]> bevels = new ArrayList<Vector3f[]>(curvePoints.length);
            bevels.add(bevelPoints);

            vertexBuffers[geomIndex] = BufferUtils.createFloatBuffer(bevelPoints.length * 3 * curvePoints.length * (smooth ? 1 : 6));
            for (int i = 1; i < curvePoints.length - 1; ++i) {
                bevelPoints = this.transformBevel(bevelPoints, curvePoints[i - 1], curvePoints[i], curvePoints[i + 1]);
                bevels.add(bevelPoints);
            }
            bevelPoints = this.transformBevel(bevelPoints, curvePoints[curvePoints.length - 2], curvePoints[curvePoints.length - 1], null);
            bevels.add(bevelPoints);

            if (bevels.size() > 2) {
                // changing the first and last bevel so that they are parallel to their neighbours (blender works this way)
                // notice this implicates that the distances of every corresponding point in th two bevels must be identical and
                // equal to the distance between the points on curve that define the bevel position
                // so instead doing complicated rotations on each point we will simply properly translate each of them

                int[][] pointIndexes = new int[][] { { 0, 1 }, { curvePoints.length - 1, curvePoints.length - 2 } };
                for (int[] indexes : pointIndexes) {
                    float distance = curvePoints[indexes[1]].subtract(curvePoints[indexes[0]], subtractResult).length();
                    Vector3f[] bevel = bevels.get(indexes[0]);
                    Vector3f[] nextBevel = bevels.get(indexes[1]);
                    for (int i = 0; i < bevel.length; ++i) {
                        float d = bevel[i].subtract(nextBevel[i], subtractResult).length();
                        subtractResult.normalizeLocal().multLocal(distance - d);
                        bevel[i].addLocal(subtractResult);
                    }
                }
            }

            // apply scales to the bevels
            float lengthAlongCurve = 0;
            for (int i = 0; i < curvePoints.length; ++i) {
                if (i > 0) {
                    lengthAlongCurve += curvePoints[i].subtract(curvePoints[i - 1], subtractResult).length();
                }
                float taperScale = this.getTaperScale(taperObject, i == 0 ? 0 : lengthAlongCurve / curveLength);
                this.applyScale(bevels.get(i), curvePoints[i], taperScale);
            }

            if (smooth) {// add everything to the buffer
                for (Vector3f[] bevel : bevels) {
                    for (Vector3f d : bevel) {
                        vertexBuffers[geomIndex].put(d.x);
                        vertexBuffers[geomIndex].put(d.y);
                        vertexBuffers[geomIndex].put(d.z);
                    }
                }
            } else {// add vertices to the buffer duplicating them so that every vertex belongs only to a single triangle
                for (int i = 0; i < curvePoints.length - 1; ++i) {
                    for (int j = 0; j < bevelPoints.length - 1; ++j) {
                        // first triangle
                        vertexBuffers[geomIndex].put(bevels.get(i)[j].x);
                        vertexBuffers[geomIndex].put(bevels.get(i)[j].y);
                        vertexBuffers[geomIndex].put(bevels.get(i)[j].z);
                        vertexBuffers[geomIndex].put(bevels.get(i)[j + 1].x);
                        vertexBuffers[geomIndex].put(bevels.get(i)[j + 1].y);
                        vertexBuffers[geomIndex].put(bevels.get(i)[j + 1].z);
                        vertexBuffers[geomIndex].put(bevels.get(i + 1)[j].x);
                        vertexBuffers[geomIndex].put(bevels.get(i + 1)[j].y);
                        vertexBuffers[geomIndex].put(bevels.get(i + 1)[j].z);

                        // second triangle
                        vertexBuffers[geomIndex].put(bevels.get(i)[j + 1].x);
                        vertexBuffers[geomIndex].put(bevels.get(i)[j + 1].y);
                        vertexBuffers[geomIndex].put(bevels.get(i)[j + 1].z);
                        vertexBuffers[geomIndex].put(bevels.get(i + 1)[j + 1].x);
                        vertexBuffers[geomIndex].put(bevels.get(i + 1)[j + 1].y);
                        vertexBuffers[geomIndex].put(bevels.get(i + 1)[j + 1].z);
                        vertexBuffers[geomIndex].put(bevels.get(i + 1)[j].x);
                        vertexBuffers[geomIndex].put(bevels.get(i + 1)[j].y);
                        vertexBuffers[geomIndex].put(bevels.get(i + 1)[j].z);
                    }
                }
            }

            indexBuffers[geomIndex] = this.generateIndexes(bevelPoints.length, curvePoints.length, smooth);
            normalBuffers[geomIndex] = this.generateNormals(indexBuffers[geomIndex], vertexBuffers[geomIndex], smooth);
        }

        // creating and returning the result
        List<Geometry> result = new ArrayList<Geometry>(vertexBuffers.length);
        Float oneReferenceToCurveLength = new Float(curveLength);// its important for array modifier to use one reference here
        for (int i = 0; i < vertexBuffers.length; ++i) {
            Mesh mesh = new Mesh();
            mesh.setBuffer(Type.Position, 3, vertexBuffers[i]);
            if (indexBuffers[i].getBuffer() instanceof IntBuffer) {
                mesh.setBuffer(Type.Index, 3, (IntBuffer) indexBuffers[i].getBuffer());
            } else {
                mesh.setBuffer(Type.Index, 3, (ShortBuffer) indexBuffers[i].getBuffer());
            }
            mesh.setBuffer(Type.Normal, 3, normalBuffers[i]);
            Geometry g = new Geometry("g" + i, mesh);
            g.setUserData("curveLength", oneReferenceToCurveLength);
            g.updateModelBound();
            result.add(g);
        }
        return result;
    }

    /**
     * the method applies scale for the given bevel points. The points table is
     * being modified so expect ypur result there.
     * 
     * @param points
     *            the bevel points
     * @param centerPoint
     *            the center point of the bevel
     * @param scale
     *            the scale to be applied
     */
    private void applyScale(Vector3f[] points, Vector3f centerPoint, float scale) {
        Vector3f taperScaleVector = new Vector3f();
        for (Vector3f p : points) {
            taperScaleVector.set(centerPoint).subtractLocal(p).multLocal(1 - scale);
            p.addLocal(taperScaleVector);
        }
    }

    /**
     * The method generates normal buffer for the created mesh of the curve.
     * 
     * @param indexes
     *            the indexes of the mesh points
     * @param points
     *            the mesh's points
     * @param smooth
     *            the flag indicating if the result is to be smooth or solid
     * @return normals buffer for the mesh
     */
    private FloatBuffer generateNormals(IndexBuffer indexes, FloatBuffer points, boolean smooth) {
        Map<Integer, Vector3f> normalMap = new TreeMap<Integer, Vector3f>();
        Vector3f[] allVerts = BufferUtils.getVector3Array(points);

        for (int i = 0; i < indexes.size(); i += 3) {
            int index1 = indexes.get(i);
            int index2 = indexes.get(i + 1);
            int index3 = indexes.get(i + 2);

            Vector3f n = FastMath.computeNormal(allVerts[index1], allVerts[index2], allVerts[index3]);
            this.addNormal(n, normalMap, smooth, index1, index2, index3);
        }

        FloatBuffer normals = BufferUtils.createFloatBuffer(normalMap.size() * 3);
        for (Entry<Integer, Vector3f> entry : normalMap.entrySet()) {
            normals.put(entry.getValue().x);
            normals.put(entry.getValue().y);
            normals.put(entry.getValue().z);
        }
        return normals;
    }

    /**
     * The amount of faces in the final mesh is the amount of edges in the bevel
     * curve (which is less by 1 than its number of vertices) multiplied by 2
     * (because each edge has two faces assigned on both sides) and multiplied
     * by the amount of bevel curve repeats which is equal to the amount of
     * vertices on the target curve finally we need to subtract the bevel edges
     * amount 2 times because the border edges have only one face attached and
     * at last multiply everything by 3 because each face needs 3 indexes to be
     * described
     * 
     * @param bevelShapeVertexCount
     *            amount of points in bevel shape
     * @param bevelRepeats
     *            amount of bevel shapes along the curve
     * @param smooth
     *            the smooth flag
     * @return index buffer for the mesh
     */
    private IndexBuffer generateIndexes(int bevelShapeVertexCount, int bevelRepeats, boolean smooth) {
        int putIndex = 0;
        if (smooth) {
            int indexBufferSize = (bevelRepeats - 1) * (bevelShapeVertexCount - 1) * 6;
            IndexBuffer result = IndexBuffer.createIndexBuffer(indexBufferSize, indexBufferSize);

            for (int i = 0; i < bevelRepeats - 1; ++i) {
                for (int j = 0; j < bevelShapeVertexCount - 1; ++j) {
                    result.put(putIndex++, i * bevelShapeVertexCount + j);
                    result.put(putIndex++, i * bevelShapeVertexCount + j + 1);
                    result.put(putIndex++, (i + 1) * bevelShapeVertexCount + j);

                    result.put(putIndex++, i * bevelShapeVertexCount + j + 1);
                    result.put(putIndex++, (i + 1) * bevelShapeVertexCount + j + 1);
                    result.put(putIndex++, (i + 1) * bevelShapeVertexCount + j);
                }
            }
            return result;
        } else {
            // every pair of bevel vertices belongs to two triangles
            // we have the same amount of pairs as the amount of vertices in bevel
            // so the amount of triangles is: bevelShapeVertexCount * 2 * (bevelRepeats - 1)
            // and this gives the amount of vertices in non smooth shape as below ...
            int indexBufferSize = bevelShapeVertexCount * bevelRepeats * 6;// 6 = 2 * 3 where 2 is stated above and 3 is the count of vertices for each triangle
            IndexBuffer result = IndexBuffer.createIndexBuffer(indexBufferSize, indexBufferSize);
            for (int i = 0; i < indexBufferSize; ++i) {
                result.put(putIndex++, i);
            }
            return result;
        }
    }

    /**
     * The method transforms the bevel along the curve.
     * 
     * @param bevel
     *            the bevel to be transformed
     * @param prevPos
     *            previous curve point
     * @param currPos
     *            current curve point (here the center of the new bevel will be
     *            set)
     * @param nextPos
     *            next curve point
     * @return points of transformed bevel
     */
    private Vector3f[] transformBevel(Vector3f[] bevel, Vector3f prevPos, Vector3f currPos, Vector3f nextPos) {
        bevel = bevel.clone();

        // currPos and directionVector define the line in 3D space
        Vector3f directionVector = prevPos != null ? currPos.subtract(prevPos) : nextPos.subtract(currPos);
        directionVector.normalizeLocal();

        // plane is described by equation: Ax + By + Cz + D = 0 where planeNormal = [A, B, C] and D = -(Ax + By + Cz)
        Vector3f planeNormal = null;
        if (prevPos != null) {
            planeNormal = currPos.subtract(prevPos).normalizeLocal();
            if (nextPos != null) {
                planeNormal.addLocal(nextPos.subtract(currPos).normalizeLocal()).normalizeLocal();
            }
        } else {
            planeNormal = nextPos.subtract(currPos).normalizeLocal();
        }
        float D = -planeNormal.dot(currPos);// D = -(Ax + By + Cz)

        // now we need to compute paralell cast of each bevel point on the plane, the leading line is already known
        // parametric equation of a line: x = px + vx * t; y = py + vy * t; z = pz + vz * t
        // where p = currPos and v = directionVector
        // using x, y and z in plane equation we get value of 't' that will allow us to compute the point where plane and line cross
        float temp = planeNormal.dot(directionVector);
        for (int i = 0; i < bevel.length; ++i) {
            float t = -(planeNormal.dot(bevel[i]) + D) / temp;
            if (fixUpAxis) {
                bevel[i] = new Vector3f(bevel[i].x + directionVector.x * t, bevel[i].y + directionVector.y * t, bevel[i].z + directionVector.z * t);
            } else {
                bevel[i] = new Vector3f(bevel[i].x + directionVector.x * t, -bevel[i].z + directionVector.z * t, bevel[i].y + directionVector.y * t);
            }
        }
        return bevel;
    }

    /**
     * This method transforms the first line of the bevel points positioning it
     * on the first point of the curve.
     * 
     * @param startingLinePoints
     *            the vbevel shape points
     * @param firstCurvePoint
     *            the first curve's point
     * @param secondCurvePoint
     *            the second curve's point
     * @return points of transformed bevel
     */
    private Vector3f[] transformToFirstLineOfBevelPoints(Vector3f[] startingLinePoints, Vector3f firstCurvePoint, Vector3f secondCurvePoint) {
        Vector3f planeNormal = secondCurvePoint.subtract(firstCurvePoint).normalizeLocal();

        float angle = FastMath.acos(planeNormal.dot(Vector3f.UNIT_Y));
        planeNormal.crossLocal(Vector3f.UNIT_Y).normalizeLocal();// planeNormal is the rotation axis now
        Quaternion pointRotation = new Quaternion();
        pointRotation.fromAngleAxis(angle, planeNormal);

        Matrix4f m = new Matrix4f();
        m.setRotationQuaternion(pointRotation);
        m.setTranslation(firstCurvePoint);

        float[] temp = new float[] { 0, 0, 0, 1 };
        Vector3f[] verts = new Vector3f[startingLinePoints.length];
        for (int j = 0; j < verts.length; ++j) {
            temp[0] = startingLinePoints[j].x;
            temp[1] = startingLinePoints[j].y;
            temp[2] = startingLinePoints[j].z;
            temp = m.mult(temp);// the result is stored in the array
            if (fixUpAxis) {
                verts[j] = new Vector3f(temp[0], -temp[2], temp[1]);
            } else {
                verts[j] = new Vector3f(temp[0], temp[1], temp[2]);
            }
        }
        return verts;
    }

    /**
     * The method adds a normal to the given map. Depending in the smooth factor
     * it is either merged with the revious normal or not.
     * 
     * @param normalToAdd
     *            the normal vector to be added
     * @param normalMap
     *            the normal map where we add vectors
     * @param smooth
     *            the smooth flag
     * @param indexes
     *            the indexes of the normals
     */
    private void addNormal(Vector3f normalToAdd, Map<Integer, Vector3f> normalMap, boolean smooth, int... indexes) {
        for (int index : indexes) {
            Vector3f n = normalMap.get(index);
            if (!smooth || n == null) {
                normalMap.put(index, normalToAdd.clone());
            } else {
                n.addLocal(normalToAdd).normalizeLocal();
            }
        }
    }

    /**
     * This method loads the taper object.
     * 
     * @param taperStructure
     *            the taper structure
     * @return the taper object
     * @throws BlenderFileException
     */
    protected Spline loadTaperObject(Structure taperStructure) throws BlenderFileException {
        // reading nurbs
        List<Structure> nurbStructures = ((Structure) taperStructure.getFieldValue("nurb")).evaluateListBase();
        for (Structure nurb : nurbStructures) {
            Pointer pBezierTriple = (Pointer) nurb.getFieldValue("bezt");
            if (pBezierTriple.isNotNull()) {
                // creating the curve object
                BezierCurve bezierCurve = new BezierCurve(0, pBezierTriple.fetchData(), 3);
                List<Vector3f> controlPoints = bezierCurve.getControlPoints();
                // removing the first and last handles
                controlPoints.remove(0);
                controlPoints.remove(controlPoints.size() - 1);

                // return the first taper curve that has more than 3 control points
                if (controlPoints.size() > 3) {
                    return new Spline(SplineType.Bezier, controlPoints, 0, false);
                }
            }
        }
        return null;
    }

    /**
     * This method returns the translation of the curve. The UP axis is taken
     * into account here.
     * 
     * @param curveStructure
     *            the curve structure
     * @return curve translation
     */
    @SuppressWarnings("unchecked")
    protected Vector3f getLoc(Structure curveStructure) {
        DynamicArray<Number> locArray = (DynamicArray<Number>) curveStructure.getFieldValue("loc");
        if (fixUpAxis) {
            return new Vector3f(locArray.get(0).floatValue(), locArray.get(1).floatValue(), -locArray.get(2).floatValue());
        } else {
            return new Vector3f(locArray.get(0).floatValue(), locArray.get(2).floatValue(), locArray.get(1).floatValue());
        }
    }
}
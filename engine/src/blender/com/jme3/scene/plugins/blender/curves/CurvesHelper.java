package com.jme3.scene.plugins.blender.curves;

import com.jme3.material.Material;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.Spline.SplineType;
import com.jme3.math.*;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.plugins.blender.AbstractBlenderHelper;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.*;
import com.jme3.scene.plugins.blender.materials.MaterialHelper;
import com.jme3.scene.plugins.blender.meshes.MeshHelper;
import com.jme3.scene.plugins.blender.objects.Properties;
import com.jme3.scene.shape.Curve;
import com.jme3.scene.shape.Surface;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

/**
 * A class that is used in mesh calculations.
 * @author Marcin Roguski
 */
public class CurvesHelper extends AbstractBlenderHelper {

    private static final Logger LOGGER = Logger.getLogger(CurvesHelper.class.getName());
    /** Minimum basis U function degree for NURBS curves and surfaces. */
    protected int minimumBasisUFunctionDegree = 4;
    /** Minimum basis V function degree for NURBS curves and surfaces. */
    protected int minimumBasisVFunctionDegree = 4;

    /**
     * This constructor parses the given blender version and stores the result. Some functionalities may differ in
     * different blender versions.
     * @param blenderVersion
     *        the version read from the blend file
     * @param fixUpAxis
     *        a variable that indicates if the Y asxis is the UP axis or not
     */
    public CurvesHelper(String blenderVersion, boolean fixUpAxis) {
        super(blenderVersion, fixUpAxis);
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
            LOGGER.warning("No front face in curve implemented yet!");//TODO: implement front face
        }
        if (isBack) {
            LOGGER.warning("No back face in curve implemented yet!");//TODO: implement back face
        }

        //reading nurbs (and sorting them by material)
        List<Structure> nurbStructures = ((Structure) curveStructure.getFieldValue("nurb")).evaluateListBase(blenderContext);
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

        //getting materials
        MaterialHelper materialHelper = blenderContext.getHelper(MaterialHelper.class);
        Material[] materials = materialHelper.getMaterials(curveStructure, blenderContext);
        if (materials == null) {
            materials = new Material[]{blenderContext.getDefaultMaterial().clone()};
        }
        for (Material material : materials) {
            material.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
        }

        //getting or creating bevel object
        List<Geometry> bevelObject = null;
        Pointer pBevelObject = (Pointer) curveStructure.getFieldValue("bevobj");
        if (pBevelObject.isNotNull()) {
            Pointer pBevelStructure = (Pointer) pBevelObject.fetchData(blenderContext.getInputStream()).get(0).getFieldValue("data");
            Structure bevelStructure = pBevelStructure.fetchData(blenderContext.getInputStream()).get(0);
            bevelObject = this.toCurve(bevelStructure, blenderContext);
        } else {
            int bevResol = ((Number) curveStructure.getFieldValue("bevresol")).intValue();
            float extrude = ((Number) curveStructure.getFieldValue("ext1")).floatValue();
            float bevelDepth = ((Number) curveStructure.getFieldValue("ext2")).floatValue();
            if (bevelDepth > 0.0f) {
                float handlerLength = bevelDepth / 2.0f;

                List<Vector3f> conrtolPoints = new ArrayList<Vector3f>(extrude > 0.0f ? 19 : 13);
                conrtolPoints.add(new Vector3f(-bevelDepth, extrude, 0));
                conrtolPoints.add(new Vector3f(-bevelDepth, handlerLength + extrude, 0));

                conrtolPoints.add(new Vector3f(-handlerLength, bevelDepth + extrude, 0));
                conrtolPoints.add(new Vector3f(0, bevelDepth + extrude, 0));
                conrtolPoints.add(new Vector3f(handlerLength, bevelDepth + extrude, 0));

                conrtolPoints.add(new Vector3f(bevelDepth, extrude + handlerLength, 0));
                conrtolPoints.add(new Vector3f(bevelDepth, extrude, 0));
                conrtolPoints.add(new Vector3f(bevelDepth, extrude - handlerLength, 0));

                if (extrude > 0.0f) {
                    conrtolPoints.add(new Vector3f(bevelDepth, -extrude + handlerLength, 0));
                    conrtolPoints.add(new Vector3f(bevelDepth, -extrude, 0));
                    conrtolPoints.add(new Vector3f(bevelDepth, -extrude - handlerLength, 0));
                }

                conrtolPoints.add(new Vector3f(handlerLength, -bevelDepth - extrude, 0));
                conrtolPoints.add(new Vector3f(0, -bevelDepth - extrude, 0));
                conrtolPoints.add(new Vector3f(-handlerLength, -bevelDepth - extrude, 0));

                conrtolPoints.add(new Vector3f(-bevelDepth, -handlerLength - extrude, 0));
                conrtolPoints.add(new Vector3f(-bevelDepth, -extrude, 0));

                if (extrude > 0.0f) {
                    conrtolPoints.add(new Vector3f(-bevelDepth, handlerLength - extrude, 0));

                    conrtolPoints.add(new Vector3f(-bevelDepth, -handlerLength + extrude, 0));
                    conrtolPoints.add(new Vector3f(-bevelDepth, extrude, 0));
                }

                Spline bevelSpline = new Spline(SplineType.Bezier, conrtolPoints, 0, false);
                Curve bevelCurve = new Curve(bevelSpline, bevResol);
                bevelObject = new ArrayList<Geometry>(1);
                bevelObject.add(new Geometry("", bevelCurve));
            } else if (extrude > 0.0f) {
                Spline bevelSpline = new Spline(SplineType.Linear, new Vector3f[]{
                            new Vector3f(0, extrude, 0), new Vector3f(0, -extrude, 0)
                        }, 1, false);
                Curve bevelCurve = new Curve(bevelSpline, bevResol);
                bevelObject = new ArrayList<Geometry>(1);
                bevelObject.add(new Geometry("", bevelCurve));
            }
        }

        //getting taper object
        Curve taperObject = null;
        Pointer pTaperObject = (Pointer) curveStructure.getFieldValue("taperobj");
        if (bevelObject != null && pTaperObject.isNotNull()) {
            Pointer pTaperStructure = (Pointer) pTaperObject.fetchData(blenderContext.getInputStream()).get(0).getFieldValue("data");
            Structure taperStructure = pTaperStructure.fetchData(blenderContext.getInputStream()).get(0);
            taperObject = this.loadTaperObject(taperStructure, blenderContext);
        }

        Vector3f loc = this.getLoc(curveStructure);
        //creating the result curves
        List<Geometry> result = new ArrayList<Geometry>(nurbs.size());
        for (Entry<Number, List<Structure>> nurbEntry : nurbs.entrySet()) {
            for (Structure nurb : nurbEntry.getValue()) {
                int type = ((Number) nurb.getFieldValue("type")).intValue();
                List<Geometry> nurbGeoms = null;
                if ((type & 0x01) != 0) {//Bezier curve
                    nurbGeoms = this.loadBezierCurve(loc, nurb, bevelObject, taperObject, blenderContext);
                } else if ((type & 0x04) != 0) {//NURBS
                    nurbGeoms = this.loadNurb(loc, nurb, bevelObject, taperObject, blenderContext);
                }
                if (nurbGeoms != null) {//setting the name and assigning materials
                    for (Geometry nurbGeom : nurbGeoms) {
                        nurbGeom.setMaterial(materials[nurbEntry.getKey().intValue()]);
                        nurbGeom.setName(name);
                        result.add(nurbGeom);
                    }
                }
            }
        }
        
        //reading custom properties
		Properties properties = this.loadProperties(curveStructure, blenderContext);
		if(properties != null && properties.getValue() != null) {
			for(Geometry geom : result) {
				geom.setUserData("properties", properties);
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
    protected List<Geometry> loadBezierCurve(Vector3f loc, Structure nurb, List<Geometry> bevelObject, Curve taperObject,
            BlenderContext blenderContext) throws BlenderFileException {
        Pointer pBezierTriple = (Pointer) nurb.getFieldValue("bezt");
        List<Geometry> result = new ArrayList<Geometry>();
        if (pBezierTriple.isNotNull()) {
            boolean smooth = (((Number) nurb.getFlatFieldValue("flag")).intValue() & 0x01) != 0;
            int resolution = ((Number) nurb.getFieldValue("resolu")).intValue();
            boolean cyclic = (((Number) nurb.getFieldValue("flagu")).intValue() & 0x01) != 0;

            //creating the curve object
            BezierCurve bezierCurve = new BezierCurve(0, pBezierTriple.fetchData(blenderContext.getInputStream()), 3);
            List<Vector3f> controlPoints = bezierCurve.getControlPoints();
            if (cyclic) {
                //copy the first three points at the end
                for (int i = 0; i < 3; ++i) {
                    controlPoints.add(controlPoints.get(i));
                }
            }
            //removing the first and last handles
            controlPoints.remove(0);
            controlPoints.remove(controlPoints.size() - 1);

            //creating curve
            Spline spline = new Spline(SplineType.Bezier, controlPoints, 0, false);
            Curve curve = new Curve(spline, resolution);
            if (bevelObject == null) {//creating a normal curve
                Geometry curveGeometry = new Geometry(null, curve);
                result.add(curveGeometry);
                //TODO: use front and back flags; surface excluding algorithm for bezier circles should be added
            } else {//creating curve with bevel and taper shape
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
    protected List<Geometry> loadNurb(Vector3f loc, Structure nurb, List<Geometry> bevelObject, Curve taperObject,
            BlenderContext blenderContext) throws BlenderFileException {
        //loading the knots
        List<Float>[] knots = new List[2];
        Pointer[] pKnots = new Pointer[]{(Pointer) nurb.getFieldValue("knotsu"), (Pointer) nurb.getFieldValue("knotsv")};
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

        //loading the flags and orders (basis functions degrees)
        int flagU = ((Number) nurb.getFieldValue("flagu")).intValue();
        int flagV = ((Number) nurb.getFieldValue("flagv")).intValue();
        int orderU = ((Number) nurb.getFieldValue("orderu")).intValue();
        int orderV = ((Number) nurb.getFieldValue("orderv")).intValue();

        //loading control points and their weights
        int pntsU = ((Number) nurb.getFieldValue("pntsu")).intValue();
        int pntsV = ((Number) nurb.getFieldValue("pntsv")).intValue();
        List<Structure> bPoints = ((Pointer) nurb.getFieldValue("bp")).fetchData(blenderContext.getInputStream());
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
        if (knots[1] == null) {//creating the curve
            Spline nurbSpline = new Spline(controlPoints.get(0), knots[0]);
            Curve nurbCurve = new Curve(nurbSpline, resolu);
            if (bevelObject != null) {
                result = this.applyBevelAndTaper(nurbCurve, bevelObject, taperObject, true, blenderContext);//TODO: smooth
            } else {
                result = new ArrayList<Geometry>(1);
                Geometry nurbGeometry = new Geometry("", nurbCurve);
                result.add(nurbGeometry);
            }
        } else {//creating the nurb surface
            int resolv = ((Number) nurb.getFieldValue("resolv")).intValue() + 1;
            Surface nurbSurface = Surface.createNurbsSurface(controlPoints, knots, resolu, resolv, orderU, orderV);
            Geometry nurbGeometry = new Geometry("", nurbSurface);
            result = new ArrayList<Geometry>(1);
            result.add(nurbGeometry);
        }
        return result;
    }

    /**
     * This method returns the taper scale that should be applied to the object.
     * @param taperPoints
     *            the taper points
     * @param taperLength
     *            the taper curve length
     * @param percent
     *            the percent of way along the whole taper curve
     * @param store
     *            the vector where the result will be stored
     */
    protected float getTaperScale(float[] taperPoints, float taperLength, float percent) {
        float length = taperLength * percent;
        float currentLength = 0;
        Vector3f p = new Vector3f();
        int i;
        for (i = 0; i < taperPoints.length - 6 && currentLength < length; i += 3) {
            p.set(taperPoints[i], taperPoints[i + 1], taperPoints[i + 2]);
            p.subtractLocal(taperPoints[i + 3], taperPoints[i + 4], taperPoints[i + 5]);
            currentLength += p.length();
        }
        currentLength -= p.length();
        float leftLength = length - currentLength;
        float percentOnSegment = p.length() == 0 ? 0 : leftLength / p.length();
        Vector3f store = FastMath.interpolateLinear(percentOnSegment,
                new Vector3f(taperPoints[i], taperPoints[i + 1], taperPoints[i + 2]),
                new Vector3f(taperPoints[i + 3], taperPoints[i + 4], taperPoints[i + 5]));
        return store.y;
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
     * 			  the smooth flag
     * @param blenderContext
     *            the blender context
     * @return a list of geometries representing the beveled and/or tapered curve
     */
    protected List<Geometry> applyBevelAndTaper(Curve curve, List<Geometry> bevelObject, Curve taperObject,
            boolean smooth, BlenderContext blenderContext) {
        float[] curvePoints = BufferUtils.getFloatArray(curve.getFloatBuffer(Type.Position));
        MeshHelper meshHelper = blenderContext.getHelper(MeshHelper.class);
        float curveLength = curve.getLength();
        //TODO: use the smooth var

        //taper data
        float[] taperPoints = null;
        float taperLength = 0;
        if (taperObject != null) {
            taperPoints = BufferUtils.getFloatArray(taperObject.getFloatBuffer(Type.Position));
            taperLength = taperObject.getLength();
        }

        //several objects can be allocated only once
        Vector3f p = new Vector3f();
        Vector3f z = new Vector3f(0, 0, 1);
        Vector3f negativeY = new Vector3f(0, -1, 0);
        Matrix4f m = new Matrix4f();
        float lengthAlongCurve = 0, taperScale = 1.0f;
        Quaternion planeRotation = new Quaternion();
        Quaternion zRotation = new Quaternion();
        float[] temp = new float[]{0, 0, 0, 1};
        Map<Vector3f, Vector3f> normalMap = new HashMap<Vector3f, Vector3f>();//normalMap merges normals of faces that will be rendered smooth

        FloatBuffer[] vertexBuffers = new FloatBuffer[bevelObject.size()];
        FloatBuffer[] normalBuffers = new FloatBuffer[bevelObject.size()];
        IntBuffer[] indexBuffers = new IntBuffer[bevelObject.size()];
        for (int geomIndex = 0; geomIndex < bevelObject.size(); ++geomIndex) {
            Mesh mesh = bevelObject.get(geomIndex).getMesh();
            FloatBuffer positions = mesh.getFloatBuffer(Type.Position);
            float[] vertices = BufferUtils.getFloatArray(positions);

            for (int i = 0; i < curvePoints.length; i += 3) {
                p.set(curvePoints[i], curvePoints[i + 1], curvePoints[i + 2]);
                Vector3f v;
                if (i == 0) {
                    v = new Vector3f(curvePoints[3] - p.x, curvePoints[4] - p.y, curvePoints[5] - p.z);
                } else if (i + 3 >= curvePoints.length) {
                    v = new Vector3f(p.x - curvePoints[i - 3], p.y - curvePoints[i - 2], p.z - curvePoints[i - 1]);
                    lengthAlongCurve += v.length();
                } else {
                    v = new Vector3f(curvePoints[i + 3] - curvePoints[i - 3],
                            curvePoints[i + 4] - curvePoints[i - 2],
                            curvePoints[i + 5] - curvePoints[i - 1]);
                    lengthAlongCurve += new Vector3f(curvePoints[i + 3] - p.x, curvePoints[i + 4] - p.y, curvePoints[i + 5] - p.z).length();
                }
                v.normalizeLocal();

                float angle = FastMath.acos(v.dot(z));
                v.crossLocal(z).normalizeLocal();//v is the rotation axis now
                planeRotation.fromAngleAxis(angle, v);

                Vector3f zAxisRotationVector = negativeY.cross(v).normalizeLocal();
                float zAxisRotationAngle = FastMath.acos(negativeY.dot(v));
                zRotation.fromAngleAxis(zAxisRotationAngle, zAxisRotationVector);

                //point transformation matrix
                if (taperPoints != null) {
                    taperScale = this.getTaperScale(taperPoints, taperLength, lengthAlongCurve / curveLength);
                }
                m.set(Matrix4f.IDENTITY);
                m.setRotationQuaternion(planeRotation.multLocal(zRotation));
                m.setTranslation(p);

                //these vertices need to be thrown on XY plane
                //and moved to the origin of [p1.x, p1.y] on the plane
                Vector3f[] verts = new Vector3f[vertices.length / 3];
                for (int j = 0; j < verts.length; ++j) {
                    temp[0] = vertices[j * 3] * taperScale;
                    temp[1] = vertices[j * 3 + 1] * taperScale;
                    temp[2] = 0;
                    m.mult(temp);//the result is stored in the array
                    if (fixUpAxis) {//TODO: not the other way ???
                        verts[j] = new Vector3f(temp[0], temp[1], temp[2]);
                    } else {
                        verts[j] = new Vector3f(temp[0], temp[2], -temp[1]);
                    }
                }
                if (vertexBuffers[geomIndex] == null) {
                    vertexBuffers[geomIndex] = BufferUtils.createFloatBuffer(verts.length * curvePoints.length);
                }
                FloatBuffer buffer = BufferUtils.createFloatBuffer(verts);
                vertexBuffers[geomIndex].put(buffer);

                //adding indexes
                IntBuffer indexBuffer = indexBuffers[geomIndex];
                if (indexBuffer == null) {
                    //the amount of faces in the final mesh is the amount of edges in the bevel curve
                    //(which is less by 1 than its number of vertices)
                    //multiplied by 2 (because each edge has two faces assigned on both sides)
                    //and multiplied by the amount of bevel curve repeats which is equal to the amount of vertices on the target curve
                    //finally we need to subtract the bevel edges amount 2 times because the border edges have only one face attached
                    //and at last multiply everything by 3 because each face needs 3 indexes to be described
                    int bevelCurveEdgesAmount = verts.length - 1;
                    indexBuffer = BufferUtils.createIntBuffer(((bevelCurveEdgesAmount << 1) * curvePoints.length - bevelCurveEdgesAmount << 1) * 3);
                    indexBuffers[geomIndex] = indexBuffer;
                }
                int pointOffset = i / 3 * verts.length;
                if (i + 3 < curvePoints.length) {
                    for (int index = 0; index < verts.length - 1; ++index) {
                        indexBuffer.put(index + pointOffset);
                        indexBuffer.put(index + pointOffset + 1);
                        indexBuffer.put(verts.length + index + pointOffset);
                        indexBuffer.put(verts.length + index + pointOffset);
                        indexBuffer.put(index + pointOffset + 1);
                        indexBuffer.put(verts.length + index + pointOffset + 1);
                    }
                }
            }
        }

        //calculating the normals
        for (int geomIndex = 0; geomIndex < bevelObject.size(); ++geomIndex) {
            Vector3f[] allVerts = BufferUtils.getVector3Array(vertexBuffers[geomIndex]);
            int[] allIndices = BufferUtils.getIntArray(indexBuffers[geomIndex]);
            for (int i = 0; i < allIndices.length - 3; i += 3) {
                Vector3f n = FastMath.computeNormal(allVerts[allIndices[i]], allVerts[allIndices[i + 1]], allVerts[allIndices[i + 2]]);
                meshHelper.addNormal(n, normalMap, smooth, allVerts[allIndices[i]], allVerts[allIndices[i + 1]], allVerts[allIndices[i + 2]]);
            }
            if (normalBuffers[geomIndex] == null) {
                normalBuffers[geomIndex] = BufferUtils.createFloatBuffer(allVerts.length * 3);
            }
            for (Vector3f v : allVerts) {
                Vector3f n = normalMap.get(v);
                normalBuffers[geomIndex].put(n.x);
                normalBuffers[geomIndex].put(n.y);
                normalBuffers[geomIndex].put(n.z);
            }
        }

        List<Geometry> result = new ArrayList<Geometry>(vertexBuffers.length);
        Float oneReferenceToCurveLength = new Float(curveLength);//its important for array modifier to use one reference here
        for (int i = 0; i < vertexBuffers.length; ++i) {
            Mesh mesh = new Mesh();
            mesh.setBuffer(Type.Position, 3, vertexBuffers[i]);
            mesh.setBuffer(Type.Index, 3, indexBuffers[i]);
            mesh.setBuffer(Type.Normal, 3, normalBuffers[i]);
            Geometry g = new Geometry("g" + i, mesh);
            g.setUserData("curveLength", oneReferenceToCurveLength);
            g.updateModelBound();
            result.add(g);
        }

        return result;
    }

    /**
     * This method loads the taper object.
     * @param taperStructure
     *            the taper structure
     * @param blenderContext
     *            the blender context
     * @return the taper object
     * @throws BlenderFileException
     */
    protected Curve loadTaperObject(Structure taperStructure, BlenderContext blenderContext) throws BlenderFileException {
        //reading nurbs
        List<Structure> nurbStructures = ((Structure) taperStructure.getFieldValue("nurb")).evaluateListBase(blenderContext);
        for (Structure nurb : nurbStructures) {
            Pointer pBezierTriple = (Pointer) nurb.getFieldValue("bezt");
            if (pBezierTriple.isNotNull()) {
                //creating the curve object
                BezierCurve bezierCurve = new BezierCurve(0, pBezierTriple.fetchData(blenderContext.getInputStream()), 3);
                List<Vector3f> controlPoints = bezierCurve.getControlPoints();
                //removing the first and last handles
                controlPoints.remove(0);
                controlPoints.remove(controlPoints.size() - 1);

                //return the first taper curve that has more than 3 control points
                if (controlPoints.size() > 3) {
                    Spline spline = new Spline(SplineType.Bezier, controlPoints, 0, false);
                    int resolution = ((Number) taperStructure.getFieldValue("resolu")).intValue();
                    return new Curve(spline, resolution);
                }
            }
        }
        return null;
    }

    /**
     * This method returns the translation of the curve. The UP axis is taken into account here.
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
    
    @Override
    public boolean shouldBeLoaded(Structure structure, BlenderContext blenderContext) {
    	return true;
    }
}
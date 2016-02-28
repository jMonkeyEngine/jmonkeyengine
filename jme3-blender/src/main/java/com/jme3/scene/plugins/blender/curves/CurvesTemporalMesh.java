package com.jme3.scene.plugins.blender.curves;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Logger;

import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.FastMath;
import com.jme3.math.Spline;
import com.jme3.math.Spline.SplineType;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.file.BlenderFileException;
import com.jme3.scene.plugins.blender.file.BlenderInputStream;
import com.jme3.scene.plugins.blender.file.DynamicArray;
import com.jme3.scene.plugins.blender.file.FileBlockHeader;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.materials.MaterialContext;
import com.jme3.scene.plugins.blender.materials.MaterialHelper;
import com.jme3.scene.plugins.blender.meshes.Edge;
import com.jme3.scene.plugins.blender.meshes.Face;
import com.jme3.scene.plugins.blender.meshes.TemporalMesh;
import com.jme3.scene.shape.Curve;
import com.jme3.scene.shape.Surface;
import com.jme3.util.BufferUtils;

/**
 * A temporal mesh for curves and surfaces. It works in similar way as TemporalMesh for meshes.
 * It prepares all neccessary lines and faces and allows to apply modifiers just like in regular temporal mesh.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public class CurvesTemporalMesh extends TemporalMesh {
    private static final Logger  LOGGER         = Logger.getLogger(CurvesTemporalMesh.class.getName());

    private static final int     TYPE_BEZIER    = 0x0001;
    private static final int     TYPE_NURBS     = 0x0004;

    private static final int     FLAG_3D        = 0x0001;
    private static final int     FLAG_FRONT     = 0x0002;
    private static final int     FLAG_BACK      = 0x0004;
    private static final int     FLAG_FILL_CAPS = 0x4000;

    private static final int     FLAG_SMOOTH    = 0x0001;

    protected CurvesHelper       curvesHelper;
    protected boolean            is2D;
    protected boolean            isFront;
    protected boolean            isBack;
    protected boolean            fillCaps;
    protected float              bevelStart;
    protected float              bevelEnd;
    protected List<BezierLine>   beziers        = new ArrayList<BezierLine>();
    protected CurvesTemporalMesh bevelObject;
    protected CurvesTemporalMesh taperObject;
    /** The scale that is used if the curve is a bevel or taper curve. */
    protected Vector3f           scale          = new Vector3f(1, 1, 1);

    /**
     * The constructor creates an empty temporal mesh.
     * @param blenderContext
     *            the blender context
     * @throws BlenderFileException
     *             this will never be thrown here
     */
    protected CurvesTemporalMesh(BlenderContext blenderContext) throws BlenderFileException {
        super(null, blenderContext, false);
    }
    
    /**
     * Loads the temporal mesh from the given curve structure. The mesh can be either curve or surface.
     * @param curveStructure
     *            the structure that contains the curve/surface data
     * @param blenderContext
     *            the blender context
     * @throws BlenderFileException
     *             an exception is thrown when problems with reading occur
     */
    public CurvesTemporalMesh(Structure curveStructure, BlenderContext blenderContext) throws BlenderFileException {
        this(curveStructure, new Vector3f(1, 1, 1), true, blenderContext);
    }

    /**
     * Loads the temporal mesh from the given curve structure. The mesh can be either curve or surface.
     * @param curveStructure
     *            the structure that contains the curve/surface data
     * @param scale
     *            the scale used if the current curve is used as a bevel curve
     * @param loadBevelAndTaper indicates if bevel and taper should be loaded (this is not needed for curves that are loaded to be used as bevel and taper)
     * @param blenderContext
     *            the blender context
     * @throws BlenderFileException
     *             an exception is thrown when problems with reading occur
     */
    @SuppressWarnings("unchecked")
    private CurvesTemporalMesh(Structure curveStructure, Vector3f scale, boolean loadBevelAndTaper, BlenderContext blenderContext) throws BlenderFileException {
        super(curveStructure, blenderContext, false);
        name = curveStructure.getName();
        curvesHelper = blenderContext.getHelper(CurvesHelper.class);
        this.scale = scale;

        int flag = ((Number) curveStructure.getFieldValue("flag")).intValue();
        is2D = (flag & FLAG_3D) == 0;
        if (is2D) {
            // TODO: add support for 3D flag
            LOGGER.warning("2D flag not yet supported for curves!");
        }
        isFront = (flag & FLAG_FRONT) != 0;
        isBack = (flag & FLAG_BACK) != 0;
        fillCaps = (flag & FLAG_FILL_CAPS) != 0;
        bevelStart = ((Number) curveStructure.getFieldValue("bevfac1", 0)).floatValue();
        bevelEnd = ((Number) curveStructure.getFieldValue("bevfac2", 1)).floatValue();
        if (bevelStart > bevelEnd) {
            float temp = bevelStart;
            bevelStart = bevelEnd;
            bevelEnd = temp;
        }

        LOGGER.fine("Reading nurbs (and sorting them by material).");
        Map<Number, List<Structure>> nurbs = new HashMap<Number, List<Structure>>();
        List<Structure> nurbStructures = ((Structure) curveStructure.getFieldValue("nurb")).evaluateListBase();
        for (Structure nurb : nurbStructures) {
            Number matNumber = (Number) nurb.getFieldValue("mat_nr");
            List<Structure> nurbList = nurbs.get(matNumber);
            if (nurbList == null) {
                nurbList = new ArrayList<Structure>();
                nurbs.put(matNumber, nurbList);
            }
            nurbList.add(nurb);
        }

        LOGGER.fine("Getting materials.");
        MaterialHelper materialHelper = blenderContext.getHelper(MaterialHelper.class);
        materials = materialHelper.getMaterials(curveStructure, blenderContext);
        if (materials != null) {
            for (MaterialContext materialContext : materials) {
                materialContext.setFaceCullMode(FaceCullMode.Off);
            }
        }

        LOGGER.fine("Getting or creating bevel object.");
        bevelObject = loadBevelAndTaper ? this.loadBevelObject(curveStructure) : null;

        LOGGER.fine("Getting taper object.");
        Pointer pTaperObject = (Pointer) curveStructure.getFieldValue("taperobj");
        if (bevelObject != null && pTaperObject.isNotNull()) {
            Structure taperObjectStructure = pTaperObject.fetchData().get(0);
            DynamicArray<Number> scaleArray = (DynamicArray<Number>) taperObjectStructure.getFieldValue("size");
            scale = blenderContext.getBlenderKey().isFixUpAxis() ? new Vector3f(scaleArray.get(0).floatValue(), scaleArray.get(1).floatValue(), scaleArray.get(2).floatValue()) : new Vector3f(scaleArray.get(0).floatValue(), scaleArray.get(2).floatValue(), scaleArray.get(1).floatValue());
            Pointer pTaperStructure = (Pointer) taperObjectStructure.getFieldValue("data");
            Structure taperStructure = pTaperStructure.fetchData().get(0);
            taperObject = new CurvesTemporalMesh(taperStructure, blenderContext);
        }

        LOGGER.fine("Creating the result curves.");
        for (Entry<Number, List<Structure>> nurbEntry : nurbs.entrySet()) {
            for (Structure nurb : nurbEntry.getValue()) {
                int type = ((Number) nurb.getFieldValue("type")).intValue();
                if ((type & TYPE_BEZIER) != 0) {
                    this.loadBezierCurve(nurb, nurbEntry.getKey().intValue());
                } else if ((type & TYPE_NURBS) != 0) {
                    this.loadNurbSurface(nurb, nurbEntry.getKey().intValue());
                } else {
                    throw new BlenderFileException("Unknown curve type: " + type);
                }
            }
        }

        if (bevelObject != null && beziers.size() > 0) {
            this.append(this.applyBevelAndTaper(this, bevelObject, taperObject, blenderContext));
        } else {
            for (BezierLine bezierLine : beziers) {
            	int originalVerticesAmount = vertices.size();
                vertices.add(bezierLine.vertices[0]);
                Vector3f v = bezierLine.vertices[1].subtract(bezierLine.vertices[0]).normalizeLocal();
                float temp = v.x;
                v.x = -v.y;
                v.y = temp;
                v.z = 0;
                normals.add(v);// this will be smoothed in the next iteration

                for (int i = 1; i < bezierLine.vertices.length; ++i) {
                    vertices.add(bezierLine.vertices[i]);
                    edges.add(new Edge(originalVerticesAmount + i - 1, originalVerticesAmount + i, 0, false, this));

                    // generating normal for vertex at 'i'
                    v = bezierLine.vertices[i].subtract(bezierLine.vertices[i - 1]).normalizeLocal();
                    temp = v.x;
                    v.x = -v.y;
                    v.y = temp;
                    v.z = 0;

                    // make the previous normal smooth
                    normals.get(i - 1).addLocal(v).multLocal(0.5f).normalizeLocal();
                    normals.add(v);// this will be smoothed in the next iteration
                }
            }
        }
    }

    /**
     * The method computes the value of a point at the certain relational distance from its beggining.
     * @param alongRatio
     *            the relative distance along the curve; should be a value between 0 and 1 inclusive;
     *            if the value exceeds the boundaries it is truncated to them
     * @return computed value along the curve
     */
    private Vector3f getValueAlongCurve(float alongRatio) {
        alongRatio = FastMath.clamp(alongRatio, 0, 1);
        Vector3f result = new Vector3f();
        float probeLength = this.getLength() * alongRatio, length = 0;
        for (BezierLine bezier : beziers) {
            float edgeLength = bezier.getLength();
            if (length + edgeLength >= probeLength) {
                float ratioAlongEdge = (probeLength - length) / edgeLength;
                return bezier.getValueAlongCurve(ratioAlongEdge);
            }
            length += edgeLength;
        }
        return result;
    }

    /**
     * @return the length of the curve
     */
    private float getLength() {
        float result = 0;
        for (BezierLine bezier : beziers) {
            result += bezier.getLength();
        }
        return result;
    }

    /**
     * The methods loads the bezier curve from the given structure.
     * @param nurbStructure
     *            the structure containing a single curve definition
     * @param materialIndex
     *            the index of this segment's material
     * @throws BlenderFileException
     *             an exception is thrown when problems with reading occur
     */
    private void loadBezierCurve(Structure nurbStructure, int materialIndex) throws BlenderFileException {
        Pointer pBezierTriple = (Pointer) nurbStructure.getFieldValue("bezt");
        if (pBezierTriple.isNotNull()) {
            int resolution = ((Number) nurbStructure.getFieldValue("resolu")).intValue();
            boolean cyclic = (((Number) nurbStructure.getFieldValue("flagu")).intValue() & 0x01) != 0;
            boolean smooth = (((Number) nurbStructure.getFieldValue("flag")).intValue() & FLAG_SMOOTH) != 0;

            // creating the curve object
            BezierCurve bezierCurve = new BezierCurve(0, pBezierTriple.fetchData(), 3, blenderContext.getBlenderKey().isFixUpAxis());
            List<Vector3f> controlPoints = bezierCurve.getControlPoints();

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
            Curve curve = new Curve(new Spline(SplineType.Bezier, controlPoints, 0, false), resolution);

            FloatBuffer vertsBuffer = (FloatBuffer) curve.getBuffer(Type.Position).getData();
            beziers.add(new BezierLine(BufferUtils.getVector3Array(vertsBuffer), materialIndex, smooth, cyclic));
        }
    }

    /**
     * This method loads the NURBS curve or surface.
     * @param nurb
     *            the NURBS data structure
     * @throws BlenderFileException
     *             an exception is thrown when problems with reading occur
     */
    @SuppressWarnings("unchecked")
    private void loadNurbSurface(Structure nurb, int materialIndex) throws BlenderFileException {
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
        int flag = ((Number) nurb.getFieldValue("flag")).intValue();
        boolean smooth = (flag & FLAG_SMOOTH) != 0;
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
                if (blenderContext.getBlenderKey().isFixUpAxis()) {
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

        int originalVerticesAmount = vertices.size();
        int resolu = ((Number) nurb.getFieldValue("resolu")).intValue();
        if (knots[1] == null) {// creating the NURB curve
            Curve curve = new Curve(new Spline(controlPoints.get(0), knots[0]), resolu);
            FloatBuffer vertsBuffer = (FloatBuffer) curve.getBuffer(Type.Position).getData();
            beziers.add(new BezierLine(BufferUtils.getVector3Array(vertsBuffer), materialIndex, smooth, false));
        } else {// creating the NURB surface
            int resolv = ((Number) nurb.getFieldValue("resolv")).intValue();
            int uSegments = resolu * controlPoints.get(0).size() - 1;
            int vSegments = resolv * controlPoints.size() - 1;
            Surface nurbSurface = Surface.createNurbsSurface(controlPoints, knots, uSegments, vSegments, orderU, orderV, smooth);

            FloatBuffer vertsBuffer = (FloatBuffer) nurbSurface.getBuffer(Type.Position).getData();
            vertices.addAll(Arrays.asList(BufferUtils.getVector3Array(vertsBuffer)));
            FloatBuffer normalsBuffer = (FloatBuffer) nurbSurface.getBuffer(Type.Normal).getData();
            normals.addAll(Arrays.asList(BufferUtils.getVector3Array(normalsBuffer)));

            IndexBuffer indexBuffer = nurbSurface.getIndexBuffer();
            for (int i = 0; i < indexBuffer.size(); i += 3) {
                int index1 = indexBuffer.get(i) + originalVerticesAmount;
                int index2 = indexBuffer.get(i + 1) + originalVerticesAmount;
                int index3 = indexBuffer.get(i + 2) + originalVerticesAmount;
                faces.add(new Face(new Integer[] { index1, index2, index3 }, smooth, materialIndex, null, null, this));
            }
        }
    }

    /**
     * The method loads the bevel object that sould be applied to curve. It can either be another curve or a generated one
     * based on the bevel generating parameters in blender.
     * @param curveStructure
     *            the structure with the curve's data (the curve being loaded, NOT the bevel curve)
     * @return the curve's bevel object
     * @throws BlenderFileException
     *             an exception is thrown when problems with reading occur
     */
    @SuppressWarnings("unchecked")
    private CurvesTemporalMesh loadBevelObject(Structure curveStructure) throws BlenderFileException {
        CurvesTemporalMesh bevelObject = null;
        Pointer pBevelObject = (Pointer) curveStructure.getFieldValue("bevobj");
        boolean cyclic = false;
        if (pBevelObject.isNotNull()) {
            Structure bevelObjectStructure = pBevelObject.fetchData().get(0);
            DynamicArray<Number> scaleArray = (DynamicArray<Number>) bevelObjectStructure.getFieldValue("size");
            Vector3f scale = blenderContext.getBlenderKey().isFixUpAxis() ? new Vector3f(scaleArray.get(0).floatValue(), scaleArray.get(1).floatValue(), scaleArray.get(2).floatValue()) : new Vector3f(scaleArray.get(0).floatValue(), scaleArray.get(2).floatValue(), scaleArray.get(1).floatValue());
            Pointer pBevelStructure = (Pointer) bevelObjectStructure.getFieldValue("data");
            Structure bevelStructure = pBevelStructure.fetchData().get(0);
            bevelObject = new CurvesTemporalMesh(bevelStructure, scale, false, blenderContext);

            // transforming the bezier lines from plane XZ to plane YZ
            for (BezierLine bl : bevelObject.beziers) {
                for (Vector3f v : bl.vertices) {
                    // casting the bezier curve orthogonally on the plane XZ (making Y = 0) and then moving the plane XZ to ZY in a way that:
                    // -Z => +Y and +X => +Z and +Y => +X (but because casting would make Y = 0, then we simply set X = 0)
                    v.y = -v.z;
                    v.z = v.x;
                    v.x = 0;
                }

                // bevel curves should not have repeated the first vertex at the end when they are cyclic (this is handled differently)
                if (bl.isCyclic()) {
                    bl.removeLastVertex();
                }
            }
        } else {
            fillCaps = false;// this option is inactive in blender when there is no bevel object applied
            int bevResol = ((Number) curveStructure.getFieldValue("bevresol")).intValue();
            float extrude = ((Number) curveStructure.getFieldValue("ext1")).floatValue();
            float bevelDepth = ((Number) curveStructure.getFieldValue("ext2")).floatValue();
            float offset = ((Number) curveStructure.getFieldValue("offset", 0)).floatValue();
            if (offset != 0) {
                // TODO: add support for offset parameter
                LOGGER.warning("Offset parameter not yet supported.");
            }
            Curve bevelCurve = null;
            if (bevelDepth > 0.0f) {
                float handlerLength = bevelDepth / 2.0f;
                cyclic = !isFront && !isBack;
                List<Vector3f> conrtolPoints = new ArrayList<Vector3f>();

                // blenders from 2.49 to 2.52 did not pay attention to fron and back faces
                // so in order to draw the scene exactly as it is in different blender versions the blender version is checked here
                // when neither fron and back face is selected all version behave the same and draw full bevel around the curve
                if (cyclic || blenderContext.getBlenderVersion() < 253) {
                    conrtolPoints.add(new Vector3f(0, -extrude - bevelDepth, 0));
                    conrtolPoints.add(new Vector3f(0, -extrude - bevelDepth, -handlerLength));

                    conrtolPoints.add(new Vector3f(0, -extrude - handlerLength, -bevelDepth));
                    conrtolPoints.add(new Vector3f(0, -extrude, -bevelDepth));
                    conrtolPoints.add(new Vector3f(0, -extrude + handlerLength, -bevelDepth));

                    if (extrude > 0) {
                        conrtolPoints.add(new Vector3f(0, extrude - handlerLength, -bevelDepth));
                        conrtolPoints.add(new Vector3f(0, extrude, -bevelDepth));
                        conrtolPoints.add(new Vector3f(0, extrude + handlerLength, -bevelDepth));
                    }

                    conrtolPoints.add(new Vector3f(0, extrude + bevelDepth, -handlerLength));
                    conrtolPoints.add(new Vector3f(0, extrude + bevelDepth, 0));

                    if (cyclic) {
                        conrtolPoints.add(new Vector3f(0, extrude + bevelDepth, handlerLength));

                        conrtolPoints.add(new Vector3f(0, extrude + handlerLength, bevelDepth));
                        conrtolPoints.add(new Vector3f(0, extrude, bevelDepth));
                        conrtolPoints.add(new Vector3f(0, extrude - handlerLength, bevelDepth));

                        if (extrude > 0) {
                            conrtolPoints.add(new Vector3f(0, -extrude + handlerLength, bevelDepth));
                            conrtolPoints.add(new Vector3f(0, -extrude, bevelDepth));
                            conrtolPoints.add(new Vector3f(0, -extrude - handlerLength, bevelDepth));
                        }

                        conrtolPoints.add(new Vector3f(0, -extrude - bevelDepth, handlerLength));
                        conrtolPoints.add(new Vector3f(0, -extrude - bevelDepth, 0));
                    }
                } else {
                    if (extrude > 0) {
                        if (isBack) {
                            conrtolPoints.add(new Vector3f(0, -extrude - bevelDepth, 0));
                            conrtolPoints.add(new Vector3f(0, -extrude - bevelDepth, -handlerLength));

                            conrtolPoints.add(new Vector3f(0, -extrude - handlerLength, -bevelDepth));
                        }

                        conrtolPoints.add(new Vector3f(0, -extrude, -bevelDepth));
                        conrtolPoints.add(new Vector3f(0, -extrude + handlerLength, -bevelDepth));
                        conrtolPoints.add(new Vector3f(0, extrude - handlerLength, -bevelDepth));
                        conrtolPoints.add(new Vector3f(0, extrude, -bevelDepth));

                        if (isFront) {
                            conrtolPoints.add(new Vector3f(0, extrude + handlerLength, -bevelDepth));

                            conrtolPoints.add(new Vector3f(0, extrude + bevelDepth, -handlerLength));
                            conrtolPoints.add(new Vector3f(0, extrude + bevelDepth, 0));
                        }
                    } else {
                        if (isFront && isBack) {
                            conrtolPoints.add(new Vector3f(0, -bevelDepth, 0));
                            conrtolPoints.add(new Vector3f(0, -bevelDepth, -handlerLength));

                            conrtolPoints.add(new Vector3f(0, -handlerLength, -bevelDepth));
                            conrtolPoints.add(new Vector3f(0, 0, -bevelDepth));
                            conrtolPoints.add(new Vector3f(0, handlerLength, -bevelDepth));

                            conrtolPoints.add(new Vector3f(0, bevelDepth, -handlerLength));
                            conrtolPoints.add(new Vector3f(0, bevelDepth, 0));
                        } else {
                            if (isBack) {
                                conrtolPoints.add(new Vector3f(0, -bevelDepth, 0));
                                conrtolPoints.add(new Vector3f(0, -bevelDepth, -handlerLength));

                                conrtolPoints.add(new Vector3f(0, -handlerLength, -bevelDepth));
                                conrtolPoints.add(new Vector3f(0, 0, -bevelDepth));
                            } else {
                                conrtolPoints.add(new Vector3f(0, 0, -bevelDepth));
                                conrtolPoints.add(new Vector3f(0, handlerLength, -bevelDepth));

                                conrtolPoints.add(new Vector3f(0, bevelDepth, -handlerLength));
                                conrtolPoints.add(new Vector3f(0, bevelDepth, 0));
                            }
                        }
                    }
                }

                bevelCurve = new Curve(new Spline(SplineType.Bezier, conrtolPoints, 0, false), bevResol);
            } else if (extrude > 0.0f) {
                Spline bevelSpline = new Spline(SplineType.Linear, new Vector3f[] { new Vector3f(0, extrude, 0), new Vector3f(0, -extrude, 0) }, 1, false);
                bevelCurve = new Curve(bevelSpline, bevResol);
            }
            if (bevelCurve != null) {
                bevelObject = new CurvesTemporalMesh(blenderContext);
                FloatBuffer vertsBuffer = (FloatBuffer) bevelCurve.getBuffer(Type.Position).getData();
                Vector3f[] verts = BufferUtils.getVector3Array(vertsBuffer);
                if (cyclic) {// get rid of the last vertex which is identical to the first one
                    verts = Arrays.copyOf(verts, verts.length - 1);
                }
                bevelObject.beziers.add(new BezierLine(verts, 0, false, cyclic));
            }
        }
        return bevelObject;
    }

    private List<BezierLine> getScaledBeziers() {
        if (scale.equals(Vector3f.UNIT_XYZ)) {
            return beziers;
        }
        List<BezierLine> result = new ArrayList<BezierLine>();
        for (BezierLine bezierLine : beziers) {
            result.add(bezierLine.scale(scale));
        }
        return result;
    }

    /**
     * This method applies bevel and taper objects to the curve.
     * @param curve
     *            the curve we apply the objects to
     * @param bevelObject
     *            the bevel object
     * @param taperObject
     *            the taper object
     * @param blenderContext
     *            the blender context
     * @return a list of geometries representing the beveled and/or tapered curve
     * @throws BlenderFileException
     *             an exception is thrown when problems with reading occur
     */
    private CurvesTemporalMesh applyBevelAndTaper(CurvesTemporalMesh curve, CurvesTemporalMesh bevelObject, CurvesTemporalMesh taperObject, BlenderContext blenderContext) throws BlenderFileException {
        List<BezierLine> bevelBezierLines = bevelObject.getScaledBeziers();
        List<BezierLine> curveLines = curve.beziers;
        if (bevelBezierLines.size() == 0 || curveLines.size() == 0) {
            return null;
        }

        CurvesTemporalMesh result = new CurvesTemporalMesh(blenderContext);
        for (BezierLine curveLine : curveLines) {
            Vector3f[] curveLineVertices = curveLine.getVertices(bevelStart, bevelEnd);

            for (BezierLine bevelBezierLine : bevelBezierLines) {
                CurvesTemporalMesh partResult = new CurvesTemporalMesh(blenderContext);

                Vector3f[] bevelLineVertices = bevelBezierLine.getVertices();
                List<Vector3f[]> bevels = new ArrayList<Vector3f[]>();

                Vector3f[] bevelPoints = curvesHelper.transformToFirstLineOfBevelPoints(bevelLineVertices, curveLineVertices[0], curveLineVertices[1]);
                bevels.add(bevelPoints);
                for (int i = 1; i < curveLineVertices.length - 1; ++i) {
                    bevelPoints = curvesHelper.transformBevel(bevelPoints, curveLineVertices[i - 1], curveLineVertices[i], curveLineVertices[i + 1]);
                    bevels.add(bevelPoints);
                }
                bevelPoints = curvesHelper.transformBevel(bevelPoints, curveLineVertices[curveLineVertices.length - 2], curveLineVertices[curveLineVertices.length - 1], null);
                bevels.add(bevelPoints);

                Vector3f subtractResult = new Vector3f();
                if (bevels.size() > 2) {
                    // changing the first and last bevel so that they are parallel to their neighbours (blender works this way)
                    // notice this implicates that the distances of every corresponding point in the two bevels must be identical and
                    // equal to the distance between the points on curve that define the bevel position
                    // so instead doing complicated rotations on each point we will simply properly translate each of them
                    int[][] pointIndexes = new int[][] { { 0, 1 }, { curveLineVertices.length - 1, curveLineVertices.length - 2 } };
                    for (int[] indexes : pointIndexes) {
                        float distance = curveLineVertices[indexes[1]].subtract(curveLineVertices[indexes[0]], subtractResult).length();
                        Vector3f[] bevel = bevels.get(indexes[0]);
                        Vector3f[] nextBevel = bevels.get(indexes[1]);
                        for (int i = 0; i < bevel.length; ++i) {
                            float d = bevel[i].subtract(nextBevel[i], subtractResult).length();
                            subtractResult.normalizeLocal().multLocal(distance - d);
                            bevel[i].addLocal(subtractResult);
                        }
                    }
                }

                if (taperObject != null) {
                    float curveLength = curveLine.getLength(), lengthAlongCurve = bevelStart;
                    for (int i = 0; i < curveLineVertices.length; ++i) {
                        if (i > 0) {
                            lengthAlongCurve += curveLineVertices[i].subtract(curveLineVertices[i - 1], subtractResult).length();
                        }
                        float taperScale = -taperObject.getValueAlongCurve(lengthAlongCurve / curveLength).z * taperObject.scale.z;
                        if (taperScale != 1) {
                            this.applyScale(bevels.get(i), curveLineVertices[i], taperScale);
                        }
                    }
                }

                // adding vertices to the part result
                for (Vector3f[] bevel : bevels) {
                    for (Vector3f d : bevel) {
                        partResult.getVertices().add(d);
                    }
                }

                // preparing faces for the part result (each face is a quad)
                int bevelVertCount = bevelPoints.length;
                for (int i = 0; i < bevels.size() - 1; ++i) {
                    for (int j = 0; j < bevelVertCount - 1; ++j) {
                        Integer[] indexes = new Integer[] { i * bevelVertCount + j + 1, (i + 1) * bevelVertCount + j + 1, (i + 1) * bevelVertCount + j, i * bevelVertCount + j };
                        partResult.getFaces().add(new Face(indexes, curveLine.isSmooth(), curveLine.getMaterialNumber(), null, null, partResult));
                        partResult.getEdges().add(new Edge(indexes[0], indexes[1], 0, true, partResult));
                        partResult.getEdges().add(new Edge(indexes[1], indexes[2], 0, true, partResult));
                        partResult.getEdges().add(new Edge(indexes[2], indexes[3], 0, true, partResult));
                        partResult.getEdges().add(new Edge(indexes[3], indexes[0], 0, true, partResult));
                    }
                    if (bevelBezierLine.isCyclic()) {
                        int j = bevelVertCount - 1;
                        Integer[] indexes = new Integer[] { i * bevelVertCount, (i + 1) * bevelVertCount, (i + 1) * bevelVertCount + j, i * bevelVertCount + j };
                        partResult.getFaces().add(new Face(indexes, curveLine.isSmooth(), curveLine.getMaterialNumber(), null, null, partResult));
                        partResult.getEdges().add(new Edge(indexes[0], indexes[1], 0, true, partResult));
                        partResult.getEdges().add(new Edge(indexes[1], indexes[2], 0, true, partResult));
                        partResult.getEdges().add(new Edge(indexes[2], indexes[3], 0, true, partResult));
                        partResult.getEdges().add(new Edge(indexes[3], indexes[0], 0, true, partResult));
                    }
                }

                partResult.generateNormals();

                if (fillCaps) {// caps in blender behave as if they weren't affected by the smooth factor
                    // START CAP
                    Vector3f[] cap = bevels.get(0);
                    List<Integer> capIndexes = new ArrayList<Integer>(cap.length);
                    Vector3f capNormal = curveLineVertices[0].subtract(curveLineVertices[1]).normalizeLocal();
                    for (int i = 0; i < cap.length; ++i) {
                        capIndexes.add(partResult.getVertices().size());
                        partResult.getVertices().add(cap[i]);
                        partResult.getNormals().add(capNormal);
                    }
                    Collections.reverse(capIndexes);// the indexes ned to be reversed for the face to have fron face outside the beveled line
                    partResult.getFaces().add(new Face(capIndexes.toArray(new Integer[capIndexes.size()]), false, curveLine.getMaterialNumber(), null, null, partResult));
                    for (int i = 1; i < capIndexes.size(); ++i) {
                        partResult.getEdges().add(new Edge(capIndexes.get(i - 1), capIndexes.get(i), 0, true, partResult));
                    }

                    // END CAP
                    cap = bevels.get(bevels.size() - 1);
                    capIndexes.clear();
                    capNormal = curveLineVertices[curveLineVertices.length - 1].subtract(curveLineVertices[curveLineVertices.length - 2]).normalizeLocal();
                    for (int i = 0; i < cap.length; ++i) {
                        capIndexes.add(partResult.getVertices().size());
                        partResult.getVertices().add(cap[i]);
                        partResult.getNormals().add(capNormal);
                    }
                    partResult.getFaces().add(new Face(capIndexes.toArray(new Integer[capIndexes.size()]), false, curveLine.getMaterialNumber(), null, null, partResult));
                    for (int i = 1; i < capIndexes.size(); ++i) {
                        partResult.getEdges().add(new Edge(capIndexes.get(i - 1), capIndexes.get(i), 0, true, partResult));
                    }
                }

                result.append(partResult);
            }
        }

        return result;
    }

    /**
     * The method generates normals for the curve. If any normals were already stored they are discarded.
     */
    private void generateNormals() {
        Map<Integer, Vector3f> normalMap = new TreeMap<Integer, Vector3f>();
        for (Face face : faces) {
            // the first 3 verts are enough here (all faces are triangles except for the caps, but those are fully flat anyway)
            int index1 = face.getIndexes().get(0);
            int index2 = face.getIndexes().get(1);
            int index3 = face.getIndexes().get(2);

            Vector3f n = FastMath.computeNormal(vertices.get(index1), vertices.get(index2), vertices.get(index3));
            for (int index : face.getIndexes()) {
                Vector3f normal = normalMap.get(index);
                if (normal == null) {
                    normalMap.put(index, n.clone());
                } else {
                    normal.addLocal(n).normalizeLocal();
                }
            }
        }

        normals.clear();
        Collections.addAll(normals, new Vector3f[normalMap.size()]);
        for (Entry<Integer, Vector3f> entry : normalMap.entrySet()) {
            normals.set(entry.getKey(), entry.getValue());
        }
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
     * A helper class that represents a single bezier line. It consists of Edge's and allows to
     * get a subline of a lentgh of the line.
     * 
     * @author Marcin Roguski (Kaelthas)
     */
    public static class BezierLine {
        /** The edges of the bezier line. */
        private Vector3f[] vertices;
        /** The material number of the line. */
        private int        materialNumber;
        /** Indicates if the line is smooth of flat. */
        private boolean    smooth;
        /** The length of the line. */
        private float      length;
        /** Indicates if the current line is cyclic or not. */
        private boolean    cyclic;

        public BezierLine(Vector3f[] vertices, int materialNumber, boolean smooth, boolean cyclik) {
            this.vertices = vertices;
            this.materialNumber = materialNumber;
            this.smooth = smooth;
            cyclic = cyclik;
            this.recomputeLength();
        }

        public BezierLine scale(Vector3f scale) {
            BezierLine result = new BezierLine(vertices, materialNumber, smooth, cyclic);
            result.vertices = new Vector3f[vertices.length];
            for (int i = 0; i < vertices.length; ++i) {
                result.vertices[i] = vertices[i].mult(scale);
            }
            result.recomputeLength();
            return result;
        }

        public void removeLastVertex() {
            Vector3f[] newVertices = new Vector3f[vertices.length - 1];
            for (int i = 0; i < vertices.length - 1; ++i) {
                newVertices[i] = vertices[i];
            }
            vertices = newVertices;
            this.recomputeLength();
        }

        private void recomputeLength() {
            length = 0;
            for (int i = 1; i < vertices.length; ++i) {
                length += vertices[i - 1].distance(vertices[i]);
            }
            if (cyclic) {
                // if the first vertex is repeated at the end the distance will be = 0 so it won't affect the result, and if it is not repeated
                // then it is neccessary to add the length between the last and the first vertex
                length += vertices[vertices.length - 1].distance(vertices[0]);
            }
        }

        public Vector3f[] getVertices() {
            return this.getVertices(0, 1);
        }

        public Vector3f[] getVertices(float startSlice, float endSlice) {
            if (startSlice == 0 && endSlice == 1) {
                return vertices;
            }
            List<Vector3f> result = new ArrayList<Vector3f>();
            float length = this.getLength(), temp = 0;
            float startSliceLength = length * startSlice;
            float endSliceLength = length * endSlice;
            int index = 1;

            if (startSlice > 0) {
                while (temp < startSliceLength) {
                    Vector3f v1 = vertices[index - 1];
                    Vector3f v2 = vertices[index++];
                    float edgeLength = v1.distance(v2);
                    temp += edgeLength;
                    if (temp == startSliceLength) {
                        result.add(v2);
                    } else if (temp > startSliceLength) {
                        result.add(v1.subtract(v2).normalizeLocal().multLocal(temp - startSliceLength).addLocal(v2));
                    }
                }
            }

            if (endSlice < 1) {
                if (index == vertices.length) {
                    Vector3f v1 = vertices[vertices.length - 2];
                    Vector3f v2 = vertices[vertices.length - 1];
                    result.add(v1.subtract(v2).normalizeLocal().multLocal(length - endSliceLength).addLocal(v2));
                } else {
                    for (int i = index; i < vertices.length && temp < endSliceLength; ++i) {
                        Vector3f v1 = vertices[index - 1];
                        Vector3f v2 = vertices[index++];
                        temp += v1.distance(v2);
                        if (temp == endSliceLength) {
                            result.add(v2);
                        } else if (temp > endSliceLength) {
                            result.add(v1.subtract(v2).normalizeLocal().multLocal(temp - startSliceLength).addLocal(v2));
                        }
                    }
                }
            } else {
                result.addAll(Arrays.asList(Arrays.copyOfRange(vertices, index, vertices.length)));
            }

            return result.toArray(new Vector3f[result.size()]);
        }

        /**
         * The method computes the value of a point at the certain relational distance from its beggining.
         * @param alongRatio
         *            the relative distance along the curve; should be a value between 0 and 1 inclusive;
         *            if the value exceeds the boundaries it is truncated to them
         * @return computed value along the curve
         */
        public Vector3f getValueAlongCurve(float alongRatio) {
            alongRatio = FastMath.clamp(alongRatio, 0, 1);
            Vector3f result = new Vector3f();
            float probeLength = this.getLength() * alongRatio;
            float length = 0;
            for (int i = 1; i < vertices.length; ++i) {
                float edgeLength = vertices[i].distance(vertices[i - 1]);
                if (length + edgeLength > probeLength) {
                    float ratioAlongEdge = (probeLength - length) / edgeLength;
                    return FastMath.interpolateLinear(ratioAlongEdge, vertices[i - 1], vertices[i]);
                } else if (length + edgeLength == probeLength) {
                    return vertices[i];
                }
                length += edgeLength;
            }

            return result;
        }

        /**
         * @return the material number of this bezier line
         */
        public int getMaterialNumber() {
            return materialNumber;
        }

        /**
         * @return indicates if the line is smooth of flat
         */
        public boolean isSmooth() {
            return smooth;
        }

        /**
         * @return the length of this bezier line
         */
        public float getLength() {
            return length;
        }

        /**
         * @return indicates if the current line is cyclic or not
         */
        public boolean isCyclic() {
            return cyclic;
        }
    }
}

package com.jme3.scene.plugins.blender.curves;

import com.jme3.math.Vector3f;
import com.jme3.scene.plugins.blender.file.DynamicArray;
import com.jme3.scene.plugins.blender.file.Structure;
import java.util.ArrayList;
import java.util.List;

/**
 * A class that helps to calculate the bezier curves calues. It uses doubles for performing calculations to minimize
 * floating point operations errors.
 * @author Marcin Roguski
 */
public class BezierCurve {

    public static final int X_VALUE = 0;
    public static final int Y_VALUE = 1;
    public static final int Z_VALUE = 2;
    /** 
     * The type of the curve. Describes the data it modifies. 
     * Used in ipos calculations.
     */
    private int type;
    /** The dimension of the curve. */
    private int dimension;
    /** A table of the bezier points. */
    private float[][][] bezierPoints;

    @SuppressWarnings("unchecked")
    public BezierCurve(final int type, final List<Structure> bezTriples, final int dimension) {
        if (dimension != 2 && dimension != 3) {
            throw new IllegalArgumentException("The dimension of the curve should be 2 or 3!");
        }
        this.type = type;
        this.dimension = dimension;
        //first index of the bezierPoints table has the length of triples amount
        //the second index points to a table od three points of a bezier triple (handle, point, handle)
        //the third index specifies the coordinates of the specific point in a bezier triple
        bezierPoints = new float[bezTriples.size()][3][dimension];
        int i = 0, j, k;
        for (Structure bezTriple : bezTriples) {
            DynamicArray<Number> vec = (DynamicArray<Number>) bezTriple.getFieldValue("vec");
            for (j = 0; j < 3; ++j) {
                for (k = 0; k < dimension; ++k) {
                    bezierPoints[i][j][k] = vec.get(j, k).floatValue();
                }
            }
            ++i;
        }
    }

    /**
     * This method evaluates the data for the specified frame. The Y value is returned.
     * @param frame
     *        the frame for which the value is being calculated
     * @param valuePart
     *        this param specifies wheather we should return the X, Y or Z part of the result value; it should have
     *        one of the following values: X_VALUE - the X factor of the result Y_VALUE - the Y factor of the result
     *        Z_VALUE - the Z factor of the result
     * @return the value of the curve
     */
    public float evaluate(int frame, int valuePart) {
        for (int i = 0; i < bezierPoints.length - 1; ++i) {
            if (frame >= bezierPoints[i][1][0] && frame <= bezierPoints[i + 1][1][0]) {
                float t = (frame - bezierPoints[i][1][0]) / (bezierPoints[i + 1][1][0] - bezierPoints[i][1][0]);
                float oneMinusT = 1.0f - t;
                float oneMinusT2 = oneMinusT * oneMinusT;
                float t2 = t * t;
                return bezierPoints[i][1][valuePart] * oneMinusT2 * oneMinusT + 3.0f * bezierPoints[i][2][valuePart] * t * oneMinusT2 + 3.0f * bezierPoints[i + 1][0][valuePart] * t2 * oneMinusT + bezierPoints[i + 1][1][valuePart] * t2 * t;
            }
        }
        if (frame < bezierPoints[0][1][0]) {
            return bezierPoints[0][1][1];
        } else { //frame>bezierPoints[bezierPoints.length-1][1][0]
            return bezierPoints[bezierPoints.length - 1][1][1];
        }
    }

    /**
     * This method returns the frame where last bezier triple center point of the bezier curve is located.
     * @return the frame number of the last defined bezier triple point for the curve
     */
    public int getLastFrame() {
        return (int) bezierPoints[bezierPoints.length - 1][1][0];
    }

    /**
     * This method returns the type of the bezier curve. The type describes the parameter that this curve modifies
     * (ie. LocationX or rotationW of the feature).
     * @return the type of the bezier curve
     */
    public int getType() {
        return type;
    }

    /**
     * This method returns a list of control points for this curve.
     * @return a list of control points for this curve.
     */
    public List<Vector3f> getControlPoints() {
        List<Vector3f> controlPoints = new ArrayList<Vector3f>(bezierPoints.length * 3);
        for (int i = 0; i < bezierPoints.length; ++i) {
            controlPoints.add(new Vector3f(bezierPoints[i][0][0], bezierPoints[i][0][1], bezierPoints[i][0][2]));
            controlPoints.add(new Vector3f(bezierPoints[i][1][0], bezierPoints[i][1][1], bezierPoints[i][1][2]));
            controlPoints.add(new Vector3f(bezierPoints[i][2][0], bezierPoints[i][2][1], bezierPoints[i][2][2]));
        }
        return controlPoints;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Bezier curve: ").append(type).append('\n');
        for (int i = 0; i < bezierPoints.length; ++i) {
            sb.append(this.toStringBezTriple(i)).append('\n');
        }
        return sb.toString();
    }

    /**
     * This method converts the bezier triple of a specified index into text.
     * @param tripleIndex
     *        index of the triple
     * @return text representation of the triple
     */
    private String toStringBezTriple(int tripleIndex) {
        if (this.dimension == 2) {
            return "[(" + bezierPoints[tripleIndex][0][0] + ", " + bezierPoints[tripleIndex][0][1] + ") ("
                    + bezierPoints[tripleIndex][1][0] + ", " + bezierPoints[tripleIndex][1][1] + ") ("
                    + bezierPoints[tripleIndex][2][0] + ", " + bezierPoints[tripleIndex][2][1] + ")]";
        } else {
            return "[(" + bezierPoints[tripleIndex][0][0] + ", " + bezierPoints[tripleIndex][0][1] + ", " + bezierPoints[tripleIndex][0][2] + ") ("
                    + bezierPoints[tripleIndex][1][0] + ", " + bezierPoints[tripleIndex][1][1] + ", " + bezierPoints[tripleIndex][1][2] + ") ("
                    + bezierPoints[tripleIndex][2][0] + ", " + bezierPoints[tripleIndex][2][1] + ", " + bezierPoints[tripleIndex][2][2] + ")]";
        }
    }
}
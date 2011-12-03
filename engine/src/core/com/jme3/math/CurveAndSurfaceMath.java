package com.jme3.math;

import com.jme3.math.Spline.SplineType;
import java.util.List;

/**
 * This class offers methods to help with curves and surfaces calculations.
 * @author Marcin Roguski (Kealthas)
 */
public class CurveAndSurfaceMath {
	private static final float KNOTS_MINIMUM_DELTA = 0.0001f;

	/**
	 * A private constructor is defined to avoid instatiation of this class.
	 */
	private CurveAndSurfaceMath() {}
	
	/**
	 * This method interpolates tha data for the nurbs curve.
	 * @param u
	 *            the u value
	 * @param nurbSpline
	 *            the nurbs spline definition
	 * @param store
	 *            the resulting point in 3D space
	 */
	public static void interpolateNurbs(float u, Spline nurbSpline, Vector3f store) {
		if (nurbSpline.getType() != SplineType.Nurb) {
			throw new IllegalArgumentException("Given spline is not of a NURB type!");
		}
		List<Vector3f> controlPoints = nurbSpline.getControlPoints();
		float[] weights = nurbSpline.getWeights();
		List<Float> knots = nurbSpline.getKnots();
		int controlPointAmount = controlPoints.size();

		store.set(Vector3f.ZERO);
		float delimeter = 0;
		for (int i = 0; i < controlPointAmount; ++i) {
			float val = weights[i] * CurveAndSurfaceMath.computeBaseFunctionValue(i, nurbSpline.getBasisFunctionDegree(), u, knots);
			store.addLocal(nurbSpline.getControlPoints().get(i)
					.mult(val));
			delimeter += val;
		}
		store.divideLocal(delimeter);
	}

	/**
	 * This method interpolates tha data for the nurbs surface.
	 * 
	 * @param u
	 *            the u value
	 * @param v
	 *            the v value
	 * @param controlPoints
	 *            the nurbs' control points
	 * @param knots
	 *            the nurbs' knots
	 * @param basisUFunctionDegree
	 *            the degree of basis U function
	 * @param basisVFunctionDegree
	 *            the degree of basis V function
	 * @param store
	 *            the resulting point in 3D space
	 */
	public static void interpolate(float u, float v, List<List<Vector4f>> controlPoints, List<Float>[] knots, 
			int basisUFunctionDegree, int basisVFunctionDegree, Vector3f store) {
		store.set(Vector3f.ZERO);
		float delimeter = 0;
		int vControlPointsAmount = controlPoints.size();
		int uControlPointsAmount = controlPoints.get(0).size();
		for (int i = 0; i < vControlPointsAmount; ++i) {
			for (int j = 0; j < uControlPointsAmount; ++j) {
				Vector4f controlPoint = controlPoints.get(i).get(j);
				float val = controlPoint.w
								* CurveAndSurfaceMath.computeBaseFunctionValue(i, basisVFunctionDegree, v, knots[1])
								* CurveAndSurfaceMath.computeBaseFunctionValue(j, basisUFunctionDegree, u, knots[0]);
				store.addLocal(controlPoint.x * val, controlPoint.y * val, controlPoint.z * val);
				delimeter += val;
			}
		}
		store.divideLocal(delimeter);
	}

	/**
	 * This method prepares the knots to be used. If the knots represent non-uniform B-splines (first and last knot values are being
	 * repeated) it leads to NaN results during calculations. This method adds a small number to each of such knots to avoid NaN's.
	 * @param knots
	 *            the knots to be prepared to use
	 * @param basisFunctionDegree
	 *            the degree of basis function
	 */
	// TODO: improve this; constant delta may lead to errors if the difference between tha last repeated
	// point and the following one is lower than it
	public static void prepareNurbsKnots(List<Float> knots, int basisFunctionDegree) {
		float delta = KNOTS_MINIMUM_DELTA;
		float prevValue = knots.get(0).floatValue();
		for(int i=1;i<knots.size();++i) {
			float value = knots.get(i).floatValue();
			if(value<=prevValue) {
				value += delta;
				knots.set(i, Float.valueOf(value));
				delta += KNOTS_MINIMUM_DELTA;
			} else {
				delta = KNOTS_MINIMUM_DELTA;//reset the delta's value
			}
			
			prevValue = value;
		}
	}

	/**
	 * This method computes the base function value for the NURB curve.
	 * @param i
	 *            the knot index
	 * @param k
	 *            the base function degree
	 * @param t
	 *            the knot value
	 * @param knots
	 *            the knots' values
	 * @return the base function value
	 */
	private static float computeBaseFunctionValue(int i, int k, float t, List<Float> knots) {
		if (k == 1) {
			return knots.get(i) <= t && t < knots.get(i + 1) ? 1.0f : 0.0f;
		} else {
			return (t - knots.get(i)) / (knots.get(i + k - 1) - knots.get(i)) * 
					CurveAndSurfaceMath.computeBaseFunctionValue(i, k - 1, t, knots)
					+ (knots.get(i + k) - t) / (knots.get(i + k) - knots.get(i + 1)) * 
					CurveAndSurfaceMath.computeBaseFunctionValue(i + 1, k - 1, t, knots);
		}
	}
}

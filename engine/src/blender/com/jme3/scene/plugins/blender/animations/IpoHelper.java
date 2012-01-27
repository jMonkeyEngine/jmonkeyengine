package com.jme3.scene.plugins.blender.animations;

import java.util.List;

import com.jme3.animation.BoneTrack;
import com.jme3.scene.plugins.blender.AbstractBlenderHelper;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.curves.BezierCurve;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.BlenderInputStream;
import com.jme3.scene.plugins.blender.file.FileBlockHeader;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;

/**
 * This class helps to compute values from interpolation curves for features
 * like animation or constraint influence. The curves are 3rd degree bezier
 * curves.
 * 
 * @author Marcin Roguski
 */
public class IpoHelper extends AbstractBlenderHelper {

	/**
	 * This constructor parses the given blender version and stores the result.
	 * Some functionalities may differ in different blender versions.
	 * 
	 * @param blenderVersion
	 *            the version read from the blend file
	 * @param fixUpAxis
	 *            a variable that indicates if the Y asxis is the UP axis or not
	 */
	public IpoHelper(String blenderVersion, boolean fixUpAxis) {
		super(blenderVersion, fixUpAxis);
	}

	/**
	 * This method creates an ipo object used for interpolation calculations.
	 * 
	 * @param ipoStructure
	 *            the structure with ipo definition
	 * @param blenderContext
	 *            the blender context
	 * @return the ipo object
	 * @throws BlenderFileException
	 *             this exception is thrown when the blender file is somehow
	 *             corrupted
	 */
	public Ipo fromIpoStructure(Structure ipoStructure, BlenderContext blenderContext) throws BlenderFileException {
		Structure curvebase = (Structure) ipoStructure.getFieldValue("curve");

		// preparing bezier curves
		Ipo result = null;
		List<Structure> curves = curvebase.evaluateListBase(blenderContext);// IpoCurve
		if (curves.size() > 0) {
			BezierCurve[] bezierCurves = new BezierCurve[curves.size()];
			int frame = 0;
			for (Structure curve : curves) {
				Pointer pBezTriple = (Pointer) curve.getFieldValue("bezt");
				List<Structure> bezTriples = pBezTriple.fetchData(blenderContext.getInputStream());
				int type = ((Number) curve.getFieldValue("adrcode")).intValue();
				bezierCurves[frame++] = new BezierCurve(type, bezTriples, 2);
			}
			curves.clear();
			result = new Ipo(bezierCurves, fixUpAxis);
			blenderContext.addLoadedFeatures(ipoStructure.getOldMemoryAddress(), ipoStructure.getName(), ipoStructure, result);
		}
		return result;
	}

	/**
	 * This method creates an ipo object used for interpolation calculations. It
	 * should be called for blender version 2.50 and higher.
	 * 
	 * @param actionStructure
	 *            the structure with action definition
	 * @param blenderContext
	 *            the blender context
	 * @return the ipo object
	 * @throws BlenderFileException
	 *             this exception is thrown when the blender file is somehow
	 *             corrupted
	 */
	public Ipo fromAction(Structure actionStructure, BlenderContext blenderContext) throws BlenderFileException {
		Ipo result = null;
		List<Structure> curves = ((Structure) actionStructure.getFieldValue("curves")).evaluateListBase(blenderContext);// FCurve
		if (curves.size() > 0) {
			BezierCurve[] bezierCurves = new BezierCurve[curves.size()];
			int frame = 0;
			for (Structure curve : curves) {
				Pointer pBezTriple = (Pointer) curve.getFieldValue("bezt");
				List<Structure> bezTriples = pBezTriple.fetchData(blenderContext.getInputStream());
				int type = this.getCurveType(curve, blenderContext);
				bezierCurves[frame++] = new BezierCurve(type, bezTriples, 2);
			}
			curves.clear();
			result = new Ipo(bezierCurves, fixUpAxis);
		}
		return result;
	}

	/**
	 * This method returns the type of the ipo curve.
	 * 
	 * @param structure
	 *            the structure must contain the 'rna_path' field and
	 *            'array_index' field (the type is not important here)
	 * @param blenderContext
	 *            the blender context
	 * @return the type of the curve
	 */
	public int getCurveType(Structure structure, BlenderContext blenderContext) {
		// reading rna path first
		BlenderInputStream bis = blenderContext.getInputStream();
		int currentPosition = bis.getPosition();
		Pointer pRnaPath = (Pointer) structure.getFieldValue("rna_path");
		FileBlockHeader dataFileBlock = blenderContext.getFileBlock(pRnaPath.getOldMemoryAddress());
		bis.setPosition(dataFileBlock.getBlockPosition());
		String rnaPath = bis.readString();
		bis.setPosition(currentPosition);
		int arrayIndex = ((Number) structure.getFieldValue("array_index")).intValue();

		// determining the curve type
		if (rnaPath.endsWith("location")) {
			return Ipo.AC_LOC_X + arrayIndex;
		}
		if (rnaPath.endsWith("rotation_quaternion")) {
			return Ipo.AC_QUAT_W + arrayIndex;
		}
		if (rnaPath.endsWith("scale")) {
			return Ipo.AC_SIZE_X + arrayIndex;
		}
		if (rnaPath.endsWith("rotation")) {
			return Ipo.OB_ROT_X + arrayIndex;
		}
		throw new IllegalStateException("Unknown curve rna path: " + rnaPath);
	}

	/**
	 * This method creates an ipo with only a single value. No track type is
	 * specified so do not use it for calculating tracks.
	 * 
	 * @param constValue
	 *            the value of this ipo
	 * @return constant ipo
	 */
	public Ipo fromValue(float constValue) {
		return new ConstIpo(constValue);
	}

	@Override
	public boolean shouldBeLoaded(Structure structure, BlenderContext blenderContext) {
		return true;
	}

	/**
	 * Ipo constant curve. This is a curve with only one value and no specified
	 * type. This type of ipo cannot be used to calculate tracks. It should only
	 * be used to calculate single value for a given frame.
	 * 
	 * @author Marcin Roguski
	 */
	private class ConstIpo extends Ipo {

		/** The constant value of this ipo. */
		private float	constValue;

		/**
		 * Constructor. Stores the constant value of this ipo.
		 * 
		 * @param constValue
		 *            the constant value of this ipo
		 */
		public ConstIpo(float constValue) {
			super(null, false);
			this.constValue = constValue;
		}

		@Override
		public float calculateValue(int frame) {
			return constValue;
		}

		@Override
		public float calculateValue(int frame, int curveIndex) {
			return constValue;
		}

		@Override
		public int getCurvesAmount() {
			return 0;
		}

		@Override
		public BoneTrack calculateTrack(int boneIndex, int startFrame, int stopFrame, int fps, boolean boneTrack) {
			throw new IllegalStateException("Constatnt ipo object cannot be used for calculating bone tracks!");
		}
	}
}

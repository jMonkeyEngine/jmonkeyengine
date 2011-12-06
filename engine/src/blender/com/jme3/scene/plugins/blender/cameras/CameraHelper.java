package com.jme3.scene.plugins.blender.cameras;

import com.jme3.asset.BlenderKey.FeaturesToLoad;
import com.jme3.renderer.Camera;
import com.jme3.scene.plugins.blender.AbstractBlenderHelper;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Structure;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class that is used to load cameras into the scene.
 * @author Marcin Roguski
 */
public class CameraHelper extends AbstractBlenderHelper {

    private static final Logger LOGGER = Logger.getLogger(CameraHelper.class.getName());
    protected static final int DEFAULT_CAM_WIDTH = 640;
    protected static final int DEFAULT_CAM_HEIGHT = 480;
    
    /**
     * This constructor parses the given blender version and stores the result. Some functionalities may differ in
     * different blender versions.
     * @param blenderVersion
     *        the version read from the blend file
     * @param fixUpAxis
     *        a variable that indicates if the Y asxis is the UP axis or not
     */
    public CameraHelper(String blenderVersion, boolean fixUpAxis) {
        super(blenderVersion, fixUpAxis);
    }
    
	/**
	 * This method converts the given structure to jme camera.
	 * 
	 * @param structure
	 *            camera structure
	 * @return jme camera object
	 * @throws BlenderFileException
	 *             an exception is thrown when there are problems with the
	 *             blender file
	 */
    public Camera toCamera(Structure structure) throws BlenderFileException {
    	if (blenderVersion >= 250) {
            return this.toCamera250(structure);
        } else {
        	return this.toCamera249(structure);
        }
    }

	/**
	 * This method converts the given structure to jme camera. Should be used form blender 2.5+.
	 * 
	 * @param structure
	 *            camera structure
	 * @return jme camera object
	 * @throws BlenderFileException
	 *             an exception is thrown when there are problems with the
	 *             blender file
	 */
    public Camera toCamera250(Structure structure) throws BlenderFileException {
        Camera result = new Camera(DEFAULT_CAM_WIDTH, DEFAULT_CAM_HEIGHT);
        int type = ((Number) structure.getFieldValue("type")).intValue();
        if (type != 0 && type != 1) {
            LOGGER.log(Level.WARNING, "Unknown camera type: {0}. Perspective camera is being used!", type);
            type = 0;
        }
        //type==0 - perspective; type==1 - orthographic; perspective is used as default
        result.setParallelProjection(type == 1);
        float aspect = 0;
        float clipsta = ((Number) structure.getFieldValue("clipsta")).floatValue();
        float clipend = ((Number) structure.getFieldValue("clipend")).floatValue();
        if (type == 0) {
            aspect = ((Number) structure.getFieldValue("lens")).floatValue();
        } else {
            aspect = ((Number) structure.getFieldValue("ortho_scale")).floatValue();
        }
        result.setFrustumPerspective(45, aspect, clipsta, clipend);
        return result;
    }
    
    /**
	 * This method converts the given structure to jme camera. Should be used form blender 2.49.
	 * 
	 * @param structure
	 *            camera structure
	 * @return jme camera object
	 * @throws BlenderFileException
	 *             an exception is thrown when there are problems with the
	 *             blender file
	 */
    public Camera toCamera249(Structure structure) throws BlenderFileException {
        Camera result = new Camera(DEFAULT_CAM_WIDTH, DEFAULT_CAM_HEIGHT);
        int type = ((Number) structure.getFieldValue("type")).intValue();
        if (type != 0 && type != 1) {
            LOGGER.log(Level.WARNING, "Unknown camera type: {0}. Perspective camera is being used!", type);
            type = 0;
        }
        //type==0 - perspective; type==1 - orthographic; perspective is used as default
        result.setParallelProjection(type == 1);
        float aspect = 0;
        float clipsta = ((Number) structure.getFieldValue("clipsta")).floatValue();
        float clipend = ((Number) structure.getFieldValue("clipend")).floatValue();
        if (type == 0) {
            aspect = ((Number) structure.getFieldValue("lens")).floatValue();
        } else {
            aspect = ((Number) structure.getFieldValue("ortho_scale")).floatValue();
        }
        result.setFrustumPerspective(aspect, result.getWidth() / result.getHeight(), clipsta, clipend);
        return result;
    }

	@Override
	public boolean shouldBeLoaded(Structure structure, BlenderContext blenderContext) {
		return (blenderContext.getBlenderKey().getFeaturesToLoad() & FeaturesToLoad.CAMERAS) != 0;
	}
}

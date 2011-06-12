package com.jme3.scene.plugins.blender.helpers.v249;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.renderer.Camera;
import com.jme3.scene.plugins.blender.data.Structure;
import com.jme3.scene.plugins.blender.exception.BlenderFileException;
import com.jme3.scene.plugins.blender.utils.AbstractBlenderHelper;

/**
 * A class that is used in light calculations.
 * @author Marcin Roguski
 */
public class CameraHelper extends AbstractBlenderHelper {

    private static final Logger LOGGER = Logger.getLogger(CameraHelper.class.getName());
    protected static final int DEFAULT_CAM_WIDTH = 100;
    protected static final int DEFAULT_CAM_HEIGHT = 100;

    /**
     * This constructor parses the given blender version and stores the result. Some functionalities may differ in
     * different blender versions.
     * @param blenderVersion
     *        the version read from the blend file
     */
    public CameraHelper(String blenderVersion) {
        super(blenderVersion);
    }

    /**
     * This method reads the camera object.
     * @param structure the structure containing the camera data
     * @return the camera object
     * @throws BlenderFileException
     */
    public Camera toCamera(Structure structure) throws BlenderFileException {
        Camera result = new Camera(DEFAULT_CAM_WIDTH, DEFAULT_CAM_HEIGHT);
        int type = ((Number) structure.getFieldValue("type")).intValue();
        if (type != 0 && type != 1) {
            LOGGER.log(Level.WARNING, "Unknown camera type: {0}. Perspective camera is being used!", type);
            type = 0;
        }
        //type==0 - perspective; type==1 - orthographic; perspective is used as default
        result.setParallelProjection(type == 1);
        float angle = ((Number) structure.getFieldValue("angle")).floatValue();
        float aspect = 0;
        float clipsta = ((Number) structure.getFieldValue("clipsta")).floatValue();
        float clipend = ((Number) structure.getFieldValue("clipend")).floatValue();
        if (type == 0) {
            aspect = ((Number) structure.getFieldValue("lens")).floatValue();
        } else {
            aspect = ((Number) structure.getFieldValue("ortho_scale")).floatValue();
        }
        result.setFrustumPerspective(angle, aspect, clipsta, clipend);
        return result;
    }
}

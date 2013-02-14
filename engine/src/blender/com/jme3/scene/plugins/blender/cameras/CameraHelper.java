package com.jme3.scene.plugins.blender.cameras;

import com.jme3.asset.BlenderKey.FeaturesToLoad;
import com.jme3.math.FastMath;
import com.jme3.renderer.Camera;
import com.jme3.scene.CameraNode;
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

    private static final Logger LOGGER             = Logger.getLogger(CameraHelper.class.getName());
    protected static final int  DEFAULT_CAM_WIDTH  = 640;
    protected static final int  DEFAULT_CAM_HEIGHT = 480;

    /**
     * This constructor parses the given blender version and stores the result. Some functionalities may differ in
     * different blender versions.
     * @param blenderVersion
     *            the version read from the blend file
     * @param fixUpAxis
     *            a variable that indicates if the Y asxis is the UP axis or not
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
    public CameraNode toCamera(Structure structure, BlenderContext blenderContext) throws BlenderFileException {
        if (blenderVersion >= 250) {
            return this.toCamera250(structure, blenderContext.getSceneStructure());
        } else {
            return this.toCamera249(structure);
        }
    }

    /**
     * This method converts the given structure to jme camera. Should be used form blender 2.5+.
     * 
     * @param structure
     *            camera structure
     * @param sceneStructure
     *            scene structure
     * @return jme camera object
     * @throws BlenderFileException
     *             an exception is thrown when there are problems with the
     *             blender file
     */
    private CameraNode toCamera250(Structure structure, Structure sceneStructure) throws BlenderFileException {
        int width = DEFAULT_CAM_WIDTH;
        int height = DEFAULT_CAM_HEIGHT;
        if (sceneStructure != null) {
            Structure renderData = (Structure) sceneStructure.getFieldValue("r");
            width = ((Number) renderData.getFieldValue("xsch")).shortValue();
            height = ((Number) renderData.getFieldValue("ysch")).shortValue();
        }
        Camera camera = new Camera(width, height);
        int type = ((Number) structure.getFieldValue("type")).intValue();
        if (type != 0 && type != 1) {
            LOGGER.log(Level.WARNING, "Unknown camera type: {0}. Perspective camera is being used!", type);
            type = 0;
        }
        // type==0 - perspective; type==1 - orthographic; perspective is used as default
        camera.setParallelProjection(type == 1);
        float aspect = width / (float) height;
        float fovY; // Vertical field of view in degrees
        float clipsta = ((Number) structure.getFieldValue("clipsta")).floatValue();
        float clipend = ((Number) structure.getFieldValue("clipend")).floatValue();
        if (type == 0) {
            // Convert lens MM to vertical degrees in fovY, see Blender rna_Camera_angle_get()
            // Default sensor size prior to 2.60 was 32.
            float sensor = 32.0f;
            boolean sensorVertical = false;
            Number sensorFit = (Number) structure.getFieldValue("sensor_fit");
            if (sensorFit != null) {
                // If sensor_fit is vert (2), then sensor_y is used
                sensorVertical = sensorFit.byteValue() == 2;
                String sensorName = "sensor_x";
                if (sensorVertical) {
                    sensorName = "sensor_y";
                }
                sensor = ((Number) structure.getFieldValue(sensorName)).floatValue();
            }
            float focalLength = ((Number) structure.getFieldValue("lens")).floatValue();
            float fov = 2.0f * FastMath.atan((sensor / 2.0f) / focalLength);
            if (sensorVertical) {
                fovY = fov * FastMath.RAD_TO_DEG;
            } else {
                // Convert fov from horizontal to vertical
                fovY = 2.0f * FastMath.atan(FastMath.tan(fov / 2.0f) / aspect) * FastMath.RAD_TO_DEG;
            }
        } else {
            // This probably is not correct.
            fovY = ((Number) structure.getFieldValue("ortho_scale")).floatValue();
        }
        camera.setFrustumPerspective(fovY, aspect, clipsta, clipend);
        return new CameraNode(null, camera);
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
    private CameraNode toCamera249(Structure structure) throws BlenderFileException {
        Camera camera = new Camera(DEFAULT_CAM_WIDTH, DEFAULT_CAM_HEIGHT);
        int type = ((Number) structure.getFieldValue("type")).intValue();
        if (type != 0 && type != 1) {
            LOGGER.log(Level.WARNING, "Unknown camera type: {0}. Perspective camera is being used!", type);
            type = 0;
        }
        // type==0 - perspective; type==1 - orthographic; perspective is used as default
        camera.setParallelProjection(type == 1);
        float aspect = 0;
        float clipsta = ((Number) structure.getFieldValue("clipsta")).floatValue();
        float clipend = ((Number) structure.getFieldValue("clipend")).floatValue();
        if (type == 0) {
            aspect = ((Number) structure.getFieldValue("lens")).floatValue();
        } else {
            aspect = ((Number) structure.getFieldValue("ortho_scale")).floatValue();
        }
        camera.setFrustumPerspective(aspect, camera.getWidth() / camera.getHeight(), clipsta, clipend);
        return new CameraNode(null, camera);
    }

    @Override
    public boolean shouldBeLoaded(Structure structure, BlenderContext blenderContext) {
        return (blenderContext.getBlenderKey().getFeaturesToLoad() & FeaturesToLoad.CAMERAS) != 0;
    }
}

package com.jme3.input.vr.lwjgl_openvr;

import com.jme3.app.VREnvironment;
import com.jme3.input.vr.VRInputAPI;
import com.jme3.input.vr.VRTrackedController;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

/**
 * A controller that is tracked within the VR environment. Such a controller can provide its position within the VR space.
 * @author Julien Seinturier - COMEX SA - <a href="http://www.seinturier.fr">http://www.seinturier.fr</a>
 * @author Rickard Edén
 */
public class LWJGLOpenVRTrackedController implements VRTrackedController{
    /**
     * The index of the controller within the underlying VR API.
     */
    private int controllerIndex = -1;

    /**
     * The underlying VRAPI.
     */
    private VRInputAPI hardware     = null;

    /**
     * The name of the controller.
     */
    private String name;

    private VREnvironment environment;

    /**
     * Wrap a new VR tracked controller on an OpenVR system.
     * @param controllerIndex the index of the controller within the underlying VR system.
     * @param hardware the underlying VR system.
     * @param name the name of the controller.
     * @param manufacturer the manufacturer of the controller.
     * @param environment the VR environment.
     */
    public LWJGLOpenVRTrackedController(int controllerIndex, VRInputAPI hardware, String name, String manufacturer, VREnvironment environment){
        this.controllerIndex = controllerIndex;
        this.hardware        = hardware;

        this.name            = name;
        this.manufacturer    = manufacturer;

        this.environment     = environment;
    }

    /**
     * The manufacturer of the controller.
     */
    private String manufacturer;

    @Override
    public Vector3f getPosition() {
        if (hardware != null){
            return hardware.getPosition(controllerIndex);
        } else {
            throw new IllegalStateException("No underlying VR API.");
        }
    }

    @Override
    public Quaternion getOrientation() {
        if (hardware != null){
            return hardware.getOrientation(controllerIndex);
        } else {
            throw new IllegalStateException("No underlying VR API.");
        }
    }

    @Override
    public Matrix4f getPose(){
        if (environment != null){
             if (hardware != null){
                    return ((LWJGLOpenVR)environment.getVRHardware()).getPoseMatrices()[controllerIndex];
                } else {
                    throw new IllegalStateException("No underlying VR API.");
                }
        } else {
            throw new IllegalStateException("VR tracked device is not attached to any environment.");
        }
    }

    @Override
    public String getControllerName() {
        return name;
    }

    @Override
    public String getControllerManufacturer() {
        return manufacturer;
    }
}
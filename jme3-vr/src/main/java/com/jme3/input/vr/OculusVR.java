/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.input.vr;

import com.jme3.app.VREnvironment;
import com.jme3.math.*;
import com.jme3.renderer.Camera;
import java.util.logging.Logger;

import static org.lwjgl.ovr.OVRUtil.ovr_Detect;

import org.lwjgl.ovr.*;

/**
 * Oculus VR (LibOVR 1.3.0) Native support.
 *
 * @author Campbell Suter <znix@znix.xyz>
 */
public class OculusVR implements VRAPI {

    private static final Logger LOGGER = Logger.getLogger(OculusVR.class.getName());

    private final VREnvironment environment;
    private boolean initialized;

    public OculusVR(VREnvironment environment) {
        this.environment = environment;
    }

    @Override
    public OpenVRInput getVRinput() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName() {
        return "OVR";
    }

    @Override
    public int getDisplayFrequency() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean initialize() {
        OVRDetectResult detect = OVRDetectResult.calloc();
        ovr_Detect(0, detect);
        System.out.println("OVRDetectResult.IsOculusHMDConnected = " + detect.IsOculusHMDConnected());
        System.out.println("OVRDetectResult.IsOculusServiceRunning = " + detect.IsOculusServiceRunning());
        detect.free();
        if (detect.IsOculusHMDConnected() == false) {
            return false;
        }

        initialized = true;

        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void destroy() {
    }

    @Override
    public void reset() {
    }

    @Override
    public void getRenderSize(Vector2f store) {
    }

    @Override
    public float getInterpupillaryDistance() {
        return 0.065f; // TODO
    }

    @Override
    public Quaternion getOrientation() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Vector3f getPosition() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void getPositionAndOrientation(Vector3f storePos, Quaternion storeRot) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updatePose() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Matrix4f getHMDMatrixProjectionLeftEye(Camera cam) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Matrix4f getHMDMatrixProjectionRightEye(Camera cam) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Vector3f getHMDVectorPoseLeftEye() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Vector3f getHMDVectorPoseRightEye() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Vector3f getSeatedToAbsolutePosition() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Matrix4f getHMDMatrixPoseLeftEye() {
        throw new UnsupportedOperationException();
    }

    @Override
    public HmdType getType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Matrix4f getHMDMatrixPoseRightEye() {
        throw new UnsupportedOperationException();
    }

    public void printLatencyInfoToConsole(boolean set) {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    public void setFlipEyes(boolean set) {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    public Void getCompositor() {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    public Void getVRSystem() {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    public boolean initVRCompositor(boolean set) {
        throw new UnsupportedOperationException("Not yet implemented!");
    }
}

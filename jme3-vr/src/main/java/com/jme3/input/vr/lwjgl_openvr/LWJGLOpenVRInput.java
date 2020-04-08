/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.input.vr.lwjgl_openvr;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.jme3.app.VREnvironment;
import com.jme3.input.vr.VRInputAPI;
import com.jme3.input.vr.VRInputType;
import com.jme3.input.vr.VRTrackedController;
import com.jme3.input.vr.VRViewManager;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Spatial;
import com.jme3.util.VRUtil;
import java.nio.IntBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.openvr.HmdVector3;
import org.lwjgl.openvr.VR;
import org.lwjgl.openvr.VRControllerState;
import org.lwjgl.openvr.VRSystem;

/*
make helper functions to pull the following easily from raw data (DONE)
trigger:
Controller#1, Axis#0 X: 0.0, Y: 0.0
Controller#1, Axis#1 X: 1.0, Y: 0.0
Controller#1, Axis#2 X: 0.0, Y: 0.0
Controller#1, Axis#3 X: 0.0, Y: 0.0
Controller#1, Axis#4 X: 0.0, Y: 0.0
Button press: 8589934592 (when full), touch: 8589934592
touchpad (upper left):
Controller#1, Axis#0 X: -0.6059755, Y: 0.2301706
Controller#1, Axis#1 X: 0.0, Y: 0.0
Controller#1, Axis#2 X: 0.0, Y: 0.0
Controller#1, Axis#3 X: 0.0, Y: 0.0
Controller#1, Axis#4 X: 0.0, Y: 0.0
Button press: 4294967296 (when pressed in), touch: 4294967296
grip:
Controller#1, Axis#0 X: 0.0, Y: 0.0
Controller#1, Axis#1 X: 0.0, Y: 0.0
Controller#1, Axis#2 X: 0.0, Y: 0.0
Controller#1, Axis#3 X: 0.0, Y: 0.0
Controller#1, Axis#4 X: 0.0, Y: 0.0
Button press: 4, touch: 4
thumb:
Controller#1, Axis#0 X: 0.0, Y: 0.0
Controller#1, Axis#1 X: 0.0, Y: 0.0
Controller#1, Axis#2 X: 0.0, Y: 0.0
Controller#1, Axis#3 X: 0.0, Y: 0.0
Controller#1, Axis#4 X: 0.0, Y: 0.0
Button press: 2, touch: 2
 */
/**
 * A class that wraps an
 * <a href="https://github.com/ValveSoftware/openvr/wiki/API-Documentation">OpenVR</a>
 * input.<br>
 * <code>null</code> values will be returned if no valid pose exists, or that
 * input device isn't available user code should check for <code>null</code>
 * values.
 *
 * @author reden - phr00t
 * @author Julien Seinturier - COMEX SA - <a href="http://www.seinturier.fr">http://www.seinturier.fr</a>
 * @author Rickard Edén
 */
public class LWJGLOpenVRInput implements VRInputAPI {

    private static final Logger logger = Logger.getLogger(LWJGLOpenVRInput.class.getName());

    private final VRControllerState[] cStates = new VRControllerState[VR.k_unMaxTrackedDeviceCount];

    private final Quaternion[] rotStore = new Quaternion[VR.k_unMaxTrackedDeviceCount];

    private final Vector3f[] posStore = new Vector3f[VR.k_unMaxTrackedDeviceCount];

    private static final int[] controllerIndex = new int[VR.k_unMaxTrackedDeviceCount];

    private int controllerCount = 0;

    private final Vector2f tempAxis = new Vector2f(), temp2Axis = new Vector2f();

    private final Vector2f lastCallAxis[] = new Vector2f[VR.k_unMaxTrackedDeviceCount];

    private final boolean buttonDown[][] = new boolean[VR.k_unMaxTrackedDeviceCount][16];

    private float axisMultiplier = 1f;

    private final Vector3f tempVel = new Vector3f();

    private final Quaternion tempq = new Quaternion();

    private final VREnvironment environment;

    private List<VRTrackedController> trackedControllers = null;

    /**
     * Create a new
     * <a href="https://github.com/ValveSoftware/openvr/wiki/API-Documentation">OpenVR</a>
     * input attached to the given VR environment.
     *
     * @param environment the VR environment to which the input is attached.
     */
    public LWJGLOpenVRInput(VREnvironment environment) {
        this.environment = environment;
    }

    @Override
    public float getAxisMultiplier() {
        return axisMultiplier;
    }

    @Override
    public void setAxisMultiplier(float set) {
        axisMultiplier = set;
    }

    @Override
    public void swapHands() {
        if (controllerCount != 2) {
            return;
        }
        int temp = controllerIndex[0];
        controllerIndex[0] = controllerIndex[1];
        controllerIndex[1] = temp;
    }

    @Override
    public boolean isButtonDown(int controllerIndex, VRInputType checkButton) {
        VRControllerState cs = cStates[LWJGLOpenVRInput.controllerIndex[controllerIndex]];
        switch (checkButton) {
            default:
                return false;
            case ViveGripButton:
                return (cs.ulButtonPressed() & 4) != 0;
            case ViveMenuButton:
                return (cs.ulButtonPressed() & 2) != 0;
            case ViveTrackpadAxis:
                return (cs.ulButtonPressed() & 4294967296l) != 0;
            case ViveTriggerAxis:
                return (cs.ulButtonPressed() & 8589934592l) != 0;
        }
    }

    @Override
    public boolean wasButtonPressedSinceLastCall(int controllerIndex, VRInputType checkButton) {
        boolean buttonDownNow = isButtonDown(controllerIndex, checkButton);
        int checkButtonValue = checkButton.getValue();
        int cIndex = LWJGLOpenVRInput.controllerIndex[controllerIndex];
        boolean retval = buttonDownNow == true && buttonDown[cIndex][checkButtonValue] == false;
        buttonDown[cIndex][checkButtonValue] = buttonDownNow;
        return retval;
    }

    @Override
    public void resetInputSinceLastCall() {
        for (int i = 0; i < lastCallAxis.length; i++) {
            lastCallAxis[i].x = 0f;
            lastCallAxis[i].y = 0f;
        }
        for (int i = 0; i < VR.k_unMaxTrackedDeviceCount; i++) {
            for (int j = 0; j < 16; j++) {
                buttonDown[i][j] = false;
            }
        }
    }

    @Override
    public Vector2f getAxisDeltaSinceLastCall(int controllerIndex, VRInputType forAxis) {
        int axisIndex = forAxis.getValue();
        temp2Axis.set(lastCallAxis[axisIndex]);
        lastCallAxis[axisIndex].set(getAxis(controllerIndex, forAxis));
        if ((temp2Axis.x != 0f || temp2Axis.y != 0f) && (lastCallAxis[axisIndex].x != 0f || lastCallAxis[axisIndex].y != 0f)) {
            temp2Axis.subtractLocal(lastCallAxis[axisIndex]);
        } else {
            // move made from rest, don't count as a delta move
            temp2Axis.x = 0f;
            temp2Axis.y = 0f;
        }
        return temp2Axis;
    }

    @Override
    public Vector3f getVelocity(int controllerIndex) {

        if (environment != null) {

            if (environment.getVRHardware() instanceof LWJGLOpenVR) {
                int index = LWJGLOpenVRInput.controllerIndex[controllerIndex];
//                if( needsNewVelocity[index] ) {
                HmdVector3 tempVec = ((LWJGLOpenVR) environment.getVRHardware()).hmdTrackedDevicePoses[index].vVelocity();
//                    needsNewVelocity[index] = false;
//                }
                tempVel.x = tempVec.v(0);
                tempVel.y = tempVec.v(1);
                tempVel.z = tempVec.v(2);
                return tempVel;
            } else {
                throw new IllegalStateException("VR hardware " + environment.getVRHardware().getClass().getSimpleName() + " is not a subclass of " + LWJGLOpenVR.class.getSimpleName());
            }
        } else {
            throw new IllegalStateException("VR input is not attached to a VR environment.");
        }
    }

    @Override
    public Vector3f getAngularVelocity(int controllerIndex) {

        if (environment != null) {

            if (environment.getVRHardware() instanceof LWJGLOpenVR) {

                int index = LWJGLOpenVRInput.controllerIndex[controllerIndex];
                HmdVector3 tempVec = ((LWJGLOpenVR) environment.getVRHardware()).hmdTrackedDevicePoses[index].vAngularVelocity();
//                    needsNewVelocity[index] = false;
//                }
                tempVel.x = tempVec.v(0);
                tempVel.y = tempVec.v(1);
                tempVel.z = tempVec.v(2);
                return tempVel;
            } else {
                throw new IllegalStateException("VR hardware " + environment.getVRHardware().getClass().getSimpleName() + " is not a subclass of " + LWJGLOpenVR.class.getSimpleName());
            }
        } else {
            throw new IllegalStateException("VR input is not attached to a VR environment.");
        }

    }

    @Override
    public Vector2f getAxisRaw(int controllerIndex, VRInputType forAxis) {
        VRControllerState cs = cStates[LWJGLOpenVRInput.controllerIndex[controllerIndex]];
        switch (forAxis) {
            default:
                return null;
            case ViveTriggerAxis:
                tempAxis.x = cs.rAxis(1).x();
                tempAxis.y = tempAxis.x;
                break;
            case ViveTrackpadAxis:
                tempAxis.x = cs.rAxis(0).x();
                tempAxis.y = cs.rAxis(0).y();
                break;
        }
        return tempAxis;
    }

    @Override
    public Vector2f getAxis(int controllerIndex, VRInputType forAxis) {
        getAxisRaw(controllerIndex, forAxis);
        tempAxis.x *= axisMultiplier;
        tempAxis.y *= axisMultiplier;
        return tempAxis;
    }

    @Override
    public boolean init() {

        logger.config("Initialize OpenVR input.");

        for (int i = 0; i < VR.k_unMaxTrackedDeviceCount; i++) {
            rotStore[i] = new Quaternion();
            posStore[i] = new Vector3f();
            cStates[i] = VRControllerState.create();
            lastCallAxis[i] = new Vector2f();
            logger.config("  Input " + (i + 1) + "/" + VR.k_unMaxTrackedDeviceCount + " binded.");
        }

        return true;
    }

    @Override
    public VRTrackedController getTrackedController(int index) {
        if (trackedControllers != null) {
            if ((trackedControllers.size() > 0) && (index < trackedControllers.size())) {
                return trackedControllers.get(index);
            }
        }

        return null;
    }

    @Override
    public int getTrackedControllerCount() {
        return controllerCount;
    }

    @Override
    public VRControllerState getRawControllerState(int index) {
        if (isInputDeviceTracking(index) == false) {
            return null;
        }
        return cStates[controllerIndex[index]];
    }

    @Override
    public boolean isInputFocused() {
    	if (environment != null){
            // not a 100% match, but the closest i can find in lwjgl. Doc seems to confirm this too.
            return VRSystem.VRSystem_IsInputAvailable();
            //return ((VR_IVRSystem_FnTable)environment.getVRHardware().getVRSystem()).IsInputFocusCapturedByAnotherProcess.apply() == 0;
    	} else {
    		throw new IllegalStateException("VR input is not attached to a VR environment.");
    	}      
    }

    @Override
    public boolean isInputDeviceTracking(int index) {
        if (index < 0 || index >= controllerCount) {
            return false;
        }

        if (environment != null) {

            if (environment.getVRHardware() instanceof LWJGLOpenVR) {
                return ((LWJGLOpenVR) environment.getVRHardware()).hmdTrackedDevicePoses[controllerIndex[index]].bPoseIsValid();
            } else {
                throw new IllegalStateException("VR hardware " + environment.getVRHardware().getClass().getSimpleName() + " is not a subclass of " + LWJGLOpenVR.class.getSimpleName());
            }
        } else {
            throw new IllegalStateException("VR input is not attached to a VR environment.");
        }
    }

    @Override
    public Quaternion getOrientation(int index) {
        if (isInputDeviceTracking(index) == false) {
            return null;
        }

        if (environment != null) {

            if (environment.getVRHardware() instanceof LWJGLOpenVR) {
                index = controllerIndex[index];
                VRUtil.convertMatrix4toQuat(((LWJGLOpenVR) environment.getVRHardware()).poseMatrices[index], rotStore[index]);
                return rotStore[index];
            } else {
                throw new IllegalStateException("VR hardware " + environment.getVRHardware().getClass().getSimpleName() + " is not a subclass of " + LWJGLOpenVR.class.getSimpleName());
            }
        } else {
            throw new IllegalStateException("VR input is not attached to a VR environment.");
        }
    }

    @Override
    public Vector3f getPosition(int index) {
        if (isInputDeviceTracking(index) == false) {
            return null;
        }

        if (environment != null) {

            if (environment.getVRHardware() instanceof LWJGLOpenVR) {
                // the hmdPose comes in rotated funny, fix that here
                index = controllerIndex[index];
                ((LWJGLOpenVR) environment.getVRHardware()).poseMatrices[index].toTranslationVector(posStore[index]);
                posStore[index].x = -posStore[index].x;
                posStore[index].z = -posStore[index].z;
                return posStore[index];
            } else {
                throw new IllegalStateException("VR hardware " + environment.getVRHardware().getClass().getSimpleName() + " is not a subclass of " + LWJGLOpenVR.class.getSimpleName());
            }
        } else {
            throw new IllegalStateException("VR input is not attached to a VR environment.");
        }

    }

    @Override
    public Quaternion getFinalObserverRotation(int index) {

        if (environment != null) {
            VRViewManager vrvm = environment.getVRViewManager();

            if (vrvm != null) {
                if (isInputDeviceTracking(index) == false) {
                    return null;
                }

                Object obs = environment.getObserver();
                if (obs instanceof Camera) {
                    tempq.set(((Camera) obs).getRotation());
                } else {
                    tempq.set(((Spatial) obs).getWorldRotation());
                }

                return tempq.multLocal(getOrientation(index));
            } else {
                throw new IllegalStateException("VR environment has no valid view manager.");
            }

        } else {
            throw new IllegalStateException("VR input is not attached to a VR environment.");
        }
    }

    @Override
    public Vector3f getFinalObserverPosition(int index) {

        if (environment != null) {
            VRViewManager vrvm = environment.getVRViewManager();

            if (vrvm != null) {
                if (isInputDeviceTracking(index) == false) {
                    return null;
                }
                Object obs = environment.getObserver();
                Vector3f pos = getPosition(index);
                if (obs instanceof Camera) {
                    ((Camera) obs).getRotation().mult(pos, pos);
                    return pos.addLocal(((Camera) obs).getLocation());
                } else {
                    ((Spatial) obs).getWorldRotation().mult(pos, pos);
                    return pos.addLocal(((Spatial) obs).getWorldTranslation());
                }
            } else {
                throw new IllegalStateException("VR environment has no valid view manager.");
            }

        } else {
            throw new IllegalStateException("VR input is not attached to a VR environment.");
        }
    }

    @Override
    public void triggerHapticPulse(int controllerIndex, float seconds) {
        if (environment.isInVR() == false || isInputDeviceTracking(controllerIndex) == false) {
            return;
        }

        // apparently only axis ID of 0 works
        VRSystem.VRSystem_TriggerHapticPulse(LWJGLOpenVRInput.controllerIndex[controllerIndex],
                0, (short) Math.round(3f * seconds / 1e-3f));
    }

    @Override
    public void updateConnectedControllers() {
        logger.config("Updating connected controllers.");

        if (environment != null) {
            controllerCount = 0;
            for (int i = 0; i < VR.k_unMaxTrackedDeviceCount; i++) {
                int classCallback = VRSystem.VRSystem_GetTrackedDeviceClass(i);
                if (classCallback == VR.ETrackedDeviceClass_TrackedDeviceClass_Controller || classCallback == VR.ETrackedDeviceClass_TrackedDeviceClass_GenericTracker) {
                    IntBuffer error = BufferUtils.createIntBuffer(1);
                    String controllerName = "Unknown";
                    String manufacturerName = "Unknown";
                    controllerName = VRSystem.VRSystem_GetStringTrackedDeviceProperty(i, VR.ETrackedDeviceProperty_Prop_TrackingSystemName_String, error);
                    manufacturerName = VRSystem.VRSystem_GetStringTrackedDeviceProperty(i, VR.ETrackedDeviceProperty_Prop_ManufacturerName_String, error);

                    if (error.get(0) != 0) {
                        logger.warning("Error getting controller information " + controllerName + " " + manufacturerName + "Code (" + error.get(0) + ")");
                    }
                    controllerIndex[controllerCount] = i;

                    // Adding tracked controller to control.
                    if (trackedControllers == null) {
                        trackedControllers = new ArrayList<VRTrackedController>(VR.k_unMaxTrackedDeviceCount);
                    }
                    trackedControllers.add(new LWJGLOpenVRTrackedController(i, this, controllerName, manufacturerName, environment));

                    // Send a Haptic pulse to the controller
                    triggerHapticPulse(controllerCount, 1.0f);

                    controllerCount++;
                    logger.config("  Tracked controller " + (i + 1) + "/" + VR.k_unMaxTrackedDeviceCount + " " + controllerName + " (" + manufacturerName + ") attached.");
                } else {
                    logger.config("  Controller " + (i + 1) + "/" + VR.k_unMaxTrackedDeviceCount + " ignored.");
                }
            }
        } else {
            throw new IllegalStateException("VR input is not attached to a VR environment.");
        }
    }

    @Override
    public void updateControllerStates() {

        if (environment != null) {
            for (int i = 0; i < controllerCount; i++) {
                int index = controllerIndex[i];
                VRSystem.VRSystem_GetControllerState(index, cStates[index], 64);
                cStates[index].ulButtonPressed();
                cStates[index].rAxis();
            }
        } else {
            throw new IllegalStateException("VR input is not attached to a VR environment.");
        }

    }

}
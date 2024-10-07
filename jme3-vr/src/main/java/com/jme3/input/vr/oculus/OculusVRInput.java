package com.jme3.input.vr.oculus;

import com.jme3.app.VREnvironment;
import com.jme3.input.vr.VRInputAPI;
import com.jme3.input.vr.VRInputType;
import com.jme3.input.vr.VRTrackedController;
import com.jme3.math.*;
import com.jme3.renderer.Camera;
import com.jme3.scene.Spatial;

import org.lwjgl.ovr.*;

import static org.lwjgl.ovr.OVR.*;

public class OculusVRInput implements VRInputAPI {
    // State control
    private final OVRInputState inputState;
    private final OVRSessionStatus sessionStatus;
    private final OVRTrackingState trackingState;
    private final OculusVR hardware;
    private long session;

    // Setup values
    private float axisMultiplier = 1;

    // Cached stuff
    private int buttons, touch;

    // Used to calculate sinceLastCall stuff
    private int lastButtons, lastTouch;
    private final Vector2f[][] lastAxes;

    /**
     * The state data (linear and angular velocity and acceleration) for each hand
     */
    private OVRPoseStatef[] handStates;

    /**
     * The position and orientation of the Touch controllers.
     */
    private OVRPosef[] handPoses;

    /**
     * The object forms of the tracked controllers.
     */
    private final OculusController[] controllers = {
            new OculusController(0),
            new OculusController(1)
    };

    public OculusVRInput(OculusVR hardware, long session,
                         OVRSessionStatus sessionStatus, OVRTrackingState trackingState) {
        this.hardware = hardware;
        this.session = session;
        this.sessionStatus = sessionStatus;
        this.trackingState = trackingState;

        inputState = OVRInputState.calloc();

        handStates = new OVRPoseStatef[ovrHand_Count];
        handPoses = new OVRPosef[handStates.length];
        lastAxes = new Vector2f[handStates.length][3]; // trigger+grab+thumbstick for each hand.
    }

    public void dispose() {
        inputState.free();
        session = 0; // Crashing > undefined behaviour if this object is incorrectly accessed again.
    }

    @Override
    public void updateControllerStates() {
        // Handle buttons, axes
        ovr_GetInputState(session, ovrControllerType_Touch, inputState);
        buttons = inputState.Buttons();
        touch = inputState.Touches();

        // Get the touch controller poses
        // TODO what if no touch controllers are available?
        for (int hand = 0; hand < handPoses.length; hand++) {
            handStates[hand] = trackingState.HandPoses(hand);
            handPoses[hand] = handStates[hand].ThePose();
        }
    }

    private Vector3f cv(OVRVector3f in) {
        // TODO do we want to reuse vectors rather than making new ones?
        // TODO OpenVRInput does this, but it will probably cause some bugs.
        return OculusVR.vecO2J(in, new Vector3f()); // This also fixes the coordinate space transform issues.
    }

    private Vector2f cv(OVRVector2f in) {
        // TODO do we want to reuse vectors rather than making new ones?
        // TODO OpenVRInput does this, but it will probably cause some bugs.
        return new Vector2f(in.x(), in.y());
    }

    private Quaternion cq(OVRQuatf in) {
        // TODO do we want to reuse quaternions rather than making new ones?
        // TODO OpenVRInput does this, but it will probably cause some bugs.
        return OculusVR.quatO2J(in, new Quaternion()); // This also fixes the coordinate space transform issues.
    }

    private Vector2f axis(float input) {
        // See above comments about reusing vectors
        return new Vector2f(input, input);
    }

    // Tracking (position, rotation, velocity, status)

    @Override
    public Vector3f getPosition(int index) {
        return cv(handPoses[index].Position());
    }

    @Override
    public Vector3f getVelocity(int controllerIndex) {
        return cv(handStates[controllerIndex].LinearVelocity());
    }

    @Override
    public Quaternion getOrientation(int index) {
        return cq(handPoses[index].Orientation());
    }

    @Override
    public Vector3f getAngularVelocity(int controllerIndex) {
        return cv(handStates[controllerIndex].AngularVelocity());
    }

    @Override
    public Quaternion getFinalObserverRotation(int index) {
        // Copied from OpenVRInput

        VREnvironment env = hardware.getEnvironment();
        OculusViewManager vrvm = (OculusViewManager) hardware.getEnvironment().getVRViewManager();

        Object obs = env.getObserver();
        Quaternion tempQuaternion = new Quaternion(); // TODO move to class scope?
        if (obs instanceof Camera) {
            tempQuaternion.set(((Camera) obs).getRotation());
        } else {
            tempQuaternion.set(((Spatial) obs).getWorldRotation());
        }

        return tempQuaternion.multLocal(getOrientation(index));
    }

    @Override
    public Vector3f getFinalObserverPosition(int index) {
        // Copied from OpenVRInput

        VREnvironment env = hardware.getEnvironment();
        OculusViewManager vrvm = (OculusViewManager) hardware.getEnvironment().getVRViewManager();

        Object obs = env.getObserver();
        Vector3f pos = getPosition(index);
        if (obs instanceof Camera) {
            ((Camera) obs).getRotation().mult(pos, pos);
            return pos.addLocal(((Camera) obs).getLocation());
        } else {
            ((Spatial) obs).getWorldRotation().mult(pos, pos);
            return pos.addLocal(((Spatial) obs).getWorldTranslation());
        }
    }

    @Override
    public boolean isInputDeviceTracking(int index) {
        int flags = trackingState.HandStatusFlags(index);
        return (flags & ovrStatus_PositionTracked) != 0; // TODO do we require orientation as well?
    }

    // Input Getters

    @Override
    public Vector2f getAxis(int controllerIndex, VRInputType forAxis) {
        Vector2f result = getAxisRaw(controllerIndex, forAxis);
        return result == null ? null : result.multLocal(axisMultiplier);
    }

    @Override
    public Vector2f getAxisRaw(int controllerIndex, VRInputType forAxis) {
        switch (forAxis) {
            case OculusThumbstickAxis:
                return cv(inputState.Thumbstick(controllerIndex));
            case OculusTriggerAxis:
                return axis(inputState.IndexTrigger(controllerIndex));
            case OculusGripAxis:
                return axis(inputState.HandTrigger(controllerIndex));
            default:
                return null;
        }
    }

    @Override
    public boolean isButtonDown(int controllerIndex, VRInputType checkButton) {
        return isButtonDownForStatus(controllerIndex, checkButton, buttons, touch);
    }

    public boolean isButtonDownForStatus(int controllerIndex, VRInputType checkButton, int buttons, int touch) {
        int buttonMask = (controllerIndex == ovrHand_Left) ? ovrButton_LMask : ovrButton_RMask;
        int touchMask = (controllerIndex == ovrHand_Left) ?
                (ovrTouch_LButtonMask + ovrTouch_LPoseMask) :
                (ovrTouch_RButtonMask + ovrTouch_RPoseMask);

        switch (checkButton) {
            default:
                return false;

            case OculusTopButton: // Physical buttons
            case OculusBottomButton:
            case OculusThumbstickButton:
            case OculusMenuButton:
                return (buttons & buttonMask & checkButton.getValue()) != 0;

            case OculusTopTouch: // Standard capacitive buttons
            case OculusBottomTouch:
            case OculusThumbstickTouch:
            case OculusThumbrestTouch:
            case OculusIndexTouch:
            case OculusThumbUp: // Calculated/virtual capacitive buttons
            case OculusIndexPointing:
                return (touch & touchMask & checkButton.getValue()) != 0;
        }
    }

    // Since-last-call stuff

    @Override
    public void resetInputSinceLastCall() {
        lastButtons = 0;
        lastTouch = 0;
    }

    @Override
    public boolean wasButtonPressedSinceLastCall(int controllerIndex, VRInputType checkButton) {
        boolean wasPressed = isButtonDownForStatus(controllerIndex, checkButton, lastButtons, lastTouch);
        lastButtons = buttons;
        lastTouch = touch;
        return !wasPressed && isButtonDown(controllerIndex, checkButton);
    }

    @Override
    public Vector2f getAxisDeltaSinceLastCall(int controllerIndex, VRInputType forAxis) {
        int index;
        switch (forAxis) {
            case OculusTriggerAxis:
                index = 0;
                break;
            case OculusGripAxis:
                index = 1;
                break;
            case OculusThumbstickAxis:
                index = 2;
                break;
            default:
                return null;
        }

        Vector2f last = lastAxes[controllerIndex][index];
        if (last == null) {
            last = lastAxes[controllerIndex][index] = new Vector2f();
        }

        Vector2f current = getAxis(controllerIndex, forAxis);

        // TODO could this lead to accuracy problems?
        current.subtractLocal(last);
        last.addLocal(current);

        return current;
    }

    // Misc

    @Override
    public boolean init() {
        throw new UnsupportedOperationException("Input initialized at creation time");
    }

    @Override
    public void updateConnectedControllers() {
        throw new UnsupportedOperationException("Automatically done by LibOVR (I think?)");
    }

    @Override
    public float getAxisMultiplier() {
        return axisMultiplier;
    }

    @Override
    public void setAxisMultiplier(float axisMultiplier) {
        this.axisMultiplier = axisMultiplier;
    }

    @Override
    public void triggerHapticPulse(int controllerIndex, float seconds) {
        // TODO: How do we time so we can turn the feedback off?
    }

    @Override
    public boolean isInputFocused() {
        return sessionStatus.IsVisible(); // TODO do we need HmdMounted, or is it counted in IsVisible
    }

    @Override
    public Object getRawControllerState(int index) {
        throw new UnsupportedOperationException("Cannot get raw controller state!");
    }

    @Override
    public void swapHands() {
        // Do nothing.
        // TODO although OSVR and OpenVR if it has more than two controllers both do nothing, shouldn't we be
        // TODO throwing an exception or something?
    }

    @Override
    public int getTrackedControllerCount() {
        // TODO: Shouldn't we be seeing if the user has the touch controllers first?
        return 2;
    }

    @Override
    public VRTrackedController getTrackedController(int index) {
        return controllers[index];
    }

    /**
     * The object form representation of a controller.
     */
    public class OculusController implements VRTrackedController {

        /**
         * The ID of the hand to track
         */
        private int hand;

        public OculusController(int hand) {
            this.hand = hand;
        }

        @Override
        public String getControllerName() {
            return "Touch"; // TODO
        }

        @Override
        public String getControllerManufacturer() {
            return "Oculus"; // TODO
        }

        @Override
        public Vector3f getPosition() {
            return OculusVRInput.this.getPosition(hand);
        }

        @Override
        public Quaternion getOrientation() {
            return OculusVRInput.this.getOrientation(hand);
        }

        @Override
        public Matrix4f getPose() {
            Matrix4f mat = new Matrix4f();
            mat.setRotationQuaternion(getOrientation());
            mat.setTranslation(getPosition());
            return mat;
        }
    }
}

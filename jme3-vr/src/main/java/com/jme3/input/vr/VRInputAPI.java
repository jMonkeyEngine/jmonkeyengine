package com.jme3.input.vr;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;

/**
 * An interface that represents a VR input (typically a VR device such as a controller).
 * @author reden - phr00t - https://github.com/phr00t
 * @author Julien Seinturier - COMEX SA - <a href="http://www.seinturier.fr">http://www.seinturier.fr</a>
 */
public interface VRInputAPI {

    /**
     * Registers an action manifest. An actions manifest is a file that defines "actions" a player can make.
     * (An action is an abstract version of a button press). The action manifest may then also include references to
     * further files that define default mappings between those actions and physical buttons on the VR controllers.
     *
     * Note that registering an actions manifest will deactivate legacy inputs (i.e. methods such as {@link #isButtonDown}
     * will no longer work
     *
     * See https://github.com/ValveSoftware/openvr/wiki/Action-manifest for documentation on how to create an
     * action manifest
     *
     * This option is only relevant to OpenVR
     *
     * @param actionManifestAbsolutePath
     *          the absolute file path to an actions manifest
     * @param startingActiveActionSet
     *          the actions in the manifest are divided into action sets (groups) by their prefix (e.g. "/actions/main").
     *          These action sets can be turned off and on per frame. This argument sets the action set that will be
     *          active now. The active action sets can be later be changed by calling {@link #setActiveActionSet}.
     *          Note that at present only a single set at a time is supported
     *
     */
    default void registerActionManifest( String actionManifestAbsolutePath, String startingActiveActionSet ){
        throw new UnsupportedOperationException("Action manifests are not supported for the currently used VR API");
    }

    /**
     * Updates the active action set (the action group that will have their states available to be polled).
     *
     * Note that this update will not take effect until the next loop
     * Note that at present only a single set at a time is supported
     *
     * @param activeActionSet
     *          the actions in the manifest are divided into action sets (groups) by their prefix (e.g. "/actions/main").
     *          These action sets can be turned off and on per frame. This argument sets the action set that will be
     *          active now.
     */
    default void setActiveActionSet( String activeActionSet ){
        throw new UnsupportedOperationException("Action manifests are not supported for the currently used VR API");
    }

    /**
     * Gets the current state of the action (abstract version of a button press).
     *
     * This is called for digital style actions (a button is pressed, or not)
     *
     * This method is commonly called when it's not important which hand the action is bound to (e.g. if a button press
     * is opening your inventory that could be bound to either left or right hand and that would not matter).
     *
     * If the handedness matters use {@link #getDigitalActionState(String, String)}
     *
     * {@link #registerActionManifest} must have been called before using this method.
     *
     * @param actionName The name of the action. Will be something like /actions/main/in/openInventory
     * @return the DigitalActionState that has details on if the state has changed, what the state is etc.
     */
    default DigitalActionState getDigitalActionState( String actionName ){
        return getDigitalActionState(actionName, null);
    }

    /**
     * Gets the current state of the action (abstract version of a button press).
     *
     * This is called for digital style actions (a button is pressed, or not)
     *
     * This method is commonly called when it is important which hand the action is found on. For example while
     * holding a weapon a button may be bound to "eject magazine" to allow you to load a new one, but that would only
     * want to take effect on the hand that is holding the weapon
     *
     * Note that restrictToInput only restricts, it must still be bound to the input you want to receive the input from in
     * the action manifest default bindings.
     *
     * {@link #registerActionManifest} must have been called before using this method.
     *
     * @param actionName The name of the action. E.g. /actions/main/in/openInventory
     * @param restrictToInput the input to restrict the action to. E.g. /user/hand/right. Or null, which means "any input"
     * @return the DigitalActionState that has details on if the state has changed, what the state is etc.
     */
    default DigitalActionState getDigitalActionState( String actionName, String restrictToInput ){
        throw new UnsupportedOperationException("Action manifests are not supported for the currently used VR API");
    }

    /**
     * Gets the current state of the action (abstract version of a button press).
     *
     * This is called for analog style actions (most commonly joysticks, but button pressure can also be mapped in analog).
     *
     * This method is commonly called when it's not important which hand the action is bound to (e.g. if the thumb stick
     * is controlling a third-person character in-game that could be bound to either left or right hand and that would
     * not matter).
     *
     * If the handedness matters use {@link #getAnalogActionState(String, String)}
     *
     * {@link #registerActionManifest} must have been called before using this method.
     *
     * @param actionName The name of the action. E.g. /actions/main/in/openInventory
     * @return the DigitalActionState that has details on if the state has changed, what the state is etc.
     */
    default AnalogActionState getAnalogActionState( String actionName ){
        return getAnalogActionState(actionName, null);
    }

    /**
     * Gets the current state of the action (abstract version of a button press).
     *
     * This is called for analog style actions (most commonly joysticks, but button pressure can also be mapped in analog).
     *
     * This method is commonly called when it is important which hand the action is found on. For example an "in universe"
     * joystick that has a hat control might (while you are holding it) bind to the on-controller hat, but only on the hand
     * holding it
     *
     * Note that restrictToInput only restricts, it must still be bound to the input you want to receive the input from in
     * the action manifest default bindings.
     *
     * {@link #registerActionManifest} must have been called before using this method.
     *
     * @param actionName The name of the action. E.g. /actions/main/in/openInventory
     * @param restrictToInput the input to restrict the action to. E.g. /user/hand/right. Or null, which means "any input"
     * @return the DigitalActionState that has details on if the state has changed, what the state is etc.
     */
    default AnalogActionState getAnalogActionState( String actionName, String restrictToInput ){
        throw new UnsupportedOperationException("Action manifests are not supported for the currently used VR API");
    }

    /**
     * Check if the given button is down (more generally if the given input type is activated).
     *
     * Deprecated as should use an actions manifest approach. See {@link #registerActionManifest}. Note; action based will only work with the OpenVR api
     *
     * @param controllerIndex the index of the controller to check.
     * @param checkButton the button / input to check.
     * @return <code>true</code> if the button / input is down / activated and <code>false</code> otherwise.
     */
    @Deprecated
    public boolean isButtonDown(int controllerIndex, VRInputType checkButton);

    /**
     * Check if the given button / input from the given controller has been just pressed / activated.
     *
     * Deprecated as should use an actions manifest approach. See {@link #registerActionManifest}.  Note; action based will only work with the OpenVR api
     *
     * @param controllerIndex the index of the controller.
     * @param checkButton the button / input to check.
     * @return <code>true</code> if the given button / input from the given controller has been just pressed / activated and <code>false</code> otherwise.
     */
    @Deprecated
    public boolean wasButtonPressedSinceLastCall(int controllerIndex, VRInputType checkButton);

    /**
     * Reset the current activation of the inputs. After a call to this method, all input activation is considered as new activation.
     * @see #wasButtonPressedSinceLastCall(int, VRInputType)
     *
     * Deprecated as should use an actions manifest approach. See {@link #registerActionManifest}.  Note; action based will only work with the OpenVR api
     */
    @Deprecated
    public void resetInputSinceLastCall();

    /**
     * Get the controller axis delta from the last value.
     *
     * Deprecated as should use an actions manifest approach. See {@link #registerActionManifest}.  Note; action based will only work with the OpenVR api
     *
     * @param controllerIndex the index of the controller.
     * @param forAxis the axis.
     * @return the controller axis delta from the last call.
     */
    @Deprecated
    public Vector2f getAxisDeltaSinceLastCall(int controllerIndex, VRInputType forAxis);

    /**
     * Get the controller velocity on all axes.
     * @param controllerIndex the index of the controller.
     * @return the controller velocity on all axes.
     * @see #getAngularVelocity(int)
     */
    public Vector3f getVelocity(int controllerIndex);

    /**
     * Get the controller angular velocity on all axes.
     * @param controllerIndex the index of the controller.
     * @return the controller angular velocity on all axes.
     * @see #getVelocity(int)
     */
    public Vector3f getAngularVelocity(int controllerIndex);

    /**
     * Get the axis value for the given input on the given controller.
     * This value is the {@link #getAxisRaw(int, VRInputType) raw value} multiplied by the  {@link #getAxisMultiplier() axis multiplier}.
     *
     * Deprecated as should use an actions manifest approach. See {@link #registerActionManifest}. Note; action based will only work with the OpenVR api
     *
     * @param controllerIndex the index of the controller.
     * @param forAxis the axis.
     * @return the axis value for the given input on the given controller.
     * @see #getAxisRaw(int, VRInputType)
     * @see #getAxisMultiplier()
     */
    @Deprecated
    public Vector2f getAxis(int controllerIndex, VRInputType forAxis);

    /**
     * Get the axis value for the given input on the given controller.
     *
     * Deprecated as should use an actions manifest approach. See {@link #registerActionManifest}  Note; action based will only work with the OpenVR api
     *
     * @param controllerIndex the index of the controller.
     * @param forAxis the axis.
     * @return the axis value for the given input on the given controller.
     * @see #getAxis(int, VRInputType)
     */
    @Deprecated
    public Vector2f getAxisRaw(int controllerIndex, VRInputType forAxis);

    /**
     * Initialize the input.
     * @return <code>true</code> if the initialization is successful and <code>false</code> otherwise.
     */
    public boolean init();

    /**
     * Get the number of tracked controllers (for example, hand controllers) attached to the VR system.
     * @return the number of controllers attached to the VR system.
     * @see #getTrackedController(int)
     */
    public int getTrackedControllerCount();

    /**
     * Get a tracked controller (for example, a hand controller) that is attached to the VR system.
     * @param index the index of the controller.
     * @return the tracked controller (for example, a hand controller) that is attached to the VR system.
     * @see #getTrackedControllerCount()
     */
    public VRTrackedController getTrackedController(int index);

    /**
     * Update the connected controllers.
     * This method should be used just after the initialization of the input.
     */
    public void updateConnectedControllers();

    /**
     * Update the controller states.
     * This method should be called before accessing any controller data.
     */
    public void updateControllerStates();

    /**
     * Get the native wrapping of a controller state.
     * @param index the index of the controller.
     * @return the native wrapping of a controller state.
     */
    public Object getRawControllerState(int index);

    /**
     * Swap the two hands (exchange the hands' controller 1 and 2 indices).
     */
    public void swapHands();

    /**
     * Get the controller axis multiplier.
     * The controller axis raw data (trackpad, trigger, ...) value is multiplied by the one given in parameter.
     * @return the controller axis multiplier.
     * @see #setAxisMultiplier(float)
     */
    public float getAxisMultiplier();

    /**
     * Set the controller axis multiplier.
     * The controller axis raw data (trackpad, trigger, ...) value is multiplied by the one given in parameter.
     * @param set the controller axis multiplier.
     * @see #getAxisMultiplier()
     */
    public void setAxisMultiplier(float set);

    //public Matrix4f getPoseForInputDevice(int index);

    /**
     * Check if the VR system has the focus and if it's not used by other process.
     * @return <code>true</code> if the VR system has the focus and <code>false</code> otherwise.
     */
    public boolean isInputFocused();

    /**
     * Check if the input device is actually tracked (i-e if we can obtain a pose from the input).
     * @param index the index of the controller.
     * @return <code>true</code> if the input device is actually tracked and <code>false</code> otherwise.
     */
    public boolean isInputDeviceTracking(int index);

    /**
     * Get the orientation of the input.
     * @param index the index of the controller.
     * @return the orientation of the input.
     */
    public Quaternion getOrientation(int index);

    /**
     * Get the position of the input.
     * @param index the index of the controller.
     * @return the position of the input.
     */
    public Vector3f getPosition(int index);

    /**
     * Get where is the controller pointing, after all rotations are combined.
     * This position should include observer rotation from the VR application.
     * @param index the index of the controller.
     * @return the rotation of the input after all positional tracking is complete.
     */
    public Quaternion getFinalObserverRotation(int index);

    /**
     * Get the position of the input after all positional tracking is complete.
     * This position should include observer position from the VR application.
     * @param index the index of the controller.
     * @return the position of the input after all positional tracking is complete.
     */
    public Vector3f getFinalObserverPosition(int index);

    /**
     * Trigger a haptic pulse on the selected controller for the duration given in parameters (in seconds).
     *
     * Deprecated, use triggerHapticAction instead (as it has more options and doesn't use deprecated methods)
     *
     * @param controllerIndex the index of the controller.
     * @param seconds the duration of the pulse in seconds.
     */
    @Deprecated
    public void triggerHapticPulse(int controllerIndex, float seconds);

    /**
     * Triggers a haptic action (aka a vibration).
     *
     * Note if you want a haptic action in only one hand that is done either by only binding the action to one hand in
     * the action manifest's standard bindings or by binding to both and using {@link #triggerHapticAction(String, float, float, float, String)}
     * to control which input it gets set to at run time
     *
     * @param actionName The name of the action. Will be something like /actions/main/out/vibrate
     * @param duration how long in seconds the
     * @param frequency in cycles per second
     * @param amplitude between 0 and 1
     */
   default void triggerHapticAction( String actionName, float duration, float frequency, float amplitude){
       triggerHapticAction( actionName, duration, frequency, amplitude, null );
   }

    /**
     * Triggers a haptic action (aka a vibration) restricted to just one input (e.g. left or right hand).
     *
     * Note that restrictToInput only restricts, it must still be bound to the input you want to send the haptic to in
     * the action manifest default bindings.
     *
     * This method is typically used to bind the haptic to both hands then decide at run time which hand to sent to     *
     *
     * @param actionName The name of the action. Will be something like /actions/main/out/vibrate
     * @param duration how long in seconds the
     * @param frequency in cycles per second
     * @param amplitude between 0 and 1
     * @param restrictToInput the input to restrict the action to. E.g. /user/hand/right, /user/hand/left. Or null, which means "any input"
     */
    default void triggerHapticAction( String actionName, float duration, float frequency, float amplitude, String restrictToInput){
        throw new UnsupportedOperationException("Action manifests are not supported for the currently used VR API");
    }
}

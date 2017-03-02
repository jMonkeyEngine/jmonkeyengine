/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.input.vr;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;

/**
 * An interface that represents a VR input (typically a VR device such as a controller).
 * @author reden - phr00t - https://github.com/phr00t
 * @author Julien Seinturier - (c) 2016 - JOrigin project - <a href="http://www.jorigin.org">http:/www.jorigin.org</a>
 */
public interface VRInputAPI {

	/**
	 * Check if the given button is down (more generally if the given input type is activated).
	 * @param controllerIndex the index of the controller to check.
	 * @param checkButton the button / input to check.
	 * @return <code>true</code> if the button / input is down / activated and <code>false</code> otherwise.
	 */
    public boolean isButtonDown(int controllerIndex, VRInputType checkButton);
    
    /**
     * Check if the given button / input from the given controller has been just pressed / activated.
     * @param controllerIndex the index of the controller.
     * @param checkButton the button / input to check.
     * @return <code>true</code> if the given button / input from the given controller has been just pressed / activated and <code>false</code> otherwise.
     */
    public boolean wasButtonPressedSinceLastCall(int controllerIndex, VRInputType checkButton);
    
    /**
     * Reset the current activation of the inputs. After a call to this method, all input activation is considered as new activation.
     * @see #wasButtonPressedSinceLastCall(int, VRInputType)
     */
    public void resetInputSinceLastCall();
    
    /**
     * Get the controller axis delta from the last value.
     * @param controllerIndex the index of the controller.
     * @param forAxis the axis.
     * @return the controller axis delta from the last call.
     */
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
     * @param controllerIndex the index of the controller.
     * @param forAxis the axis.
     * @return the axis value for the given input on the given controller.
     * @see #getAxisRaw(int, VRInputType)
     * @see #getAxisMultiplier()
     */
    public Vector2f getAxis(int controllerIndex, VRInputType forAxis);
    
    /**
     * Get the axis value for the given input on the given controller. 
     * @param controllerIndex the index of the controller.
     * @param forAxis the axis.
     * @return the axis value for the given input on the given controller.
     * @see #getAxis(int, VRInputType)
     */
    public Vector2f getAxisRaw(int controllerIndex, VRInputType forAxis);

    /**
     * Initialize the input.
     * @return <code>true</code> if the initialization is successful and <code>false</code> otherwise.
     */
    public boolean init();
    
    /**
     * Get the number of tracked controller (for example an hand controllers) attached to the VR system.
     * @return the number of controller attached to the VR system.
     * @see #getTrackedController(int)
     */
    public int getTrackedControllerCount();
    
    /**
     * Get the tracked controller (for example an hand controllers) that is attached to the VR system.
     * @param index the index of the controller.
     * @return the tracked controller (for example an hand controllers) that is attached to the VR system.
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
     * Swap the two hands (exchange the hands controller 1 & 2 indices).
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
     * This position should include includes observer rotation from the VR application.
     * @param index the index of the controller.
     * @return the rotation of the input after all positional tracking is complete.
     */
    public Quaternion getFinalObserverRotation(int index);
    
    /**
     * Get the position of the input after all positional tracking is complete.
     * This position should include includes observer position from the VR application.
     * @param index the index of the controller.
     * @return the position of the input after all positional tracking is complete.
     */
    public Vector3f getFinalObserverPosition(int index);
    
    /**
     * Trigger an haptic pulse on the selected controller for the duration given in parameters (in seconds).
     * @param controllerIndex the index of the controller.
     * @param seconds the duration of the pulse in seconds.
     */
    public void triggerHapticPulse(int controllerIndex, float seconds);
    
}

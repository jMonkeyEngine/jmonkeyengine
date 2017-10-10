package com.jme3.input.vr;

import static org.lwjgl.ovr.OVR.*; // For the button constants

/**
 * The type of a VR input. This enumeration enables to determine which part of the VR device is involved within input callback.
 * @author reden - phr00t - https://github.com/phr00t
 * @author Julien Seinturier - (c) 2016 - JOrigin project - <a href="http://www.jorigin.org">http:/www.jorigin.org</a>
 *
 */
public enum VRInputType {
	
	/**
	 * an HTC vive trigger axis (about <a href="https://www.vive.com/us/support/category_howto/720435.html">Vive controller</a>).
	 */
    ViveTriggerAxis(0), 
    
	/**
	 * an HTC vive trackpad axis (about <a href="https://www.vive.com/us/support/category_howto/720435.html">Vive controller</a>).
	 */
    ViveTrackpadAxis(1), 
    
	/**
	 * an HTC vive grip button (about <a href="https://www.vive.com/us/support/category_howto/720435.html">Vive controller</a>).
	 */
    ViveGripButton(2), 
    
	/**
	 * an HTC vive menu button (about <a href="https://www.vive.com/us/support/category_howto/720435.html">Vive controller</a>).
	 */
    ViveMenuButton(3),

	/**
	 * The thumbstick on the Oculus Touch controllers.
	 * 
	 * Unlike the Vive controllers where the touchpad is commonly used
	 * as a virtual DPad, you should avoid using the thumbstick for purposes
	 * that do not require analog input.
	 */
	OculusThumbstickAxis(0),

	/**
	 * The trigger button on the Oculus Touch controllers.
	 * 
	 * This is the button under the user's index finger, and should not be used to
	 * pick up objects. See the 
	 * <a href="https://developer.oculus.com/documentation/pcsdk/latest/concepts/dg-input-touch-overview/"
	 * >Oculus Developer</a> documentation.
	 */
	OculusTriggerAxis(0),

	/**
	 * The 'grab' button on the Oculus Touch controllers.
	 * 
	 * This button should only (unless you have a compelling reason otherwise) be used to pick up objects.
	 */
	OculusGripAxis(0),

	/**
	 * The upper buttons on the Oculus Touch controllers - B on the right controller, and Y on the left.
	 */
	OculusTopButton(ovrButton_B | ovrButton_Y),

	/**
	 * The lower (not counting menu) buttons on the Oculus Touch
	 * controllers - A on the right controller, and X on the left.
	 */
	OculusBottomButton(ovrButton_A | ovrButton_X),

	/**
	 * The 'click' button on the Oculus Touch thumbsticks.
	 */
	OculusThumbstickButton(ovrButton_LThumb | ovrButton_RThumb),

	/**
	 * The game-usable menu button, under and to the left of the 'X' button on the left controller.
	 * 
	 * Most games use this to pause - it preferably should be used for at least that purpose, and is
	 * uncomfortable to rest your thumb on (in games where you suddenly have to pause/open a menu).
	 */
	OculusMenuButton(ovrButton_Enter),

	/**
	 * The capacitive touch sensors on the top buttons (Y and B) of the Oculus Touch.
	 */
	OculusTopTouch(ovrTouch_B | ovrTouch_Y),

	/**
	 * The capacitive touch sensors on the lower buttons (X and A) of the Oculus Touch.
	 */
	OculusBottomTouch(ovrTouch_A | ovrTouch_X),

	/**
	 * The capacitive touch sensors on the thumbsticks of the Oculus Touch.
	 */
	OculusThumbstickTouch(ovrTouch_LThumb | ovrTouch_RThumb),

	/**
	 * The capacitive touch sensors on the thumbrests of the Oculus Touch - this is a textured pad
     * on the Oculus Touch controller next to the ABXY buttons for users to reset their thumbs on.
	 * 
	 * While it probably goes without saying, only use this for gesture support and do not bind game
	 * elements to it.
	 */
	OculusThumbrestTouch(ovrTouch_LThumbRest | ovrTouch_RThumbRest),

	/**
	 * The state of a software calculation based on the capacitive touch sensor values that determine if
	 * the user has lifted their thumb off the controller, and can be used for gesture support.
	 * 
	 * This should be used instead of calculating this yourself based on the touch results of all the other
	 * parts of the controller.
	 */
	OculusThumbUp(ovrTouch_LThumbUp | ovrTouch_RThumbUp),

	/**
	 * Is the user resting their finger on the trigger of an Oculus Touch controller?
	 */
	OculusIndexTouch(ovrTouch_LIndexPointing | ovrTouch_RIndexPointing),

	/**
	 * Is the user pointing their finger forwards, as if to press a button?
	 * 
	 * This is internally calculated from proximity and filtering is applied - it should be used rather
	 * than !OculusIndexTouch, as it will probably lead to better results.
	 */
	OculusIndexPointing(ovrTouch_LIndexPointing | ovrTouch_RIndexPointing);
    
    /**
     * The value that codes the input type.
     */
	private final int value;
    
	/**
	 * Construct a new input type with the given code.
	 * @param value the code of the input type.
	 */
    private VRInputType(int value) {
        this.value = value;
    }

    /**
     * Get the value (code) of the input type.
     * @return the value (code) of the input type.
     */
    public int getValue() {
        return value;
    }        
}
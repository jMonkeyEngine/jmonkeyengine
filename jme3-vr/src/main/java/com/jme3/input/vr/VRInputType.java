package com.jme3.input.vr;

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
    ViveMenuButton(3);
    
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
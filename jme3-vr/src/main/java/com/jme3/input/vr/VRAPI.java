/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.input.vr;

import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;

/**
 * An interface that represents a VR system. This interface has to be implemented in order to wrap underlying VR system (OpenVR, OSVR, ...)
 * @author reden - phr00t - https://github.com/phr00t
 * @author Julien Seinturier - (c) 2016 - JOrigin project - <a href="http://www.jorigin.org">http:/www.jorigin.org</a>
 */
public interface VRAPI {
    
	/**
	 * Initialize this object from a VR system. All the native bindings to underlying VR system should be done within this method.
	 * @return <code>true</code> if the initialization is a success and <code>false</code> otherwise.
	 */
    public boolean initialize();
    
    /**
     * Initialize the VR compositor that will be used for rendering.
     * @param allowed <code>true</code> if the use of VR compositor is allowed and <code>false</code> otherwise.
     * @return <code>true</code> if the initialization is a success and <code>false</code> otherwise.
     */
    public boolean initVRCompositor(boolean allowed);
       
    /**
     * Get the object that wraps natively the VR system.
     * @return the object that wraps natively the VR system.
     */
    public Object getVRSystem();
    
    /**
     * Get the object that wraps natively the VR compositor.
     * @return the object that wraps natively the VR system.
     */
    public Object getCompositor();
    
    /**
     * Get the name of the underlying VR system.
     * @return the name of the underlying VR system.
     */
    public String getName();
    
    /**
     * Get the input provided by the underlying VR system.
     * @return the input provided by the underlying VR system.
     */
    public VRInputAPI getVRinput();
    
    /**
     * Flip the left and right eye..
     * @param set <code>true</code> if the eyes has to be flipped and <code>false</code> otherwise.
     */
    public void setFlipEyes(boolean set);
    
    /**
     * Set if latency information has to be logged.
     * @param set <code>true</code> if latency information has to be logged and <code>false</code> otherwise.
     */
    public void printLatencyInfoToConsole(boolean set);

    /**
     * Get the Head Mounted Device (HMD) display frequency.
     * @return the Head Mounted DEvice (HMD) display frequency.
     */
    public int getDisplayFrequency();
    
    /**
     * Close the link with underlying VR system and free all attached resources.
     */
    public void destroy();

    /**
     * Check if the VR API is initialized.
     * @return <code>true</code> if the VR API is initialized and <code>false</code> otherwise.
     * @see #initialize()
     */
    public boolean isInitialized();

    /**
     * Reset the VR system.
     */
    public void reset();

    /**
     * Get the size of an Head Mounted Device (HMD) rendering area in pixels.
     * @param store the size of an Head Mounted Device (HMD) rendering area in pixels (modified).
     */
    public void getRenderSize(Vector2f store);
    
    //public float getFOV(int dir);

    /**
     * Get the Head Mounted Device (HMD) interpupilar distance in meters.
     * @return the Head Mounted Device (HMD) interpupilar distance in meters.
     */
    public float getInterpupillaryDistance();
    
    /**
     * Get the Head Mounted Device (HMD) orientation.
     * @return the Head Mounted Device (HMD) orientation.
     */
    public Quaternion getOrientation();

    /**
     * Get the Head Mounted Device (HMD) position.
     * @return the Head Mounted Device (HMD) orientation.
     */
    public Vector3f getPosition();
    
    /**
     * Get the Head Mounted Device (HMD) position and orientation.
     * @param storePos the Head Mounted Device (HMD) position (modified).
     * @param storeRot the Head Mounted Device (HMD) rotation (modified).
     */
    public void getPositionAndOrientation(Vector3f storePos, Quaternion storeRot);
    
    /**
     * Update Head Mounted Device (HMD) pose internal storage. This method should be called before other calls to HMD position/orientation access.
     */
    public void updatePose();

    /**
     * Get the Head Mounted Device (HMD) left eye projection matrix.
     * @param cam the camera attached to the left eye.
     * @return the Head Mounted Device (HMD) left eye projection matrix.
     */
    public Matrix4f getHMDMatrixProjectionLeftEye(Camera cam);
        
    /**
     * Get the Head Mounted Device (HMD) right eye projection matrix.
     * @param cam the camera attached to the right eye.
     * @return the Head Mounted Device (HMD) right eye projection matrix.
     */
    public Matrix4f getHMDMatrixProjectionRightEye(Camera cam);
    
    /**
     * Get the Head Mounted Device (HMD) left eye pose (position of the eye from the head) as a {@link Vector3f vector}.
     * @return the Head Mounted Device (HMD) left eye pose as a {@link Vector3f vector}.
     */
    public Vector3f getHMDVectorPoseLeftEye();
    
    /**
     * Get the Head Mounted Device (HMD) right eye pose (position of the eye from the head) as a {@link Vector3f vector}.
     * @return the Head Mounted Device (HMD) right eye pose as a {@link Vector3f vector}.
     */
    public Vector3f getHMDVectorPoseRightEye();
    
    /**
     * Returns the transform between the view space and left eye space. 
     * Eye space is the per-eye flavor of view space that provides stereo disparity. 
     * Instead of Model * View * Projection the model is Model * View * Eye * Projection. 
     * Normally View and Eye will be multiplied together and treated as View.
     * This matrix incorporates the user's interpupillary distance (IPD).
     * @return the transform between the view space and eye space. 
     */
    public Matrix4f getHMDMatrixPoseLeftEye();
    
    /**
     * Returns the transform between the view space and right eye space. 
     * Eye space is the per-eye flavor of view space that provides stereo disparity. 
     * Instead of Model * View * Projection the model is Model * View * Eye * Projection. 
     * Normally View and Eye will be multiplied together and treated as View.
     * This matrix incorporates the user's interpupillary distance (IPD).
     * @return the transform between the view space and eye space. 
     */
    public Matrix4f getHMDMatrixPoseRightEye();
    
    /**
     * Get the Head Mounted Device (HMD) type.
     * @return the Head Mounted Device (HMD) type.
     */
    public HmdType getType();
    
    /**
     * Get the seated to absolute position.
     * @return the seated to absolute position.
     */
    public Vector3f getSeatedToAbsolutePosition();
    
}

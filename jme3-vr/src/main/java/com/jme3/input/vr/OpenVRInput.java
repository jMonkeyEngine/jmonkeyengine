/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.input.vr;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.app.VREnvironment;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Spatial;
import com.jme3.system.jopenvr.JOpenVRLibrary;
import com.jme3.system.jopenvr.OpenVRUtil;
import com.jme3.system.jopenvr.VRControllerState_t;
import com.jme3.system.jopenvr.VR_IVRSystem_FnTable;
import com.jme3.util.VRUtil;
import com.jme3.util.VRViewManagerOpenVR;

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
 * A class that wraps an <a href="https://github.com/ValveSoftware/openvr/wiki/API-Documentation">OpenVR</a> input.<br>
 * <code>null</code> values will be returned if no valid pose exists, or that input device isn't available
 * user code should check for <code>null</code> values.
 * @author reden - phr00t - https://github.com/phr00t
 * @author Julien Seinturier - (c) 2016 - JOrigin project - <a href="http://www.jorigin.org">http:/www.jorigin.org</a>
 */
public class OpenVRInput implements VRInputAPI {
        
	private static final Logger logger = Logger.getLogger(OpenVRInput.class.getName());
	
    private final VRControllerState_t[] cStates = new VRControllerState_t[JOpenVRLibrary.k_unMaxTrackedDeviceCount];
    
    private final Quaternion[] rotStore = new Quaternion[JOpenVRLibrary.k_unMaxTrackedDeviceCount];
    
    private final Vector3f[] posStore   = new Vector3f[JOpenVRLibrary.k_unMaxTrackedDeviceCount];
    
    private static final int[] controllerIndex = new int[JOpenVRLibrary.k_unMaxTrackedDeviceCount];
    
    private int controllerCount = 0;
    
    private final Vector2f tempAxis = new Vector2f(), temp2Axis = new Vector2f();
    
    private final Vector2f lastCallAxis[] = new Vector2f[JOpenVRLibrary.k_unMaxTrackedDeviceCount];
    
    private final boolean needsNewVelocity[]    = new boolean[JOpenVRLibrary.k_unMaxTrackedDeviceCount];
    
    private final boolean needsNewAngVelocity[] = new boolean[JOpenVRLibrary.k_unMaxTrackedDeviceCount];
    
    private final boolean buttonDown[][]        = new boolean[JOpenVRLibrary.k_unMaxTrackedDeviceCount][16];
    
    private float axisMultiplier = 1f;
    
    private final Vector3f tempVel = new Vector3f();
    
    private final Quaternion tempq = new Quaternion();

    private VREnvironment environment;

    private List<VRTrackedController> trackedControllers = null;
    
    /**
     * Create a new <a href="https://github.com/ValveSoftware/openvr/wiki/API-Documentation">OpenVR</a> input attached to the given VR environment.
     * @param environment the VR environment to which the input is attached.
     */
    public OpenVRInput(VREnvironment environment){
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
        if( controllerCount != 2 ) return; 
        int temp = controllerIndex[0];
        controllerIndex[0] = controllerIndex[1];
        controllerIndex[1] = temp;
    }
    
    @Override
    public boolean isButtonDown(int controllerIndex, VRInputType checkButton) {
        VRControllerState_t cs = cStates[OpenVRInput.controllerIndex[controllerIndex]];
        switch( checkButton ) {
            default:
                return false;
            case ViveGripButton:
                return (cs.ulButtonPressed & 4) != 0;
            case ViveMenuButton:
                return (cs.ulButtonPressed & 2) != 0;                
            case ViveTrackpadAxis:
                return (cs.ulButtonPressed & 4294967296l) != 0;
            case ViveTriggerAxis:
                return (cs.ulButtonPressed & 8589934592l) != 0;                
        }
    }
    
    @Override
    public boolean wasButtonPressedSinceLastCall(int controllerIndex, VRInputType checkButton) {
        boolean buttonDownNow = isButtonDown(controllerIndex, checkButton);
        int checkButtonValue = checkButton.getValue();
        int cIndex = OpenVRInput.controllerIndex[controllerIndex];
        boolean retval = buttonDownNow == true && buttonDown[cIndex][checkButtonValue] == false;
        buttonDown[cIndex][checkButtonValue] = buttonDownNow;
        return retval;
    }
    
    @Override
    public void resetInputSinceLastCall() {
        for(int i=0;i<lastCallAxis.length;i++) {
            lastCallAxis[i].x = 0f;
            lastCallAxis[i].y = 0f;
        }
        for(int i=0;i<JOpenVRLibrary.k_unMaxTrackedDeviceCount;i++) {
            for(int j=0;j<16;j++) {
                buttonDown[i][j] = false;
            }
        }
    }
    
    @Override
    public Vector2f getAxisDeltaSinceLastCall(int controllerIndex, VRInputType forAxis) {                
        int axisIndex = forAxis.getValue();
        temp2Axis.set(lastCallAxis[axisIndex]);
        lastCallAxis[axisIndex].set(getAxis(controllerIndex, forAxis));
        if( (temp2Axis.x != 0f || temp2Axis.y != 0f) && (lastCallAxis[axisIndex].x != 0f || lastCallAxis[axisIndex].y != 0f) ) {
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
        int index = OpenVRInput.controllerIndex[controllerIndex];
        if( needsNewVelocity[index] ) {
            OpenVR.hmdTrackedDevicePoses[index].readField("vVelocity");
            needsNewVelocity[index] = false;
        }
        tempVel.x = OpenVR.hmdTrackedDevicePoses[index].vVelocity.v[0];
        tempVel.y = OpenVR.hmdTrackedDevicePoses[index].vVelocity.v[1];
        tempVel.z = OpenVR.hmdTrackedDevicePoses[index].vVelocity.v[2];
        return tempVel;
    }
    
    @Override
    public Vector3f getAngularVelocity(int controllerIndex) {
        int index = OpenVRInput.controllerIndex[controllerIndex];
        if( needsNewAngVelocity[index] ) {
            OpenVR.hmdTrackedDevicePoses[index].readField("vAngularVelocity");
            needsNewAngVelocity[index] = false;
        }
        tempVel.x = OpenVR.hmdTrackedDevicePoses[index].vAngularVelocity.v[0];
        tempVel.y = OpenVR.hmdTrackedDevicePoses[index].vAngularVelocity.v[1];
        tempVel.z = OpenVR.hmdTrackedDevicePoses[index].vAngularVelocity.v[2];
        return tempVel;
    }
    
    @Override
    public Vector2f getAxisRaw(int controllerIndex, VRInputType forAxis) {
        VRControllerState_t cs = cStates[OpenVRInput.controllerIndex[controllerIndex]];
        switch( forAxis ) {
            default:
                return null;
            case ViveTriggerAxis:
                tempAxis.x = cs.rAxis[1].x;
                tempAxis.y = tempAxis.x;
                break;
            case ViveTrackpadAxis:
                tempAxis.x = cs.rAxis[0].x;
                tempAxis.y = cs.rAxis[0].y;
                break;
        }       
        return tempAxis;
    }

    @Override
    public Vector2f getAxis(int controllerIndex, VRInputType forAxis) {
        VRControllerState_t cs = cStates[OpenVRInput.controllerIndex[controllerIndex]];
        switch( forAxis ) {
            default:
                return null;
            case ViveTriggerAxis:
                tempAxis.x = cs.rAxis[1].x;
                tempAxis.y = tempAxis.x;
                break;
            case ViveTrackpadAxis:
                tempAxis.x = cs.rAxis[0].x;
                tempAxis.y = cs.rAxis[0].y;
                break;
        }       
        tempAxis.x *= axisMultiplier;
        tempAxis.y *= axisMultiplier;
        return tempAxis;
    }
    
    @Override
    public boolean init() {
    	
    	logger.config("Initialize OpenVR input.");
    	
        for(int i=0;i<JOpenVRLibrary.k_unMaxTrackedDeviceCount;i++) {
            rotStore[i] = new Quaternion();
            posStore[i] = new Vector3f();
            cStates[i] = new VRControllerState_t();
            cStates[i].setAutoSynch(false);
            cStates[i].setAutoRead(false);
            cStates[i].setAutoWrite(false);
            lastCallAxis[i] = new Vector2f();
            needsNewVelocity[i] = true;
            needsNewAngVelocity[i] = true;
            logger.config("  Input "+(i+1)+"/"+JOpenVRLibrary.k_unMaxTrackedDeviceCount+" binded.");
        }        
        
        return true;
    }
    
    @Override
    public VRTrackedController getTrackedController(int index){
    	if (trackedControllers != null){
    		if ((trackedControllers.size() > 0) && (index < trackedControllers.size())){
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
    public VRControllerState_t getRawControllerState(int index) {
        if( isInputDeviceTracking(index) == false ) return null;
        return cStates[controllerIndex[index]];
    }
    
    //public Matrix4f getPoseForInputDevice(int index) {
    //    if( isInputDeviceTracking(index) == false ) return null;
    //    return OpenVR.poseMatrices[controllerIndex[index]];
    //}
    
    @Override
    public boolean isInputFocused() {
    	
    	if (environment != null){
    		return ((VR_IVRSystem_FnTable)environment.getVRHardware().getVRSystem()).IsInputFocusCapturedByAnotherProcess.apply() == 0;
    	} else {
    		throw new IllegalStateException("VR input is not attached to a VR environment.");
    	}      
    }
    
    @Override
    public boolean isInputDeviceTracking(int index) {
        if( index < 0 || index >= controllerCount ){
        	return false;
        }
        
        return OpenVR.hmdTrackedDevicePoses[controllerIndex[index]].bPoseIsValid != 0;
    }
    
    @Override
    public Quaternion getOrientation(int index) {
        if( isInputDeviceTracking(index) == false ){
        	return null;
        }
        index = controllerIndex[index];
        VRUtil.convertMatrix4toQuat(OpenVR.poseMatrices[index], rotStore[index]);
        return rotStore[index];
    }

    @Override
    public Vector3f getPosition(int index) {
        if( isInputDeviceTracking(index) == false ){
        	return null;
        }
        
        // the hmdPose comes in rotated funny, fix that here
        index = controllerIndex[index];
        OpenVR.poseMatrices[index].toTranslationVector(posStore[index]);
        posStore[index].x = -posStore[index].x;
        posStore[index].z = -posStore[index].z;
        return posStore[index];
    }
    
    @Override
    public Quaternion getFinalObserverRotation(int index) {
    	
    	if (environment != null){
            VRViewManagerOpenVR vrvm = (VRViewManagerOpenVR)environment.getVRViewManager();
            
            if (vrvm != null){
                if(isInputDeviceTracking(index) == false ){
                	return null;
                }
                
                Object obs = environment.getObserver();
                if( obs instanceof Camera ) {
                    tempq.set(((Camera)obs).getRotation());
                } else {
                    tempq.set(((Spatial)obs).getWorldRotation());
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
    	
    	if (environment != null){
            VRViewManagerOpenVR vrvm = (VRViewManagerOpenVR)environment.getVRViewManager();
            
            if (vrvm != null){
                if(isInputDeviceTracking(index) == false ){
                	return null;
                }
                Object obs = environment.getObserver();
                Vector3f pos = getPosition(index);
                if( obs instanceof Camera ) {
                    ((Camera)obs).getRotation().mult(pos, pos);
                    return pos.addLocal(((Camera)obs).getLocation());
                } else {
                    ((Spatial)obs).getWorldRotation().mult(pos, pos);
                    return pos.addLocal(((Spatial)obs).getWorldTranslation());
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
        if( environment.isInVR() == false || isInputDeviceTracking(controllerIndex) == false ){
        	return;
        }
        
        // apparently only axis ID of 0 works
        ((VR_IVRSystem_FnTable)environment.getVRHardware().getVRSystem()).TriggerHapticPulse.apply(OpenVRInput.controllerIndex[controllerIndex],
                                                                                                     0, (short)Math.round(3f * seconds / 1e-3f));
    }
    
    @Override
    public void updateConnectedControllers() {
    	logger.config("Updating connected controllers.");
    	
    	if (environment != null){
    		controllerCount = 0;
        	for(int i=0;i<JOpenVRLibrary.k_unMaxTrackedDeviceCount;i++) {
        		if( ((OpenVR)environment.getVRHardware()).getVRSystem().GetTrackedDeviceClass.apply(i) == JOpenVRLibrary.ETrackedDeviceClass.ETrackedDeviceClass_TrackedDeviceClass_Controller ) {
        			
        			String controllerName   = "Unknown";
    				String manufacturerName = "Unknown";
    				try {
    					controllerName = OpenVRUtil.getTrackedDeviceStringProperty(((OpenVR)environment.getVRHardware()).getVRSystem(), i, JOpenVRLibrary.ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_TrackingSystemName_String);
    					manufacturerName = OpenVRUtil.getTrackedDeviceStringProperty(((OpenVR)environment.getVRHardware()).getVRSystem(), i, JOpenVRLibrary.ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_ManufacturerName_String);
    				} catch (Exception e) {
                      logger.log(Level.WARNING, e.getMessage(), e);
    				}
        			
        			controllerIndex[controllerCount] = i;
        			
        			// Send an Haptic pulse to the controller
        			triggerHapticPulse(controllerCount, 1.0f);
        			
        			controllerCount++;
        			logger.config("  Tracked controller "+(i+1)+"/"+JOpenVRLibrary.k_unMaxTrackedDeviceCount+" "+controllerName+" ("+manufacturerName+") attached.");
        		} else {
        			logger.config("  Controller "+(i+1)+"/"+JOpenVRLibrary.k_unMaxTrackedDeviceCount+" ignored.");
        		}
        	}
    	} else {
    	  throw new IllegalStateException("VR input is not attached to a VR environment.");
    	}
    }

    @Override
    public void updateControllerStates() {
    	
    	if (environment != null){
        	for(int i=0;i<controllerCount;i++) {
        		int index = controllerIndex[i];
        		((OpenVR)environment.getVRHardware()).getVRSystem().GetControllerState.apply(index, cStates[index], 5);
        		cStates[index].readField("ulButtonPressed");
        		cStates[index].readField("rAxis");
        		needsNewVelocity[index] = true;
        		needsNewAngVelocity[index] = true;
        	}
    	} else {
    		throw new IllegalStateException("VR input is not attached to a VR environment.");
    	}

    }

}

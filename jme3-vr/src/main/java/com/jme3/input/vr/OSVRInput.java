/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.input.vr;

import java.util.logging.Logger;

import com.jme3.app.VREnvironment;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Spatial;
import com.jme3.system.osvr.osvrclientkit.OsvrClientKitLibrary;
import com.jme3.system.osvr.osvrclientkit.OsvrClientKitLibrary.OSVR_ClientInterface;
import com.jme3.system.osvr.osvrclientreporttypes.OSVR_AnalogReport;
import com.jme3.system.osvr.osvrclientreporttypes.OSVR_ButtonReport;
import com.jme3.system.osvr.osvrclientreporttypes.OSVR_Pose3;
import com.jme3.system.osvr.osvrinterface.OsvrInterfaceLibrary;
import com.jme3.system.osvr.osvrtimevalue.OSVR_TimeValue;
import com.jme3.util.VRViewManagerOSVR;
import com.sun.jna.Callback;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;


/**
 * A class that wraps an <a href="http://www.osvr.org/">OSVR</a> input. 
 * @author reden - phr00t - https://github.com/phr00t
 * @author Julien Seinturier - (c) 2016 - JOrigin project - <a href="http://www.jorigin.org">http:/www.jorigin.org</a>
 */
public class OSVRInput implements VRInputAPI {

	private static final Logger logger = Logger.getLogger(OSVRInput.class.getName());
	
    // position example: https://github.com/OSVR/OSVR-Core/blob/master/examples/clients/TrackerState.c
    // button example: https://github.com/OSVR/OSVR-Core/blob/master/examples/clients/ButtonCallback.c
    // analog example: https://github.com/OSVR/OSVR-Core/blob/master/examples/clients/AnalogCallback.c
    
    private static final int ANALOG_COUNT = 3, BUTTON_COUNT = 7, CHANNEL_COUNT = 3;
    
    OSVR_ClientInterface[][] buttons;
    OSVR_ClientInterface[][][] analogs;
    OSVR_ClientInterface[] hands;
    
    OSVR_Pose3[] handState;
    Callback buttonHandler, analogHandler;
    OSVR_TimeValue tv = new OSVR_TimeValue();
    boolean[] isHandTracked = new boolean[2];
    
    private float[][][] analogState;
    private float[][] buttonState;
    
    private final Quaternion tempq = new Quaternion();
    private final Vector3f tempv = new Vector3f();
    private final Vector2f temp2 = new Vector2f();
    private final boolean[][] buttonDown = new boolean[16][16];
    
    private static final Vector2f temp2Axis = new Vector2f();
    private static final Vector2f lastCallAxis[] = new Vector2f[16];
    private static float axisMultiplier = 1f;
    
    private VREnvironment environment = null;
    
    /**
     * Get the system String that identifies a controller.
     * @param left is the controller is the left one (<code>false</code> if the right controller is needed).
     * @param index the index of the controller.
     * @return the system String that identifies the controller.
     */
    public static byte[] getButtonString(boolean left, byte index) {
        if( left ) {
            return new byte[] { '/', 'c', 'o', 'n', 't', 'r', 'o', 'l', 'l', 'e', 'r', '/', 'l', 'e', 'f', 't', '/', index, (byte)0 };
        }
        return new byte[] { '/', 'c', 'o', 'n', 't', 'r', 'o', 'l', 'l', 'e', 'r', '/', 'r', 'i', 'g', 'h', 't', '/', index, (byte)0 };
    }
    
    /**
     * The left hand system String.
     */
    public static byte[] leftHand = { '/', 'm', 'e', '/', 'h', 'a', 'n', 'd', 's', '/', 'l', 'e', 'f', 't', (byte)0 };
    
    /**
     * The right hand system String.
     */
    public static byte[] rightHand = { '/', 'm', 'e', '/', 'h', 'a', 'n', 'd', 's', '/', 'r', 'i', 'g', 'h', 't', (byte)0 };

    
    /**
     * Create a new <a href="http://www.osvr.org/">OSVR</a> input attached to the given {@link VREnvironment VR environment}.
     * @param environment the {@link VREnvironment VR environment} to which the input is attached.
     */
    public OSVRInput(VREnvironment environment){
      this.environment = environment;
    }
    
    
    @Override
    public boolean isButtonDown(int controllerIndex, VRInputType checkButton) {
        return buttonState[controllerIndex][checkButton.getValue()] != 0f;
    }

    @Override
    public boolean wasButtonPressedSinceLastCall(int controllerIndex, VRInputType checkButton) {
        boolean buttonDownNow = isButtonDown(controllerIndex, checkButton);
        int checkButtonValue = checkButton.getValue();
        boolean retval = buttonDownNow == true && buttonDown[controllerIndex][checkButtonValue] == false;
        buttonDown[controllerIndex][checkButtonValue] = buttonDownNow;
        return retval;
    }

    @Override
    public void resetInputSinceLastCall() {
        for(int i=0;i<lastCallAxis.length;i++) {
            lastCallAxis[i].x = 0f;
            lastCallAxis[i].y = 0f;
        }
        for(int i=0;i<16;i++) {
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
        return Vector3f.ZERO;
    }

    @Override
    public Vector3f getAngularVelocity(int controllerIndex) {
        return Vector3f.ZERO;
    }

    @Override
    public Vector2f getAxisRaw(int controllerIndex, VRInputType forAxis) {
        temp2.x = analogState[controllerIndex][forAxis.getValue()][0];
        temp2.y = analogState[controllerIndex][forAxis.getValue()][1];
        return temp2;
    }

    @Override
    public Vector2f getAxis(int controllerIndex, VRInputType forAxis) {
        temp2.x = analogState[controllerIndex][forAxis.getValue()][0] * axisMultiplier;
        temp2.y = analogState[controllerIndex][forAxis.getValue()][1] * axisMultiplier;
        return temp2;
    }
    
    private OSVR_ClientInterface getInterface(byte[] str) {
        PointerByReference pbr = new PointerByReference();
        OsvrClientKitLibrary.osvrClientGetInterface((OsvrClientKitLibrary.OSVR_ClientContext)environment.getVRHardware().getVRSystem(), str, pbr);
        return new OSVR_ClientInterface(pbr.getValue());
    }

    @Override
    public boolean init() {
        
    	logger.config("Initialize OSVR input.");
    	
        buttonHandler = new Callback() {
            @SuppressWarnings("unused")
			public void invoke(Pointer userdata, Pointer timeval, OSVR_ButtonReport report) {
                for(int i=0;i<2;i++) {
                    for(int j=0;j<BUTTON_COUNT;j++) {
                        if( buttons[i][j] == null ) continue;
                        if( userdata.toString().equals(buttons[i][j].getPointer().toString()) ) {
                            buttonState[i][j] = report.state;
                            return;
                        }
                    }
                }
            }                
        };  
        analogHandler = new Callback() {
            @SuppressWarnings("unused")
			public void invoke(Pointer userdata, Pointer timeval, OSVR_AnalogReport report) {
                for(int i=0;i<2;i++) {
                    for(int j=0;j<ANALOG_COUNT;j++) {
                        for(int k=0;k<CHANNEL_COUNT;k++) {
                            if( analogs[i][j][k] == null ) continue;
                            if( userdata.toString().equals(analogs[i][j][k].getPointer().toString()) ) {
                                analogState[i][j][k] = (float)report.state;
                                return;
                            }
                        }
                    }
                }
            }                
        };  
        
        buttons = new OSVR_ClientInterface[2][BUTTON_COUNT];
        analogs = new OSVR_ClientInterface[2][ANALOG_COUNT][CHANNEL_COUNT];
        buttonState = new float[2][BUTTON_COUNT];
        analogState = new float[2][ANALOG_COUNT][CHANNEL_COUNT];
        hands = new OSVR_ClientInterface[2];
        hands[0] = getInterface(leftHand);
        hands[1] = getInterface(rightHand);
        handState = new OSVR_Pose3[2];
        handState[0] = new OSVR_Pose3(); handState[1] = new OSVR_Pose3();
        for(int h=0;h<2;h++) {
            for(int i=0;i<BUTTON_COUNT-2;i++) {
                buttons[h][i] = getInterface(getButtonString(h==0, (byte)Integer.toString(i).toCharArray()[0]));
                OsvrClientKitLibrary.osvrRegisterButtonCallback(buttons[h][i], buttonHandler, buttons[h][i].getPointer()); 
            }
        }
        buttons[0][BUTTON_COUNT-2] = getInterface(new byte[] { '/', 'c', 'o', 'n', 't', 'r', 'o', 'l', 'l', 'e', 'r', '/', 'l', 'e', 'f', 't', '/', 'b', 'u', 'm', 'p', 'e', 'r', (byte)0 } );
        OsvrClientKitLibrary.osvrRegisterButtonCallback(buttons[0][BUTTON_COUNT-2], buttonHandler, buttons[0][BUTTON_COUNT-2].getPointer()); 
        buttons[1][BUTTON_COUNT-2] = getInterface(new byte[] { '/', 'c', 'o', 'n', 't', 'r', 'o', 'l', 'l', 'e', 'r', '/', 'r', 'i', 'g', 'h', 't', '/', 'b', 'u', 'm', 'p', 'e', 'r', (byte)0 } );
        OsvrClientKitLibrary.osvrRegisterButtonCallback(buttons[1][BUTTON_COUNT-2], buttonHandler, buttons[1][BUTTON_COUNT-2].getPointer()); 
        buttons[0][BUTTON_COUNT-1] = getInterface(new byte[] { '/', 'c', 'o', 'n', 't', 'r', 'o', 'l', 'l', 'e', 'r', '/', 'l', 'e', 'f', 't', '/', 'j', 'o', 'y', 's', 't', 'i', 'c', 'k', '/', 'b', 'u', 't', 't', 'o', 'n', (byte)0 } );
        OsvrClientKitLibrary.osvrRegisterButtonCallback(buttons[0][BUTTON_COUNT-1], buttonHandler, buttons[0][BUTTON_COUNT-1].getPointer()); 
        buttons[1][BUTTON_COUNT-1] = getInterface(new byte[] { '/', 'c', 'o', 'n', 't', 'r', 'o', 'l', 'l', 'e', 'r', '/', 'r', 'i', 'g', 'h', 't', '/', 'j', 'o', 'y', 's', 't', 'i', 'c', 'k', '/', 'b', 'u', 't', 't', 'o', 'n', (byte)0 } );
        OsvrClientKitLibrary.osvrRegisterButtonCallback(buttons[1][BUTTON_COUNT-1], buttonHandler, buttons[1][BUTTON_COUNT-1].getPointer()); 
            
        analogs[0][0][0] = getInterface(new byte[] { '/', 'c', 'o', 'n', 't', 'r', 'o', 'l', 'l', 'e', 'r', '/', 'l', 'e', 'f', 't', '/', 't', 'r', 'i', 'g', 'g', 'e', 'r', (byte)0 } );
        analogs[1][0][0] = getInterface(new byte[] { '/', 'c', 'o', 'n', 't', 'r', 'o', 'l', 'l', 'e', 'r', '/', 'r', 'i', 'g', 'h', 't', '/', 't', 'r', 'i', 'g', 'g', 'e', 'r', (byte)0 } );
        OsvrClientKitLibrary.osvrRegisterAnalogCallback(analogs[0][0][0], analogHandler, analogs[0][0][0].getPointer());
        OsvrClientKitLibrary.osvrRegisterAnalogCallback(analogs[1][0][0], analogHandler, analogs[1][0][0].getPointer());
        analogs[0][1][0] = getInterface(new byte[] { '/', 'c', 'o', 'n', 't', 'r', 'o', 'l', 'l', 'e', 'r', '/', 'l', 'e', 'f', 't', '/', 'j', 'o', 'y', 's', 't', 'i', 'c', 'k', '/', 'x', (byte)0 } );
        analogs[0][1][1] = getInterface(new byte[] { '/', 'c', 'o', 'n', 't', 'r', 'o', 'l', 'l', 'e', 'r', '/', 'l', 'e', 'f', 't', '/', 'j', 'o', 'y', 's', 't', 'i', 'c', 'k', '/', 'y', (byte)0 } );
        OsvrClientKitLibrary.osvrRegisterAnalogCallback(analogs[0][1][0], analogHandler, analogs[0][1][0].getPointer());
        OsvrClientKitLibrary.osvrRegisterAnalogCallback(analogs[0][1][1], analogHandler, analogs[0][1][1].getPointer());
        analogs[1][1][0] = getInterface(new byte[] { '/', 'c', 'o', 'n', 't', 'r', 'o', 'l', 'l', 'e', 'r', '/', 'r', 'i', 'g', 'h', 't', '/', 'j', 'o', 'y', 's', 't', 'i', 'c', 'k', '/', 'x', (byte)0 } );
        analogs[1][1][1] = getInterface(new byte[] { '/', 'c', 'o', 'n', 't', 'r', 'o', 'l', 'l', 'e', 'r', '/', 'r', 'i', 'g', 'h', 't', '/', 'j', 'o', 'y', 's', 't', 'i', 'c', 'k', '/', 'y', (byte)0 } );
        OsvrClientKitLibrary.osvrRegisterAnalogCallback(analogs[1][1][0], analogHandler, analogs[1][1][0].getPointer());
        OsvrClientKitLibrary.osvrRegisterAnalogCallback(analogs[1][1][1], analogHandler, analogs[1][1][1].getPointer());
        
        return true;
    }

    @Override
    public int getTrackedControllerCount() {
        return (isHandTracked[0]?1:0) + (isHandTracked[1]?1:0);
    }

    @Override
    public void updateConnectedControllers() {
        
    }

    @Override
    public void updateControllerStates() {
        for(int i=0;i<hands.length;i++) {
            isHandTracked[i] = OsvrInterfaceLibrary.osvrGetPoseState(hands[i], tv, handState[i]) == 0;
        }
    }

    @Override
    public Object getRawControllerState(int index) {
        return handState[index];
    }

    //@Override
    //public Matrix4f getPoseForInputDevice(int index) {
    //    return handState[i].
    //}

    @Override
    public boolean isInputFocused() {
        return true;
    }

    @Override
    public boolean isInputDeviceTracking(int index) {
        return isHandTracked[index];
    }

    @Override
    public Quaternion getOrientation(int index) {
        tempq.set((float)-handState[index].rotation.data[1],
                  (float)handState[index].rotation.data[2],
                  (float)-handState[index].rotation.data[3],
                  (float)handState[index].rotation.data[0]);
        return tempq;
    }

    @Override
    public Vector3f getPosition(int index) {
        tempv.x = (float)-handState[index].translation.data[0];
        tempv.y = (float) handState[index].translation.data[1];
        tempv.z = (float)-handState[index].translation.data[2];
        return tempv;
    }

    @Override
    public Quaternion getFinalObserverRotation(int index) {
    	VRViewManagerOSVR vrvm = (VRViewManagerOSVR)environment.getVRViewManager();
        if( vrvm == null || isInputDeviceTracking(index) == false ) return null;
        Object obs = environment.getObserver();
        if( obs instanceof Camera ) {
            tempq.set(((Camera)obs).getRotation());
        } else {
            tempq.set(((Spatial)obs).getWorldRotation());
        }
        return tempq.multLocal(getOrientation(index));
    }
    
    @Override
    public Vector3f getFinalObserverPosition(int index) {
    	VRViewManagerOSVR vrvm = (VRViewManagerOSVR) environment.getVRViewManager();
        if( vrvm == null || isInputDeviceTracking(index) == false ) return null;
        Object obs = environment.getObserver();
        Vector3f pos = getPosition(index);
        if( obs instanceof Camera ) {
            ((Camera)obs).getRotation().mult(pos, pos);
            return pos.addLocal(((Camera)obs).getLocation());
        } else {
            ((Spatial)obs).getWorldRotation().mult(pos, pos);
            return pos.addLocal(((Spatial)obs).getWorldTranslation());
        }
    } 

    @Override
    public void triggerHapticPulse(int controllerIndex, float seconds) {
        
    }

    @Override
    public void swapHands() {
        // not supported yet
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
	public VRTrackedController getTrackedController(int index) {
		// TODO Auto-generated method stub
		return null;
	}
    
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.input.vr;

import com.jme3.app.VREnvironment;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.system.jopenvr.HmdMatrix34_t;
import com.jme3.system.jopenvr.HmdMatrix44_t;
import com.jme3.system.jopenvr.JOpenVRLibrary;
import com.jme3.system.jopenvr.OpenVRUtil;
import com.jme3.system.jopenvr.TrackedDevicePose_t;
import com.jme3.system.jopenvr.VR_IVRCompositor_FnTable;
import com.jme3.system.jopenvr.VR_IVRSystem_FnTable;
import com.jme3.util.VRUtil;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.FloatByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;

import java.nio.IntBuffer;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class that wraps an <a href="https://github.com/ValveSoftware/openvr/wiki/API-Documentation">OpenVR</a> system. 
 * @author reden - phr00t - https://github.com/phr00t
 * @author Julien Seinturier - (c) 2016 - JOrigin project - <a href="http://www.jorigin.org">http:/www.jorigin.org</a>
 */
public class OpenVR implements VRAPI {
    
	private static final Logger logger = Logger.getLogger(OpenVR.class.getName());
	
    private static VR_IVRCompositor_FnTable compositorFunctions;
    private static VR_IVRSystem_FnTable vrsystemFunctions;
    
    private static boolean initSuccess = false;
    private static boolean flipEyes    = false;
    
    private static IntBuffer hmdDisplayFrequency;
    private static TrackedDevicePose_t.ByReference hmdTrackedDevicePoseReference;
    protected static TrackedDevicePose_t[] hmdTrackedDevicePoses;
    
    protected static IntByReference hmdErrorStore;
    
    private static final Quaternion rotStore = new Quaternion();
    private static final Vector3f posStore = new Vector3f();
    
    private static FloatByReference tlastVsync;
    
    /**
     * The actual frame count.
     */
    public static LongByReference _tframeCount;
    
    // for debugging latency
    private int frames = 0;    
    
    protected static Matrix4f[] poseMatrices;
    
    private static final Matrix4f hmdPose = Matrix4f.IDENTITY.clone();
    private static Matrix4f hmdProjectionLeftEye;
    private static Matrix4f hmdProjectionRightEye;
    private static Matrix4f hmdPoseLeftEye;
    private static Matrix4f hmdPoseRightEye;
    
    private static Vector3f hmdPoseLeftEyeVec, hmdPoseRightEyeVec, hmdSeatToStand;
    
    private static float vsyncToPhotons;
    private static double timePerFrame, frameCountRun;
    private static long frameCount;
    private static OpenVRInput VRinput;
    
    private VREnvironment environment = null;
    
    /**
     * Create a new <a href="https://github.com/ValveSoftware/openvr/wiki/API-Documentation">OpenVR</a> system 
     * attached to the given {@link VREnvironment VR environment}.
     * @param environment the VR environment to which this API is attached.
     */
    public OpenVR(VREnvironment environment){
      this.environment = environment;
    }
    
    @Override
    public OpenVRInput getVRinput() {
        return VRinput;
    }
    
    @Override
    public VR_IVRSystem_FnTable getVRSystem() {
        return vrsystemFunctions;
    }
    
    @Override
    public VR_IVRCompositor_FnTable getCompositor() {
        return compositorFunctions;
    }
    
    @Override
    public String getName() {
        return "OpenVR";
    }
    
    private static long latencyWaitTime = 0;
    
    @Override
    public void setFlipEyes(boolean set) {
        flipEyes = set;
    }
    
    private boolean enableDebugLatency = false;
    
    @Override
    public void printLatencyInfoToConsole(boolean set) {
        enableDebugLatency = set;
    }

    @Override
    public int getDisplayFrequency() {
        if( hmdDisplayFrequency == null ) return 0;
        return hmdDisplayFrequency.get(0);
    }
    
    @Override
    public boolean initialize() {
    	
    	logger.config("Initializing OpenVR system...");
    	
        hmdErrorStore = new IntByReference();
        vrsystemFunctions = null;
        JOpenVRLibrary.VR_InitInternal(hmdErrorStore, JOpenVRLibrary.EVRApplicationType.EVRApplicationType_VRApplication_Scene);
        if( hmdErrorStore.getValue() == 0 ) {
            vrsystemFunctions = new VR_IVRSystem_FnTable(JOpenVRLibrary.VR_GetGenericInterface(JOpenVRLibrary.IVRSystem_Version, hmdErrorStore).getPointer());
        }
        
        if( vrsystemFunctions == null || hmdErrorStore.getValue() != 0 ) {
            logger.severe("OpenVR Initialize Result: " + JOpenVRLibrary.VR_GetVRInitErrorAsEnglishDescription(hmdErrorStore.getValue()).getString(0));
            logger.severe("Initializing OpenVR system [FAILED]");
            return false;
        } else {
            logger.config("OpenVR initialized & VR connected.");
            
            vrsystemFunctions.setAutoSynch(false);
            vrsystemFunctions.read();
            
            tlastVsync = new FloatByReference();
            _tframeCount = new LongByReference();
            
            hmdDisplayFrequency = IntBuffer.allocate(1);
            hmdDisplayFrequency.put( (int) JOpenVRLibrary.ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_DisplayFrequency_Float);
            hmdTrackedDevicePoseReference = new TrackedDevicePose_t.ByReference();
            hmdTrackedDevicePoses = (TrackedDevicePose_t[])hmdTrackedDevicePoseReference.toArray(JOpenVRLibrary.k_unMaxTrackedDeviceCount);
            poseMatrices = new Matrix4f[JOpenVRLibrary.k_unMaxTrackedDeviceCount];
            for(int i=0;i<poseMatrices.length;i++) poseMatrices[i] = new Matrix4f();

            timePerFrame = 1.0 / hmdDisplayFrequency.get(0);
            
            // disable all this stuff which kills performance
            hmdTrackedDevicePoseReference.setAutoRead(false);
            hmdTrackedDevicePoseReference.setAutoWrite(false);
            hmdTrackedDevicePoseReference.setAutoSynch(false);
            for(int i=0;i<JOpenVRLibrary.k_unMaxTrackedDeviceCount;i++) {
                hmdTrackedDevicePoses[i].setAutoRead(false);
                hmdTrackedDevicePoses[i].setAutoWrite(false);
                hmdTrackedDevicePoses[i].setAutoSynch(false);
            }
            
            // init controllers for the first time
            VRinput = new OpenVRInput(environment);
            VRinput.init();
            VRinput.updateConnectedControllers();
            
            // init bounds & chaperone info
            VRBounds.init();
            
            logger.config("Initializing OpenVR system [SUCCESS]");
            initSuccess = true;
            return true;
        }
    }
    
    @Override
    public boolean initVRCompositor(boolean allowed) {
        hmdErrorStore.setValue(0); // clear the error store
        if( allowed && vrsystemFunctions != null ) {
        	
        	IntByReference intptr = JOpenVRLibrary.VR_GetGenericInterface(JOpenVRLibrary.IVRCompositor_Version, hmdErrorStore);
        	if (intptr != null){
        	
        		if (intptr.getPointer() != null){
            		compositorFunctions = new VR_IVRCompositor_FnTable(intptr.getPointer());
                    if(compositorFunctions != null && hmdErrorStore.getValue() == 0 ){          
                        compositorFunctions.setAutoSynch(false);
                        compositorFunctions.read();
                        if( environment.isSeatedExperience() ) {                    
                            compositorFunctions.SetTrackingSpace.apply(JOpenVRLibrary.ETrackingUniverseOrigin.ETrackingUniverseOrigin_TrackingUniverseSeated);
                        } else {
                            compositorFunctions.SetTrackingSpace.apply(JOpenVRLibrary.ETrackingUniverseOrigin.ETrackingUniverseOrigin_TrackingUniverseStanding);                
                        }
                        logger.config("OpenVR Compositor initialized");
                    } else {
                        logger.severe("OpenVR Compositor error: " + hmdErrorStore.getValue());
                        compositorFunctions = null;
                    }
        		} else {
        			logger.log(Level.SEVERE, "Cannot get valid pointer for generic interface \""+JOpenVRLibrary.IVRCompositor_Version+"\", "+OpenVRUtil.getEVRInitErrorString(hmdErrorStore.getValue())+" ("+hmdErrorStore.getValue()+")");
        			compositorFunctions = null;
        		}

        	} else {
        		logger.log(Level.SEVERE, "Cannot get generic interface for \""+JOpenVRLibrary.IVRCompositor_Version+"\", "+OpenVRUtil.getEVRInitErrorString(hmdErrorStore.getValue())+" ("+hmdErrorStore.getValue()+")");
        		compositorFunctions = null;
        	}
        	
            
        }
        if( compositorFunctions == null ) {
            logger.severe("Skipping VR Compositor...");
            if( vrsystemFunctions != null ) {
                vsyncToPhotons = vrsystemFunctions.GetFloatTrackedDeviceProperty.apply(JOpenVRLibrary.k_unTrackedDeviceIndex_Hmd, JOpenVRLibrary.ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_SecondsFromVsyncToPhotons_Float, hmdErrorStore);
            } else {
                vsyncToPhotons = 0f;
            }
        }
        return compositorFunctions != null;
    }

    @Override
    public void destroy() {
        JOpenVRLibrary.VR_ShutdownInternal();
    }

    @Override
    public boolean isInitialized() {
        return initSuccess;
    }

    @Override
    public void reset() {
        if( vrsystemFunctions == null ) return;
        vrsystemFunctions.ResetSeatedZeroPose.apply();
        hmdSeatToStand = null;
    }

    @Override
    public void getRenderSize(Vector2f store) {
        if( vrsystemFunctions == null ) {
            // 1344x1512
            store.x = 1344f;
            store.y = 1512f;
        } else {
            IntByReference x = new IntByReference();
            IntByReference y = new IntByReference();
            vrsystemFunctions.GetRecommendedRenderTargetSize.apply(x, y);
            store.x = x.getValue();
            store.y = y.getValue();
        }
    }
    /*
    @Override
    public float getFOV(int dir) {
        float val = 0f;
        if( vrsystemFunctions != null ) {      
            val = vrsystemFunctions.GetFloatTrackedDeviceProperty.apply(JOpenVRLibrary.k_unTrackedDeviceIndex_Hmd, dir, hmdErrorStore);
        }
        // verification of number
        if( val == 0f ) {
            return 55f;
        } else if( val <= 10f ) {
            // most likely a radian number
            return val * 57.2957795f;
        }
        return val;
    }
    */
    
    @Override
    public float getInterpupillaryDistance() {
        if( vrsystemFunctions == null ) return 0.065f;
        return vrsystemFunctions.GetFloatTrackedDeviceProperty.apply(JOpenVRLibrary.k_unTrackedDeviceIndex_Hmd, JOpenVRLibrary.ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_UserIpdMeters_Float, hmdErrorStore);
    }
    
    @Override
    public Quaternion getOrientation() {
        VRUtil.convertMatrix4toQuat(hmdPose, rotStore);
        return rotStore;
    }

    @Override
    public Vector3f getPosition() {
        // the hmdPose comes in rotated funny, fix that here
        hmdPose.toTranslationVector(posStore);
        posStore.x = -posStore.x;
        posStore.z = -posStore.z;
        return posStore;
    }
    
    @Override
    public void getPositionAndOrientation(Vector3f storePos, Quaternion storeRot) {
        hmdPose.toTranslationVector(storePos);
        storePos.x = -storePos.x;
        storePos.z = -storePos.z;
        storeRot.set(getOrientation());
    }    
    
    @Override
    public void updatePose(){
        if(vrsystemFunctions == null) return;
        if(compositorFunctions != null) {
           compositorFunctions.WaitGetPoses.apply(hmdTrackedDevicePoseReference, JOpenVRLibrary.k_unMaxTrackedDeviceCount, null, 0);
        } else {
            // wait
            if( latencyWaitTime > 0 ) VRUtil.sleepNanos(latencyWaitTime);
                        
            vrsystemFunctions.GetTimeSinceLastVsync.apply(tlastVsync, _tframeCount);
            float fSecondsUntilPhotons = (float)timePerFrame - tlastVsync.getValue() + vsyncToPhotons;
            
            if( enableDebugLatency ) {
                if( frames == 10 ) {
                    System.out.println("Waited (nanos): " + Long.toString(latencyWaitTime));
                    System.out.println("Predict ahead time: " + Float.toString(fSecondsUntilPhotons));
                }
                frames = (frames + 1) % 60;            
            }            
            
            // handle skipping frame stuff
            long nowCount = _tframeCount.getValue();
            if( nowCount - frameCount > 1 ) {
                // skipped a frame!
                if( enableDebugLatency ) System.out.println("Frame skipped!");
                frameCountRun = 0;
                if( latencyWaitTime > 0 ) {
                    latencyWaitTime -= TimeUnit.MILLISECONDS.toNanos(1);
                    if( latencyWaitTime < 0 ) latencyWaitTime = 0;
                }
            } else if( latencyWaitTime < timePerFrame * 1000000000.0 ) {
                // didn't skip a frame, lets try waiting longer to improve latency
                frameCountRun++;
                latencyWaitTime += Math.round(Math.pow(frameCountRun / 10.0, 2.0));
            }

            frameCount = nowCount;
            
            vrsystemFunctions.GetDeviceToAbsoluteTrackingPose.apply(
                    environment.isSeatedExperience()?JOpenVRLibrary.ETrackingUniverseOrigin.ETrackingUniverseOrigin_TrackingUniverseSeated:
                                                       JOpenVRLibrary.ETrackingUniverseOrigin.ETrackingUniverseOrigin_TrackingUniverseStanding,
                    fSecondsUntilPhotons, hmdTrackedDevicePoseReference, JOpenVRLibrary.k_unMaxTrackedDeviceCount);   
        }
        
        // deal with controllers being plugged in and out
        // causing an invalid memory crash... skipping for now
        /*boolean hasEvent = false;
        while( JOpenVRLibrary.VR_IVRSystem_PollNextEvent(OpenVR.getVRSystemInstance(), tempEvent) != 0 ) {
            // wait until the events are clear..
            hasEvent = true;
        }
        if( hasEvent ) {
            // an event probably changed controller state
            VRInput._updateConnectedControllers();
        }*/
        //update controllers pose information
        environment.getVRinput().updateControllerStates();
                
        // read pose data from native
        for (int nDevice = 0; nDevice < JOpenVRLibrary.k_unMaxTrackedDeviceCount; ++nDevice ){
            hmdTrackedDevicePoses[nDevice].readField("bPoseIsValid");
            if( hmdTrackedDevicePoses[nDevice].bPoseIsValid != 0 ){
                hmdTrackedDevicePoses[nDevice].readField("mDeviceToAbsoluteTracking");
                VRUtil.convertSteamVRMatrix3ToMatrix4f(hmdTrackedDevicePoses[nDevice].mDeviceToAbsoluteTracking, poseMatrices[nDevice]);
            }            
        }
        
        if ( hmdTrackedDevicePoses[JOpenVRLibrary.k_unTrackedDeviceIndex_Hmd].bPoseIsValid != 0 ){
            hmdPose.set(poseMatrices[JOpenVRLibrary.k_unTrackedDeviceIndex_Hmd]);
        } else {
            hmdPose.set(Matrix4f.IDENTITY);
        }
    }

    @Override
    public Matrix4f getHMDMatrixProjectionLeftEye(Camera cam){
        if( hmdProjectionLeftEye != null ) {
            return hmdProjectionLeftEye;
        } else if(vrsystemFunctions == null){
            return cam.getProjectionMatrix();
        } else {
            HmdMatrix44_t mat = vrsystemFunctions.GetProjectionMatrix.apply(JOpenVRLibrary.EVREye.EVREye_Eye_Left, cam.getFrustumNear(), cam.getFrustumFar());
            hmdProjectionLeftEye = new Matrix4f();
            VRUtil.convertSteamVRMatrix4ToMatrix4f(mat, hmdProjectionLeftEye);
            return hmdProjectionLeftEye;
        }
    }
        
    @Override
    public Matrix4f getHMDMatrixProjectionRightEye(Camera cam){
        if( hmdProjectionRightEye != null ) {
            return hmdProjectionRightEye;
        } else if(vrsystemFunctions == null){
            return cam.getProjectionMatrix();
        } else {
            HmdMatrix44_t mat = vrsystemFunctions.GetProjectionMatrix.apply(JOpenVRLibrary.EVREye.EVREye_Eye_Right, cam.getFrustumNear(), cam.getFrustumFar());
            hmdProjectionRightEye = new Matrix4f();
            VRUtil.convertSteamVRMatrix4ToMatrix4f(mat, hmdProjectionRightEye);
            return hmdProjectionRightEye;
        }
    }
    
    @Override
    public Vector3f getHMDVectorPoseLeftEye() {
        if( hmdPoseLeftEyeVec == null ) {
            hmdPoseLeftEyeVec = getHMDMatrixPoseLeftEye().toTranslationVector();
            // set default IPD if none or broken
            if( hmdPoseLeftEyeVec.x <= 0.080f * -0.5f || hmdPoseLeftEyeVec.x >= 0.040f * -0.5f ) {
                hmdPoseLeftEyeVec.x = 0.065f * -0.5f;
            }
            if( flipEyes == false ) hmdPoseLeftEyeVec.x *= -1f; // it seems these need flipping
        }
        return hmdPoseLeftEyeVec;
    }
    
    @Override
    public Vector3f getHMDVectorPoseRightEye() {
        if( hmdPoseRightEyeVec == null ) {
            hmdPoseRightEyeVec = getHMDMatrixPoseRightEye().toTranslationVector();
            // set default IPD if none or broken
            if( hmdPoseRightEyeVec.x >= 0.080f * 0.5f || hmdPoseRightEyeVec.x <= 0.040f * 0.5f ) {
                hmdPoseRightEyeVec.x = 0.065f * 0.5f;
            }
            if( flipEyes == false ) hmdPoseRightEyeVec.x *= -1f; // it seems these need flipping
        }
        return hmdPoseRightEyeVec;
    }
    
    @Override
    public Vector3f getSeatedToAbsolutePosition() {
        if( environment.isSeatedExperience() == false ) return Vector3f.ZERO;
        if( hmdSeatToStand == null ) {
            hmdSeatToStand = new Vector3f();
            HmdMatrix34_t mat = vrsystemFunctions.GetSeatedZeroPoseToStandingAbsoluteTrackingPose.apply();
            Matrix4f tempmat = new Matrix4f();
            VRUtil.convertSteamVRMatrix3ToMatrix4f(mat, tempmat);
            tempmat.toTranslationVector(hmdSeatToStand);
        }
        return hmdSeatToStand;
    }
    
    @Override
    public Matrix4f getHMDMatrixPoseLeftEye(){
        if( hmdPoseLeftEye != null ) {
            return hmdPoseLeftEye;
        } else if(vrsystemFunctions == null) {
            return Matrix4f.IDENTITY;
        } else {
            HmdMatrix34_t mat = vrsystemFunctions.GetEyeToHeadTransform.apply(JOpenVRLibrary.EVREye.EVREye_Eye_Left);
            hmdPoseLeftEye = new Matrix4f();
            return VRUtil.convertSteamVRMatrix3ToMatrix4f(mat, hmdPoseLeftEye);
        }
    }
    
    @Override
    public HmdType getType() {
        if( vrsystemFunctions != null ) {      
            Pointer str1 = new Memory(128);
            Pointer str2 = new Memory(128);
            String completeName = "";
            vrsystemFunctions.GetStringTrackedDeviceProperty.apply(JOpenVRLibrary.k_unTrackedDeviceIndex_Hmd,
                                                                   JOpenVRLibrary.ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_ManufacturerName_String,
                                                                   str1, 128, hmdErrorStore);
            if( hmdErrorStore.getValue() == 0 ) completeName += str1.getString(0);
            vrsystemFunctions.GetStringTrackedDeviceProperty.apply(JOpenVRLibrary.k_unTrackedDeviceIndex_Hmd,
                                                                   JOpenVRLibrary.ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_ModelNumber_String,
                                                                   str2, 128, hmdErrorStore);
            if( hmdErrorStore.getValue() == 0 ) completeName += " " + str2.getString(0);
            if( completeName.length() > 0 ) {
                completeName = completeName.toLowerCase(Locale.ENGLISH).trim();
                if( completeName.contains("htc") || completeName.contains("vive") ) {
                    return HmdType.HTC_VIVE;
                } else if( completeName.contains("osvr") ) {
                    return HmdType.OSVR;
                } else if( completeName.contains("oculus") || completeName.contains("rift") ||
                           completeName.contains("dk1") || completeName.contains("dk2") || completeName.contains("cv1") ) {
                    return HmdType.OCULUS_RIFT;
                } else if( completeName.contains("fove") ) {
                    return HmdType.FOVE;
                } else if( completeName.contains("game") && completeName.contains("face") ) {
                    return HmdType.GAMEFACE;
                } else if( completeName.contains("morpheus") ) {
                    return HmdType.MORPHEUS;
                } else if( completeName.contains("gear") ) {
                    return HmdType.GEARVR;
                } else if( completeName.contains("star") ) {
                    return HmdType.STARVR;
                } else if( completeName.contains("null") ) {
                    return HmdType.NULL;
                }
            }
        } else return HmdType.NONE;
        return HmdType.OTHER;
    }
    
    @Override
    public Matrix4f getHMDMatrixPoseRightEye(){
        if( hmdPoseRightEye != null ) {
            return hmdPoseRightEye;
        } else if(vrsystemFunctions == null) {
            return Matrix4f.IDENTITY;
        } else {
            HmdMatrix34_t mat = vrsystemFunctions.GetEyeToHeadTransform.apply(JOpenVRLibrary.EVREye.EVREye_Eye_Right);
            hmdPoseRightEye = new Matrix4f();
            return VRUtil.convertSteamVRMatrix3ToMatrix4f(mat, hmdPoseRightEye);
        }
    }
  
}

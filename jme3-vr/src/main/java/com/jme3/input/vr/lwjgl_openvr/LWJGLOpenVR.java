/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.input.vr.lwjgl_openvr;

import com.jme3.app.VREnvironment;
import com.jme3.input.vr.HmdType;
import com.jme3.input.vr.VRAPI;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.util.VRUtil;

import java.nio.IntBuffer;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.openvr.HmdMatrix34;
import org.lwjgl.openvr.HmdMatrix44;
import org.lwjgl.openvr.TrackedDevicePose;
import org.lwjgl.openvr.VR;
import org.lwjgl.openvr.VRCompositor;
import org.lwjgl.openvr.VRSystem;

/**
 * A class that wraps an <a href="https://github.com/ValveSoftware/openvr/wiki/API-Documentation">OpenVR</a> system. 
 * @author reden - phr00t
 * @author Julien Seinturier - COMEX SA - <a href="http://www.seinturier.fr">http://www.seinturier.fr</a>
 * @author Rickard Edén
 */
public class LWJGLOpenVR implements VRAPI {
    
    private static final Logger logger = Logger.getLogger(LWJGLOpenVR.class.getName());
	
    private static boolean initSuccess = false;
    private static boolean flipEyes    = false;
    
    private IntBuffer hmdDisplayFrequency;
    private TrackedDevicePose.Buffer trackedDevicePose;
    protected TrackedDevicePose[] hmdTrackedDevicePoses;
    
    protected IntBuffer hmdErrorStore = BufferUtils.createIntBuffer(1);
    
    private final Quaternion rotStore = new Quaternion();
    private final Vector3f posStore = new Vector3f();
    
    // for debugging latency
    private int frames = 0;    
    
    protected Matrix4f[] poseMatrices;

    private final Matrix4f hmdPose = Matrix4f.IDENTITY.clone();
    private Matrix4f hmdProjectionLeftEye;
    private Matrix4f hmdProjectionRightEye;
    private Matrix4f hmdPoseLeftEye;
    private Matrix4f hmdPoseRightEye;
    
    private Vector3f hmdPoseLeftEyeVec, hmdPoseRightEyeVec, hmdSeatToStand;
    
    private float vsyncToPhotons;
    private double timePerFrame, frameCountRun;
    private long frameCount;
    private LWJGLOpenVRInput VRinput;
    
    
    private VREnvironment environment = null;
    
    
    /**
     * Convert specific OpenVR {@link org.lwjgl.openvr.HmdMatrix34 HmdMatrix34} into JME {@link Matrix4f Matrix4f}
     * @param hmdMatrix the input matrix
     * @param mat the converted matrix
     * @return the converted matrix
     */
    public static Matrix4f convertSteamVRMatrix3ToMatrix4f(org.lwjgl.openvr.HmdMatrix34 hmdMatrix, Matrix4f mat){
        mat.set(hmdMatrix.m(0), hmdMatrix.m(1), hmdMatrix.m(2), hmdMatrix.m(3), 
                hmdMatrix.m(4), hmdMatrix.m(5), hmdMatrix.m(6), hmdMatrix.m(7), 
                hmdMatrix.m(8), hmdMatrix.m(9), hmdMatrix.m(10), hmdMatrix.m(11), 
                0f, 0f, 0f, 1f);
        return mat;
    }
    
    /**
     * Convert specific OpenVR {@link org.lwjgl.openvr.HmdMatrix34 HmdMatrix34_t} into JME {@link Matrix4f Matrix4f}
     * @param hmdMatrix the input matrix
     * @param mat the converted matrix
     * @return the converted matrix
     */
    public static Matrix4f convertSteamVRMatrix4ToMatrix4f(org.lwjgl.openvr.HmdMatrix44 hmdMatrix, Matrix4f mat){
        mat.set(hmdMatrix.m(0), hmdMatrix.m(1), hmdMatrix.m(2), hmdMatrix.m(3), 
                hmdMatrix.m(4), hmdMatrix.m(5), hmdMatrix.m(6), hmdMatrix.m(7),
                hmdMatrix.m(8), hmdMatrix.m(9), hmdMatrix.m(10), hmdMatrix.m(11), 
                hmdMatrix.m(12), hmdMatrix.m(13), hmdMatrix.m(14), hmdMatrix.m(15));
        return mat;
    }
    
    /**
     * Create a new <a href="https://github.com/ValveSoftware/openvr/wiki/API-Documentation">OpenVR</a> system 
     * attached to the given {@link VREnvironment VR environment}.
     * @param environment the VR environment to which this API is attached.
     */
    public LWJGLOpenVR(VREnvironment environment){
      this.environment = environment;
    }
    
    @Override
    public LWJGLOpenVRInput getVRinput() {
        return VRinput;
    }
    
    @Override
    public Object getVRSystem() {
        throw new UnsupportedOperationException("Not yet implemented!");
    }
    
    @Override
    public Object getCompositor() {
        throw new UnsupportedOperationException("Not yet implemented!");
    }
    
    @Override
    public String getName() {
        return "OpenVR/LWJGL";
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
    	
        // Init the native linking to the OpenVR library.
        
        int result = VR.VR_InitInternal(hmdErrorStore, VR.EVRApplicationType_VRApplication_Scene);
        
        if(hmdErrorStore.get(0) != VR.EVRInitError_VRInitError_None) {
            logger.severe("OpenVR Initialize Result: " + VR.VR_GetVRInitErrorAsEnglishDescription(hmdErrorStore.get(0)));
            logger.severe("Initializing OpenVR system [FAILED]");
            return false;
        } else {
            logger.config("OpenVR initialized & VR connected.");
            org.lwjgl.openvr.OpenVR.create(result);
            logger.info("Model Number : " + VRSystem.VRSystem_GetStringTrackedDeviceProperty(
                    VR.k_unTrackedDeviceIndex_Hmd, VR.ETrackedDeviceProperty_Prop_ModelNumber_String, hmdErrorStore));
            logger.info("Serial Number: " + VRSystem.VRSystem_GetStringTrackedDeviceProperty(
                    VR.k_unTrackedDeviceIndex_Hmd, VR.ETrackedDeviceProperty_Prop_SerialNumber_String, hmdErrorStore));

            hmdDisplayFrequency = BufferUtils.createIntBuffer(1);
            hmdDisplayFrequency.put( (int) VR.ETrackedDeviceProperty_Prop_DisplayFrequency_Float);
            
            trackedDevicePose = TrackedDevicePose.create(VR.k_unMaxTrackedDeviceCount);
            hmdTrackedDevicePoses = new TrackedDevicePose[VR.k_unMaxTrackedDeviceCount];
            poseMatrices = new Matrix4f[VR.k_unMaxTrackedDeviceCount];
            for(int i=0;i<poseMatrices.length;i++){
                poseMatrices[i] = new Matrix4f();
                hmdTrackedDevicePoses[i] = trackedDevicePose.get(i);
            }
            timePerFrame = 1.0 / hmdDisplayFrequency.get(0);
            TrackedDevicePose.create(VR.k_unMaxTrackedDeviceCount);
            // init controllers for the first time
            VRinput = new LWJGLOpenVRInput(environment);
            VRinput.init();
            VRinput.updateConnectedControllers();
            
            // init bounds & chaperone info
            LWJGLOpenVRBounds bounds = new LWJGLOpenVRBounds();
//            bounds.init(this);
            environment.setVRBounds(bounds);
            VRCompositor.VRCompositor_SetExplicitTimingMode(VR.EVRCompositorTimingMode_VRCompositorTimingMode_Explicit_ApplicationPerformsPostPresentHandoff);
            logger.info("Initializing OpenVR system [SUCCESS]");
            initSuccess = true;
            return true;
        }
    }
    
    @Override
    public boolean initVRCompositor(boolean allowed) {
        hmdErrorStore.put(0, VR.EVRInitError_VRInitError_None); // clear the error store
        if( allowed) {
            long result = VR.VR_GetGenericInterface(VR.IVRCompositor_Version, hmdErrorStore);
            if (result > 0){
                if(hmdErrorStore.get(0) == VR.EVRInitError_VRInitError_None){
                    setTrackingSpace(environment.isSeatedExperience() );
                    logger.config("OpenVR Compositor initialized");
                } else {
                    logger.severe("OpenVR Compositor error: " + hmdErrorStore.get(0));
                }
            } else {
                logger.log(Level.SEVERE, "Cannot get generic interface for \""+VR.IVRCompositor_Version+"\", "+VR.VR_GetVRInitErrorAsEnglishDescription(hmdErrorStore.get(0))+" ("+hmdErrorStore.get(0)+")");
            }
        }
        return true;
    }

    /**
     * Initialize the headset camera.
     * @param allowed <code>true</code> is the use of the headset camera is allowed and <code>false</code> otherwise.
     * @return token for camera
     */
    public long initCamera(boolean allowed) {
      hmdErrorStore.put(0, VR.EVRInitError_VRInitError_None); // clear the error store
      if( allowed) {
          
        long result = VR.VR_GetGenericInterface(VR.IVRTrackedCamera_Version, hmdErrorStore);
    	  if (result > 0){
    	    if(hmdErrorStore.get(0) == VR.EVRInitError_VRInitError_None ){
    	        logger.config("OpenVR Camera initialized");
    	    }
            return result;
    	  } else {
              logger.severe("Failed to initialize camera");
          }
       }
      return 0;
    }
    
    @Override
    public void destroy() {
        VR.VR_ShutdownInternal();
    }

    @Override
    public boolean isInitialized() {
        return initSuccess;
    }

    @Override
    public void reset() {
        VRSystem.VRSystem_ResetSeatedZeroPose();
        hmdSeatToStand = null;
    }

    @Override
    public void getRenderSize(Vector2f store) {
        IntBuffer w = BufferUtils.createIntBuffer(1);
        IntBuffer h = BufferUtils.createIntBuffer(1);
        VRSystem.VRSystem_GetRecommendedRenderTargetSize(w, h);
        logger.config("Recommended render width : " + w.get(0));
        logger.config("Recommended render height: " + h.get(0));
        store.x = w.get(0);
        store.y = h.get(0);
    }
    
    @Override
    public float getInterpupillaryDistance() {
        throw new UnsupportedOperationException("Not yet implemented!");
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
        int result = VRCompositor.nVRCompositor_WaitGetPoses(trackedDevicePose.address(), trackedDevicePose.remaining(), 0, 0);
        // NPE when calling without a gamePoseArray. Issue filed with lwjgl #418
//        int result = VRCompositor.VRCompositor_WaitGetPoses(trackedDevicePose, null);
        environment.getVRinput().updateControllerStates();
                
        // read pose data from native
        for (int nDevice = 0; nDevice < VR.k_unMaxTrackedDeviceCount; ++nDevice ){
            if( hmdTrackedDevicePoses[nDevice].bPoseIsValid() ){
                convertSteamVRMatrix3ToMatrix4f(hmdTrackedDevicePoses[nDevice].mDeviceToAbsoluteTracking(), poseMatrices[nDevice]);
            }            
        }
        
        if ( hmdTrackedDevicePoses[VR.k_unTrackedDeviceIndex_Hmd].bPoseIsValid()){
            hmdPose.set(poseMatrices[VR.k_unTrackedDeviceIndex_Hmd]);
        } else {
            hmdPose.set(Matrix4f.IDENTITY);
        }
    }

    @Override
    public Matrix4f getHMDMatrixProjectionLeftEye(Camera cam){
        if( hmdProjectionLeftEye != null ) {
            return hmdProjectionLeftEye;
        } else {
            HmdMatrix44 mat = HmdMatrix44.create();
            mat = VRSystem.VRSystem_GetProjectionMatrix(VR.EVREye_Eye_Left, cam.getFrustumNear(), cam.getFrustumFar(), mat);
            hmdProjectionLeftEye = new Matrix4f();
            convertSteamVRMatrix4ToMatrix4f(mat, hmdProjectionLeftEye);
            return hmdProjectionLeftEye;
        }
    }
        
    @Override
    public Matrix4f getHMDMatrixProjectionRightEye(Camera cam){
        if( hmdProjectionRightEye != null ) {
            return hmdProjectionRightEye;
        } else {
            HmdMatrix44 mat = HmdMatrix44.create();
            mat = VRSystem.VRSystem_GetProjectionMatrix(VR.EVREye_Eye_Right, cam.getFrustumNear(), cam.getFrustumFar(), mat);
            hmdProjectionRightEye = new Matrix4f();
            convertSteamVRMatrix4ToMatrix4f(mat, hmdProjectionRightEye);
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
            
            HmdMatrix34 mat = HmdMatrix34.create();
            VRSystem.VRSystem_GetSeatedZeroPoseToStandingAbsoluteTrackingPose(mat);
            Matrix4f tempmat = new Matrix4f();
            convertSteamVRMatrix3ToMatrix4f(mat, tempmat);
            tempmat.toTranslationVector(hmdSeatToStand);
        }
        return hmdSeatToStand;
    }
    
    @Override
    public Matrix4f getHMDMatrixPoseLeftEye(){
        if( hmdPoseLeftEye != null ) {
            return hmdPoseLeftEye;
        } else {
            HmdMatrix34 mat = HmdMatrix34.create();
            VRSystem.VRSystem_GetEyeToHeadTransform(VR.EVREye_Eye_Left, mat);
            hmdPoseLeftEye = new Matrix4f();
            return convertSteamVRMatrix3ToMatrix4f(mat, hmdPoseLeftEye);
        }
    }
    
    
    @Override
    public Matrix4f getHMDMatrixPoseRightEye(){
        if( hmdPoseRightEye != null ) {
            return hmdPoseRightEye;
        } else {
            HmdMatrix34 mat = HmdMatrix34.create();
            VRSystem.VRSystem_GetEyeToHeadTransform(VR.EVREye_Eye_Right, mat);
            hmdPoseRightEye = new Matrix4f();
            return convertSteamVRMatrix3ToMatrix4f(mat, hmdPoseRightEye);
        }
    }
    
    @Override
    public HmdType getType() {
            String completeName = "";
            String name = VRSystem.VRSystem_GetStringTrackedDeviceProperty(VR.k_unTrackedDeviceIndex_Hmd,
                                                                   VR.ETrackedDeviceProperty_Prop_ManufacturerName_String,
                                                                   128, hmdErrorStore);
            if( hmdErrorStore.get(0) == 0 ) completeName += name;
            String number = VRSystem.VRSystem_GetStringTrackedDeviceProperty(VR.k_unTrackedDeviceIndex_Hmd,
                                                                   VR.ETrackedDeviceProperty_Prop_ModelNumber_String,
                                                                   128, hmdErrorStore);
            if( hmdErrorStore.get(0) == 0 ) completeName += " " + number;
            if( completeName.length() > 0 ) {
                completeName = completeName.toLowerCase(Locale.ENGLISH).trim();
                if( completeName.contains("htc") || completeName.contains("vive") ) {
                    return HmdType.HTC_VIVE;
                } else if ( completeName.contains("index") ) {
                    return HmdType.VALVE_INDEX;
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
        return HmdType.OTHER;
    }
    
    public void setTrackingSpace(boolean isSeated){
        if( isSeated) {    
            VRCompositor.VRCompositor_SetTrackingSpace(VR.ETrackingUniverseOrigin_TrackingUniverseSeated);
        } else {
            VRCompositor.VRCompositor_SetTrackingSpace(VR.ETrackingUniverseOrigin_TrackingUniverseStanding);
        }
    }
    
    
    public Matrix4f[] getPoseMatrices() {
        return poseMatrices;
    }
    
}
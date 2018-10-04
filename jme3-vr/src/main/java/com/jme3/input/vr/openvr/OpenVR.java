/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.input.vr.openvr;

import com.jme3.app.VREnvironment;
import com.jme3.input.vr.HmdType;
import com.jme3.input.vr.VRAPI;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
//import com.jme3.util.BufferUtils;
import com.jme3.system.NativeLibraryLoader;
import com.jme3.util.VRUtil;
import org.lwjgl.BufferUtils;
import org.lwjgl.openvr.HmdMatrix34;
import org.lwjgl.openvr.HmdMatrix44;
import org.lwjgl.openvr.TrackedDevicePose;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.lwjgl.openvr.VR.*;
import static org.lwjgl.openvr.VRCompositor.VRCompositor_SetTrackingSpace;
import static org.lwjgl.openvr.VRCompositor.VRCompositor_WaitGetPoses;
import static org.lwjgl.openvr.VRSystem.*;

/**
 * A class that wraps an <a href="https://github.com/ValveSoftware/openvr/wiki/API-Documentation">OpenVR</a> system.
 * @author reden - phr00t - https://github.com/phr00t
 * @author Julien Seinturier - COMEX SA - <a href="http://www.seinturier.fr">http://www.seinturier.fr</a>
 */
public class OpenVR implements VRAPI {

	private static final Logger logger = Logger.getLogger(OpenVR.class.getName());
	private static final boolean DEBUG = false;

//    private static VR_IVRCompositor_FnTable compositorFunctions;
    private static long vrsystemFunctions;
//    private static VR_IVRTrackedCamera_FnTable cameraFunctions;

    private static boolean initSuccess = false;
    private static boolean flipEyes    = false;

    private IntBuffer hmdDisplayFrequency;
    protected TrackedDevicePose.Buffer trackedDeviceRenderPoses;
    protected TrackedDevicePose.Buffer trackedDeviceGamePoses;
//    protected TrackedDevicePose[] trackedDeviceRenderPoses;

    protected IntBuffer hmdErrorStore = BufferUtils.createIntBuffer(1);

    private final Quaternion rotStore = new Quaternion();
    private final Vector3f posStore = new Vector3f();

    private static FloatBuffer tlastVsync;

    /**
     * The actual frame count.
     */
    public static LongBuffer _tframeCount;

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
    private OpenVRInput VRinput;

    private boolean enableDebugLatency = false;

    private VREnvironment environment = null;
    private Object compositorPlaceholder = new Object();
    private Object systemPlaceholder = new Object();

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

//    @Override
//    public VR_IVRSystem_FnTable getVRSystem() {
//        return vrsystemFunctions;
//    }
//
//    @Override
//    public VR_IVRCompositor_FnTable getCompositor() {
//        return compositorFunctions;
//    }

//    public VR_IVRTrackedCamera_FnTable getTrackedCamera(){
//      return cameraFunctions;
//    }

    @Override
    public String getName() {
        return "OpenVR";
    }

    private static long latencyWaitTime = 0;

    @Override
    public void setFlipEyes(boolean set) {
        flipEyes = set;
    }

    @Override
    public Object getVRSystem() {
        // TODO: is this used anywhere?
        return systemPlaceholder;
    }

    @Override
    public Object getCompositor() {
        // Looks like this is our way of saying we have a validp
        // compositor
        return compositorPlaceholder;
    }

    @Override
    public void printLatencyInfoToConsole(boolean set) {
        enableDebugLatency = set;
    }

    @Override
    public int getDisplayFrequency() {
        if( hmdDisplayFrequency == null ) return 0;
        return hmdDisplayFrequency.get(0);
    }

//    private static final java.lang.reflect.Field LIBRARIES;
    static {

    }
    public static String[] getLoadedLibraries(final ClassLoader loader) {
        java.lang.reflect.Field LIBRARIES = null;
        try {
            LIBRARIES = ClassLoader.class.getDeclaredField("loadedLibraryNames");
            LIBRARIES.setAccessible(true);

            final Vector<String> libraries = (Vector<String>) LIBRARIES.get(loader);
            return libraries.toArray(new String[] {});
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException("was unable to get native libraries!");
        }
    }

    @Override
    public boolean initialize() {

    	logger.config("Initializing OpenVR system...");

    	String[] loadedLibraries = getLoadedLibraries(ClassLoader.getSystemClassLoader());
        logger.config("Loaded native libs: " + Arrays.toString(loadedLibraries));

//        if (VR_IsHmdPresent()) { throw new RuntimeException("Error : HMD not detected on the system"); }

        if (!VR_IsRuntimeInstalled()) {
            throw new RuntimeException("Error : OpenVR Runtime not detected on the system");
        }

        // Init the native linking to the OpenVR library.
        VR_InitInternal(hmdErrorStore, EVRApplicationType_VRApplication_Scene);
        int token = VR_GetInitToken();
        org.lwjgl.openvr.OpenVR.create(token);

        if( hmdErrorStore.get(0) == 0 ) {
            vrsystemFunctions = VR_GetGenericInterface(IVRSystem_Version, hmdErrorStore);
            initSuccess = true;
        }

        if( !initSuccess || hmdErrorStore.get(0) != 0 ) {
            logger.severe("OpenVR Initialize Result: " + VR_GetVRInitErrorAsEnglishDescription(hmdErrorStore.get(0)));
            logger.severe("Initializing OpenVR system [FAILED]");
            return false;
        } else {
            logger.config("OpenVR initialized & VR connected.");

            tlastVsync   = BufferUtils.createFloatBuffer(1);
            _tframeCount = BufferUtils.createLongBuffer(1);

            hmdDisplayFrequency = IntBuffer.allocate(1);
            hmdDisplayFrequency.put(ETrackedDeviceProperty_Prop_DisplayFrequency_Float);
            trackedDeviceRenderPoses = new TrackedDevicePose.Buffer(BufferUtils.createByteBuffer(TrackedDevicePose.SIZEOF * k_unMaxTrackedDeviceCount));
            trackedDeviceGamePoses = new TrackedDevicePose.Buffer(BufferUtils.createByteBuffer(TrackedDevicePose.SIZEOF * k_unMaxTrackedDeviceCount));

            poseMatrices = new Matrix4f[k_unMaxTrackedDeviceCount];
            for(int i=0;i<poseMatrices.length;i++) poseMatrices[i] = new Matrix4f();

            timePerFrame = 1.0 / hmdDisplayFrequency.get(0);

            // init controllers for the first time
            VRinput = new OpenVRInput(environment);
            VRinput.init();
            VRinput.updateConnectedControllers();

            // init bounds & chaperone info
            OpenVRBounds bounds = new OpenVRBounds();
            bounds.init(this);
            environment.setVRBounds(bounds);

            logger.config("Initializing OpenVR system [SUCCESS]");
            //initSuccess = true;
            return true;
        }
    }

    @Override
    public boolean initVRCompositor(boolean allowed) {
        resetErrorStore();

        if( environment.isSeatedExperience() ) {
            VRCompositor_SetTrackingSpace(ETrackingUniverseOrigin_TrackingUniverseSeated);
        } else {
            VRCompositor_SetTrackingSpace(ETrackingUniverseOrigin_TrackingUniverseStanding);
        }

        logger.config("OpenVR Compositor initialized");
        return true;
        // if( compositorFunctions == null ) {
        //     logger.severe("Skipping VR Compositor...");
        //     if( vrsystemFunctions != null ) {
        //         vsyncToPhotons = VRSystem_GetFloatTrackedDeviceProperty.apply(JOpenVRLibrary.k_unTrackedDeviceIndex_Hmd, JOpenVRLibrary.ETrackedDeviceProperty.ETrackedDeviceProperty_Prop_SecondsFromVsyncToPhotons_Float, hmdErrorStore);
        //     } else {
        //         vsyncToPhotons = 0f;
        //     }
        // }
        //return compositorFunctions != null;
    }

    private void resetErrorStore() {
        hmdErrorStore.put(0,0); // clear the error store
        hmdErrorStore.flip();
    }

    @Override
    public void destroy() {
        VR_ShutdownInternal();
    }

    @Override
    public boolean isInitialized() {
        return initSuccess;
    }

    @Override
    public void reset() {
        VRSystem_ResetSeatedZeroPose();
        hmdSeatToStand = null;
    }

    @Override
    public void getRenderSize(Vector2f store) {
        if( !initSuccess ) {
            // 1344x1512
            store.x = 1344f;
            store.y = 1512f;
        } else {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer x = stack.ints(1);
                IntBuffer y = stack.ints(1);
                VRSystem_GetRecommendedRenderTargetSize(x, y);
                store.x = x.get(0);
                store.y = y.get(0);
            }
        }
    }
    /*
    @Override
    public float getFOV(int dir) {
        float val = 0f;
        if( vrsystemFunctions != null ) {
            val = VRSystem_GetFloatTrackedDeviceProperty.apply(JOpenVRLibrary.k_unTrackedDeviceIndex_Hmd, dir, hmdErrorStore);
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
        if( !initSuccess ) return 0.065f;
        return VRSystem_GetFloatTrackedDeviceProperty(k_unTrackedDeviceIndex_Hmd, ETrackedDeviceProperty_Prop_UserIpdMeters_Float, hmdErrorStore);
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
        if(!initSuccess) return;

        if(!DEBUG) {
            VRCompositor_WaitGetPoses(trackedDeviceRenderPoses, trackedDeviceGamePoses);
        } else {
            // wait
            if( latencyWaitTime > 0 ) VRUtil.sleepNanos(latencyWaitTime);

            VRSystem_GetTimeSinceLastVsync(tlastVsync,_tframeCount);
            float fSecondsUntilPhotons = (float)timePerFrame - tlastVsync.get(0) + vsyncToPhotons;

            if( enableDebugLatency ) {
                if( frames == 10 ) {
                    System.out.println("Waited (nanos): " + Long.toString(latencyWaitTime));
                    System.out.println("Predict ahead time: " + Float.toString(fSecondsUntilPhotons));
                }
                frames = (frames + 1) % 60;
            }

            // handle skipping frame stuff
            long nowCount = _tframeCount.get(0);
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

            VRSystem_GetDeviceToAbsoluteTrackingPose(
                    environment.isSeatedExperience() ? ETrackingUniverseOrigin_TrackingUniverseSeated:
                                                       ETrackingUniverseOrigin_TrackingUniverseStanding,
                    fSecondsUntilPhotons,
                    trackedDeviceRenderPoses
            );
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
        for (int nDevice = 0; nDevice < k_unMaxTrackedDeviceCount; ++nDevice ){
            trackedDeviceRenderPoses.get(nDevice).bPoseIsValid();
            if( trackedDeviceRenderPoses.get(nDevice).bPoseIsValid()){
                HmdMatrix34 absolute = trackedDeviceRenderPoses.get(nDevice).mDeviceToAbsoluteTracking();
                VRUtil.convertSteamVRMatrix3ToMatrix4f(absolute, poseMatrices[nDevice]);
            }
        }

        if (trackedDeviceRenderPoses.get(k_unTrackedDeviceIndex_Hmd).bPoseIsValid() ){
            hmdPose.set(poseMatrices[k_unTrackedDeviceIndex_Hmd]);
        } else {
            hmdPose.set(Matrix4f.IDENTITY);
        }
    }

    @Override
    public Matrix4f getHMDMatrixProjectionLeftEye(Camera cam) {
        if (hmdProjectionLeftEye != null) {
            return hmdProjectionLeftEye;
        } else if (!initSuccess) {
            return cam.getProjectionMatrix();
        } else {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                HmdMatrix44 mat = VRSystem_GetProjectionMatrix(EVREye_Eye_Left, cam.getFrustumNear(), cam.getFrustumFar(), new HmdMatrix44(stack.malloc(HmdMatrix44.SIZEOF)));
                hmdProjectionLeftEye = new Matrix4f();
                VRUtil.convertSteamVRMatrix4ToMatrix4f(mat, hmdProjectionLeftEye);
                return hmdProjectionLeftEye;
            }
        }
    }

    @Override
    public Matrix4f getHMDMatrixProjectionRightEye(Camera cam){
        if( hmdProjectionRightEye != null ) {
            return hmdProjectionRightEye;
        } else if(!initSuccess){
            return cam.getProjectionMatrix();
        } else {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                HmdMatrix44 mat = VRSystem_GetProjectionMatrix(EVREye_Eye_Right, cam.getFrustumNear(), cam.getFrustumFar(), new HmdMatrix44(stack.malloc(HmdMatrix44.SIZEOF)));
                hmdProjectionRightEye = new Matrix4f();
                VRUtil.convertSteamVRMatrix4ToMatrix4f(mat, hmdProjectionRightEye);
                return hmdProjectionRightEye;
            }
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
            try (MemoryStack stack = MemoryStack.stackPush()) {
                HmdMatrix34 mat = VRSystem_GetSeatedZeroPoseToStandingAbsoluteTrackingPose(new HmdMatrix34(stack.malloc(HmdMatrix34.SIZEOF)));
                Matrix4f tempmat = new Matrix4f();
                VRUtil.convertSteamVRMatrix3ToMatrix4f(mat, tempmat);
                tempmat.toTranslationVector(hmdSeatToStand);
            }
        }
        return hmdSeatToStand;
    }

    @Override
    public Matrix4f getHMDMatrixPoseLeftEye(){
        if( hmdPoseLeftEye != null ) {
            return hmdPoseLeftEye;
        } else if(!initSuccess) {
            return Matrix4f.IDENTITY;
        } else {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                HmdMatrix34 mat = VRSystem_GetEyeToHeadTransform(EVREye_Eye_Left, new HmdMatrix34(stack.malloc(HmdMatrix34.SIZEOF)));
                hmdPoseLeftEye = new Matrix4f();
                return VRUtil.convertSteamVRMatrix3ToMatrix4f(mat, hmdPoseLeftEye);
            }
        }
    }

    @Override
    public Matrix4f getHMDMatrixPoseRightEye(){
        if( hmdPoseRightEye != null ) {
            return hmdPoseRightEye;
        } else if(!initSuccess) {
            return Matrix4f.IDENTITY;
        } else {
            try (MemoryStack stack = MemoryStack.stackPush()) {
              HmdMatrix34 mat = VRSystem_GetEyeToHeadTransform(EVREye_Eye_Right, new HmdMatrix34(stack.malloc(HmdMatrix34.SIZEOF)));
              hmdPoseRightEye = new Matrix4f();
              return VRUtil.convertSteamVRMatrix3ToMatrix4f(mat, hmdPoseRightEye);
            }
        }
    }

    @Override
    public HmdType getType() {
        if (initSuccess) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                ByteBuffer str1 = stack.malloc(k_unMaxPropertyStringSize);
                ByteBuffer str2 = stack.malloc(k_unMaxPropertyStringSize);

                VRSystem_GetStringTrackedDeviceProperty(k_unTrackedDeviceIndex_Hmd,
                        ETrackedDeviceProperty_Prop_ManufacturerName_String,
                        str1,
                        hmdErrorStore);

                String completeName = "";
                if (hmdErrorStore.get(0) == 0) completeName += new String(str1.array());
                VRSystem_GetStringTrackedDeviceProperty(k_unTrackedDeviceIndex_Hmd,
                        ETrackedDeviceProperty_Prop_ModelNumber_String,
                        str2, hmdErrorStore);
                if (hmdErrorStore.get(0) == 0) completeName += " " + new String(str2.array());
                if (completeName.length() > 0) {
                    completeName = completeName.toLowerCase(Locale.ENGLISH).trim();
                    if (completeName.contains("htc") || completeName.contains("vive")) {
                        return HmdType.HTC_VIVE;
                    } else if (completeName.contains("osvr")) {
                        return HmdType.OSVR;
                    } else if (completeName.contains("oculus") || completeName.contains("rift") ||
                            completeName.contains("dk1") || completeName.contains("dk2") || completeName.contains("cv1")) {
                        return HmdType.OCULUS_RIFT;
                    } else if (completeName.contains("fove")) {
                        return HmdType.FOVE;
                    } else if (completeName.contains("game") && completeName.contains("face")) {
                        return HmdType.GAMEFACE;
                    } else if (completeName.contains("morpheus")) {
                        return HmdType.MORPHEUS;
                    } else if (completeName.contains("gear")) {
                        return HmdType.GEARVR;
                    } else if (completeName.contains("star")) {
                        return HmdType.STARVR;
                    } else if (completeName.contains("null")) {
                        return HmdType.NULL;
                    }
                }
            }
            return HmdType.NONE;
        }
        return HmdType.OTHER;
    }

}

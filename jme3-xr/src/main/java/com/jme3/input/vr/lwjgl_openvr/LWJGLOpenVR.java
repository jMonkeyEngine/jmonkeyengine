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
import java.util.logging.Logger;
import org.lwjgl.BufferUtils;

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
//    private TrackedDevicePose.Buffer trackedDevicePose;
//    protected TrackedDevicePose[] hmdTrackedDevicePoses;

    protected IntBuffer hmdErrorStore = BufferUtils.createIntBuffer(1);

    private final Quaternion rotStore = new Quaternion();
    private final Vector3f posStore = new Vector3f();

    protected Matrix4f[] poseMatrices;

    private Matrix4f hmdProjectionLeftEye;
    private Matrix4f hmdProjectionRightEye;
    private Matrix4f hmdPoseLeftEye;
    private Matrix4f hmdPoseRightEye;

    private Vector3f hmdPoseLeftEyeVec, hmdPoseRightEyeVec, hmdSeatToStand;

    private LWJGLOpenVRInput vrInput;

    private VREnvironment environment = null;

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
        return vrInput;
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
        return "OpenXR/LWJGL";
    }

    @Override
    public void setFlipEyes(boolean set) {
        flipEyes = set;
    }

    @Override
    public void printLatencyInfoToConsole(boolean set) {
        // not implemented
    }

    @Override
    public int getDisplayFrequency() {
        if( hmdDisplayFrequency == null ) return 0;
        return hmdDisplayFrequency.get(0);
    }

    @Override
    public boolean initialize() {
        logger.config("Initializing OpenVR system...");

        com.jme3.input.vr.lwjgl_openxr.HelloOpenXRGL mainXr = new com.jme3.input.vr.lwjgl_openxr.HelloOpenXRGL(environment);
        vrInput = new LWJGLOpenVRInput(environment);
        initSuccess = vrInput.init();
        if (initSuccess)
        {
          vrInput.updateConnectedControllers();
          environment.setXR(mainXr);
        }
        return initSuccess;
    }

    @Override
    public boolean initVRCompositor(boolean allowed) {
        return true;
    }

    /**
     * Initialize the headset camera.
     * @param allowed <code>true</code> is the use of the headset camera is allowed and <code>false</code> otherwise.
     * @return token for camera
     */
    public long initCamera(boolean allowed) {
        return 0;
    }

    @Override
    public void destroy() {
    }

    @Override
    public boolean isInitialized() {
        return initSuccess;
    }

    @Override
    public void reset() {
//        VRChaperone.VRChaperone_ResetZeroPose(VR.ETrackingUniverseOrigin_TrackingUniverseSeated);
        hmdSeatToStand = null;
    }

    @Override
    public void getRenderSize(Vector2f store) {
        environment.getXr().getRenderSize(store);
    }

    @Override
    public float getInterpupillaryDistance() {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public Quaternion getOrientation() {
    	environment.getXr().getViewRotation(rotStore);
        return rotStore;
    }

    @Override
    public Vector3f getPosition() {
    	environment.getXr().getViewPosition(posStore);
        return posStore;
    }

    @Override
    public void getPositionAndOrientation(Vector3f storePos, Quaternion storeRot) {
        storePos.set(getPosition());
        storeRot.set(getOrientation());
    }

    @Override
    public void updatePose(){
    }

    @Override
    public Matrix4f getHMDMatrixProjectionLeftEye(Camera cam){
        if( hmdProjectionLeftEye != null ) {
            return hmdProjectionLeftEye;
        } else {
//            HmdMatrix44 mat = HmdMatrix44.create();
//            mat = VRSystem.VRSystem_GetProjectionMatrix(VR.EVREye_Eye_Left, cam.getFrustumNear(), cam.getFrustumFar(), mat);
            hmdProjectionLeftEye = new Matrix4f();
            //TODO
//            convertSteamVRMatrix4ToMatrix4f(mat, hmdProjectionLeftEye);
            return hmdProjectionLeftEye;
        }
    }

    @Override
    public Matrix4f getHMDMatrixProjectionRightEye(Camera cam){
        if( hmdProjectionRightEye != null ) {
            return hmdProjectionRightEye;
        } else {
//            HmdMatrix44 mat = HmdMatrix44.create();
//            mat = VRSystem.VRSystem_GetProjectionMatrix(VR.EVREye_Eye_Right, cam.getFrustumNear(), cam.getFrustumFar(), mat);
            hmdProjectionRightEye = new Matrix4f();
            //TODO
//            convertSteamVRMatrix4ToMatrix4f(mat, hmdProjectionRightEye);
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

            //TODO
        }
        return hmdSeatToStand;
    }

    @Override
    public Matrix4f getHMDMatrixPoseLeftEye(){
        if( hmdPoseLeftEye != null ) {
            return hmdPoseLeftEye;
        } else {
            //HmdMatrix34 mat = HmdMatrix34.create();
            //VRSystem.VRSystem_GetEyeToHeadTransform(VR.EVREye_Eye_Left, mat);
            //TODO
            hmdPoseLeftEye = new Matrix4f();
            //return convertSteamVRMatrix3ToMatrix4f(mat, hmdPoseLeftEye);
            return hmdPoseLeftEye;
        }
    }

    @Override
    public Matrix4f getHMDMatrixPoseRightEye(){
        if( hmdPoseRightEye != null ) {
            return hmdPoseRightEye;
        } else {
            //HmdMatrix34 mat = HmdMatrix34.create();
            //VRSystem.VRSystem_GetEyeToHeadTransform(VR.EVREye_Eye_Right, mat);
            //TODO
            hmdPoseRightEye = new Matrix4f();
            //return convertSteamVRMatrix3ToMatrix4f(mat, hmdPoseRightEye);
            return hmdPoseRightEye;
        }
    }

    @Override
    public HmdType getType() {
        return HmdType.OTHER;
    }

    public void setTrackingSpace(boolean isSeated){
    }


    public Matrix4f[] getPoseMatrices() {
        return poseMatrices;
    }
}

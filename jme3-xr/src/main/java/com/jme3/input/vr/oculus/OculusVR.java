package com.jme3.input.vr.oculus;

import com.jme3.app.VREnvironment;
import com.jme3.input.vr.HmdType;
import com.jme3.input.vr.VRAPI;
import com.jme3.math.*;
import com.jme3.renderer.Camera;
import com.jme3.texture.*;
import org.lwjgl.*;
import org.lwjgl.ovr.*;

import java.nio.IntBuffer;
import java.util.logging.Logger;

import static org.lwjgl.BufferUtils.createPointerBuffer;
import static org.lwjgl.ovr.OVR.*;
import static org.lwjgl.ovr.OVRErrorCode.ovrSuccess;
import static org.lwjgl.ovr.OVRUtil.ovr_Detect;
import static org.lwjgl.system.MemoryUtil.*;

/**
 * Oculus VR (LibOVR 1.3.0) Native support.
 * <p>
 * A few notes about the Oculus coordinate system:
 * <ul>
 * <li>Matrices should be transposed</li>
 * <li>Quaternions should be inverted</li>
 * <li>Vectors should have their X and Z axes flipped, but apparently not Y.</li>
 * </ul>
 *
 * @author Campbell Suter (znix@znix.xyz)
 */
public class OculusVR implements VRAPI {

    private static final Logger LOGGER = Logger.getLogger(OculusVR.class.getName());

    private final VREnvironment environment;
    private boolean initialized;

    /**
     * Pointer to the HMD object
     */
    private long session;

    /**
     * Information about the VR session (should the app quit, is
     * it visible or is the universal menu open, etcetera)
     */
    private OVRSessionStatus sessionStatus;

    /**
     * HMD information, such as product name and manufacturer.
     */
    private OVRHmdDesc hmdDesc;

    /**
     * The horizontal resolution of the HMD
     */
    private int resolutionW;

    /**
     * The vertical resolution of the HMD
     */
    private int resolutionH;

    /**
     * Field-of-view data for each eye (how many degrees from the
     * center can the user see).
     */
    private final OVRFovPort fovPorts[] = new OVRFovPort[2];

    /**
     * Data about each eye to be rendered - in particular, the
     * offset from the center of the HMD to the eye.
     */
    private final OVREyeRenderDesc eyeRenderDesc[] = new OVREyeRenderDesc[2];

    /**
     * Store the projections for each eye, so we don't have to malloc
     * and recalculate them each frame.
     */
    private final OVRMatrix4f[] projections = new OVRMatrix4f[2];

    /**
     * Store the poses for each eye, relative to the HMD.
     *
     * @see #getHMDMatrixPoseLeftEye()
     */
    private final Matrix4f[] hmdRelativeEyePoses = new Matrix4f[2];

    /**
     * Store the positions for each eye, relative to the HMD.
     *
     * @see #getHMDVectorPoseLeftEye()
     */
    private final Vector3f[] hmdRelativeEyePositions = new Vector3f[2];

    /**
     * The current state of the tracked components (HMD, touch)
     */
    private OVRTrackingState trackingState;

    /**
     * The position and orientation of the user's head.
     */
    private OVRPosef headPose;

    /**
     * The state of the Touch controllers.
     */
    private OculusVRInput input;

    // The size of the texture drawn onto the HMD
    private int textureW;
    private int textureH;

    // Layers to render into
    private PointerBuffer layers;
    private OVRLayerEyeFov layer0;

    /**
     * Chain texture set thing.
     */
    private long chains[];

    /**
     * Frame buffers we can draw into.
     */
    private FrameBuffer framebuffers[][];

    public OculusVR(VREnvironment environment) {
        this.environment = environment;
    }

    @Override
    public OculusVRInput getVRinput() {
        return input;
    }

    @Override
    public String getName() {
        return "OVR";
    }

    @Override
    public int getDisplayFrequency() {
        // TODO find correct frequency. I'm not sure
        // if LibOVR has a way to do that, though.
        return 60;
    }

    @Override
    public boolean initialize() {
        // Check to make sure the HMD is connected
        OVRDetectResult detect = OVRDetectResult.calloc();
        ovr_Detect(0, detect);
        boolean connected = detect.IsOculusHMDConnected();
        LOGGER.config("OVRDetectResult.IsOculusHMDConnected = " + connected);
        LOGGER.config("OVRDetectResult.IsOculusServiceRunning = " + detect.IsOculusServiceRunning());
        detect.free();

        if (!connected) {
            LOGGER.info("Oculus Rift not connected");
            return false;
        }

        initialized = true;

        // Set up the HMD
        OVRLogCallback callback = new OVRLogCallback() {
            @Override
            public void invoke(long userData, int level, long message) {
                LOGGER.fine("LibOVR [" + userData + "] [" + level + "] " + memASCII(message));
            }
        };
        OVRInitParams initParams = OVRInitParams.calloc();
        initParams.LogCallback(callback);
        if (ovr_Initialize(initParams) != ovrSuccess) {
            LOGGER.severe("LibOVR Init Failed");
            return false; // TODO fix memory leak - destroy() is not called
        }
        LOGGER.config("LibOVR Version " + ovr_GetVersionString());
        initParams.free();

        // Get access to the HMD
        LOGGER.info("Initialize HMD Session");
        PointerBuffer pHmd = memAllocPointer(1);
        OVRGraphicsLuid luid = OVRGraphicsLuid.calloc();
        if (ovr_Create(pHmd, luid) != ovrSuccess) {
            LOGGER.severe("Failed to create HMD");
            return false; // TODO fix memory leak - destroy() is not called
        }
        session = pHmd.get(0);
        memFree(pHmd);
        luid.free();
        sessionStatus = OVRSessionStatus.calloc();

        // Get the information about the HMD
        LOGGER.fine("Get HMD properties");
        hmdDesc = OVRHmdDesc.malloc();
        ovr_GetHmdDesc(session, hmdDesc);
        if (hmdDesc.Type() == ovrHmd_None) {
            LOGGER.warning("No HMD connected");
            return false; // TODO fix memory leak - destroy() is not called
        }

        resolutionW = hmdDesc.Resolution().w();
        resolutionH = hmdDesc.Resolution().h();

        LOGGER.config("HMD Properties: "
                + "\t Manufacturer: " + hmdDesc.ManufacturerString()
                + "\t Product: " + hmdDesc.ProductNameString()
                + "\t Serial: <hidden>" // + hmdDesc.SerialNumberString() // Hidden for privacy reasons
                + "\t Type: " + hmdDesc.Type()
                + "\t Resolution (total): " + resolutionW + "," + resolutionH);

        if (resolutionW == 0) {
            LOGGER.severe("HMD width=0 : aborting");
            return false; // TODO fix memory leak - destroy() is not called
        }

        // Find the FOV for each eye
        for (int eye = 0; eye < 2; eye++) {
            fovPorts[eye] = hmdDesc.DefaultEyeFov(eye);
        }

        // Get the pose for each eye, and cache it for later.
        for (int eye = 0; eye < 2; eye++) {
            // Create the projection objects
            projections[eye] = OVRMatrix4f.malloc();
            hmdRelativeEyePoses[eye] = new Matrix4f();
            hmdRelativeEyePositions[eye] = new Vector3f();

            // Find the eye render information - we use this in the
            // view manager for giving LibOVR its timewarp information.
            eyeRenderDesc[eye] = OVREyeRenderDesc.malloc();
            ovr_GetRenderDesc(session, eye, fovPorts[eye], eyeRenderDesc[eye]);

            // Get the pose of the eye
            OVRPosef pose = eyeRenderDesc[eye].HmdToEyePose();

            // Get the position and rotation of the eye
            vecO2J(pose.Position(), hmdRelativeEyePositions[eye]);
            Quaternion rotation = quatO2J(pose.Orientation(), new Quaternion());

            // Put it into a matrix for the get eye pose functions
            hmdRelativeEyePoses[eye].loadIdentity();
            hmdRelativeEyePoses[eye].setTranslation(hmdRelativeEyePositions[eye]);
            hmdRelativeEyePoses[eye].setRotationQuaternion(rotation);
        }

        // Recenter the HMD. The game itself should do this too, but just in case / before they do.
        reset();

        // Do this so others relying on our texture size (the GUI in particular) get it correct.
        findHMDTextureSize();

        // Allocate the memory for the tracking state - we actually
        // set it up later, but Input uses it so calloc it now.
        trackingState = OVRTrackingState.calloc();

        // Set up the input
        input = new OculusVRInput(this, session, sessionStatus, trackingState);

        // TODO find some way to get in ovrTrackingOrigin_FloorLevel

        // throw new UnsupportedOperationException("Not yet implemented!");
        return true;
    }

    @Override
    public void updatePose() {
        double ftiming = ovr_GetPredictedDisplayTime(session, 0);
        ovr_GetTrackingState(session, ftiming, true, trackingState);
        ovr_GetSessionStatus(session, sessionStatus);

        input.updateControllerStates();

        headPose = trackingState.HeadPose().ThePose();
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void destroy() {
        // fovPorts: contents are managed by LibOVR, no need to do anything.

        // Clean up the input
        input.dispose();

        // Check if we've set up rendering - if so, clean that up.
        if (chains != null) {
            // Destroy our set of huge buffer images.
            for (long chain : chains) {
                ovr_DestroyTextureSwapChain(session, chain);
            }

            // Free up the layer
            layer0.free();

            // The layers array apparently takes care of itself (and crashes if we try to free it)
        }

        for (OVREyeRenderDesc eye : eyeRenderDesc) {
            eye.free();
        }
        for (OVRMatrix4f projection : projections) {
            projection.free();
        }

        hmdDesc.free();
        trackingState.free();
        sessionStatus.free();

        // Wrap everything up
        ovr_Destroy(session);
        ovr_Shutdown();
    }

    @Override
    public void reset() {
        // Reset the coordinate system - where the user's head is now is facing forwards from [0,0,0]
        ovr_RecenterTrackingOrigin(session);
    }

    @Override
    public void getRenderSize(Vector2f store) {
        if (!isInitialized()) {
            throw new IllegalStateException("Cannot call getRenderSize() before initialized!");
        }
        store.x = textureW;
        store.y = textureH;
    }

    @Override
    public float getInterpupillaryDistance() {
        return 0.065f; // TODO
    }

    @Override
    public Quaternion getOrientation() {
        return quatO2J(headPose.Orientation(), new Quaternion());
    }

    @Override
    public Vector3f getPosition() {
        return vecO2J(headPose.Position(), new Vector3f());
    }

    @Override
    public void getPositionAndOrientation(Vector3f storePos, Quaternion storeRot) {
        storePos.set(getPosition());
        storeRot.set(getOrientation());
    }

    private Matrix4f calculateProjection(int eye, Camera cam) {
        Matrix4f mat = new Matrix4f();

        // Get LibOVR to find the correct projection
        OVRUtil.ovrMatrix4f_Projection(fovPorts[eye], cam.getFrustumNear(), cam.getFrustumFar(), OVRUtil.ovrProjection_None, projections[eye]);

        matrixO2J(projections[eye], mat);

        return mat;
    }

    @Override
    public Matrix4f getHMDMatrixProjectionLeftEye(Camera cam) {
        return calculateProjection(ovrEye_Left, cam);
    }

    @Override
    public Matrix4f getHMDMatrixProjectionRightEye(Camera cam) {
        return calculateProjection(ovrEye_Right, cam);
    }

    @Override
    public Vector3f getHMDVectorPoseLeftEye() {
        return hmdRelativeEyePositions[ovrEye_Left];
    }

    @Override
    public Vector3f getHMDVectorPoseRightEye() {
        return hmdRelativeEyePositions[ovrEye_Right];
    }

    @Override
    public Vector3f getSeatedToAbsolutePosition() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Matrix4f getHMDMatrixPoseLeftEye() {
        return hmdRelativeEyePoses[ovrEye_Left];
    }

    @Override
    public Matrix4f getHMDMatrixPoseRightEye() {
        return hmdRelativeEyePoses[ovrEye_Left];
    }

    @Override
    public HmdType getType() {
        return HmdType.OCULUS_RIFT;
    }

    @Override
    public boolean initVRCompositor(boolean set) {
        if (!set) {
            throw new UnsupportedOperationException("Cannot use LibOVR without compositor!");
        }

        setupLayers();

        framebuffers = new FrameBuffer[2][];
        for (int eye = 0; eye < 2; eye++)
            setupFramebuffers(eye);

        // TODO move initialization code here from VRViewManagerOculus
        return true;
    }

    @Override
    public void printLatencyInfoToConsole(boolean set) {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public void setFlipEyes(boolean set) {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public Void getCompositor() {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public Void getVRSystem() {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    // Rendering-type stuff

    public void findHMDTextureSize() {
        // Texture sizes
        float pixelScaling = 1.0f; // pixelsPerDisplayPixel

        OVRSizei leftTextureSize = OVRSizei.malloc();
        ovr_GetFovTextureSize(session, ovrEye_Left, fovPorts[ovrEye_Left], pixelScaling, leftTextureSize);

        OVRSizei rightTextureSize = OVRSizei.malloc();
        ovr_GetFovTextureSize(session, ovrEye_Right, fovPorts[ovrEye_Right], pixelScaling, rightTextureSize);

        if (leftTextureSize.w() != rightTextureSize.w()) {
            throw new IllegalStateException("Texture sizes do not match [horizontal]");
        }
        if (leftTextureSize.h() != rightTextureSize.h()) {
            throw new IllegalStateException("Texture sizes do not match [vertical]");
        }

        textureW = leftTextureSize.w();
        textureH = leftTextureSize.h();

        leftTextureSize.free();
        rightTextureSize.free();
    }

    private long setupTextureChain() {
        // Set up the information for the texture buffer chain thing
        OVRTextureSwapChainDesc swapChainDesc = OVRTextureSwapChainDesc.calloc()
                .Type(ovrTexture_2D)
                .ArraySize(1)
                .Format(OVR_FORMAT_R8G8B8A8_UNORM_SRGB)
                .Width(textureW)
                .Height(textureH)
                .MipLevels(1)
                .SampleCount(1)
                .StaticImage(false); // ovrFalse

        // Create the chain
        PointerBuffer textureSetPB = createPointerBuffer(1);
        if (OVRGL.ovr_CreateTextureSwapChainGL(session, swapChainDesc, textureSetPB) != ovrSuccess) {
            throw new RuntimeException("Failed to create Swap Texture Set");
        }
        swapChainDesc.free();

        return textureSetPB.get(); // TODO is this a memory leak?
    }

    public void setupLayers() {
        //Layers
        layer0 = OVRLayerEyeFov.calloc();
        layer0.Header().Type(ovrLayerType_EyeFov);
        layer0.Header().Flags(ovrLayerFlag_TextureOriginAtBottomLeft);

        chains = new long[2];
        for (int eye = 0; eye < 2; eye++) {
            long eyeChain = setupTextureChain();
            chains[eye] = eyeChain;

            OVRRecti viewport = OVRRecti.calloc();
            viewport.Pos().x(0);
            viewport.Pos().y(0);
            viewport.Size().w(textureW);
            viewport.Size().h(textureH);

            layer0.ColorTexture(eye, eyeChain);
            layer0.Viewport(eye, viewport);
            layer0.Fov(eye, fovPorts[eye]);

            viewport.free();
            // we update pose only when we have it in the render loop
        }

        layers = createPointerBuffer(1);
        layers.put(0, layer0);
    }

    /**
     * Create a framebuffer for an eye.
     */
    public void setupFramebuffers(int eye) {
        // Find the chain length
        IntBuffer length = BufferUtils.createIntBuffer(1);
        ovr_GetTextureSwapChainLength(session, chains[eye], length);
        int chainLength = length.get();

        LOGGER.fine("HMD Eye #" + eye + " texture chain length: " + chainLength);

        // Create the frame buffers
        framebuffers[eye] = new FrameBuffer[chainLength];
        for (int i = 0; i < chainLength; i++) {
            // find the GL texture ID for this texture
            IntBuffer textureIdB = BufferUtils.createIntBuffer(1);
            OVRGL.ovr_GetTextureSwapChainBufferGL(session, chains[eye], i, textureIdB);
            int textureId = textureIdB.get();

            // TODO less hacky way of getting our texture into JMonkeyEngine
            Image img = new Image();
            img.setId(textureId);
            img.setFormat(Image.Format.RGBA8);
            img.setWidth(textureW);
            img.setHeight(textureH);

            Texture2D tex = new Texture2D(img);

            FrameBuffer buffer = new FrameBuffer(textureW, textureH, 1);
            buffer.setDepthBuffer(Image.Format.Depth);
            buffer.setColorTexture(tex);

            framebuffers[eye][i] = buffer;
        }
    }

    // UTILITIES
    // TODO move to helper class

    /**
     * Copy the values from a LibOVR matrix into a jMonkeyEngine matrix.
     *
     * @param from The matrix to copy from.
     * @param to   The matrix to copy to.
     * @return The {@code to} argument.
     */
    public static Matrix4f matrixO2J(OVRMatrix4f from, Matrix4f to) {
        to.loadIdentity(); // For the additional columns (unless I'm badly misunderstanding matrices)

        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                float val = from.M(x + y * 4); // TODO verify this
                to.set(x, y, val);
            }
        }

        to.transposeLocal(); // jME vs LibOVR coordinate spaces - Yay!

        return to;
    }

    /**
     * Copy the values from a LibOVR quaternion into a jMonkeyEngine quaternion.
     *
     * @param from The quaternion to copy from.
     * @param to   The quaternion to copy to.
     * @return The {@code to} argument.
     */
    public static Quaternion quatO2J(OVRQuatf from, Quaternion to) {
        // jME and LibOVR do their coordinate spaces differently for rotations, so flip Y and W (thanks, jMonkeyVR).
        to.set(
                from.x(),
                -from.y(),
                from.z(),
                -from.w()
        );

        to.normalizeLocal();

        return to;
    }

    /**
     * Copy the values from a LibOVR vector into a jMonkeyEngine vector.
     *
     * @param from The vector to copy from.
     * @param to   The vector to copy to.
     * @return The {@code to} argument.
     */
    public static Vector3f vecO2J(OVRVector3f from, Vector3f to) {
        // jME and LibOVR disagree on which way X and Z are, too.
        to.set(
                -from.x(),
                from.y(),
                -from.z()
        );

        return to;
    }

    // Getters, intended for VRViewManager.

    public long getSessionPointer() {
        return session;
    }

    public long getChain(int eye) {
        return chains[eye];
    }

    public FrameBuffer[] getFramebuffers(int eye) {
        return framebuffers[eye];
    }

    public PointerBuffer getLayers() {
        return layers;
    }

    public OVRLayerEyeFov getLayer0() {
        return layer0;
    }

    public OVRFovPort getFovPort() {
        return fovPorts[ovrEye_Left]; // TODO checking the left and right eyes match
    }

    public OVRPosef getHeadPose() {
        return headPose;
    }

    public OVRPosef getEyePose(int eye) {
        return eyeRenderDesc[eye].HmdToEyePose();
    }

    public VREnvironment getEnvironment() {
        return environment;
    }
}

/* vim: set ts=4 softtabstop=0 sw=4 expandtab: */


/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.input.vr;

import com.jme3.app.VREnvironment;
import com.jme3.math.*;
import com.jme3.renderer.Camera;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
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
 *
 * @author Campbell Suter <znix@znix.xyz>
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
     * it visible or is the universal menu open, etc)
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
     * Store the poses for each eye.
     *
     * @see #getHMDMatrixPoseLeftEye()
     */
    private final Matrix4f[] eyePoses = new Matrix4f[2];

    // The size of the texture drawn onto the HMD
    private int textureW;
    private int textureH;

    // Layers to render into
    private PointerBuffer layers;
    private OVRLayerEyeFov layer0;

    /**
     * Chain texture set thing.
     */
    private long chain;

    /**
     * Frame buffers we can draw into.
     */
    private FrameBuffer framebuffers[];

    public OculusVR(VREnvironment environment) {
        this.environment = environment;
    }

    @Override
    public OpenVRInput getVRinput() {
        throw new UnsupportedOperationException();
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
        OVRDetectResult detect = OVRDetectResult.calloc();
        ovr_Detect(0, detect);
        boolean connected = detect.IsOculusHMDConnected();
        LOGGER.info("OVRDetectResult.IsOculusHMDConnected = " + connected);
        LOGGER.info("OVRDetectResult.IsOculusServiceRunning = " + detect.IsOculusServiceRunning());
        detect.free();

        if (!connected) {
            return false;
        }

        initialized = true;

        // step 1 - hmd init
        System.out.println("step 1 - hmd init");
        OVRLogCallback callback = new OVRLogCallback() {
            @Override
            public void invoke(long userData, int level, long message) {
                System.out.println("LibOVR [" + userData + "] [" + level + "] " + memASCII(message));
            }
        };
        OVRInitParams initParams = OVRInitParams.calloc();
        initParams.LogCallback(callback);
        //initParams.Flags(ovrInit_Debug);
        if (ovr_Initialize(initParams) != ovrSuccess) {
            System.out.println("init failed");
        }
        System.out.println("OVR SDK " + ovr_GetVersionString());
        initParams.free();

        // step 2 - hmd create
        System.out.println("step 2 - hmd create");
        PointerBuffer pHmd = memAllocPointer(1);
        OVRGraphicsLuid luid = OVRGraphicsLuid.calloc();
        if (ovr_Create(pHmd, luid) != ovrSuccess) {
            System.out.println("create failed, try debug");
            //debug headset is now enabled via the Oculus Configuration util . tools -> Service -> Configure
            return false;
        }
        session = pHmd.get(0);
        memFree(pHmd);
        luid.free();
        sessionStatus = OVRSessionStatus.calloc();

        // step 3 - hmdDesc queries
        System.out.println("step 3 - hmdDesc queries");
        hmdDesc = OVRHmdDesc.malloc();
        ovr_GetHmdDesc(session, hmdDesc);
        System.out.println("ovr_GetHmdDesc = " + hmdDesc.ManufacturerString() + " " + hmdDesc.ProductNameString() + " " + hmdDesc.SerialNumberString() + " " + hmdDesc.Type());
        if (hmdDesc.Type() == ovrHmd_None) {
            System.out.println("missing init");
            return false;
        }

        resolutionW = hmdDesc.Resolution().w();
        resolutionH = hmdDesc.Resolution().h();
        System.out.println("resolution W=" + resolutionW + ", H=" + resolutionH);
        if (resolutionW == 0) {
            System.out.println("Huh - width=0");
            return false;
        }

        // FOV
        for (int eye = 0; eye < 2; eye++) {
            fovPorts[eye] = hmdDesc.DefaultEyeFov(eye);
            System.out.println("eye " + eye + " = " + fovPorts[eye].UpTan() + ", " + fovPorts[eye].DownTan() + ", " + fovPorts[eye].LeftTan() + ", " + fovPorts[eye].RightTan());
        }
        // TODO what does this do? I think it might be the height of the player, for correct floor heights?
        // playerEyePos = new Vector3f(0.0f, -ovr_GetFloat(session, OVR_KEY_EYE_HEIGHT, 1.65f), 0.0f);

        // step 4 - tracking - no longer needed as of 0.8.0.0

        // step 5 - projections
        System.out.println("step 5 - projections");
        for (int eye = 0; eye < 2; eye++) {
            projections[eye] = OVRMatrix4f.malloc();
            OVRUtil.ovrMatrix4f_Projection(fovPorts[eye], 0.5f, 500f, OVRUtil.ovrProjection_None, projections[eye]);
            //1.3 was right handed, now none flag
        }

        // step 6 - render desc
        System.out.println("step 6 - render desc");
        for (int eye = 0; eye < 2; eye++) {
            eyeRenderDesc[eye] = OVREyeRenderDesc.malloc();
            ovr_GetRenderDesc(session, eye, fovPorts[eye], eyeRenderDesc[eye]);

            // Changed from an offset to a pose, so there is also a rotation.
            System.out.println("ipd eye " + eye + " = " + eyeRenderDesc[eye].HmdToEyePose().Position().x());

            OVRPosef pose = eyeRenderDesc[eye].HmdToEyePose();

            Matrix4f jPose = new Matrix4f();
            jPose.setTranslation(vecO2J(pose.Position(), new Vector3f()));
            jPose.setRotationQuaternion(quatO2J(pose.Orientation(), new Quaternion()));

            eyePoses[eye] = jPose;
        }

        // step 7 - recenter
        System.out.println("step 7 - recenter");
        ovr_RecenterTrackingOrigin(session);

        // throw new UnsupportedOperationException("Not yet implemented!");
        return true;
    }

    @Override
    public void updatePose() {
        double ftiming = ovr_GetPredictedDisplayTime(session, 0);
        OVRTrackingState hmdState = OVRTrackingState.malloc();
        ovr_GetTrackingState(session, ftiming, true, hmdState);

        // TODO
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void destroy() {
        // fovPorts: contents are managed by LibOVR, no need to do anything.

        // Check if we've set up rendering - if so, clean that up.
        if (chain != 0) {
            // Destroy our set of huge buffer images.
            ovr_DestroyTextureSwapChain(session, chain);

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
        sessionStatus.free();

        // Wrap everything up
        ovr_Destroy(session);
        ovr_Shutdown();
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void getRenderSize(Vector2f store) {
        store.x = resolutionW;
        store.y = resolutionH;
    }

    @Override
    public float getInterpupillaryDistance() {
        return 0.065f; // TODO
    }

    @Override
    public Quaternion getOrientation() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Vector3f getPosition() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void getPositionAndOrientation(Vector3f storePos, Quaternion storeRot) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Matrix4f getHMDMatrixProjectionLeftEye(Camera cam) {
        return matrixO2J(projections[ovrEye_Left], new Matrix4f());
    }

    @Override
    public Matrix4f getHMDMatrixProjectionRightEye(Camera cam) {
        return matrixO2J(projections[ovrEye_Right], new Matrix4f());
    }

    @Override
    public Vector3f getHMDVectorPoseLeftEye() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Vector3f getHMDVectorPoseRightEye() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Vector3f getSeatedToAbsolutePosition() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Matrix4f getHMDMatrixPoseLeftEye() {
        return eyePoses[ovrEye_Left];
    }

    @Override
    public Matrix4f getHMDMatrixPoseRightEye() {
        return eyePoses[ovrEye_Left];
    }

    @Override
    public HmdType getType() {
        throw new UnsupportedOperationException();
    }

    public boolean initVRCompositor(boolean set) {
        if (!set) {
            throw new UnsupportedOperationException("Cannot use LibOVR without compositor!");
        }

        findHMDTextureSize();
        setupLayers();
        setupFramebuffers();

        // TODO move initialization code here from VRViewManagerOculus
        return true;
    }

    public void printLatencyInfoToConsole(boolean set) {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    public void setFlipEyes(boolean set) {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    public Void getCompositor() {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    public Void getVRSystem() {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    // Rendering-type stuff

    public void findHMDTextureSize() {
        // Texture sizes
        float pixelScaling = 1.0f; // pixelsPerDisplayPixel

        OVRSizei leftTextureSize = OVRSizei.malloc();
        ovr_GetFovTextureSize(session, ovrEye_Left, fovPorts[ovrEye_Left], pixelScaling, leftTextureSize);
        System.out.println("leftTextureSize W=" + leftTextureSize.w() + ", H=" + leftTextureSize.h());

        OVRSizei rightTextureSize = OVRSizei.malloc();
        ovr_GetFovTextureSize(session, ovrEye_Right, fovPorts[ovrEye_Right], pixelScaling, rightTextureSize);
        System.out.println("rightTextureSize W=" + rightTextureSize.w() + ", H=" + rightTextureSize.h());

        textureW = leftTextureSize.w() + rightTextureSize.w();
        textureH = Math.max(leftTextureSize.h(), rightTextureSize.h());

        leftTextureSize.free();
        rightTextureSize.free();
    }

    private PointerBuffer setupTextureChain() {
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
        chain = textureSetPB.get(0);
        swapChainDesc.free();
        System.out.println("done chain creation");

        return textureSetPB;
    }

    public void setupLayers() {
        PointerBuffer chainPtr = setupTextureChain();

        //Layers
        layer0 = OVRLayerEyeFov.calloc();
        layer0.Header().Type(ovrLayerType_EyeFov);
        layer0.Header().Flags(ovrLayerFlag_TextureOriginAtBottomLeft);

        for (int eye = 0; eye < 2; eye++) {
            OVRRecti viewport = OVRRecti.calloc();
            viewport.Pos().x(0);
            viewport.Pos().y(0);
            viewport.Size().w(textureW);
            viewport.Size().h(textureH);

            layer0.ColorTexture(chainPtr);
            layer0.Viewport(eye, viewport);
            layer0.Fov(eye, fovPorts[eye]);

            viewport.free();
            // we update pose only when we have it in the render loop
        }

        layers = createPointerBuffer(1);
        layers.put(0, layer0);
    }

    /**
     * Create framebuffers bound to each of the eye textures
     */
    public void setupFramebuffers() {
        // Find the chain length
        IntBuffer length = BufferUtils.createIntBuffer(1);
        ovr_GetTextureSwapChainLength(session, chain, length);
        int chainLength = length.get();

        System.out.println("chain length=" + chainLength);

        // Create the frame buffers
        framebuffers = new FrameBuffer[chainLength];
        for (int i = 0; i < chainLength; i++) {
            // find the GL texture ID for this texture
            IntBuffer textureIdB = BufferUtils.createIntBuffer(1);
            OVRGL.ovr_GetTextureSwapChainBufferGL(session, chain, i, textureIdB);
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

            framebuffers[i] = buffer;
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
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                float val = from.M(x + y * 4); // TODO verify this
                to.set(x, y, val);
            }
        }

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
        to.set(
                from.x(),
                from.y(),
                from.z(),
                from.w()
        );

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
        to.set(
                from.x(),
                from.y(),
                from.z()
        );

        return to;
    }

    // Getters, intended for VRViewManager.

    public long getSessionPointer() {
        return session;
    }

    public long getChain() {
        return chain;
    }

    public FrameBuffer[] getFramebuffers() {
        return framebuffers;
    }

    public PointerBuffer getLayers() {
        return layers;
    }
}

/* vim: set ts=4 softtabstop=0 sw=4 expandtab: */


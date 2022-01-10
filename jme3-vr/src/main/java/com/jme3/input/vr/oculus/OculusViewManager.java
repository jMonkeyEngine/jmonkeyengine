/*
 * Copyright (c) 2009-2018 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.input.vr.oculus;

import com.jme3.app.VREnvironment;
import com.jme3.input.vr.AbstractVRViewManager;
import com.jme3.input.vr.VRAPI;
import com.jme3.math.*;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.util.BufferUtils;
import com.jme3.util.VRGUIPositioningMode;

import java.nio.IntBuffer;
import java.util.Iterator;
import java.util.Objects;

import org.lwjgl.ovr.OVRFovPort;
import org.lwjgl.ovr.OVRPosef;
import org.lwjgl.ovr.OVRUtil;

import static org.lwjgl.ovr.OVR.*;
import static org.lwjgl.ovr.OVRErrorCode.*;

/**
 * A rendering system for Oculus's LibOVR API.
 *
 * @author Campbell Suter (znix@znix.xyz)
 */
public class OculusViewManager extends AbstractVRViewManager {

    private final VREnvironment environment;
    private final OculusVR hardware;

    // Copied from OSVR
    //final & temp values for camera calculations
    private final Vector3f finalPosition = new Vector3f();
    private final Quaternion finalRotation = new Quaternion();
    private final Vector3f hmdPos = new Vector3f();
    private final Quaternion hmdRot = new Quaternion();

    public OculusViewManager(VREnvironment environment) {
        this.environment = environment;

        VRAPI hardware = environment.getVRHardware();
        Objects.requireNonNull(hardware, "Attached VR Hardware cannot be null");
        if (!(hardware instanceof OculusVR)) {
            throw new IllegalStateException("Cannot use Oculus VR view manager on non-Oculus hardware state!");
        }

        this.hardware = (OculusVR) hardware;

        if (!environment.compositorAllowed()) {
            throw new UnsupportedOperationException("Cannot render without compositor on LibOVR");
        }
    }

    @Override
    public void initialize() {
        setupCamerasAndViews();

        if (environment.hasTraditionalGUIOverlay()) {

            environment.getVRMouseManager().initialize();

            // update the pose to position the gui correctly on start
            update(0f);
            environment.getVRGUIManager().positionGui();
        }
    }

    private long session() {
        return hardware.getSessionPointer();
    }

    @Override
    public void update(float tpf) {
        // TODO

        hardware.updatePose();

        // TODO deduplicate
        if (environment == null) {
            throw new IllegalStateException("This VR view manager is not attached to any VR environment.");
        }

        // grab the observer
        Object obs = environment.getObserver();
        Quaternion objRot;
        Vector3f objPos;
        if (obs instanceof Camera) {
            objRot = ((Camera) obs).getRotation();
            objPos = ((Camera) obs).getLocation();
        } else {
            objRot = ((Spatial) obs).getWorldRotation();
            objPos = ((Spatial) obs).getWorldTranslation();
        }

        // update the HMD's position & orientation
        hardware.getPositionAndOrientation(hmdPos, hmdRot);
        if (obs != null) {
            // update hmdPos based on obs rotation
            finalRotation.set(objRot);
            finalRotation.mult(hmdPos, hmdPos);
            finalRotation.multLocal(hmdRot);
        }

        // Update both eye cameras
        finalizeCamera(hardware.getHMDVectorPoseLeftEye(), objPos, leftCamera);
        finalizeCamera(hardware.getHMDVectorPoseRightEye(), objPos, rightCamera);

        // Update the main camera, so it shows the same basic view the HMD is getting
        // TODO: Do this in VRAppState, so it works on all HMDs.
        // I only have a Rift, so I can't test it on anything else.
        if(!environment.isInstanceRendering()) { // We use the app camera as the left camera here
            // TODO: Double up on rendering and use one eye, to reduce GPU load rendering the scene again.
            // TODO: Snip at the image to remove the distorted corners from a very high FOV.
            finalizeCamera(Vector3f.ZERO, objPos, environment.getApplication().getCamera());
        }

        if (environment.hasTraditionalGUIOverlay()) {
            // update the mouse?
            environment.getVRMouseManager().update(tpf);

            // update GUI position?
            if (environment.getVRGUIManager().isWantsReposition() || environment.getVRGUIManager().getPositioningMode() != VRGUIPositioningMode.MANUAL) {
                environment.getVRGUIManager().positionGuiNow(tpf);
                environment.getVRGUIManager().updateGuiQuadGeometricState();
            }
        }
    }

    /**
     * Place the camera within the scene.
     *
     * @param eyePos      the eye position.
     * @param obsPosition the observer position.
     * @param cam         the camera to place.
     */
    private void finalizeCamera(Vector3f eyePos, Vector3f obsPosition, Camera cam) {
        finalRotation.mult(eyePos, finalPosition);
        finalPosition.addLocal(hmdPos);
        if (obsPosition != null) {
            finalPosition.addLocal(obsPosition);
        }
        finalPosition.y += getHeightAdjustment();
        cam.setFrame(finalPosition, finalRotation);
    }

    @Override
    public void render() {

        // Calculate the render pose (translation/rotation) for each eye.
        // LibOVR takes the difference between this and the real position of each eye at display time
        // to apply AZW (timewarp).

        OVRPosef.Buffer hmdToEyeOffsets = OVRPosef.calloc(2);
        hmdToEyeOffsets.put(0, hardware.getEyePose(ovrEye_Left));
        hmdToEyeOffsets.put(1, hardware.getEyePose(ovrEye_Right));

        //calculate eye poses
        OVRUtil.ovr_CalcEyePoses(hardware.getHeadPose(), hmdToEyeOffsets, hardware.getLayer0().RenderPose());
        hmdToEyeOffsets.free();

        for (int eye = 0; eye < 2; eye++) {
            IntBuffer currentIndexB = BufferUtils.createIntBuffer(1);
            ovr_GetTextureSwapChainCurrentIndex(session(), hardware.getChain(eye), currentIndexB);
            int index = currentIndexB.get();

            // Constantly (each frame) rotating through a series of
            // frame buffers, so make sure we write into the correct one.
            (eye == ovrEye_Left ? leftViewPort : rightViewPort).setOutputFrameBuffer(hardware.getFramebuffers(eye)[index]);
        }

        // Now the game will render into the buffers given to us by LibOVR
    }

    @Override
    public void postRender() {
        // We're done with our textures now - the game is done drawing into them.
        for (int eye = 0; eye < 2; eye++) {
            ovr_CommitTextureSwapChain(session(), hardware.getChain(eye));
        }

        // Send the result to the HMD
        int result = ovr_SubmitFrame(session(), 0, null, hardware.getLayers());
        if (result != ovrSuccess) {
            throw new IllegalStateException("Failed to submit frame!");
        }
    }

    /*
     *********************************************************
     *  Show's over, now it's just boring camera stuff etc.  *
     *********************************************************
     */

    /**
     * Set up the cameras and views for each eye and the mirror display.
     */
    private void setupCamerasAndViews() {
        // TODO: Use LobOVR IPD etc
        if (environment != null) {
            // get desired frustum from original camera
            Camera origCam = environment.getCamera();
            float fFar = origCam.getFrustumFar();
            float fNear = origCam.getFrustumNear();

            // restore frustum on distortion scene cam, if needed
            if (environment.isInstanceRendering()) {
                leftCamera = origCam;
            } else {
                leftCamera = origCam.clone();
            }

            OVRFovPort fp = hardware.getFovPort();
            float hFov = fp.LeftTan() + fp.RightTan();
            float vFov = fp.UpTan() + fp.DownTan();
            getLeftCamera().setFrustumPerspective(hFov / FastMath.TWO_PI * 360, vFov / hFov, fNear, fFar);

            prepareCameraSize(getLeftCamera(), 1f);
            if (environment.getVRHardware() != null) {
                getLeftCamera().setProjectionMatrix(environment.getVRHardware().getHMDMatrixProjectionLeftEye(getLeftCamera()));
            }
            //org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_SRGB);

            if (!environment.isInstanceRendering()) {
                leftViewPort = setupViewBuffers(getLeftCamera(), LEFT_VIEW_NAME);
                rightCamera = getLeftCamera().clone();
                if (environment.getVRHardware() != null) {
                    getRightCamera().setProjectionMatrix(environment.getVRHardware().getHMDMatrixProjectionRightEye(getRightCamera()));
                }
                rightViewPort = setupViewBuffers(getRightCamera(), RIGHT_VIEW_NAME);
            } else if (environment.getApplication() != null) {
                throw new UnsupportedOperationException("Not yet implemented!");
            } else {
                throw new IllegalStateException("This VR environment is not attached to any application.");
            }

            // setup gui
            environment.getVRGUIManager().setupGui(getLeftCamera(), getRightCamera(), getLeftViewPort(), getRightViewPort());
        } else {
            throw new IllegalStateException("This VR view manager is not attached to any VR environment.");
        }
    }

    private void prepareCameraSize(Camera cam, float xMult) {
        // TODO this function is identical to that in VRViewManagerOpenVR; merge the two.
        if (environment != null) {
            if (environment.getApplication() != null) {
                Vector2f size = new Vector2f();
                VRAPI vrhmd = environment.getVRHardware();

                if (vrhmd == null) {
                    size.x = 1280f;
                    size.y = 720f;
                } else {
                    vrhmd.getRenderSize(size);
                }

                if (size.x < environment.getApplication().getContext().getSettings().getWidth()) {
                    size.x = environment.getApplication().getContext().getSettings().getWidth();
                }
                if (size.y < environment.getApplication().getContext().getSettings().getHeight()) {
                    size.y = environment.getApplication().getContext().getSettings().getHeight();
                }

                if (environment.isInstanceRendering()) {
                    size.x *= 2f;
                }

                // other adjustments
                size.x *= xMult;
                size.x *= getResolutionMuliplier();
                size.y *= getResolutionMuliplier();

                if (cam.getWidth() != size.x || cam.getHeight() != size.y) {
                    cam.resize((int) size.x, (int) size.y, false);
                }
            } else {
                throw new IllegalStateException("This VR environment is not attached to any application.");
            }
        } else {
            throw new IllegalStateException("This VR view manager is not attached to any VR environment.");
        }
    }

    private ViewPort setupViewBuffers(Camera cam, String viewName) {
        // TODO this function is identical to that in VRViewManagerOpenVR; merge the two.
        if (environment != null) {
            if (environment.getApplication() != null) {
                ViewPort viewPort = environment.getApplication().getRenderManager().createPreView(viewName, cam);
                viewPort.setClearFlags(true, true, true);
                viewPort.setBackgroundColor(ColorRGBA.Black);

                Iterator<Spatial> spatialIter = environment.getApplication().getViewPort().getScenes().iterator();
                while (spatialIter.hasNext()) {
                    viewPort.attachScene(spatialIter.next());
                }

                // The target view buffer will be set during prerender.
                return viewPort;
            } else {
                throw new IllegalStateException("This VR environment is not attached to any application.");
            }
        } else {
            throw new IllegalStateException("This VR view manager is not attached to any VR environment.");
        }
    }
}

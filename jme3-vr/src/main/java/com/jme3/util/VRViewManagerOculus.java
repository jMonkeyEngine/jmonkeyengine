/*
 * Copyright (c) 2009-2017 jMonkeyEngine
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
package com.jme3.util;

import com.jme3.app.VREnvironment;
import com.jme3.input.vr.OculusVR;
import com.jme3.input.vr.VRAPI;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.texture.*;

import java.nio.IntBuffer;
import java.util.Iterator;
import java.util.logging.Logger;

import org.lwjgl.PointerBuffer;

import org.lwjgl.ovr.*;

import static org.lwjgl.BufferUtils.*;
import static org.lwjgl.ovr.OVR.*;
import static org.lwjgl.ovr.OVRErrorCode.*;

/**
 * A rendering system for Oculus's LibOVR API.
 *
 * @author Campbell Suter <znix@znix.xyz>
 */
public class VRViewManagerOculus extends AbstractVRViewManager {

    private static final Logger LOG = Logger.getLogger(VRViewManagerOculus.class.getName());

    private final VREnvironment environment;
    private final OculusVR hardware;

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

    public VRViewManagerOculus(VREnvironment environment) {
        this.environment = environment;

        VRAPI hardware = environment.getVRHardware();
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

        findHMDTextureSize();
        setupLayers();
        setupFramebuffers();
    }

    private void findHMDTextureSize() {
        OVRFovPort fovPorts[] = hardware.getFovPorts();

        // Texture sizes
        float pixelScaling = 1.0f; // pixelsPerDisplayPixel

        OVRSizei leftTextureSize = OVRSizei.malloc();
        ovr_GetFovTextureSize(session(), ovrEye_Left, fovPorts[ovrEye_Left], pixelScaling, leftTextureSize);
        System.out.println("leftTextureSize W=" + leftTextureSize.w() + ", H=" + leftTextureSize.h());

        OVRSizei rightTextureSize = OVRSizei.malloc();
        ovr_GetFovTextureSize(session(), ovrEye_Right, fovPorts[ovrEye_Right], pixelScaling, rightTextureSize);
        System.out.println("rightTextureSize W=" + rightTextureSize.w() + ", H=" + rightTextureSize.h());

        textureW = leftTextureSize.w() + rightTextureSize.w();
        textureH = Math.max(leftTextureSize.h(), rightTextureSize.h());

        leftTextureSize.free();
        rightTextureSize.free();
    }

    private long session() {
        return hardware.getSessionPointer();
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
        if (OVRGL.ovr_CreateTextureSwapChainGL(session(), swapChainDesc, textureSetPB) != ovrSuccess) {
            throw new RuntimeException("Failed to create Swap Texture Set");
        }
        chain = textureSetPB.get(0);
        swapChainDesc.free();
        System.out.println("done chain creation");

        return textureSetPB;
    }

    private void setupLayers() {
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
            layer0.Fov(eye, hardware.getFovPorts()[eye]);

            viewport.free();
            // we update pose only when we have it in the render loop
        }

        layers = createPointerBuffer(1);
        layers.put(0, layer0);
    }

    /**
     * Create framebuffers bound to each of the eye textures
     */
    private void setupFramebuffers() {
        // Find the chain length
        IntBuffer length = BufferUtils.createIntBuffer(1);
        ovr_GetTextureSwapChainLength(session(), chain, length);
        int chainLength = length.get();

        System.out.println("chain length=" + chainLength);

        // Create the frame buffers
        framebuffers = new FrameBuffer[chainLength];
        for (int i = 0; i < chainLength; i++) {
            // find the GL texture ID for this texture
            IntBuffer textureIdB = BufferUtils.createIntBuffer(1);
            OVRGL.ovr_GetTextureSwapChainBufferGL(session(), chain, i, textureIdB);
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

    @Override
    public void update(float tpf) {
        // TODO

        hardware.updatePose();
    }

    @Override
    public void render() {
        for (int eye = 0; eye < 2; eye++) {
            // TODO add eyePoses
//            OVRPosef eyePose = eyePoses[eye];
//            layer0.RenderPose(eye, eyePose);

            IntBuffer currentIndexB = BufferUtils.createIntBuffer(1);
            ovr_GetTextureSwapChainCurrentIndex(session(), chain, currentIndexB);
            int index = currentIndexB.get();

            (eye == 0 ? leftViewPort : rightViewPort).setOutputFrameBuffer(framebuffers[index]);
        }

        // Now the game will render into the buffers given to us by LibOVR
    }

    @Override
    public void postRender() {
        // We're done with our textures now - the game is done drawing into them.
        ovr_CommitTextureSwapChain(session(), chain);

        // Send the result to the HMD
        int result = ovr_SubmitFrame(session(), 0, null, layers);
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
            // get desired frustrum from original camera
            Camera origCam = environment.getCamera();
            float fFar = origCam.getFrustumFar();
            float fNear = origCam.getFrustumNear();

            // restore frustrum on distortion scene cam, if needed
            if (environment.isInstanceRendering()) {
                leftCamera = origCam;
            } else if (environment.compositorAllowed() == false) {
                origCam.setFrustumFar(100f);
                origCam.setFrustumNear(1f);
                leftCamera = origCam.clone();
                prepareCameraSize(origCam, 2f);
            } else {
                leftCamera = origCam.clone();
            }

            getLeftCamera().setFrustumPerspective(environment.getDefaultFOV(), environment.getDefaultAspect(), fNear, fFar);

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

                LOG.severe("THIS CODE NEED CHANGES !!!");
                leftViewPort = environment.getApplication().getViewPort();
                //leftViewport.attachScene(app.getRootNode());
                rightCamera = getLeftCamera().clone();
                if (environment.getVRHardware() != null) {
                    getRightCamera().setProjectionMatrix(environment.getVRHardware().getHMDMatrixProjectionRightEye(getRightCamera()));
                }

                org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL30.GL_CLIP_DISTANCE0);

                //FIXME: [jme-vr] Fix with JMonkey next release
                //RenderManager._VRInstancing_RightCamProjection = camRight.getViewProjectionMatrix();
                // TODO: Add LibOVR support
                // setupFinalFullTexture(environment.getApplication().getViewPort().getCamera());
            } else {
                throw new IllegalStateException("This VR environment is not attached to any application.");
            }

            // setup gui
            environment.getVRGUIManager().setupGui(getLeftCamera(), getRightCamera(), getLeftViewPort(), getRightViewPort());

            if (environment.getVRHardware() != null) {
                // call these to cache the results internally
                environment.getVRHardware().getHMDMatrixPoseLeftEye();
                environment.getVRHardware().getHMDMatrixPoseRightEye();
            }
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
                // create offscreen framebuffer
                FrameBuffer offBufferLeft = new FrameBuffer(cam.getWidth(), cam.getHeight(), 1);
                //offBufferLeft.setSrgb(true);

                //setup framebuffer's texture
                Texture2D offTex = new Texture2D(cam.getWidth(), cam.getHeight(), Image.Format.RGBA8);
                offTex.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
                offTex.setMagFilter(Texture.MagFilter.Bilinear);

                //setup framebuffer to use texture
                offBufferLeft.setDepthBuffer(Image.Format.Depth);
                offBufferLeft.setColorTexture(offTex);

                ViewPort viewPort = environment.getApplication().getRenderManager().createPreView(viewName, cam);
                viewPort.setClearFlags(true, true, true);
                viewPort.setBackgroundColor(ColorRGBA.Black);

                Iterator<Spatial> spatialIter = environment.getApplication().getViewPort().getScenes().iterator();
                while (spatialIter.hasNext()) {
                    viewPort.attachScene(spatialIter.next());
                }

                //set viewport to render to offscreen framebuffer
                viewPort.setOutputFrameBuffer(offBufferLeft);
                return viewPort;
            } else {
                throw new IllegalStateException("This VR environment is not attached to any application.");
            }
        } else {
            throw new IllegalStateException("This VR view manager is not attached to any VR environment.");
        }
    }
}

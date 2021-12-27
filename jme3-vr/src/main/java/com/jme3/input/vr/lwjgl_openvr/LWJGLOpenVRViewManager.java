package com.jme3.input.vr.lwjgl_openvr;

import com.jme3.app.VREnvironment;
import com.jme3.input.vr.AbstractVRViewManager;
import com.jme3.input.vr.VRAPI;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Spatial;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;
import com.jme3.util.VRGUIPositioningMode;

import java.util.Iterator;
import java.util.logging.Logger;
import org.lwjgl.openvr.VRTextureBounds;
import org.lwjgl.openvr.Texture;
import org.lwjgl.openvr.VR;
import org.lwjgl.openvr.VRCompositor;

/**
 * A VR view manager based on OpenVR. This class enable to submit 3D views to
 * the VR compositor.
 *
 * @author reden - phr00t
 * @author Julien Seinturier - COMEX SA - <a href="http://www.seinturier.fr">http://www.seinturier.fr</a>
 * @author Rickard Edén
 */
public class LWJGLOpenVRViewManager extends AbstractVRViewManager {

    private static final Logger logger = Logger.getLogger(LWJGLOpenVRViewManager.class.getName());

    // OpenVR values
    private VRTextureBounds leftTextureBounds;
    private Texture leftTextureType;

    private VRTextureBounds rightTextureBounds;
    private Texture rightTextureType;

    private Texture2D dualEyeTex;

    //final & temp values for camera calculations
    private final Vector3f finalPosition = new Vector3f();
    private final Quaternion finalRotation = new Quaternion();
    private final Vector3f hmdPos = new Vector3f();
    private final Quaternion hmdRot = new Quaternion();

    /**
     * Create a new VR view manager attached to the given
     * {@link VREnvironment VR environment}.
     *
     * @param environment the {@link VREnvironment VR environment} to which this
     * view manager is attached.
     */
    public LWJGLOpenVRViewManager(VREnvironment environment) {
        this.environment = environment;
    }

    /**
     * Get the identifier of the left eye texture.
     *
     * @return the identifier of the left eye texture.
     * @see #getRightTexId()
     * @see #getFullTexId()
     */
    protected int getLeftTexId() {
        return getLeftTexture().getImage().getId();
    }

    /**
     * Get the identifier of the right eye texture.
     *
     * @return the identifier of the right eye texture.
     * @see #getLeftTexId()
     * @see #getFullTexId()
     */
    protected int getRightTexId() {
        return getRightTexture().getImage().getId();
    }

    /**
     * Get the identifier of the full (dual eye) texture.
     *
     * @return the identifier of the full (dual eye) texture.
     * @see #getLeftTexId()
     * @see #getRightTexId()
     */
    private int getFullTexId() {
        return dualEyeTex.getImage().getId();
    }

    /**
     * Initialize the system binds of the textures.
     */
    private void initTextureSubmitStructs() {
        leftTextureType = Texture.create();
        rightTextureType = Texture.create();

        if (environment != null) {
            if (environment.getVRHardware() instanceof LWJGLOpenVR) {
                leftTextureBounds = VRTextureBounds.create();
                rightTextureBounds = VRTextureBounds.create();
                // left eye
                leftTextureBounds.set(0f, 0f, 0.5f, 1f);
                // right eye
                rightTextureBounds.set(0.5f, 0f, 1f, 1f);
                // texture type
                leftTextureType.set(-1, VR.ETextureType_TextureType_OpenGL, VR.EColorSpace_ColorSpace_Gamma);
                rightTextureType.set(-1, VR.ETextureType_TextureType_OpenGL, VR.EColorSpace_ColorSpace_Gamma);

            }
        } else {
            throw new IllegalStateException("This VR view manager is not attached to any VR environment.");
        }
    }

    /**
     * updatePose can be called here because appstates are always called before the main renderer. This way we get the latest pose close to when it's supposed to render
     */
    @Override
    public void render() {
        if (environment != null) {
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
            // grab the hardware handle
            VRAPI dev = environment.getVRHardware();
            if (dev != null) {

                // update the HMD's position & orientation
                dev.updatePose();
                dev.getPositionAndOrientation(hmdPos, hmdRot);

                if (obs != null) {
                    // update hmdPos based on obs rotation
                    finalRotation.set(objRot);
                    finalRotation.mult(hmdPos, hmdPos);
                    finalRotation.multLocal(hmdRot);
    }

                finalizeCamera(dev.getHMDVectorPoseLeftEye(), objPos, getLeftCamera());
                finalizeCamera(dev.getHMDVectorPoseRightEye(), objPos, getRightCamera());
            } else {
                getLeftCamera().setFrame(objPos, objRot);
                getRightCamera().setFrame(objPos, objRot);
            }
        }
    }

    @Override
    public void postRender() {

        if (environment != null) {
            if (environment.isInVR()) {
                VRAPI api = environment.getVRHardware();
                // using the compositor...
                int errl = 0, errr = 0;
                if (environment.isInstanceRendering()) {
                    if (leftTextureType.handle() == -1 || leftTextureType.handle() != getFullTexId()) {
                        leftTextureType.set(getFullTexId(), leftTextureType.eType(), leftTextureType.eColorSpace());
                    } else {
                        if (api instanceof LWJGLOpenVR) {
                            int submitFlag = VR.EVRSubmitFlags_Submit_Default;
                            errr = VRCompositor.VRCompositor_Submit(VR.EVREye_Eye_Right, rightTextureType, rightTextureBounds, submitFlag);
                            errl = VRCompositor.VRCompositor_Submit(VR.EVREye_Eye_Left, leftTextureType, leftTextureBounds, submitFlag);
                        }
                    }
                } else if (leftTextureType.handle() == -1 || rightTextureType.handle() == -1
                        || leftTextureType.handle() != getLeftTexId() || rightTextureType.handle() != getRightTexId()) {
                    leftTextureType.set(getLeftTexId(), leftTextureType.eType(), leftTextureType.eColorSpace());
                    rightTextureType.set(getRightTexId(), leftTextureType.eType(), leftTextureType.eColorSpace());
                } else {
                    if (api instanceof LWJGLOpenVR) {
                        int submitFlag = VR.EVRSubmitFlags_Submit_Default;
                        errr = VRCompositor.VRCompositor_Submit(VR.EVREye_Eye_Right, rightTextureType, null, submitFlag);
                        errl = VRCompositor.VRCompositor_Submit(VR.EVREye_Eye_Left, leftTextureType, null, submitFlag);
                    } else {

                    }
                }

                if (errl != 0) {
                    logger.severe("Submit to left compositor error: " + " (" + Integer.toString(errl) + ")");
                    logger.severe("  Texture handle: " + leftTextureType.handle());

                    logger.severe("  Left eye texture " + leftEyeTexture.getName() + " (" + leftEyeTexture.getImage().getId() + ")");
                    logger.severe("                 Type: " + leftEyeTexture.getType());
                    logger.severe("                 Size: " + leftEyeTexture.getImage().getWidth() + "x" + leftEyeTexture.getImage().getHeight());
                    logger.severe("          Image depth: " + leftEyeTexture.getImage().getDepth());
                    logger.severe("         Image format: " + leftEyeTexture.getImage().getFormat());
                    logger.severe("    Image color space: " + leftEyeTexture.getImage().getColorSpace());

                }

                if (errr != 0) {
                    logger.severe("Submit to right compositor error: " + " (" + Integer.toString(errl) + ")");
//                    logger.severe("  Texture color space: "+OpenVRUtil.getEColorSpaceString(rightTextureType.eColorSpace));
//                    logger.severe("  Texture type: "+OpenVRUtil.getETextureTypeString(rightTextureType.eType));
                    logger.severe("  Texture handle: " + rightTextureType.handle());

                    logger.severe("  Right eye texture " + rightEyeTexture.getName() + " (" + rightEyeTexture.getImage().getId() + ")");
                    logger.severe("                 Type: " + rightEyeTexture.getType());
                    logger.severe("                 Size: " + rightEyeTexture.getImage().getWidth() + "x" + rightEyeTexture.getImage().getHeight());
                    logger.severe("          Image depth: " + rightEyeTexture.getImage().getDepth());
                    logger.severe("         Image format: " + rightEyeTexture.getImage().getFormat());
                    logger.severe("    Image color space: " + rightEyeTexture.getImage().getColorSpace());
                }
            }
        } else {
            throw new IllegalStateException("This VR view manager is not attached to any VR environment.");
        }

        VRCompositor.VRCompositor_PostPresentHandoff();

    }

    @Override
    public void initialize() {

        logger.config("Initializing VR view manager.");

        if (environment != null) {

            initTextureSubmitStructs();
            setupCamerasAndViews();
            setupVRScene();
            moveScreenProcessingToEyes();

            if (environment.hasTraditionalGUIOverlay()) {

                environment.getVRMouseManager().initialize();

                // update the pose to position the gui correctly on start
                update(0f);
                environment.getVRGUIManager().positionGui();
            }

            logger.config("Initialized VR view manager [SUCCESS]");

        } else {
            throw new IllegalStateException("This VR view manager is not attached to any VR environment.");
        }
    }

    /**
     * Prepare the size of the given {@link Camera camera} to adapt it to the
     * underlying rendering context.
     *
     * @param cam the {@link Camera camera} to prepare.
     * @param xMult the camera width multiplier.
     */
    private void prepareCameraSize(Camera cam, float xMult) {

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

    /**
     * Replaces rootNode with the distortion mesh as the main camera's scene.
     */
    private void setupVRScene() {

        if (environment != null) {
            if (environment.getApplication() != null) {
                // no special scene to set up if we are doing instancing
                if (environment.isInstanceRendering()) {
                    // distortion has to be done with compositor here... we want only one pass on our end!
                    if (environment.getApplication().getContext().getSettings().isSwapBuffers()) {
                        setupMirrorBuffers(environment.getCamera(), dualEyeTex, true);
                    }
                    return;
                }

                leftEyeTexture = (Texture2D) getLeftViewPort().getOutputFrameBuffer().getColorBuffer().getTexture();
                rightEyeTexture = (Texture2D) getRightViewPort().getOutputFrameBuffer().getColorBuffer().getTexture();
                leftEyeDepth = (Texture2D) getLeftViewPort().getOutputFrameBuffer().getDepthBuffer().getTexture();
                rightEyeDepth = (Texture2D) getRightViewPort().getOutputFrameBuffer().getDepthBuffer().getTexture();

                // main viewport is either going to be a distortion scene or nothing
                // mirroring is handled by copying framebuffers
                Iterator<Spatial> spatialIter = environment.getApplication().getViewPort().getScenes().iterator();
                while (spatialIter.hasNext()) {
                    environment.getApplication().getViewPort().detachScene(spatialIter.next());
                }

                spatialIter = environment.getApplication().getGuiViewPort().getScenes().iterator();
                while (spatialIter.hasNext()) {
                    environment.getApplication().getGuiViewPort().detachScene(spatialIter.next());
                }

                // only setup distortion scene if compositor isn't running (or using custom mesh distortion option)
                if (environment.getApplication().getContext().getSettings().isSwapBuffers()) {
                    setupMirrorBuffers(environment.getCamera(), leftEyeTexture, false);

                }
            } else {
                throw new IllegalStateException("This VR environment is not attached to any application.");
            }
        } else {
            throw new IllegalStateException("This VR view manager is not attached to any VR environment.");
        }
    }

    @Override
    public void update(float tpf) {

        if (environment != null) {

            if (environment.hasTraditionalGUIOverlay()) {
                // update the mouse?
                environment.getVRMouseManager().update(tpf);

                // update GUI position?
                if (environment.getVRGUIManager().isWantsReposition() || environment.getVRGUIManager().getPositioningMode() != VRGUIPositioningMode.MANUAL) {
                    environment.getVRGUIManager().positionGuiNow(tpf);
                    environment.getVRGUIManager().updateGuiQuadGeometricState();
                }
            }
        } else {
            throw new IllegalStateException("This VR view manager is not attached to any VR environment.");
        }
    }

    /**
     * Place the camera within the scene.
     *
     * @param eyePos the eye position.
     * @param obsPosition the observer position.
     * @param cam the camera to place.
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

    private void setupCamerasAndViews() {

        if (environment != null) {
            // get desired frustum from original camera
            Camera origCam = environment.getCamera();
            float fFar = origCam.getFrustumFar();
            float fNear = origCam.getFrustumNear();

            // restore frustum on distortion scene cam, if needed
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
            } else {

                if (environment.getApplication() != null) {

                    logger.severe("THIS CODE NEED CHANGES !!!");
                    leftViewPort = environment.getApplication().getViewPort();
                    //leftViewport.attachScene(app.getRootNode());
                    rightCamera = getLeftCamera().clone();
                    if (environment.getVRHardware() != null) {
                        getRightCamera().setProjectionMatrix(environment.getVRHardware().getHMDMatrixProjectionRightEye(getRightCamera()));
                    }

                    org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL30.GL_CLIP_DISTANCE0);

                    //FIXME: [jme-vr] Fix with JMonkey next release
                    //RenderManager._VRInstancing_RightCamProjection = camRight.getViewProjectionMatrix();
                    setupFinalFullTexture(environment.getApplication().getViewPort().getCamera());
                } else {
                    throw new IllegalStateException("This VR environment is not attached to any application.");
                }

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

    private ViewPort setupMirrorBuffers(Camera cam, Texture2D tex, boolean expand) {

        if (environment != null) {
            if (environment.getApplication() != null) {
                Camera clonecam = cam.clone();
                ViewPort viewPort = environment.getApplication().getRenderManager().createPostView("MirrorView", clonecam);
                clonecam.setParallelProjection(true);
                viewPort.setClearFlags(true, true, true);
                viewPort.setBackgroundColor(ColorRGBA.Black);
                Picture pic = new Picture("fullscene");
                pic.setLocalTranslation(-0.75f, -0.5f, 0f);
                if (expand) {
                    pic.setLocalScale(3f, 1f, 1f);
                } else {
                    pic.setLocalScale(1.5f, 1f, 1f);
                }
                pic.setQueueBucket(Bucket.Opaque);
                pic.setTexture(environment.getApplication().getAssetManager(), tex, false);
                viewPort.attachScene(pic);
                viewPort.setOutputFrameBuffer(null);

                pic.updateGeometricState();

                return viewPort;
            } else {
                throw new IllegalStateException("This VR environment is not attached to any application.");
            }
        } else {
            throw new IllegalStateException("This VR view manager is not attached to any VR environment.");
        }
    }

    private void setupFinalFullTexture(Camera cam) {

        if (environment != null) {
            if (environment.getApplication() != null) {
                // create offscreen framebuffer
                FrameBuffer out = new FrameBuffer(cam.getWidth(), cam.getHeight(), 1);
                //offBuffer.setSrgb(true);

                //setup framebuffer's texture
                dualEyeTex = new Texture2D(cam.getWidth(), cam.getHeight(), Image.Format.RGBA8);
                dualEyeTex.setMinFilter(Texture2D.MinFilter.BilinearNoMipMaps);
                dualEyeTex.setMagFilter(Texture2D.MagFilter.Bilinear);

                logger.config("Dual eye texture " + dualEyeTex.getName() + " (" + dualEyeTex.getImage().getId() + ")");
                logger.config("               Type: " + dualEyeTex.getType());
                logger.config("               Size: " + dualEyeTex.getImage().getWidth() + "x" + dualEyeTex.getImage().getHeight());
                logger.config("        Image depth: " + dualEyeTex.getImage().getDepth());
                logger.config("       Image format: " + dualEyeTex.getImage().getFormat());
                logger.config("  Image color space: " + dualEyeTex.getImage().getColorSpace());

                //setup framebuffer to use texture
                out.setDepthBuffer(Image.Format.Depth);
                out.setColorTexture(dualEyeTex);

                ViewPort viewPort = environment.getApplication().getViewPort();
                viewPort.setClearFlags(true, true, true);
                viewPort.setBackgroundColor(ColorRGBA.Black);
                viewPort.setOutputFrameBuffer(out);
            } else {
                throw new IllegalStateException("This VR environment is not attached to any application.");
            }
        } else {
            throw new IllegalStateException("This VR view manager is not attached to any VR environment.");
        }
    }

    private ViewPort setupViewBuffers(Camera cam, String viewName) {

        if (environment != null) {
            if (environment.getApplication() != null) {
                // create offscreen framebuffer
                FrameBuffer offBufferLeft = new FrameBuffer(cam.getWidth(), cam.getHeight(), 1);
                //offBufferLeft.setSrgb(true);

                //setup framebuffer's texture
                Texture2D offTex = new Texture2D(cam.getWidth(), cam.getHeight(), Image.Format.RGBA8);
                offTex.setMinFilter(Texture2D.MinFilter.BilinearNoMipMaps);
                offTex.setMagFilter(Texture2D.MagFilter.Bilinear);

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
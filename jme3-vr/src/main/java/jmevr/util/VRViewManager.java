/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jmevr.util;

import com.jme3.app.VRApplication;
import com.jme3.input.vr.OSVR;
import com.jme3.input.vr.OpenVR;
import com.jme3.input.vr.VRAPI;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.post.CartoonSSAO;
import com.jme3.post.Filter;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.SceneProcessor;
import com.jme3.post.filters.FogFilter;
import com.jme3.post.filters.TranslucentBucketFilter;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.shadow.VRDirectionalLightShadowRenderer;
import com.jme3.system.jopenvr.JOpenVRLibrary;
import com.jme3.system.jopenvr.Texture_t;
import com.jme3.system.jopenvr.VRTextureBounds_t;
import com.jme3.system.lwjgl.LwjglWindow;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;

import java.awt.GraphicsEnvironment;
import java.util.logging.Logger;

import osvrrendermanageropengl.OSVR_RenderBufferOpenGL;
import osvrrendermanageropengl.OSVR_ViewportDescription;
import osvrrendermanageropengl.OsvrRenderManagerOpenGLLibrary;

/**
 *
 * @author reden
 */
public class VRViewManager {

	private static final Logger logger = Logger.getLogger(VRViewManager.class.getName());
	
    private final VRApplication app;
    private Camera camLeft,camRight;
    private ViewPort viewPortLeft, viewPortRight;
    private FilterPostProcessor ppLeft, ppRight;
    
    // OpenVR values
    private VRTextureBounds_t texBoundsLeft, texBoundsRight;
    private Texture_t texTypeLeft, texTypeRight;
    
    // OSVR values
    OSVR_RenderBufferOpenGL.ByValue[] osvr_renderBuffer;
    OSVR_ViewportDescription.ByValue osvr_viewDescFull, osvr_viewDescLeft, osvr_viewDescRight;
    Pointer osvr_rmBufferState;
    
    //private static boolean useCustomDistortion;
    private float heightAdjustment;
    
    private Texture2D leftEyeTex, rightEyeTex, dualEyeTex;
    private Texture2D leftEyeDepth, rightEyeDepth;
    
    private final static String LEFT_VIEW_NAME = "Left View";
    private final static String RIGHT_VIEW_NAME = "Right View";

    /*
        do not use. set via preconfigure routine in VRApplication
    */
    @Deprecated
    public static void _setCustomDistortion(boolean set) {
        //useCustomDistortion = set;
    }
    
    public VRViewManager(VRApplication forVRApp){
        app = forVRApp;
    }
    
    private int getRightTexId() {
        return (int)rightEyeTex.getImage().getId();
    }
    
    private int getFullTexId() {
        return (int)dualEyeTex.getImage().getId();
    }
    
    private int getLeftTexId() {
        return (int)leftEyeTex.getImage().getId();
    }
    
    public float getHeightAdjustment() {
        return heightAdjustment;
    }
    
    public void setHeightAdjustment(float amount) {
        heightAdjustment = amount;
    }
    
    private void initTextureSubmitStructs() {
        texTypeLeft = new Texture_t();
        texTypeRight = new Texture_t();
        if( app.getVRHardware() instanceof OpenVR ) {
            texBoundsLeft = new VRTextureBounds_t();
            texBoundsRight = new VRTextureBounds_t();
            // left eye
            texBoundsLeft.uMax = 0.5f;
            texBoundsLeft.uMin = 0f;
            texBoundsLeft.vMax = 1f;
            texBoundsLeft.vMin = 0f;
            texBoundsLeft.setAutoSynch(false);
            texBoundsLeft.setAutoRead(false);
            texBoundsLeft.setAutoWrite(false);
            texBoundsLeft.write();
            // right eye
            texBoundsRight.uMax = 1f;
            texBoundsRight.uMin = 0.5f;
            texBoundsRight.vMax = 1f;
            texBoundsRight.vMin = 0f;
            texBoundsRight.setAutoSynch(false);
            texBoundsRight.setAutoRead(false);
            texBoundsRight.setAutoWrite(false);
            texBoundsRight.write();
            // texture type
            texTypeLeft.eColorSpace = JOpenVRLibrary.EColorSpace.EColorSpace_ColorSpace_Gamma;
            texTypeLeft.eType = JOpenVRLibrary.EGraphicsAPIConvention.EGraphicsAPIConvention_API_OpenGL;
            texTypeLeft.setAutoSynch(false);
            texTypeLeft.setAutoRead(false);
            texTypeLeft.setAutoWrite(false);
            texTypeLeft.handle = -1;
            texTypeRight.eColorSpace = JOpenVRLibrary.EColorSpace.EColorSpace_ColorSpace_Gamma;
            texTypeRight.eType = JOpenVRLibrary.EGraphicsAPIConvention.EGraphicsAPIConvention_API_OpenGL;
            texTypeRight.setAutoSynch(false);
            texTypeRight.setAutoRead(false);
            texTypeRight.setAutoWrite(false);
            texTypeRight.handle = -1;
        } else if( app.getVRHardware() instanceof OSVR ) {
            // must be OSVR
            osvr_renderBuffer = new OSVR_RenderBufferOpenGL.ByValue[2];
            osvr_renderBuffer[OSVR.EYE_LEFT] = new OSVR_RenderBufferOpenGL.ByValue();
            osvr_renderBuffer[OSVR.EYE_RIGHT] = new OSVR_RenderBufferOpenGL.ByValue();
            osvr_renderBuffer[OSVR.EYE_LEFT].setAutoSynch(false);
            osvr_renderBuffer[OSVR.EYE_RIGHT].setAutoSynch(false);
            osvr_viewDescFull = new OSVR_ViewportDescription.ByValue();
            osvr_viewDescFull.setAutoSynch(false);
            osvr_viewDescFull.left = osvr_viewDescFull.lower = 0.0;
            osvr_viewDescFull.width = osvr_viewDescFull.height = 1.0;    
            osvr_viewDescLeft = new OSVR_ViewportDescription.ByValue();
            osvr_viewDescLeft.setAutoSynch(false);
            osvr_viewDescLeft.left = osvr_viewDescLeft.lower = 0.0;
            osvr_viewDescLeft.width = 0.5;
            osvr_viewDescLeft.height = 1.0;    
            osvr_viewDescRight = new OSVR_ViewportDescription.ByValue();
            osvr_viewDescRight.setAutoSynch(false);
            osvr_viewDescRight.left = 0.5;
            osvr_viewDescRight.lower = 0.0;
            osvr_viewDescRight.width = 0.5;
            osvr_viewDescRight.height = 1.0;
            osvr_viewDescRight.write();
            osvr_viewDescLeft.write();
            osvr_viewDescFull.write();
            osvr_renderBuffer[OSVR.EYE_LEFT].depthStencilBufferName = -1;
            osvr_renderBuffer[OSVR.EYE_LEFT].colorBufferName = -1;
            osvr_renderBuffer[OSVR.EYE_RIGHT].depthStencilBufferName = -1;
            osvr_renderBuffer[OSVR.EYE_RIGHT].colorBufferName = -1;
        }
    }
    
    private final PointerByReference grabRBS = new PointerByReference();
    private void registerOSVRBuffer(OSVR_RenderBufferOpenGL.ByValue buf) {
        OsvrRenderManagerOpenGLLibrary.osvrRenderManagerStartRegisterRenderBuffers(grabRBS);
        OsvrRenderManagerOpenGLLibrary.osvrRenderManagerRegisterRenderBufferOpenGL(grabRBS.getValue(), buf);
        OsvrRenderManagerOpenGLLibrary.osvrRenderManagerFinishRegisterRenderBuffers(((OSVR)app.getVRHardware()).getCompositor(), grabRBS.getValue(), (byte)0);
    }
    
    public void sendTextures() {
        if( app.isInVR() ) {
            VRAPI api = app.getVRHardware();
            if( api.getCompositor() != null ) {
                // using the compositor...
                int errl = 0, errr = 0;
                if( app.isInstanceVRRendering() ) {
                    if( texTypeLeft.handle == -1 || texTypeLeft.handle != getFullTexId() ) {
                        texTypeLeft.handle = getFullTexId();
                        if( texTypeLeft.handle != -1 ) {
                            texTypeLeft.write();
                            if( api instanceof OSVR ) {
                                osvr_renderBuffer[OSVR.EYE_LEFT].colorBufferName = texTypeLeft.handle;
                                osvr_renderBuffer[OSVR.EYE_LEFT].depthStencilBufferName = dualEyeTex.getImage().getId();
                                osvr_renderBuffer[OSVR.EYE_LEFT].write();
                                registerOSVRBuffer(osvr_renderBuffer[OSVR.EYE_LEFT]);
                            }
                        }
                    } else {
                        if( api instanceof OpenVR ) {
                            int submitFlag = JOpenVRLibrary.EVRSubmitFlags.EVRSubmitFlags_Submit_Default;
                            errr = ((OpenVR)api).getCompositor().Submit.apply(JOpenVRLibrary.EVREye.EVREye_Eye_Right, texTypeLeft, texBoundsRight, submitFlag);
                            errl = ((OpenVR)api).getCompositor().Submit.apply(JOpenVRLibrary.EVREye.EVREye_Eye_Left, texTypeLeft, texBoundsLeft, submitFlag);
                        } else if( api instanceof OSVR ) {
                            ((OSVR)api).handleRenderBufferPresent(osvr_viewDescLeft, osvr_viewDescRight,
                                                                  osvr_renderBuffer[OSVR.EYE_LEFT], osvr_renderBuffer[OSVR.EYE_LEFT]);
                        }
                    }
                } else if( texTypeLeft.handle == -1 || texTypeRight.handle == -1 ||
                           texTypeLeft.handle != getLeftTexId() || texTypeRight.handle != getRightTexId() ) {
                    texTypeLeft.handle = getLeftTexId();
                    if( texTypeLeft.handle != -1 ) {
                        texTypeLeft.write();
                        if( api instanceof OSVR ) {
                            osvr_renderBuffer[OSVR.EYE_LEFT].colorBufferName = texTypeLeft.handle;
                            if( leftEyeDepth != null ) osvr_renderBuffer[OSVR.EYE_LEFT].depthStencilBufferName = leftEyeDepth.getImage().getId();
                            osvr_renderBuffer[OSVR.EYE_LEFT].write();
                            registerOSVRBuffer(osvr_renderBuffer[OSVR.EYE_LEFT]);
                        }
                    }
                    texTypeRight.handle = getRightTexId();
                    if( texTypeRight.handle != -1 ) {
                        texTypeRight.write();
                        if( api instanceof OSVR ) {
                            osvr_renderBuffer[OSVR.EYE_RIGHT].colorBufferName = texTypeRight.handle;
                            if( rightEyeDepth != null ) osvr_renderBuffer[OSVR.EYE_RIGHT].depthStencilBufferName = rightEyeDepth.getImage().getId();
                            osvr_renderBuffer[OSVR.EYE_RIGHT].write();
                            registerOSVRBuffer(osvr_renderBuffer[OSVR.EYE_RIGHT]);
                        }
                    }                    
                } else {
                    if( api instanceof OpenVR ) {
                        errl = ((OpenVR)api).getCompositor().Submit.apply(JOpenVRLibrary.EVREye.EVREye_Eye_Left, texTypeLeft, null,
                                                               JOpenVRLibrary.EVRSubmitFlags.EVRSubmitFlags_Submit_Default);
                        errr = ((OpenVR)api).getCompositor().Submit.apply(JOpenVRLibrary.EVREye.EVREye_Eye_Right, texTypeRight, null,
                                                               JOpenVRLibrary.EVRSubmitFlags.EVRSubmitFlags_Submit_Default);
                    } else if( api instanceof OSVR ) {
                        ((OSVR)api).handleRenderBufferPresent(osvr_viewDescFull, osvr_viewDescFull,
                                                              osvr_renderBuffer[OSVR.EYE_LEFT], osvr_renderBuffer[OSVR.EYE_RIGHT]);
                    }
                }
                if( errl != 0 ) System.out.println("Submit left compositor error: " + Integer.toString(errl));
                if( errr != 0 ) System.out.println("Submit right compositor error: " + Integer.toString(errr));
            }
        }                
    }

    public Camera getCamLeft() {
        return camLeft;
    }
    
    public Camera getCamRight() {
        return camRight;
    }
    
    public ViewPort getViewPortLeft() {
        return viewPortLeft;
    }
    
    public ViewPort getViewPortRight() {
        return viewPortRight;
    }
    
    public void initialize() {     
    	
    	logger.config("Initializing VR view manager.");
    	
        initTextureSubmitStructs();
        setupCamerasAndViews();        
        setupVRScene();                    
        moveScreenProcessingToEyes();       
        if( app.hasTraditionalGUIOverlay() ) {
        	
            app.getVRMouseManager().init();
            
            // update the pose to position the gui correctly on start
            update(0f);
            app.getVRGUIManager().positionGui();
        }       
        // if we are OSVR, our primary mirror window needs to be the same size as the render manager's output...
        if( app.getVRHardware() instanceof OSVR ) {
            int origWidth = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getWidth();
            int origHeight = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getHeight();
            long window = ((LwjglWindow)app.getContext()).getWindowHandle();
            Vector2f windowSize = new Vector2f();
            ((OSVR)app.getVRHardware()).getRenderSize(windowSize);
            windowSize.x = Math.max(windowSize.x * 2f, camLeft.getWidth());
            org.lwjgl.glfw.GLFW.glfwSetWindowSize(window, (int)windowSize.x, (int)windowSize.y);
            app.getContext().getSettings().setResolution((int)windowSize.x, (int)windowSize.y);
            app.reshape((int)windowSize.x, (int)windowSize.y);            
            org.lwjgl.glfw.GLFW.glfwSetWindowPos(window, origWidth - (int)windowSize.x, 32);
            
            org.lwjgl.glfw.GLFW.glfwFocusWindow(window);
            
            org.lwjgl.glfw.GLFW.glfwSetCursorPos(window, origWidth / 2.0, origHeight / 2.0);
        }       
    }
    
    private float resMult = 1f;
    public void setResolutionMultiplier(float resMult) {
        this.resMult = resMult;
    }
    
    public float getResolutionMuliplier() {
        return resMult;
    }
    
    private void prepareCameraSize(Camera cam, float xMult) {
        Vector2f size = new Vector2f();
        VRAPI vrhmd = app.getVRHardware();

        if( vrhmd == null ) {
            size.x = 1280f;
            size.y = 720f;
        } else {
            vrhmd.getRenderSize(size);
        }
        
        if( size.x < app.getContext().getSettings().getWidth() ) {
            size.x = app.getContext().getSettings().getWidth();
        }
        if( size.y < app.getContext().getSettings().getHeight() ) {
            size.y = app.getContext().getSettings().getHeight();
        }
        
        if( app.isInstanceVRRendering() ) size.x *= 2f;
        
        // other adjustments
        size.x *= xMult;
        size.x *= resMult;
        size.y *= resMult;
        
        if( cam.getWidth() != size.x || cam.getHeight() != size.y ) cam.resize((int)size.x, (int)size.y, false);
    }
    
    /**
     * Replaces rootNode as the main cameras scene with the distortion mesh
     */
    private void setupVRScene(){
        // no special scene to setup if we are doing instancing
        if( app.isInstanceVRRendering() ) {
            // distortion has to be done with compositor here... we want only one pass on our end!
            if( app.getContext().getSettings().isSwapBuffers() ) {
                setupMirrorBuffers(app.getCamera(), dualEyeTex, true);
            }       
            return;
        }
        
        leftEyeTex = (Texture2D)viewPortLeft.getOutputFrameBuffer().getColorBuffer().getTexture();
        rightEyeTex = (Texture2D)viewPortRight.getOutputFrameBuffer().getColorBuffer().getTexture();        
        leftEyeDepth = (Texture2D)viewPortLeft.getOutputFrameBuffer().getDepthBuffer().getTexture();
        rightEyeDepth = (Texture2D)viewPortRight.getOutputFrameBuffer().getDepthBuffer().getTexture();        
        // main viewport is either going to be a distortion scene or nothing
        // mirroring is handled by copying framebuffers
        app.getViewPort().detachScene(app.getRootNode());
        app.getViewPort().detachScene(app.getGuiNode());
        
        // only setup distortion scene if compositor isn't running (or using custom mesh distortion option)
        if( app.getVRHardware().getCompositor() == null ) {
            Node distortionScene = new Node();
            Material leftMat = new Material(app.getAssetManager(), "Common/MatDefs/VR/OpenVR.j3md");
            leftMat.setTexture("Texture", leftEyeTex);
            Geometry leftEye = new Geometry("box", MeshUtil.setupDistortionMesh(JOpenVRLibrary.EVREye.EVREye_Eye_Left, app));
            leftEye.setMaterial(leftMat);
            distortionScene.attachChild(leftEye);

            Material rightMat = new Material(app.getAssetManager(), "Common/MatDefs/VR/OpenVR.j3md");
            rightMat.setTexture("Texture", rightEyeTex);
            Geometry rightEye = new Geometry("box", MeshUtil.setupDistortionMesh(JOpenVRLibrary.EVREye.EVREye_Eye_Right, app));
            rightEye.setMaterial(rightMat);
            distortionScene.attachChild(rightEye);

            distortionScene.updateGeometricState();

            app.getViewPort().attachScene(distortionScene);
            
            //if( useCustomDistortion ) setupFinalFullTexture(app.getViewPort().getCamera());
        }
        
        if( app.getContext().getSettings().isSwapBuffers() ) {
            setupMirrorBuffers(app.getCamera(), leftEyeTex, false);
        }       
    }
    
    //final & temp values for camera calculations
    private final Vector3f finalPosition = new Vector3f();
    private final Quaternion finalRotation = new Quaternion();
    private final Vector3f hmdPos = new Vector3f();
    private final Quaternion hmdRot = new Quaternion();
    
    public void update(float tpf) {
        
        // grab the observer
        Object obs = app.getObserver();
        Quaternion objRot;
        Vector3f objPos;
        if( obs instanceof Camera ) {
            objRot = ((Camera)obs).getRotation();
            objPos = ((Camera)obs).getLocation();
        } else {
            objRot = ((Spatial)obs).getWorldRotation();
            objPos = ((Spatial)obs).getWorldTranslation();
        }
        // grab the hardware handle
        VRAPI dev = app.getVRHardware();
        if( dev != null ) {
            // update the HMD's position & orientation
            dev.updatePose();
            dev.getPositionAndOrientation(hmdPos, hmdRot);
            if( obs != null ) {
                // update hmdPos based on obs rotation
                finalRotation.set(objRot);
                finalRotation.mult(hmdPos, hmdPos);
                finalRotation.multLocal(hmdRot);
            }
            
            finalizeCamera(dev.getHMDVectorPoseLeftEye(), objPos, camLeft);
            finalizeCamera(dev.getHMDVectorPoseRightEye(), objPos, camRight);
        } else {
            camLeft.setFrame(objPos, objRot);
            camRight.setFrame(objPos, objRot);
        }
        
        if( app.hasTraditionalGUIOverlay() ) {
            // update the mouse?
        	app.getVRMouseManager().update(tpf);
        
            // update GUI position?
            if( app.getVRGUIManager().wantsReposition || app.getVRGUIManager().getPositioningMode() != VRGuiManager.POSITIONING_MODE.MANUAL ) {
            	app.getVRGUIManager().positionGuiNow(tpf);
            	app.getVRGUIManager().updateGuiQuadGeometricState();
            }
        }
    }
    
    private void finalizeCamera(Vector3f eyePos, Vector3f obsPosition, Camera cam) {
        finalRotation.mult(eyePos, finalPosition);
        finalPosition.addLocal(hmdPos);
        if( obsPosition != null ) finalPosition.addLocal(obsPosition);
        finalPosition.y += heightAdjustment;
        cam.setFrame(finalPosition, finalRotation);
    }
    
    /*
        handles moving filters from the main view to each eye
    */
    public void moveScreenProcessingToEyes() {
        if( viewPortRight == null ) return;
        syncScreenProcessing(app.getViewPort());
        app.getViewPort().clearProcessors();
    }
    
    /*
        sets the two views to use the list of processors
    */
    public void syncScreenProcessing(ViewPort sourceViewport) {
        if( viewPortRight == null ) return;
        // setup post processing filters
        if( ppRight == null ) {
            ppRight = new FilterPostProcessor(app.getAssetManager());               
            ppLeft =  new FilterPostProcessor(app.getAssetManager());
        }
        // clear out all filters & processors, to start from scratch
        ppRight.removeAllFilters();
        ppLeft.removeAllFilters();
        viewPortLeft.clearProcessors();
        viewPortRight.clearProcessors();
        // if we have no processors to sync, don't add the FilterPostProcessor
        if( sourceViewport.getProcessors().isEmpty() ) return;
        // add post processors we just made, which are empty
        viewPortLeft.addProcessor(ppLeft);
        viewPortRight.addProcessor(ppRight);
        // go through all of the filters in the processors list
        // add them to the left viewport processor & clone them to the right
        for(SceneProcessor sceneProcessor : sourceViewport.getProcessors()) {
            if (sceneProcessor instanceof FilterPostProcessor) {
                for(Filter f : ((FilterPostProcessor)sceneProcessor).getFilterList() ) {
                    if( f instanceof TranslucentBucketFilter ) {
                        // just remove this filter, we will add it at the end manually
                        ((FilterPostProcessor)sceneProcessor).removeFilter(f);
                    } else {
                        ppLeft.addFilter(f);
                        // clone to the right
                        Filter f2;
                        if(f instanceof FogFilter){
                            f2 = FilterUtil.cloneFogFilter((FogFilter)f); 
                        } else if (f instanceof CartoonSSAO ) {
                            f2 = new CartoonSSAO((CartoonSSAO)f);
                        } else if (f instanceof SSAOFilter){
                            f2 = FilterUtil.cloneSSAOFilter((SSAOFilter)f);
                        } else if (f instanceof DirectionalLightShadowFilter){
                            f2 = FilterUtil.cloneDirectionalLightShadowFilter(app.getAssetManager(), (DirectionalLightShadowFilter)f);
                        } else {
                            f2 = f; // dof, bloom, lightscattering etc.
                        }                    
                        ppRight.addFilter(f2);
                    }
                }
            } else if (sceneProcessor instanceof VRDirectionalLightShadowRenderer) {
                // shadow processing
                // TODO: make right shadow processor use same left shadow maps for performance
                VRDirectionalLightShadowRenderer dlsr = (VRDirectionalLightShadowRenderer) sceneProcessor;
                VRDirectionalLightShadowRenderer dlsrRight = dlsr.clone();
                dlsrRight.setLight(dlsr.getLight());
                viewPortRight.getProcessors().add(0, dlsrRight);
                viewPortLeft.getProcessors().add(0, sceneProcessor);
            }
        }
        // make sure each has a translucent filter renderer
        ppLeft.addFilter(new TranslucentBucketFilter());
        ppRight.addFilter(new TranslucentBucketFilter());
    }
    
    private void setupCamerasAndViews() {        
        // get desired frustrum from original camera
        Camera origCam = app.getBaseCamera();        
        float fFar = origCam.getFrustumFar();
        float fNear = origCam.getFrustumNear();
        
        // if we are using OSVR get the eye info here
        if( app.getVRHardware() instanceof OSVR ) {
            ((OSVR)app.getVRHardware()).getEyeInfo();
        }
        
        // restore frustrum on distortion scene cam, if needed
        if( app.isInstanceVRRendering() ) {
            camLeft = origCam;
        } else if( app.compositorAllowed() == false ) {
            origCam.setFrustumFar(100f);
            origCam.setFrustumNear(1f); 
            camLeft = origCam.clone();  
            prepareCameraSize(origCam, 2f);
        } else {
            camLeft = origCam.clone();
        }
        
        camLeft.setFrustumPerspective(app.DEFAULT_FOV, app.DEFAULT_ASPECT, fNear, fFar);                     
                
        prepareCameraSize(camLeft, 1f);
        if( app.getVRHardware() != null ) camLeft.setProjectionMatrix(app.getVRHardware().getHMDMatrixProjectionLeftEye(camLeft));
        //org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_SRGB);
        
        if( app.isInstanceVRRendering() == false ) {
            viewPortLeft = setupViewBuffers(camLeft, LEFT_VIEW_NAME);
            camRight = camLeft.clone();
            if( app.getVRHardware() != null ) camRight.setProjectionMatrix(app.getVRHardware().getHMDMatrixProjectionRightEye(camRight));
            viewPortRight = setupViewBuffers(camRight, RIGHT_VIEW_NAME);
        } else {
            viewPortLeft = app.getViewPort();
            viewPortLeft.attachScene(app.getRootNode());
            camRight = camLeft.clone();
            if( app.getVRHardware() != null ) camRight.setProjectionMatrix(app.getVRHardware().getHMDMatrixProjectionRightEye(camRight));
            
            org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL30.GL_CLIP_DISTANCE0);
            
            //FIXME: [jme-vr] Fix with JMonkey next release
            //RenderManager._VRInstancing_RightCamProjection = camRight.getViewProjectionMatrix();
            
            setupFinalFullTexture(app.getViewPort().getCamera());            
        }
        
        // setup gui
        app.getVRGUIManager().setupGui(camLeft, camRight, viewPortLeft, viewPortRight);
        
        if( app.getVRHardware() != null ) {
            // call these to cache the results internally
        	app.getVRHardware().getHMDMatrixPoseLeftEye();
        	app.getVRHardware().getHMDMatrixPoseRightEye();
        }

    }
    
    private ViewPort setupMirrorBuffers(Camera cam, Texture tex, boolean expand) {        
        Camera clonecam = cam.clone();
        ViewPort viewPort = app.getRenderManager().createPostView("MirrorView", clonecam);
        clonecam.setParallelProjection(true);
        viewPort.setClearFlags(true, true, true);
        viewPort.setBackgroundColor(ColorRGBA.Black);
        Picture pic = new Picture("fullscene");
        pic.setLocalTranslation(-0.75f, -0.5f, 0f);
        if( expand ) {
            pic.setLocalScale(3f, 1f, 1f);
        } else {
            pic.setLocalScale(1.5f, 1f, 1f);            
        }
        pic.setQueueBucket(Bucket.Opaque);
        pic.setTexture(app.getAssetManager(), (Texture2D)tex, false);
        viewPort.attachScene(pic);
        viewPort.setOutputFrameBuffer(null);
        
        pic.updateGeometricState();
        
        return viewPort;
    }
    
    private void setupFinalFullTexture(Camera cam) {
        // create offscreen framebuffer
        FrameBuffer out = new FrameBuffer(cam.getWidth(), cam.getHeight(), 1);
        //offBuffer.setSrgb(true);

        //setup framebuffer's texture
        dualEyeTex = new Texture2D(cam.getWidth(), cam.getHeight(), Image.Format.RGBA8);
        dualEyeTex.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
        dualEyeTex.setMagFilter(Texture.MagFilter.Bilinear);

        //setup framebuffer to use texture
        out.setDepthBuffer(Image.Format.Depth);
        out.setColorTexture(dualEyeTex);       

        ViewPort viewPort = this.app.getViewPort();
        viewPort.setClearFlags(true, true, true);
        viewPort.setBackgroundColor(ColorRGBA.Black);
        viewPort.setOutputFrameBuffer(out);
        
    }
    
    private ViewPort setupViewBuffers(Camera cam, String viewName){
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
        
        ViewPort viewPort = app.getRenderManager().createPreView(viewName, cam);
        viewPort.setClearFlags(true, true, true);
        viewPort.setBackgroundColor(ColorRGBA.Black);
        viewPort.attachScene(this.app.getRootNode());
        //set viewport to render to offscreen framebuffer
        viewPort.setOutputFrameBuffer(offBufferLeft);
        return viewPort;
    }
}

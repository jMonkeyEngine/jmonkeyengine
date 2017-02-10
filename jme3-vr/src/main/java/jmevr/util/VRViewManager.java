/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jmevr.util;

import com.jme3.app.Application;
import com.jme3.app.VRAppState;
import com.jme3.app.VRApplication;
import com.jme3.app.state.AppState;
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
import com.jme3.system.jopenvr.OpenVRUtil;
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
import java.util.Iterator;
import java.util.logging.Logger;

import osvrrendermanageropengl.OSVR_RenderBufferOpenGL;
import osvrrendermanageropengl.OSVR_ViewportDescription;
import osvrrendermanageropengl.OsvrRenderManagerOpenGLLibrary;

/**
 * A VR view manager. This class enable to submit 3D views to the VR compositor.
 * @author reden - phr00t - https://github.com/phr00t
 * @author Julien Seinturier - (c) 2016 - JOrigin project - <a href="http://www.jorigin.org">http:/www.jorigin.org</a>
 */
public class VRViewManager {

	private static final Logger logger = Logger.getLogger(VRViewManager.class.getName());
	
    /**
     * The name of the left view.
     */
    public final static String LEFT_VIEW_NAME = "Left View";
    
    /**
     * The name of the right view.
     */
    public final static String RIGHT_VIEW_NAME = "Right View";
	
    private VRAppState app;
    private Application application;
    
    private Camera leftCamera;
    private ViewPort leftViewport;
    private FilterPostProcessor leftPostProcessor;
    private Texture2D leftEyeTexture;
    private Texture2D leftEyeDepth;
    
    private Camera rightCamera;
    private ViewPort rightViewport;
    private FilterPostProcessor rightPostProcessor;
    private Texture2D rightEyeTexture;
    private Texture2D rightEyeDepth;
    
    // OpenVR values
    private VRTextureBounds_t leftTextureBounds;
    private Texture_t leftTextureType;
    
    private VRTextureBounds_t rightTextureBounds;
    private Texture_t rightTextureType;
    
    // OSVR values
    OSVR_RenderBufferOpenGL.ByValue[] osvr_renderBuffer;
    OSVR_ViewportDescription.ByValue osvr_viewDescFull;
    OSVR_ViewportDescription.ByValue osvr_viewDescLeft;
    OSVR_ViewportDescription.ByValue osvr_viewDescRight;
    Pointer osvr_rmBufferState;
    
    //private static boolean useCustomDistortion;
    private float heightAdjustment;

    private Texture2D dualEyeTex;

    private final PointerByReference grabRBS = new PointerByReference();
    
    private float resMult = 1f;
    
    //final & temp values for camera calculations
    private final Vector3f finalPosition   = new Vector3f();
    private final Quaternion finalRotation = new Quaternion();
    private final Vector3f hmdPos          = new Vector3f();
    private final Quaternion hmdRot        = new Quaternion();
    
    /**
     * Create a new VR view manager attached to the given {@link VRAppState VR app state}.<br>
     * in order to be used, this manager has to be attached to an app state and to an application.
     */
    public VRViewManager(){
    }
    
    /**
     * Attach this manager to the given {@link VRAppState app state} and the given {@link Application application}.
     * The application has to be the one that the app state is attached.
     * This method should be called from the {@link AppState#initialize(com.jme3.app.state.AppStateManager, Application) initialize} 
     * method of the {@link AppState} instance.
     * @param app the {@link VRAppState VR app state} to which this manager is linked.
     * @param application the {@link Application} which the app state is attached.
     */
    public void attach(VRAppState app, Application application){
        this.app         = app;
        this.application = application;
    }
    
    /**
     * Get the {@link Camera camera} attached to the left eye.
     * @return the {@link Camera camera} attached to the left eye.
     * @see #getRightCamera()
     */
    public Camera getLeftCamera() {
        return leftCamera;
    }
    
    /**
     * Get the {@link Camera camera} attached to the right eye.
     * @return the {@link Camera camera} attached to the right eye.
     * @see #getLeftCamera()
     */
    public Camera getRightCamera() {
        return rightCamera;
    }
    
    /**
     * Get the {@link ViewPort viewport} attached to the left eye.
     * @return the {@link ViewPort viewport} attached to the left eye.
     * @see #getRightViewport()
     */
    public ViewPort getLeftViewport() {
        return leftViewport;
    }
    
    /**
     * Get the {@link ViewPort viewport} attached to the right eye.
     * @return the {@link ViewPort viewport} attached to the right eye.
     * @see #getLeftViewport()
     */
    public ViewPort getRightViewport() {
        return rightViewport;
    }
    
    /**
     * Get the identifier of the left eye texture.
     * @return the identifier of the left eye texture.
     * @see #getRightTexId()
     * @see #getFullTexId()
     */
    private int getLeftTexId() {
        return (int)leftEyeTexture.getImage().getId();
    }
    
    /**
     * Get the identifier of the right eye texture.
     * @return the identifier of the right eye texture.
     * @see #getLeftTexId()
     * @see #getFullTexId()
     */
    private int getRightTexId() {
        return (int)rightEyeTexture.getImage().getId();
    }
    
    /**
     * Get the identifier of the full (dual eye) texture.
     * @return the identifier of the full (dual eye) texture.
     * @see #getLeftTexId()
     * @see #getRightTexId()
     */
    private int getFullTexId() {
        return (int)dualEyeTex.getImage().getId();
    }
    
    /**
     * Get the height adjustment to apply to the cameras before rendering.
     * @return the height adjustment to apply to the cameras before rendering.
     * @see #setHeightAdjustment(float)
     */
    public float getHeightAdjustment() {
        return heightAdjustment;
    }
    
    /**
     * Set the height adjustment to apply to the cameras before rendering.
     * @param amount the height adjustment to apply to the cameras before rendering.
     * @see #getHeightAdjustment()
     */
    public void setHeightAdjustment(float amount) {
        heightAdjustment = amount;
    }
    
    /**
     * Get the resolution multiplier.
     * @return the resolution multiplier.
     * @see #setResolutionMultiplier(float)
     */
    public float getResolutionMuliplier() {
        return resMult;
    }
    
    /**
     * Set the resolution multiplier.
     * @param resMult the resolution multiplier.
     * @see #getResolutionMuliplier()
     */
    public void setResolutionMultiplier(float resMult) {
        this.resMult = resMult;
    }
    
    /**
     * Initialize the system binds of the textures.
     */
    private void initTextureSubmitStructs() {
        leftTextureType = new Texture_t();
        rightTextureType = new Texture_t();
        

        if( app.getVRHardware() instanceof OpenVR ) {
            leftTextureBounds = new VRTextureBounds_t();
            rightTextureBounds = new VRTextureBounds_t();
            // left eye
            leftTextureBounds.uMax = 0.5f;
            leftTextureBounds.uMin = 0f;
            leftTextureBounds.vMax = 1f;
            leftTextureBounds.vMin = 0f;
            leftTextureBounds.setAutoSynch(false);
            leftTextureBounds.setAutoRead(false);
            leftTextureBounds.setAutoWrite(false);
            leftTextureBounds.write();
            // right eye
            rightTextureBounds.uMax = 1f;
            rightTextureBounds.uMin = 0.5f;
            rightTextureBounds.vMax = 1f;
            rightTextureBounds.vMin = 0f;
            rightTextureBounds.setAutoSynch(false);
            rightTextureBounds.setAutoRead(false);
            rightTextureBounds.setAutoWrite(false);
            rightTextureBounds.write();
            // texture type
            // FIXME: Synchronize with JMonkey given texture (at this time is linear but was Gamma with phr00t implementation)
            leftTextureType.eColorSpace = JOpenVRLibrary.EColorSpace.EColorSpace_ColorSpace_Gamma;
            //leftTextureType.eColorSpace = JOpenVRLibrary.EColorSpace.EColorSpace_ColorSpace_Linear;
            leftTextureType.eType = JOpenVRLibrary.ETextureType.ETextureType_TextureType_OpenGL;
            leftTextureType.setAutoSynch(false);
            leftTextureType.setAutoRead(false);
            leftTextureType.setAutoWrite(false);
            leftTextureType.handle = -1;
            // FIXME: Synchronize with JMonkey given texture (at this time is linear but was Gamma with phr00t implementation)
            rightTextureType.eColorSpace = JOpenVRLibrary.EColorSpace.EColorSpace_ColorSpace_Gamma;
            //rightTextureType.eColorSpace = JOpenVRLibrary.EColorSpace.EColorSpace_ColorSpace_Linear;
            rightTextureType.eType = JOpenVRLibrary.ETextureType.ETextureType_TextureType_OpenGL;
            rightTextureType.setAutoSynch(false);
            rightTextureType.setAutoRead(false);
            rightTextureType.setAutoWrite(false);
            rightTextureType.handle = -1;
            
            
            logger.config("Init eyes native texture binds");
            logger.config("  Left eye texture");
            logger.config("           address: "+leftTextureType.getPointer());
            logger.config("              size: "+leftTextureType.size()+" bytes");
            logger.config("       color space: "+OpenVRUtil.getEColorSpaceString(leftTextureType.eColorSpace));
            logger.config("              type: "+OpenVRUtil.getETextureTypeString(leftTextureType.eType));
            logger.config("         auto read: "+leftTextureType.getAutoRead());
            logger.config("        auto write: "+leftTextureType.getAutoWrite());
            logger.config("    handle address: "+leftTextureType.handle);
            logger.config("      handle value: "+leftTextureType.handle);
            logger.config("");
            logger.config("  Right eye texture");
            logger.config("           address: "+rightTextureType.getPointer());
            logger.config("              size: "+rightTextureType.size()+" bytes");
            logger.config("       color space: "+OpenVRUtil.getEColorSpaceString(rightTextureType.eColorSpace));
            logger.config("              type: "+OpenVRUtil.getETextureTypeString(rightTextureType.eType));
            logger.config("         auto read: "+rightTextureType.getAutoRead());
            logger.config("        auto write: "+rightTextureType.getAutoWrite());
            logger.config("    handle address: "+rightTextureType.handle);
            logger.config("      handle value: "+rightTextureType.handle);
            
            
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

    /**
     * Register the OSVR OpenGL buffer.
     * @param buf the OSVR OpenGL buffer.
     */
    private void registerOSVRBuffer(OSVR_RenderBufferOpenGL.ByValue buf) {
        OsvrRenderManagerOpenGLLibrary.osvrRenderManagerStartRegisterRenderBuffers(grabRBS);
        OsvrRenderManagerOpenGLLibrary.osvrRenderManagerRegisterRenderBufferOpenGL(grabRBS.getValue(), buf);
        OsvrRenderManagerOpenGLLibrary.osvrRenderManagerFinishRegisterRenderBuffers(((OSVR)app.getVRHardware()).getCompositor(), grabRBS.getValue(), (byte)0);
    }
    
    /**
     * Send the textures to the two eyes.
     */
    public void sendTextures() {
        if( app.isInVR() ) {
            VRAPI api = app.getVRHardware();
            if( api.getCompositor() != null ) {
                // using the compositor...
                int errl = 0, errr = 0;
                if( app.isInstanceVRRendering() ) {
                    if( leftTextureType.handle == -1 || leftTextureType.handle != getFullTexId() ) {
                    	leftTextureType.handle = getFullTexId();
                        if( leftTextureType.handle != -1 ) {
                            leftTextureType.write();
                            if( api instanceof OSVR ) {
                                osvr_renderBuffer[OSVR.EYE_LEFT].colorBufferName = leftTextureType.handle;
                                osvr_renderBuffer[OSVR.EYE_LEFT].depthStencilBufferName = dualEyeTex.getImage().getId();
                                osvr_renderBuffer[OSVR.EYE_LEFT].write();
                                registerOSVRBuffer(osvr_renderBuffer[OSVR.EYE_LEFT]);
                            }
                        }
                    } else {
                        if( api instanceof OpenVR ) {
                            int submitFlag = JOpenVRLibrary.EVRSubmitFlags.EVRSubmitFlags_Submit_Default;
                            errr = ((OpenVR)api).getCompositor().Submit.apply(JOpenVRLibrary.EVREye.EVREye_Eye_Right, leftTextureType, rightTextureBounds, submitFlag);
                            errl = ((OpenVR)api).getCompositor().Submit.apply(JOpenVRLibrary.EVREye.EVREye_Eye_Left, leftTextureType, leftTextureBounds, submitFlag);
                        } else if( api instanceof OSVR ) {
                            ((OSVR)api).handleRenderBufferPresent(osvr_viewDescLeft, osvr_viewDescRight,
                                                                  osvr_renderBuffer[OSVR.EYE_LEFT], osvr_renderBuffer[OSVR.EYE_LEFT]);
                        }
                    }
                } else if( leftTextureType.handle == -1 || rightTextureType.handle == -1 ||
                           leftTextureType.handle != getLeftTexId() || rightTextureType.handle != getRightTexId() ) {
                    leftTextureType.handle = getLeftTexId();
                    if( leftTextureType.handle != -1 ) {
                    	logger.fine("Writing Left texture to native memory at " + leftTextureType.getPointer());
                        leftTextureType.write();
                        if( api instanceof OSVR ) {
                            osvr_renderBuffer[OSVR.EYE_LEFT].colorBufferName = leftTextureType.handle;
                            if( leftEyeDepth != null ) osvr_renderBuffer[OSVR.EYE_LEFT].depthStencilBufferName = leftEyeDepth.getImage().getId();
                            osvr_renderBuffer[OSVR.EYE_LEFT].write();
                            registerOSVRBuffer(osvr_renderBuffer[OSVR.EYE_LEFT]);
                        }
                    }
                    rightTextureType.handle = getRightTexId();
                    if( rightTextureType.handle != -1 ) {
                    	logger.fine("Writing Right texture to native memory at " + leftTextureType.getPointer());
                        rightTextureType.write();
                        if( api instanceof OSVR ) {
                            osvr_renderBuffer[OSVR.EYE_RIGHT].colorBufferName = rightTextureType.handle;
                            if( rightEyeDepth != null ) osvr_renderBuffer[OSVR.EYE_RIGHT].depthStencilBufferName = rightEyeDepth.getImage().getId();
                            osvr_renderBuffer[OSVR.EYE_RIGHT].write();
                            registerOSVRBuffer(osvr_renderBuffer[OSVR.EYE_RIGHT]);
                        }
                    }                    
                } else {
                    if( api instanceof OpenVR ) {
                        errl = ((OpenVR)api).getCompositor().Submit.apply(JOpenVRLibrary.EVREye.EVREye_Eye_Left, leftTextureType, null,
                                                               JOpenVRLibrary.EVRSubmitFlags.EVRSubmitFlags_Submit_Default);
                        errr = ((OpenVR)api).getCompositor().Submit.apply(JOpenVRLibrary.EVREye.EVREye_Eye_Right, rightTextureType, null,
                                                               JOpenVRLibrary.EVRSubmitFlags.EVRSubmitFlags_Submit_Default);
                    } else if( api instanceof OSVR ) {
                        ((OSVR)api).handleRenderBufferPresent(osvr_viewDescFull, osvr_viewDescFull,
                                                              osvr_renderBuffer[OSVR.EYE_LEFT], osvr_renderBuffer[OSVR.EYE_RIGHT]);
                    }
                }
                
                if( errl != 0 ){
                	logger.severe("Submit to left compositor error: " + OpenVRUtil.getEVRCompositorErrorString(errl)+" ("+Integer.toString(errl)+")");
                	logger.severe("  Texture color space: "+OpenVRUtil.getEColorSpaceString(leftTextureType.eColorSpace));
                	logger.severe("  Texture type: "+OpenVRUtil.getETextureTypeString(leftTextureType.eType));
                	logger.severe("  Texture handle: "+leftTextureType.handle);
                	
                    logger.severe("  Left eye texture "+leftEyeTexture.getName()+" ("+leftEyeTexture.getImage().getId()+")");
                    logger.severe("                 Type: "+leftEyeTexture.getType());
                    logger.severe("                 Size: "+leftEyeTexture.getImage().getWidth()+"x"+leftEyeTexture.getImage().getHeight());
                    logger.severe("          Image depth: "+leftEyeTexture.getImage().getDepth());
                    logger.severe("         Image format: "+leftEyeTexture.getImage().getFormat());
                    logger.severe("    Image color space: "+leftEyeTexture.getImage().getColorSpace());
                	
                }
                
                if( errr != 0 ){
                	logger.severe("Submit to right compositor error: " + OpenVRUtil.getEVRCompositorErrorString(errl)+" ("+Integer.toString(errl)+")");
                	logger.severe("  Texture color space: "+OpenVRUtil.getEColorSpaceString(rightTextureType.eColorSpace));
                	logger.severe("  Texture type: "+OpenVRUtil.getETextureTypeString(rightTextureType.eType));
                	logger.severe("  Texture handle: "+rightTextureType.handle);
                	
                    logger.severe("  Right eye texture "+rightEyeTexture.getName()+" ("+rightEyeTexture.getImage().getId()+")");
                    logger.severe("                 Type: "+rightEyeTexture.getType());
                    logger.severe("                 Size: "+rightEyeTexture.getImage().getWidth()+"x"+rightEyeTexture.getImage().getHeight());
                    logger.severe("          Image depth: "+rightEyeTexture.getImage().getDepth());
                    logger.severe("         Image format: "+rightEyeTexture.getImage().getFormat());
                    logger.severe("    Image color space: "+rightEyeTexture.getImage().getColorSpace());
                }
            }
        }                
    }


    /**
     * Initialize the VR view manager.
     */
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
            long window = ((LwjglWindow)application.getContext()).getWindowHandle();
            Vector2f windowSize = new Vector2f();
            ((OSVR)app.getVRHardware()).getRenderSize(windowSize);
            windowSize.x = Math.max(windowSize.x * 2f, leftCamera.getWidth());
            org.lwjgl.glfw.GLFW.glfwSetWindowSize(window, (int)windowSize.x, (int)windowSize.y);
            application.getContext().getSettings().setResolution((int)windowSize.x, (int)windowSize.y);
            
            if (application.getRenderManager() != null) {
            	application.getRenderManager().notifyReshape((int)windowSize.x, (int)windowSize.y);
            }
                   
            org.lwjgl.glfw.GLFW.glfwSetWindowPos(window, origWidth - (int)windowSize.x, 32);
            
            org.lwjgl.glfw.GLFW.glfwFocusWindow(window);
            
            org.lwjgl.glfw.GLFW.glfwSetCursorPos(window, origWidth / 2.0, origHeight / 2.0);
        }  
        
        logger.config("Initialized VR view manager [SUCCESS]");
    }
    
    /**
     * Prepare the size of the given {@link Camera camera} to adapt it to the underlying rendering context.
     * @param cam the {@link Camera camera} to prepare.
     * @param xMult the camera width multiplier.
     */
    private void prepareCameraSize(Camera cam, float xMult) {
        Vector2f size = new Vector2f();
        VRAPI vrhmd = app.getVRHardware();

        if( vrhmd == null ) {
            size.x = 1280f;
            size.y = 720f;
        } else {
            vrhmd.getRenderSize(size);
        }
        
        if( size.x < application.getContext().getSettings().getWidth() ) {
            size.x = application.getContext().getSettings().getWidth();
        }
        if( size.y < application.getContext().getSettings().getHeight() ) {
            size.y = application.getContext().getSettings().getHeight();
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
            if( application.getContext().getSettings().isSwapBuffers() ) {
                setupMirrorBuffers(app.getCamera(), dualEyeTex, true);
            }       
            return;
        }
        
        leftEyeTexture  = (Texture2D) leftViewport.getOutputFrameBuffer().getColorBuffer().getTexture();
        rightEyeTexture = (Texture2D)rightViewport.getOutputFrameBuffer().getColorBuffer().getTexture();        
        leftEyeDepth    = (Texture2D) leftViewport.getOutputFrameBuffer().getDepthBuffer().getTexture();
        rightEyeDepth   = (Texture2D)rightViewport.getOutputFrameBuffer().getDepthBuffer().getTexture();        
      
        // main viewport is either going to be a distortion scene or nothing
        // mirroring is handled by copying framebuffers
        Iterator<Spatial> spatialIter = application.getViewPort().getScenes().iterator();
        while(spatialIter.hasNext()){
        	application.getViewPort().detachScene(spatialIter.next());
        }
        
        spatialIter = application.getGuiViewPort().getScenes().iterator();
        while(spatialIter.hasNext()){
        	application.getGuiViewPort().detachScene(spatialIter.next());
        }
        
        // only setup distortion scene if compositor isn't running (or using custom mesh distortion option)
        if( app.getVRHardware().getCompositor() == null ) {
            Node distortionScene = new Node();
            Material leftMat = new Material(application.getAssetManager(), "Common/MatDefs/VR/OpenVR.j3md");
            leftMat.setTexture("Texture", leftEyeTexture);
            Geometry leftEye = new Geometry("box", MeshUtil.setupDistortionMesh(JOpenVRLibrary.EVREye.EVREye_Eye_Left, app.getVRHardware()));
            leftEye.setMaterial(leftMat);
            distortionScene.attachChild(leftEye);

            Material rightMat = new Material(application.getAssetManager(), "Common/MatDefs/VR/OpenVR.j3md");
            rightMat.setTexture("Texture", rightEyeTexture);
            Geometry rightEye = new Geometry("box", MeshUtil.setupDistortionMesh(JOpenVRLibrary.EVREye.EVREye_Eye_Right, app.getVRHardware()));
            rightEye.setMaterial(rightMat);
            distortionScene.attachChild(rightEye);

            distortionScene.updateGeometricState();

            application.getViewPort().attachScene(distortionScene);
            
            //if( useCustomDistortion ) setupFinalFullTexture(app.getViewPort().getCamera());
        }
        
        if( application.getContext().getSettings().isSwapBuffers() ) {
            setupMirrorBuffers(app.getCamera(), leftEyeTexture, false);
        }       
    }
    
    /**
     * Update the VR view manager. 
     * This method is called by the attached {@link VRApplication VR application} and should not be called manually.
     * @param tpf the time per frame.
     */
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
            
            finalizeCamera(dev.getHMDVectorPoseLeftEye(), objPos, leftCamera);
            finalizeCamera(dev.getHMDVectorPoseRightEye(), objPos, rightCamera);
        } else {
            leftCamera.setFrame(objPos, objRot);
            rightCamera.setFrame(objPos, objRot);
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
    
    /**
     * Place the camera within the scene.
     * @param eyePos the eye position.
     * @param obsPosition the observer position.
     * @param cam the camera to place.
     */
    private void finalizeCamera(Vector3f eyePos, Vector3f obsPosition, Camera cam) {
        finalRotation.mult(eyePos, finalPosition);
        finalPosition.addLocal(hmdPos);
        if( obsPosition != null ) finalPosition.addLocal(obsPosition);
        finalPosition.y += heightAdjustment;
        cam.setFrame(finalPosition, finalRotation);
    }
    
    /**
     * Handles moving filters from the main view to each eye
     */
    public void moveScreenProcessingToEyes() {
        if( rightViewport == null ) return;
        syncScreenProcessing(application.getViewPort());
        application.getViewPort().clearProcessors();
    }
    
    /**
     * Sets the two views to use the list of {@link SceneProcessor processors}.
     * @param sourceViewport the {@link ViewPort viewport} that contains the processors to use.
     */
    public void syncScreenProcessing(ViewPort sourceViewport) {
        if( rightViewport == null ) return;
        // setup post processing filters
        if( rightPostProcessor == null ) {
            rightPostProcessor = new FilterPostProcessor(application.getAssetManager());               
            leftPostProcessor =  new FilterPostProcessor(application.getAssetManager());
        }
        // clear out all filters & processors, to start from scratch
        rightPostProcessor.removeAllFilters();
        leftPostProcessor.removeAllFilters();
        leftViewport.clearProcessors();
        rightViewport.clearProcessors();
        // if we have no processors to sync, don't add the FilterPostProcessor
        if( sourceViewport.getProcessors().isEmpty() ) return;
        // add post processors we just made, which are empty
        leftViewport.addProcessor(leftPostProcessor);
        rightViewport.addProcessor(rightPostProcessor);
        // go through all of the filters in the processors list
        // add them to the left viewport processor & clone them to the right
        for(SceneProcessor sceneProcessor : sourceViewport.getProcessors()) {
            if (sceneProcessor instanceof FilterPostProcessor) {
                for(Filter f : ((FilterPostProcessor)sceneProcessor).getFilterList() ) {
                    if( f instanceof TranslucentBucketFilter ) {
                        // just remove this filter, we will add it at the end manually
                        ((FilterPostProcessor)sceneProcessor).removeFilter(f);
                    } else {
                        leftPostProcessor.addFilter(f);
                        // clone to the right
                        Filter f2;
                        if(f instanceof FogFilter){
                            f2 = FilterUtil.cloneFogFilter((FogFilter)f); 
                        } else if (f instanceof CartoonSSAO ) {
                            f2 = new CartoonSSAO((CartoonSSAO)f);
                        } else if (f instanceof SSAOFilter){
                            f2 = FilterUtil.cloneSSAOFilter((SSAOFilter)f);
                        } else if (f instanceof DirectionalLightShadowFilter){
                            f2 = FilterUtil.cloneDirectionalLightShadowFilter(application.getAssetManager(), (DirectionalLightShadowFilter)f);
                        } else {
                            f2 = f; // dof, bloom, lightscattering etc.
                        }                    
                        rightPostProcessor.addFilter(f2);
                    }
                }
            } else if (sceneProcessor instanceof VRDirectionalLightShadowRenderer) {
                // shadow processing
                // TODO: make right shadow processor use same left shadow maps for performance
                VRDirectionalLightShadowRenderer dlsr = (VRDirectionalLightShadowRenderer) sceneProcessor;
                VRDirectionalLightShadowRenderer dlsrRight = dlsr.clone();
                dlsrRight.setLight(dlsr.getLight());
                rightViewport.getProcessors().add(0, dlsrRight);
                leftViewport.getProcessors().add(0, sceneProcessor);
            }
        }
        // make sure each has a translucent filter renderer
        leftPostProcessor.addFilter(new TranslucentBucketFilter());
        rightPostProcessor.addFilter(new TranslucentBucketFilter());
    }
    
    private void setupCamerasAndViews() {        
        // get desired frustrum from original camera
        Camera origCam = app.getCamera();        
        float fFar = origCam.getFrustumFar();
        float fNear = origCam.getFrustumNear();
        
        // if we are using OSVR get the eye info here
        if( app.getVRHardware() instanceof OSVR ) {
            ((OSVR)app.getVRHardware()).getEyeInfo();
        }
        
        // restore frustrum on distortion scene cam, if needed
        if( app.isInstanceVRRendering() ) {
            leftCamera = origCam;
        } else if( app.compositorAllowed() == false ) {
            origCam.setFrustumFar(100f);
            origCam.setFrustumNear(1f); 
            leftCamera = origCam.clone();  
            prepareCameraSize(origCam, 2f);
        } else {
            leftCamera = origCam.clone();
        }
        
        leftCamera.setFrustumPerspective(app.getDefaultFOV(), app.getDefaultAspect(), fNear, fFar);                     
                
        prepareCameraSize(leftCamera, 1f);
        if( app.getVRHardware() != null ) leftCamera.setProjectionMatrix(app.getVRHardware().getHMDMatrixProjectionLeftEye(leftCamera));
        //org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_SRGB);
        
        if( !app.isInstanceVRRendering()) {
            leftViewport = setupViewBuffers(leftCamera, LEFT_VIEW_NAME);
            rightCamera = leftCamera.clone();
            if( app.getVRHardware() != null ){
            	rightCamera.setProjectionMatrix(app.getVRHardware().getHMDMatrixProjectionRightEye(rightCamera));
            }
            rightViewport = setupViewBuffers(rightCamera, RIGHT_VIEW_NAME);
        } else {
        	
        	System.err.println("[VRViewManager] THIS CODE NEED CHANGES !!!");
            leftViewport = application.getViewPort();
            //leftViewport.attachScene(app.getRootNode());
            rightCamera = leftCamera.clone();
            if( app.getVRHardware() != null ){
            	rightCamera.setProjectionMatrix(app.getVRHardware().getHMDMatrixProjectionRightEye(rightCamera));
            }
            
            org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL30.GL_CLIP_DISTANCE0);
            
            //FIXME: [jme-vr] Fix with JMonkey next release
            //RenderManager._VRInstancing_RightCamProjection = camRight.getViewProjectionMatrix();
            setupFinalFullTexture(application.getViewPort().getCamera());            
        }
        
        // setup gui
        app.getVRGUIManager().setupGui(leftCamera, rightCamera, leftViewport, rightViewport);
        
        if( app.getVRHardware() != null ) {
            // call these to cache the results internally
        	app.getVRHardware().getHMDMatrixPoseLeftEye();
        	app.getVRHardware().getHMDMatrixPoseRightEye();
        }

    }
    
    private ViewPort setupMirrorBuffers(Camera cam, Texture tex, boolean expand) {        
        Camera clonecam = cam.clone();
        ViewPort viewPort = application.getRenderManager().createPostView("MirrorView", clonecam);
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
        pic.setTexture(application.getAssetManager(), (Texture2D)tex, false);
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

        logger.config("Dual eye texture "+dualEyeTex.getName()+" ("+dualEyeTex.getImage().getId()+")");
        logger.config("               Type: "+dualEyeTex.getType());
        logger.config("               Size: "+dualEyeTex.getImage().getWidth()+"x"+dualEyeTex.getImage().getHeight());
        logger.config("        Image depth: "+dualEyeTex.getImage().getDepth());
        logger.config("       Image format: "+dualEyeTex.getImage().getFormat());
        logger.config("  Image color space: "+dualEyeTex.getImage().getColorSpace());
        
        //setup framebuffer to use texture
        out.setDepthBuffer(Image.Format.Depth);
        out.setColorTexture(dualEyeTex);       

        ViewPort viewPort = application.getViewPort();
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
        
        ViewPort viewPort = application.getRenderManager().createPreView(viewName, cam);
        viewPort.setClearFlags(true, true, true);
        viewPort.setBackgroundColor(ColorRGBA.Black);
        
        Iterator<Spatial> spatialIter = application.getViewPort().getScenes().iterator();
        while(spatialIter.hasNext()){
        	viewPort.attachScene(spatialIter.next());
        }

        //set viewport to render to offscreen framebuffer
        viewPort.setOutputFrameBuffer(offBufferLeft);
        return viewPort;
    }
}

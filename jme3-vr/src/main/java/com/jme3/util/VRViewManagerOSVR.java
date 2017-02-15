package com.jme3.util;

import java.awt.GraphicsEnvironment;
import java.util.Iterator;
import java.util.logging.Logger;

import com.jme3.app.VREnvironment;
import com.jme3.input.vr.OSVR;
import com.jme3.input.vr.VRAPI;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.post.CartoonSSAO;
import com.jme3.post.Filter;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.FilterUtil;
import com.jme3.post.SceneProcessor;
import com.jme3.post.filters.FogFilter;
import com.jme3.post.filters.TranslucentBucketFilter;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.shadow.VRDirectionalLightShadowRenderer;
import com.jme3.system.jopenvr.DistortionCoordinates_t;
import com.jme3.system.jopenvr.JOpenVRLibrary;
import com.jme3.system.jopenvr.OpenVRUtil;
import com.jme3.system.jopenvr.Texture_t;
import com.jme3.system.jopenvr.VR_IVRSystem_FnTable;
import com.jme3.system.lwjgl.LwjglWindow;
import com.jme3.system.osvr.osvrrendermanageropengl.OSVR_RenderBufferOpenGL;
import com.jme3.system.osvr.osvrrendermanageropengl.OSVR_ViewportDescription;
import com.jme3.system.osvr.osvrrendermanageropengl.OsvrRenderManagerOpenGLLibrary;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;

public class VRViewManagerOSVR extends AbstractVRViewManager{
	private static final Logger logger = Logger.getLogger(VRViewManagerOpenVR.class.getName());
    
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
    private Texture_t leftTextureType;
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
     * Create a new VR view manager attached to the given {@link VREnvironment VR environment}.
     * @param environment the {@link VREnvironment VR environment} to which this view manager is attached.
     */
    public VRViewManagerOSVR(VREnvironment environment){
    	this.environment = environment;
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
    protected int getLeftTexId() {
        return (int)leftEyeTexture.getImage().getId();
    }
    
    /**
     * Get the identifier of the right eye texture.
     * @return the identifier of the right eye texture.
     * @see #getLeftTexId()
     * @see #getFullTexId()
     */
    protected int getRightTexId() {
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

    /**
     * Register the OSVR OpenGL buffer.
     * @param buf the OSVR OpenGL buffer.
     */
    private void registerOSVRBuffer(OSVR_RenderBufferOpenGL.ByValue buf) {
    	
    	if (environment != null){
    		OsvrRenderManagerOpenGLLibrary.osvrRenderManagerStartRegisterRenderBuffers(grabRBS);
            OsvrRenderManagerOpenGLLibrary.osvrRenderManagerRegisterRenderBufferOpenGL(grabRBS.getValue(), buf);
            OsvrRenderManagerOpenGLLibrary.osvrRenderManagerFinishRegisterRenderBuffers(((OSVR)environment.getVRHardware()).getCompositor(), grabRBS.getValue(), (byte)0);

    	} else {
    		throw new IllegalStateException("This VR view manager is not attached to any VR environment.");
    	}
    }
    
    /**
     * Send the textures to the two eyes.
     */
    public void postRender() {
    	
    	if (environment != null){
    		if( environment.isInVR() ) {
                VRAPI api = environment.getVRHardware();
                if( api.getCompositor() != null ) {
                    // using the compositor...
                    int errl = 0, errr = 0;
                    if( environment.isInstanceRendering() ) {
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
                            if( api instanceof OSVR ) {
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
                        if( api instanceof OSVR ) {
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
    	} else {
    		throw new IllegalStateException("This VR view manager is not attached to any VR environment.");
    	}          
    }


    /**
     * Initialize the VR view manager.
     */
    public void initialize() {     
    	
    	logger.config("Initializing VR view manager.");
    	
    	if (environment != null){
    		initTextureSubmitStructs();
            setupCamerasAndViews();        
            setupVRScene();                    
            moveScreenProcessingToEyes();       
            if( environment.hasTraditionalGUIOverlay() ) {
            	
            	environment.getVRMouseManager().initialize();
                
                // update the pose to position the gui correctly on start
                update(0f);
                environment.getVRGUIManager().positionGui();
            }   
            
            if (environment.getApplication() != null){
                // if we are OSVR, our primary mirror window needs to be the same size as the render manager's output...
                if( environment.getVRHardware() instanceof OSVR ) {
                    int origWidth = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getWidth();
                    int origHeight = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getHeight();
                    long window = ((LwjglWindow)environment.getApplication().getContext()).getWindowHandle();
                    Vector2f windowSize = new Vector2f();
                    ((OSVR)environment.getVRHardware()).getRenderSize(windowSize);
                    windowSize.x = Math.max(windowSize.x * 2f, leftCamera.getWidth());
                    org.lwjgl.glfw.GLFW.glfwSetWindowSize(window, (int)windowSize.x, (int)windowSize.y);
                    environment.getApplication().getContext().getSettings().setResolution((int)windowSize.x, (int)windowSize.y);
                    
                    if (environment.getApplication().getRenderManager() != null) {
                    	environment.getApplication().getRenderManager().notifyReshape((int)windowSize.x, (int)windowSize.y);
                    }
                           
                    org.lwjgl.glfw.GLFW.glfwSetWindowPos(window, origWidth - (int)windowSize.x, 32);
                    
                    org.lwjgl.glfw.GLFW.glfwFocusWindow(window);
                    
                    org.lwjgl.glfw.GLFW.glfwSetCursorPos(window, origWidth / 2.0, origHeight / 2.0);
                    
                    logger.config("Initialized VR view manager [SUCCESS]");
                } else {
                	throw new IllegalStateException("Underlying VR hardware should be "+OSVR.class.getSimpleName());
                }
            } else {
    			throw new IllegalStateException("This VR environment is not attached to any application.");
    		}
    	} else {
    		throw new IllegalStateException("This VR view manager is not attached to any VR environment.");
    	}  
    	
        
    }
    
    /**
     * Prepare the size of the given {@link Camera camera} to adapt it to the underlying rendering context.
     * @param cam the {@link Camera camera} to prepare.
     * @param xMult the camera width multiplier.
     */
    private void prepareCameraSize(Camera cam, float xMult) {
    	
    	if (environment != null){
    		
    		if (environment.getApplication() != null){
    			
    		} else {
    			throw new IllegalStateException("This VR environment is not attached to any application.");
    		}
    		
    		Vector2f size = new Vector2f();
            VRAPI vrhmd = environment.getVRHardware();

            if( vrhmd == null ) {
                size.x = 1280f;
                size.y = 720f;
            } else {
                vrhmd.getRenderSize(size);
            }
            
            if( size.x < environment.getApplication().getContext().getSettings().getWidth() ) {
                size.x = environment.getApplication().getContext().getSettings().getWidth();
            }
            if( size.y < environment.getApplication().getContext().getSettings().getHeight() ) {
                size.y = environment.getApplication().getContext().getSettings().getHeight();
            }
            
            if( environment.isInstanceRendering() ){
            	size.x *= 2f;
            }
            
            // other adjustments
            size.x *= xMult;
            size.x *= resMult;
            size.y *= resMult;
            
            if( cam.getWidth() != size.x || cam.getHeight() != size.y ){
            	cam.resize((int)size.x, (int)size.y, false);
            }
    	} else {
    		throw new IllegalStateException("This VR view manager is not attached to any VR environment.");
    	}  
    }
    
    /**
     * Replaces rootNode as the main cameras scene with the distortion mesh
     */
    private void setupVRScene(){
    	
    	if (environment != null){
    		if (environment.getApplication() != null){
    			// no special scene to setup if we are doing instancing
    	        if( environment.isInstanceRendering() ) {
    	            // distortion has to be done with compositor here... we want only one pass on our end!
    	            if( environment.getApplication().getContext().getSettings().isSwapBuffers() ) {
    	                setupMirrorBuffers(environment.getCamera(), dualEyeTex, true);
    	            }       
    	            return;
    	        }
    	        
    	        leftEyeTexture  = (Texture2D) leftViewport.getOutputFrameBuffer().getColorBuffer().getTexture();
    	        rightEyeTexture = (Texture2D)rightViewport.getOutputFrameBuffer().getColorBuffer().getTexture();        
    	        leftEyeDepth    = (Texture2D) leftViewport.getOutputFrameBuffer().getDepthBuffer().getTexture();
    	        rightEyeDepth   = (Texture2D)rightViewport.getOutputFrameBuffer().getDepthBuffer().getTexture();        
    	      
    	        // main viewport is either going to be a distortion scene or nothing
    	        // mirroring is handled by copying framebuffers
    	        Iterator<Spatial> spatialIter = environment.getApplication().getViewPort().getScenes().iterator();
    	        while(spatialIter.hasNext()){
    	        	environment.getApplication().getViewPort().detachScene(spatialIter.next());
    	        }
    	        
    	        spatialIter = environment.getApplication().getGuiViewPort().getScenes().iterator();
    	        while(spatialIter.hasNext()){
    	        	environment.getApplication().getGuiViewPort().detachScene(spatialIter.next());
    	        }
    	        
    	        // only setup distortion scene if compositor isn't running (or using custom mesh distortion option)
    	        if( environment.getVRHardware().getCompositor() == null ) {
    	            Node distortionScene = new Node();
    	            Material leftMat = new Material(environment.getApplication().getAssetManager(), "Common/MatDefs/VR/OpenVR.j3md");
    	            leftMat.setTexture("Texture", leftEyeTexture);
    	            Geometry leftEye = new Geometry("box", setupDistortionMesh(JOpenVRLibrary.EVREye.EVREye_Eye_Left, environment.getVRHardware()));
    	            leftEye.setMaterial(leftMat);
    	            distortionScene.attachChild(leftEye);

    	            Material rightMat = new Material(environment.getApplication().getAssetManager(), "Common/MatDefs/VR/OpenVR.j3md");
    	            rightMat.setTexture("Texture", rightEyeTexture);
    	            Geometry rightEye = new Geometry("box", setupDistortionMesh(JOpenVRLibrary.EVREye.EVREye_Eye_Right, environment.getVRHardware()));
    	            rightEye.setMaterial(rightMat);
    	            distortionScene.attachChild(rightEye);

    	            distortionScene.updateGeometricState();

    	            environment.getApplication().getViewPort().attachScene(distortionScene);
    	            
    	            //if( useCustomDistortion ) setupFinalFullTexture(app.getViewPort().getCamera());
    	        }
    	        
    	        if( environment.getApplication().getContext().getSettings().isSwapBuffers() ) {
    	            setupMirrorBuffers(environment.getCamera(), leftEyeTexture, false);
    	        }   
    		} else {
    			throw new IllegalStateException("This VR environment is not attached to any application.");
    		}
    	} else {
    		throw new IllegalStateException("This VR view manager is not attached to any VR environment.");
    	}  
    	
            
    }
    
    /**
     * Update the VR view manager. 
     * This method is called by the attached {@link VRApplication VR application} and should not be called manually.
     * @param tpf the time per frame.
     */
    public void update(float tpf) {
        
    	if (environment != null){
    		// grab the observer
            Object obs = environment.getObserver();
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
            VRAPI dev = environment.getVRHardware();
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
            
            if( environment.hasTraditionalGUIOverlay() ) {
                // update the mouse?
            	environment.getVRMouseManager().update(tpf);
            
                // update GUI position?
                if( environment.getVRGUIManager().wantsReposition || environment.getVRGUIManager().getPositioningMode() != VRGUIPositioningMode.MANUAL ) {
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
     * @param eyePos the eye position.
     * @param obsPosition the observer position.
     * @param cam the camera to place.
     */
    private void finalizeCamera(Vector3f eyePos, Vector3f obsPosition, Camera cam) {
        finalRotation.mult(eyePos, finalPosition);
        finalPosition.addLocal(hmdPos);
        if( obsPosition != null ){
        	finalPosition.addLocal(obsPosition);
        }
        finalPosition.y += heightAdjustment;
        cam.setFrame(finalPosition, finalRotation);
    }
    
    /**
     * Handles moving filters from the main view to each eye
     */
    public void moveScreenProcessingToEyes() {
        if( rightViewport == null ){
        	return;
        }
        
    	if (environment != null){
    		if (environment.getApplication() != null){
    	        
    	        syncScreenProcessing(environment.getApplication().getViewPort());
    	        environment.getApplication().getViewPort().clearProcessors();
    		} else {
    			throw new IllegalStateException("This VR environment is not attached to any application.");
    		}
    	} else {
    		throw new IllegalStateException("This VR view manager is not attached to any VR environment.");
    	}  
    }
    
    /**
     * Sets the two views to use the list of {@link SceneProcessor processors}.
     * @param sourceViewport the {@link ViewPort viewport} that contains the processors to use.
     */
    public void syncScreenProcessing(ViewPort sourceViewport) {
        if( rightViewport == null ){
        	return;
        }
        
        if (environment != null){
    		if (environment.getApplication() != null){
    			// setup post processing filters
    	        if( rightPostProcessor == null ) {
    	            rightPostProcessor = new FilterPostProcessor(environment.getApplication().getAssetManager());               
    	            leftPostProcessor =  new FilterPostProcessor(environment.getApplication().getAssetManager());
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
    	                            f2 = FilterUtil.cloneDirectionalLightShadowFilter(environment.getApplication().getAssetManager(), (DirectionalLightShadowFilter)f);
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
    		} else {
    			throw new IllegalStateException("This VR environment is not attached to any application.");
    		}
    	} else {
    		throw new IllegalStateException("This VR view manager is not attached to any VR environment.");
    	}  
    }
    
    private void setupCamerasAndViews() {   
    	
    	if (environment != null){
    		if (environment.getApplication() != null){
    			// get desired frustrum from original camera
    	        Camera origCam = environment.getCamera();        
    	        float fFar = origCam.getFrustumFar();
    	        float fNear = origCam.getFrustumNear();
    	        
    	        // if we are using OSVR get the eye info here
    	        if( environment.getVRHardware() instanceof OSVR ) {
    	            ((OSVR)environment.getVRHardware()).getEyeInfo();
    	        }
    	        
    	        // restore frustrum on distortion scene cam, if needed
    	        if( environment.isInstanceRendering() ) {
    	            leftCamera = origCam;
    	        } else if( environment.compositorAllowed() == false ) {
    	            origCam.setFrustumFar(100f);
    	            origCam.setFrustumNear(1f); 
    	            leftCamera = origCam.clone();  
    	            prepareCameraSize(origCam, 2f);
    	        } else {
    	            leftCamera = origCam.clone();
    	        }
    	        
    	        leftCamera.setFrustumPerspective(environment.getDefaultFOV(), environment.getDefaultAspect(), fNear, fFar);                     
    	                
    	        prepareCameraSize(leftCamera, 1f);
    	        if( environment.getVRHardware() != null ) leftCamera.setProjectionMatrix(environment.getVRHardware().getHMDMatrixProjectionLeftEye(leftCamera));
    	        //org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_SRGB);
    	        
    	        if( !environment.isInstanceRendering()) {
    	            leftViewport = setupViewBuffers(leftCamera, LEFT_VIEW_NAME);
    	            rightCamera = leftCamera.clone();
    	            if( environment.getVRHardware() != null ){
    	            	rightCamera.setProjectionMatrix(environment.getVRHardware().getHMDMatrixProjectionRightEye(rightCamera));
    	            }
    	            rightViewport = setupViewBuffers(rightCamera, RIGHT_VIEW_NAME);
    	        } else {
    	        	
    	        	System.err.println("[VRViewManager] THIS CODE NEED CHANGES !!!");
    	            leftViewport = environment.getApplication().getViewPort();
    	            //leftViewport.attachScene(app.getRootNode());
    	            rightCamera = leftCamera.clone();
    	            if( environment.getVRHardware() != null ){
    	            	rightCamera.setProjectionMatrix(environment.getVRHardware().getHMDMatrixProjectionRightEye(rightCamera));
    	            }
    	            
    	            org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL30.GL_CLIP_DISTANCE0);
    	            
    	            //FIXME: [jme-vr] Fix with JMonkey next release
    	            //RenderManager._VRInstancing_RightCamProjection = camRight.getViewProjectionMatrix();
    	            setupFinalFullTexture(environment.getApplication().getViewPort().getCamera());            
    	        }
    	        
    	        // setup gui
    	        environment.getVRGUIManager().setupGui(leftCamera, rightCamera, leftViewport, rightViewport);
    	        
    	        if( environment.getVRHardware() != null ) {
    	            // call these to cache the results internally
    	        	environment.getVRHardware().getHMDMatrixPoseLeftEye();
    	        	environment.getVRHardware().getHMDMatrixPoseRightEye();
    	        }
    		} else {
    			throw new IllegalStateException("This VR environment is not attached to any application.");
    		}
    	} else {
    		throw new IllegalStateException("This VR view manager is not attached to any VR environment.");
    	}  
    }
    
    private ViewPort setupMirrorBuffers(Camera cam, Texture tex, boolean expand) {     
    	
    	if (environment != null){
    		if (environment.getApplication() != null){
    			Camera clonecam = cam.clone();
    	        ViewPort viewPort = environment.getApplication().getRenderManager().createPostView("MirrorView", clonecam);
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
    	        pic.setTexture(environment.getApplication().getAssetManager(), (Texture2D)tex, false);
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
    	
    	if (environment != null){
    		if (environment.getApplication() != null){
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
    
    private ViewPort setupViewBuffers(Camera cam, String viewName){
    	
    	if (environment != null){
    		if (environment.getApplication() != null){
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
    	        while(spatialIter.hasNext()){
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
    
    /**
     * Setup a distortion mesh for the stereo view.
     * @param eye the eye to apply.
     * @param api the underlying VR api
     * @return the distorted mesh.
     */
    public static Mesh setupDistortionMesh(int eye, VRAPI api) {
        Mesh distortionMesh = new Mesh();
        float m_iLensGridSegmentCountH = 43, m_iLensGridSegmentCountV = 43;
        
        float w = 1f / (m_iLensGridSegmentCountH - 1f);
        float h = 1f / (m_iLensGridSegmentCountV - 1f);

        float u, v;

        float verts[] = new float[(int) (m_iLensGridSegmentCountV * m_iLensGridSegmentCountH) * 3];

        float texcoordR[] = new float[(int) (m_iLensGridSegmentCountV * m_iLensGridSegmentCountH) * 2];
        float texcoordG[] = new float[(int) (m_iLensGridSegmentCountV * m_iLensGridSegmentCountH) * 2];
        float texcoordB[] = new float[(int) (m_iLensGridSegmentCountV * m_iLensGridSegmentCountH) * 2];

        int vertPos = 0, coordPos = 0;
        
        float Xoffset = eye == JOpenVRLibrary.EVREye.EVREye_Eye_Left ? -1f : 0;
        for (int y = 0; y < m_iLensGridSegmentCountV; y++) {
            for (int x = 0; x < m_iLensGridSegmentCountH; x++) {
                u = x * w;
                v = 1 - y * h;
                verts[vertPos] = Xoffset + u; // x
                verts[vertPos + 1] = -1 + 2 * y * h; // y
                verts[vertPos + 2] = 0f; // z
                vertPos += 3;

                DistortionCoordinates_t dc0 = new DistortionCoordinates_t();
                if( api.getVRSystem() == null ) {
                    // default to no distortion
                    texcoordR[coordPos] = u;
                    texcoordR[coordPos + 1] = 1 - v;
                    texcoordG[coordPos] = u;
                    texcoordG[coordPos + 1] = 1 - v;
                    texcoordB[coordPos] = u;
                    texcoordB[coordPos + 1] = 1 - v;                    
                } else {
                    ((VR_IVRSystem_FnTable)api.getVRSystem()).ComputeDistortion.apply(eye, u, v, dc0);
                    
                    texcoordR[coordPos] = dc0.rfRed[0];
                    texcoordR[coordPos + 1] = 1 - dc0.rfRed[1];
                    texcoordG[coordPos] = dc0.rfGreen[0];
                    texcoordG[coordPos + 1] = 1 - dc0.rfGreen[1];
                    texcoordB[coordPos] = dc0.rfBlue[0];
                    texcoordB[coordPos + 1] = 1 - dc0.rfBlue[1];
                }                
                
                coordPos += 2;
            }
        }

        // have UV coordinates & positions, now to setup indices

        int[] indices = new int[(int) ((m_iLensGridSegmentCountV - 1) * (m_iLensGridSegmentCountH - 1)) * 6];
        int indexPos = 0;
        int a, b, c, d;

        int offset = 0;
        for (int y = 0; y < m_iLensGridSegmentCountV - 1; y++) {
            for (int x = 0; x < m_iLensGridSegmentCountH - 1; x++) {
                a = (int) (m_iLensGridSegmentCountH * y + x + offset);
                b = (int) (m_iLensGridSegmentCountH * y + x + 1 + offset);
                c = (int) ((y + 1) * m_iLensGridSegmentCountH + x + 1 + offset);
                d = (int) ((y + 1) * m_iLensGridSegmentCountH + x + offset);
                
                indices[indexPos] = a;
                indices[indexPos + 1] = b;
                indices[indexPos + 2] = c;

                indices[indexPos + 3] = a;
                indices[indexPos + 4] = c;
                indices[indexPos + 5] = d;

                indexPos += 6;
            }
        }
        
        // OK, create the mesh        
        distortionMesh.setBuffer(VertexBuffer.Type.Position, 3, verts);
        distortionMesh.setBuffer(VertexBuffer.Type.Index, 1, indices);
        distortionMesh.setBuffer(VertexBuffer.Type.TexCoord, 2, texcoordR);
        distortionMesh.setBuffer(VertexBuffer.Type.TexCoord2, 2, texcoordG);
        distortionMesh.setBuffer(VertexBuffer.Type.TexCoord3, 2, texcoordB);
        distortionMesh.setStatic();
        return distortionMesh;
    }
}

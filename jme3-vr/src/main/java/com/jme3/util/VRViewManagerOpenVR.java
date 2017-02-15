/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.util;

import com.jme3.app.VREnvironment;
import com.jme3.input.vr.OpenVR;
import com.jme3.input.vr.VRAPI;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.system.jopenvr.DistortionCoordinates_t;
import com.jme3.system.jopenvr.JOpenVRLibrary;
import com.jme3.system.jopenvr.OpenVRUtil;
import com.jme3.system.jopenvr.Texture_t;
import com.jme3.system.jopenvr.VRTextureBounds_t;
import com.jme3.system.jopenvr.VR_IVRSystem_FnTable;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;

import java.util.Iterator;
import java.util.logging.Logger;

/**
 * A VR view manager based on OpenVR. This class enable to submit 3D views to the VR compositor.
 * @author reden - phr00t - https://github.com/phr00t
 * @author Julien Seinturier - (c) 2016 - JOrigin project - <a href="http://www.jorigin.org">http:/www.jorigin.org</a>
 */
public class VRViewManagerOpenVR extends AbstractVRViewManager {

	private static final Logger logger = Logger.getLogger(VRViewManagerOpenVR.class.getName());

    // OpenVR values
    private VRTextureBounds_t leftTextureBounds;
    private Texture_t leftTextureType;
    
    private VRTextureBounds_t rightTextureBounds;
    private Texture_t rightTextureType;

    private Texture2D dualEyeTex;
    
    //final & temp values for camera calculations
    private final Vector3f finalPosition   = new Vector3f();
    private final Quaternion finalRotation = new Quaternion();
    private final Vector3f hmdPos          = new Vector3f();
    private final Quaternion hmdRot        = new Quaternion();
    
    /**
     * Create a new VR view manager attached to the given {@link VREnvironment VR environment}.
     * @param environment the {@link VREnvironment VR environment} to which this view manager is attached.
     */
    public VRViewManagerOpenVR(VREnvironment environment){
    	this.environment = environment;
    }
    
    /**
     * Get the identifier of the left eye texture.
     * @return the identifier of the left eye texture.
     * @see #getRightTexId()
     * @see #getFullTexId()
     */
    protected int getLeftTexId() {
        return (int)getLeftTexture().getImage().getId();
    }
    
    /**
     * Get the identifier of the right eye texture.
     * @return the identifier of the right eye texture.
     * @see #getLeftTexId()
     * @see #getFullTexId()
     */
    protected int getRightTexId() {
        return (int)getRightTexture().getImage().getId();
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
     * Initialize the system binds of the textures.
     */
    private void initTextureSubmitStructs() {
        leftTextureType = new Texture_t();
        rightTextureType = new Texture_t();
        
        if (environment != null){
        	if( environment.getVRHardware() instanceof OpenVR ) {
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
                leftTextureType.eColorSpace = JOpenVRLibrary.EColorSpace.EColorSpace_ColorSpace_Gamma;
                leftTextureType.eType = JOpenVRLibrary.ETextureType.ETextureType_TextureType_OpenGL;
                leftTextureType.setAutoSynch(false);
                leftTextureType.setAutoRead(false);
                leftTextureType.setAutoWrite(false);
                leftTextureType.handle = -1;
                rightTextureType.eColorSpace = JOpenVRLibrary.EColorSpace.EColorSpace_ColorSpace_Gamma;
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
            }
        } else {
    		throw new IllegalStateException("This VR view manager is not attached to any VR environment.");
    	}
    }
    
    @Override
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
                            }
                        } else {
                            if( api instanceof OpenVR ) {
                                int submitFlag = JOpenVRLibrary.EVRSubmitFlags.EVRSubmitFlags_Submit_Default;
                                errr = ((OpenVR)api).getCompositor().Submit.apply(JOpenVRLibrary.EVREye.EVREye_Eye_Right, leftTextureType, rightTextureBounds, submitFlag);
                                errl = ((OpenVR)api).getCompositor().Submit.apply(JOpenVRLibrary.EVREye.EVREye_Eye_Left, leftTextureType, leftTextureBounds, submitFlag);
                            }
                        }
                    } else if( leftTextureType.handle == -1 || rightTextureType.handle == -1 ||
                               leftTextureType.handle != getLeftTexId() || rightTextureType.handle != getRightTexId() ) {
                        leftTextureType.handle = getLeftTexId();
                        if( leftTextureType.handle != -1 ) {
                        	logger.fine("Writing Left texture to native memory at " + leftTextureType.getPointer());
                            leftTextureType.write();
                        }
                        rightTextureType.handle = getRightTexId();
                        if( rightTextureType.handle != -1 ) {
                        	logger.fine("Writing Right texture to native memory at " + leftTextureType.getPointer());
                            rightTextureType.write();
                        }                    
                    } else {
                        if( api instanceof OpenVR ) {
                            errl = ((OpenVR)api).getCompositor().Submit.apply(JOpenVRLibrary.EVREye.EVREye_Eye_Left, leftTextureType, null,
                                                                   JOpenVRLibrary.EVRSubmitFlags.EVRSubmitFlags_Submit_Default);
                            errr = ((OpenVR)api).getCompositor().Submit.apply(JOpenVRLibrary.EVREye.EVREye_Eye_Right, rightTextureType, null,
                                                                   JOpenVRLibrary.EVRSubmitFlags.EVRSubmitFlags_Submit_Default);
                        } else {
                        	
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


    @Override
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
            
          logger.config("Initialized VR view manager [SUCCESS]");
          
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
    	        size.x *= getResolutionMuliplier();
    	        size.y *= getResolutionMuliplier();
    	        
    	        if( cam.getWidth() != size.x || cam.getHeight() != size.y ){
    	        	cam.resize((int)size.x, (int)size.y, false);
    	        }
    		} else {
    			throw new IllegalStateException("This VR environment is not attached to any application.");
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
    	        
    	        leftEyeTexture  = (Texture2D) getLeftViewport().getOutputFrameBuffer().getColorBuffer().getTexture();
    	        rightEyeTexture = (Texture2D)getRightViewport().getOutputFrameBuffer().getColorBuffer().getTexture();        
    	        leftEyeDepth    = (Texture2D) getLeftViewport().getOutputFrameBuffer().getDepthBuffer().getTexture();
    	        rightEyeDepth   = (Texture2D)getRightViewport().getOutputFrameBuffer().getDepthBuffer().getTexture();        
    	      
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
    
    @Override
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
                
                finalizeCamera(dev.getHMDVectorPoseLeftEye(), objPos, getLeftCamera());
                finalizeCamera(dev.getHMDVectorPoseRightEye(), objPos, getRightCamera());
            } else {
            	getLeftCamera().setFrame(objPos, objRot);
            	getRightCamera().setFrame(objPos, objRot);
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
        if( obsPosition != null ) finalPosition.addLocal(obsPosition);
        finalPosition.y += getHeightAdjustment();
        cam.setFrame(finalPosition, finalRotation);
    }
    

    private void setupCamerasAndViews() { 
    	
    	if (environment != null){
    		// get desired frustrum from original camera
            Camera origCam = environment.getCamera();        
            float fFar = origCam.getFrustumFar();
            float fNear = origCam.getFrustumNear();
            
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
            
            getLeftCamera().setFrustumPerspective(environment.getDefaultFOV(), environment.getDefaultAspect(), fNear, fFar);                     
                    
            prepareCameraSize(getLeftCamera(), 1f);
            if( environment.getVRHardware() != null ) {
            	getLeftCamera().setProjectionMatrix(environment.getVRHardware().getHMDMatrixProjectionLeftEye(getLeftCamera()));
            }
            //org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_SRGB);
            
            if( !environment.isInstanceRendering()) {
                leftViewport = setupViewBuffers(getLeftCamera(), LEFT_VIEW_NAME);
                rightCamera = getLeftCamera().clone();
                if( environment.getVRHardware() != null ){
                	getRightCamera().setProjectionMatrix(environment.getVRHardware().getHMDMatrixProjectionRightEye(getRightCamera()));
                }
                rightViewport = setupViewBuffers(getRightCamera(), RIGHT_VIEW_NAME);
            } else {
            	
            	if (environment.getApplication() != null){
                	
                	logger.severe("THIS CODE NEED CHANGES !!!");
                    leftViewport = environment.getApplication().getViewPort();
                    //leftViewport.attachScene(app.getRootNode());
                    rightCamera = getLeftCamera().clone();
                    if( environment.getVRHardware() != null ){
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
            environment.getVRGUIManager().setupGui(getLeftCamera(), getRightCamera(), getLeftViewport(), getRightViewport());
            
            if( environment.getVRHardware() != null ) {
                // call these to cache the results internally
            	environment.getVRHardware().getHMDMatrixPoseLeftEye();
            	environment.getVRHardware().getHMDMatrixPoseRightEye();
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

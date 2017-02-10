package com.jme3.app;


import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.input.vr.OSVR;
import com.jme3.input.vr.OpenVR;
import com.jme3.input.vr.VRAPI;
import com.jme3.input.vr.VRInputAPI;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.PreNormalCaching;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;
import com.jme3.system.jopenvr.JOpenVRLibrary;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Iterator;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import jmevr.util.VRGuiManager;
import jmevr.util.VRMouseManager;
import jmevr.util.VRViewManager;
import jmevr.util.VRGuiManager.POSITIONING_MODE;

/**
 * A JMonkey app state dedicated to Virtual Reality. 
 * An application that want to use VR devices (HTC vive, ...) has to use this app state.<br>
 * As this app state and the main {@link Application application} have to share {@link AppSettings application settings}, 
 * the common way to use this app state is:<br>
 * <ul>
 * <li>To create {@link AppSettings application settings} and set the VR related settings (see {@link VRConstants}).
 * <li>To instantiate this app state with the created settings.
 * <li>To instantiate the main {@link Application application} and to attach it to the created settings (with {@link Application#setSettings(AppSettings) setSettings(AppSettings)}).
 * <li>To start the main {@link Application application}.
 * </ul>
 * Attaching an instance of this app state to an already started application may cause crashes.
 * @author Julien Seinturier - JOrigin project - <a href="http://www.jorigin.org">http:/www.jorigin.org</a>
 */
public class VRAppState extends AbstractAppState {

    private static final Logger logger = Logger.getLogger(VRAppState.class.getName());
    
    /**
     * The underlying system VR API. By default set to {@link VRConstants#SETTING_VRAPI_OPENVR_VALUE}.
     */
    public int vrBinding = VRConstants.SETTING_VRAPI_OPENVR_VALUE;
    
    /**
     * Is the application has not to start within VR mode (default is <code>false</code>).
     */
    public boolean DISABLE_VR = false;
    
    private VRAPI VRhardware            = null;
    private VRGuiManager guiManager     = null;
    private VRMouseManager mouseManager = null;
    private VRViewManager viewmanager   = null;
    
    private String OS;
    
    private Camera dummyCam;
    
    private Spatial observer = null;
    
    private boolean VRSupportedOS;
    private boolean forceVR            = false;;
    private boolean disableSwapBuffers = true;
    private boolean disableVR          = false;
    private boolean seated;
    private boolean nogui;
    private boolean instanceVR         = false;

	private float defaultFOV           = 108f;
    private float defaultAspect        = 1f;
    
   
    
    private float fFar  = 1000f;
    private float fNear = 0.1f;
    private int xWin    = 1920;
    private int yWin    = 1080;
    
    private float resMult = 1f;
    
    private boolean useCompositor = true;
    private boolean compositorOS;
    
    /*
     where is the headset pointing, after all rotations are combined?
     depends on observer rotation, if any
     */
    private Quaternion tempq = new Quaternion();
    
    private Application application      = null;
    private AppStateManager stateManager = null;
    private AppSettings settings         = null;
    
    
    /**
     * Create a new default VR app state.
     */
    public VRAppState() {
        super();

        dummyCam = new Camera();
        
        // Create the GUI manager.
        guiManager = new VRGuiManager();
        
        // Create a new view manager.
        viewmanager = new VRViewManager();

        // Create a new mouse manager.
        mouseManager = new VRMouseManager();
      
     }
    
    /**
     * Create a new VR app state with given settings. 
     * @param settings the settings to use.
     */
    public VRAppState(AppSettings settings){
      this();
      this.settings = settings;
      processSettings(settings);
    }

    
    /**
     * Simple update of the app state, this method should contains any spatial updates.
     * This method is called by the {@link #update(float) update()} method and should not be called manually.
     * @param tpf the application time.
     */
    public void simpleUpdate(float tpf) {
    	return;
    }
    
    /**
     * Rendering callback of the app state. This method is called by the {@link #update(float) update()} method and should not be called manually.
     * @param renderManager the {@link RenderManager render manager}.
     */
    public void simpleRender(RenderManager renderManager) {
        PreNormalCaching.resetCache(isInVR());
    }

    /**
     * Set the frustrum values for the application.
     * @param near the frustrum near value.
     * @param far the frustrum far value.
     */
    public void setFrustrumNearFar(float near, float far) {
        fNear = near;
        fFar = far;
    }
    
    /**
     * Set the mirror window size in pixel.
     * @param width the width of the mirror window in pixel.
     * @param height the height of the mirror window in pixel.
     */
    public void setMirrorWindowSize(int width, int height) {
        xWin = width;
        yWin = height;
    }
    
    /**
     * Set the resolution multiplier.
     * @param val the resolution multiplier.
     */
    public void setResolutionMultiplier(float val) {
        resMult = val;
        if( viewmanager != null ){
        	viewmanager.setResolutionMultiplier(resMult);
        }
    }

    /**
     * Is the VR compositor is active.
     * @return <code>true</code> if the VR compositor is active and <code>false</code> otherwise.
     */
    public boolean compositorAllowed() {
        return useCompositor && compositorOS;
    }
    
    /**
     * Get if the system currently support VR.
     * @return <code>true</code> if the system currently support VR and <code>false</Code> otherwise.
     */
    public boolean isVRSupported() {
        return VRSupportedOS;
    }
    
    /**
     * Get the {@link Camera camera} attached to this application state. 
     * If the VR mode is {@link #isInVR() active}, this method return a dummy camera, otherwise, 
     * this method return the camera of the attached application.
     * @return the camera attached to this application state.
     */
    public Camera getCamera() {
        if( isInVR() && viewmanager != null && viewmanager.getLeftCamera() != null ) {
            return dummyCam;
        }
        
        return application.getCamera();
    }

    /**
     * Can be used to change seated experience during runtime.
     * @param isSeated <code>true</code> if designed for sitting, <code>false</code> for standing/roomscale
     * @see #isSeatedExperience()
     */
    public void setSeatedExperience(boolean isSeated) {
        seated = isSeated;
        if( VRhardware instanceof OpenVR ) {
            if( VRhardware.getCompositor() == null ) return;
            if( seated ) {
                ((OpenVR)VRhardware).getCompositor().SetTrackingSpace.apply(JOpenVRLibrary.ETrackingUniverseOrigin.ETrackingUniverseOrigin_TrackingUniverseSeated);
            } else {
                ((OpenVR)VRhardware).getCompositor().SetTrackingSpace.apply(JOpenVRLibrary.ETrackingUniverseOrigin.ETrackingUniverseOrigin_TrackingUniverseStanding);                
            }        
        }
    }
    
    /**
     * Check if the application is configured as a seated experience.
     * @return <code>true</code> if the application is configured as a seated experience and <code>false</code> otherwise.
     * @see #setSeatedExperience(boolean)
     */
    public boolean isSeatedExperience() {
        return seated;
    }
    
    /**
     * Reset headset pose if seating experience.
     */
    public void resetSeatedPose(){
        if( VRSupportedOS == false || isSeatedExperience() == false ) return;
        VRhardware.reset();
    }
    
    /**
     * Check if the rendering is instanced (see <a href="https://en.wikipedia.org/wiki/Geometry_instancing">Geometry instancing</a>).
     * @return <code>true</code> if the rendering is instanced and <code>false</code> otherwise.
     */
    public boolean isInstanceVRRendering() {
        return instanceVR && isInVR();
    }
    
    /**
     * Check if the VR mode is enabled.
     * @return <code>true</code> if the VR mode is enabled and <code>false</code> otherwise.
     */
    public boolean isInVR() {
        return DISABLE_VR == false && (forceVR || VRSupportedOS && VRhardware != null && VRhardware.isInitialized());
    }
    
    /**
     * Get the default Field Of View (FOV) value.
     * @return the default Field Of View (FOV) value.
     * @see #setDefaultFOV(float)
     */
    public float getDefaultFOV() {
		return defaultFOV;
	}

    /**
     * Set the default Field Of View (FOV) value.
     * @param defaultFOV the default Field Of View (FOV) value.
     * @see #getDefaultFOV()
     */
	public void setDefaultFOV(float defaultFOV) {
		this.defaultFOV = defaultFOV;
	}

	/**
	 * Get the default aspect ratio.
	 * @return the default aspect ratio.
	 * @see #setDefaultAspect(float)
	 */
	public float getDefaultAspect() {
		return defaultAspect;
	}

	/**
	 * Set the default aspect ratio.
	 * @param defaultAspect the default aspect ratio.
	 * @see #getDefaultAspect()
	 */
	public void setDefaultAspect(float defaultAspect) {
		this.defaultAspect = defaultAspect;
	}
	
    /**
     * Move filters from the main scene into the eye's.
     * This removes filters from the main scene.
     */
    public void moveScreenProcessingToVR() {
        if( isInVR() ) {
        	viewmanager.moveScreenProcessingToEyes();
        }
    }
 
    /**
     * Check if the application has a GUI overlay attached.
     * @return <code>true</code> if the application has a GUI overlay attached and <code>false</code> otherwise.
     */
    public boolean hasTraditionalGUIOverlay() {
        return !nogui;
    }
    
    /**
     * Get the scene observer. If no observer has been set, this method return the application {@link #getCamera() camera}.
     * @return the scene observer. 
     * @see #setObserver(Spatial)
     */
    public Object getObserver() {
        if( observer == null ) {
            return getCamera();
        }
        return observer;
    }
    
    /**
     * Set the scene observer. The VR headset will be linked to it. If no observer is set, the VR headset is linked to the the application {@link #getCamera() camera}.
     * @param observer the scene observer.
     */
    public void setObserver(Spatial observer) {
       this.observer = observer;
    }
    
    /**
     * Get the observer final rotation within the scene.
     * @return the observer final rotation within the scene.
     * @see #getFinalObserverPosition()
     */
    public Quaternion getFinalObserverRotation() {
        if( viewmanager == null ) {
            if( observer == null ) {
                return getCamera().getRotation();
            } else return observer.getWorldRotation();
        }        
        if( observer == null ) {
            tempq.set(dummyCam.getRotation());
        } else {
            tempq.set(observer.getWorldRotation());
        }
        return tempq.multLocal(VRhardware.getOrientation());
    }
    
    /**
     * Get the observer final position within the scene.
     * @return the observer position.
     * @see #getFinalObserverRotation()
     */
    public Vector3f getFinalObserverPosition() {
        if( viewmanager == null ) {
            if( observer == null ) {
                return getCamera().getLocation();
            } else return observer.getWorldTranslation();            
        }
        Vector3f pos = VRhardware.getPosition();
        if( observer == null ) {
            dummyCam.getRotation().mult(pos, pos);
            return pos.addLocal(dummyCam.getLocation());
        } else {
            observer.getWorldRotation().mult(pos, pos);
            return pos.addLocal(observer.getWorldTranslation());
        }
    }
    
    /**
     * Set the VR headset height from the ground.
     * @param amount the VR headset height from the ground.
     * @see #getVRHeightAdjustment()
     */
    public void setVRHeightAdjustment(float amount) {
        if( viewmanager != null ) viewmanager.setHeightAdjustment(amount);
    }
    
    /**
     * Get the VR headset height from the ground.
     * @return the VR headset height from the ground.
     * @see #setVRHeightAdjustment(float)
     */
    public float getVRHeightAdjustment() {
        if( viewmanager != null ){
        	return viewmanager.getHeightAdjustment();
        }
        return 0f;
    }
    
    /**
     * Get the VR headset left viewport.
     * @return the VR headset left viewport.
     * @see #getRightViewPort()
     */
    public ViewPort getLeftViewPort() {
        if( viewmanager == null ) return application.getViewPort();
        return viewmanager.getLeftViewport();
    }
    
    /**
     * Get the VR headset right viewport.
     * @return the VR headset right viewport.
     * @see #getLeftViewPort()
     */
    public ViewPort getRightViewPort() {
        if( viewmanager == null ) return application.getViewPort();
        return viewmanager.getRightViewport();
    }
    
    /**
     * Set the background color for both left and right view ports.
     * @param clr the background color.
     */
    public void setBackgroundColors(ColorRGBA clr) {
        if( viewmanager == null ) {
            application.getViewPort().setBackgroundColor(clr);
        } else if( viewmanager.getLeftViewport() != null ) {
        	viewmanager.getLeftViewport().setBackgroundColor(clr);
            if( viewmanager.getRightViewport() != null ) viewmanager.getRightViewport().setBackgroundColor(clr);
        }
    }
    
    /**
     * Get the {@link Application} to which this app state is attached.
     * @return the {@link Application} to which this app state is attached.
     * @see #getStateManager()
     */
    public Application getApplication(){
    	return application;
    }
    
    /**
     * Get the {@link AppStateManager state manager} to which this app state is attached.
     * @return the {@link AppStateManager state manager} to which this app state is attached.
     * @see #getApplication()
     */
    public AppStateManager getStateManager(){
    	return stateManager;
    }
    
	/**
	 * Get the VR underlying hardware.
	 * @return the VR underlying hardware.
	 */
	public VRAPI getVRHardware() {
	    return VRhardware;
	}
	
	/**
	 * Get the VR dedicated input.
	 * @return the VR dedicated input.
	 */
	public VRInputAPI getVRinput() {
	    if( VRhardware == null ){
	    	return null;
	    }
	    
	    return VRhardware.getVRinput();
	}
	
	/**
	 * Get the VR view manager.
	 * @return the VR view manager.
	 */
	public VRViewManager getVRViewManager() {
	    return viewmanager;
	}
	
	/**
	 * Get the GUI manager attached to this application.
	 * @return the GUI manager attached to this application.
	 */
	public VRGuiManager getVRGUIManager(){
		return guiManager;
	}
	
	/**
	 * Get the VR mouse manager attached to this application.
	 * @return the VR mouse manager attached to this application.
	 */
	public VRMouseManager getVRMouseManager(){
		return mouseManager;
	}
    
	/**
	 * Get the {@link AppSettings settings} attached to this app state.
	 * @return the {@link AppSettings settings} attached to this app state.
	 * @see #setSettings(AppSettings)
	 */
	public AppSettings getSettings(){
		return settings;
	}
	
	/**
	 * Set the {@link AppSettings settings} attached to this app state.
	 * @param settings the {@link AppSettings settings} attached to this app state.
	 * @see #getSettings()
	 */
	public void setSettings(AppSettings settings){
		this.settings = settings;
		processSettings(settings);
	}
	
    @Override
    public void update(float tpf) {    
        
        // update VR pose & cameras
        if( viewmanager != null ) {
        	viewmanager.update(tpf);    
        } else if( observer != null ) {
            getCamera().setFrame(observer.getWorldTranslation(), observer.getWorldRotation());
        }
        
        //FIXME: check if this code is necessary.
        // Updates scene and gui states.
        Iterator<Spatial> spatialIter = application.getViewPort().getScenes().iterator();
        Spatial spatial = null;
        while(spatialIter.hasNext()){
        	spatial = spatialIter.next();
        	spatial.updateLogicalState(tpf);
        	spatial.updateGeometricState();
        }        
        
        if( isInVR() == false || guiManager.getPositioningMode() == POSITIONING_MODE.MANUAL ) {
            // only update geometric state here if GUI is in manual mode, or not in VR
            // it will get updated automatically in the viewmanager update otherwise
        	spatialIter = application.getGuiViewPort().getScenes().iterator();
            spatial = null;
            while(spatialIter.hasNext()){
            	spatial = spatialIter.next();
            	spatial.updateGeometricState();
            }    
        }
        
        
        // use the analog control on the first tracked controller to push around the mouse
        getVRMouseManager().updateAnalogAsMouse(0, null, null, null, tpf);
    }

    @Override
    public void postRender() {
        super.postRender();
        // update compositor?
        if( viewmanager != null ) {
        	viewmanager.sendTextures();
        }
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        
        this.application  = app;
        this.stateManager = stateManager;
        
        // disable annoying warnings about GUI stuff being updated, which is normal behavior
        // for late GUI placement for VR purposes
        Logger.getLogger("com.jme3").setLevel(Level.SEVERE);     
        
        // VR module attch
        guiManager.attach(this, app);
        viewmanager.attach(this, app);
        mouseManager.attach(this,  app);
        
        app.getCamera().setFrustumFar(fFar);
        app.getCamera().setFrustumNear(fNear);
        dummyCam = app.getCamera().clone();
        
        if( isInVR() ) {
        	
        	logger.config("VR mode enabled.");
        	
            if( VRhardware != null ) {
                VRhardware.initVRCompositor(compositorAllowed());
            } else {
            	logger.warning("No VR system found.");
            }
            
            
            viewmanager.setResolutionMultiplier(resMult);
            //inputManager.addMapping(RESET_HMD, new KeyTrigger(KeyInput.KEY_F9));
            //setLostFocusBehavior(LostFocusBehavior.Disabled);
        } else {
        	logger.config("VR mode disabled.");
            //viewPort.attachScene(rootNode);
            //guiViewPort.attachScene(guiNode);
        }
        
        if( viewmanager != null ) {
        	viewmanager.initialize();
        }
    }
    
    @Override
    public void stateAttached(AppStateManager stateManager) {
        super.stateAttached(stateManager); //To change body of generated methods, choose Tools | Templates.
        
        if (settings == null) {
            settings = new AppSettings(true);
            logger.config("Using default settings.");
        } else {
        	logger.config("Using given settings.");
        }
        
        // we are going to use OpenVR now, not the Oculus Rift
        // OpenVR does support the Rift
        OS            = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
        VRSupportedOS = !OS.contains("nux") && System.getProperty("sun.arch.data.model").equalsIgnoreCase("64"); //for the moment, linux/unix causes crashes, 64-bit only
        compositorOS  = OS.contains("indows");
        
        if( VRSupportedOS && disableVR == false ) {
        	if( vrBinding == VRConstants.SETTING_VRAPI_OSVR_VALUE ) {
                VRhardware = new OSVR(this);
                logger.config("Creating OSVR wrapper [SUCCESS]");
            } else if( vrBinding == VRConstants.SETTING_VRAPI_OPENVR_VALUE ) {
                VRhardware = new OpenVR(this);
                logger.config("Creating OpenVR wrapper [SUCCESS]");
            } else {
            	logger.config("Cannot create VR binding: "+vrBinding+" [FAILED]");
            }
        	
            if( VRhardware.initialize() ) {
            	logger.config("VR native wrapper initialized [SUCCESS]");
            } else {
            	logger.warning("VR native wrapper initialized [FAILED]");
            }
            }
        
        GraphicsDevice defDev = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
                                    
        if( isInVR() && !compositorAllowed() ) {
            // "easy extended" mode
            // setup experimental JFrame on external device
            // first, find the VR device
            GraphicsDevice VRdev = null;
            GraphicsDevice[] devs = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
            // pick the display that isn't the default one
            for(GraphicsDevice gd : devs) {
                if( gd != defDev ) {
                    VRdev = gd;
                    break;
                }
            }

            // did we get the VR device?
            if( VRdev != null ) {
                // set properties for VR acceleration
                try {   
                    java.awt.DisplayMode useDM = null;
                    int max = 0;
                    for(java.awt.DisplayMode dm : VRdev.getDisplayModes()) {
                        int check = dm.getHeight() + dm.getWidth() + dm.getRefreshRate() + dm.getBitDepth();
                        if( check > max ) {
                            max = check;
                            useDM = dm;
                        }
                    }
                    
                    // create a window for the VR device
                    settings.setWidth(useDM.getWidth());
                    settings.setHeight(useDM.getHeight());
                    settings.setBitsPerPixel(useDM.getBitDepth());
                    settings.setFrequency(useDM.getRefreshRate());
                    settings.setSwapBuffers(true);
                    settings.setVSync(true); // allow vsync on this display
                    stateManager.getApplication().setSettings(settings);
                    logger.config("Updated underlying application settings.");
                    
                    //VRdev.setFullScreenWindow(VRwindow);
                    // make sure we are in the right display mode
                    if( VRdev.getDisplayMode().equals(useDM) == false ) {
                        VRdev.setDisplayMode(useDM);
                    }
                    
                    return;
                } catch(Exception e) { 
                    logger.log(Level.SEVERE, e.getMessage(), e);
                }
            } else {
            	logger.config("Cannot access to external screen.");
            }
        } else {
        	if (!isInVR()){
        	  logger.config("Cannot switch to VR mode (VR disabled by user).");
        	} else if (!compositorAllowed()){
        	  logger.warning("Cannot switch to VR mode (VR not supported).");
        	}
        }
        
        if( !isInVR() ) {
        	
        	//FIXME: Handling GLFW workaround on MacOS
        	boolean macOs = false;
            if (macOs) {
                // GLFW workaround on macs
                settings.setFrequency(defDev.getDisplayMode().getRefreshRate());
                settings.setDepthBits(24);
                settings.setVSync(true);
                // try and read resolution from file in local dir
                File resfile = new File("resolution.txt");
                if( resfile.exists() ) {
                    try {
                        BufferedReader br = new BufferedReader(new FileReader(resfile));
                        settings.setWidth(Integer.parseInt(br.readLine()));
                        settings.setHeight(Integer.parseInt(br.readLine()));
                        try {
                            settings.setFullscreen(br.readLine().toLowerCase(Locale.ENGLISH).contains("full"));
                        } catch(Exception e) {
                            settings.setFullscreen(false);
                        }
                        br.close();
                    } catch(Exception e) {
                        settings.setWidth(1280);
                        settings.setHeight(720);
                    }
                } else {
                    settings.setWidth(1280);
                    settings.setHeight(720);
                    settings.setFullscreen(false);
                }
                settings.setResizable(false);
            }
            settings.setSwapBuffers(true);
        } else {
            // use basic mirroring window, skip settings window
            settings.setSamples(1);
            settings.setWidth(xWin);
            settings.setHeight(yWin);
            settings.setBitsPerPixel(32);     
            settings.setFrameRate(0);
            settings.setFrequency(VRhardware.getDisplayFrequency());
            settings.setFullscreen(false);
            settings.setVSync(false); // stop vsyncing on primary monitor!
            settings.setSwapBuffers(disableSwapBuffers);
        }

        // Updating application settings
        stateManager.getApplication().setSettings(settings);
        logger.config("Updated underlying application settings.");
        
    }

    @Override
    public void cleanup() {
        if( VRhardware != null ) {
            VRhardware.destroy();
            VRhardware = null;
        }        
        disableVR = true;
        
        this.application  = null;
        this.stateManager = null;
    }
    
    @Override
    public void stateDetached(AppStateManager stateManager) {
      super.stateDetached(stateManager);
    }
    
    /**
     * Process the attached settings and apply changes to this app state.
     * @param settings the app settings to process.
     */
    protected void processSettings(AppSettings settings){
    	if (settings != null){
    		if (settings.get(VRConstants.SETTING_USE_COMPOSITOR) != null){
    			useCompositor = settings.getBoolean(VRConstants.SETTING_USE_COMPOSITOR);
                if( useCompositor == false ){
                	disableSwapBuffers = false;
                }
    		}

            if (settings.get(VRConstants.SETTING_VR_FORCE) != null){
            	forceVR = settings.getBoolean(VRConstants.SETTING_VR_FORCE);
    		}
    		
            if (settings.get(VRConstants.SETTING_FLIP_EYES) != null){
                if( VRhardware != null ){
                	VRhardware._setFlipEyes(settings.getBoolean(VRConstants.SETTING_FLIP_EYES));
                } 
    		}
    	    
            if (settings.get(VRConstants.SETTING_GUI_OVERDRAW) != null){
            	guiManager._enableGuiOverdraw(settings.getBoolean(VRConstants.SETTING_GUI_OVERDRAW));
    		}
    	    
            if (settings.get(VRConstants.SETTING_GUI_CURVED_SURFACE) != null){
            	guiManager._enableCurvedSuface(settings.getBoolean(VRConstants.SETTING_GUI_CURVED_SURFACE));
    		}
    	    
            if (settings.get(VRConstants.SETTING_ENABLE_MIRROR_WINDOW) != null){
                if( useCompositor == false ) {
                    disableSwapBuffers = false;
                } else {
                	disableSwapBuffers = !settings.getBoolean(VRConstants.SETTING_ENABLE_MIRROR_WINDOW);
                }
    		}
    	    
            if (settings.get(VRConstants.SETTING_DISABLE_VR) != null){
                DISABLE_VR = settings.getBoolean(VRConstants.SETTING_DISABLE_VR);
    		}
    	    
            if (settings.get(VRConstants.SETTING_SEATED_EXPERIENCE) != null){
            	seated = settings.getBoolean(VRConstants.SETTING_SEATED_EXPERIENCE);
    		}
    	    
            if (settings.get(VRConstants.SETTING_NO_GUI) != null){
            	nogui = settings.getBoolean(VRConstants.SETTING_NO_GUI);
    		}
    	    
            if (settings.get(VRConstants.SETTING_INSTANCE_RENDERING) != null){
            	instanceVR = settings.getBoolean(VRConstants.SETTING_INSTANCE_RENDERING);
    		}

            if (settings.get(VRConstants.SETTING_DEFAULT_FOV) != null){
            	defaultFOV = settings.getFloat(VRConstants.SETTING_DEFAULT_FOV);
    		}
            
            if (settings.get(VRConstants.SETTING_DEFAULT_ASPECT_RATIO) != null){
            	defaultAspect = settings.getFloat(VRConstants.SETTING_DEFAULT_ASPECT_RATIO);
    		}
            
            if (settings.get(VRConstants.SETTING_VRAPI) != null){
            	vrBinding = settings.getInteger(VRConstants.SETTING_VRAPI);
    		}
            
    	}
    }
}
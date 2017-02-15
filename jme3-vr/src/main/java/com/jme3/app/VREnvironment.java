package com.jme3.app;

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.app.state.AppState;
import com.jme3.input.vr.OSVR;
import com.jme3.input.vr.OpenVR;
import com.jme3.input.vr.VRAPI;
import com.jme3.input.vr.VRInputAPI;
import com.jme3.renderer.Camera;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;
import com.jme3.system.jopenvr.JOpenVRLibrary;
import com.jme3.util.VRGuiManager;
import com.jme3.util.VRMouseManager;
import com.jme3.util.VRViewManager;
import com.jme3.util.VRViewManagerOSVR;
import com.jme3.util.VRViewManagerOpenVR;

public class VREnvironment {
	
	private static final Logger logger = Logger.getLogger(VREnvironment.class.getName());
	
    private VRAPI hardware              = null;
    private VRGuiManager guiManager     = null;
    private VRMouseManager mouseManager = null;
    private VRViewManager viewmanager   = null;
    
    /**
     * The underlying system VR API. By default set to {@link VRConstants#SETTING_VRAPI_OPENVR_VALUE}.
     */
    public int vrBinding = VRConstants.SETTING_VRAPI_OPENVR_VALUE;
    
    private boolean seated        = false;
    
    private Spatial observer      = null;
    
    private boolean forceVR       = false;
    
    private boolean vrSupportedOS = false;
    
    private boolean nogui = false;
    
    private boolean compositorOS;
    
    private boolean useCompositor = true;
    
    private boolean instanceRendering = false;
    
    private boolean disableSwapBuffers = true;
    
	private float defaultFOV           = 108f;
	
    private float defaultAspect        = 1f;
    
    private AppSettings settings = null;
    
    private Application application = null;
    
    private Camera dummyCam = null;
    
    private AppState app = null;
    
    private boolean initialized = false;
    
    private boolean attached    = false;
    
    public VREnvironment(AppSettings settings){
    	
    	this.settings = settings;
    	
        guiManager   = new VRGuiManager(this);
        mouseManager = new VRMouseManager(this);
        dummyCam = new Camera();
        
        processSettings();
    }
    
	/**
	 * Get the VR underlying hardware.
	 * @return the VR underlying hardware.
	 */
	public VRAPI getVRHardware() {
	    return hardware;
	}
	
	/**
	 * Get the VR dedicated input.
	 * @return the VR dedicated input.
	 */
	public VRInputAPI getVRinput() {
	    if( hardware == null ){
	    	return null;
	    }
	    
	    return hardware.getVRinput();
	}
	
	/**
	 * Get the VR view manager.
	 * @return the VR view manager.
	 */
	public VRViewManager getVRViewManager() {
	    return viewmanager;
	}
	
	/**
	 * Get the GUI manager attached to this environment.
	 * @return the GUI manager attached to this environment.
	 */
	public VRGuiManager getVRGUIManager(){
		return guiManager;
	}
	
	/**
	 * Get the VR mouse manager attached to this environment.
	 * @return the VR mouse manager attached to this environment.
	 */
	public VRMouseManager getVRMouseManager(){
		return mouseManager;
	}

    /**
     * Can be used to change seated experience during runtime.
     * @param isSeated <code>true</code> if designed for sitting, <code>false</code> for standing/roomscale
     * @see #isSeatedExperience()
     */
    public void setSeatedExperience(boolean isSeated) {
        seated = isSeated;
        if( hardware instanceof OpenVR ) {
            if( hardware.getCompositor() == null ) {
            	return;
            }
            
            if( seated ) {
                ((OpenVR)hardware).getCompositor().SetTrackingSpace.apply(JOpenVRLibrary.ETrackingUniverseOrigin.ETrackingUniverseOrigin_TrackingUniverseSeated);
            } else {
                ((OpenVR)hardware).getCompositor().SetTrackingSpace.apply(JOpenVRLibrary.ETrackingUniverseOrigin.ETrackingUniverseOrigin_TrackingUniverseStanding);                
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
     * Set the VR headset height from the ground.
     * @param amount the VR headset height from the ground.
     * @see #getVRHeightAdjustment()
     */
    public void setVRHeightAdjustment(float amount) {
        if( viewmanager != null ){
        	viewmanager.setHeightAdjustment(amount);
        }
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
     * Get the scene observer. If no observer has been set, this method return the application {@link #getCamera() camera}.
     * @return the scene observer. 
     * @see #setObserver(Spatial)
     */
    public Object getObserver() {
        if( observer == null ) {
        	
        	if (application != null){
              return application.getCamera();
        	} else {
        		throw new IllegalStateException("VR environment is not attached to any application.");
        	}
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
	 * Get the {@link AppSettings settings} attached to this environment.
	 * @return the {@link AppSettings settings} attached to this environment.
	 * @see #setSettings(AppSettings)
	 */
	public AppSettings getSettings(){
		return settings;
	}
	
	/**
	 * Set the {@link AppSettings settings} attached to this environment.
	 * @param settings the {@link AppSettings settings} attached to this environment.
	 * @see #getSettings()
	 */
	public void setSettings(AppSettings settings){
		this.settings = settings;
		processSettings();
	}
    
    /**
     * Get if the system currently support VR.
     * @return <code>true</code> if the system currently support VR and <code>false</Code> otherwise.
     */
    public boolean isVRSupported() {
        return vrSupportedOS;
    }
	
    /**
     * Check if the VR mode is enabled.
     * @return <code>true</code> if the VR mode is enabled and <code>false</code> otherwise.
     */
    public boolean isInVR() {
        return (forceVR || vrSupportedOS && hardware != null && hardware.isInitialized() && isInitialized());
    }

    /**
     * Check if the rendering is instanced (see <a href="https://en.wikipedia.org/wiki/Geometry_instancing">Geometry instancing</a>).
     * @return <code>true</code> if the rendering is instanced and <code>false</code> otherwise.
     */
    public boolean isInstanceRendering() {
        return instanceRendering;
    }
    
    public boolean isSwapBuffers(){
    	return disableSwapBuffers;
    }
    
    /**
     * Check if the application has a GUI overlay attached.
     * @return <code>true</code> if the application has a GUI overlay attached and <code>false</code> otherwise.
     */
    public boolean hasTraditionalGUIOverlay() {
        return !nogui;
    }
    
    /**
     * Check if the VR environment is initialized. A call to the {@link #initialize() initialize()} method should set this value to <code>true</code>
     * @return <code>true</code> if the VR environment is initialized and <code>false</code> otherwise.
     */
    public boolean isInitialized(){
    	return initialized;
    }
    
    /**
     * Is the VR compositor is active.
     * @return <code>true</code> if the VR compositor is active and <code>false</code> otherwise.
     */
    public boolean compositorAllowed() {
        return useCompositor && compositorOS;
    }

    /**
     * Reset headset pose if seating experience.
     */
    public void resetSeatedPose(){
        if( vrSupportedOS == false || isSeatedExperience() == false ){
        	return;
        }
        getVRHardware().reset();
    }
    
    public AppState getAppState(){
    	return app;
    }
    
    public Application getApplication(){
    	return application;
    }
    
    /**
     * Get the {@link Camera camera} used for rendering. 
     * If the VR mode is {@link #isInVR() active}, this method return a dummy camera, otherwise, 
     * this method return the camera of the attached application.
     * @return the camera attached used for rendering.
     */
    public Camera getCamera() {
        if( isInVR() && getVRViewManager() != null && getVRViewManager().getLeftCamera() != null ) {
            return dummyCam;
        }
        
        return application.getCamera();
    }
    
    public Camera getDummyCamera(){
    	
    	if (dummyCam == null){
    		
    		if (application != null){
    			
    			if (application.getCamera() != null){
    				dummyCam = application.getCamera().clone();
    			} else {
    				return new Camera();
    			}
    		} else {
    			throw new IllegalStateException("VR environment is not attached to any application.");
    		}
    	}
    	
    	return dummyCam;
    }
    
    /**
     * Attach the VR environment to the given app state and application. 
     * This method should be called within the {@link AppState#stateAttached(com.jme3.app.state.AppStateManager) stateAttached(com.jme3.app.state.AppStateManager)} method 
     * from the app state.
     * @param appState the app state to attach.
     * @param application the application to attach.
     */
    public void atttach(AppState appState, Application application){    	
    	this.application = application;
    	this.app         = appState;
    	
    	// Instanciate view manager
    	if (vrBinding == VRConstants.SETTING_VRAPI_OPENVR_VALUE){
    		viewmanager = new VRViewManagerOpenVR(this);
    	} else if (vrBinding == VRConstants.SETTING_VRAPI_OSVR_VALUE){
    		viewmanager = new VRViewManagerOSVR(this);
    	} else {
    		logger.severe("Cannot instanciate view manager, unknown VRAPI type: "+vrBinding);
    	}
    }
    
    /**
     * Initialize this VR environment. This method enable the system bindings and configure all the VR system modules. 
     * A call to this method has to be made before any use of VR capabilities.
     * @return <code>true</code> if the VR environment is successfully initialized and <code>false</code> otherwise.
     */
    public boolean initialize(){
    	
    	logger.config("Initializing VR environment.");
    	
    	initialized = false;
    	
        // we are going to use OpenVR now, not the Oculus Rift
        // OpenVR does support the Rift
        String OS     = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
        vrSupportedOS = !OS.contains("nux") && System.getProperty("sun.arch.data.model").equalsIgnoreCase("64"); //for the moment, linux/unix causes crashes, 64-bit only
        compositorOS  = OS.contains("indows");
        
        if( vrSupportedOS) {
        	if( vrBinding == VRConstants.SETTING_VRAPI_OSVR_VALUE ) {
                hardware = new OSVR(this);
                initialized = true;
                logger.config("Creating OSVR wrapper [SUCCESS]");
            } else if( vrBinding == VRConstants.SETTING_VRAPI_OPENVR_VALUE ) {
            	hardware = new OpenVR(this);
            	initialized = true;
                logger.config("Creating OpenVR wrapper [SUCCESS]");
            } else {
            	logger.config("Cannot create VR binding: "+vrBinding+" [FAILED]");
            	logger.log(Level.SEVERE, "Cannot initialize VR environment [FAILED]");
            }
        	
            if( hardware.initialize() ) {
            	initialized &= true;
            	logger.config("VR native wrapper initialized [SUCCESS]");
            } else {
            	initialized &= false;
            	logger.warning("VR native wrapper initialized [FAILED]");
            	logger.log(Level.SEVERE, "Cannot initialize VR environment [FAILED]");
            }
        } else {
        	logger.log(Level.SEVERE, "System does not support VR capabilities.");
        	logger.log(Level.SEVERE, "Cannot initialize VR environment [FAILED]");
        }
    	
    	return initialized;
    }
    
    private void processSettings(){
    	if (settings != null){
    		
    		if (settings.get(VRConstants.SETTING_USE_COMPOSITOR) != null){
    			useCompositor = settings.getBoolean(VRConstants.SETTING_USE_COMPOSITOR);
                if( useCompositor == false ){
                	disableSwapBuffers = false;
                }
    		}
    		
            if (settings.get(VRConstants.SETTING_ENABLE_MIRROR_WINDOW) != null){
                if( useCompositor == false ) {
                    disableSwapBuffers = false;
                } else {
                	disableSwapBuffers = !settings.getBoolean(VRConstants.SETTING_ENABLE_MIRROR_WINDOW);
                }
    		}
    		
            if (settings.get(VRConstants.SETTING_GUI_OVERDRAW) != null){
            	getVRGUIManager().setGuiOverdraw(settings.getBoolean(VRConstants.SETTING_GUI_OVERDRAW));
    		}
    	    
            if (settings.get(VRConstants.SETTING_GUI_CURVED_SURFACE) != null){
            	getVRGUIManager().setCurvedSurface(settings.getBoolean(VRConstants.SETTING_GUI_CURVED_SURFACE));
    		}
    		
    		if (settings.get(VRConstants.SETTING_NO_GUI) != null){
            	nogui = settings.getBoolean(VRConstants.SETTING_NO_GUI);
    		}
    		
            if (settings.get(VRConstants.SETTING_VRAPI) != null){
            	vrBinding = settings.getInteger(VRConstants.SETTING_VRAPI);
    		}
            
            if (settings.get(VRConstants.SETTING_SEATED_EXPERIENCE) != null){
            	seated = settings.getBoolean(VRConstants.SETTING_SEATED_EXPERIENCE);
    		}
            
            if (settings.get(VRConstants.SETTING_INSTANCE_RENDERING) != null){
            	instanceRendering = settings.getBoolean(VRConstants.SETTING_INSTANCE_RENDERING);
    		}

            if (settings.get(VRConstants.SETTING_DEFAULT_FOV) != null){
            	defaultFOV = settings.getFloat(VRConstants.SETTING_DEFAULT_FOV);
    		}
            
            if (settings.get(VRConstants.SETTING_DEFAULT_ASPECT_RATIO) != null){
            	defaultAspect = settings.getFloat(VRConstants.SETTING_DEFAULT_ASPECT_RATIO);
    		}
            
            if (settings.get(VRConstants.SETTING_FLIP_EYES) != null){
                if( getVRHardware() != null ){
                	getVRHardware().setFlipEyes(settings.getBoolean(VRConstants.SETTING_FLIP_EYES));
                } 
    		}
    	}
    }
}

package com.jme3.app;

/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.input.vr.VRAPI;
import com.jme3.input.vr.VRInputAPI;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.PreNormalCaching;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;
import com.jme3.util.VRGUIPositioningMode;
import com.jme3.util.VRGuiManager;
import com.jme3.util.VRMouseManager;
import com.jme3.util.VRViewManager;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Iterator;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

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
     * Is the application has not to start within VR mode (default is <code>false</code>).
     */
    public boolean DISABLE_VR = false;
    


    private float fFar  = 1000f;
    private float fNear = 0.1f;
    private int xWin    = 1920;
    private int yWin    = 1080;
    
    private float resMult = 1f;
 
    /*
     where is the headset pointing, after all rotations are combined?
     depends on observer rotation, if any
     */
    private Quaternion tempq = new Quaternion();
    
    private Application application      = null;
    private AppStateManager stateManager = null;
    private AppSettings settings         = null;
    
    private VREnvironment environment    = null;
    
    /**
     * Create a new default VR app state that relies on the given {@link VREnvironment VR environment}.
     * @param environment the {@link VREnvironment VR environment} that this app state is using.
     */
    public VRAppState(VREnvironment environment) {
      super();

      this.environment = environment; 
      
      this.setSettings(environment.getSettings());
     }
    
    /**
     * Create a new VR app state with given settings. The app state relies on the the given {@link VREnvironment VR environment}.
     * @param settings the settings to use.
     * @param environment the {@link VREnvironment VR environment} that this app state is using.
     */
    public VRAppState(AppSettings settings, VREnvironment environment){
      this(environment);
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
        PreNormalCaching.resetCache(environment.isInVR());
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
        if( environment.getVRViewManager() != null ){
        	environment.getVRViewManager().setResolutionMultiplier(resMult);
        }
    }
   
	
    /**
     * Move filters from the main scene into the eye's.
     * This removes filters from the main scene.
     */
    public void moveScreenProcessingToVR() {
      environment.getVRViewManager().moveScreenProcessingToEyes();
    }
    
    /**
     * Get the observer final rotation within the scene.
     * @return the observer final rotation within the scene.
     * @see #getFinalObserverPosition()
     */
    public Quaternion getFinalObserverRotation() {
        if( environment.getVRViewManager() == null ) {
            if( environment.getObserver() == null ) {
                return environment.getCamera().getRotation();
            } else {
            	return ((Spatial)environment.getObserver()).getWorldRotation();
            }
        }  
        
        if( environment.getObserver() == null ) {
            tempq.set(environment.getDummyCamera().getRotation());
        } else {
            tempq.set(((Spatial)environment.getObserver()).getWorldRotation());
        }
        return tempq.multLocal(environment.getVRHardware().getOrientation());
    }
    
    /**
     * Get the observer final position within the scene.
     * @return the observer position.
     * @see #getFinalObserverRotation()
     */
    public Vector3f getFinalObserverPosition() {
        if( environment.getVRViewManager() == null ) {
            if( environment.getObserver() == null ) {
                return environment.getCamera().getLocation();
            } else{
            	return ((Spatial)environment.getObserver()).getWorldTranslation();            
            }
        }
        
        Vector3f pos = environment.getVRHardware().getPosition();
        if( environment.getObserver() == null ) {
        	environment.getDummyCamera().getRotation().mult(pos, pos);
            return pos.addLocal(environment.getDummyCamera().getLocation());
        } else {
        	((Spatial)environment.getObserver()).getWorldRotation().mult(pos, pos);
            return pos.addLocal(((Spatial)environment.getObserver()).getWorldTranslation());
        }
    }
    
    /**
     * Get the VR headset left viewport.
     * @return the VR headset left viewport.
     * @see #getRightViewPort()
     */
    public ViewPort getLeftViewPort() {
        if( environment.getVRViewManager() == null ){
        	return application.getViewPort();
        }
        
        return environment.getVRViewManager().getLeftViewport();
    }
    
    /**
     * Get the VR headset right viewport.
     * @return the VR headset right viewport.
     * @see #getLeftViewPort()
     */
    public ViewPort getRightViewPort() {
        if( environment.getVRViewManager() == null ){
        	return application.getViewPort();
        }
        return environment.getVRViewManager().getRightViewport();
    }
    
    /**
     * Set the background color for both left and right view ports.
     * @param clr the background color.
     */
    public void setBackgroundColors(ColorRGBA clr) {
        if( environment.getVRViewManager() == null ) {
            application.getViewPort().setBackgroundColor(clr);
        } else if( environment.getVRViewManager().getLeftViewport() != null ) {
        	
        	environment.getVRViewManager().getLeftViewport().setBackgroundColor(clr);
            
        	if( environment.getVRViewManager().getRightViewport() != null ){
            	environment.getVRViewManager().getRightViewport().setBackgroundColor(clr);
            }
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
     * Get the scene observer. If no observer has been set, this method return the application {@link #getCamera() camera}.
     * @return the scene observer. 
     * @see #setObserver(Spatial)
     */
    public Object getObserver() {
        return environment.getObserver();
    }
    
    /**
     * Set the scene observer. The VR headset will be linked to it. If no observer is set, the VR headset is linked to the the application {@link #getCamera() camera}.
     * @param observer the scene observer.
     */
    public void setObserver(Spatial observer) {
       environment.setObserver(observer);
    }
    
    /**
     * Check if the rendering is instanced (see <a href="https://en.wikipedia.org/wiki/Geometry_instancing">Geometry instancing</a>).
     * @return <code>true</code> if the rendering is instanced and <code>false</code> otherwise.
     */
    public boolean isInstanceRendering() {
        return environment.isInstanceRendering();
    }
    
    /**
     * Return the {@link VREnvironment VR environment} on which this app state relies. 
     * @return the {@link VREnvironment VR environment} on which this app state relies. 
     */
    public VREnvironment getVREnvironment(){
    	return environment;
    }
    
	/**
	 * Get the VR underlying hardware.
	 * @return the VR underlying hardware.
	 */
	public VRAPI getVRHardware() {
	    return getVREnvironment().getVRHardware();
	}
	
	/**
	 * Get the VR dedicated input.
	 * @return the VR dedicated input.
	 */
	public VRInputAPI getVRinput() {
	    if( getVREnvironment().getVRHardware() == null ){
	    	return null;
	    }
	    
	    return getVREnvironment().getVRHardware().getVRinput();
	}
	
	/**
	 * Get the VR view manager.
	 * @return the VR view manager.
	 */
	public VRViewManager getVRViewManager() {
	    return getVREnvironment().getVRViewManager();
	}
	
	/**
	 * Get the GUI manager attached to this app state.
	 * @return the GUI manager attached to this app state.
	 */
	public VRGuiManager getVRGUIManager(){
		return getVREnvironment().getVRGUIManager();
	}
	
	/**
	 * Get the VR mouse manager attached to this app state.
	 * @return the VR mouse manager attached to this application.
	 */
	public VRMouseManager getVRMouseManager(){
		return getVREnvironment().getVRMouseManager();
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
        if( environment.getVRViewManager() != null ) {
        	environment.getVRViewManager().update(tpf);    
        } else if( environment.getObserver() != null ) {
            environment.getCamera().setFrame(((Spatial)environment.getObserver()).getWorldTranslation(), ((Spatial)environment.getObserver()).getWorldRotation());
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
        
        if( environment.isInVR() == false || environment.getVRGUIManager().getPositioningMode() == VRGUIPositioningMode.MANUAL ) {
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
        environment.getVRMouseManager().updateAnalogAsMouse(0, null, null, null, tpf);
    }

    @Override
    public void postRender() {
        super.postRender();
        
        // update compositor
        if( environment.getVRViewManager() != null ) {
        	environment.getVRViewManager().postRender();
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
        
        app.getCamera().setFrustumFar(fFar);
        app.getCamera().setFrustumNear(fNear);

        if( environment.isInVR() ) {
        	
        	logger.config("VR mode enabled.");
        	
            if( environment.getVRHardware() != null ) {
            	environment.getVRHardware().initVRCompositor(environment.compositorAllowed());
            } else {
            	logger.warning("No VR system found.");
            }
            
            
            environment.getVRViewManager().setResolutionMultiplier(resMult);
            //inputManager.addMapping(RESET_HMD, new KeyTrigger(KeyInput.KEY_F9));
            //setLostFocusBehavior(LostFocusBehavior.Disabled);
        } else {
        	logger.config("VR mode disabled.");
            //viewPort.attachScene(rootNode);
            //guiViewPort.attachScene(guiNode);
        }
        
        if( environment.getVRViewManager() != null ) {
        	environment.getVRViewManager().initialize();
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
   
        // Attach VR environment to the application
        if (!environment.isInitialized()){
        	environment.initialize();
        }
        
        if (environment.isInitialized()){
        	environment.atttach(this, stateManager.getApplication());
        } else {
        	logger.severe("Cannot attach VR environment to the VR app state as its not initialized.");
        }

        GraphicsDevice defDev = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
                                    
        if( environment.isInVR() && !environment.compositorAllowed() ) {
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
        	if (!environment.isInVR()){
        	  logger.config("Cannot switch to VR mode (VR disabled by user).");
        	} else if (!environment.compositorAllowed()){
        	  logger.warning("Cannot switch to VR mode (VR not supported).");
        	}
        }
        
        if( !environment.isInVR() ) {
        	
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
            settings.setFrequency(environment.getVRHardware().getDisplayFrequency());
            settings.setFullscreen(false);
            settings.setVSync(false); // stop vsyncing on primary monitor!
            settings.setSwapBuffers(environment.isSwapBuffers());
        }

        // Updating application settings
        stateManager.getApplication().setSettings(settings);
        logger.config("Updated underlying application settings.");
        
    }

    @Override
    public void cleanup() {
        if( environment.getVRHardware() != null ) {
        	environment.getVRHardware().destroy();
        }        
        
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

            if (settings.get(VRConstants.SETTING_DISABLE_VR) != null){
                DISABLE_VR = settings.getBoolean(VRConstants.SETTING_DISABLE_VR);
    		}
    	}
    }
}
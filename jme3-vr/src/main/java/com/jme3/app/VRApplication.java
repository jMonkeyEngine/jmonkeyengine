package com.jme3.app;

import com.jme3.app.AppTask;
import com.jme3.app.Application;
import com.jme3.app.LegacyApplication;
import com.jme3.app.LostFocusBehavior;
import com.jme3.app.ResetStatsState;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioContext;
import com.jme3.audio.AudioRenderer;
import com.jme3.audio.Listener;
import com.jme3.input.InputManager;
import com.jme3.input.JoyInput;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.TouchInput;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.vr.OSVR;
import com.jme3.input.vr.OpenVR;
import com.jme3.input.vr.VRAPI;
import com.jme3.input.vr.VRInputAPI;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.PreNormalCaching;
import com.jme3.profile.AppProfiler;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.system.JmeContext.Type;
import com.jme3.system.jopenvr.JOpenVRLibrary;
import com.jme3.system.JmeSystem;
import com.jme3.system.NanoTimer;
import com.jme3.system.SystemListener;
import com.jme3.system.Timer;
import com.jme3.system.lwjgl.LwjglDisplayVR;
import com.jme3.system.lwjgl.LwjglOffscreenBufferVR;
import com.jme3.util.VRGUIPositioningMode;
import com.jme3.util.VRGuiManager;
import com.jme3.util.VRMouseManager;
import com.jme3.util.VRViewManagerOpenVR;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lwjgl.system.Platform;


/**
 * A JMonkey application dedicated to Virtual Reality. An application that use VR devices (HTC vive, ...) has to extends this one.<br>
 * <p>
 * <b>This class is no more functional and is deprecated. Please use {@link VRAppState VRAppState} instead.</b>
 * @author reden - phr00t - https://github.com/phr00t
 * @author Julien Seinturier - (c) 2016 - JOrigin project - <a href="http://www.jorigin.org">http:/www.jorigin.org</a>
 * @deprecated use {@link VRAppState VRAppState} instead.
 */
public abstract class VRApplication implements Application, SystemListener {

	
    private static final Logger logger = Logger.getLogger(LegacyApplication.class.getName());

    /**
     * The default FOV.
     */
    public float DEFAULT_FOV    = 108f;
    
    
    /**
     * The default aspect ratio.
     */
    public float DEFAULT_ASPECT = 1f;
    
    /**
     * Is the application is based on OSVR (default is <code>false</code>).
     */
    public boolean CONSTRUCT_WITH_OSVR = false;
    
    /**
     * Is the application has not to start within VR mode (default is <code>false</code>).
     */
    public boolean DISABLE_VR = false;
    
    /**
     * VR application configuration parameters.
     * @author reden - phr00t - https://github.com/phr00t
     * @author Julien Seinturier - (c) 2016 - JOrigin project - <a href="http://www.jorigin.org">http:/www.jorigin.org</a>
     *
     */
    public static enum PreconfigParameter {
    	/**
    	 * Is the SteamVR compositor is used (kinda needed at the moment)
    	 */
        USE_VR_COMPOSITOR, 
        
        /**
         * Render two eyes, regardless of VR API detection.
         */
        FORCE_VR_MODE, 
        
        /**
         * Invert the eyes.
         */
        FLIP_EYES,
        
        /**
         * Show GUI even if it is behind objects.
         */
        SET_GUI_OVERDRAW, 
        
        /**
         * 
         */
        SET_GUI_CURVED_SURFACE, 
        
        /**
         * Display a mirror rendering on the screen. Runs faster when set to <code>false</code>.
         */
        ENABLE_MIRROR_WINDOW, 
        
        /**
         * 
         */
        PREFER_OPENGL3, 
        
        /**
         * Disable VR rendering, regardless VR API and devices are presents.
         */
        DISABLE_VR,
        
        /**
         * 
         */
        SEATED_EXPERIENCE, 
        
        /**
         * Remove GUI node from the application.
         */
        NO_GUI, 
        
        /**
         * Faster VR rendering, requires some vertex shader changes (see Common/MatDefs/VR/Unshaded.j3md)
         */
        INSTANCE_VR_RENDERING, 
        
        /**
         * 
         */
        FORCE_DISABLE_MSAA
    }
    
    private VRAPI VRhardware            = null;
    private VRGuiManager guiManager     = null;
    private VRMouseManager mouseManager = null;
    private VRViewManagerOpenVR viewmanager   = null;
    
    private String OS;
     
    private Camera dummyCam;
    private Spatial observer;
    private boolean VRSupportedOS;
    private boolean forceVR;
    private boolean disableSwapBuffers = true;
    private boolean tryOpenGL3 = true;
    private boolean seated;
    private boolean nogui;
    private boolean instanceVR;
    private boolean forceDisableMSAA;
    
    // things taken from LegacyApplication
    private AppStateManager stateManager;    
    private Camera cam;    
    private AppSettings settings;
    private JmeContext context;    
    private float speed = 1f;
    private AudioRenderer audioRenderer;    
    private LostFocusBehavior lostFocusBehavior = LostFocusBehavior.ThrottleOnLostFocus;
    private final ConcurrentLinkedQueue<AppTask<?>> taskQueue = new ConcurrentLinkedQueue<AppTask<?>>();
    private Timer timer = new NanoTimer();
    private boolean paused = false, inputEnabled = true;
    private InputManager inputManager;
    private RenderManager renderManager;    
    private ViewPort viewPort;
    private ViewPort guiViewPort;
    private AssetManager assetManager;
    private Renderer renderer;
    private Listener listener;
    private MouseInput mouseInput;
    private KeyInput keyInput;
    private JoyInput joyInput;
    private TouchInput touchInput;

    protected Node guiNode, rootNode;
    
    private float fFar = 1000f, fNear = 1f;
    private int xWin = 1280, yWin = 720;
    
    private float resMult = 1f;
    
    private boolean useCompositor = true, compositorOS;
    private final String RESET_HMD = "ResetHMD";  
    
    /**
     * Create a new VR application and attach the given {@link AppState app states}.<br>
     * The application scene is made of a {@link #getRootNode() root node} that holds the scene spatials 
     * and a {@link #getGuiNode() GUI node} that is the root of the Graphical user interface.
     * @param initialStates the {@link AppState app states} to attach to the application.
     */
    public VRApplication(AppState... initialStates) {
        this();
        
        if (initialStates != null) {
            for (AppState a : initialStates) {
                if (a != null) {
                    stateManager.attach(a);
                }
            }
        }
    }
    
    /**
     * Create a new VR application.<br> 
     * The application scene is made of a {@link #getRootNode() root node} that holds the scene spatials 
     * and a {@link #getGuiNode() GUI node} that is the root of the Graphical user interface.
     */
    public VRApplication() {
        super();
        
        rootNode = new Node("root");
        guiNode = new Node("guiNode");
        
        guiNode.setQueueBucket(Bucket.Gui);
        guiNode.setCullHint(CullHint.Never);
        dummyCam = new Camera();
        
        initStateManager();
        
        // Create the GUI manager.
        guiManager = new VRGuiManager(null);
        
        // Create a new view manager.
        viewmanager = new VRViewManagerOpenVR(null);
        
        // Create a new mouse manager.
        mouseManager = new VRMouseManager(null);
        
        // we are going to use OpenVR now, not the Oculus Rift
        // OpenVR does support the Rift
        OS = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
        VRSupportedOS = !OS.contains("nux") && System.getProperty("sun.arch.data.model").equalsIgnoreCase("64"); //for the moment, linux/unix causes crashes, 64-bit only
        compositorOS = OS.contains("indows");
        
        if( !VRSupportedOS ) {
        	logger.warning("Non-supported OS: " + OS + ", architecture: " + System.getProperty("sun.arch.data.model"));
        } else if( DISABLE_VR ) {
        	logger.warning("VR disabled via code.");
        } else if( VRSupportedOS && DISABLE_VR == false ) {
            if( CONSTRUCT_WITH_OSVR ) {
            	//FIXME: WARNING !!
                VRhardware = new OSVR(null);
                logger.config("Creating OSVR wrapper [SUCCESS]");
            } else {
            	//FIXME: WARNING !!
                VRhardware = new OpenVR(null);
                logger.config("Creating OpenVR wrapper [SUCCESS]");
            }
            if( VRhardware.initialize() ) {
                setPauseOnLostFocus(false);
            }
        }
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
        if( VRhardware == null ) return null;
        return VRhardware.getVRinput();
    }
    
    /**
     * Get the VR view manager.
     * @return the VR view manager.
     */
    public VRViewManagerOpenVR getVRViewManager() {
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
        if( viewmanager != null ) viewmanager.setResolutionMultiplier(resMult);
    }
    
    
    /**
     * Is the SteamVR compositor is active.
     * @return <code>true</code> if the SteamVR compositor is active and <code>false</code> otherwise.
     */
    public boolean compositorAllowed() {
        return useCompositor && compositorOS;
    }
    
    /**
     * Get if the system currently support VR.
     * @return <code>true</code> if the system currently support VR and <code>false</Code> otherwise.
     */
    public boolean isOSVRSupported() {
        return VRSupportedOS;
    }
    
    /**
     * Simple update of the application, this method should contains {@link #getRootNode() root node} updates.
     * This method is called by the {@link #update() update()} method and should not be called manually.
     * @param tpf the application time.
     */
    public void simpleUpdate(float tpf) {   }
    
    /**
     * Rendering callback of the application. This method is called by the {@link #update() update()} method and should not be called manually.
     * @param renderManager the {@link RenderManager render manager}.
     */
    public void simpleRender(RenderManager renderManager) {
        PreNormalCaching.resetCache(isInVR());
    }

    
    /*
        we do NOT want to get & modify the distortion scene camera, so
        return the left viewport camera instead if we are in VR mode
    */
    @Override
    public Camera getCamera() {
        if( isInVR() && viewmanager != null && viewmanager.getLeftCamera() != null ) {
            return dummyCam;
        }
        return cam;
    }
    
    /**
     * Get the application internal camera.
     * @return the application internal camera.
     * @see #getCamera()
     */
    public Camera getBaseCamera() {
        return cam;
    }
 

    @Override
    public JmeContext getContext(){
        return context;
    }

    @Override
    public AssetManager getAssetManager(){
        return assetManager;
    }

    @Override
    public InputManager getInputManager(){
        return inputManager;
    }

    @Override
    public AppStateManager getStateManager() {
        return stateManager;
    }

    @Override
    public RenderManager getRenderManager() {
        return renderManager;
    }

    @Override
    public Renderer getRenderer(){
        return renderer;
    }

    @Override
    public AudioRenderer getAudioRenderer() {
        return audioRenderer;
    }

    @Override
    public Listener getListener() {
        return listener;
    }
    
    @Override
    public Timer getTimer(){
        return timer;
    }    

    /**
     * Handle the error given in parameters by creating a log entry and a dialog window. Internal use only.
     */
    public void handleError(String errMsg, Throwable t){
        // Print error to log.
        logger.log(Level.SEVERE, errMsg, t);
        // Display error message on screen if not in headless mode
        if (context.getType() != JmeContext.Type.Headless) {
            if (t != null) {
                JmeSystem.showErrorDialog(errMsg + "\n" + t.getClass().getSimpleName() +
                        (t.getMessage() != null ? ": " +  t.getMessage() : ""));
            } else {
                JmeSystem.showErrorDialog(errMsg);
            }
        }

        stop(); // stop the application
    }


    /**
     * Force the focus gain for the application. Internal use only.
     */
    public void gainFocus(){
        if (lostFocusBehavior != LostFocusBehavior.Disabled) {
            if (lostFocusBehavior == LostFocusBehavior.PauseOnLostFocus) {
                paused = false;
            }
            context.setAutoFlushFrames(true);
            if (inputManager != null) {
                inputManager.reset();
            }
        }
    }
    
    /**
     * Force the focus lost for the application. Internal use only.
     */
    public void loseFocus(){
        if (lostFocusBehavior != LostFocusBehavior.Disabled){
            if (lostFocusBehavior == LostFocusBehavior.PauseOnLostFocus) {
                paused = true;
            }
            context.setAutoFlushFrames(false);
        }
    }
    
    /**
     * Reshape the display window. Internal use only.
     */
    public void reshape(int w, int h){
        if (renderManager != null) {
            renderManager.notifyReshape(w, h);
        }
    }    

    /**
     * Request the application to close. Internal use only.
     */
    public void requestClose(boolean esc){
        context.destroy(false);
    }
    
    /**
     * Set the {@link AppSettings display settings} to define the display created.
     * <p>
     * Examples of display parameters include display frame {@link AppSettings#getWidth() width} and {@link AppSettings#getHeight() height},
     * pixel {@link AppSettings#getBitsPerPixel() color bit depth}, {@link AppSettings#getDepthBits() z-buffer bits}, {@link AppSettings#getSamples() anti-aliasing samples}, {@link AppSettings#getFrequency() update frequency}, ...
     * <br><br>If this method is called while the application is already running, then
     * {@link #restart() } must be called to apply the settings to the display.
     *
     * @param settings The settings to set.
     */
    public void setSettings(AppSettings settings){
        this.settings = settings;
        if (context != null && settings.useInput() != inputEnabled){
            // may need to create or destroy input based
            // on settings change
            inputEnabled = !inputEnabled;
            if (inputEnabled){
                initInput();
            }else{
                destroyInput();
            }
        }else{
            inputEnabled = settings.useInput();
        }
    }    
    
    /**
     * Sets the {@link Timer} implementation that will be used for calculating
     * frame times.<br><br>
     * By default, Application will use the Timer as returned by the current {@link JmeContext} implementation.
     * @param timer the timer to use.
     */
    public void setTimer(Timer timer){
        this.timer = timer;

        if (timer != null) {
            timer.reset();
        }

        if (renderManager != null) {
            renderManager.setTimer(timer);
        }
    }
    

    /**
     * Determine the application's behavior when unfocused.
     * @return The lost focus behavior of the application.
     */
    public LostFocusBehavior getLostFocusBehavior() {
        return lostFocusBehavior;
    }

    /**
     * Change the application's behavior when unfocused. By default, the application will
     * {@link LostFocusBehavior#ThrottleOnLostFocus throttle the update loop}
     * so as to not take 100% CPU usage when it is not in focus, e.g.
     * alt-tabbed, minimized, or obstructed by another window.
     *
     * @param lostFocusBehavior The new {@link LostFocusBehavior lost focus behavior} to use.
     */
    public void setLostFocusBehavior(LostFocusBehavior lostFocusBehavior) {
        this.lostFocusBehavior = lostFocusBehavior;
    }

    /**
     * Get if the application has to pause then it lost the focus.
     * @return <code>true</code> if pause on lost focus is enabled, <code>false</code> otherwise.
     * @see #getLostFocusBehavior()
     */
    public boolean isPauseOnLostFocus() {
        return getLostFocusBehavior() == LostFocusBehavior.PauseOnLostFocus;
    }

    /**
     * Enable or disable pause on lost focus.
     * <p>
     * By default, pause on lost focus is enabled.
     * If enabled, the application will stop updating
     * when it loses focus or becomes inactive (e.g. alt-tab).
     * For online or real-time applications, this might not be preferable,
     * so this feature should be set to disabled. For other applications,
     * it is best to keep it on so that CPU usage is not used when
     * not necessary.
     *
     * @param pauseOnLostFocus <code>true</code> to enable pause on lost focus, <code>false</code>
     * otherwise.
     *
     * @see #setLostFocusBehavior(com.jme3.app.LostFocusBehavior)
     */
    public void setPauseOnLostFocus(boolean pauseOnLostFocus) {
        if (pauseOnLostFocus) {
            setLostFocusBehavior(LostFocusBehavior.PauseOnLostFocus);
        } else {
            setLostFocusBehavior(LostFocusBehavior.Disabled);
        }
    }    
    
    @Override
    public void start() {
    	
    	logger.config("Starting application...");
    	
        // set some default settings in-case
        // settings dialog is not shown
        boolean loadSettings = false;
        if (settings == null) {
            setSettings(new AppSettings(true));
            loadSettings = true;
        }
        
        GraphicsDevice defDev = null;
		try {
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			defDev = ge.getDefaultScreenDevice();
		} catch (Throwable e1) {
			logger.log(Level.SEVERE, "Cannot access default screen device: "+e1.getMessage(), e1);
		}
		
        if( isInVR() && !compositorAllowed() ) {
        	logger.warning("VR Composition is not allowed.");
            // "easy extended" mode
            // TO-DO: JFrame was removed in LWJGL 3, need to use new GLFW library to pick "monitor" display of VR device
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
                    setSettings(settings);
                    //VRdev.setFullScreenWindow(VRwindow);
                    // make sure we are in the right display mode
                    if( VRdev.getDisplayMode().equals(useDM) == false ) {
                        VRdev.setDisplayMode(useDM);
                    }
                    // make a blank cursor to hide it
                    //BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
                    //Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImg, new Point(0, 0), "blank cursor");                    
                    //VRwindow.setCursor(blankCursor);
                    //jmeCanvas.getCanvas().setCursor(blankCursor);
                    //VRwindow.pack();
                    //VRwindow.setVisible(true);
                    //startCanvas();
                    logger.config("Starting application [SUCCESS]");
                    return;
                } catch(Exception e) { 
                	logger.log(Level.SEVERE, "Error during application start: "+e.getMessage(), e);
                }
            }
        }
        
        if( !isInVR() ) {
        	
        	logger.config("VR mode disabled.");
        	
            // not in VR, show settings dialog
            if( Platform.get() != Platform.MACOSX ) {
                if (!JmeSystem.showSettingsDialog(settings, loadSettings)) {
                	logger.config("Starting application [SUCCESS]");
                    return;
                }            
            } else {
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
        	
        	logger.config("VR mode enabled.");
        	
            // use basic mirroring window, skip settings window
            settings.setWidth(xWin);
            settings.setHeight(yWin);
            settings.setBitsPerPixel(24);     
            settings.setFrameRate(0); // never sleep in main loop
            settings.setFrequency(VRhardware.getDisplayFrequency());
            settings.setFullscreen(false);
            settings.setVSync(false); // stop vsyncing on primary monitor!
            settings.setSwapBuffers(!disableSwapBuffers || VRhardware instanceof OSVR);
            settings.setTitle("Put Headset On Now: " + settings.getTitle());
            settings.setResizable(true);
        }
        
        if( forceDisableMSAA ) {
        	logger.config("Disabling multisampling.");
            // disable multisampling, which is more likely to break things than be useful
            settings.setSamples(1);
        }
        
        // set opengl mode
        if( tryOpenGL3 ) {
        	logger.config("Using LWJGL OpenGL 3 renderer.");
            settings.setRenderer(AppSettings.LWJGL_OPENGL3);
        } else {
        	logger.config("Using LWJGL OpenGL 2 renderer.");
            settings.setRenderer(AppSettings.LWJGL_OPENGL2);
        }

        
        setSettings(settings);     
        start(JmeContext.Type.Display, false);
        
        // disable annoying warnings about GUI stuff being updated, which is normal behavior
        // for late GUI placement for VR purposes
        Logger.getLogger("com.jme3").setLevel(Level.SEVERE);        
    }    
    
    /**
     * Starts the application in {@link com.jme3.system.JmeContext.Type#Display display} mode.
     * @param waitFor if <code>true</code>, the method will wait until the application is started.
     * @see #start(com.jme3.system.JmeContext.Type, boolean)
     */
    public void start(boolean waitFor){
        start(JmeContext.Type.Display, waitFor);
    }   
    
    /**
     * Starts the application.
     * Creating a rendering context and executing the main loop in a separate thread.
     * @param contextType the {@link com.jme3.system.JmeContext.Type type} of the context to create.
     * @param waitFor if <code>true</code>, the method will wait until the application is started.
     * @throws IllegalArgumentException if the context type is not supported.
     */
    public void start(JmeContext.Type contextType, boolean waitFor){
        if (context != null && context.isCreated()){
            logger.warning("start() called when application already created!");
            return;
        }

        if (settings == null){
            settings = new AppSettings(true);
        }

        logger.log(Level.FINE, "Starting application: {0}", getClass().getName());
        
        // Create VR decicated context
        if (contextType == Type.Display){
          context = new LwjglDisplayVR();
          context.setSettings(settings);
        } else if (contextType == Type.OffscreenSurface){
          context = new LwjglOffscreenBufferVR();
          context.setSettings(settings);
        } else {
          logger.severe("Unsupported context type \""+contextType+"\". Supported are \"Display\" and \"OffscreenSurface\"");
          throw new IllegalArgumentException("Unsupported context type \""+contextType+"\". Supported are \"Display\" and \"OffscreenSurface\"");
        }

        context.setSystemListener(this);
        context.create(waitFor);
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
     * Set VR application {@link PreconfigParameter specific parameter}.
     * If making changes to default values, this must be called before the VRApplication starts
     * @param parm the parameter to set.
     * @param value the value of the parameter.
     */
    public void preconfigureVRApp(PreconfigParameter parm, boolean value) {        
        switch( parm ) {
            case SET_GUI_OVERDRAW:
                guiManager.setGuiOverdraw(value);
                break;
            case SET_GUI_CURVED_SURFACE:
            	guiManager.setCurvedSurface(value);
                break;
            case FORCE_VR_MODE:
                forceVR = value;
                break;
            //case USE_CUSTOM_DISTORTION: //deprecated, always using a render manager
            //    VRViewManager._setCustomDistortion(value);
            //    break;
            case USE_VR_COMPOSITOR:
                useCompositor = value;
                if( value == false ) disableSwapBuffers = false;
                break;
            case FLIP_EYES:
                if( VRhardware == null ) return;
                VRhardware.setFlipEyes(value);
                break;
            case INSTANCE_VR_RENDERING:
                instanceVR = value;
                break;
            case ENABLE_MIRROR_WINDOW:
                if( useCompositor == false ) {
                    disableSwapBuffers = false;
                } else disableSwapBuffers = !value;
                break;
            case PREFER_OPENGL3:
                tryOpenGL3 = value;
                break;
            case DISABLE_VR:
                DISABLE_VR = value;
                break;
            case NO_GUI:
                nogui = value;
                break;
            case SEATED_EXPERIENCE:
                seated = value;
                break;
            case FORCE_DISABLE_MSAA:
                forceDisableMSAA = value;
                break;
        }
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
     * Get the GUI node from the application.
     * @return the GUI node from the application.
     * @see #setGuiNode(Node)
     */
    public Node getGuiNode(){
        return guiNode;
    }
    
    /**
     * Set the GUI node that is displayed within the GUI viewport. 
     * Calling this method involve clearing all the scenes previously attached to the gui viewport.
     * @param node the GUI node to attach.
     * @see #getGuiNode()
     */
    public void setGuiNode(Node node){
    	if (node != null){
    		if (guiViewPort != null){
        		  
                enqueue(new Callable<Object>(){

				  @Override
				  public Object call() throws Exception {
				    guiViewPort.clearScenes();
				    guiViewPort.attachScene(node);
					guiNode = node;
					return null;
				  }
        			  
        		});

        	} else {
        		throw new IllegalArgumentException("GUI view port is not initialized.");
        	}
    	}
    	
    }
    
    /**
     * Get the root node of the application.
     * @return the root node of the application.
     */
    public Node getRootNode() {
        return rootNode;
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
    
    /*
        where is the headset pointing, after all rotations are combined?
        depends on observer rotation, if any
    */
    private static Quaternion tempq = new Quaternion();
    
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
        if( viewmanager != null ) return viewmanager.getHeightAdjustment();
        return 0f;
    }
      
    /**
     * Get the VR headset left viewport.
     * @return the VR headset left viewport.
     * @see #getRightViewPort()
     */
    public ViewPort getLeftViewPort() {
        if( viewmanager == null ) return getViewPort();
        return viewmanager.getLeftViewport();
    }
    
    /**
     * Get the VR headset right viewport.
     * @return the VR headset right viewport.
     * @see #getLeftViewPort()
     */
    public ViewPort getRightViewPort() {
        if( viewmanager == null ) return getViewPort();
        return viewmanager.getRightViewport();
    }
    
    
    /**
     * Set the background color for both left and right view ports.
     * @param clr the background color.
     */
    public void setBackgroundColors(ColorRGBA clr) {
        if( viewmanager == null ) {
            getViewPort().setBackgroundColor(clr);
        } else if( viewmanager.getLeftViewport() != null ) {
        	viewmanager.getLeftViewport().setBackgroundColor(clr);
            if( viewmanager.getRightViewport() != null ) viewmanager.getRightViewport().setBackgroundColor(clr);
        }
    }
    

    /**
     * Runs tasks enqueued via {@link #enqueue(Callable)}
     */
    protected void runQueuedTasks() {
	  AppTask<?> task;
        while( (task = taskQueue.poll()) != null ) {
            if (!task.isCancelled()) {
                task.invoke();
            }
        }
    }
    
    @Override
    public void update() {    
        // Make sure the audio renderer is available to callables
        AudioContext.setAudioRenderer(audioRenderer);

        runQueuedTasks();

        if (speed != 0 && !paused) {

            timer.update();

            if (inputEnabled){
                inputManager.update(timer.getTimePerFrame());
            }

            if (audioRenderer != null){
                audioRenderer.update(timer.getTimePerFrame());
            }
        }
        
        if (speed == 0 || paused) {
            try {
                Thread.sleep(50); // throttle the CPU when paused
            } catch (InterruptedException ex) {
                Logger.getLogger(SimpleApplication.class.getName()).log(Level.SEVERE, null, ex);
            }
            return;
        }
        
        float tpf = timer.getTimePerFrame() * speed;
        
        // update states
        stateManager.update(tpf);

        // simple update and root node
        simpleUpdate(tpf);
 
        
        // render states
        stateManager.render(renderManager);
        
        // update VR pose & cameras
        if( viewmanager != null ) {
        	viewmanager.update(tpf);    
        } else if( observer != null ) {
            getCamera().setFrame(observer.getWorldTranslation(), observer.getWorldRotation());
        }
        
        //FIXME: check if this code is necessary.
        // Updates scene and gui states.
        rootNode.updateLogicalState(tpf);
        guiNode.updateLogicalState(tpf);
        
        rootNode.updateGeometricState();
        
        if( isInVR() == false || guiManager.getPositioningMode() == VRGUIPositioningMode.MANUAL ) {
            // only update geometric state here if GUI is in manual mode, or not in VR
            // it will get updated automatically in the viewmanager update otherwise
            guiNode.updateGeometricState();
        }
        
        renderManager.render(tpf, context.isRenderable());
        simpleRender(renderManager);
        stateManager.postRender();
        
        // update compositor?
        if( viewmanager != null ) {
        	viewmanager.postRender();
        }
    }

    private void initAssetManager(){
        URL assetCfgUrl = null;

        if (settings != null){
            String assetCfg = settings.getString("AssetConfigURL");
            if (assetCfg != null){
                try {
                    assetCfgUrl = new URL(assetCfg);
                } catch (MalformedURLException ex) {
                }
                if (assetCfgUrl == null) {
                    assetCfgUrl = LegacyApplication.class.getClassLoader().getResource(assetCfg);
                    if (assetCfgUrl == null) {
                        logger.log(Level.SEVERE, "Unable to access AssetConfigURL in asset config:{0}", assetCfg);
                        return;
                    }
                }
            }
        }
        if (assetCfgUrl == null) {
            assetCfgUrl = JmeSystem.getPlatformAssetConfigURL();
        }
        if (assetManager == null){
            assetManager = JmeSystem.newAssetManager(assetCfgUrl);
            logger.config("Created asset manager from "+assetCfgUrl);
        }
    }
    

    private void initDisplay(){
        // aquire important objects
        // from the context
        settings = context.getSettings();

        // Only reset the timer if a user has not already provided one
        if (timer == null) {
            timer = context.getTimer();
        }

        renderer = context.getRenderer();
    }

    private void initAudio(){
        if (settings.getAudioRenderer() != null && context.getType() != JmeContext.Type.Headless){
            audioRenderer = JmeSystem.newAudioRenderer(settings);
            audioRenderer.initialize();
            AudioContext.setAudioRenderer(audioRenderer);

            listener = new Listener();
            audioRenderer.setListener(listener);
        }
    }

    /**
     * Creates the camera to use for rendering. Default values are perspective
     * projection with 45° field of view, with near and far values 1 and 1000
     * units respectively.
     */
    private void initCamera(){
        cam = new Camera(settings.getWidth(), settings.getHeight());

        cam.setFrustumPerspective(45f, (float)cam.getWidth() / cam.getHeight(), 1f, 1000f);
        cam.setLocation(new Vector3f(0f, 0f, 10f));
        cam.lookAt(new Vector3f(0f, 0f, 0f), Vector3f.UNIT_Y);

        renderManager = new RenderManager(renderer);
        //Remy - 09/14/2010 setted the timer in the renderManager
        renderManager.setTimer(timer);

        viewPort = renderManager.createMainView("Default", cam);
        viewPort.setClearFlags(true, true, true);

        // Create a new cam for the gui
        Camera guiCam = new Camera(settings.getWidth(), settings.getHeight());
        guiViewPort = renderManager.createPostView("Gui Default", guiCam);
        guiViewPort.setClearFlags(false, false, false);
    }

    /**
     * Initializes mouse and keyboard input. Also
     * initializes joystick input if joysticks are enabled in the
     * AppSettings.
     */
    private void initInput(){
        mouseInput = context.getMouseInput();
        if (mouseInput != null)
            mouseInput.initialize();

        keyInput = context.getKeyInput();
        if (keyInput != null)
            keyInput.initialize();

        touchInput = context.getTouchInput();
        if (touchInput != null)
            touchInput.initialize();

        if (!settings.getBoolean("DisableJoysticks")){
            joyInput = context.getJoyInput();
            if (joyInput != null)
                joyInput.initialize();
        }

        inputManager = new InputManager(mouseInput, keyInput, joyInput, touchInput);
    }

    private void initStateManager(){
        stateManager = new AppStateManager(this);

        // Always register a ResetStatsState to make sure
        // that the stats are cleared every frame
        stateManager.attach(new ResetStatsState());
    }    

    /**
     * Do not call manually.
     * Callback from ContextListener.
     * <p>
     * Initializes the <code>Application</code>, by creating a display and
     * default camera. If display settings are not specified, a default
     * 640x480 display is created. Default values are used for the camera;
     * perspective projection with 45° field of view, with near
     * and far values 1 and 1000 units respectively.
     */
    private void initialize_internal(){
        if (assetManager == null){
            initAssetManager();
        }

        initDisplay();
        initCamera();

        if (inputEnabled){
            initInput();
        }
        initAudio();

        // update timer so that the next delta is not too large
//        timer.update();
        timer.reset();

        // user code here..
    }
    
    @Override
    public void initialize() {
    	
    	logger.config("Initialize VR application...");
    	
        initialize_internal();
        cam.setFrustumFar(fFar);
        cam.setFrustumNear(fNear);
        dummyCam = cam.clone();
        if( isInVR() ) {
        	
        	logger.config("VR mode enabled.");
        	
            if( VRhardware != null ) {
                VRhardware.initVRCompositor(compositorAllowed());
            } else {
            	logger.warning("No VR system found.");
            }
            
            //FIXME: WARNING !!
            viewmanager = new VRViewManagerOpenVR(null);
            viewmanager.setResolutionMultiplier(resMult);
            inputManager.addMapping(RESET_HMD, new KeyTrigger(KeyInput.KEY_F9));
            setLostFocusBehavior(LostFocusBehavior.Disabled);
        } else {
        	logger.config("VR mode disabled.");
            viewPort.attachScene(rootNode);
            guiViewPort.attachScene(guiNode);
        }
        
        if( viewmanager != null ) {
        	viewmanager.initialize();
        }
        
        simpleInitApp();
        
        // any filters created, move them now
        if( viewmanager != null ) {
        	viewmanager.moveScreenProcessingToEyes();
            
            // print out camera information
            if( isInVR() ) {
                logger.info("VR Initialization Information");
                if( viewmanager.getLeftCamera() != null ){ 
                  logger.info("camLeft: " + viewmanager.getLeftCamera().toString());
                }
                
                if( viewmanager.getRightCamera() != null ){ 
                  logger.info("camRight: " + viewmanager.getRightCamera().toString());
                }
            }
        }
    }
    
    /**
     * Initialize the application. This method has to be overridden by implementations.
     */
    public abstract void simpleInitApp();
    
    /**
     * Destroy the application (release all resources).
     */
    public void destroy() {
        if( VRhardware != null ) {
            VRhardware.destroy();
            VRhardware = null;
        }        
        DISABLE_VR = true;
        stateManager.cleanup();

        destroyInput();
        if (audioRenderer != null)
            audioRenderer.cleanup();

        timer.reset();
        Runtime.getRuntime().exit(0);
    }
    
    protected void destroyInput(){
        if (mouseInput != null)
            mouseInput.destroy();

        if (keyInput != null)
            keyInput.destroy();

        if (joyInput != null)
            joyInput.destroy();

        if (touchInput != null)
            touchInput.destroy();

        inputManager = null;
    }
    
    @Override
    public ViewPort getGuiViewPort() {
        return guiViewPort;
    }

    @Override
    public ViewPort getViewPort() {
        return viewPort;
    }
    
    @Override
    public <V> Future<V> enqueue(Callable<V> callable) {
        AppTask<V> task = new AppTask<V>(callable);
        taskQueue.add(task);
        return task;
    }
    
    /**
     * Enqueues a runnable object to execute in the jME3
     * rendering thread.
     * <p>
     * Runnables are executed right at the beginning of the main loop.
     * They are executed even if the application is currently paused
     * or out of focus.
     *
     * @param runnable The runnable to run in the main jME3 thread
     */
    public void enqueue(Runnable runnable){
        enqueue(new RunnableWrapper(runnable));
    }

    private class RunnableWrapper implements Callable<Object>{
        private final Runnable runnable;

        public RunnableWrapper(Runnable runnable){
            this.runnable = runnable;
        }

        @Override
        public Object call(){
            runnable.run();
            return null;
        }

    }    

    /**
     * Requests the context to close, shutting down the main loop
     * and making necessary cleanup operations.
     *
     * Same as calling stop(false)
     *
     * @see #stop(boolean)
     */
    @Override
    public void stop(){
        stop(false);
    }

    /**
     * Requests the context to close, shutting down the main loop
     * and making necessary cleanup operations.
     * After the application has stopped, it cannot be used anymore.
     */
    @Override
    public void stop(boolean waitFor){
        logger.log(Level.FINE, "Closing application: {0}", getClass().getName());
        context.destroy(waitFor);
    }

    /**
     * Restarts the context, applying any changed settings.
     * <p>
     * Changes to the {@link AppSettings} of this Application are not
     * applied immediately; calling this method forces the context
     * to restart, applying the new settings.
     */
    @Override
    public void restart(){
        context.setSettings(settings);
        context.restart();
    }

    /**
     * Sets an AppProfiler hook that will be called back for
     * specific steps within a single update frame.  Value defaults
     * to null.
     */
    
    public void setAppProfiler(AppProfiler prof) {
        return;
    }

    /**
     * Returns the current AppProfiler hook, or null if none is set.
     */
    public AppProfiler getAppProfiler() {
        return null;
    }
}
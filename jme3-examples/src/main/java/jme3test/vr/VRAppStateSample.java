package jme3test.vr;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.VRAppState;
import com.jme3.app.VRConstants;
import com.jme3.app.VREnvironment;
import com.jme3.app.state.AppState;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.vr.VRInputType;
import com.jme3.input.vr.openvr.OpenVR;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.post.CartoonSSAO;
import com.jme3.post.FilterPostProcessor;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.MagFilter;
import com.jme3.texture.Texture.MinFilter;
import com.jme3.ui.Picture;
import com.jme3.util.SkyFactory;
import com.jme3.util.VRGUIPositioningMode;

/**
 * A Jmonkey sample that show the use of VR rendering with a JMonkey Application.
 * @author Julien Seinturier - COMEX SA - <a href="http://www.seinturier.fr">http://www.seinturier.fr</a>
 */
public class VRAppStateSample extends SimpleApplication {

	private static final Logger logger = Logger.getLogger(VRAppStateSample.class.getName());
    
    // general objects for scene management
    Node boxes = new Node("");
    Spatial observer;
    boolean moveForward, moveBackwards, rotateLeft, rotateRight;
    Material mat;
    Node mainScene;
    Geometry leftHand, rightHand;

    private float distance  = 100f;
    private float prod      = 0f;
    private float placeRate = 0f;
    
    VRAppState vrAppState = null;
    
    public VRAppStateSample(AppState... initialStates) {
        super(initialStates);
        
        vrAppState = getStateManager().getState(VRAppState.class);
    }
    
    
    @Override
    public void simpleInitApp() {    
    	
    	logger.info("Updating asset manager with "+System.getProperty("user.dir"));
    	getAssetManager().registerLocator(System.getProperty("user.dir")+File.separator+"assets", FileLocator.class);
    	
        mainScene = new Node("scene");
        observer = new Node("observer");
        
        Spatial sky = SkyFactory.createSky(getAssetManager(), "Textures/Sky/Bright/spheremap.png", SkyFactory.EnvMapType.EquirectMap);
        rootNode.attachChild(sky);
        
        Geometry box = new Geometry("", new Box(5,5,5));
        mat = new Material(getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        Texture noise = getAssetManager().loadTexture("Textures/noise.png");
        noise.setMagFilter(MagFilter.Nearest);
        noise.setMinFilter(MinFilter.Trilinear);
        noise.setAnisotropicFilter(16);
        mat.setTexture("ColorMap", noise);
                     
        // make the floor according to the size of our play area
        Geometry floor = new Geometry("floor", new Box(1f, 1f, 1f));
        
        Vector2f playArea = vrAppState.getVREnvironment().getVRBounds().getPlaySize();
        if( playArea == null ) {
            // no play area, use default size & height
            floor.setLocalScale(2f, 0.5f, 2f);
            floor.move(0f, -1.5f, 0f);
        } else {
            // cube model is actually 2x as big, cut it down to proper playArea size with * 0.5
            floor.setLocalScale(playArea.x * 0.5f, 0.5f, playArea.y * 0.5f);
            floor.move(0f, -0.5f, 0f);
        }
        floor.setMaterial(mat);
        rootNode.attachChild(floor);
        
        // hand wands
        leftHand = (Geometry)getAssetManager().loadModel("Models/vive_controller.j3o");
        rightHand = leftHand.clone();
        Material handMat = new Material(getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        handMat.setTexture("ColorMap", getAssetManager().loadTexture("Textures/vive_controller.png"));
        leftHand.setMaterial(handMat);
        rightHand.setMaterial(handMat);
        rootNode.attachChild(rightHand);
        rootNode.attachChild(leftHand);
        
        // gui element
        Vector2f guiCanvasSize = vrAppState.getVRGUIManager().getCanvasSize();
        Picture test = new Picture("testpic");
        test.setImage(getAssetManager(), "Textures/crosshair.png", true);
        test.setWidth(192f);
        test.setHeight(128f);
        test.setPosition(guiCanvasSize.x * 0.5f - 192f * 0.5f, guiCanvasSize.y * 0.5f - 128f * 0.5f);
        guiNode.attachChild(test);
        
        
        // test any positioning mode here (defaults to AUTO_CAM_ALL)
        vrAppState.getVRGUIManager().setPositioningMode(VRGUIPositioningMode.AUTO_OBSERVER_ALL);
        vrAppState.getVRGUIManager().setGuiScale(0.4f);
        
        box.setMaterial(mat);
        
        Geometry box2 = box.clone();
        box2.move(15, 0, 0);
        box2.setMaterial(mat);
        Geometry box3 = box.clone();
        box3.move(-15, 0, 0);
        box3.setMaterial(mat);        
        
        boxes.attachChild(box);
        boxes.attachChild(box2);
        boxes.attachChild(box3);
        rootNode.attachChild(boxes);
        
        observer.setLocalTranslation(new Vector3f(0.0f, 0.0f, 0.0f));
        
        vrAppState.setObserver(observer);
        mainScene.attachChild(observer);
        rootNode.attachChild(mainScene);
        
        addAllBoxes();

        initInputs();
        
        // use magic VR mouse cusor (same usage as non-VR mouse cursor)
        getInputManager().setCursorVisible(true);
      
        // filter test (can be added here like this)
        // but we are going to save them for the F key during runtime
        /*
        CartoonSSAO cartfilt = new CartoonSSAO();
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        fpp.addFilter(cartfilt);
        viewPort.addProcessor(fpp);        
        */
    }


     private void initInputs() {
        InputManager inputManager = getInputManager();
        inputManager.addMapping("toggle", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("incShift", new KeyTrigger(KeyInput.KEY_Q));
        inputManager.addMapping("decShift", new KeyTrigger(KeyInput.KEY_E));
        inputManager.addMapping("forward", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("back", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("filter", new KeyTrigger(KeyInput.KEY_F));
        inputManager.addMapping("dumpImages", new KeyTrigger(KeyInput.KEY_I));
        inputManager.addMapping("exit", new KeyTrigger(KeyInput.KEY_ESCAPE));
        
        ActionListener acl = new ActionListener() {

            public void onAction(String name, boolean keyPressed, float tpf) {
                if(name.equals("incShift") && keyPressed){
                	vrAppState.getVRGUIManager().adjustGuiDistance(-0.1f);
                }else if(name.equals("decShift") && keyPressed){
                	vrAppState.getVRGUIManager().adjustGuiDistance(0.1f);
                }else if(name.equals("filter") && keyPressed){
                    // adding filters in realtime
                    CartoonSSAO cartfilt = new CartoonSSAO(vrAppState.isInstanceRendering());
                    FilterPostProcessor fpp = new FilterPostProcessor(getAssetManager());
                    fpp.addFilter(cartfilt);
                    getViewPort().addProcessor(fpp);
                    // filters added to main viewport during runtime,
                    // move them into VR processing
                    // (won't do anything if not in VR mode)
                    vrAppState.moveScreenProcessingToVR();
                }
                if( name.equals("toggle") ) {
                	vrAppState.getVRGUIManager().positionGui();
                }                
                if(name.equals("forward")){
                    if(keyPressed){
                        moveForward = true;
                    } else {
                        moveForward = false;
                    }
                } else if(name.equals("back")){
                    if(keyPressed){
                        moveBackwards = true;
                    } else {
                        moveBackwards = false;
                    }
                } else if( name.equals("dumpImages") ) {
                    ((OpenVR)vrAppState.getVRHardware()).getCompositor().CompositorDumpImages.apply();
                }else if(name.equals("left")){
                    if(keyPressed){
                        rotateLeft = true;
                    } else {
                        rotateLeft = false;
                    }
                } else if(name.equals("right")){
                    if(keyPressed){
                        rotateRight = true;
                    } else {
                        rotateRight = false;
                    }
                } else if( name.equals("exit") ) {
                    stop(true);
                    System.exit(0);
                }
                
                
            }
        };
        inputManager.addListener(acl, "forward");
        inputManager.addListener(acl, "back");
        inputManager.addListener(acl, "left");
        inputManager.addListener(acl, "right");
        inputManager.addListener(acl, "toggle");
        inputManager.addListener(acl, "incShift");
        inputManager.addListener(acl, "decShift");
        inputManager.addListener(acl, "filter");
        inputManager.addListener(acl, "dumpImages");
        inputManager.addListener(acl, "exit");
    }
     
     @Override
     public void simpleUpdate(float tpf){

         //FPS test
         /*tpfAdder += tpf;
         tpfCount++;
         if( tpfCount == 60 ) {
             System.out.println("FPS: " + Float.toString(1f / (tpfAdder / tpfCount)));
             tpfCount = 0;
             tpfAdder = 0f;
         }*/
         
         prod+=tpf;
         distance = 100f * FastMath.sin(prod);
         boxes.setLocalTranslation(0, 0, 200f+ distance);
         
         if(moveForward){
             observer.move(vrAppState.getFinalObserverRotation().getRotationColumn(2).mult(tpf*8f));
         }
         if(moveBackwards){
             observer.move(vrAppState.getFinalObserverRotation().getRotationColumn(2).mult(-tpf*8f));
         }
         if(rotateLeft){
             observer.rotate(0, 0.75f*tpf, 0);
         }
         if(rotateRight){
             observer.rotate(0, -0.75f*tpf, 0);
         }
         
         handleWandInput(0, leftHand);
         handleWandInput(1, rightHand);
         if( placeRate > 0f ) placeRate -= tpf;
     }
     
     private void handleWandInput(int index, Geometry geo) {
    	 
         Quaternion q = vrAppState.getVRinput().getFinalObserverRotation(index);
         Vector3f v = vrAppState.getVRinput().getFinalObserverPosition(index);
         if( q != null && v != null ) {
             geo.setCullHint(CullHint.Dynamic); // make sure we see it
             geo.setLocalTranslation(v);
             geo.setLocalRotation(q);
             // place boxes when holding down trigger
             if( vrAppState.getVRinput().getAxis(index, VRInputType.ViveTriggerAxis).x >= 1f &&
                 placeRate <= 0f ) {
                 placeRate = 0.5f;
                 addBox(v, q, 0.1f);
                 vrAppState.getVRinput().triggerHapticPulse(index, 0.1f);
             }
             // print out all of the known information about the controllers here
             /*for(int i=0;i<VRInput.getRawControllerState(index).rAxis.length;i++) {
                 VRControllerAxis_t cs = VRInput.getRawControllerState(index).rAxis[i];
                 System.out.println("Controller#" + Integer.toString(index) + ", Axis#" + Integer.toString(i) + " X: " + Float.toString(cs.x) + ", Y: " + Float.toString(cs.y));
             }
             System.out.println("Button press: " + Long.toString(VRInput.getRawControllerState(index).ulButtonPressed.longValue()) + ", touch: " + Long.toString(VRInput.getRawControllerState(index).ulButtonTouched.longValue()));
             */
         } else {
             geo.setCullHint(CullHint.Always); // hide it             
         }
     }
     
     private void addAllBoxes() {
        float distance = 8;
        for (int x = 0; x < 35; x++) {
            float cos = FastMath.cos(x * FastMath.PI / 16f) * distance;
            float sin = FastMath.sin(x * FastMath.PI / 16f) * distance;
            Vector3f loc = new Vector3f(cos, 0, sin);
            addBox(loc, null, 1f);
            loc = new Vector3f(0, cos, sin);
            addBox(loc, null, 1f);
        }

    }

    private void addBox(Vector3f location, Quaternion rot, float scale) {
        Box b = new Box(0.3f, 0.3f, 0.3f);

        Geometry leftQuad = new Geometry("Box", b);
        if( rot != null ) {
            leftQuad.setLocalRotation(rot);
        } else {
            leftQuad.rotate(0.5f, 0f, 0f);
        }
        leftQuad.setLocalScale(scale);
        leftQuad.setMaterial(mat);
        leftQuad.setLocalTranslation(location);
        mainScene.attachChild(leftQuad);
    }
    
    private static void initLog(){
    	// Set the logger to display config messages.
    	Logger log = Logger.getLogger("");
    	log.setLevel(Level.FINE);
    	
    	// Disable Nifty µGUI logs
    	Logger.getLogger("de.lessvoid.nifty").setLevel(Level.SEVERE); 
    	Logger.getLogger("NiftyInputEventHandlingLog").setLevel(Level.SEVERE); 
        
        Filter filter = new Filter(){
			public boolean isLoggable(LogRecord record) {
				return true;
			}
        };
      
        Formatter formatter = new Formatter(){

          private final String lineSeparator = System.getProperty("line.separator");
          
          SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd-HH:mm:ss");
          
          @Override
          public String format(LogRecord record) {
            
            if (record != null){
              
              String simpleClassName = record.getSourceClassName();
              
              if (simpleClassName != null){
                int index = simpleClassName.lastIndexOf(".");
                if ((index > -1)&&(index < (simpleClassName.length() - 1))){
                  simpleClassName = simpleClassName.substring(index+1);
                }
              } else {
                simpleClassName = "Unknow,";
              }
              
              String level =  "";
              if (record.getLevel().equals(Level.FINEST)){
                level = "FINEST ";
              } else if (record.getLevel().equals(Level.FINER)){
                level = "FINER  ";
              } else if (record.getLevel().equals(Level.FINE)){
                level = "FINE   ";
              } else if (record.getLevel().equals(Level.CONFIG)){
                level = "CONFIG ";
              } else if (record.getLevel().equals(Level.INFO)){
                level = "INFO   ";
              } else if (record.getLevel().equals(Level.WARNING)){
                level = "WARNING";
              } else if (record.getLevel().equals(Level.SEVERE)){
                level = "SEVERE ";
              } else {
                level = "???????";
              }
              
              // Use record parameters
              String message = record.getMessage();
              if (record.getParameters() != null){
                for(int i = 0; i < record.getParameters().length; i++){
                  message = message.replace("{"+i+"}", ""+record.getParameters()[i]);
                }
              }
              
              if (record.getThrown() == null){
                return "("+sdf.format(new Date(record.getMillis()))+") "+level+" ["+simpleClassName+"] ["+record.getSourceMethodName()+"] "+message+lineSeparator;
              } else {
                String str = "("+sdf.format(new Date(record.getMillis()))+") "+level+" ["+simpleClassName+"] ["+record.getSourceMethodName()+"] caused by "+message+lineSeparator;
                
                StackTraceElement[] elements = record.getThrown().getStackTrace();
                for(int i = 0; i < elements.length; i++){
                  str += "("+sdf.format(new Date(record.getMillis()))+") "+level+" ["+simpleClassName+"] ["+record.getSourceMethodName()+"] at "+elements[i]+lineSeparator;
                }
                return "("+sdf.format(new Date(record.getMillis()))+") "+level+" ["+record.getSourceClassName()+"] ["+record.getSourceMethodName()+"] "+message+lineSeparator+str;
              }
            } else {
              return null;
            }
        }};  

    	// If the init is forced from a previous configuration, we remove the older handlers.
    	if (log != null){
          if (log.getHandlers() != null){
            for(int i = log.getHandlers().length - 1; i >= 0; i--){
              log.getHandlers()[i].setFilter(filter);
              log.getHandlers()[i].setFormatter(formatter);
              log.getHandlers()[i].setLevel(Level.CONFIG);
            }
          }
        }
    }
    
    /**
     * Create a {@link VRAppState VR app state} and use a Simple application that use it.<br>
     * The recommended procedure is:<br>
     * <ul>
     * <li>Create some {@link AppSettings AppSettings} with VR related parameters.
     * <li>Instanciate the {@link VRAppState VRAppState} attached to the settings.
     * <li>Instanciate your {@link Application Application}.
     * <li>Attach the settings to the application.
     * <li>Start the application.
     * </ul>
     * @param args not used
     */
    public static void main(String[] args){
    	
    	// Init the log to display all the configuration informations.
    	// This is not needed within final application.
    	initLog();
    	
    	// Prepare settings for VR rendering. 
    	// It is recommended to share same settings between the VR app state and the application.
    	AppSettings settings = new AppSettings(true);
    	
    	settings.put(VRConstants.SETTING_VRAPI, VRConstants.SETTING_VRAPI_OPENVR_VALUE); // The VR api to use (need to be present on the system)
    	settings.put(VRConstants.SETTING_DISABLE_VR, false);          // Enable VR
    	settings.put(VRConstants.SETTING_ENABLE_MIRROR_WINDOW, true); // Enable Mirror rendering oh the screen (disable to be faster)
    	settings.put(VRConstants.SETTING_VR_FORCE, false);            // Not forcing VR rendering if no VR system is found.
    	settings.put(VRConstants.SETTING_GUI_CURVED_SURFACE, true);   // Curve the mesh that is displaying the GUI
    	settings.put(VRConstants.SETTING_FLIP_EYES, false);           // Is the HMD eyes have to be inverted.
    	settings.put(VRConstants.SETTING_NO_GUI, false);              // enable gui.
    	settings.put(VRConstants.SETTING_GUI_OVERDRAW, true);         // show gui even if it is behind things.
    	
    	settings.put(VRConstants.SETTING_DEFAULT_FOV, 108f);          // The default ield Of View (FOV)
    	settings.put(VRConstants.SETTING_DEFAULT_ASPECT_RATIO, 1f);   // The default aspect ratio.
    	
    	settings.setRenderer(AppSettings.LWJGL_OPENGL3); // Setting the renderer. OpenGL 3 is needed if you're using Instance Rendering.
    	
        // The VR Environment.
        // This object is the interface between the JMonkey world (Application, AppState, ...) and the VR specific stuff.
    	VREnvironment environment = new VREnvironment(settings);
    	environment.initialize();
    	
    	// Checking if the VR environment is well initialized 
    	// (access to the underlying VR system is effective, VR devices are detected).
    	if (environment.isInitialized()){
        	
        	// Initialise VR AppState with the VR environment.
            VRAppState vrAppState = new VRAppState(settings, environment);
        	
        	// Create the sample application with the VRAppState attached.
            // There is no constraint on the Application type.
            SimpleApplication test = new VRAppStateSample(vrAppState);
            test.setShowSettings(false);

            // Sharing settings between app state and application is recommended.
            test.setSettings(settings);   
            
            // Starting the application.
            test.start();
    	} else {
    		logger.severe("Cannot start VR sample application as VR system is not initialized (see log for details)");
    	}
    }
}
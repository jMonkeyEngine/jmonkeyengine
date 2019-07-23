package jme3test.awt;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JFrame;

import com.jme3.app.Application;
import com.jme3.app.LegacyApplication;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AWTComponentAppState;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import com.jme3.system.awt.AWTContext;

/**
 * An example class that describe the use of an AWT component as render support.
 * @author Julien Seinturier
 *
 */
public class AWTComponentAppStateSample {



  /**
   * The root node of the scene to display.
   */
  private Node sceneRootNode = null;

  /**
   * The camera used for rendering.
   */
  private Camera camera      = null;

  /**
   * The camera target. This node is attached to the camera controler.
   */
  private Node cameraTarget = null;

  /**
   * Initialize the JMonkey Application that will render the scenes.
   * @return the initialized JMonkey Application that will render the scenes.
   */
  
  protected SimpleApplication createJMEApplication(AppSettings settings){

    SimpleApplication application = new SimpleApplication(){

      @Override
      public void initialize() {
        System.out.println("Initialising JMonkey Application...");
        super.initialize();
        System.out.println("Initialising JMonkey Application [DONE]");
      }

      @Override
      public void restart(){
        try {
          System.out.println("Restarting application");
          super.restart();
          System.out.println("Restart done");
        } catch (Exception e) {
          System.err.println("Cannot restart JMonkey underlying application: "+e.getMessage());
          e.printStackTrace(System.err);
        }
      }  

      @Override
      public void start(){
        System.out.println("Starting application... ");
        super.start();
        System.out.print("Starting application [DONE]");
      }

      @Override
      public void startCanvas(){
        System.out.println("Starting application canvas");
        super.startCanvas();
      }

      @Override
      public void startCanvas(boolean waitFor){
        System.out.println("Starting application canvas and wait: "+waitFor);
        super.startCanvas(waitFor);      
      }

      @Override
      public void simpleInitApp() {

        System.out.println("Init application...");

        // Preparing the scene to be rendered.
        sceneRootNode = getRootNode();

        Box b = new Box(1, 1, 1);
        
        Geometry greenBox = new Geometry("GreenBox", b);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Green);
        greenBox.setMaterial(mat);
        rootNode.attachChild(greenBox);
        
        cameraTarget = new Node("CameraTarget");
        cameraTarget.setLocalTranslation(sceneRootNode.getLocalTranslation());
        sceneRootNode.attachChild(cameraTarget);

        // Configuring the camera and its controler
        camera = cam;
        camera.setFrustumPerspective(45.0f, ((float)camera.getWidth()) / ((float)camera.getHeight()), 0.001f, 10000.0f);
        camera.update();

        System.out.println("Init application [DONE]");
      }

    };

    application.setSettings(settings); 
    application.setShowSettings(false);

    return application;
  }

  /**
   * Initialize the link between the JMonkey components (application, display panel) and the GUI.
   * This method start the 3D rendering loop and link the graphical user components.
   */
  protected void initJMEApplicationLink(LegacyApplication application){

    if (application != null){
      System.out.println("JMonkey application created.");
    } else {
      System.err.println("Cannot create JMonkey application.");
      return;
    }

    while((application.getContext() == null)||(!application.getContext().isCreated())){
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        System.err.println("Error during initialization: "+e.getMessage());
        e.printStackTrace(System.err);
      }
    }

    if (application.getContext() != null){
      System.out.println("JMonkey Application context initialized.");
    } else {
      System.err.println("Cannot initialize JMonkey Application context.");
    }

    while((application.getViewPort() == null)||(!application.getViewPort().isEnabled())){
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        System.err.println("Error during initialization: "+e.getMessage());
        e.printStackTrace(System.err);
      }
    }


    if ((application.getViewPort() != null)&&(application.getViewPort().isEnabled())){

      ColorRGBA bg = new ColorRGBA(0.4313f, 0.4313f, 0.4313f, 1.0f);

      application.getViewPort().setBackgroundColor(bg);
      System.out.println("JMonkey Application viewport initialized.");
    } else {
      System.err.println("Cannot initialize JMonkey Application viewport.");
    }

    while(application.getAssetManager() == null){
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        System.err.println("Error during initialization: "+e.getMessage());
        e.printStackTrace(System.err);
      }
    }

    while(application.getStateManager() == null){
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        System.err.println("Error during initialization: "+e.getMessage());
        e.printStackTrace(System.err);
      }
    }

    if (application.getStateManager() != null){
      System.out.println("State manager initialized.");
    } else {
      System.err.println("Cannot initialize state manager.");
    }

    while(application.getCamera() == null){
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        System.err.println("Error during initialization: "+e.getMessage());
        e.printStackTrace(System.err);
      }
    }
  }

  /**
   * An example that shows how to render JME content within an AWT component. 
   * The use of this panel requires:<br>
   * <ul>
   * <li><b>1.</b> The initialization of the JMonkey related object required by the system (application).
   * <li><b>2.</b> The initialization of the AWT component to use as rendering target. 
   * <li><b>2.</b> The initialization of the link between JME underlying components and AWT component. 
   * </ul>
   */
  public AWTComponentAppStateSample(){

	// 1. Set the underlying renderer
	String renderer = System.getProperty("jme3.awt.renderer");
	if (renderer == null) {
	  System.setProperty("jme3.awt.renderer", AppSettings.LWJGL_OPENGL45);
	}
	  
    // 2. Create settings that are compatible with AWT rendering
    AppSettings settings = new AppSettings(true);
    settings.setFullscreen(false);
    settings.setCustomRenderer(AWTContext.class);

    // 3. Initialize the underlying JMonkey related objects
    System.out.print("Initializing JME application... ");
    final Application application = createJMEApplication(settings);
    System.out.println(" [DONE]");

    // 4. Create the component that will support the rendering.
    //    Any subclass of Component can be used.
    Canvas component = new Canvas();
    component.setSize(800,  600);

    // 5. Create the app state dedicated to AWT component rendering
    AWTComponentAppState appState = new AWTComponentAppState(component);

    // 6. Attach the app state to the application
    application.getStateManager().attach(appState);

    // 6. Start the application
    Thread thread = new Thread() {
      @Override
      public void run() {
        try {
          application.start();
        } catch (Exception e) {
          System.err.println(e.getMessage());
          e.printStackTrace(System.err);
        }
      }
    };

    thread.start();

    JFrame frame = new JFrame();
    frame.setTitle("JMonkey AWT Component rendering Example");
    frame.setSize(new Dimension(800, 600));
    frame.setPreferredSize(new Dimension(800, 600));

    frame.getContentPane().setLayout(new BorderLayout());
    frame.getContentPane().add(component, BorderLayout.CENTER);

    frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    frame.addWindowListener(new WindowListener(){

      @Override
      public void windowOpened(WindowEvent e) {}

      @Override
      public void windowClosing(WindowEvent e) {
        System.out.print("Stopping JME Application");
        application.stop(true);
        System.out.println(" [OK]");
        System.exit(0);
      }

      @Override
      public void windowClosed(WindowEvent e) {}

      @Override
      public void windowIconified(WindowEvent e) {}

      @Override
      public void windowDeiconified(WindowEvent e) {}

      @Override
      public void windowActivated(WindowEvent e) {}

      @Override
      public void windowDeactivated(WindowEvent e) {}
    });


    frame.setVisible(true);



  }

  /**
   * The main entry point.
   * @param args the command line arguments.
   */
  public static void main(String[] args){

    // Instanciate the new sample.
    new AWTComponentAppStateSample();

  }
}

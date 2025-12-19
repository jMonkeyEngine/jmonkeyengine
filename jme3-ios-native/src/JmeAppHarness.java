import com.jme3.system.ios.IosHarness;
import com.jme3.input.ios.IosInputHandler;
import com.jme3.math.Vector2f;
import com.jme3.renderer.opengl.GLRenderer;
import com.jme3.system.JmeContext;
import com.jme3.system.AppSettings;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * You can extend this class to perform iOS-only operations from java,
 * native methods can reside either in .c/.m files in this directory
 * or in the XCode project itself.
 * @author normenhansen
 */
public class JmeAppHarness extends IosHarness{

    private static final Logger logger = Logger.getLogger(JmeAppHarness.class.getName());
	protected Renderer renderer;
	protected IosInputHandler input;
	protected boolean autoFlush = true;
	protected Vector2f resizePending = null;


    /**
     * An instance of this object is created when your application
     * has started on the iOS side.
     * You can e.g. attach special AppStates or do similar things here. You can
     * access classes from this source directory as well as your main projects
     * sources and classpath.
     */
    public JmeAppHarness(long id) {
        super(id);
        app = new mygame.Main();
        AppSettings settings = new AppSettings(true);
        this.app.setSettings(settings);
        app.start();
        logger.log(Level.FINE, "JmeAppHarness constructor");
        app.gainFocus();
    }

    @Override
    public void appPaused() {
        logger.log(Level.FINE, "JmeAppHarness appPaused");
    }

    @Override
    public void appReactivated() {
        logger.log(Level.FINE, "JmeAppHarness appReactivated");
    }

    @Override
    public void appClosed() {
        logger.log(Level.FINE, "JmeAppHarness appClosed");
        app.stop();
    }

    @Override
    public void appUpdate() {
        logger.log(Level.FINE, "JmeAppHarness appUpdate");
       //app.update();
    }

    @Override
    public void appDraw() {
        logger.log(Level.FINE, "JmeAppHarness appDraw");
        if (renderer == null) {
            JmeContext iosContext = app.getContext();
            renderer = iosContext.getRenderer();
            renderer.initialize();
            input = (IosInputHandler)iosContext.getTouchInput();
            input.initialize();
        } else {
            if(resizePending != null) {
                appReshape((int)resizePending.x, (int)resizePending.y);
                resizePending = null;
            }
            app.update();
    	    if (autoFlush) {
                renderer.postFrame();
            }
        }
    }
    
    @Override
    public void appReshape(int width, int height) {
        logger.log(Level.FINE, "JmeAppHarness reshape");
        AppSettings settings = app.getContext().getSettings();
        settings.setResolution(width, height);
        if (renderer != null) {
            app.reshape(width, height);
            resizePending = null;
        } else {
            resizePending = new Vector2f(width, height);
        }

        if (input != null) {
            input.loadSettings(settings);
        }
    }
    
    public void injectTouchBegin(int pointerId, long time, float x, float y) {
    	if (input != null) {
        	logger.log(Level.FINE, "JmeAppHarness injectTouchBegin");
    		input.injectTouchDown(pointerId, time, x, y);
    	}
    }
    
    public void injectTouchMove(int pointerId, long time, float x, float y) {
    	if (input != null) {
        	logger.log(Level.FINE, "JmeAppHarness injectTouchMove");
    		input.injectTouchMove(pointerId, time, x, y);
    	}
    }
    
    public void injectTouchEnd(int pointerId, long time, float x, float y) {
    	if (input != null) {
        	logger.log(Level.FINE, "JmeAppHarness injectTouchEnd");
    		input.injectTouchUp(pointerId, time, x, y);
    	}
    }
    
    /**
     * Example of a native method calling iOS code.
     * See the native code in IosHarness.m
     * @param text The message to display
     */
    public native void showDialog(String text);

}

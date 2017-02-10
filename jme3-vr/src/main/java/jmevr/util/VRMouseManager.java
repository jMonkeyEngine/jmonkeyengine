/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jmevr.util;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.lwjgl.glfw.GLFW;

import com.jme3.app.Application;
import com.jme3.app.VRAppState;
import com.jme3.app.VRApplication;
import com.jme3.app.state.AppState;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.lwjgl.GlfwMouseInputVR;
import com.jme3.input.vr.VRInputType;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.Vector2f;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;
import com.jme3.system.lwjgl.LwjglWindow;
import com.jme3.system.lwjgl.LwjglWindowVR;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;

/**
 *
 * @author Phr00t
 */
public class VRMouseManager {
 
	private static final Logger logger = Logger.getLogger(VRMouseManager.class.getName());
	
	private Application application = null;
	private VRAppState app = null;
	
	private final int AVERAGE_AMNT = 4;
    private int avgCounter;
    
    private Picture mouseImage;
    private int recentCenterCount = 0;
    private final Vector2f cursorPos = new Vector2f();
    private float ySize, sensitivity = 8f, acceleration = 2f;
    private final float[] lastXmv = new float[AVERAGE_AMNT], lastYmv = new float[AVERAGE_AMNT];
    private boolean thumbstickMode;
    private float moveScale = 1f;
    
    private float avg(float[] arr) {
        float amt = 0f;
        for(float f : arr) amt += f;
        return amt / arr.length;
    }
    
    public VRMouseManager(){
    }
    
    /**
     * Attach the mouse manager to an app state and an Application. 
     * The application has to be the one that the app state is attached.
     * This method should be called from the {@link AppState#initialize(com.jme3.app.state.AppStateManager, Application) initialize} 
     * method of the {@link AppState} instance.
     * @param app the VR app state that this manager is attached to.
     * @param application the application to whitch the app state is attcached.
     */
    public void attach(VRAppState app, Application application){
    	this.app = app;
    	this.application = application;
    }
    
    protected void init() {
    	
    	logger.config("Initializing VR mouse manager.");
    	
        // load default mouseimage
        mouseImage = new Picture("mouse");
        setImage("Common/Util/mouse.png");
        // hide default cursor by making it invisible        
        MouseInput mi = application.getContext().getMouseInput();
        if( mi instanceof GlfwMouseInputVR ){       	
        	((GlfwMouseInputVR)mi).hideActiveCursor();
        }
        centerMouse();
        
        logger.config("Initialized VR mouse manager [SUCCESS]");
    }
    
    public void setThumbstickMode(boolean set) {
        thumbstickMode = set;
    }
    
    public boolean isThumbstickMode() {
        return thumbstickMode;
    }
    
    public void setSpeed(float sensitivity, float acceleration) {
        this.sensitivity = sensitivity;
        this.acceleration = acceleration;
    }
    
    public float getSpeedSensitivity() {
        return sensitivity;
    }
    
    public float getSpeedAcceleration() {
        return acceleration;
    }
    
    public void setMouseMoveScale(float set) {
        moveScale = set;
    }
    
    public void setImage(String texture) {
        if( app.isInVR() == false ){
        	Texture tex = application.getAssetManager().loadTexture(texture);
            mouseImage.setTexture(application.getAssetManager(), (Texture2D)tex, true);
            ySize = tex.getImage().getHeight();
            mouseImage.setHeight(ySize);
            mouseImage.setWidth(tex.getImage().getWidth());
            mouseImage.getMaterial().getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
            mouseImage.getMaterial().getAdditionalRenderState().setDepthWrite(false);
        } else {
        	Texture tex = application.getAssetManager().loadTexture(texture);
            mouseImage.setTexture(application.getAssetManager(), (Texture2D)tex, true);
            ySize = tex.getImage().getHeight();
            mouseImage.setHeight(ySize);
            mouseImage.setWidth(tex.getImage().getWidth());
            mouseImage.getMaterial().getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
            mouseImage.getMaterial().getAdditionalRenderState().setDepthWrite(false);
        }
        
    }
    
    public void updateAnalogAsMouse(int inputIndex, AnalogListener mouseListener, String mouseXName, String mouseYName, float tpf) {
        // got a tracked controller to use as the "mouse"
        if( app.isInVR() == false || 
        	app.getVRinput() == null ||
        	app.getVRinput().isInputDeviceTracking(inputIndex) == false ) return;
        Vector2f tpDelta;
        if( thumbstickMode ) {
            tpDelta = app.getVRinput().getAxis(inputIndex, VRInputType.ViveTrackpadAxis);
        } else {
            tpDelta = app.getVRinput().getAxisDeltaSinceLastCall(inputIndex, VRInputType.ViveTrackpadAxis);            
        }
        float Xamount = (float)Math.pow(Math.abs(tpDelta.x) * sensitivity, acceleration);
        float Yamount = (float)Math.pow(Math.abs(tpDelta.y) * sensitivity, acceleration);
        if( tpDelta.x < 0f ) Xamount = -Xamount;
        if( tpDelta.y < 0f ) Yamount = -Yamount;
        Xamount *= moveScale; Yamount *= moveScale;
        if( mouseListener != null ) {
            if( tpDelta.x != 0f && mouseXName != null ) mouseListener.onAnalog(mouseXName, Xamount * 0.2f, tpf);
            if( tpDelta.y != 0f && mouseYName != null ) mouseListener.onAnalog(mouseYName, Yamount * 0.2f, tpf);            
        }
        if( application.getInputManager().isCursorVisible() ) {
            int index = (avgCounter+1) % AVERAGE_AMNT;
            lastXmv[index] = Xamount * 133f;
            lastYmv[index] = Yamount * 133f;
            cursorPos.x -= avg(lastXmv);
            cursorPos.y -= avg(lastYmv);
            Vector2f maxsize = app.getVRGUIManager().getCanvasSize();
            if( cursorPos.x > maxsize.x ) cursorPos.x = maxsize.x;
            if( cursorPos.x < 0f ) cursorPos.x = 0f;
            if( cursorPos.y > maxsize.y ) cursorPos.y = maxsize.y;
            if( cursorPos.y < 0f ) cursorPos.y = 0f;
        }
    }
    
    public Vector2f getCursorPosition() {
        if( app.isInVR() ) {
            return cursorPos;
        }
        return application.getInputManager().getCursorPosition();
    }
    
    public void centerMouse() {
        // set mouse in center of the screen if newly added
        Vector2f size = app.getVRGUIManager().getCanvasSize();
        MouseInput mi = application.getContext().getMouseInput();
        AppSettings as = application.getContext().getSettings();
        if( mi instanceof GlfwMouseInputVR ) ((GlfwMouseInputVR)mi).setCursorPosition((int)(as.getWidth() / 2f), (int)(as.getHeight() / 2f));
        if( app.isInVR() ) {
            cursorPos.x = size.x / 2f;
            cursorPos.y = size.y / 2f;
            recentCenterCount = 2;
        }
    }
    
    protected void update(float tpf) {
        // if we are showing the cursor, add our picture as it

        if( application.getInputManager().isCursorVisible() ) {
            if( mouseImage.getParent() == null ) {
            	
            	application.getGuiViewPort().attachScene(mouseImage);         
                centerMouse();
                // the "real" mouse pointer should stay hidden
                if (application.getContext() instanceof LwjglWindow){
                	GLFW.glfwSetInputMode(((LwjglWindow)application.getContext()).getWindowHandle(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
                }
            }
            // handle mouse movements, which may be in addition to (or exclusive from) tracked movement
            MouseInput mi = application.getContext().getMouseInput();
            if( mi instanceof GlfwMouseInputVR ) {
                if( recentCenterCount <= 0 ) {
                    //Vector2f winratio = VRGuiManager.getCanvasToWindowRatio();
                    cursorPos.x += ((GlfwMouseInputVR)mi).getLastDeltaX();// * winratio.x;
                    cursorPos.y += ((GlfwMouseInputVR)mi).getLastDeltaY();// * winratio.y;
                    if( cursorPos.x < 0f ) cursorPos.x = 0f;
                    if( cursorPos.y < 0f ) cursorPos.y = 0f;
                    if( cursorPos.x > app.getVRGUIManager().getCanvasSize().x ) cursorPos.x = app.getVRGUIManager().getCanvasSize().x;
                    if( cursorPos.y > app.getVRGUIManager().getCanvasSize().y ) cursorPos.y = app.getVRGUIManager().getCanvasSize().y;
                } else recentCenterCount--;
                ((GlfwMouseInputVR)mi).clearDeltas();
            }
            // ok, update the cursor graphic position
            Vector2f currentPos = getCursorPosition();
            mouseImage.setLocalTranslation(currentPos.x, currentPos.y - ySize, app.getVRGUIManager().getGuiDistance() + 1f);
		    
            mouseImage.updateGeometricState();
            
        } else if( mouseImage.getParent() != null ) {
        	Node n = mouseImage.getParent();
            mouseImage.removeFromParent();
            
            if (n != null){
              n.updateGeometricState();
            }
        }
    }    
}

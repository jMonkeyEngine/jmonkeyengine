/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.util;

import java.util.logging.Logger;

import org.lwjgl.glfw.GLFW;

import com.jme3.app.VREnvironment;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.lwjgl.GlfwMouseInputVR;
import com.jme3.input.vr.VRInputType;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.Vector2f;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;
import com.jme3.system.lwjgl.LwjglWindow;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;

/**
 * A class dedicated to the handling of the mouse within VR environment.
 * @author Julien Seinturier - (c) 2016 - JOrigin project - <a href="http://www.jorigin.org">http:/www.jorigin.org</a>
 *
 */
public class VRMouseManager {
 
	private static final Logger logger = Logger.getLogger(VRMouseManager.class.getName());
	

	private VREnvironment environment = null;
	
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
    
    /**
     * Create a new VR mouse manager within the given {@link VREnvironment VR environment}.
     * @param environment the VR environment of the mouse manager.
     */
    public VRMouseManager(VREnvironment environment){
    	this.environment = environment;
    }
    
    /**
     * Initialize the VR mouse manager.
     */
    protected void initialize() {
    	
    	logger.config("Initializing VR mouse manager.");
    	
        // load default mouseimage
        mouseImage = new Picture("mouse");
        setImage("Common/Util/mouse.png");
        // hide default cursor by making it invisible
        
        MouseInput mi = environment.getApplication().getContext().getMouseInput();
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
    
    /**
     * Set the speed of the mouse.
     * @param sensitivity the sensitivity of the mouse.
     * @param acceleration the acceleration of the mouse.
     * @see #getSpeedAcceleration()
     * @see #getSpeedSensitivity()
     */
    public void setSpeed(float sensitivity, float acceleration) {
        this.sensitivity = sensitivity;
        this.acceleration = acceleration;
    }
    
    /**
     * Get the sensitivity of the mouse.
     * @return the sensitivity of the mouse.
     * @see #setSpeed(float, float)
     */
    public float getSpeedSensitivity() {
        return sensitivity;
    }
    
    /**
     * Get the acceleration of the mouse.
     * @return the acceleration of the mouse.
     * @see #setSpeed(float, float)
     */
    public float getSpeedAcceleration() {
        return acceleration;
    }
    
    /**
     * Set the mouse move scale.
     * @param set the mouse move scale.
     */
    public void setMouseMoveScale(float set) {
        moveScale = set;
    }
    
    /**
     * Set the image to use as mouse cursor. The given string describe an asset that the underlying application asset manager has to load.
     * @param texture the image to use as mouse cursor.
     */
    public void setImage(String texture) {
    	
    	if (environment != null){
    		
    		if (environment.getApplication() != null){
    			if( environment.isInVR() == false ){
    	        	Texture tex = environment.getApplication().getAssetManager().loadTexture(texture);
    	            mouseImage.setTexture(environment.getApplication().getAssetManager(), (Texture2D)tex, true);
    	            ySize = tex.getImage().getHeight();
    	            mouseImage.setHeight(ySize);
    	            mouseImage.setWidth(tex.getImage().getWidth());
    	            mouseImage.getMaterial().getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
    	            mouseImage.getMaterial().getAdditionalRenderState().setDepthWrite(false);
    	        } else {
    	        	Texture tex = environment.getApplication().getAssetManager().loadTexture(texture);
    	            mouseImage.setTexture(environment.getApplication().getAssetManager(), (Texture2D)tex, true);
    	            ySize = tex.getImage().getHeight();
    	            mouseImage.setHeight(ySize);
    	            mouseImage.setWidth(tex.getImage().getWidth());
    	            mouseImage.getMaterial().getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
    	            mouseImage.getMaterial().getAdditionalRenderState().setDepthWrite(false);
    	        }
    		} else {
    			throw new IllegalStateException("This VR environment is not attached to any application.");
    		}
    		
    	} else {
          throw new IllegalStateException("This VR view manager is not attached to any VR environment.");
    	} 
    }
    
    /**
     * Update analog controller as it was a mouse controller.
     * @param inputIndex the index of the controller attached to the VR system.
     * @param mouseListener the JMonkey mouse listener to trigger.
     * @param mouseXName the mouseX identifier.
     * @param mouseYName the mouseY identifier
     * @param tpf the time per frame.
     */
    public void updateAnalogAsMouse(int inputIndex, AnalogListener mouseListener, String mouseXName, String mouseYName, float tpf) {
        
    	if (environment != null){
    		if (environment.getApplication() != null){
    			// got a tracked controller to use as the "mouse"
    	        if( environment.isInVR() == false || 
    	        	environment.getVRinput() == null ||
    	        	environment.getVRinput().isInputDeviceTracking(inputIndex) == false ){
    	        	return;
    	        }
    	        
    	        Vector2f tpDelta;
    	        if( thumbstickMode ) {
    	            tpDelta = environment.getVRinput().getAxis(inputIndex, VRInputType.ViveTrackpadAxis);
    	        } else {
    	            tpDelta = environment.getVRinput().getAxisDeltaSinceLastCall(inputIndex, VRInputType.ViveTrackpadAxis);            
    	        }
    	        
    	        float Xamount = (float)Math.pow(Math.abs(tpDelta.x) * sensitivity, acceleration);
    	        float Yamount = (float)Math.pow(Math.abs(tpDelta.y) * sensitivity, acceleration);
    	        
    	        if( tpDelta.x < 0f ){
    	        	Xamount = -Xamount;
    	        }
    	        
    	        if( tpDelta.y < 0f ){
    	        	Yamount = -Yamount;
    	        }
    	        
    	        Xamount *= moveScale; Yamount *= moveScale;
    	        if( mouseListener != null ) {
    	            if( tpDelta.x != 0f && mouseXName != null ) mouseListener.onAnalog(mouseXName, Xamount * 0.2f, tpf);
    	            if( tpDelta.y != 0f && mouseYName != null ) mouseListener.onAnalog(mouseYName, Yamount * 0.2f, tpf);            
    	        }
    	        
    	        if( environment.getApplication().getInputManager().isCursorVisible() ) {
    	            int index = (avgCounter+1) % AVERAGE_AMNT;
    	            lastXmv[index] = Xamount * 133f;
    	            lastYmv[index] = Yamount * 133f;
    	            cursorPos.x -= avg(lastXmv);
    	            cursorPos.y -= avg(lastYmv);
    	            Vector2f maxsize = environment.getVRGUIManager().getCanvasSize();
    	            
    	            if( cursorPos.x > maxsize.x ){
    	            	cursorPos.x = maxsize.x;
    	            }
    	            
    	            if( cursorPos.x < 0f ){
    	            	cursorPos.x = 0f;
    	            }
    	            
    	            if( cursorPos.y > maxsize.y ){
    	            	cursorPos.y = maxsize.y;
    	            }
    	            
    	            if( cursorPos.y < 0f ){
    	            	cursorPos.y = 0f;
    	            }
    	        }
    		} else {
    			throw new IllegalStateException("This VR environment is not attached to any application.");
    		}
    	} else {
            throw new IllegalStateException("This VR view manager is not attached to any VR environment.");
      	} 
    }
    
    /**
     * Get the actual cursor position.
     * @return the actual cursor position.
     */
    public Vector2f getCursorPosition() {
    	
    	if (environment != null){
    		if (environment.getApplication() != null){
    			 if( environment.isInVR() ) {
    		            return cursorPos;
    		        }
    		        
    		        return environment.getApplication().getInputManager().getCursorPosition();
    		} else {
    			throw new IllegalStateException("This VR environment is not attached to any application.");
    		}
    	} else {
            throw new IllegalStateException("This VR view manager is not attached to any VR environment.");
      	} 
    }
    
    /**
     * Center the mouse on the display.
     */
    public void centerMouse() {
    	
    	if (environment != null){
    		if (environment.getApplication() != null){
    	        // set mouse in center of the screen if newly added
    	        Vector2f size = environment.getVRGUIManager().getCanvasSize();
    	        MouseInput mi = environment.getApplication().getContext().getMouseInput();
    	        AppSettings as = environment.getApplication().getContext().getSettings();
    	        if( mi instanceof GlfwMouseInputVR ) ((GlfwMouseInputVR)mi).setCursorPosition((int)(as.getWidth() / 2f), (int)(as.getHeight() / 2f));
    	        if( environment.isInVR() ) {
    	            cursorPos.x = size.x / 2f;
    	            cursorPos.y = size.y / 2f;
    	            recentCenterCount = 2;
    	        }
    		} else {
    			throw new IllegalStateException("This VR environment is not attached to any application.");
    		}
    	} else {
            throw new IllegalStateException("This VR view manager is not attached to any VR environment.");
      	} 
    	
    }
    
    /**
     * Update the mouse manager. This method should not be called manually. 
     * The standard behavior for this method is to be called from the {@link VRViewManager#update(float) update method} of the attached {@link VRViewManager VR view manager}.
     * @param tpf the time per frame.
     */
    protected void update(float tpf) {
        // if we are showing the cursor, add our picture as it

        if( environment.getApplication().getInputManager().isCursorVisible() ) {
            if( mouseImage.getParent() == null ) {
            	
            	environment.getApplication().getGuiViewPort().attachScene(mouseImage);         
                centerMouse();
                // the "real" mouse pointer should stay hidden
                if (environment.getApplication().getContext() instanceof LwjglWindow){
                	GLFW.glfwSetInputMode(((LwjglWindow)environment.getApplication().getContext()).getWindowHandle(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
                }
            }
            // handle mouse movements, which may be in addition to (or exclusive from) tracked movement
            MouseInput mi = environment.getApplication().getContext().getMouseInput();
            if( mi instanceof GlfwMouseInputVR ) {
                if( recentCenterCount <= 0 ) {
                    //Vector2f winratio = VRGuiManager.getCanvasToWindowRatio();
                    cursorPos.x += ((GlfwMouseInputVR)mi).getLastDeltaX();// * winratio.x;
                    cursorPos.y += ((GlfwMouseInputVR)mi).getLastDeltaY();// * winratio.y;
                    if( cursorPos.x < 0f ) cursorPos.x = 0f;
                    if( cursorPos.y < 0f ) cursorPos.y = 0f;
                    if( cursorPos.x > environment.getVRGUIManager().getCanvasSize().x ) cursorPos.x = environment.getVRGUIManager().getCanvasSize().x;
                    if( cursorPos.y > environment.getVRGUIManager().getCanvasSize().y ) cursorPos.y = environment.getVRGUIManager().getCanvasSize().y;
                } else recentCenterCount--;
                ((GlfwMouseInputVR)mi).clearDeltas();
            }
            // ok, update the cursor graphic position
            Vector2f currentPos = getCursorPosition();
            mouseImage.setLocalTranslation(currentPos.x, currentPos.y - ySize, environment.getVRGUIManager().getGuiDistance() + 1f);
		    
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

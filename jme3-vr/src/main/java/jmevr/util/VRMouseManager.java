/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jmevr.util;

import com.jme3.app.VRApplication;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.lwjgl.GlfwMouseInputVR;
import com.jme3.input.vr.VRInputType;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.Vector2f;
import com.jme3.system.AppSettings;
import com.jme3.system.lwjgl.LwjglWindowVR;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;

/**
 *
 * @author Phr00t
 */
public class VRMouseManager {
 
    private static final int AVERAGE_AMNT = 4;
    private static int avgCounter;
    
    private static Picture mouseImage;
    private static int recentCenterCount = 0;
    private static final Vector2f cursorPos = new Vector2f();
    private static float ySize, sensitivity = 8f, acceleration = 2f;
    private static final float[] lastXmv = new float[AVERAGE_AMNT], lastYmv = new float[AVERAGE_AMNT];
    private static boolean thumbstickMode;
    private static float moveScale = 1f;
    
    private static float avg(float[] arr) {
        float amt = 0f;
        for(float f : arr) amt += f;
        return amt / arr.length;
    }
    
    protected static void init() {
        // load default mouseimage
        mouseImage = new Picture("mouse");
        setImage("jmevr/util/mouse.png");
        // hide default cursor by making it invisible        
        MouseInput mi = VRApplication.getMainVRApp().getContext().getMouseInput();
        if( mi instanceof GlfwMouseInputVR ){       	
        	((GlfwMouseInputVR)mi).hideActiveCursor();
        }
        centerMouse();
    }
    
    public static void setThumbstickMode(boolean set) {
        thumbstickMode = set;
    }
    
    public static boolean isThumbstickMode() {
        return thumbstickMode;
    }
    
    public static void setSpeed(float sensitivity, float acceleration) {
        VRMouseManager.sensitivity = sensitivity;
        VRMouseManager.acceleration = acceleration;
    }
    
    public static float getSpeedSensitivity() {
        return sensitivity;
    }
    
    public static float getSpeedAcceleration() {
        return acceleration;
    }
    
    public static void setMouseMoveScale(float set) {
        moveScale = set;
    }
    
    public static void setImage(String texture) {
        if( VRApplication.isInVR() == false ) return;
        Texture tex = VRApplication.getMainVRApp().getAssetManager().loadTexture(texture);
        mouseImage.setTexture(VRApplication.getMainVRApp().getAssetManager(), (Texture2D)tex, true);
        ySize = tex.getImage().getHeight();
        mouseImage.setHeight(ySize);
        mouseImage.setWidth(tex.getImage().getWidth());
        mouseImage.getMaterial().getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        mouseImage.getMaterial().getAdditionalRenderState().setDepthWrite(false);
    }
    
    public static void updateAnalogAsMouse(int inputIndex, AnalogListener mouseListener, String mouseXName, String mouseYName, float tpf) {
        // got a tracked controller to use as the "mouse"
        if( VRApplication.isInVR() == false || 
            VRApplication.getVRinput() == null ||
            VRApplication.getVRinput().isInputDeviceTracking(inputIndex) == false ) return;
        Vector2f tpDelta;
        if( thumbstickMode ) {
            tpDelta = VRApplication.getVRinput().getAxis(inputIndex, VRInputType.ViveTrackpadAxis);
        } else {
            tpDelta = VRApplication.getVRinput().getAxisDeltaSinceLastCall(inputIndex, VRInputType.ViveTrackpadAxis);            
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
        if( VRApplication.getMainVRApp().getInputManager().isCursorVisible() ) {
            int index = (avgCounter+1) % AVERAGE_AMNT;
            lastXmv[index] = Xamount * 133f;
            lastYmv[index] = Yamount * 133f;
            cursorPos.x -= avg(lastXmv);
            cursorPos.y -= avg(lastYmv);
            Vector2f maxsize = VRGuiManager.getCanvasSize();
            if( cursorPos.x > maxsize.x ) cursorPos.x = maxsize.x;
            if( cursorPos.x < 0f ) cursorPos.x = 0f;
            if( cursorPos.y > maxsize.y ) cursorPos.y = maxsize.y;
            if( cursorPos.y < 0f ) cursorPos.y = 0f;
        }
    }
    
    public static Vector2f getCursorPosition() {
        if( VRApplication.isInVR() ) {
            return cursorPos;
        }
        return VRApplication.getMainVRApp().getInputManager().getCursorPosition();
    }
    
    public static void centerMouse() {
        // set mouse in center of the screen if newly added
        Vector2f size = VRGuiManager.getCanvasSize();
        MouseInput mi = VRApplication.getMainVRApp().getContext().getMouseInput();
        AppSettings as = VRApplication.getMainVRApp().getContext().getSettings();
        if( mi instanceof GlfwMouseInputVR ) ((GlfwMouseInputVR)mi).setCursorPosition((int)(as.getWidth() / 2f), (int)(as.getHeight() / 2f));
        if( VRApplication.isInVR() ) {
            cursorPos.x = size.x / 2f;
            cursorPos.y = size.y / 2f;
            recentCenterCount = 2;
        }
    }
    
    protected static void update(float tpf) {
        // if we are showing the cursor, add our picture as it
        VRApplication vrapp = VRApplication.getMainVRApp();
        if( vrapp.getInputManager().isCursorVisible() ) {
            if( mouseImage.getParent() == null ) {
                VRApplication.getMainVRApp().getGuiNode().attachChild(mouseImage);                
                centerMouse();
                // the "real" mouse pointer should stay hidden
                org.lwjgl.glfw.GLFW.glfwSetInputMode(((LwjglWindowVR)VRApplication.getMainVRApp().getContext()).getWindowHandle(),
                                                      org.lwjgl.glfw.GLFW.GLFW_CURSOR, org.lwjgl.glfw.GLFW.GLFW_CURSOR_DISABLED);
            }
            // handle mouse movements, which may be in addition to (or exclusive from) tracked movement
            MouseInput mi = VRApplication.getMainVRApp().getContext().getMouseInput();
            if( mi instanceof GlfwMouseInputVR ) {
                if( recentCenterCount <= 0 ) {
                    //Vector2f winratio = VRGuiManager.getCanvasToWindowRatio();
                    cursorPos.x += ((GlfwMouseInputVR)mi).getLastDeltaX();// * winratio.x;
                    cursorPos.y += ((GlfwMouseInputVR)mi).getLastDeltaY();// * winratio.y;
                    if( cursorPos.x < 0f ) cursorPos.x = 0f;
                    if( cursorPos.y < 0f ) cursorPos.y = 0f;
                    if( cursorPos.x > VRGuiManager.getCanvasSize().x ) cursorPos.x = VRGuiManager.getCanvasSize().x;
                    if( cursorPos.y > VRGuiManager.getCanvasSize().y ) cursorPos.y = VRGuiManager.getCanvasSize().y;
                } else recentCenterCount--;
                ((GlfwMouseInputVR)mi).clearDeltas();
            }
            // ok, update the cursor graphic position
            Vector2f currentPos = getCursorPosition();
            mouseImage.setLocalTranslation(currentPos.x, currentPos.y - ySize, VRGuiManager.getGuiDistance() + 1f);
        } else if( mouseImage.getParent() != null ) {
            mouseImage.removeFromParent();
        }
    }    
}

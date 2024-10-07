package com.jme3.input.vr.oculus;

import com.jme3.app.VREnvironment;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.vr.AbstractVRMouseManager;
import com.jme3.input.vr.VRInputType;
import com.jme3.math.Vector2f;

/**
 * A class dedicated to the mouse handling within Oculus Rift based VR experience.
 * @author Julien Seinturier - COMEX SA - <a href="http://www.seinturier.fr">http://www.seinturier.fr</a>
 *
 */
public class OculusMouseManager extends AbstractVRMouseManager {
    private final int AVERAGE_AMNT = 4;

    private int avgCounter;

    private final float[] lastXmv = new float[AVERAGE_AMNT];

    private final float[] lastYmv = new float[AVERAGE_AMNT];

    /**
     * Create a new VR mouse manager within the given {@link VREnvironment VR environment}.
     * @param environment the VR environment of the mouse manager.
     */
    public OculusMouseManager(VREnvironment environment){
        super(environment);
    }

    @Override
    public void updateAnalogAsMouse(int inputIndex, AnalogListener mouseListener, String mouseXName, String mouseYName, float tpf) {
        if (getVREnvironment() != null){
            if (getVREnvironment().getApplication() != null){
                // got a tracked controller to use as the "mouse"
                if( getVREnvironment().isInVR() == false ||
                    getVREnvironment().getVRinput() == null ||
                    getVREnvironment().getVRinput().isInputDeviceTracking(inputIndex) == false ){
                    return;
                }

                Vector2f tpDelta;
                // TODO option to use Touch joysticks
                if( isThumbstickMode() ) {
                    tpDelta = getVREnvironment().getVRinput().getAxis(inputIndex, VRInputType.OculusThumbstickAxis);
                } else {
                    tpDelta = getVREnvironment().getVRinput().getAxisDeltaSinceLastCall(inputIndex, VRInputType.OculusThumbstickAxis);
                }

                float Xamount = (float)Math.pow(Math.abs(tpDelta.x) * getSpeedSensitivity(), getSpeedAcceleration());
                float Yamount = (float)Math.pow(Math.abs(tpDelta.y) * getSpeedSensitivity(), getSpeedAcceleration());

                if( tpDelta.x < 0f ){
                    Xamount = -Xamount;
                }

                if( tpDelta.y < 0f ){
                    Yamount = -Yamount;
                }

                Xamount *= getMouseMoveScale();
                Yamount *= getMouseMoveScale();

                if( mouseListener != null ) {
                    if( tpDelta.x != 0f && mouseXName != null ) mouseListener.onAnalog(mouseXName, Xamount * 0.2f, tpf);
                    if( tpDelta.y != 0f && mouseYName != null ) mouseListener.onAnalog(mouseYName, Yamount * 0.2f, tpf);
                }

                if( getVREnvironment().getApplication().getInputManager().isCursorVisible() ) {
                    int index = (avgCounter+1) % AVERAGE_AMNT;
                    lastXmv[index] = Xamount * 133f;
                    lastYmv[index] = Yamount * 133f;
                    cursorPos.x -= avg(lastXmv);
                    cursorPos.y -= avg(lastYmv);
                    Vector2f maxsize = getVREnvironment().getVRGUIManager().getCanvasSize();

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

    private float avg(float[] arr) {
        float amt = 0f;
        for(float f : arr) amt += f;
        return amt / arr.length;
    }
}

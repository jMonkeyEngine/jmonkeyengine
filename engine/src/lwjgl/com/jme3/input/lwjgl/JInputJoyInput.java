package com.jme3.input.lwjgl;

import com.jme3.input.InputManager;
import com.jme3.input.JoyInput;
import com.jme3.input.Joystick;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.util.IntMap;
import java.util.HashMap;
import net.java.games.input.Component.Identifier;
import net.java.games.input.Component.Identifier.Axis;
import net.java.games.input.Component.Identifier.Button;
import net.java.games.input.Component.POV;
import net.java.games.input.*;

public class JInputJoyInput implements JoyInput {

    private boolean inited = false;
    private Joystick[] joysticks;
    private RawInputListener listener;

    private HashMap<Button, Integer>[] buttonIdsToIndices;
    private HashMap<Axis, Integer>[] axisIdsToIndices;
    private HashMap<Controller, Integer> controllerToIndices;
    private IntMap<Controller> indicesToController;

    private int xAxis, yAxis;

    private void loadIdentifiers(int controllerIdx, Controller c){
        Component[] ces = c.getComponents();
        int numButtons = 0;
        int numAxes = 0;
        xAxis = -1;
        yAxis = -1;
        for (Component comp : ces){
            Identifier id = comp.getIdentifier();
            if (id instanceof Button){
                buttonIdsToIndices[controllerIdx].put((Button)id, numButtons);
                numButtons ++;
            }else if (id instanceof Axis){
                Axis axis = (Axis) id;
                if (axis == Axis.X){
                    xAxis = numAxes;
                }else if (axis == Axis.Y){
                    yAxis = numAxes;
                }

                axisIdsToIndices[controllerIdx].put((Axis)id, numAxes);
                numAxes ++;
            }
        }
    }

    public void setJoyRumble(int joyId, float amount){
        Controller c = indicesToController.get(joyId);
        if (c == null)
            throw new IllegalArgumentException();

        for (Rumbler r : c.getRumblers()){
            r.rumble(amount);
        }
    }

    public Joystick[] loadJoysticks(InputManager inputManager){
        ControllerEnvironment ce =
            ControllerEnvironment.getDefaultEnvironment();

        int joyIndex = 0;
        controllerToIndices = new HashMap<Controller, Integer>();
        indicesToController = new IntMap<Controller>();
        Controller[] cs = ce.getControllers();
        for (int i = 0; i < cs.length; i++){
            Controller c = cs[i];
            if (c.getType() == Controller.Type.KEYBOARD
             || c.getType() == Controller.Type.MOUSE)
                continue;

            controllerToIndices.put(c, joyIndex);
            indicesToController.put(joyIndex, c);
            joyIndex ++;
        }

        buttonIdsToIndices = new HashMap[joyIndex];
        axisIdsToIndices = new HashMap[joyIndex];
        joysticks = new Joystick[joyIndex];

        joyIndex = 0;

        for (int i = 0; i < cs.length; i++){
            Controller c = cs[i];
            if (c.getType() == Controller.Type.KEYBOARD
             || c.getType() == Controller.Type.MOUSE)
                continue;

            buttonIdsToIndices[joyIndex] = new HashMap<Button, Integer>();
            axisIdsToIndices[joyIndex] = new HashMap<Axis, Integer>();
            loadIdentifiers(joyIndex, c);
            Joystick joy = new Joystick(inputManager,
                                        this,
                                        joyIndex, c.getName(),
                                        buttonIdsToIndices[joyIndex].size(),
                                        axisIdsToIndices[joyIndex].size(),
                                        xAxis, yAxis);
            joysticks[joyIndex] = joy;
            joyIndex++;
        }

        return joysticks;
    }

    public void initialize() {
        inited = true;
    }

    public void update() {
        ControllerEnvironment ce =
            ControllerEnvironment.getDefaultEnvironment();

        Controller[] cs = ce.getControllers();
        Event e = new Event();
        for (int i = 0; i < cs.length; i++){
            Controller c = cs[i];
            if (c.getType() == Controller.Type.UNKNOWN
             || c.getType() == Controller.Type.KEYBOARD
             || c.getType() == Controller.Type.MOUSE)
                continue;

            if (!c.poll())
                continue;

            int joyId = controllerToIndices.get(c);
            EventQueue q = c.getEventQueue();
            while (q.getNextEvent(e)){
                Identifier id = e.getComponent().getIdentifier();
                if (id == Identifier.Axis.POV){
                    float x = 0, y = 0;
                    float v = e.getValue();
                    
                    if (v == POV.CENTER){
                        x = 0; y = 0;
                    }else if (v == POV.DOWN){
                        x = 0; y = -1f;
                    }else if (v == POV.DOWN_LEFT){
                        x = -1f; y = -1f;
                    }else if (v == POV.DOWN_RIGHT){
                        x = -1f; y = 1f;
                    }else if (v == POV.LEFT){
                        x = -1f; y = 0;
                    }else if (v == POV.RIGHT){
                        x = 1f; y = 0;
                    }else if (v == POV.UP){
                        x = 0; y = 1f;
                    }else if (v == POV.UP_LEFT){
                        x = -1f; y = 1f;
                    }else if (v == POV.UP_RIGHT){
                        x = 1f; y = 1f;
                    }

                    JoyAxisEvent evt1 = new JoyAxisEvent(joyId, JoyInput.AXIS_POV_X, x);
                    JoyAxisEvent evt2 = new JoyAxisEvent(joyId, JoyInput.AXIS_POV_Y, y);
                    listener.onJoyAxisEvent(evt1);
                    listener.onJoyAxisEvent(evt2);
                }else if (id instanceof Axis){
                    float value = e.getValue();   
                    Axis axis = (Axis) id;
                    JoyAxisEvent evt = new JoyAxisEvent(joyId, axisIdsToIndices[joyId].get(axis), value);
                    listener.onJoyAxisEvent(evt);
                }else if (id instanceof Button){
                    Button button = (Button) id;
                    JoyButtonEvent evt = new JoyButtonEvent(joyId, buttonIdsToIndices[joyId].get(button), e.getValue() == 1f);
                    listener.onJoyButtonEvent(evt);
                }
            }
        }
    }

    public void destroy() {
        inited = false;
    }

    public boolean isInitialized() {
        return inited;
    }

    public void setInputListener(RawInputListener listener) {
        this.listener = listener;
    }

    public long getInputTimeNanos() {
        return 0;
    }

}

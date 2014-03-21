package com.jme3.input.lwjgl;

import com.jme3.input.AbstractJoystick;
import com.jme3.input.DefaultJoystickAxis;
import com.jme3.input.DefaultJoystickButton;
import com.jme3.input.InputManager;
import com.jme3.input.JoyInput;
import com.jme3.input.Joystick;
import com.jme3.input.JoystickAxis;
import com.jme3.input.JoystickButton;
import com.jme3.input.JoystickCompatibilityMappings;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.java.games.input.*;
import net.java.games.input.Component.Identifier;
import net.java.games.input.Component.Identifier.Axis;
import net.java.games.input.Component.Identifier.Button;
import net.java.games.input.Component.POV;

public class JInputJoyInput implements JoyInput {

    private static final Logger logger = Logger.getLogger(InputManager.class.getName());

    private boolean inited = false;
    private JInputJoystick[] joysticks;
    private RawInputListener listener;

    private Map<Controller, JInputJoystick> joystickIndex = new HashMap<Controller, JInputJoystick>();
    
    public void setJoyRumble(int joyId, float amount){

        if( joyId >= joysticks.length )        
            throw new IllegalArgumentException();
            
        Controller c = joysticks[joyId].controller;
        for (Rumbler r : c.getRumblers()){
            r.rumble(amount);
        }
    }

    public Joystick[] loadJoysticks(InputManager inputManager){
        ControllerEnvironment ce =
            ControllerEnvironment.getDefaultEnvironment();

        Controller[] cs = ce.getControllers();
        
        List<Joystick> list = new ArrayList<Joystick>();
        for( Controller c : ce.getControllers() ) {
            if (c.getType() == Controller.Type.KEYBOARD
             || c.getType() == Controller.Type.MOUSE)
                continue;

            logger.log(Level.FINE, "Attempting to create joystick for: \"{0}\"", c);        
 
            // Try to create it like a joystick
            JInputJoystick stick = new JInputJoystick(inputManager, this, c, list.size(),
                                                      c.getName()); 
            for( Component comp : c.getComponents() ) {
                stick.addComponent(comp);                   
            }
 
            // If it has no axes then we'll assume it's not
            // a joystick
            if( stick.getAxisCount() == 0 ) {
                logger.log(Level.FINE, "Not a joystick: {0}", c);
                continue;
            }
 
            joystickIndex.put(c, stick);
            list.add(stick);                      
        }

        joysticks = list.toArray( new JInputJoystick[list.size()] );
        
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
            
            JInputJoystick stick = joystickIndex.get(c);
            if( stick == null )
                continue;
                
            if( !c.poll() )
                continue;
        
            int joyId = stick.getJoyId();
                    
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
                        x = 1f; y = -1f;
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

                    JoyAxisEvent evt1 = new JoyAxisEvent(stick.povX, x);
                    JoyAxisEvent evt2 = new JoyAxisEvent(stick.povY, y);
                    listener.onJoyAxisEvent(evt1);
                    listener.onJoyAxisEvent(evt2);
                }else if (id instanceof Axis){
                    float value = e.getValue();
                    
                    JoystickAxis axis = stick.axisIndex.get(e.getComponent());
                    JoyAxisEvent evt = new JoyAxisEvent(axis, value);
                    listener.onJoyAxisEvent(evt);
                }else if (id instanceof Button){
                    
                    JoystickButton button = stick.buttonIndex.get(e.getComponent());                    
                    JoyButtonEvent evt = new JoyButtonEvent(button, e.getValue() == 1f);
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

    protected class JInputJoystick extends AbstractJoystick {

        private JoystickAxis nullAxis;
        private Controller controller;    
        private JoystickAxis xAxis;
        private JoystickAxis yAxis;
        private JoystickAxis povX;
        private JoystickAxis povY;
        private Map<Component, JoystickAxis> axisIndex = new HashMap<Component, JoystickAxis>();
        private Map<Component, JoystickButton> buttonIndex = new HashMap<Component, JoystickButton>();
    
        public JInputJoystick( InputManager inputManager, JoyInput joyInput, Controller controller, 
                               int joyId, String name ) {
            super( inputManager, joyInput, joyId, name );
            
            this.controller = controller;
            
            this.nullAxis = new DefaultJoystickAxis( getInputManager(), this, -1, 
                                                     "Null", "null", false, false, 0 );
            this.xAxis = nullAxis;                                                     
            this.yAxis = nullAxis;                                                     
            this.povX = nullAxis;
            this.povY = nullAxis;                                                     
        }

        protected void addComponent( Component comp ) {
            
            Identifier id = comp.getIdentifier();
            if( id instanceof Button ) {
                addButton(comp);
            } else if( id instanceof Axis ) {
                addAxis(comp);
            } else {
                logger.log(Level.FINE, "Ignoring: \"{0}\"", comp);
            }
        }

        protected void addButton( Component comp ) {
        
            logger.log(Level.FINE, "Adding button: \"{0}\" id:" + comp.getIdentifier(), comp);
            
            Identifier id = comp.getIdentifier();            
            if( !(id instanceof Button) ) {
                throw new IllegalArgumentException( "Component is not an axis:" + comp );
            }

            String name = comp.getName();
            String original = id.getName();
            String logicalId = JoystickCompatibilityMappings.remapComponent( controller.getName(), original );
            if( name != original ) {
                logger.log(Level.FINE, "Remapped:" + original + " to:" + logicalId);
            }
 
            JoystickButton button = new DefaultJoystickButton( getInputManager(), this, getButtonCount(),
                                                               name, logicalId );
            addButton(button);                                                               
            buttonIndex.put( comp, button );
        }
        
        protected void addAxis( Component comp ) {

            logger.log(Level.FINE, "Adding axis: \"{0}\" id:" + comp.getIdentifier(), comp );
                            
            Identifier id = comp.getIdentifier();
            if( !(id instanceof Axis) ) {
                throw new IllegalArgumentException( "Component is not an axis:" + comp );
            }
            
            String name = comp.getName();
            String original = id.getName();
            String logicalId = JoystickCompatibilityMappings.remapComponent( controller.getName(), original );
            if( name != original ) {
                logger.log(Level.FINE, "Remapped:" + original + " to:" + logicalId);
            }
            
            JoystickAxis axis = new DefaultJoystickAxis( getInputManager(), 
                                                         this, getAxisCount(), name, logicalId,
                                                         comp.isAnalog(), comp.isRelative(), 
                                                         comp.getDeadZone() );
            addAxis(axis);                                                          
            axisIndex.put( comp, axis );
                       
            // Support the X/Y axis indexes
            if( id == Axis.X ) {
                xAxis = axis;
            } else if( id == Axis.Y ) {
                yAxis = axis;
            } else if( id == Axis.POV ) {
                
                // Add two fake axes for the JME provided convenience
                // axes: AXIS_POV_X, AXIS_POV_Y
                povX = new DefaultJoystickAxis( getInputManager(), 
                                                this, getAxisCount(), JoystickAxis.POV_X, 
                                                id.getName() + "_x",
                                                comp.isAnalog(), comp.isRelative(), comp.getDeadZone() );
                logger.log(Level.FINE, "Adding axis: \"{0}\" id:" + id.getName() + "_x", povX.getName() );
                addAxis(povX);
                povY = new DefaultJoystickAxis( getInputManager(), 
                                                this, getAxisCount(), JoystickAxis.POV_Y, 
                                                id.getName() + "_y",
                                                comp.isAnalog(), comp.isRelative(), comp.getDeadZone() );
                logger.log(Level.FINE, "Adding axis: \"{0}\" id:" + id.getName() + "_y", povY.getName() );
                addAxis(povY);
            }
            
        }
 
        @Override
        public JoystickAxis getXAxis() {
            return xAxis;
        }     

        @Override
        public JoystickAxis getYAxis() {
            return yAxis;
        }     

        @Override
        public JoystickAxis getPovXAxis() {
            return povX;
        }     

        @Override
        public JoystickAxis getPovYAxis() {
            return povY;
        }     

        @Override
        public int getXAxisIndex(){
            return xAxis.getAxisId();
        }

        @Override
        public int getYAxisIndex(){
            return yAxis.getAxisId();
        }
    }    
}




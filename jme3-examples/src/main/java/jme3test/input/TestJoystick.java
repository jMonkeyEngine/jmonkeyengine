package jme3test.input;

import com.jme3.app.SimpleApplication;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapText;
import com.jme3.input.Joystick;
import com.jme3.input.JoystickAxis;
import com.jme3.input.JoystickButton;
import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.system.AppSettings;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class TestJoystick extends SimpleApplication {

    private Joystick viewedJoystick;
    private GamepadView gamepad;
    private Node joystickInfo;
    private float yInfo = 0;
    private JoystickButton lastButton;

    public static void main(String[] args){
        TestJoystick app = new TestJoystick();
        AppSettings settings = new AppSettings(true);
        settings.setUseJoysticks(true);
        app.setSettings(settings);
        app.start();
    }
    
    @Override
    public void simpleInitApp() {
        getFlyByCamera().setEnabled(false);

        Joystick[] joysticks = inputManager.getJoysticks();
        if (joysticks == null)
            throw new IllegalStateException("Cannot find any joysticks!");

        try {
            PrintWriter out = new PrintWriter( new FileWriter( "joysticks-" + System.currentTimeMillis() + ".txt" ) );
            dumpJoysticks( joysticks, out );
            out.close();
        } catch( IOException e ) {
            throw new RuntimeException( "Error writing joystick dump", e );
        }   


        int gamepadSize = cam.getHeight() / 2;
        float scale = gamepadSize / 512.0f;        
        gamepad = new GamepadView();       
        gamepad.setLocalTranslation( cam.getWidth() - gamepadSize - (scale * 20), 0, 0 );
        gamepad.setLocalScale( scale, scale, scale ); 
        guiNode.attachChild(gamepad); 

        joystickInfo = new Node( "joystickInfo" );
        joystickInfo.setLocalTranslation( 0, cam.getHeight(), 0 );
        guiNode.attachChild( joystickInfo );

        // Add a raw listener because it's easier to get all joystick events
        // this way.
        inputManager.addRawInputListener( new JoystickEventListener() );
        
        // add action listener for mouse click 
        // to all easier custom mapping
        inputManager.addMapping("mouseClick", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if(isPressed){
                    pickGamePad(getInputManager().getCursorPosition());
                }
            }
        }, "mouseClick");
    }

    protected void dumpJoysticks( Joystick[] joysticks, PrintWriter out ) {
        for( Joystick j : joysticks ) {
            out.println( "Joystick[" + j.getJoyId() + "]:" + j.getName() );
            out.println( "  buttons:" + j.getButtonCount() );
            for( JoystickButton b : j.getButtons() ) {
                out.println( "   " + b );
            }
            
            out.println( "  axes:" + j.getAxisCount() );
            for( JoystickAxis axis : j.getAxes() ) {
                out.println( "   " + axis );
            }
        }
    }

    protected void addInfo( String info, int column ) {
    
        BitmapText t = new BitmapText(guiFont);
        t.setText( info );
        t.setLocalTranslation( column * 200, yInfo, 0 );
        joystickInfo.attachChild(t);
        yInfo -= t.getHeight();
    }

    protected void setViewedJoystick( Joystick stick ) {
        if( this.viewedJoystick == stick )
            return;
 
        if( this.viewedJoystick != null ) {
            joystickInfo.detachAllChildren();
        }
                   
        this.viewedJoystick = stick;
 
        if( this.viewedJoystick != null ) {       
            // Draw the hud
            yInfo = 0;
 
            addInfo(  "Joystick:\"" + stick.getName() + "\"  id:" + stick.getJoyId(), 0 );
 
            yInfo -= 5;
                       
            float ySave = yInfo;
            
            // Column one for the buttons
            addInfo( "Buttons:", 0 );
            for( JoystickButton b : stick.getButtons() ) {
                addInfo( " '" + b.getName() + "' id:'" + b.getLogicalId() + "'", 0 );
            }
            yInfo = ySave;
            
            // Column two for the axes
            addInfo( "Axes:", 1 );
            for( JoystickAxis a : stick.getAxes() ) {
                addInfo( " '" + a.getName() + "' id:'" + a.getLogicalId() + "' analog:" + a.isAnalog(), 1 );
            }
            
        } 
    }
 
    /**
     *  Easier to watch for all button and axis events with a raw input listener.
     */   
    protected class JoystickEventListener implements RawInputListener {

        final private Map<JoystickAxis, Float> lastValues = new HashMap<>();

        @Override
        public void onJoyAxisEvent(JoyAxisEvent evt) {
            Float last = lastValues.remove(evt.getAxis());
            float value = evt.getValue();
                    
            // Check the axis dead zone.  InputManager normally does this
            // by default but not for raw events like we get here.
            float effectiveDeadZone = Math.max(inputManager.getAxisDeadZone(), evt.getAxis().getDeadZone());
            if( Math.abs(value) < effectiveDeadZone ) {
                if( last == null ) {
                    // Just skip the event
                    return;
                }
                // Else set the value to 0
                lastValues.remove(evt.getAxis());
                value = 0;
            }         
            setViewedJoystick( evt.getAxis().getJoystick() );            
            gamepad.setAxisValue( evt.getAxis(), value );
            if( value != 0 ) {
                lastValues.put(evt.getAxis(), value);
            } 
        }

        @Override
        public void onJoyButtonEvent(JoyButtonEvent evt) {
            setViewedJoystick( evt.getButton().getJoystick() );
            gamepad.setButtonValue( evt.getButton(), evt.isPressed() ); 
        }

        @Override
        public void beginInput() {}
        @Override
        public void endInput() {}
        @Override
        public void onMouseMotionEvent(MouseMotionEvent evt) {}
        @Override
        public void onMouseButtonEvent(MouseButtonEvent evt) {}
        @Override
        public void onKeyEvent(KeyInputEvent evt) {}
        @Override
        public void onTouchEvent(TouchEvent evt) {}        
    }

    protected class GamepadView extends Node {
    
        private float xAxis = 0;
        private float yAxis = 0;
        private float zAxis = 0;
        private float zRotation = 0;
        
        private float lastPovX = 0;
        private float lastPovY = 0;
 
        final private Geometry leftStick;
        final private Geometry rightStick;
            
        final private Map<String, ButtonView> buttons = new HashMap<>();
    
        public GamepadView() {
            super( "gamepad" );
 
            // Sizes naturally for the texture size.  All positions will
            // be in that space because it's easier.
            int size = 512;
 
            Material m = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            m.setTexture( "ColorMap", assetManager.loadTexture( "Interface/Joystick/gamepad-buttons.png" ) );
            m.getAdditionalRenderState().setBlendMode( BlendMode.Alpha ); 
            Geometry buttonPanel = new Geometry( "buttons", new Quad(size, size) );
            buttonPanel.setLocalTranslation( 0, 0, -1 );
            buttonPanel.setMaterial(m);
            attachChild(buttonPanel);
        
            m = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            m.setTexture( "ColorMap", assetManager.loadTexture( "Interface/Joystick/gamepad-frame.png" ) );
            m.getAdditionalRenderState().setBlendMode( BlendMode.Alpha ); 
            Geometry frame = new Geometry( "frame", new Quad(size, size) );
            frame.setMaterial(m);
            attachChild(frame);
            
            m = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            m.setTexture( "ColorMap", assetManager.loadTexture( "Interface/Joystick/gamepad-stick.png" ) );
            m.getAdditionalRenderState().setBlendMode( BlendMode.Alpha ); 
            leftStick = new Geometry( "leftStick", new Quad(64, 64) );
            leftStick.setMaterial(m);
            attachChild(leftStick);
            rightStick = new Geometry( "rightStick", new Quad(64, 64) );
            rightStick.setMaterial(m);
            attachChild(rightStick);

            // A "standard" mapping... fits a majority of my game pads
            addButton( JoystickButton.BUTTON_0, 371, 512 - 176, 42, 42 );
            addButton( JoystickButton.BUTTON_1, 407, 512 - 212, 42, 42 );
            addButton( JoystickButton.BUTTON_2, 371, 512 - 248, 42, 42 );
            addButton( JoystickButton.BUTTON_3, 334, 512 - 212, 42, 42 );
 
            // Front buttons  Some of these have the top ones and the bottoms ones flipped.           
            addButton( JoystickButton.BUTTON_4, 67, 512 - 111, 95, 21 );
            addButton( JoystickButton.BUTTON_5, 348, 512 - 111, 95, 21 );
            addButton( JoystickButton.BUTTON_6, 67, 512 - 89, 95, 21 );
            addButton( JoystickButton.BUTTON_7, 348, 512 - 89, 95, 21 );
 
            // Select and start buttons           
            addButton( JoystickButton.BUTTON_8, 206, 512 - 198, 48, 30 );
            addButton( JoystickButton.BUTTON_9, 262, 512 - 198, 48, 30 );
            
            // Joystick push buttons
            addButton( JoystickButton.BUTTON_10, 147, 512 - 300, 75, 70 );
            addButton( JoystickButton.BUTTON_11, 285, 512 - 300, 75, 70 );

            // Fake button highlights for the POV axes
            //
            //    +Y  
            //  -X  +X
            //    -Y
            //
            addButton( "POV +Y", 96, 512 - 174, 40, 38 );
            addButton( "POV +X", 128, 512 - 208, 40, 38 );
            addButton( "POV -Y", 96, 512 - 239, 40, 38 );
            addButton( "POV -X", 65, 512 - 208, 40, 38 );

            resetPositions();                                               
        }
 
        private void addButton( String name, float x, float y, float width, float height ) {
            ButtonView b = new ButtonView(name, x, y, width, height);
            attachChild(b);
            buttons.put(name, b);
        }
 
        public void setAxisValue( JoystickAxis axis, float value ) {
                
            System.out.println( "Axis:" + axis.getName() + "(id:" + axis.getLogicalId() + ")=" + value );
            if( axis == axis.getJoystick().getXAxis() ) {
                setXAxis(value);
            } else if( axis == axis.getJoystick().getYAxis() ) {
                setYAxis(-value);
            } else if( axis == axis.getJoystick().getAxis(JoystickAxis.Z_AXIS) ) {
                // Note: in the above condition, we could check the axis name, but
                //       I have at least one joystick that reports 2 "Z Axis" axes.
                //       In this particular case, the first one is the right one so
                //       a name based lookup will find the proper one.  It's a problem
                //       because the erroneous axis sends a constant stream of values.
                setZAxis(value);
            } else if( axis == axis.getJoystick().getAxis(JoystickAxis.Z_ROTATION) ) {
                setZRotation(-value);
            } else if( axis == axis.getJoystick().getAxis(JoystickAxis.LEFT_TRIGGER) ) {
                if( axis.getJoystick().getButton(JoystickButton.BUTTON_6) == null ) {
                    // left/right triggers sometimes only show up as axes
                    boolean pressed = value != 0;
                    if( pressed != buttons.get(JoystickButton.BUTTON_6).isDown() ) {
                        setButtonValue(JoystickButton.BUTTON_6, pressed);
                    }
                }
            } else if( axis == axis.getJoystick().getAxis(JoystickAxis.RIGHT_TRIGGER) ) {
                if( axis.getJoystick().getButton(JoystickButton.BUTTON_7) == null ) {
                    // left/right triggers sometimes only show up as axes
                    boolean pressed = value != 0;
                    if( pressed != buttons.get(JoystickButton.BUTTON_7).isDown() ) {
                        setButtonValue(JoystickButton.BUTTON_7, pressed);
                    }
                }
            } else if( axis == axis.getJoystick().getPovXAxis() ) {
                if( lastPovX < 0 ) {
                    setButtonValue( "POV -X", false );    
                } else if( lastPovX > 0 ) {
                    setButtonValue( "POV +X", false );    
                } 
                if( value < 0 ) {
                    setButtonValue( "POV -X", true );    
                } else if( value > 0 ) {
                    setButtonValue( "POV +X", true );    
                }
                lastPovX = value; 
            } else if( axis == axis.getJoystick().getPovYAxis() ) {
                if( lastPovY < 0 ) {
                    setButtonValue( "POV -Y", false );    
                } else if( lastPovY > 0 ) {
                    setButtonValue( "POV +Y", false );    
                } 
                if( value < 0 ) {
                    setButtonValue( "POV -Y", true );    
                } else if( value > 0 ) {
                    setButtonValue( "POV +Y", true );    
                }
                lastPovY = value; 
            }
        }
  
        public void setButtonValue( JoystickButton button, boolean isPressed ) {
            System.out.println( "Button:" + button.getName() + "=" + (isPressed ? "Down" : "Up") );
            setButtonValue( button.getLogicalId(), isPressed );
            lastButton = button;
        }

        protected void setButtonValue( String name, boolean isPressed ) {
            ButtonView view = buttons.get(name);
            if( view != null ) {
                if( isPressed ) {
                    view.down();
                } else {
                    view.up();
                }
            }
        }           

        public void setXAxis( float f ) {
            xAxis = f;
            resetPositions();           
        }

        public void setYAxis( float f ) {
            yAxis = f;
            resetPositions();           
        }

        public void setZAxis( float f ) {
            zAxis = f;
            resetPositions();
        }

        public void setZRotation( float f ) {
            zRotation = f;
            resetPositions();
        }
            
        private void resetPositions() {
 
            float xBase = 155;
            float yBase = 212;
            
            Vector2f dir = new Vector2f(xAxis, yAxis);
            float length = Math.min(1, dir.length());
            dir.normalizeLocal();
            
            float angle = dir.getAngle();
            float x = FastMath.cos(angle) * length * 10;
            float y = FastMath.sin(angle) * length * 10;  
            leftStick.setLocalTranslation( xBase + x, yBase + y, 0 );
            
             
            xBase = 291;
            dir = new Vector2f(zAxis, zRotation);
            length = Math.min(1, dir.length());
            dir.normalizeLocal();
            
            angle = dir.getAngle();
            x = FastMath.cos(angle) * length * 10;
            y = FastMath.sin(angle) * length * 10;  
            rightStick.setLocalTranslation( xBase + x, yBase + y, 0 );
        }
    }
    
    protected class ButtonView extends Node {
 
        private int state = 0;
        final private Material material;
        final private ColorRGBA hilite = new ColorRGBA( 0.0f, 0.75f, 0.75f, 0.5f );
        
        public ButtonView( String name, float x, float y, float width, float height ) {
            super( "Button:" + name );
            setLocalTranslation( x, y, -0.5f );
            
            material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            material.setColor( "Color", hilite );
            material.getAdditionalRenderState().setBlendMode( BlendMode.Alpha ); 

            Geometry g = new Geometry( "highlight", new Quad(width, height) );
            g.setMaterial(material); 
            attachChild(g);
            
            resetState();            
        }
 
        private void resetState() {
            if( state <= 0 ) {
                setCullHint( CullHint.Always );
            } else {
                setCullHint( CullHint.Dynamic );
            }
            
            System.out.println( getName() + " state:" + state );
        }
        
        public boolean isDown() {
            return state > 0;
        }
        
        public void down() {
            state++;            
            resetState();
        }
        
        public void up() {
            state--;
            resetState();
        } 
    }
    
    private void pickGamePad(Vector2f mouseLoc){
        if (lastButton != null) {
            CollisionResults results = pick(cam, mouseLoc, gamepad);
            for (CollisionResult cr : results) {
                Node n = cr.getGeometry().getParent();
                if (n != null && (n instanceof ButtonView)) {
                    String b = n.getName().substring("Button:".length());
                    String name = lastButton.getJoystick().getName().replaceAll(" ", "\\\\ ");
                    String id = lastButton.getLogicalId().replaceAll(" ", "\\\\ ");
                    System.out.println(name + "." + id + "=" + b);
                    return;
                }
            }
        }
    }
    
    private static CollisionResults pick(Camera cam, Vector2f mouseLoc, Node node) {
        CollisionResults results = new CollisionResults();
        Ray ray = new Ray();
        Vector3f pos = new Vector3f(mouseLoc.x, mouseLoc.y, -1);
        Vector3f dir = new Vector3f(mouseLoc.x, mouseLoc.y, 1);
        dir.subtractLocal(pos).normalizeLocal();
        ray.setOrigin(pos);
        ray.setDirection(dir);
        node.collideWith(ray, results);
        return results;
    }
}

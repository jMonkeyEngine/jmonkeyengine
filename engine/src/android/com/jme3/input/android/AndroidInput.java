package com.jme3.input.android;

import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import com.jme3.input.android.TouchEvent;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.math.Vector2f;


public class AndroidInput extends GLSurfaceView implements KeyInput, MouseInput, 
                                                           GestureDetector.OnGestureListener, ScaleGestureDetector.OnScaleGestureListener
{
    private final static Logger logger = Logger.getLogger(AndroidInput.class.getName());
    
    private RawInputListener listenerRaw = null;
    private AndroidTouchInputListener listenerTouch = null;
    private ScaleGestureDetector scaledetector;
    private GestureDetector detector;
    private Vector2f lastPos = new Vector2f();
    private boolean dragging = false;
    
    private List<Object> currentEvents = new ArrayList<Object>();

    private final static int MAX_EVENTS = 1024;

    
    private boolean FIRE_MOUSE_EVENTS = true;

   
    private static final int[] ANDROID_TO_JME = {
        0x0, // unknown
        0x0, // key code soft left
        0x0, // key code soft right
        KeyInput.KEY_HOME,
        KeyInput.KEY_ESCAPE, // key back
        0x0, // key call
        0x0, // key endcall
        KeyInput.KEY_0,
        KeyInput.KEY_1,
        KeyInput.KEY_2,
        KeyInput.KEY_3,
        KeyInput.KEY_4,
        KeyInput.KEY_5,
        KeyInput.KEY_6,
        KeyInput.KEY_7,
        KeyInput.KEY_8,
        KeyInput.KEY_9,
        KeyInput.KEY_MULTIPLY,
        0x0, // key pound
        KeyInput.KEY_UP,
        KeyInput.KEY_DOWN,
        KeyInput.KEY_LEFT,
        KeyInput.KEY_RIGHT,
        KeyInput.KEY_RETURN, // dpad center
        0x0, // volume up
        0x0, // volume down
        KeyInput.KEY_POWER, // power (?)
        0x0, // camera
        0x0, // clear
        KeyInput.KEY_A,
        KeyInput.KEY_B,
        KeyInput.KEY_C,
        KeyInput.KEY_D,
        KeyInput.KEY_E,
        KeyInput.KEY_F,
        KeyInput.KEY_G,
        KeyInput.KEY_H,
        KeyInput.KEY_I,
        KeyInput.KEY_J,
        KeyInput.KEY_K,
        KeyInput.KEY_L,
        KeyInput.KEY_M,
        KeyInput.KEY_N,
        KeyInput.KEY_O,
        KeyInput.KEY_P,
        KeyInput.KEY_Q,
        KeyInput.KEY_R,
        KeyInput.KEY_S,
        KeyInput.KEY_T,
        KeyInput.KEY_U,
        KeyInput.KEY_V,
        KeyInput.KEY_W,
        KeyInput.KEY_X,
        KeyInput.KEY_Y,
        KeyInput.KEY_Z,
        KeyInput.KEY_COMMA,
        KeyInput.KEY_PERIOD,

        KeyInput.KEY_LMENU,
        KeyInput.KEY_RMENU,

        KeyInput.KEY_LSHIFT,
        KeyInput.KEY_RSHIFT,

//        0x0, // fn
//        0x0, // cap (?)

        KeyInput.KEY_TAB,
        KeyInput.KEY_SPACE,
        0x0, // sym (?) symbol
        0x0, // explorer
        0x0, // envelope
        KeyInput.KEY_RETURN, // newline/enter
        KeyInput.KEY_DELETE,
        KeyInput.KEY_GRAVE,
        KeyInput.KEY_MINUS,
        KeyInput.KEY_EQUALS,

        KeyInput.KEY_LBRACKET,
        KeyInput.KEY_RBRACKET,

        KeyInput.KEY_BACKSLASH,
        KeyInput.KEY_SEMICOLON,
        KeyInput.KEY_APOSTROPHE,
        KeyInput.KEY_SLASH,
        KeyInput.KEY_AT, // at (@)
        KeyInput.KEY_NUMLOCK, //0x0, // num
        0x0, //headset hook
        0x0, //focus
        KeyInput.KEY_ADD,
        KeyInput.KEY_LMETA, //menu
        0x0,//notification
        0x0,//search
        0x0,//media play/pause
        0x0,//media stop
        0x0,//media next
        0x0,//media previous
        0x0,//media rewind
        0x0,//media fastforward
        0x0,//mute
    };

    public AndroidInput(Context ctx, AttributeSet attribs)
    {
        super(ctx, attribs);
        detector=new GestureDetector(this);
        scaledetector=new ScaleGestureDetector(ctx, this);        
    }

    public AndroidInput(Context ctx)
    {
        super(ctx);
        detector=new GestureDetector(this);
        scaledetector=new ScaleGestureDetector(ctx, this);
    }

    /**
     * onTouchEvent gets called from android thread on touchpad events
     */
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        boolean bWasHandled = false;
        MouseButtonEvent btn;
        TouchEvent touch;

        // Send the raw event
        processEvent(event);
        
        // Try to detect gestures
        this.detector.onTouchEvent(event);
        this.scaledetector.onTouchEvent(event);

        
        switch (event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                
                // Store current pos
                lastPos.set(event.getX(),event.getY());
                
                if (FIRE_MOUSE_EVENTS)
                {
                   // Handle mouse events 
                    btn = new MouseButtonEvent(0, true, (int)lastPos.getX(), (int)lastPos.getY());
                    btn.setTime(event.getEventTime());
                    processEvent(btn);
                }
                
                // Handle gesture events
                touch = new TouchEvent(TouchEvent.Type.GRABBED, TouchEvent.Operation.NOP,event.getX(),event.getY(),0,0,null);
                processEvent(touch);
                
                bWasHandled = true;
                break;
                
            case MotionEvent.ACTION_UP:
                
                if (FIRE_MOUSE_EVENTS)
                {                
                    // Handle mouse events 
                    btn = new MouseButtonEvent(0, false, (int)event.getX(), (int)event.getY());
                    btn.setTime(event.getEventTime());
                    processEvent(btn);
                }                
                // Handle gesture events             
                if(dragging)
                {
                    touch = new TouchEvent(TouchEvent.Type.DRAGGED, TouchEvent.Operation.STOPPED,event.getX(),event.getY(),event.getX()-lastPos.getX(),event.getY()-lastPos.getY(),null);
                    processEvent(touch);
                }
                touch = new TouchEvent(TouchEvent.Type.RELEASED, TouchEvent.Operation.NOP,event.getX(),event.getY(),0,0,null);
                processEvent(touch);
                dragging=false;
                bWasHandled = true;
                break;
            case MotionEvent.ACTION_MOVE:
                if(!scaledetector.isInProgress())
                {
                    if(!dragging)
                        touch = new TouchEvent(TouchEvent.Type.DRAGGED, TouchEvent.Operation.STARTED,event.getX(),event.getY(),event.getX()-lastPos.getX(),event.getY()-lastPos.getY(),null);
                    else
                        touch = new TouchEvent(TouchEvent.Type.DRAGGED, TouchEvent.Operation.RUNNING,event.getX(),event.getY(),event.getX()-lastPos.getX(),event.getY()-lastPos.getY(),null);
                        
                    processEvent(touch);
                    dragging=true;
                }
                if (FIRE_MOUSE_EVENTS)
                {
                    int newX = getWidth() - (int) event.getX();
                    int newY = (int) event.getY();
                    int dx;
                    int dy;
                    if (lastPos.getX() != -1){
                        dx = newX - (int)lastPos.getX();
                        dy = newY - (int)lastPos.getY();
                    }else{
                        dx = 0;
                        dy = 0;
                    }                    
                    MouseMotionEvent mot = new MouseMotionEvent(newX, newY, dx, dy, 0, 0);
                    mot.setTime(event.getEventTime());
                    processEvent(mot);
                }
                bWasHandled = true;
                break;
                
            // TODO: implement motion events
            case MotionEvent.ACTION_POINTER_UP:
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                break;

            case MotionEvent.ACTION_OUTSIDE:
                break;

            case MotionEvent.ACTION_CANCEL:
                break;
        }
        return bWasHandled;        
    }

    @Override
    public boolean onKeyDown (int keyCode, KeyEvent event) {

        // Send the raw event
        processEvent(event);
        
        int jmeCode  = ANDROID_TO_JME[keyCode];
        if (jmeCode != 0)
        {
            String str =  event.getCharacters();
            char c = str != null && str.length() > 0 ? str.charAt(0) : 0x0;
            KeyInputEvent evt = new KeyInputEvent(jmeCode, c, true, false);
            logger.info("onKeyDown " + evt);
            processEvent(evt);
        }
        // Handle all keys ourself, except the back button (4)
        if (keyCode == 4)
            return false;
        else
            return true;
    }

    @Override
    public boolean onKeyUp (int keyCode, KeyEvent event) {
        
        // Send the raw event
        processEvent(event);
        
        int jmeCode  = ANDROID_TO_JME[keyCode];
        if (jmeCode != 0)
        {            
            String str =  event.getCharacters();
            char c = str != null && str.length() > 0 ? str.charAt(0) : 0x0;
            KeyInputEvent evt = new KeyInputEvent(jmeCode, c, false, false);
            logger.info("onKeyUp " + evt);
            processEvent(evt);
        }
        
        // Handle all keys ourself, except the back button (4)
        if (keyCode == 4)
            return false;
        else
            return true;
    }

    public void setCursorVisible(boolean visible){
    }

    public int getButtonCount(){
        return 255;
    }

    public void initialize() {
    }

    public void update() {
		generateEvents();
    }

    public void destroy() {
    }

    public boolean isInitialized() {
        return true;
    }



	private void processEvent(Object event) 
	{
		synchronized (currentEvents) {
			if (currentEvents.size() < MAX_EVENTS)
				currentEvents.add(event);
		}
	}

	Object event;
	private void generateEvents() {
	    if (listenerRaw != null)
	    {
    		synchronized (currentEvents) {
    			//for (Object event: currentEvents) {
    			for (int i = 0; i < currentEvents.size(); i++) {
    			    event = currentEvents.get(i);
    				if (event instanceof MouseButtonEvent) {
    				    listenerRaw.onMouseButtonEvent((MouseButtonEvent) event);
    				} else if (event instanceof MouseMotionEvent) {
    					listenerRaw.onMouseMotionEvent((MouseMotionEvent) event);
    				} else if (event instanceof KeyInputEvent) {
    					listenerRaw.onKeyEvent((KeyInputEvent) event);				
                    } else if (event instanceof TouchEvent) {
                        if (listenerTouch != null)
                            listenerTouch.onTouchEvent((TouchEvent) event);
                    } else if (event instanceof MotionEvent) {
                        if (listenerTouch != null)
                            listenerTouch.onMotionEvent((MotionEvent) event);                                                                        
                    } else if (event instanceof KeyEvent) {
                        if (listenerTouch != null)
                            listenerTouch.onAndroidKeyEvent((KeyEvent) event);
                    }    				    				    				
    			}
    			currentEvents.clear();
    		}
	    }
	}

    public void setInputListener(RawInputListener listener) {
        this.listenerRaw = listener;
    }
    
    public void setInputListener(AndroidTouchInputListener listener) {
        this.listenerRaw = listener;
        this.listenerTouch = listener;
    }

    public long getInputTimeNanos() {
        return System.nanoTime();
    }

    
    // --------------- Gesture detected callback events ----------------------------------
    
    public boolean onDown(MotionEvent event)
    {
        return false;
    }

    public void onLongPress(MotionEvent event)
    {
        TouchEvent touch = new TouchEvent(TouchEvent.Type.LONGPRESSED, TouchEvent.Operation.NOP,event.getX(),event.getY(),0,0,null);
        processEvent(touch);
    }

    public boolean onFling(MotionEvent event, MotionEvent event2, float vx, float vy)
    {
        TouchEvent touch = new TouchEvent(TouchEvent.Type.FLING, TouchEvent.Operation.NOP,event.getX(),event.getY(),0,0,null);
        processEvent(touch);
        return true;
    }

    public boolean onSingleTapConfirmed(MotionEvent event)
    {
        TouchEvent touch = new TouchEvent(TouchEvent.Type.TAP, TouchEvent.Operation.NOP,event.getX(),event.getY(),0,0,null);
        processEvent(touch);
        return true;
    }

    public boolean onDoubleTap(MotionEvent event)
    {
        TouchEvent touch = new TouchEvent(TouchEvent.Type.DOUBLETAP, TouchEvent.Operation.NOP,event.getX(),event.getY(),0,0,null);
        processEvent(touch);
        return true;
    }

    public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector)
    {
        TouchEvent touch = new TouchEvent(TouchEvent.Type.SCALE, TouchEvent.Operation.STARTED,scaleGestureDetector.getFocusX(),scaleGestureDetector.getFocusY(),0,0,new float[]{scaleGestureDetector.getCurrentSpan(),scaleGestureDetector.getScaleFactor()});
        processEvent(touch);
        return true;
    }

    public boolean onScale(ScaleGestureDetector scaleGestureDetector)
    {        
        TouchEvent touch = new TouchEvent(TouchEvent.Type.SCALE, TouchEvent.Operation.RUNNING,scaleGestureDetector.getFocusX(),scaleGestureDetector.getFocusY(),0,0,new float[]{scaleGestureDetector.getCurrentSpan(),scaleGestureDetector.getScaleFactor()});
        processEvent(touch);
        return false;
    }

    public void onScaleEnd(ScaleGestureDetector scaleGestureDetector)
    {
        TouchEvent touch = new TouchEvent(TouchEvent.Type.SCALE, TouchEvent.Operation.STOPPED,scaleGestureDetector.getFocusX(),scaleGestureDetector.getFocusY(),0,0,new float[]{scaleGestureDetector.getCurrentSpan(),scaleGestureDetector.getScaleFactor()});
        processEvent(touch);        
    }

    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
            float distanceY) {
        // TODO Auto-generated method stub
        return false;
    }

    public void onShowPress(MotionEvent e) {
        // TODO Auto-generated method stub
        
    }

    public boolean onSingleTapUp(MotionEvent event) 
    {
        TouchEvent touch = new TouchEvent(TouchEvent.Type.TAP, TouchEvent.Operation.NOP,event.getX(),event.getY(),0,0,null);
        processEvent(touch);
        return true;
    }

}

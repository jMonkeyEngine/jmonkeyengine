package com.jme3.input.android;

import java.util.HashMap;
import java.util.logging.Logger;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import com.jme3.input.KeyInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.TouchInput;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.input.event.TouchEvent.Type;
import com.jme3.math.Vector2f;
import com.jme3.util.RingBuffer;


public class AndroidInput extends GLSurfaceView implements TouchInput, 
                                                           GestureDetector.OnGestureListener, ScaleGestureDetector.OnScaleGestureListener
{
    private final static Logger logger = Logger.getLogger(AndroidInput.class.getName());
    private boolean isInitialized = false;
    private RawInputListener listener = null;
    
    final private static int MAX_EVENTS = 1024;
    
    final private RingBuffer<TouchEvent> eventQueue = new RingBuffer<TouchEvent>(MAX_EVENTS);
    final private RingBuffer<TouchEvent> eventPool = new RingBuffer<TouchEvent>(MAX_EVENTS);
    final private HashMap<Integer, Vector2f> lastPositions = new HashMap<Integer, Vector2f>();
     
    public boolean fireMouseEvents = true;
    public boolean fireKeyboardEvents = false;

    private ScaleGestureDetector scaledetector;
    private GestureDetector detector;
    private int lastX;
    private int lastY;

   
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
    
    private TouchEvent getNextFreeTouchEvent()
    {
        return getNextFreeTouchEvent(false);
    }

    private TouchEvent getNextFreeTouchEvent(boolean wait)
    {
        TouchEvent evt;
        if (eventPool.isEmpty() && wait)
        {
            logger.warning("eventPool buffer underrun");
            boolean isEmpty;
            do
            {
                synchronized(eventPool)
                {
                    isEmpty = eventPool.isEmpty();
                }
                try { Thread.sleep(50); } catch (InterruptedException e) { }
            }
            while (isEmpty);
            evt = eventPool.pop();
        }
        else if (eventPool.isEmpty())
        {
            evt = new TouchEvent(); 
            logger.warning("eventPool buffer underrun");
        }
        else
        {
            evt = eventPool.pop();    
        }
        return evt;
    }
    /**
     * onTouchEvent gets called from android thread on touchpad events
     */
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        boolean bWasHandled = false;
        TouchEvent touch;
        
        // Try to detect gestures
        this.detector.onTouchEvent(event);
        this.scaledetector.onTouchEvent(event);

        
        switch (event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                
                // Convert all pointers into events
                for (int p = 0; p < event.getPointerCount(); p++)
                {                              
                    touch = getNextFreeTouchEvent();
                    touch.set(Type.DOWN, event.getX(p), event.getY(p), 0, 0);
                    touch.setPointerId(event.getPointerId(p));
                    touch.setTime(event.getEventTime());
                    processEvent(touch);
                }
                
                bWasHandled = true;
                break;
                
            case MotionEvent.ACTION_UP:
                
                // Convert all pointers into events
                for (int p = 0; p < event.getPointerCount(); p++)
                {                              
                    touch = getNextFreeTouchEvent();
                    touch.set(Type.UP, event.getX(p), event.getY(p), 0, 0);
                    touch.setPointerId(event.getPointerId(p));
                    touch.setTime(event.getEventTime());
                    processEvent(touch);
                }
                                
                bWasHandled = true;
                break;
            case MotionEvent.ACTION_MOVE:
                
                // Convert all pointers into events
                for (int p = 0; p < event.getPointerCount(); p++)
                {                      
                    Vector2f lastPos = lastPositions.get(event.getPointerId(p));
                    if (lastPos == null)
                    {
                        lastPos = new Vector2f(event.getX(p), event.getY(p));
                        lastPositions.put(event.getPointerId(p), lastPos);
                    }
                    touch = getNextFreeTouchEvent();
                    touch.set(Type.MOVE, event.getX(p), event.getY(p), event.getX(p) - lastPos.x, event.getY(p) - lastPos.y);
                    touch.setPointerId(event.getPointerId(p));
                    touch.setTime(event.getEventTime());
                    processEvent(touch);
                    lastPos.set(event.getX(p), event.getY(p));
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
    public boolean onKeyDown (int keyCode, KeyEvent event) 
    {
        TouchEvent evt;
        evt = getNextFreeTouchEvent();
        evt.set(TouchEvent.Type.KEY_DOWN);
        evt.setKeyCode(keyCode);
        evt.setCharacters(event.getCharacters());
        evt.setTime(event.getEventTime());

        // Send the event
        processEvent(evt);
        
        // Handle all keys ourself, except the back button (4)
        if (keyCode == 4)
            return false;
        else
            return true;
    }

    @Override
    public boolean onKeyUp (int keyCode, KeyEvent event) 
    {       
        TouchEvent evt;        
        evt = getNextFreeTouchEvent();        
        evt.set(TouchEvent.Type.KEY_UP);
        evt.setKeyCode(keyCode);
        evt.setCharacters(event.getCharacters());
        evt.setTime(event.getEventTime());
        
        // Send the event
        processEvent(evt);
        
        // Handle all keys ourself, except the back button (4)
        if (keyCode == 4)
            return false;
        else
            return true;
    }


    // -----------------------------------------
    // JME3 Input interface
    @Override
    public void initialize() 
    {       
        TouchEvent item;
        for (int i = 0; i < MAX_EVENTS; i++)
        {
            item = new TouchEvent();
            eventPool.push(item);
        }
        isInitialized = true;
    }
    
    @Override
    public void destroy() 
    {
        isInitialized = false;
        
        // Clean up queues
        while (! eventPool.isEmpty())
        {
            eventPool.pop();
        }
        while (! eventQueue.isEmpty())
        {
            eventQueue.pop();
        }
    }
    
    @Override
    public boolean isInitialized() 
    {
        return isInitialized;
    }
    
    @Override
    public void setInputListener(RawInputListener listener) 
    {
        this.listener = listener;
    }
    
    @Override
    public long getInputTimeNanos() 
    {
        return System.nanoTime();
    }
    // -----------------------------------------

	private void processEvent(TouchEvent event) 
	{
		synchronized (eventQueue) 
		{
		    eventQueue.push(event);
		}
	}

	@Override
    public void update() 
    {
        generateEvents();
    }
	
	private void generateEvents() 
	{
	    if (listener != null)
	    {	        
	        TouchEvent event;
	        MouseButtonEvent btn;
	        int newX;
	        int newY;
	        
	        while (!eventQueue.isEmpty())
	        {
	            synchronized (eventQueue) 
	            {
	                event = eventQueue.pop();
	            }
	            if (event != null)
	            {
	                listener.onTouchEvent(event);

                    if (fireMouseEvents)
                    {
    	                newX = getWidth() - (int) event.getX();
    	                newY = (int) event.getY();
    	                switch (event.getType())
    	                {
    	                    case DOWN:    	                  
         	                    // Handle mouse events 
        	                    btn = new MouseButtonEvent(0, true, newX, newY);
        	                    btn.setTime(event.getTime());
        	                    listener.onMouseButtonEvent(btn);
        	                    // Store current pos
        	                    lastX = -1;
        	                    lastY = -1;
        	                    break;
        	                    
    	                    case UP:
    	                        // Handle mouse events 
    	                        btn = new MouseButtonEvent(0, false, newX, newY);
    	                        btn.setTime(event.getTime());
    	                        listener.onMouseButtonEvent(btn);
    	                        // Store current pos
    	                        lastX = -1;
    	                        lastY = -1;
    	                        break;
    	                        
    	                    case MOVE:
    	                        int dx;
    	                        int dy;
    	                        if (lastX != -1){
    	                            dx = newX - lastX;
    	                            dy = newY - lastY;
    	                        }else{
    	                            dx = 0;
    	                            dy = 0;
    	                        }                    
    	                        MouseMotionEvent mot = new MouseMotionEvent(newX, newY, dx, dy, 0, 0);
    	                        mot.setTime(event.getTime());
    	                        listener.onMouseMotionEvent(mot);
    	                        lastX = newX;
    	                        lastY = newY;
    	                        break;
            	        }	                
                    }
	            }
	            synchronized (eventPool) 
	            {
	                eventPool.push(event);
	            }	            
	        }

	    }
	}
    
    // --------------- Gesture detected callback events ----------------------------------
    
    public boolean onDown(MotionEvent event)
    {
        return false;
    }

    public void onLongPress(MotionEvent event)
    {        
        TouchEvent touch = getNextFreeTouchEvent(); 
        touch.set(Type.LONGPRESSED, event.getX(), event.getY(), 0f, 0f);
        touch.setPointerId(0);
        touch.setTime(event.getEventTime());
        processEvent(touch);
    }

    public boolean onFling(MotionEvent event, MotionEvent event2, float vx, float vy)
    {
        TouchEvent touch = getNextFreeTouchEvent(); 
        touch.set(Type.FLING, event.getX(), event.getY(), vx, vy);
        touch.setPointerId(0);
        touch.setTime(event.getEventTime());
        processEvent(touch);
        
        return true;
    }

    public boolean onSingleTapConfirmed(MotionEvent event)
    {        
        TouchEvent touch = getNextFreeTouchEvent(); 
        touch.set(Type.TAP, event.getX(), event.getY(), 0f, 0f);
        touch.setPointerId(0);
        touch.setTime(event.getEventTime());
        processEvent(touch);
        
        return true;
    }

    public boolean onDoubleTap(MotionEvent event)
    {
        TouchEvent touch = getNextFreeTouchEvent(); 
        touch.set(Type.DOUBLETAP, event.getX(), event.getY(), 0f, 0f);
        touch.setPointerId(0);
        touch.setTime(event.getEventTime());
        processEvent(touch);        
        return true;
    }

    public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector)
    {     
        TouchEvent touch = getNextFreeTouchEvent(); 
        touch.set(Type.SCALE_START, scaleGestureDetector.getFocusX(), scaleGestureDetector.getFocusY(), 0f, 0f);
        touch.setPointerId(0);
        touch.setTime(scaleGestureDetector.getEventTime());
        touch.setScaleSpan(scaleGestureDetector.getCurrentSpan()); 
        touch.setScaleFactor(scaleGestureDetector.getScaleFactor());
        processEvent(touch); 
        
        return true;
    }

    public boolean onScale(ScaleGestureDetector scaleGestureDetector)
    {        
        TouchEvent touch = getNextFreeTouchEvent(); 
        touch.set(Type.SCALE_MOVE, scaleGestureDetector.getFocusX(), scaleGestureDetector.getFocusY(), 0f, 0f);
        touch.setPointerId(0);
        touch.setTime(scaleGestureDetector.getEventTime());
        touch.setScaleSpan(scaleGestureDetector.getCurrentSpan()); 
        touch.setScaleFactor(scaleGestureDetector.getScaleFactor());
        processEvent(touch); 
             
        return false;
    }

    public void onScaleEnd(ScaleGestureDetector scaleGestureDetector)
    {        
        TouchEvent touch = getNextFreeTouchEvent(); 
        touch.set(Type.SCALE_END, scaleGestureDetector.getFocusX(), scaleGestureDetector.getFocusY(), 0f, 0f);
        touch.setPointerId(0);
        touch.setTime(scaleGestureDetector.getEventTime());
        touch.setScaleSpan(scaleGestureDetector.getCurrentSpan()); 
        touch.setScaleFactor(scaleGestureDetector.getScaleFactor());
        processEvent(touch);      
    }

    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) 
    {
        TouchEvent touch = getNextFreeTouchEvent(); 
        touch.set(Type.SCROLL, e1.getX(), e1.getY(), distanceX, distanceY);
        touch.setPointerId(0);
        touch.setTime(e1.getEventTime());
        processEvent(touch);
        return false;
    }

    public void onShowPress(MotionEvent event) 
    {
        TouchEvent touch = getNextFreeTouchEvent(); 
        touch.set(Type.SHOWPRESS, event.getX(), event.getY(), 0f, 0f);
        touch.setPointerId(0);
        touch.setTime(event.getEventTime());
        processEvent(touch);
    }

    public boolean onSingleTapUp(MotionEvent event) 
    {       
        TouchEvent touch = getNextFreeTouchEvent(); 
        touch.set(Type.TAP, event.getX(), event.getY(), 0f, 0f);
        touch.setPointerId(0);
        touch.setTime(event.getEventTime());
        processEvent(touch);
        return true;
    }

    @Override
    public void setSimulateMouse(boolean simulate) 
    {
        fireMouseEvents = simulate;       
    }

    @Override
    public void setSimulateKeyboard(boolean simulate) 
    {
        fireKeyboardEvents = simulate;        
    }

}

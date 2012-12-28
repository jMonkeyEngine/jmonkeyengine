package com.jme3.input.android;

import android.view.*;
import com.jme3.input.KeyInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.TouchInput;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.input.event.TouchEvent.Type;
import com.jme3.math.Vector2f;
import com.jme3.system.AppSettings;
import com.jme3.util.RingBuffer;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * <code>AndroidInput</code> is one of the main components that connect jme with android. Is derived from GLSurfaceView and handles all Inputs
 * @author larynx
 *
 */
public class AndroidInput implements
        TouchInput,
        View.OnTouchListener,
        View.OnKeyListener,
        GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener,
        ScaleGestureDetector.OnScaleGestureListener {

    final private static int MAX_EVENTS = 1024;
    // Custom settings
    public boolean mouseEventsEnabled = true;
    public boolean mouseEventsInvertX = false;
    public boolean mouseEventsInvertY = false;
    public boolean keyboardEventsEnabled = false;
    public boolean dontSendHistory = false;
    // Used to transfer events from android thread to GLThread
    final private RingBuffer<TouchEvent> eventQueue = new RingBuffer<TouchEvent>(MAX_EVENTS);
    final private RingBuffer<TouchEvent> eventPoolUnConsumed = new RingBuffer<TouchEvent>(MAX_EVENTS);
    final private RingBuffer<TouchEvent> eventPool = new RingBuffer<TouchEvent>(MAX_EVENTS);
    final private HashMap<Integer, Vector2f> lastPositions = new HashMap<Integer, Vector2f>();
    // Internal
    private View view;
    private ScaleGestureDetector scaledetector;
    private boolean scaleInProgress = false;
    private GestureDetector detector;
    private int lastX;
    private int lastY;
    private final static Logger logger = Logger.getLogger(AndroidInput.class.getName());
    private boolean isInitialized = false;
    private RawInputListener listener = null;
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

    public AndroidInput() {
    }

    public void setView(View view) {
        this.view = view;
        if (view != null) {
            detector = new GestureDetector(null, this, null, false);
            scaledetector = new ScaleGestureDetector(view.getContext(), this);
            view.setOnTouchListener(this);
            view.setOnKeyListener(this);
        }
    }

    private TouchEvent getNextFreeTouchEvent() {
        return getNextFreeTouchEvent(false);
    }

    /**
     * Fetches a touch event from the reuse pool
     * @param wait if true waits for a reusable event to get available/released
     * by an other thread, if false returns a new one if needed.
     *
     * @return a usable TouchEvent
     */
    private TouchEvent getNextFreeTouchEvent(boolean wait) {
        TouchEvent evt = null;
        synchronized (eventPoolUnConsumed) {
            int size = eventPoolUnConsumed.size();
            while (size > 0) {
                evt = eventPoolUnConsumed.pop();
                if (!evt.isConsumed()) {
                    eventPoolUnConsumed.push(evt);
                    evt = null;
                } else {
                    break;
                }
                size--;
            }
        }

        if (evt == null) {
            if (eventPool.isEmpty() && wait) {
                logger.warning("eventPool buffer underrun");
                boolean isEmpty;
                do {
                    synchronized (eventPool) {
                        isEmpty = eventPool.isEmpty();
                    }
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                    }
                } while (isEmpty);
                synchronized (eventPool) {
                    evt = eventPool.pop();
                }
            } else if (eventPool.isEmpty()) {
                evt = new TouchEvent();
                logger.warning("eventPool buffer underrun");
            } else {
                synchronized (eventPool) {
                    evt = eventPool.pop();
                }
            }
        }
        return evt;
    }

    /**
     * onTouch gets called from android thread on touchpad events
     */
    public boolean onTouch(View view, MotionEvent event) {
        if (view != this.view) {
            return false;
        }
        boolean bWasHandled = false;
        TouchEvent touch;
        //    System.out.println("native : " + event.getAction());
        int action = event.getAction() & MotionEvent.ACTION_MASK;
        int pointerIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK)
                >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        int pointerId = event.getPointerId(pointerIndex);
        Vector2f lastPos = lastPositions.get(pointerId);

        // final int historySize = event.getHistorySize();
        //final int pointerCount = event.getPointerCount();
        switch (action) {
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_DOWN:
                touch = getNextFreeTouchEvent();
                touch.set(Type.DOWN, event.getX(pointerIndex), view.getHeight() - event.getY(pointerIndex), 0, 0);
                touch.setPointerId(pointerId);
                touch.setTime(event.getEventTime());
                touch.setPressure(event.getPressure(pointerIndex));
                processEvent(touch);

                lastPos = new Vector2f(event.getX(pointerIndex), view.getHeight() - event.getY(pointerIndex));
                lastPositions.put(pointerId, lastPos);

                bWasHandled = true;
                break;
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                touch = getNextFreeTouchEvent();
                touch.set(Type.UP, event.getX(pointerIndex), view.getHeight() - event.getY(pointerIndex), 0, 0);
                touch.setPointerId(pointerId);
                touch.setTime(event.getEventTime());
                touch.setPressure(event.getPressure(pointerIndex));
                processEvent(touch);
                lastPositions.remove(pointerId);

                bWasHandled = true;
                break;
            case MotionEvent.ACTION_MOVE:
                // Convert all pointers into events
                for (int p = 0; p < event.getPointerCount(); p++) {
                    lastPos = lastPositions.get(p);
                    if (lastPos == null) {
                        lastPos = new Vector2f(event.getX(p), view.getHeight() - event.getY(p));
                        lastPositions.put(event.getPointerId(p), lastPos);
                    }

                    float dX = event.getX(p) - lastPos.x;
                    float dY = view.getHeight() - event.getY(p) - lastPos.y;
                    if (dX != 0 || dY != 0) {
                        touch = getNextFreeTouchEvent();
                        touch.set(Type.MOVE, event.getX(p), view.getHeight() - event.getY(p), dX, dY);
                        touch.setPointerId(event.getPointerId(p));
                        touch.setTime(event.getEventTime());
                        touch.setPressure(event.getPressure(p));
                        touch.setScaleSpanInProgress(scaleInProgress);
                        processEvent(touch);
                        lastPos.set(event.getX(p), view.getHeight() - event.getY(p));
                    }
                }
                bWasHandled = true;
                break;
            case MotionEvent.ACTION_OUTSIDE:
                break;

        }

        // Try to detect gestures
        this.detector.onTouchEvent(event);
        this.scaledetector.onTouchEvent(event);

        return bWasHandled;
    }

    /**
     * onKey gets called from android thread on key events
     */
    public boolean onKey(View view, int keyCode, KeyEvent event) {
        if (view != this.view) {
            return false;
        }

        if (event.getAction() == KeyEvent.ACTION_DOWN) {
        TouchEvent evt;
        evt = getNextFreeTouchEvent();
        evt.set(TouchEvent.Type.KEY_DOWN);
        evt.setKeyCode(keyCode);
        evt.setCharacters(event.getCharacters());
        evt.setTime(event.getEventTime());

        // Send the event
        processEvent(evt);

        // Handle all keys ourself except Volume Up/Down
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP) || (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
            return false;
        } else {
            return true;
        }
        } else if (event.getAction() == KeyEvent.ACTION_UP) {
        TouchEvent evt;
        evt = getNextFreeTouchEvent();
        evt.set(TouchEvent.Type.KEY_UP);
        evt.setKeyCode(keyCode);
        evt.setCharacters(event.getCharacters());
        evt.setTime(event.getEventTime());

        // Send the event
        processEvent(evt);

        // Handle all keys ourself except Volume Up/Down
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP) || (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
            return false;
        } else {
            return true;
        }
        } else {
            return false;
        }
    }

    public void loadSettings(AppSettings settings) {
        mouseEventsEnabled = settings.isEmulateMouse();
        mouseEventsInvertX = settings.isEmulateMouseFlipX();
        mouseEventsInvertY = settings.isEmulateMouseFlipY();
    }

    // -----------------------------------------
    // JME3 Input interface
    @Override
    public void initialize() {
        TouchEvent item;
        for (int i = 0; i < MAX_EVENTS; i++) {
            item = new TouchEvent();
            eventPool.push(item);
        }
        isInitialized = true;
    }

    @Override
    public void destroy() {
        isInitialized = false;

        // Clean up queues
        while (!eventPool.isEmpty()) {
            eventPool.pop();
        }
        while (!eventQueue.isEmpty()) {
            eventQueue.pop();
        }


        this.view = null;
    }

    @Override
    public boolean isInitialized() {
        return isInitialized;
    }

    @Override
    public void setInputListener(RawInputListener listener) {
        this.listener = listener;
    }

    @Override
    public long getInputTimeNanos() {
        return System.nanoTime();
    }
    // -----------------------------------------

    private void processEvent(TouchEvent event) {
        synchronized (eventQueue) {
            eventQueue.push(event);
        }
    }

    //  ---------------  INSIDE GLThread  ---------------
    @Override
    public void update() {
        generateEvents();
    }

    private void generateEvents() {
        if (listener != null) {
            TouchEvent event;
            MouseButtonEvent btn;
            MouseMotionEvent mot;
            int newX;
            int newY;

            while (!eventQueue.isEmpty()) {
                synchronized (eventQueue) {
                    event = eventQueue.pop();
                }
                if (event != null) {
                    listener.onTouchEvent(event);

                    if (mouseEventsEnabled) {
                        if (mouseEventsInvertX) {
                            newX = view.getWidth() - (int) event.getX();
                        } else {
                            newX = (int) event.getX();
                        }

                        if (mouseEventsInvertY) {
                            newY = view.getHeight() - (int) event.getY();
                        } else {
                            newY = (int) event.getY();
                        }

                        switch (event.getType()) {
                            case DOWN:
                                // Handle mouse down event
                                btn = new MouseButtonEvent(0, true, newX, newY);
                                btn.setTime(event.getTime());
                                listener.onMouseButtonEvent(btn);
                                // Store current pos
                                lastX = -1;
                                lastY = -1;
                                break;

                            case UP:
                                // Handle mouse up event
                                btn = new MouseButtonEvent(0, false, newX, newY);
                                btn.setTime(event.getTime());
                                listener.onMouseButtonEvent(btn);
                                // Store current pos
                                lastX = -1;
                                lastY = -1;
                                break;

                            case SCALE_MOVE:
                                if (lastX != -1 && lastY != -1) {
                                    newX = lastX;
                                    newY = lastY;
                                }
                                int wheel = (int) (event.getScaleSpan() / 4f); // scale to match mouse wheel
                                int dwheel = (int) (event.getDeltaScaleSpan() / 4f); // scale to match mouse wheel
                                mot = new MouseMotionEvent(newX, newX, 0, 0, wheel, dwheel);
                                mot.setTime(event.getTime());
                                listener.onMouseMotionEvent(mot);
                                lastX = newX;
                                lastY = newY;

                                break;

                            case MOVE:
                                if (event.isScaleSpanInProgress()) {
                                    break;
                                }

                                int dx;
                                int dy;
                                if (lastX != -1) {
                                    dx = newX - lastX;
                                    dy = newY - lastY;
                                } else {
                                    dx = 0;
                                    dy = 0;
                                }

                                mot = new MouseMotionEvent(newX, newY, dx, dy, (int)event.getScaleSpan(), (int)event.getDeltaScaleSpan());
                                mot.setTime(event.getTime());
                                listener.onMouseMotionEvent(mot);
                                lastX = newX;
                                lastY = newY;

                                break;
                        }
                    }
                }

                if (event.isConsumed() == false) {
                    synchronized (eventPoolUnConsumed) {
                        eventPoolUnConsumed.push(event);
                    }

                } else {
                    synchronized (eventPool) {
                        eventPool.push(event);
                    }
                }
            }

        }
    }
    //  --------------- ENDOF INSIDE GLThread  ---------------

    // --------------- Gesture detected callback events  ---------------
    public boolean onDown(MotionEvent event) {
        return false;
    }

    public void onLongPress(MotionEvent event) {
        TouchEvent touch = getNextFreeTouchEvent();
        touch.set(Type.LONGPRESSED, event.getX(), view.getHeight() - event.getY(), 0f, 0f);
        touch.setPointerId(0);
        touch.setTime(event.getEventTime());
        processEvent(touch);
    }

    public boolean onFling(MotionEvent event, MotionEvent event2, float vx, float vy) {
        TouchEvent touch = getNextFreeTouchEvent();
        touch.set(Type.FLING, event.getX(), view.getHeight() - event.getY(), vx, vy);
        touch.setPointerId(0);
        touch.setTime(event.getEventTime());
        processEvent(touch);

        return true;
    }

    public boolean onSingleTapConfirmed(MotionEvent event) {
        //Nothing to do here the tap has already been detected.
        return false;
    }

    public boolean onDoubleTap(MotionEvent event) {
        TouchEvent touch = getNextFreeTouchEvent();
        touch.set(Type.DOUBLETAP, event.getX(), view.getHeight() - event.getY(), 0f, 0f);
        touch.setPointerId(0);
        touch.setTime(event.getEventTime());
        processEvent(touch);
        return true;
    }

    public boolean onDoubleTapEvent(MotionEvent event) {
        return false;
    }

    public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
        scaleInProgress = true;
        TouchEvent touch = getNextFreeTouchEvent();
        touch.set(Type.SCALE_START, scaleGestureDetector.getFocusX(), scaleGestureDetector.getFocusY(), 0f, 0f);
        touch.setPointerId(0);
        touch.setTime(scaleGestureDetector.getEventTime());
        touch.setScaleSpan(scaleGestureDetector.getCurrentSpan());
        touch.setDeltaScaleSpan(scaleGestureDetector.getCurrentSpan() - scaleGestureDetector.getPreviousSpan());
        touch.setScaleFactor(scaleGestureDetector.getScaleFactor());
        touch.setScaleSpanInProgress(scaleInProgress);
        processEvent(touch);
        //    System.out.println("scaleBegin");

        return true;
    }

    public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
        TouchEvent touch = getNextFreeTouchEvent();
        touch.set(Type.SCALE_MOVE, scaleGestureDetector.getFocusX(), view.getHeight() - scaleGestureDetector.getFocusY(), 0f, 0f);
        touch.setPointerId(0);
        touch.setTime(scaleGestureDetector.getEventTime());
        touch.setScaleSpan(scaleGestureDetector.getCurrentSpan());
        touch.setDeltaScaleSpan(scaleGestureDetector.getCurrentSpan() - scaleGestureDetector.getPreviousSpan());
        touch.setScaleFactor(scaleGestureDetector.getScaleFactor());
        touch.setScaleSpanInProgress(scaleInProgress);
        processEvent(touch);
        //   System.out.println("scale");

        return false;
    }

    public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
        scaleInProgress = false;
        TouchEvent touch = getNextFreeTouchEvent();
        touch.set(Type.SCALE_END, scaleGestureDetector.getFocusX(), view.getHeight() - scaleGestureDetector.getFocusY(), 0f, 0f);
        touch.setPointerId(0);
        touch.setTime(scaleGestureDetector.getEventTime());
        touch.setScaleSpan(scaleGestureDetector.getCurrentSpan());
        touch.setDeltaScaleSpan(scaleGestureDetector.getCurrentSpan() - scaleGestureDetector.getPreviousSpan());
        touch.setScaleFactor(scaleGestureDetector.getScaleFactor());
        touch.setScaleSpanInProgress(scaleInProgress);
        processEvent(touch);
    }

    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        TouchEvent touch = getNextFreeTouchEvent();
        touch.set(Type.SCROLL, e1.getX(), view.getHeight() - e1.getY(), distanceX, distanceY * (-1));
        touch.setPointerId(0);
        touch.setTime(e1.getEventTime());
        processEvent(touch);
        //System.out.println("scroll " + e1.getPointerCount());
        return false;
    }

    public void onShowPress(MotionEvent event) {
        TouchEvent touch = getNextFreeTouchEvent();
        touch.set(Type.SHOWPRESS, event.getX(), view.getHeight() - event.getY(), 0f, 0f);
        touch.setPointerId(0);
        touch.setTime(event.getEventTime());
        processEvent(touch);
    }

    public boolean onSingleTapUp(MotionEvent event) {
        TouchEvent touch = getNextFreeTouchEvent();
        touch.set(Type.TAP, event.getX(), view.getHeight() - event.getY(), 0f, 0f);
        touch.setPointerId(0);
        touch.setTime(event.getEventTime());
        processEvent(touch);
        return true;
    }

    @Override
    public void setSimulateKeyboard(boolean simulate) {
        keyboardEventsEnabled = simulate;
    }

    @Override
    public void setOmitHistoricEvents(boolean dontSendHistory) {
        this.dontSendHistory = dontSendHistory;
    }

    /**
     * @deprecated Use {@link #getSimulateMouse()};
     */
    @Deprecated
    public boolean isMouseEventsEnabled() {
        return mouseEventsEnabled;
    }

    @Deprecated
    public void setMouseEventsEnabled(boolean mouseEventsEnabled) {
        this.mouseEventsEnabled = mouseEventsEnabled;
    }

    public boolean isMouseEventsInvertY() {
        return mouseEventsInvertY;
    }

    public void setMouseEventsInvertY(boolean mouseEventsInvertY) {
        this.mouseEventsInvertY = mouseEventsInvertY;
    }

    public boolean isMouseEventsInvertX() {
        return mouseEventsInvertX;
    }

    public void setMouseEventsInvertX(boolean mouseEventsInvertX) {
        this.mouseEventsInvertX = mouseEventsInvertX;
    }

    public void setSimulateMouse(boolean simulate) {
        mouseEventsEnabled = simulate;
    }

    public boolean getSimulateMouse() {
        return isSimulateMouse();
    }

    public boolean isSimulateMouse() {
        return mouseEventsEnabled;
    }

}

package com.jme3.input.ios;

import com.jme3.input.RawInputListener;
import com.jme3.input.TouchInput;
import com.jme3.input.event.InputEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.system.AppSettings;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IosInputHandler implements TouchInput {
    private static final Logger logger = Logger.getLogger(IosInputHandler.class.getName());

    private final static int MAX_TOUCH_EVENTS = 1024;

    // Custom settings
    private boolean mouseEventsEnabled = true;
    private boolean mouseEventsInvertX = false;
    private boolean mouseEventsInvertY = false;
    private boolean keyboardEventsEnabled = false;

    // Internal
    private boolean initialized = false;
    private RawInputListener listener = null;
    private ConcurrentLinkedQueue<InputEvent> inputEventQueue = new ConcurrentLinkedQueue<>();
    private final TouchEventPool touchEventPool = new TouchEventPool(MAX_TOUCH_EVENTS);
    private IosTouchHandler touchHandler;
    private float scaleX = 1f;
    private float scaleY = 1f;
    private int width = 0;
    private int height = 0;

    public IosInputHandler() {
        touchHandler = new IosTouchHandler(this);
    }
    @Override
    public void initialize() {
        touchEventPool.initialize();
        if (touchHandler != null) {
            touchHandler.initialize();
        }
        initialized = true;
    }

    @Override
    public void update() {
         logger.log(Level.FINE, "InputEvent update : {0}", listener);
       if (listener != null) {
            InputEvent inputEvent;

            while ((inputEvent = inputEventQueue.poll()) != null) {
                if (inputEvent instanceof TouchEvent) {
                    listener.onTouchEvent((TouchEvent)inputEvent);
                } else if (inputEvent instanceof MouseButtonEvent) {
                    listener.onMouseButtonEvent((MouseButtonEvent)inputEvent);
                } else if (inputEvent instanceof MouseMotionEvent) {
                    listener.onMouseMotionEvent((MouseMotionEvent)inputEvent);
                } else if (inputEvent instanceof KeyInputEvent) {
                    listener.onKeyEvent((KeyInputEvent)inputEvent);
                }
            }
        }
    }

    @Override
    public void destroy() {
        initialized = false;
        touchEventPool.destroy();
        if (touchHandler != null) {
            touchHandler.destroy();
        }
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void setInputListener(RawInputListener listener) {
        this.listener = listener;
    }

    @Override
    public long getInputTimeNanos() {
        return System.nanoTime();
    }

    @Override
    public void setSimulateMouse(boolean simulate) {
        this.mouseEventsEnabled = simulate;
    }

    @Override
    public boolean isSimulateMouse() {
        return mouseEventsEnabled;
    }

    @Override
    public void setSimulateKeyboard(boolean simulate) {
        this.keyboardEventsEnabled = simulate;
    }

    @Override
    public boolean isSimulateKeyboard() {
        return keyboardEventsEnabled;
    }

    @Override
    public void setOmitHistoricEvents(boolean dontSendHistory) {
        // not implemented
    }

    // ----------------

    public void loadSettings(AppSettings settings) {
        // TODO: add simulate keyboard to settings
//        keyboardEventsEnabled = true;
        mouseEventsEnabled = true;//settings.isEmulateMouse();
        mouseEventsInvertX = settings.isEmulateMouseFlipX();
        mouseEventsInvertY = settings.isEmulateMouseFlipY();

        // view width and height are 0 until the view is displayed on the screen
        //if (view.getWidth() != 0 && view.getHeight() != 0) {
        //    scaleX = (float)settings.getWidth() / (float)view.getWidth();
        //    scaleY = (float)settings.getHeight() / (float)view.getHeight();
        //}
        scaleX = 1.0f;
        scaleY = 1.0f;
        width = settings.getWidth();
        height = settings.getHeight();
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Setting input scaling, scaleX: {0}, scaleY: {1}, width: {2}, height: {3}",
                    new Object[]{scaleX, scaleY, width, height});
        }
    }

    public boolean isMouseEventsInvertX() {
        return mouseEventsInvertX;
    }

    public boolean isMouseEventsInvertY() {
        return mouseEventsInvertY;
    }

    public float invertX(float origX) {
        return getJmeX(width) - origX;
    }

    public float invertY(float origY) {
        return getJmeY(height) - origY;
    }

    public float getJmeX(float origX) {
        return origX * scaleX;
    }

    public float getJmeY(float origY) {
        return origY * scaleY;
    }

    public TouchEvent getFreeTouchEvent() {
            return touchEventPool.getNextFreeEvent();
    }

    public void addEvent(InputEvent event) {
        inputEventQueue.add(event);
        if (event instanceof TouchEvent) {
            touchEventPool.storeEvent((TouchEvent)event);
        }
    }

    // ----------------

    public void injectTouchDown(int pointerId, long time, float x, float y) {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Using input scaling, scaleX: {0}, scaleY: {1}, width: {2}, height: {3}",
                    new Object[]{scaleX, scaleY, width, height});
        }
        if (touchHandler != null) {
            touchHandler.actionDown(pointerId, time, x, y);
        }
    }

    public void injectTouchUp(int pointerId, long time, float x, float y) {
        if (touchHandler != null) {
            touchHandler.actionUp(pointerId, time, x, y);
        }
    }

    public void injectTouchMove(int pointerId, long time, float x, float y) {
        if (touchHandler != null) {
            touchHandler.actionMove(pointerId, time, x, y);
        }
    }
}

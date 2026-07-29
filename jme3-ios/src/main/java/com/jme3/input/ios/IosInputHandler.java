package com.jme3.input.ios;

import com.jme3.input.RawInputListener;
import com.jme3.input.MouseInput;
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
import org.ngengine.libjglios.core.LibJGLIOSInputBridge;

import static org.ngengine.libjglios.sdl3.SDL3.SDL_GetKeyFromScancode;

public class IosInputHandler implements TouchInput {
    private static final Logger logger = Logger.getLogger(IosInputHandler.class.getName());

    private final static int MAX_TOUCH_EVENTS = 1024;

    // Custom settings
    private boolean mouseEventsEnabled = false;
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

    private int toJmeMouseButton(int sdlButton) {
        switch (sdlButton) {
            case 1:
                return MouseInput.BUTTON_LEFT;
            case 2:
                return MouseInput.BUTTON_MIDDLE;
            case 3:
                return MouseInput.BUTTON_RIGHT;
            default:
                return sdlButton;
        }
    }
    private int width = 0;
    private int height = 0;
    private final int[] nativeIntData = new int[5];
    private final float[] nativeFloatData = new float[4];

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
        pollLibJGLIOSInput();
        dispatchQueuedEvents();
    }

    void dispatchQueuedEvents() {
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
        mouseEventsEnabled = settings.isEmulateMouse();
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

    public void setFramebufferSize(int width, int height) {
        if (width > 0) {
            this.width = width;
        }
        if (height > 0) {
            this.height = height;
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

    private void pollLibJGLIOSInput() {
        long time = getInputTimeNanos();
        while (LibJGLIOSInputBridge.pollEvent(nativeIntData, nativeFloatData)) {
            dispatchBridgeEvent(nativeIntData, nativeFloatData, time);
        }
    }

    void dispatchBridgeEvent(int[] intData, float[] floatData, long time) {
        int type = intData[0];
        switch (type) {
            case LibJGLIOSInputBridge.EVENT_TOUCH_DOWN:
                if (!IosJoyInput.dispatchPointerDown(intData[1], nativeX(floatData[0]), touchY(floatData[1]), time)) {
                    injectTouchDown(intData[1], time, nativeX(floatData[0]), nativeY(floatData[1]));
                }
                break;
            case LibJGLIOSInputBridge.EVENT_TOUCH_UP:
                if (!IosJoyInput.dispatchPointerUp(intData[1], nativeX(floatData[0]), touchY(floatData[1]), time)) {
                    injectTouchUp(intData[1], time, nativeX(floatData[0]), nativeY(floatData[1]));
                }
                break;
            case LibJGLIOSInputBridge.EVENT_TOUCH_MOVE:
                if (!IosJoyInput.dispatchPointerMove(intData[1], nativeX(floatData[0]), touchY(floatData[1]), time)) {
                    injectTouchMove(intData[1], time, nativeX(floatData[0]), nativeY(floatData[1]));
                }
                break;
            case LibJGLIOSInputBridge.EVENT_MOUSE_BUTTON:
                MouseButtonEvent button = new MouseButtonEvent(
                        toJmeMouseButton(intData[1]),
                        intData[2] != 0,
                        Math.round(mouseX(floatData[0])),
                        Math.round(mouseY(floatData[1])));
                button.setTime(time);
                addEvent(button);
                break;
            case LibJGLIOSInputBridge.EVENT_MOUSE_MOTION:
                MouseMotionEvent motion = new MouseMotionEvent(
                        Math.round(mouseX(floatData[0])),
                        Math.round(mouseY(floatData[1])),
                        Math.round(mouseDeltaX(floatData[2])),
                        Math.round(mouseDeltaY(floatData[3])),
                        0,
                        0);
                motion.setTime(time);
                addEvent(motion);
                break;
            case LibJGLIOSInputBridge.EVENT_KEY:
                IosJoyInput.dispatchKeyboardInput();
                int sdlKey = SDL_GetKeyFromScancode(intData[1], intData[4], true);
                char keyChar = sdlKey > 0 && sdlKey <= Character.MAX_VALUE && !Character.isISOControl((char) sdlKey)
                        ? (char) sdlKey
                        : '\0';
                KeyInputEvent key = new KeyInputEvent(
                        IosSdlKeyMap.toJmeKeyCode(intData[1]),
                        keyChar,
                        intData[2] != 0,
                        intData[3] != 0);
                key.setTime(time);
                addEvent(key);
                break;
            case LibJGLIOSInputBridge.EVENT_GAMEPAD_ADDED:
            case LibJGLIOSInputBridge.EVENT_GAMEPAD_REMOVED:
            case LibJGLIOSInputBridge.EVENT_GAMEPAD_AXIS:
            case LibJGLIOSInputBridge.EVENT_GAMEPAD_BUTTON:
                IosJoyInput.dispatchNativeEvent(intData, floatData);
                break;
            default:
                break;
        }
    }

    private float nativeX(float value) {
        return isNormalized(value) ? value * Math.max(width, 1) : value;
    }

    private float nativeY(float value) {
        return isNormalized(value) ? value * Math.max(height, 1) : value;
    }

    private float nativeDeltaX(float value) {
        return Math.abs(value) <= 1f ? value * Math.max(width, 1) : value;
    }

    private float nativeDeltaY(float value) {
        return Math.abs(value) <= 1f ? value * Math.max(height, 1) : value;
    }

    private float mouseX(float value) {
        float x = nativeX(value);
        return mouseEventsInvertX ? invertX(x) : x;
    }

    private float mouseY(float value) {
        float y = invertY(nativeY(value));
        return mouseEventsInvertY ? invertY(y) : y;
    }

    private float touchY(float value) {
        return invertY(nativeY(value));
    }

    private float mouseDeltaX(float value) {
        float deltaX = nativeDeltaX(value);
        return mouseEventsInvertX ? -deltaX : deltaX;
    }

    private float mouseDeltaY(float value) {
        float deltaY = -nativeDeltaY(value);
        return mouseEventsInvertY ? -deltaY : deltaY;
    }

    private boolean isNormalized(float value) {
        return value >= 0f && value <= 1f;
    }
}

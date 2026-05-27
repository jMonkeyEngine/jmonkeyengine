package com.jme3.input.ios;

import com.jme3.input.AbstractJoystick;
import com.jme3.input.DefaultJoystickAxis;
import com.jme3.input.DefaultJoystickButton;
import com.jme3.input.InputManager;
import com.jme3.input.JoyInput;
import com.jme3.input.Joystick;
import com.jme3.input.JoystickAxis;
import com.jme3.input.JoystickButton;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.virtual.VirtualJoystick;
import com.jme3.math.FastMath;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeSystem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.ngengine.libjglios.core.LibJGLIOSInputBridge;
import static org.ngengine.libjglios.sdl3.SDL3.SDL_GAMEPAD_AXIS_COUNT;
import static org.ngengine.libjglios.sdl3.SDL3.SDL_GAMEPAD_AXIS_LEFTX;
import static org.ngengine.libjglios.sdl3.SDL3.SDL_GAMEPAD_AXIS_LEFTY;
import static org.ngengine.libjglios.sdl3.SDL3.SDL_GAMEPAD_AXIS_LEFT_TRIGGER;
import static org.ngengine.libjglios.sdl3.SDL3.SDL_GAMEPAD_AXIS_RIGHTX;
import static org.ngengine.libjglios.sdl3.SDL3.SDL_GAMEPAD_AXIS_RIGHTY;
import static org.ngengine.libjglios.sdl3.SDL3.SDL_GAMEPAD_AXIS_RIGHT_TRIGGER;
import static org.ngengine.libjglios.sdl3.SDL3.SDL_GAMEPAD_BUTTON_BACK;
import static org.ngengine.libjglios.sdl3.SDL3.SDL_GAMEPAD_BUTTON_COUNT;
import static org.ngengine.libjglios.sdl3.SDL3.SDL_GAMEPAD_BUTTON_DPAD_DOWN;
import static org.ngengine.libjglios.sdl3.SDL3.SDL_GAMEPAD_BUTTON_DPAD_LEFT;
import static org.ngengine.libjglios.sdl3.SDL3.SDL_GAMEPAD_BUTTON_DPAD_RIGHT;
import static org.ngengine.libjglios.sdl3.SDL3.SDL_GAMEPAD_BUTTON_DPAD_UP;
import static org.ngengine.libjglios.sdl3.SDL3.SDL_GAMEPAD_BUTTON_EAST;
import static org.ngengine.libjglios.sdl3.SDL3.SDL_GAMEPAD_BUTTON_LABEL_A;
import static org.ngengine.libjglios.sdl3.SDL3.SDL_GAMEPAD_BUTTON_LABEL_B;
import static org.ngengine.libjglios.sdl3.SDL3.SDL_GAMEPAD_BUTTON_LABEL_CIRCLE;
import static org.ngengine.libjglios.sdl3.SDL3.SDL_GAMEPAD_BUTTON_LABEL_CROSS;
import static org.ngengine.libjglios.sdl3.SDL3.SDL_GAMEPAD_BUTTON_LABEL_SQUARE;
import static org.ngengine.libjglios.sdl3.SDL3.SDL_GAMEPAD_BUTTON_LABEL_TRIANGLE;
import static org.ngengine.libjglios.sdl3.SDL3.SDL_GAMEPAD_BUTTON_LABEL_X;
import static org.ngengine.libjglios.sdl3.SDL3.SDL_GAMEPAD_BUTTON_LABEL_Y;
import static org.ngengine.libjglios.sdl3.SDL3.SDL_GAMEPAD_BUTTON_LEFT_SHOULDER;
import static org.ngengine.libjglios.sdl3.SDL3.SDL_GAMEPAD_BUTTON_LEFT_STICK;
import static org.ngengine.libjglios.sdl3.SDL3.SDL_GAMEPAD_BUTTON_NORTH;
import static org.ngengine.libjglios.sdl3.SDL3.SDL_GAMEPAD_BUTTON_RIGHT_SHOULDER;
import static org.ngengine.libjglios.sdl3.SDL3.SDL_GAMEPAD_BUTTON_RIGHT_STICK;
import static org.ngengine.libjglios.sdl3.SDL3.SDL_GAMEPAD_BUTTON_SOUTH;
import static org.ngengine.libjglios.sdl3.SDL3.SDL_GAMEPAD_BUTTON_START;
import static org.ngengine.libjglios.sdl3.SDL3.SDL_GAMEPAD_BUTTON_WEST;
import static org.ngengine.libjglios.sdl3.SDL3.SDL_CloseGamepad;
import static org.ngengine.libjglios.sdl3.SDL3.SDL_CloseJoystick;
import static org.ngengine.libjglios.sdl3.SDL3.SDL_GetGamepadButtonLabel;
import static org.ngengine.libjglios.sdl3.SDL3.SDL_GetGamepadName;
import static org.ngengine.libjglios.sdl3.SDL3.SDL_GetGamepads;
import static org.ngengine.libjglios.sdl3.SDL3.SDL_GetJoystickName;
import static org.ngengine.libjglios.sdl3.SDL3.SDL_GetJoysticks;
import static org.ngengine.libjglios.sdl3.SDL3.SDL_GetNumJoystickAxes;
import static org.ngengine.libjglios.sdl3.SDL3.SDL_GetNumJoystickButtons;
import static org.ngengine.libjglios.sdl3.SDL3.SDL_IsGamepad;
import static org.ngengine.libjglios.sdl3.SDL3.SDL_OpenGamepad;
import static org.ngengine.libjglios.sdl3.SDL3.SDL_OpenJoystick;
import static org.ngengine.libjglios.sdl3.SDL3.SDL_RumbleGamepad;
import static org.ngengine.libjglios.sdl3.SDL3.SDL_RumbleJoystick;
import static org.ngengine.libjglios.sdl3.SDL3.SDL_SetGamepadEventsEnabled;
import static org.ngengine.libjglios.sdl3.SDL3.SDL_SetJoystickEventsEnabled;

public final class IosJoyInput implements JoyInput {
    private static volatile IosJoyInput active;
    private static final int POV_X_AXIS_ID = 0x4000;
    private final Map<Integer, IosJoystick> joysticks = new HashMap<>();
    private RawInputListener listener;
    private InputManager inputManager;
    private boolean initialized;
    private boolean onDeviceJoystickRumble;
    private String virtualJoystickMode = AppSettings.VIRTUAL_JOYSTICK_AUTO;
    private String virtualJoystickDefaultLayout = AppSettings.VIRTUAL_JOYSTICK_LAYOUT_DYNAMIC;
    private boolean useJoysticks = true;
    private boolean keyboardSuppressedAutoJoystick;
    private volatile VirtualJoystick virtualJoystick;

    public static void dispatchNativeEvent(int[] intData, float[] floatData) {
        IosJoyInput joyInput = active;
        if (joyInput != null) {
            joyInput.handleNativeEvent(intData, floatData);
        }
    }

    public static boolean dispatchPointerDown(int pointerId, float x, float y, long time) {
        IosJoyInput joyInput = active;
        return joyInput != null && joyInput.onPointerDown(pointerId, x, y, time);
    }

    public static boolean dispatchPointerMove(int pointerId, float x, float y, long time) {
        IosJoyInput joyInput = active;
        return joyInput != null && joyInput.onPointerMove(pointerId, x, y, time);
    }

    public static boolean dispatchPointerUp(int pointerId, float x, float y, long time) {
        IosJoyInput joyInput = active;
        return joyInput != null && joyInput.onPointerUp(pointerId, x, y, time);
    }

    public static void dispatchKeyboardInput() {
        IosJoyInput joyInput = active;
        if (joyInput != null) {
            joyInput.onKeyboardInput();
        }
    }

    @Override
    public void initialize() {
        initialized = true;
        active = this;
        SDL_SetGamepadEventsEnabled(true);
        SDL_SetJoystickEventsEnabled(true);
    }

    @Override
    public void update() {
        VirtualJoystick joystick = virtualJoystick;
        if (joystick != null) {
            updateVirtualJoystickAutoVisibility();
            joystick.dispatchEvents(listener);
        }
    }

    @Override
    public void destroy() {
        if (active == this) {
            active = null;
        }
        for (IosJoystick joystick : joysticks.values()) {
            joystick.close();
        }
        joysticks.clear();
        initialized = false;
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
    public void setJoyRumble(int joyId, float amountHigh, float amountLow, float duration) {
        if (onDeviceJoystickRumble && JmeSystem.isDeviceRumbleSupported()) {
            JmeSystem.rumble(amountHigh, amountLow, duration);
            return;
        }

        IosJoystick joystick = joysticks.get(joyId);
        if (joystick != null) {
            joystick.rumble(toRumbleAmplitude(amountLow), toRumbleAmplitude(amountHigh),
                    toRumbleDurationMillis(duration));
        }
    }

    @Override
    public void stopJoyRumble(int joyId) {
        if (onDeviceJoystickRumble && JmeSystem.isDeviceRumbleSupported()) {
            JmeSystem.stopRumble();
            return;
        }
        setJoyRumble(joyId, 0f, 0f, 0f);
    }

    public void loadSettings(AppSettings settings) {
        onDeviceJoystickRumble = settings.isOnDeviceJoystickRumble();
        virtualJoystickMode = settings.getVirtualJoystickMode();
        virtualJoystickDefaultLayout = settings.getVirtualJoystickDefaultLayout();
        useJoysticks = settings.useJoysticks();
    }

    @Override
    public Joystick[] loadJoysticks(InputManager inputManager) {
        this.inputManager = inputManager;
        refreshJoysticks(false);
        if (shouldCreateVirtualJoystick()) {
            virtualJoystick = new VirtualJoystick(inputManager, this, nextVirtualJoyId());
            virtualJoystick.setLayout(VirtualJoystick.createLayout(virtualJoystickDefaultLayout));
            virtualJoystick.setEnabled(false);
            updateVirtualJoystickAutoVisibility();
        } else {
            virtualJoystick = null;
        }
        drainPendingEvents();
        return currentJoysticks();
    }

    private boolean onPointerDown(int pointerId, float x, float y, long time) {
        VirtualJoystick joystick = virtualJoystick;
        return joystick != null && joystick.onPointerDown(pointerId, x, y, time);
    }

    private boolean onPointerMove(int pointerId, float x, float y, long time) {
        VirtualJoystick joystick = virtualJoystick;
        return joystick != null && joystick.onPointerMove(pointerId, x, y, time);
    }

    private boolean onPointerUp(int pointerId, float x, float y, long time) {
        VirtualJoystick joystick = virtualJoystick;
        return joystick != null && joystick.onPointerUp(pointerId, x, y, time);
    }

    private void onKeyboardInput() {
        if (AppSettings.VIRTUAL_JOYSTICK_AUTO.equals(virtualJoystickMode)) {
            keyboardSuppressedAutoJoystick = true;
            updateVirtualJoystickAutoVisibility();
        }
    }

    private void drainPendingEvents() {
        int[] intData = new int[4];
        float[] floatData = new float[4];
        while (LibJGLIOSInputBridge.pollEvent(intData, floatData)) {
            handleNativeEvent(intData, floatData);
        }
    }

    private void handleNativeEvent(int[] intData, float[] floatData) {
        int type = intData[0];
        int id = intData[1];
        if (type == LibJGLIOSInputBridge.EVENT_GAMEPAD_ADDED) {
            if (intData[2] != 0) {
                connectGamepad(id, true);
            } else {
                connectJoystick(id, true);
            }
        } else if (type == LibJGLIOSInputBridge.EVENT_GAMEPAD_REMOVED) {
            disconnect(id);
        } else if (type == LibJGLIOSInputBridge.EVENT_GAMEPAD_AXIS) {
            IosJoystick joystick = joysticks.get(id);
            if (joystick == null) {
                refreshJoysticks(false);
                joystick = joysticks.get(id);
            }
            if (joystick == null) {
                return;
            }
            JoystickAxis axis = joystick.getAxisById(intData[2]);
            if (axis != null && listener != null) {
                listener.onJoyAxisEvent(new JoyAxisEvent(axis, floatData[0], intData[3]));
            }
        } else if (type == LibJGLIOSInputBridge.EVENT_GAMEPAD_BUTTON) {
            IosJoystick joystick = joysticks.get(id);
            if (joystick == null) {
                refreshJoysticks(false);
                joystick = joysticks.get(id);
            }
            if (joystick == null) {
                return;
            }
            JoystickButton button = joystick.getButtonById(intData[2]);
            boolean pressed = intData[3] != 0;
            if (button != null && listener != null) {
                listener.onJoyButtonEvent(new JoyButtonEvent(button, pressed));
            }
            joystick.updateDpadPov(intData[2], pressed, listener);
        }
    }

    private void refreshJoysticks(boolean fireConnectionEvent) {
        for (int id : SDL_GetGamepads()) {
            connectGamepad(id, fireConnectionEvent);
        }

        for (int id : SDL_GetJoysticks()) {
            if (!SDL_IsGamepad(id)) {
                connectJoystick(id, fireConnectionEvent);
            }
        }
    }

    private IosJoystick connectGamepad(int id, boolean fireConnectionEvent) {
        IosJoystick joystick = joysticks.get(id);
        if (joystick != null) {
            return joystick;
        }

        long gamepad = SDL_OpenGamepad(id);
        if (gamepad == 0L) {
            return null;
        }

        try {
            String name = SDL_GetGamepadName(gamepad);
            return connect(new IosJoystick(inputManager, this, id, true, gamepad, 0L,
                    displayName(name, "iOS Gamepad", id), SDL_GAMEPAD_AXIS_COUNT, SDL_GAMEPAD_BUTTON_COUNT),
                    fireConnectionEvent);
        } catch (RuntimeException | Error exception) {
            SDL_CloseGamepad(gamepad);
            throw exception;
        }
    }

    private IosJoystick connectJoystick(int id, boolean fireConnectionEvent) {
        IosJoystick joystick = joysticks.get(id);
        if (joystick != null) {
            return joystick;
        }

        long joystickHandle = SDL_OpenJoystick(id);
        if (joystickHandle == 0L) {
            return null;
        }

        try {
            String name = SDL_GetJoystickName(joystickHandle);
            int axisCount = Math.max(0, SDL_GetNumJoystickAxes(joystickHandle));
            int buttonCount = Math.max(0, SDL_GetNumJoystickButtons(joystickHandle));
            return connect(new IosJoystick(inputManager, this, id, false, 0L, joystickHandle,
                    displayName(name, "iOS Joystick", id), axisCount, buttonCount),
                    fireConnectionEvent);
        } catch (RuntimeException | Error exception) {
            SDL_CloseJoystick(joystickHandle);
            throw exception;
        }
    }

    private IosJoystick connect(IosJoystick joystick, boolean fireConnectionEvent) {
        joysticks.put(joystick.getJoyId(), joystick);
        try {
            if (inputManager != null) {
                updateVirtualJoystickAutoVisibility();
                inputManager.setJoysticks(currentJoysticks());
                if (fireConnectionEvent) {
                    inputManager.fireJoystickConnectedEvent(joystick);
                }
            }
            return joystick;
        } catch (RuntimeException | Error exception) {
            joysticks.remove(joystick.getJoyId());
            throw exception;
        }
    }

    private void disconnect(int id) {
        IosJoystick joystick = joysticks.remove(id);
        if (joystick != null) {
            joystick.close();
            if (inputManager != null) {
                updateVirtualJoystickAutoVisibility();
                inputManager.setJoysticks(currentJoysticks());
                inputManager.fireJoystickDisconnectedEvent(joystick);
            }
        }
    }

    private boolean shouldCreateVirtualJoystick() {
        return useJoysticks
                && !AppSettings.VIRTUAL_JOYSTICK_DISABLED.equals(virtualJoystickMode);
    }

    private void updateVirtualJoystickAutoVisibility() {
        if (virtualJoystick == null) {
            return;
        }
        boolean wasEnabled = virtualJoystick.isEnabled();
        boolean active = AppSettings.VIRTUAL_JOYSTICK_ENABLED.equals(virtualJoystickMode)
                || (AppSettings.VIRTUAL_JOYSTICK_AUTO.equals(virtualJoystickMode)
                && joysticks.isEmpty()
                && !keyboardSuppressedAutoJoystick
                && virtualJoystick.hasInputBindings());
        if (wasEnabled != active) {
            virtualJoystick.setEnabled(active);
        }
    }

    private Joystick[] currentJoysticks() {
        List<Joystick> current = new ArrayList<>(joysticks.values());
        VirtualJoystick joystick = virtualJoystick;
        if (joystick != null) {
            current.add(joystick);
        }
        return current.toArray(new Joystick[0]);
    }

    private int nextVirtualJoyId() {
        int id = 0;
        for (Integer joystickId : joysticks.keySet()) {
            id = Math.max(id, joystickId + 1);
        }
        return id;
    }

    private static String displayName(String sdlName, String fallback, int id) {
        if (sdlName == null || sdlName.isEmpty()) {
            return fallback + " " + id;
        }
        return sdlName;
    }

    private static int toRumbleAmplitude(float amount) {
        return (int) (FastMath.clamp(amount, 0f, 1f) * 0xffff);
    }

    private static int toRumbleDurationMillis(float duration) {
        if (duration == Float.POSITIVE_INFINITY) {
            return 21 * 24 * 60 * 60 * 1000;
        } else if (duration <= 0f) {
            return 0;
        }
        return Math.max(1, (int) (duration * 1000f));
    }

    private static final class IosJoystick extends AbstractJoystick {
        private final Map<Integer, JoystickAxis> axes = new HashMap<>();
        private final Map<Integer, JoystickButton> buttons = new HashMap<>();
        private final boolean gamepad;
        private final long gamepadHandle;
        private final long joystickHandle;
        private JoystickAxis xAxis;
        private JoystickAxis yAxis;
        private JoystickAxis povXAxis;
        private boolean dpadUp;
        private boolean dpadDown;
        private boolean dpadLeft;
        private boolean dpadRight;
        private float povXValue;

        IosJoystick(InputManager inputManager, JoyInput joyInput, int id, boolean gamepad, long gamepadHandle,
                long joystickHandle, String name, int axisCount, int buttonCount) {
            super(inputManager, joyInput, id, name);
            this.gamepad = gamepad;
            this.gamepadHandle = gamepadHandle;
            this.joystickHandle = joystickHandle;

            if (gamepad) {
                addGamepadAxes(inputManager);
                addGamepadButtons(inputManager);
            } else {
                addRawAxes(inputManager, axisCount);
                addRawButtons(inputManager, buttonCount);
            }

            addPovAxes(inputManager);
        }

        void close() {
            if (gamepad) {
                SDL_CloseGamepad(gamepadHandle);
            } else {
                SDL_CloseJoystick(joystickHandle);
            }
        }

        void rumble(int lowFrequency, int highFrequency, int durationMs) {
            if (gamepad) {
                SDL_RumbleGamepad(gamepadHandle, lowFrequency, highFrequency, durationMs);
            } else {
                SDL_RumbleJoystick(joystickHandle, lowFrequency, highFrequency, durationMs);
            }
        }

        private void addGamepadAxes(InputManager inputManager) {
            for (int axisIndex = 0; axisIndex < SDL_GAMEPAD_AXIS_COUNT; axisIndex++) {
                String logicalId = remapAxisToJme(axisIndex);
                if (logicalId == null) {
                    continue;
                }

                JoystickAxis axis = new DefaultJoystickAxis(inputManager, this, axisIndex, getAxisLabel(axisIndex),
                        logicalId, true, false, 0f);
                addAxis(axisIndex, axis);
            }
        }

        private void addRawAxes(InputManager inputManager, int axisCount) {
            for (int axisIndex = 0; axisIndex < axisCount; axisIndex++) {
                String logicalId = String.valueOf(axisIndex);
                JoystickAxis axis = new DefaultJoystickAxis(inputManager, this, axisIndex, logicalId,
                        logicalId, true, false, 0f);
                addAxis(axisIndex, axis);
            }
        }

        private void addGamepadButtons(InputManager inputManager) {
            for (int buttonIndex = 0; buttonIndex < SDL_GAMEPAD_BUTTON_COUNT; buttonIndex++) {
                String logicalId = remapButtonToJme(buttonIndex);
                if (logicalId == null) {
                    continue;
                }

                JoystickButton button = new DefaultJoystickButton(inputManager, this, buttonIndex,
                        getButtonLabel(buttonIndex), logicalId);
                addButton(button);
            }
        }

        private void addRawButtons(InputManager inputManager, int buttonCount) {
            for (int buttonIndex = 0; buttonIndex < buttonCount; buttonIndex++) {
                String logicalId = String.valueOf(buttonIndex);
                JoystickButton button = new DefaultJoystickButton(inputManager, this, buttonIndex,
                        logicalId, logicalId);
                addButton(button);
            }
        }

        private void addPovAxes(InputManager inputManager) {
            JoystickAxis povX = new DefaultJoystickAxis(inputManager, this, POV_X_AXIS_ID, JoystickAxis.POV_X,
                    JoystickAxis.POV_X, true, false, 0f);
            addAxis(POV_X_AXIS_ID, povX);
        }

        JoystickAxis getAxisById(int id) {
            return axes.get(id);
        }

        JoystickButton getButtonById(int id) {
            return buttons.get(id);
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
            return povXAxis;
        }

        @Override
        public JoystickAxis getPovYAxis() {
            return null;
        }

        private void addAxis(int index, JoystickAxis axis) {
            axes.put(index, axis);
            super.addAxis(axis);
            switch (index) {
                case SDL_GAMEPAD_AXIS_LEFTX:
                    xAxis = axis;
                    break;
                case SDL_GAMEPAD_AXIS_LEFTY:
                    yAxis = axis;
                    break;
                case POV_X_AXIS_ID:
                    povXAxis = axis;
                    break;
                default:
                    break;
            }
        }

        private void updateDpadPov(int buttonId, boolean pressed, RawInputListener listener) {
            if (!gamepad) {
                return;
            }
            switch (buttonId) {
                case SDL_GAMEPAD_BUTTON_DPAD_UP:
                    dpadUp = pressed;
                    break;
                case SDL_GAMEPAD_BUTTON_DPAD_DOWN:
                    dpadDown = pressed;
                    break;
                case SDL_GAMEPAD_BUTTON_DPAD_LEFT:
                    dpadLeft = pressed;
                    break;
                case SDL_GAMEPAD_BUTTON_DPAD_RIGHT:
                    dpadRight = pressed;
                    break;
                default:
                    return;
            }

            float nextPovX = (dpadRight ? 1f : 0f) + (dpadLeft ? -1f : 0f);
            if (listener != null && povXAxis != null && povXValue != nextPovX) {
                listener.onJoyAxisEvent(new JoyAxisEvent(povXAxis, nextPovX, nextPovX));
            }
            povXValue = nextPovX;
        }

        protected void addButton(JoystickButton button) {
            buttons.put(button.getButtonId(), button);
            super.addButton(button);
        }

        private String getAxisLabel(int sdlAxisIndex) {
            switch (sdlAxisIndex) {
                case SDL_GAMEPAD_AXIS_LEFTX:
                    return "LEFT THUMB STICK (X)";
                case SDL_GAMEPAD_AXIS_LEFTY:
                    return "LEFT THUMB STICK (Y)";
                case SDL_GAMEPAD_AXIS_RIGHTX:
                    return "RIGHT THUMB STICK (X)";
                case SDL_GAMEPAD_AXIS_RIGHTY:
                    return "RIGHT THUMB STICK (Y)";
                case SDL_GAMEPAD_AXIS_LEFT_TRIGGER:
                    return "LEFT TRIGGER";
                case SDL_GAMEPAD_AXIS_RIGHT_TRIGGER:
                    return "RIGHT TRIGGER";
                default:
                    return "" + sdlAxisIndex;
            }
        }

        private String getButtonLabel(int sdlButtonIndex) {
            int label = SDL_GetGamepadButtonLabel(gamepadHandle, sdlButtonIndex);
            switch (label) {
                case SDL_GAMEPAD_BUTTON_LABEL_A:
                    return "A";
                case SDL_GAMEPAD_BUTTON_LABEL_B:
                    return "B";
                case SDL_GAMEPAD_BUTTON_LABEL_X:
                    return "X";
                case SDL_GAMEPAD_BUTTON_LABEL_Y:
                    return "Y";
                case SDL_GAMEPAD_BUTTON_LABEL_CROSS:
                    return "CROSS";
                case SDL_GAMEPAD_BUTTON_LABEL_CIRCLE:
                    return "CIRCLE";
                case SDL_GAMEPAD_BUTTON_LABEL_SQUARE:
                    return "SQUARE";
                case SDL_GAMEPAD_BUTTON_LABEL_TRIANGLE:
                    return "TRIANGLE";
                default:
                    return getButtonFallbackLabel(sdlButtonIndex);
            }
        }

        private String getButtonFallbackLabel(int sdlButtonIndex) {
            switch (sdlButtonIndex) {
                case SDL_GAMEPAD_BUTTON_NORTH:
                    return "Y";
                case SDL_GAMEPAD_BUTTON_EAST:
                    return "B";
                case SDL_GAMEPAD_BUTTON_SOUTH:
                    return "A";
                case SDL_GAMEPAD_BUTTON_WEST:
                    return "X";
                case SDL_GAMEPAD_BUTTON_LEFT_SHOULDER:
                    return "LEFT SHOULDER";
                case SDL_GAMEPAD_BUTTON_RIGHT_SHOULDER:
                    return "RIGHT SHOULDER";
                case SDL_GAMEPAD_BUTTON_BACK:
                    return "BACK";
                case SDL_GAMEPAD_BUTTON_START:
                    return "START";
                case SDL_GAMEPAD_BUTTON_LEFT_STICK:
                    return "LEFT STICK";
                case SDL_GAMEPAD_BUTTON_RIGHT_STICK:
                    return "RIGHT STICK";
                case SDL_GAMEPAD_BUTTON_DPAD_UP:
                    return "D-PAD UP";
                case SDL_GAMEPAD_BUTTON_DPAD_DOWN:
                    return "D-PAD DOWN";
                case SDL_GAMEPAD_BUTTON_DPAD_LEFT:
                    return "D-PAD LEFT";
                case SDL_GAMEPAD_BUTTON_DPAD_RIGHT:
                    return "D-PAD RIGHT";
                default:
                    return "" + sdlButtonIndex;
            }
        }

        private String remapAxisToJme(int sdlAxisIndex) {
            switch (sdlAxisIndex) {
                case SDL_GAMEPAD_AXIS_LEFTX:
                    return JoystickAxis.AXIS_XBOX_LEFT_THUMB_STICK_X;
                case SDL_GAMEPAD_AXIS_LEFTY:
                    return JoystickAxis.AXIS_XBOX_LEFT_THUMB_STICK_Y;
                case SDL_GAMEPAD_AXIS_RIGHTX:
                    return JoystickAxis.AXIS_XBOX_RIGHT_THUMB_STICK_X;
                case SDL_GAMEPAD_AXIS_RIGHTY:
                    return JoystickAxis.AXIS_XBOX_RIGHT_THUMB_STICK_Y;
                case SDL_GAMEPAD_AXIS_LEFT_TRIGGER:
                    return JoystickAxis.AXIS_XBOX_LEFT_TRIGGER;
                case SDL_GAMEPAD_AXIS_RIGHT_TRIGGER:
                    return JoystickAxis.AXIS_XBOX_RIGHT_TRIGGER;
                default:
                    return null;
            }
        }

        private String remapButtonToJme(int sdlButtonIndex) {
            switch (sdlButtonIndex) {
                case SDL_GAMEPAD_BUTTON_NORTH:
                    return JoystickButton.BUTTON_XBOX_Y;
                case SDL_GAMEPAD_BUTTON_EAST:
                    return JoystickButton.BUTTON_XBOX_B;
                case SDL_GAMEPAD_BUTTON_SOUTH:
                    return JoystickButton.BUTTON_XBOX_A;
                case SDL_GAMEPAD_BUTTON_WEST:
                    return JoystickButton.BUTTON_XBOX_X;
                case SDL_GAMEPAD_BUTTON_LEFT_SHOULDER:
                    return JoystickButton.BUTTON_XBOX_LB;
                case SDL_GAMEPAD_BUTTON_RIGHT_SHOULDER:
                    return JoystickButton.BUTTON_XBOX_RB;
                case SDL_GAMEPAD_BUTTON_BACK:
                    return JoystickButton.BUTTON_XBOX_BACK;
                case SDL_GAMEPAD_BUTTON_START:
                    return JoystickButton.BUTTON_XBOX_START;
                case SDL_GAMEPAD_BUTTON_LEFT_STICK:
                    return JoystickButton.BUTTON_XBOX_L3;
                case SDL_GAMEPAD_BUTTON_RIGHT_STICK:
                    return JoystickButton.BUTTON_XBOX_R3;
                case SDL_GAMEPAD_BUTTON_DPAD_UP:
                    return JoystickButton.BUTTON_XBOX_DPAD_UP;
                case SDL_GAMEPAD_BUTTON_DPAD_DOWN:
                    return JoystickButton.BUTTON_XBOX_DPAD_DOWN;
                case SDL_GAMEPAD_BUTTON_DPAD_LEFT:
                    return JoystickButton.BUTTON_XBOX_DPAD_LEFT;
                case SDL_GAMEPAD_BUTTON_DPAD_RIGHT:
                    return JoystickButton.BUTTON_XBOX_DPAD_RIGHT;
                default:
                    return null;
            }
        }
    }
}

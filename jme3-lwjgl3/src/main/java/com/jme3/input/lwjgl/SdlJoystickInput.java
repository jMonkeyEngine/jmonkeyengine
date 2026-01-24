package com.jme3.input.lwjgl;

import com.jme3.input.*;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.math.FastMath;
import com.jme3.system.AppSettings;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lwjgl.sdl.*;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.sdl.SDLInit.*;
import static org.lwjgl.sdl.SDLEvents.*;
import static org.lwjgl.sdl.SDLGamepad.*;
import static org.lwjgl.sdl.SDLJoystick.*;
import static org.lwjgl.sdl.SDLError.*;
import static org.lwjgl.sdl.SDLTimer.*;

/**
 * The SDL based implementation of {@link JoyInput}.
 *
 * @author Riccardo Balbo
 */
public class SdlJoystickInput implements JoyInput {

    private static final Logger LOGGER = Logger.getLogger(SdlJoystickInput.class.getName());
    private static final int POV_X_AXIS_ID = 7;
    private static final int POV_Y_AXIS_ID = 8;

    private final AppSettings settings;
    private final Map<Integer, SdlJoystick> joysticks = new HashMap<>();
    private final Map<JoystickButton, Boolean> joyButtonPressed = new HashMap<>();
    private final Map<JoystickAxis, Float> joyAxisValues = new HashMap<>();
    private final int flags = SDL_INIT_GAMEPAD | SDL_INIT_JOYSTICK | SDL_INIT_HAPTIC | SDL_INIT_EVENTS;

    private boolean initialized;
    private float virtualTriggerThreshold;
    private float globalJitterThreshold;
    private boolean loadGamepads;
    private boolean loadRaw;

    private RawInputListener listener;

    public SdlJoystickInput(AppSettings settings) {
        this.settings = settings;
        try {
            String path = settings.getSDLGameControllerDBResourcePath();
            if (!path.isBlank()) {
                ByteBuffer bbf = SdlGameControllerDb.getGamecontrollerDb(path);
                if (SDL_AddGamepadMapping(bbf) == -1) {
                    throw new Exception("Failed to load");
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Unable to load gamecontrollerdb, fallback to sdl default mappings", e);
        }
    }

    @Override
    public void initialize() {
        if (!SDL_InitSubSystem(SDL_INIT_GAMEPAD | SDL_INIT_JOYSTICK | SDL_INIT_HAPTIC | SDL_INIT_EVENTS)) {
            String err = SDL_GetError();
            throw new IllegalStateException("SDL_InitSubSystem failed: " + err);
        }
        virtualTriggerThreshold = settings.getJoysticksTriggerToButtonThreshold();
        globalJitterThreshold = settings.getJoysticksAxisJitterThreshold();

        String mapper = settings.getJoysticksMapper();
        switch (mapper) {
            case AppSettings.JOYSTICKS_RAW_MAPPER:
                loadGamepads = false;
                loadRaw = true;
                break;
            case AppSettings.JOYSTICKS_XBOX_MAPPER:
                loadGamepads = true;
                loadRaw = false;
                break;
            default:
            case AppSettings.JOYSTICKS_XBOX_WITH_FALLBACK_MAPPER:
                loadGamepads = true;
                loadRaw = true;
                break;
        }

        initialized = true;
    }

    private void onDeviceConnected(int deviceIndex, boolean isGamepad) {
        if (loadGamepads && !isGamepad && SDL_IsGamepad(deviceIndex)) {
            // SDL will fire both GAMEPAD and JOYSTICK events for recognized
            // gamepads, so here we check if the joystick is expected to be
            // a gamepad, and skip it if so to avoid duplicates.
            return;
        }

        InputManager inputManager = (InputManager) listener;

        SdlJoystick joystick;
        if (isGamepad) {
            long gamepad = SDL_OpenGamepad(deviceIndex);
            if (gamepad == 0L) {
                LOGGER.log(Level.FINE, "SDL failed to open gamepad for id {0}: {1}",
                        new Object[] { deviceIndex, SDL_GetError() });
                return;
            }

            String name = SDL_GetGamepadName(gamepad);

            joystick = new SdlJoystick(inputManager, this, deviceIndex, name, gamepad, 0L);
            joysticks.put(deviceIndex, joystick);

            // Managed axes (standard layout)
            for (int axisIndex = 0; axisIndex < SDL_GAMEPAD_AXIS_COUNT; axisIndex++) {
                String logicalId = remapAxisToJme(axisIndex);
                if (logicalId == null) continue;

                String axisName = getAxisLabel(joystick, axisIndex);

                JoystickAxis axis = new DefaultJoystickAxis(inputManager, joystick, axisIndex, axisName,
                        logicalId, true, false, 0.0f);
                joystick.addAxis(axisIndex, axis);
            }

            // Managed buttons: map SDL gamepad buttons into your JME logical ids
            for (int buttonIndex = 0; buttonIndex < SDL_GAMEPAD_BUTTON_COUNT; buttonIndex++) {
                String logicalId = remapButtonToJme(buttonIndex);
                if (logicalId == null) continue;
                String buttonName = getButtonLabel(joystick, buttonIndex);

                JoystickButton button = new DefaultJoystickButton(inputManager, joystick, buttonIndex,
                        buttonName, logicalId);
                joystick.addButton(button);
                joyButtonPressed.put(button, false);
            }

        } else {
            long joy = SDL_OpenJoystick(deviceIndex);
            if (joy == 0L) return;

            String name = SDL_GetJoystickName(joy);
            joystick = new SdlJoystick(inputManager, this, deviceIndex, name, 0L, joy);
            joysticks.put(deviceIndex, joystick);

            int numAxes = SDL_GetNumJoystickAxes(joy);
            int numButtons = SDL_GetNumJoystickButtons(joy);

            for (int axisIndex = 0; axisIndex < numAxes; axisIndex++) {
                String logicalId = String.valueOf(axisIndex);
                String axisName = logicalId;

                JoystickAxis axis = new DefaultJoystickAxis(inputManager, joystick, axisIndex, axisName,
                        logicalId, true, false, 0.0f);

                joystick.addAxis(axisIndex, axis);
            }

            for (int buttonIndex = 0; buttonIndex < numButtons; buttonIndex++) {
                String logicalId = String.valueOf(buttonIndex);
                String buttonName = logicalId;
                JoystickButton button = new DefaultJoystickButton(inputManager, joystick, buttonIndex,
                        buttonName, logicalId);
                joystick.addButton(button);
            }
        }

        // Virtual POV axes for D-pad.
        JoystickAxis povX = new DefaultJoystickAxis(inputManager, joystick, POV_X_AXIS_ID, JoystickAxis.POV_X,
                JoystickAxis.POV_X, true, false, 0.0f);
        joystick.addAxis(POV_X_AXIS_ID, povX);

        JoystickAxis povY = new DefaultJoystickAxis(inputManager, joystick, POV_Y_AXIS_ID, JoystickAxis.POV_Y,
                JoystickAxis.POV_Y, true, false, 0.0f);
        joystick.addAxis(POV_Y_AXIS_ID, povY);

        ((InputManager) listener).fireJoystickConnectedEvent(joystick);

    }

    private void destroyJoystick(SdlJoystick joystick) {
        if (joystick.isGamepad()) {
            if (joystick.gamepad != 0L) {
                SDL_CloseGamepad(joystick.gamepad);
            }
        } else {
            if (joystick.joystick != 0L) {
                SDL_CloseJoystick(joystick.joystick);
            }
        }
    }

    private void onDeviceDisconnected(int deviceIndex) {
        SdlJoystick joystick = joysticks.get(deviceIndex);
        if (joystick == null) return;

        // clear all states associated with this joystick
        joyButtonPressed.entrySet().removeIf(e -> e.getKey().getJoystick() == joystick);
        joyAxisValues.entrySet().removeIf(e -> e.getKey().getJoystick() == joystick);
        joysticks.remove(deviceIndex);

        // free resources
        destroyJoystick(joystick);

        ((InputManager) listener).fireJoystickDisconnectedEvent(joystick);
    }

    @Override
    public Joystick[] loadJoysticks(InputManager inputManager) {

        for (SdlJoystick js : joysticks.values()) destroyJoystick(js);
        joysticks.clear();

        joyButtonPressed.clear();
        joyAxisValues.clear();

        if (loadGamepads) {
            // load managed gamepads
            IntBuffer gamepads = SDL_GetGamepads();
            if (gamepads != null) {
                while (gamepads.hasRemaining()) {
                    int deviceId = gamepads.get();
                    onDeviceConnected(deviceId, true);
                }
            }
        }

        if (loadRaw) {
            // load raw gamepads
            IntBuffer joys = SDL_GetJoysticks();
            if (joys != null) {
                while (joys.hasRemaining()) {
                    int deviceId = joys.get();
                    onDeviceConnected(deviceId, false);
                }
            }
        }

        return joysticks.values().toArray(new Joystick[0]);
    }

    @Override
    public void update() {
        handleConnectionEvents();
        handleInputEvents();
    }

    private void handleInputEvents() {
        float rawValue, value;
        for (SdlJoystick js : joysticks.values()) {
            if (js.isGamepad()) {
                long gp = js.gamepad;

                // for(int axisIndex=0; axisIndex<SDL_GAMEPAD_AXIS_COUNT; axisIndex++){
                for (JoystickAxis axis : js.getAxes()) {
                    int axisIndex = axis.getAxisId();
                    String jmeAxisId = axis.getLogicalId();

                    short v = SDL_GetGamepadAxis(gp, axisIndex);

                    rawValue = remapAxisValueToJme(axisIndex, v);
                    value = rawValue; // SDL handles scaling
                    updateAxis(axis, value, rawValue);

                    // Virtual trigger buttons (same idea as your GLFW code)
                    if (virtualTriggerThreshold > 0f) {
                        if (jmeAxisId == JoystickAxis.AXIS_XBOX_LEFT_TRIGGER) {
                            updateButton(js.getButton(JoystickButton.BUTTON_XBOX_LT),
                                    value > virtualTriggerThreshold);
                        } else if (jmeAxisId == JoystickAxis.AXIS_XBOX_RIGHT_TRIGGER) {
                            updateButton(js.getButton(JoystickButton.BUTTON_XBOX_RT),
                                    value > virtualTriggerThreshold);
                        }
                    }

                    // Dpad -> virtual POV axes
                    float povXValue = 0f;
                    float povYValue = 0f;

                    // button handling
                    // for (int b = 0; b <= SDL_GAMEPAD_BUTTON_COUNT; b++) {
                    for (JoystickButton button : js.getButtons()) {
                        int b = button.getButtonId();
                        String jmeButtonId = button.getLogicalId();

                        boolean pressed = SDL_GetGamepadButton(gp, b);
                        updateButton(button, pressed);

                        // Dpad -> virtual POV axes
                        if (jmeButtonId == JoystickButton.BUTTON_XBOX_DPAD_UP) {
                            povYValue += pressed ? 1f : 0f;
                        } else if (jmeButtonId == JoystickButton.BUTTON_XBOX_DPAD_DOWN) {
                            povYValue += pressed ? -1f : 0f;
                        } else if (jmeButtonId == JoystickButton.BUTTON_XBOX_DPAD_LEFT) {
                            povXValue += pressed ? -1f : 0f;
                        } else if (jmeButtonId == JoystickButton.BUTTON_XBOX_DPAD_RIGHT) {
                            povXValue += pressed ? 1f : 0f;
                        }
                    }

                    JoystickAxis povXAxis = js.getPovXAxis();
                    if (povXAxis != null) {
                        updateAxis(povXAxis, povXValue, povXValue);
                    }

                    JoystickAxis povYAxis = js.getPovYAxis();
                    if (povYAxis != null) {
                        updateAxis(povYAxis, povYValue, povYValue);
                    }
                }
            } else {
                long joy = js.joystick;

                for (JoystickAxis axis : js.getAxes()) {
                    short v = SDL_GetJoystickAxis(joy, axis.getAxisId());
                    rawValue = v;
                    value = v;
                    updateAxis(axis, value, rawValue);
                }

                for (JoystickButton button : js.getButtons()) {
                    boolean pressed = SDL_GetJoystickButton(joy, button.getButtonId());
                    updateButton(button, pressed);
                }
            }
        }
    }

    private void handleConnectionEvents() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            SDL_Event evt = SDL_Event.malloc(stack);
            while (SDL_PollEvent(evt)) {
                int type = evt.type();
                if (type == SDL_EVENT_GAMEPAD_ADDED) {
                    if (loadGamepads) {
                        int which = evt.gdevice().which();
                        onDeviceConnected(which, true);
                    }
                } else if (type == SDL_EVENT_GAMEPAD_REMOVED) {
                    int which = evt.gdevice().which();
                    onDeviceDisconnected(which);
                } else if (type == SDL_EVENT_JOYSTICK_ADDED) {
                    if (loadRaw) {
                        int which = evt.jdevice().which();
                        onDeviceConnected(which, false);
                    }
                } else if (type == SDL_EVENT_JOYSTICK_REMOVED) {
                    int which = evt.jdevice().which();
                    onDeviceDisconnected(which);
                }
            }
        }
    }

    @Override
    public void setJoyRumble(int joyId, float amount) {
        setJoyRumble(joyId, amount, amount, 100f / 1000f);
    }

    public void setJoyRumble(int joyId, float highFrequency, float lowFrequency, float duration) {
        SdlJoystick js = joysticks.get(joyId);
        if (js == null) return;

        highFrequency = FastMath.clamp(highFrequency, 0f, 1f);
        lowFrequency = FastMath.clamp(lowFrequency, 0f, 1f);

        if (js.isGamepad() && js.gamepad != 0L) {
            int ampHigh = (int) (highFrequency * 0xFFFF);
            int ampLow = (int) (lowFrequency * 0xFFFF);
            int durationMs = (int) (duration * 1000f);
            SDL_RumbleGamepad(js.gamepad, (short) ampHigh, (short) ampLow, durationMs);
        }
    }

    private String getButtonLabel(SdlJoystick gamepad, int sdlButtonIndex) {
        int label = SDL_GetGamepadButtonLabel(gamepad.gamepad, sdlButtonIndex);
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

            case SDL_GAMEPAD_BUTTON_LABEL_UNKNOWN:
            default:
                return "" + sdlButtonIndex;
        }
    }

    private String getAxisLabel(SdlJoystick gamepad, int sdlAxisIndex) {
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

    private float remapAxisValueToJme(int axisId, short v) {
        if (axisId == SDL_GAMEPAD_AXIS_LEFT_TRIGGER || axisId == SDL_GAMEPAD_AXIS_RIGHT_TRIGGER) {
            // [0..32767] -> [0..1]
            if (v <= 0) return 0f;
            return Math.min(1f, v / 32767f);
        } else {
            // [-32768..32767] -> [-1..1]
            if (v == Short.MIN_VALUE) return -1f;
            return v / 32767f;
        }
    }

    private void updateButton(JoystickButton button, boolean pressed) {
        if (button == null) return;
        Boolean old = joyButtonPressed.get(button);
        if (old == null || old != pressed) {
            joyButtonPressed.put(button, pressed);
            listener.onJoyButtonEvent(new JoyButtonEvent(button, pressed));
        }
    }

    private void updateAxis(JoystickAxis axis, float value, float rawValue) {
        if (axis == null) return;
        Float old = joyAxisValues.get(axis);
        float jitter = FastMath.clamp(Math.max(axis.getJitterThreshold(), globalJitterThreshold), 0f, 1f);
        if (old == null || FastMath.abs(old - value) > jitter) {
            joyAxisValues.put(axis, value);
            listener.onJoyAxisEvent(new JoyAxisEvent(axis, value, rawValue));
        }
    }

    @Override
    public void destroy() {
        // Close devices
        for (SdlJoystick js : joysticks.values()) {
            if (js.gamepad != 0L) SDL_CloseGamepad(js.gamepad);
            if (js.joystick != 0L) SDL_CloseJoystick(js.joystick);
        }
        joysticks.clear();

        // Quit subsystems
        SDL_QuitSubSystem(flags);

        initialized = false;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void setInputListener(final RawInputListener listener) {
        this.listener = listener;
    }

    @Override
    public long getInputTimeNanos() {
        return SDL_GetTicksNS();
    }

    private static class SdlJoystick extends AbstractJoystick {

        private JoystickAxis xAxis;
        private JoystickAxis yAxis;
        private JoystickAxis povAxisX;
        private JoystickAxis povAxisY;

        long gamepad;
        long joystick;

        SdlJoystick(InputManager inputManager, JoyInput joyInput, int joyId, String name, long gamepad,
                long joystick) {
            super(inputManager, joyInput, joyId, name);
            this.gamepad = gamepad;
            this.joystick = joystick;

        }

        boolean isGamepad() {
            return gamepad != 0L;
        }

        void addAxis(int index, JoystickAxis axis) {
            super.addAxis(axis);
            switch (index) {
                case SDL_GAMEPAD_AXIS_LEFTX: {
                    xAxis = axis;
                    break;
                }
                case SDL_GAMEPAD_AXIS_LEFTY: {
                    yAxis = axis;
                    break;
                }
                case POV_X_AXIS_ID: {
                    povAxisX = axis;
                    break;
                }
                case POV_Y_AXIS_ID: {
                    povAxisY = axis;
                    break;
                }
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
            return povAxisX;
        }

        @Override
        public JoystickAxis getPovYAxis() {
            return povAxisY;
        }

        @Override
        public int getXAxisIndex() {
            return xAxis != null ? xAxis.getAxisId() : 0;
        }

        @Override
        public int getYAxisIndex() {
            return yAxis != null ? yAxis.getAxisId() : 1;
        }

        @Override
        public void addButton(JoystickButton button) {
            super.addButton(button);
        }

    }
}

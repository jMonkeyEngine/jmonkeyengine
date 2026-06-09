/*
 * Copyright (c) 2009-2026 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.input.virtual;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.AbstractJoystick;
import com.jme3.input.DefaultJoystickAxis;
import com.jme3.input.DefaultJoystickButton;
import com.jme3.input.InputManager;
import com.jme3.input.JoyInput;
import com.jme3.input.JoystickAxis;
import com.jme3.input.JoystickButton;
import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.TouchInput;
import com.jme3.input.controls.JoyAxisTrigger;
import com.jme3.input.controls.JoyButtonTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.TouchTrigger;
import com.jme3.input.event.InputEvent;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.virtual.VirtualJoystickLayout.Element;
import com.jme3.input.virtual.VirtualJoystickTheme.TextureKey;
import com.jme3.math.FastMath;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeSystem;
import com.jme3.ui.Picture;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

/**
 * A joystick implementation driven by on-screen controls.
 */
public class VirtualJoystick extends AbstractJoystick {

    private static final int AXIS_LEFT_X = 0;
    private static final int AXIS_LEFT_Y = 1;
    private static final int AXIS_RIGHT_X = 2;
    private static final int AXIS_RIGHT_Y = 3;
    private static final int AXIS_LEFT_TRIGGER = 4;
    private static final int AXIS_RIGHT_TRIGGER = 5;
    private static final int AXIS_POV_X_ID = 6;

    private static final String ROOT_NAME = "Virtual Joystick";

    private final Map<String, JoystickAxis> axesByLogicalId = new HashMap<>();
    private final Map<String, JoystickButton> buttonsByLogicalId = new HashMap<>();
    private final Map<Integer, Capture> captures = new HashMap<>();
    private ArrayDeque<InputEvent> events = new ArrayDeque<>();
    private ArrayDeque<InputEvent> readyEvents = new ArrayDeque<>();
    private final float[] axisValues = new float[7];
    private final boolean[] buttonValues = new boolean[16];
    private final Object inputLock = new Object();
    private final Element.BoundsSnapshot inputBounds = new Element.BoundsSnapshot();

    private JoystickAxis xAxis;
    private JoystickAxis yAxis;
    private JoystickAxis povXAxis;
    private volatile boolean enabled = true;
    private volatile int buttonStateMask;
    private volatile boolean hasEvents;
    private volatile VirtualJoystickTheme theme = new VirtualJoystickTheme();
    private volatile VirtualJoystickLayout layout = new VirtualJoystickDynamicLayout(true);
    private volatile int visualWidth;
    private volatile int visualHeight;
    private Node visualRoot;
    private Node visualParent;
    private BitmapFont font;

    public VirtualJoystick(InputManager inputManager, JoyInput joyInput, int joyId) {
        super(inputManager, joyInput, joyId, "Virtual Joystick");
        addAxes();
        addButtons(inputManager);
        releaseAllLocked(0L);
    }

    /**
     * Returns true if this joystick accepts pointer input.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Enables or disables pointer processing for this joystick.
     *
     * @param enabled true to accept pointer input
     */
    public void setEnabled(boolean enabled) {
        synchronized (inputLock) {
            this.enabled = enabled;
            if (!enabled) {
                releaseAllLocked(0L);
            }
        }
    }

    public VirtualJoystickTheme getTheme() {
        return theme;
    }

    public void setTheme(VirtualJoystickTheme theme) {
        this.theme = theme == null ? new VirtualJoystickTheme() : theme;
        this.theme.markUpdateNeeded();
    }

    public VirtualJoystickLayout getLayout() {
        return layout;
    }

    public void setLayout(VirtualJoystickLayout layout) {
        synchronized (inputLock) {
            releaseAllLocked(0L);
            this.layout = layout == null ? new VirtualJoystickDynamicLayout(true) : layout;
            this.layout.markUpdateNeeded();
        }
    }

    public static VirtualJoystickLayout createLayout(String layout) {
        if (AppSettings.VIRTUAL_JOYSTICK_LAYOUT_XBOX.equalsIgnoreCase(layout)) {
            return new VirtualJoystickXboxLayout();
        }
        return new VirtualJoystickDynamicLayout(true);
    }

    @Override
    public void rumble(float amountHigh, float amountLow, float duration) {
        if (JmeSystem.isDeviceRumbleSupported()) {
            JmeSystem.rumble(amountHigh, amountLow, duration);
        }
    }

    @Override
    public void stopRumble() {
        JmeSystem.stopRumble();
    }

    /**
     * Processes a pointer-down event.
     *
     * @return true if the pointer was captured by a virtual control
     */
    public boolean onPointerDown(int pointerId, float x, float y, long time) {
        synchronized (inputLock) {
            Capture existingCapture = captures.get(pointerId);
            if (!enabled || existingCapture != null) {
                return existingCapture != null;
            }

            for (Element element : layout.getAxisElements()) {
                if (element.visible && element.contains(x, y)) {
                    captures.put(pointerId, new Capture(element, true));
                    updateAxisCapture(element, x, y, time);
                    return true;
                }
            }

            for (Element element : layout.getButtons()) {
                if (element.visible && element.contains(x, y)) {
                    captures.put(pointerId, new Capture(element, false));
                    if (isToggleButton(element.id)) {
                        pressButton(element.id, !isButtonPressed(element.id), time);
                    } else {
                        pressButton(element.id, true, time);
                    }
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * Processes a pointer-move event.
     *
     * @return true if the pointer is captured by a virtual control
     */
    public boolean onPointerMove(int pointerId, float x, float y, long time) {
        synchronized (inputLock) {
            Capture capture = captures.get(pointerId);
            if (capture == null) {
                return false;
            }
            if (capture.axis) {
                updateAxisCapture(capture.element, x, y, time);
            }
            return true;
        }
    }

    /**
     * Processes a pointer-up event.
     *
     * @return true if the pointer was captured by a virtual control
     */
    public boolean onPointerUp(int pointerId, float x, float y, long time) {
        synchronized (inputLock) {
            Capture capture = captures.remove(pointerId);
            if (capture == null) {
                return false;
            }
            if (capture.axis) {
                centerAxisCapture(capture.element, time);
            } else if (!isToggleButton(capture.element.id)) {
                pressButton(capture.element.id, false, time);
            }
            return true;
        }
    }

    /**
     * Releases all active pointer captures.
     *
     * @return true if at least one pointer was captured
     */
    public boolean onPointerCancel(long time) {
        synchronized (inputLock) {
            boolean captured = !captures.isEmpty();
            releaseAllLocked(time);
            return captured;
        }
    }

    /**
     * Dispatches pending joystick events to the backend listener.
     *
     * @param listener listener that receives joystick events
     */
    public void dispatchEvents(RawInputListener listener) {
        if (!hasEvents) {
            return;
        }

        synchronized (inputLock) {
            if (!hasEvents) {
                return;
            }
            if (listener == null) {
                events.clear();
                hasEvents = false;
                return;
            }
            ArrayDeque<InputEvent> pendingEvents = events;
            events = readyEvents;
            readyEvents = pendingEvents;
            hasEvents = false;
        }

        InputEvent event;
        while ((event = readyEvents.poll()) != null) {
            if (event instanceof JoyAxisEvent) {
                listener.onJoyAxisEvent((JoyAxisEvent) event);
            } else if (event instanceof JoyButtonEvent) {
                listener.onJoyButtonEvent((JoyButtonEvent) event);
            }
        }
    }

    /**
     * Synchronizes GUI spatials with the current joystick state.
     *
     * @param parent GUI node that should contain the controls
     * @param assetManager asset manager used to load default textures
     * @param width GUI width in pixels
     * @param height GUI height in pixels
     * @param tpf time per frame
     */
    public void updateVisuals(Node parent, AssetManager assetManager, int width, int height, float tpf) {
        if (parent == null || assetManager == null || width <= 0 || height <= 0) {
            return;
        }
        if (visualRoot == null) {
            visualRoot = new Node(ROOT_NAME);
        }
        attachVisualRootOnTop(parent);
        if (width != visualWidth || height != visualHeight) {
            synchronized (inputLock) {
                if (width != visualWidth || height != visualHeight) {
                    visualWidth = width;
                    visualHeight = height;
                    releaseAllLocked(0L);
                }
            }
        }

        if (!enabled) {
            if (visualRoot.getQuantity() > 0) {
                visualRoot.detachAllChildren();
            }
            return;
        }

        VirtualJoystickTheme currentTheme = theme;
        VirtualJoystickLayout currentLayout = layout;
        currentLayout.update(this);
        boolean themeUpdateNeeded = currentTheme.isUpdateNeeded();
        if (themeUpdateNeeded || currentLayout.isUpdateNeeded()) {
            clearVisuals(currentLayout);
            if (themeUpdateNeeded) {
                font = null;
            }
            currentTheme.clearUpdateNeeded();
            currentLayout.clearUpdateNeeded();
        }
        String fontPath = currentTheme.getFontPath();
        if (font == null && fontPath != null) {
            font = assetManager.loadFont(fontPath);
        }

        float scale = currentLayout.getScale();

        for (Element element : currentLayout.getButtons()) {
            syncElement(element, assetManager, width, height, scale);
        }
        for (Element element : currentLayout.getAxisElements()) {
            syncElement(element, assetManager, width, height, scale);
        }
    }

    private void attachVisualRootOnTop(Node parent) {
        if (visualParent != parent || visualRoot.getParent() != parent) {
            visualRoot.removeFromParent();
            parent.attachChild(visualRoot);
            visualParent = parent;
            return;
        }

        int childIndex = parent.getChildIndex(visualRoot);
        int topIndex = parent.getQuantity() - 1;
        if (childIndex >= 0 && childIndex < topIndex) {
            visualRoot.removeFromParent();
            parent.attachChild(visualRoot);
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
        return povXAxis;
    }

    @Override
    public JoystickAxis getPovYAxis() {
        return null;
    }

    public boolean hasInputBindings() {
        for (JoystickAxis axis : getAxes()) {
            if (isAxisBound(axis.getLogicalId())) {
                return true;
            }
        }
        for (JoystickButton button : getButtons()) {
            if (isButtonBound(button.getLogicalId())) {
                return true;
            }
        }
        return false;
    }

    public boolean isAxisBound(String logicalId) {
        JoystickAxis axis = axesByLogicalId.get(logicalId);
        if (axis == null) {
            return false;
        }
        InputManager manager = getInputManager();
        return manager != null
                && (manager.hasTriggerMapping(JoyAxisTrigger.joyAxisHash(getJoyId(), axis.getAxisId(), false))
                || manager.hasTriggerMapping(JoyAxisTrigger.joyAxisHash(getJoyId(), axis.getAxisId(), true)));
    }

    public boolean isButtonBound(String logicalId) {
        JoystickButton button = buttonsByLogicalId.get(logicalId);
        if (button == null) {
            return false;
        }
        InputManager manager = getInputManager();
        return manager != null
                && manager.hasTriggerMapping(JoyButtonTrigger.joyButtonHash(getJoyId(), button.getButtonId()));
    }

    public boolean hasPointerLookBindings() {
        InputManager manager = getInputManager();
        if (manager == null) {
            return false;
        }
        boolean mouseLook = hasMouseAxisBinding(manager, MouseInput.AXIS_X)
                && hasMouseAxisBinding(manager, MouseInput.AXIS_Y);
        boolean touchLook = manager.hasTriggerMapping(TouchTrigger.touchHash(TouchInput.ALL));
        return touchLook || (mouseLook && manager.isSimulateMouse());
    }

    private boolean hasMouseAxisBinding(InputManager manager, int axis) {
        return manager.hasTriggerMapping(MouseAxisTrigger.mouseAxisHash(axis, false))
                || manager.hasTriggerMapping(MouseAxisTrigger.mouseAxisHash(axis, true));
    }

    private void addAxes() {
        xAxis = addAxis(AXIS_LEFT_X, "LEFT THUMB STICK (X)", JoystickAxis.AXIS_XBOX_LEFT_THUMB_STICK_X);
        yAxis = addAxis(AXIS_LEFT_Y, "LEFT THUMB STICK (Y)", JoystickAxis.AXIS_XBOX_LEFT_THUMB_STICK_Y);
        addAxis(AXIS_RIGHT_X, "RIGHT THUMB STICK (X)", JoystickAxis.AXIS_XBOX_RIGHT_THUMB_STICK_X);
        addAxis(AXIS_RIGHT_Y, "RIGHT THUMB STICK (Y)", JoystickAxis.AXIS_XBOX_RIGHT_THUMB_STICK_Y);
        addAxis(AXIS_LEFT_TRIGGER, "LEFT TRIGGER", JoystickAxis.AXIS_XBOX_LEFT_TRIGGER);
        addAxis(AXIS_RIGHT_TRIGGER, "RIGHT TRIGGER", JoystickAxis.AXIS_XBOX_RIGHT_TRIGGER);
        povXAxis = addAxis(AXIS_POV_X_ID, JoystickAxis.POV_X, JoystickAxis.POV_X);
    }

    private JoystickAxis addAxis(int id, String name, String logicalId) {
        JoystickAxis axis = new DefaultJoystickAxis(getInputManager(), this, id, name, logicalId, true, false, 0f);
        axesByLogicalId.put(logicalId, axis);
        super.addAxis(axis);
        return axis;
    }

    private void addButtons(InputManager inputManager) {
        addButton(inputManager, 0, "A", JoystickButton.BUTTON_XBOX_A);
        addButton(inputManager, 1, "B", JoystickButton.BUTTON_XBOX_B);
        addButton(inputManager, 2, "X", JoystickButton.BUTTON_XBOX_X);
        addButton(inputManager, 3, "Y", JoystickButton.BUTTON_XBOX_Y);
        addButton(inputManager, 4, "LB", JoystickButton.BUTTON_XBOX_LB);
        addButton(inputManager, 5, "RB", JoystickButton.BUTTON_XBOX_RB);
        addButton(inputManager, 6, "LT", JoystickButton.BUTTON_XBOX_LT);
        addButton(inputManager, 7, "RT", JoystickButton.BUTTON_XBOX_RT);
        addButton(inputManager, 8, "BACK", JoystickButton.BUTTON_XBOX_BACK);
        addButton(inputManager, 9, "START", JoystickButton.BUTTON_XBOX_START);
        addButton(inputManager, 10, "L3", JoystickButton.BUTTON_XBOX_L3);
        addButton(inputManager, 11, "R3", JoystickButton.BUTTON_XBOX_R3);
        addButton(inputManager, 12, "D-PAD UP", JoystickButton.BUTTON_XBOX_DPAD_UP);
        addButton(inputManager, 13, "D-PAD DOWN", JoystickButton.BUTTON_XBOX_DPAD_DOWN);
        addButton(inputManager, 14, "D-PAD LEFT", JoystickButton.BUTTON_XBOX_DPAD_LEFT);
        addButton(inputManager, 15, "D-PAD RIGHT", JoystickButton.BUTTON_XBOX_DPAD_RIGHT);
    }

    private void addButton(InputManager inputManager, int id, String name, String logicalId) {
        JoystickButton button = new DefaultJoystickButton(inputManager, this, id, name, logicalId);
        buttonsByLogicalId.put(logicalId, button);
        super.addButton(button);
    }

    private void updateAxisCapture(Element element, float x, float y, long time) {
        element.copyBoundsTo(inputBounds);
        float radius = inputBounds.size * 0.5f;
        if (radius <= 0f) {
            return;
        }
        float dx = x - inputBounds.x;
        float dy = y - inputBounds.y;
        float length = FastMath.sqrt(dx * dx + dy * dy);
        if (length > radius && length > 0f) {
            dx *= radius / length;
            dy *= radius / length;
        }

        float valueX = FastMath.clamp(dx / radius, -1f, 1f);
        float valueY = FastMath.clamp(dy / radius, -1f, 1f);
        element.nubX = valueX;
        element.nubY = valueY;

        setAxisValue(element.id, valueX, time);
        setAxisValue(element.yAxisLogicalId, -valueY, time);
    }

    private void centerAxisCapture(Element element, long time) {
        element.nubX = 0f;
        element.nubY = 0f;
        setAxisValue(element.id, 0f, time);
        setAxisValue(element.yAxisLogicalId, 0f, time);
    }

    private void pressButton(String logicalId, boolean pressed, long time) {
        JoystickButton button = buttonsByLogicalId.get(logicalId);
        if (button == null) {
            return;
        }
        int buttonId = button.getButtonId();
        if (buttonValues[buttonId] == pressed) {
            return;
        }
        buttonValues[buttonId] = pressed;
        if (pressed) {
            buttonStateMask |= 1 << buttonId;
        } else {
            buttonStateMask &= ~(1 << buttonId);
        }
        JoyButtonEvent event = new JoyButtonEvent(button, pressed);
        event.setTime(time);
        enqueueEvent(event);

        if (JoystickButton.BUTTON_XBOX_LT.equals(logicalId)) {
            setAxisValue(JoystickAxis.AXIS_XBOX_LEFT_TRIGGER, pressed ? 1f : 0f, time);
        } else if (JoystickButton.BUTTON_XBOX_RT.equals(logicalId)) {
            setAxisValue(JoystickAxis.AXIS_XBOX_RIGHT_TRIGGER, pressed ? 1f : 0f, time);
        } else if (isDpad(logicalId)) {
            updatePovXAxis(time);
        }
    }

    private boolean isButtonPressed(String logicalId) {
        JoystickButton button = buttonsByLogicalId.get(logicalId);
        return button != null && buttonValues[button.getButtonId()];
    }

    private boolean isToggleButton(String logicalId) {
        return JoystickButton.BUTTON_XBOX_L3.equals(logicalId)
                || JoystickButton.BUTTON_XBOX_R3.equals(logicalId);
    }

    private boolean isDpad(String logicalId) {
        return JoystickButton.BUTTON_XBOX_DPAD_UP.equals(logicalId)
                || JoystickButton.BUTTON_XBOX_DPAD_DOWN.equals(logicalId)
                || JoystickButton.BUTTON_XBOX_DPAD_LEFT.equals(logicalId)
                || JoystickButton.BUTTON_XBOX_DPAD_RIGHT.equals(logicalId);
    }

    private void updatePovXAxis(long time) {
        float x = 0f;
        if (buttonValues[buttonsByLogicalId.get(JoystickButton.BUTTON_XBOX_DPAD_LEFT).getButtonId()]) {
            x -= 1f;
        }
        if (buttonValues[buttonsByLogicalId.get(JoystickButton.BUTTON_XBOX_DPAD_RIGHT).getButtonId()]) {
            x += 1f;
        }
        setAxisValue(JoystickAxis.POV_X, x, time);
    }

    private void setAxisValue(String logicalId, float value, long time) {
        JoystickAxis axis = axesByLogicalId.get(logicalId);
        if (axis == null) {
            return;
        }
        value = FastMath.clamp(value, -1f, 1f);
        int axisId = axis.getAxisId();
        if (axisValues[axisId] == value) {
            return;
        }
        axisValues[axisId] = value;
        JoyAxisEvent event = new JoyAxisEvent(axis, value, value);
        event.setTime(time);
        enqueueEvent(event);
    }

    private void enqueueEvent(InputEvent event) {
        events.add(event);
        hasEvents = true;
    }

    private void releaseAllLocked(long time) {
        captures.clear();
        for (Element element : layout.getAxisElements()) {
            centerAxisCapture(element, time);
        }
        for (String logicalId : buttonsByLogicalId.keySet()) {
            pressButton(logicalId, false, time);
        }
    }

    private void syncElement(Element element, AssetManager assetManager, int width, int height, float scale) {
        if (element == null) {
            return;
        }
        if (!element.visible) {
            if (element.node != null && element.node.getParent() != null) {
                element.node.removeFromParent();
            }
            return;
        }
        if (element.node == null) {
            createVisual(element, assetManager);
        }
        if (element.node.getParent() != visualRoot) {
            visualRoot.attachChild(element.node);
        }
        JoystickButton button = buttonsByLogicalId.get(element.id);
        boolean pressed = button != null && (buttonStateMask & (1 << button.getButtonId())) != 0;
        element.sync(width, height, scale, pressed);
    }

    private void createVisual(Element element, AssetManager assetManager) {
        element.node = new Node("Virtual Joystick " + element.id);
        element.base = new Picture(element.id);
        TextureKey baseTextureKey;
        TextureKey nubTextureKey;
        TextureKey iconTextureKey;
        String label;
        baseTextureKey = element.textureKey;
        nubTextureKey = element.nubTextureKey;
        iconTextureKey = element.iconTextureKey;
        label = element.label;
        element.base.setImage(assetManager, texture(baseTextureKey), true);
        element.node.attachChild(element.base);

        if (nubTextureKey != null) {
            element.nub = new Picture(element.id + " Nub");
            element.nub.setImage(assetManager, texture(nubTextureKey), true);
            element.node.attachChild(element.nub);
        }

        if (iconTextureKey != null) {
            element.icon = new Picture(element.id + " Icon");
            element.icon.setImage(assetManager, texture(iconTextureKey), true);
            element.node.attachChild(element.icon);
        }

        if (font != null && !label.isEmpty()) {
            element.text = new BitmapText(font, false);
            element.text.setText(label);
            element.node.attachChild(element.text);
        }
    }

    private String texture(TextureKey textureKey) {
        String texturePath = theme.getTexture(textureKey);
        if (texturePath == null) {
            throw new IllegalStateException("No virtual joystick texture bound for key: " + textureKey);
        }
        return texturePath;
    }

    private void clearVisuals(VirtualJoystickLayout layout) {
        for (Element element : layout.getButtons()) {
            element.clearVisuals();
        }
        for (Element element : layout.getAxisElements()) {
            element.clearVisuals();
        }
        if (visualRoot != null) {
            visualRoot.detachAllChildren();
        }
    }

    private static final class Capture {
        final Element element;
        final boolean axis;

        Capture(Element element, boolean axis) {
            this.element = element;
            this.axis = axis;
        }
    }
}

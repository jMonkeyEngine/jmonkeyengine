/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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
package com.jme3.input;

import com.jme3.app.Application;
import com.jme3.cursors.plugins.JmeCursor;
import com.jme3.input.controls.*;
import com.jme3.input.event.*;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.util.IntMap;
import com.jme3.util.IntMap.Entry;
import com.jme3.util.SafeArrayList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The <code>InputManager</code> is responsible for converting input events
 * received from the Key, Mouse and Joy Input implementations into an
 * abstract, input device independent representation that user code can use.
 * <p>
 * By default an <code>InputManager</code> is included with every Application instance for use
 * in user code to query input, unless the Application is created as headless
 * or with input explicitly disabled.
 * <p>
 * The input manager has two concepts, a {@link Trigger} and a mapping.
 * A trigger represents a specific input trigger, such as a key button,
 * or a mouse axis. A mapping represents a link onto one or several triggers,
 * when the appropriate trigger is activated (e.g. a key is pressed), the
 * mapping will be invoked. Any listeners registered to receive an event
 * from the mapping will have an event raised.
 * <p>
 * There are two types of events that {@link InputListener input listeners}
 * can receive, one is {@link ActionListener#onAction(java.lang.String, boolean, float) action}
 * events and another is {@link AnalogListener#onAnalog(java.lang.String, float, float) analog}
 * events.
 * <p>
 * <code>onAction</code> events are raised when the specific input
 * activates or deactivates. For a digital input such as key press, the <code>onAction()</code>
 * event will be raised with the <code>isPressed</code> argument equal to true,
 * when the key is released, <code>onAction</code> is called again but this time
 * with the <code>isPressed</code> argument set to false.
 * For analog inputs, the <code>onAction</code> method will be called any time
 * the input is non-zero, however an exception to this is for joystick axis inputs,
 * which are only called when the input is above the {@link InputManager#setAxisDeadZone(float) dead zone}.
 * <p>
 * <code>onAnalog</code> events are raised every frame while the input is activated.
 * For digital inputs, every frame that the input is active will cause the
 * <code>onAnalog</code> method to be called, the argument <code>value</code>
 * argument will equal to the frame's time per frame (TPF) value but only
 * for digital inputs. For analog inputs however, the <code>value</code> argument
 * will equal the actual analog value.
 */
public class InputManager implements RawInputListener {

    private static final Logger logger = Logger.getLogger(InputManager.class.getName());
    private final KeyInput keys;
    private final MouseInput mouse;
    private final JoyInput joystick;
    private final TouchInput touch;
    private float frameTPF;
    private long lastLastUpdateTime = 0;
    private long lastUpdateTime = 0;
    private long frameDelta = 0;
    private boolean eventsPermitted = false;
    private boolean mouseVisible = true;
    private boolean safeMode = false;
    private float globalAxisDeadZone = 0.05f;
    private final Vector2f cursorPos = new Vector2f();
    private Joystick[] joysticks;
    private final IntMap<ArrayList<Mapping>> bindings = new IntMap<>();
    private final HashMap<String, Mapping> mappings = new HashMap<>();
    private final IntMap<Long> pressedButtons = new IntMap<>();
    private final IntMap<Float> axisValues = new IntMap<>();
    private final SafeArrayList<RawInputListener> rawListeners = new SafeArrayList<>(RawInputListener.class);
    private final ArrayList<InputEvent> inputQueue = new ArrayList<>();
    private final List<JoystickConnectionListener> joystickConnectionListeners = new ArrayList<>();

    private static class Mapping {

        private final String name;
        private final ArrayList<Integer> triggers = new ArrayList<>();
        private final ArrayList<InputListener> listeners = new ArrayList<>();

        public Mapping(String name) {
            this.name = name;
        }
    }

    /**
     * Initializes the InputManager.
     *
     * <p>This should only be called internally in {@link Application}.
     *
     * @param mouse (not null, alias created)
     * @param keys (not null, alias created)
     * @param joystick (may be null, alias created)
     * @param touch (may be null, alias created)
     * @throws IllegalArgumentException If either mouseInput or keyInput are null.
     */
    public InputManager(MouseInput mouse, KeyInput keys, JoyInput joystick, TouchInput touch) {
        if (keys == null || mouse == null) {
            throw new IllegalArgumentException("Mouse or keyboard cannot be null");
        }

        this.keys = keys;
        this.mouse = mouse;
        this.joystick = joystick;
        this.touch = touch;

        keys.setInputListener(this);
        mouse.setInputListener(this);
        if (joystick != null) {
            joystick.setInputListener(this);
            joysticks = joystick.loadJoysticks(this);
        }
        if (touch != null) {
            touch.setInputListener(this);
        }

        keys.getInputTimeNanos();
    }

    public String getKeyName(int key){
        return keys.getKeyName(key);
    }

    private void invokeActions(int hash, boolean pressed) {
        ArrayList<Mapping> maps = bindings.get(hash);
        if (maps == null) {
            return;
        }

        int size = maps.size();
        for (int i = size - 1; i >= 0; i--) {
            Mapping mapping = maps.get(i);
            ArrayList<InputListener> listeners = mapping.listeners;
            int listenerSize = listeners.size();
            for (int j = listenerSize - 1; j >= 0; j--) {
                InputListener listener = listeners.get(j);
                if (listener instanceof ActionListener) {
                    ((ActionListener) listener).onAction(mapping.name, pressed, frameTPF);
                }
            }
        }
    }

    private float computeAnalogValue(long timeDelta) {
        if (safeMode || frameDelta == 0) {
            return 1f;
        } else {
            return FastMath.clamp(timeDelta / (float) frameDelta, 0, 1);
        }
    }

    private void invokeTimedActions(int hash, long time, boolean pressed) {
        if (!bindings.containsKey(hash)) {
            return;
        }

        if (pressed) {
            pressedButtons.put(hash, time);
        } else {
            Long pressTimeObj = pressedButtons.remove(hash);
            if (pressTimeObj == null) {
                return; // under certain circumstances it can be null, ignore
            }                        // the event then.

            long pressTime = pressTimeObj;
            long lastUpdate = lastLastUpdateTime;
            long releaseTime = time;
            long timeDelta = releaseTime - Math.max(pressTime, lastUpdate);

            if (timeDelta > 0) {
                invokeAnalogs(hash, computeAnalogValue(timeDelta), false);
            }
        }
    }

    private void invokeUpdateActions() {
        for (Entry<Long> pressedButton : pressedButtons) {
            int hash = pressedButton.getKey();

            long pressTime = pressedButton.getValue();
            long timeDelta = lastUpdateTime - Math.max(lastLastUpdateTime, pressTime);

            if (timeDelta > 0) {
                invokeAnalogs(hash, computeAnalogValue(timeDelta), false);
            }
        }

        for (Entry<Float> axisValue : axisValues) {
            int hash = axisValue.getKey();
            float value = axisValue.getValue();
            invokeAnalogs(hash, value * frameTPF, true);
        }
    }

    private void invokeAnalogs(int hash, float value, boolean isAxis) {
        ArrayList<Mapping> maps = bindings.get(hash);
        if (maps == null) {
            return;
        }

        if (!isAxis) {
            value *= frameTPF;
        }

        int size = maps.size();
        for (int i = size - 1; i >= 0; i--) {
            Mapping mapping = maps.get(i);
            ArrayList<InputListener> listeners = mapping.listeners;
            int listenerSize = listeners.size();
            for (int j = listenerSize - 1; j >= 0; j--) {
                InputListener listener = listeners.get(j);
                if (listener instanceof AnalogListener) {
                    // NOTE: multiply by TPF for any button bindings
                    ((AnalogListener) listener).onAnalog(mapping.name, value, frameTPF);
                }
            }
        }
    }

    private void invokeAnalogsAndActions(int hash, float value, float effectiveDeadZone, boolean applyTpf) {
        if (value < effectiveDeadZone) {
            invokeAnalogs(hash, value, !applyTpf);
            return;
        }

        ArrayList<Mapping> maps = bindings.get(hash);
        if (maps == null) {
            return;
        }

        boolean valueChanged = !axisValues.containsKey(hash);
        if (applyTpf) {
            value *= frameTPF;
        }

        int size = maps.size();
        for (int i = size - 1; i >= 0; i--) {
            Mapping mapping = maps.get(i);
            ArrayList<InputListener> listeners = mapping.listeners;
            int listenerSize = listeners.size();
            for (int j = listenerSize - 1; j >= 0; j--) {
                InputListener listener = listeners.get(j);

                if (listener instanceof ActionListener && valueChanged) {
                    ((ActionListener) listener).onAction(mapping.name, true, frameTPF);
                }

                if (listener instanceof AnalogListener) {
                    ((AnalogListener) listener).onAnalog(mapping.name, value, frameTPF);
                }

            }
        }
    }

    /**
     * Callback from RawInputListener. Do not use.
     */
    @Override
    public void beginInput() {
    }

    /**
     * Callback from RawInputListener. Do not use.
     */
    @Override
    public void endInput() {
    }

    private void onJoyAxisEventQueued(JoyAxisEvent evt) {
//        for (int i = 0; i < rawListeners.size(); i++){
//            rawListeners.get(i).onJoyAxisEvent(evt);
//        }

        int joyId = evt.getJoyIndex();
        int axis = evt.getAxisIndex();
        float value = evt.getValue();
        float effectiveDeadZone = Math.max(globalAxisDeadZone, evt.getAxis().getDeadZone()); 
        if (value < effectiveDeadZone && value > -effectiveDeadZone) {
            int hash1 = JoyAxisTrigger.joyAxisHash(joyId, axis, true);
            int hash2 = JoyAxisTrigger.joyAxisHash(joyId, axis, false);

            Float val1 = axisValues.get(hash1);
            Float val2 = axisValues.get(hash2);

            if (val1 != null && val1 > effectiveDeadZone) {
                invokeActions(hash1, false);
            }
            if (val2 != null && val2 > effectiveDeadZone) {
                invokeActions(hash2, false);
            }

            axisValues.remove(hash1);
            axisValues.remove(hash2);

        } else if (value < 0) {
            int hash = JoyAxisTrigger.joyAxisHash(joyId, axis, true);
            int otherHash = JoyAxisTrigger.joyAxisHash(joyId, axis, false);

            // Clear the reverse direction's actions in case we
            // crossed center too quickly
            Float otherVal = axisValues.get(otherHash);
            if (otherVal != null && otherVal > effectiveDeadZone) {
                invokeActions(otherHash, false);
            }

            invokeAnalogsAndActions(hash, -value, effectiveDeadZone, true);
            axisValues.put(hash, -value);
            axisValues.remove(otherHash);
        } else {
            int hash = JoyAxisTrigger.joyAxisHash(joyId, axis, false);
            int otherHash = JoyAxisTrigger.joyAxisHash(joyId, axis, true);

            // Clear the reverse direction's actions in case we
            // crossed center too quickly
            Float otherVal = axisValues.get(otherHash);
            if (otherVal != null && otherVal > effectiveDeadZone) {
                invokeActions(otherHash, false);
            }

            invokeAnalogsAndActions(hash, value, effectiveDeadZone, true);
            axisValues.put(hash, value);
            axisValues.remove(otherHash);
        }
    }

    /**
     * Callback from RawInputListener. Do not use.
     */
    @Override
    public void onJoyAxisEvent(JoyAxisEvent evt) {
        if (!eventsPermitted) {
            throw new UnsupportedOperationException("JoyInput has raised an event at an illegal time.");
        }

        inputQueue.add(evt);
    }

    private void onJoyButtonEventQueued(JoyButtonEvent evt) {
//        for (int i = 0; i < rawListeners.size(); i++){
//            rawListeners.get(i).onJoyButtonEvent(evt);
//        }

        int hash = JoyButtonTrigger.joyButtonHash(evt.getJoyIndex(), evt.getButtonIndex());
        invokeActions(hash, evt.isPressed());
        invokeTimedActions(hash, evt.getTime(), evt.isPressed());
    }

    /**
     * Callback from RawInputListener. Do not use.
     */
    @Override
    public void onJoyButtonEvent(JoyButtonEvent evt) {
        if (!eventsPermitted) {
            throw new UnsupportedOperationException("JoyInput has raised an event at an illegal time.");
        }

        inputQueue.add(evt);
    }

    private void onMouseMotionEventQueued(MouseMotionEvent evt) {
//        for (int i = 0; i < rawListeners.size(); i++){
//            rawListeners.get(i).onMouseMotionEvent(evt);
//        }

        if (evt.getDX() != 0) {
            float val = Math.abs(evt.getDX()) / 1024f;
            invokeAnalogsAndActions(MouseAxisTrigger.mouseAxisHash(MouseInput.AXIS_X, evt.getDX() < 0), val, globalAxisDeadZone, false);
        }
        if (evt.getDY() != 0) {
            float val = Math.abs(evt.getDY()) / 1024f;
            invokeAnalogsAndActions(MouseAxisTrigger.mouseAxisHash(MouseInput.AXIS_Y, evt.getDY() < 0), val, globalAxisDeadZone, false);
        }
        if (evt.getDeltaWheel() != 0) {
            float val = Math.abs(evt.getDeltaWheel()) / 100f;
            invokeAnalogsAndActions(MouseAxisTrigger.mouseAxisHash(MouseInput.AXIS_WHEEL, evt.getDeltaWheel() < 0), val, globalAxisDeadZone, false);
        }
    }

    /**
     * Sets the mouse cursor image or animation.
     * Set cursor to null to show default system cursor.
     * To hide the cursor completely, use {@link #setCursorVisible(boolean) }.
     *
     * @param jmeCursor The cursor to set, or null to reset to system cursor.
     *
     * @see JmeCursor
     */
    public void setMouseCursor(JmeCursor jmeCursor) {
        mouse.setNativeCursor(jmeCursor);
    }

    /**
     * Callback from RawInputListener. Do not use.
     *
     * @param evt event to add to the input queue (not null)
     */
    @Override
    public void onMouseMotionEvent(MouseMotionEvent evt) {
        /*
         * If events aren't allowed, the event may be a "first mouse event"
         * triggered by the constructor setting the mouse listener.
         * In that case, use the event to initialize the cursor position,
         * but don't queue it for further processing.
         * This is part of the fix for issue #792.
         */
        cursorPos.set(evt.getX(), evt.getY());
        if (eventsPermitted) {
            inputQueue.add(evt);
        }
    }

    private void onMouseButtonEventQueued(MouseButtonEvent evt) {
        int hash = MouseButtonTrigger.mouseButtonHash(evt.getButtonIndex());
        invokeActions(hash, evt.isPressed());
        invokeTimedActions(hash, evt.getTime(), evt.isPressed());
    }

    /**
     * Callback from RawInputListener. Do not use.
     */
    @Override
    public void onMouseButtonEvent(MouseButtonEvent evt) {
        if (!eventsPermitted) {
            throw new UnsupportedOperationException("MouseInput has raised an event at an illegal time.");
        }
        // Update cursor pos on click, so that non-Android touch events can properly update cursor position.
        cursorPos.set(evt.getX(), evt.getY());
        inputQueue.add(evt);
    }

    private void onKeyEventQueued(KeyInputEvent evt) {
        if (evt.isRepeating()) {
            return; // repeat events not used for bindings
        }

        int hash = KeyTrigger.keyHash(evt.getKeyCode());
        invokeActions(hash, evt.isPressed());
        invokeTimedActions(hash, evt.getTime(), evt.isPressed());
    }

    /**
     * Callback from RawInputListener. Do not use.
     */
    @Override
    public void onKeyEvent(KeyInputEvent evt) {
        if (!eventsPermitted) {
            throw new UnsupportedOperationException("KeyInput has raised an event at an illegal time.");
        }

        inputQueue.add(evt);
    }

    /**
     * Set the deadzone for joystick axes.
     *
     * <p>{@link ActionListener#onAction(java.lang.String, boolean, float) }
     * events will only be raised if the joystick axis value is greater than
     * the <code>deadZone</code>.
     *
     * @param deadZone the deadzone for joystick axes.
     */
    public void setAxisDeadZone(float deadZone) {
        this.globalAxisDeadZone = deadZone;
    }

    /**
     * Returns the deadzone for joystick axes.
     *
     * @return the deadzone for joystick axes.
     */
    public float getAxisDeadZone() {
        return globalAxisDeadZone;
    }

    /**
     * Adds a new listener to receive events on the given mappings.
     *
     * <p>The given InputListener will be registered to receive events
     * on the specified mapping names. When a mapping raises an event, the
     * listener will have its appropriate method invoked, either
     * {@link ActionListener#onAction(java.lang.String, boolean, float) }
     * or {@link AnalogListener#onAnalog(java.lang.String, float, float) }
     * depending on which interface the <code>listener</code> implements.
     * If the listener implements both interfaces, then it will receive the
     * appropriate event for each method.
     *
     * @param listener The listener to register to receive input events.
     * @param mappingNames The mapping names which the listener will receive
     * events from.
     *
     * @see InputManager#removeListener(com.jme3.input.controls.InputListener)
     */
    public void addListener(InputListener listener, String... mappingNames) {
        for (String mappingName : mappingNames) {
            Mapping mapping = mappings.get(mappingName);
            if (mapping == null) {
                mapping = new Mapping(mappingName);
                mappings.put(mappingName, mapping);
            }
            if (!mapping.listeners.contains(listener)) {
                mapping.listeners.add(listener);
            }
        }
    }

    /**
     * Removes a listener from receiving events.
     *
     * <p>This will unregister the listener from any mappings that it
     * was previously registered with via
     * {@link InputManager#addListener(com.jme3.input.controls.InputListener, java.lang.String[]) }.
     *
     * @param listener The listener to unregister.
     *
     * @see InputManager#addListener(com.jme3.input.controls.InputListener, java.lang.String[])
     */
    public void removeListener(InputListener listener) {
        for (Mapping mapping : mappings.values()) {
            mapping.listeners.remove(listener);
        }
    }

    /**
     * Create a new mapping to the given triggers.
     *
     * <p>
     * The given mapping will be assigned to the given triggers, when
     * any of the triggers given raise an event, the listeners
     * registered to the mappings will receive appropriate events.
     *
     * @param mappingName The mapping name to assign.
     * @param triggers The triggers to which the mapping is to be registered.
     *
     * @see InputManager#deleteMapping(java.lang.String)
     */
    public void addMapping(String mappingName, Trigger... triggers) {
        Mapping mapping = mappings.get(mappingName);
        if (mapping == null) {
            mapping = new Mapping(mappingName);
            mappings.put(mappingName, mapping);
        }

        for (Trigger trigger : triggers) {
            int hash = trigger.triggerHashCode();
            ArrayList<Mapping> names = bindings.get(hash);
            if (names == null) {
                names = new ArrayList<Mapping>();
                bindings.put(hash, names);
            }
            if (!names.contains(mapping)) {
                names.add(mapping);
                mapping.triggers.add(hash);
            } else {
                logger.log(Level.WARNING, "Attempted to add mapping \"{0}\" twice to trigger.", mappingName);
            }
        }
    }

    /**
     * Returns true if this InputManager has a mapping registered
     * for the given mappingName.
     *
     * @param mappingName The mapping name to check.
     * @return true if the mapping is registered, otherwise false
     * @see InputManager#addMapping(java.lang.String, com.jme3.input.controls.Trigger[])
     * @see InputManager#deleteMapping(java.lang.String)
     */
    public boolean hasMapping(String mappingName) {
        return mappings.containsKey(mappingName);
    }

    /**
     * Deletes a mapping from receiving trigger events.
     *
     * <p>
     * The given mapping will no longer be assigned to receive trigger
     * events.
     *
     * @param mappingName The mapping name to unregister.
     *
     * @see InputManager#addMapping(java.lang.String, com.jme3.input.controls.Trigger[])
     */
    public void deleteMapping(String mappingName) {
        Mapping mapping = mappings.remove(mappingName);
        if (mapping == null) {
            //throw new IllegalArgumentException("Cannot find mapping: " + mappingName);
            logger.log(Level.WARNING, "Cannot find mapping to be removed, skipping: {0}", mappingName);
            return;
        }

        ArrayList<Integer> triggers = mapping.triggers;
        for (int i = triggers.size() - 1; i >= 0; i--) {
            int hash = triggers.get(i);
            ArrayList<Mapping> maps = bindings.get(hash);
            maps.remove(mapping);
        }
    }

    /**
     * Deletes a specific trigger registered to a mapping.
     *
     * <p>
     * The given mapping will no longer receive events raised by the
     * trigger.
     *
     * @param mappingName The mapping name to cease receiving events from the
     * trigger.
     * @param trigger The trigger to no longer invoke events on the mapping.
     */
    public void deleteTrigger(String mappingName, Trigger trigger) {
        Mapping mapping = mappings.get(mappingName);
        if (mapping == null) {
            throw new IllegalArgumentException("Cannot find mapping: " + mappingName);
        }

        ArrayList<Mapping> maps = bindings.get(trigger.triggerHashCode());
        maps.remove(mapping);

    }

    /**
     * Clears all the input mappings from this InputManager.
     * Consequently, this clears all of the
     * InputListeners as well.
     */
    public void clearMappings() {
        mappings.clear();
        bindings.clear();
        reset();
    }

    /**
     * Do not use.
     * Called to reset pressed keys or buttons when focus is restored.
     */
    public void reset() {
        pressedButtons.clear();
        axisValues.clear();
    }

    /**
     * Returns whether the mouse cursor is visible or not.
     *
     * <p>By default the cursor is visible.
     *
     * @return whether the mouse cursor is visible or not.
     *
     * @see InputManager#setCursorVisible(boolean)
     */
    public boolean isCursorVisible() {
        return mouseVisible;
    }

    /**
     * Set whether the mouse cursor should be visible or not.
     *
     * @param visible whether the mouse cursor should be visible or not.
     */
    public void setCursorVisible(boolean visible) {
        if (mouseVisible != visible) {
            mouseVisible = visible;
            mouse.setCursorVisible(mouseVisible);
        }
    }

    /**
     * Returns the current cursor position. The position is relative to the
     * bottom-left of the screen and is in pixels.
     *
     * @return the current cursor position
     */
    public Vector2f getCursorPosition() {
        return cursorPos;
    }

    /**
     * Returns an array of all joysticks installed on the system.
     *
     * @return an array of all joysticks installed on the system.
     */
    public Joystick[] getJoysticks() {
        return joysticks;
    }

    /**
     * Adds a {@link RawInputListener} to receive raw input events.
     *
     * <p>
     * Any raw input listeners registered to this <code>InputManager</code>
     * will receive raw input events first, before they get handled
     * by the <code>InputManager</code> itself. The listeners are
     * each processed in the order they were added, e.g. FIFO.
     * <p>
     * If a raw input listener has handled the event and does not wish
     * other listeners down the list to process the event, it may set the
     * {@link InputEvent#setConsumed() consumed flag} to indicate the
     * event was consumed and shouldn't be processed any further.
     * The listener may do this either at each of the event callbacks
     * or at the {@link RawInputListener#endInput() } method.
     *
     * @param listener A listener to receive raw input events.
     *
     * @see RawInputListener
     */
    public void addRawInputListener(RawInputListener listener) {
        rawListeners.add(listener);
    }

    /**
     * Removes a {@link RawInputListener} so that it no longer
     * receives raw input events.
     *
     * @param listener The listener to cease receiving raw input events.
     *
     * @see InputManager#addRawInputListener(com.jme3.input.RawInputListener)
     */
    public void removeRawInputListener(RawInputListener listener) {
        rawListeners.remove(listener);
    }

    /**
     * Clears all {@link RawInputListener}s.
     *
     * @see InputManager#addRawInputListener(com.jme3.input.RawInputListener)
     */
    public void clearRawInputListeners() {
        rawListeners.clear();
    }

    /**
     * Enable simulation of mouse events. Used for touchscreen input only.
     *
     * @param value True to enable simulation of mouse events
     */
    public void setSimulateMouse(boolean value) {
        if (touch != null) {
            touch.setSimulateMouse(value);
        }
    }
    /**
     * @deprecated Use isSimulateMouse
     * Returns state of simulation of mouse events. Used for touchscreen input only.
     *
     * @return true if a mouse is simulated, otherwise false
     */
    @Deprecated
    public boolean getSimulateMouse() {
        if (touch != null) {
            return touch.isSimulateMouse();
        } else {
            return false;
        }
    }

    /**
     * Returns state of simulation of mouse events. Used for touchscreen input only.
     *
     * @return true if a mouse is simulated, otherwise false
     */
    public boolean isSimulateMouse() {
        if (touch != null) {
            return touch.isSimulateMouse();
        } else {
            return false;
        }
    }

    /**
     * Enable simulation of keyboard events. Used for touchscreen input only.
     *
     * @param value True to enable simulation of keyboard events
     */
    public void setSimulateKeyboard(boolean value) {
        if (touch != null) {
            touch.setSimulateKeyboard(value);
        }
    }

    /**
     * Returns state of simulation of key events. Used for touchscreen input only.
     *
     * @return true if a keyboard is simulated, otherwise false
     */
    public boolean isSimulateKeyboard() {
        if (touch != null) {
            return touch.isSimulateKeyboard();
        } else {
            return false;
        }
    }

    private void processQueue() {
        int queueSize = inputQueue.size();
        RawInputListener[] array = rawListeners.getArray(); 

        for (RawInputListener listener : array) {
            listener.beginInput();

            for (int j = 0; j < queueSize; j++) {
                InputEvent event = inputQueue.get(j);
                if (event.isConsumed()) {
                    continue;
                }

                if (event instanceof MouseMotionEvent) {
                    listener.onMouseMotionEvent((MouseMotionEvent) event);
                } else if (event instanceof KeyInputEvent) {
                    listener.onKeyEvent((KeyInputEvent) event);
                } else if (event instanceof MouseButtonEvent) {
                    listener.onMouseButtonEvent((MouseButtonEvent) event);
                } else if (event instanceof JoyAxisEvent) {
                    listener.onJoyAxisEvent((JoyAxisEvent) event);
                } else if (event instanceof JoyButtonEvent) {
                    listener.onJoyButtonEvent((JoyButtonEvent) event);
                } else if (event instanceof TouchEvent) {
                    listener.onTouchEvent((TouchEvent) event);
                } else {
                    assert false;
                }
            }

            listener.endInput();
        }

        for (int i = 0; i < queueSize; i++) {
            InputEvent event = inputQueue.get(i);
            if (event.isConsumed()) {
                continue;
            }

            if (event instanceof MouseMotionEvent) {
                onMouseMotionEventQueued((MouseMotionEvent) event);
            } else if (event instanceof KeyInputEvent) {
                onKeyEventQueued((KeyInputEvent) event);
            } else if (event instanceof MouseButtonEvent) {
                onMouseButtonEventQueued((MouseButtonEvent) event);
            } else if (event instanceof JoyAxisEvent) {
                onJoyAxisEventQueued((JoyAxisEvent) event);
            } else if (event instanceof JoyButtonEvent) {
                onJoyButtonEventQueued((JoyButtonEvent) event);
            } else if (event instanceof TouchEvent) {
                onTouchEventQueued((TouchEvent) event);
            } else {
                assert false;
            }
            // larynx, 2011.06.10 - flag event as reusable because
            // the android input uses a non-allocating ringbuffer which
            // needs to know when the event is not anymore in inputQueue
            // and therefore can be reused.
            event.setConsumed();
        }

        inputQueue.clear();
    }

    /**
     * Updates the <code>InputManager</code>.
     * This will query current input devices and send
     * appropriate events to registered listeners.
     *
     * @param tpf Time per frame value.
     */
    public void update(float tpf) {
        frameTPF = tpf;

        // Activate safemode if the TPF value is so small
        // that rounding errors are inevitable
        safeMode = tpf < 0.015f;

        long currentTime = keys.getInputTimeNanos();
        frameDelta = currentTime - lastUpdateTime;

        eventsPermitted = true;

        keys.update();
        mouse.update();
        if (joystick != null) {
            joystick.update();
        }
        if (touch != null) {
            touch.update();
        }

        eventsPermitted = false;

        processQueue();
        invokeUpdateActions();

        lastLastUpdateTime = lastUpdateTime;
        lastUpdateTime = currentTime;
    }

    /**
     * Dispatches touch events to touch listeners
     * @param evt The touch event to be dispatched to all onTouch listeners
     */
    public void onTouchEventQueued(TouchEvent evt) {
        ArrayList<Mapping> maps = bindings.get(TouchTrigger.touchHash(evt.getKeyCode()));
        if (maps == null) {
            return;
        }

        int size = maps.size();
        for (int i = size - 1; i >= 0; i--) {
            Mapping mapping = maps.get(i);
            ArrayList<InputListener> listeners = mapping.listeners;
            int listenerSize = listeners.size();
            for (int j = listenerSize - 1; j >= 0; j--) {
                InputListener listener = listeners.get(j);
                if (listener instanceof TouchListener) {
                    ((TouchListener) listener).onTouch(mapping.name, evt, frameTPF);
                }
            }
        }
    }

    /**
     * Callback from RawInputListener. Do not use.
     */
    @Override
    public void onTouchEvent(TouchEvent evt) {
        if (!eventsPermitted) {
            throw new UnsupportedOperationException("TouchInput has raised an event at an illegal time.");
        }
        cursorPos.set(evt.getX(), evt.getY());
        inputQueue.add(evt);
    }

    /**
     * Re-sets the joystick list when a joystick is added or removed.
     * This should only be called internally.
     *
     * @param joysticks (alias created)
     */
    public void setJoysticks(Joystick[] joysticks) {
        this.joysticks = joysticks;
    }

    /**
     * Add a listener that reports when a joystick has been added or removed.
     * Currently implemented only in LWJGL3.
     *
     * @param listener the listener
     * @return true
     */
    public boolean addJoystickConnectionListener(JoystickConnectionListener listener) {
        return joystickConnectionListeners.add(listener);
    }

    /**
     * Remove an existing listener.
     * @param listener the listener to remove.
     * @return true if this listener was removed, or false if it was not found.
     */
    public boolean removeJoystickConnectionListener(JoystickConnectionListener listener) {
        return joystickConnectionListeners.remove(listener);
    }

    /**
     * Remove all joystick connection listeners.
     */
    public void clearJoystickConnectionListeners() {
        joystickConnectionListeners.clear();
    }

    /**
     * Called when a joystick has been connected.
     * This should only be called internally.
     * @param joystick the joystick that has been connected.
     */
    public void fireJoystickConnectedEvent(Joystick joystick) {
        for (JoystickConnectionListener listener : joystickConnectionListeners) {
            listener.onConnected(joystick);
        }
    }

    /**
     * Called when a joystick has been disconnected.
     * This should only be called internally.
     * @param joystick the joystick that has been disconnected.
     */
    public void fireJoystickDisconnectedEvent(Joystick joystick) {
        for (JoystickConnectionListener listener : joystickConnectionListeners) {
            listener.onDisconnected(joystick);
        }
    }

}

/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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

import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.InputListener;
import com.jme3.input.controls.JoyAxisTrigger;
import com.jme3.input.controls.JoyButtonTrigger;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.input.controls.TouchListener;
import com.jme3.input.controls.TouchTrigger;
import com.jme3.input.controls.Trigger;
import com.jme3.input.event.InputEvent;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.util.IntMap;
import com.jme3.util.IntMap.Entry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The <code>InputManager</code> is responsible for converting input events
 * received from the Key, Mouse and Joy Input implementations into an
 * abstract, input device independent representation that user code can use.
 *
 * By default a dispatcher is included with every Application instance for use
 * in user code to query input, unless the Application is created as headless
 * or with input explicitly disabled.
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
    private long firstTime = 0;
    private boolean eventsPermitted = false;
    private boolean mouseVisible = true;
    private boolean safeMode = false;
    private float axisDeadZone = 0.05f;
    private Vector2f cursorPos = new Vector2f();
    private Joystick[] joysticks;
    private final IntMap<ArrayList<Mapping>> bindings = new IntMap<ArrayList<Mapping>>();
    private final HashMap<String, Mapping> mappings = new HashMap<String, Mapping>();
    private final IntMap<Long> pressedButtons = new IntMap<Long>();
    private final IntMap<Float> axisValues = new IntMap<Float>();
    private ArrayList<RawInputListener> rawListeners = new ArrayList<RawInputListener>();
    private RawInputListener[] rawListenerArray = null;
    private ArrayList<InputEvent> inputQueue = new ArrayList<InputEvent>();

    private static class Mapping {

        private final String name;
        private final ArrayList<Integer> triggers = new ArrayList<Integer>();
        private final ArrayList<InputListener> listeners = new ArrayList<InputListener>();

        public Mapping(String name) {
            this.name = name;
        }
    }

    /**
     * Initializes the InputManager.
     *
     * @param mouseInput
     * @param keyInput
     * @param joyInput
     * @throws IllegalArgumentException If either mouseInput or keyInput are null.
     */
    public InputManager(MouseInput mouse, KeyInput keys, JoyInput joystick, TouchInput touch) {
        if (keys == null || mouse == null) {
            throw new NullPointerException("Mouse or keyboard cannot be null");
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

        firstTime = keys.getInputTimeNanos();
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
            return FastMath.clamp((float) timeDelta / (float) frameDelta, 0, 1);
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

    private void invokeAnalogsAndActions(int hash, float value, boolean applyTpf) {
        if (value < axisDeadZone) {
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

    public void beginInput() {
    }

    public void endInput() {
    }

    private void onJoyAxisEventQueued(JoyAxisEvent evt) {
//        for (int i = 0; i < rawListeners.size(); i++){
//            rawListeners.get(i).onJoyAxisEvent(evt);
//        }

        int joyId = evt.getJoyIndex();
        int axis = evt.getAxisIndex();
        float value = evt.getValue();
        if (value < axisDeadZone && value > -axisDeadZone) {
            int hash1 = JoyAxisTrigger.joyAxisHash(joyId, axis, true);
            int hash2 = JoyAxisTrigger.joyAxisHash(joyId, axis, false);

            Float val1 = axisValues.get(hash1);
            Float val2 = axisValues.get(hash2);

            if (val1 != null && val1.floatValue() > axisDeadZone) {
                invokeActions(hash1, false);
            }
            if (val2 != null && val2.floatValue() > axisDeadZone) {
                invokeActions(hash2, false);
            }

            axisValues.remove(hash1);
            axisValues.remove(hash2);

        } else if (value < 0) {
            int hash = JoyAxisTrigger.joyAxisHash(joyId, axis, true);
            invokeAnalogsAndActions(hash, -value, true);
            axisValues.put(hash, -value);
        } else {
            int hash = JoyAxisTrigger.joyAxisHash(joyId, axis, false);
            invokeAnalogsAndActions(hash, value, true);
            axisValues.put(hash, value);
        }
    }

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
            invokeAnalogsAndActions(MouseAxisTrigger.mouseAxisHash(MouseInput.AXIS_X, evt.getDX() < 0), val, false);
        }
        if (evt.getDY() != 0) {
            float val = Math.abs(evt.getDY()) / 1024f;
            invokeAnalogsAndActions(MouseAxisTrigger.mouseAxisHash(MouseInput.AXIS_Y, evt.getDY() < 0), val, false);
        }
        if (evt.getDeltaWheel() != 0) {
            float val = Math.abs(evt.getDeltaWheel()) / 100f;
            invokeAnalogsAndActions(MouseAxisTrigger.mouseAxisHash(MouseInput.AXIS_WHEEL, evt.getDeltaWheel() < 0), val, false);
        }
    }

    public void onMouseMotionEvent(MouseMotionEvent evt) {
        if (!eventsPermitted) {
            throw new UnsupportedOperationException("MouseInput has raised an event at an illegal time.");
        }

        cursorPos.set(evt.getX(), evt.getY());
        inputQueue.add(evt);
    }

    private void onMouseButtonEventQueued(MouseButtonEvent evt) {
//        for (int i = 0; i < rawListeners.size(); i++){
//            rawListeners.get(i).onMouseButtonEvent(evt);
//        }

        int hash = MouseButtonTrigger.mouseButtonHash(evt.getButtonIndex());
        invokeActions(hash, evt.isPressed());
        invokeTimedActions(hash, evt.getTime(), evt.isPressed());
    }

    public void onMouseButtonEvent(MouseButtonEvent evt) {
        if (!eventsPermitted) {
            throw new UnsupportedOperationException("MouseInput has raised an event at an illegal time.");
        }

        inputQueue.add(evt);
    }

    private void onKeyEventQueued(KeyInputEvent evt) {
//        for (int i = 0; i < rawListeners.size(); i++){
//            rawListeners.get(i).onKeyEvent(evt);
//        }

        if (evt.isRepeating()) {
            return; // repeat events not used for bindings
        }
        int hash = KeyTrigger.keyHash(evt.getKeyCode());
        invokeActions(hash, evt.isPressed());
        invokeTimedActions(hash, evt.getTime(), evt.isPressed());
    }

    public void onKeyEvent(KeyInputEvent evt) {
        if (!eventsPermitted) {
            throw new UnsupportedOperationException("KeyInput has raised an event at an illegal time.");
        }

        inputQueue.add(evt);
    }

    public void setAxisDeadZone(float deadZone) {
        this.axisDeadZone = deadZone;
    }

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

    public void removeListener(InputListener listener) {
        for (Mapping mapping : mappings.values()) {
            mapping.listeners.remove(listener);
        }
    }

    public void addMapping(String mappingName, Trigger... triggers) {
        Mapping mapping = mappings.get(mappingName);
        if (mapping == null) {
            mapping = new Mapping(mappingName);
            mappings.put(mappingName, mapping);
        }

        for (Trigger trigger : triggers) {
            int hash = trigger.hashCode();
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

    public void deleteMapping(String mappingName) {
        Mapping mapping = mappings.remove(mappingName);
        if (mapping == null) {
            throw new IllegalArgumentException("Cannot find mapping: " + mappingName);
        }

        ArrayList<Integer> triggers = mapping.triggers;
        for (int i = triggers.size() - 1; i >= 0; i--) {
            int hash = triggers.get(i);
            ArrayList<Mapping> maps = bindings.get(hash);
            maps.remove(mapping);
        }
    }

    public void deleteTrigger(String mappingName, Trigger trigger) {
        Mapping mapping = mappings.get(mappingName);
        if (mapping == null) {
            throw new IllegalArgumentException("Cannot find mapping: " + mappingName);
        }

        ArrayList<Mapping> maps = bindings.get(trigger.hashCode());
        maps.remove(mapping);

    }

    /**
     * Clears all the input mappings from this InputManager. Consequently, also clears all of the
     * InputListeners as well.
     */
    public void clearMappings() {
        mappings.clear();
        bindings.clear();
        reset();
    }

    /**
     * Called to reset pressed keys or buttons when focus is restored.
     */
    public void reset() {
        pressedButtons.clear();
        axisValues.clear();
    }

    /**
     * @param visible whether the mouse cursor is visible or not.
     */
    public boolean isCursorVisible() {
        return mouseVisible;
    }

    /**
     * @param visible whether the mouse cursor should be visible or not.
     */
    public void setCursorVisible(boolean visible) {
        if (mouseVisible != visible) {
            mouseVisible = visible;
            mouse.setCursorVisible(mouseVisible);
        }
    }

    public Vector2f getCursorPosition() {
        return cursorPos;
    }

    public Joystick[] getJoysticks() {
        return joysticks;
    }

    public void addRawInputListener(RawInputListener listener) {
        rawListeners.add(listener);
        rawListenerArray = null;
    }

    public void removeRawInputListener(RawInputListener listener) {
        rawListeners.remove(listener);
        rawListenerArray = null;
    }

    public void clearRawInputListeners() {
        rawListeners.clear();
        rawListenerArray = null;
    }

    private RawInputListener[] getRawListenerArray() {
        if (rawListenerArray == null) 
            rawListenerArray = rawListeners.toArray(new RawInputListener[rawListeners.size()]);
        return rawListenerArray;
    }
    
    public TouchInput getTouchInput() {
        return touch;
    }
    
    public void setSimulateMouse(boolean value) {
        if (touch != null) {
            touch.setSimulateMouse(value);
        }
    }
    
    public void setSimulateKeyboard(boolean value) {
        if (touch != null) {
            touch.setSimulateKeyboard(value);
        }
    }

    private void processQueue() {
        int queueSize = inputQueue.size();
        RawInputListener[] array = getRawListenerArray();
 
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
            // and therefor can be reused.
            event.setConsumed();
        }

        inputQueue.clear();
    }

    /**
     * Updates the Dispatcher. This will query current input devices and send
     * appropriate events to registered listeners.
     *
     * @param tpf Time per frame value.
     */
    public void update(float tpf) {
        frameTPF = tpf;
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
        ArrayList<Mapping> maps = bindings.get(TouchTrigger.getHash());
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
     * Receives the touch events from the touch hardware via the input interface
     * @param evt The touch Event received
     */
    @Override
    public void onTouchEvent(TouchEvent evt) {
        if (!eventsPermitted) {
            throw new UnsupportedOperationException("TouchInput has raised an event at an illegal time.");
        }
        inputQueue.add(evt);         
    }
}

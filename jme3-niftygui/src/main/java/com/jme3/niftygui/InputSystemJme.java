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
package com.jme3.niftygui;

import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.SoftTextDialogInput;
import com.jme3.input.controls.SoftTextDialogInputListener;
import com.jme3.input.event.*;
import com.jme3.system.JmeSystem;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.NiftyInputConsumer;
import de.lessvoid.nifty.controls.TextField;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.input.keyboard.KeyboardInputEvent;
import de.lessvoid.nifty.spi.input.InputSystem;
import de.lessvoid.nifty.tools.resourceloader.NiftyResourceLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class InputSystemJme implements InputSystem, RawInputListener {

    private final List<InputEvent> inputQueue = new ArrayList<>();
    private final InputManager inputManager;
    private final boolean[] niftyOwnsDragging = new boolean[3];
    private int inputPointerId = -1;
    private int x, y;
    private int height;
    private boolean shiftDown = false;
    private boolean ctrlDown = false;
    private Nifty nifty;

    public InputSystemJme(InputManager inputManager) {
        this.inputManager = inputManager;
    }

    @Override
    public void setResourceLoader(NiftyResourceLoader niftyResourceLoader) {
    }

    /**
     * Must be set in order for nifty events to be forwarded correctly.
     *
     * @param nifty the Nifty instance to use
     */
    public void setNifty(Nifty nifty) {
        this.nifty = nifty;
    }

    /**
     * Reset internal state of the input system.
     * Must be called when the display is reinitialized
     * or when the internal state becomes invalid.
     */
    public void reset() {
        x = 0;
        y = 0;
        inputPointerId = -1;
        for (int i = 0; i < niftyOwnsDragging.length; i++) {
            niftyOwnsDragging[i] = false;
        }
        shiftDown = false;
        ctrlDown = false;
    }

    /**
     * @param height The height of the viewport. Used to convert
     * bottom-left origin to upper-left origin.
     */
    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public void setMousePosition(int x, int y) {
        // TODO: When does nifty use this?
    }

    @Override
    public void beginInput() {
    }

    @Override
    public void endInput() {
        nifty.update();
    }

    private void handleMouseEvent(int button, boolean value, NiftyInputConsumer nic, InputEvent evt) {
        if (value) {
            // If nifty consumed the mouse down event, then
            // it now owns the next mouse up event which
            // won't be forwarded to jME3.

            // processMouseEvent doesn't return true even if cursor is above
            // a nifty element (bug).
            boolean consumed = nic.processMouseEvent(x, y, 0, button, true)
                             | nifty.getCurrentScreen().isMouseOverElement();
            niftyOwnsDragging[button] = consumed;
            if (consumed) {
                evt.setConsumed();
            }
            //System.out.format("niftyMouse(%d, %d, %d, true) = %b\n", x, y, button, consumed);
        } else {
            // Forward the event if nifty owns it or if the cursor is visible.
            //
            // 2013-10-06: void256 was here and changed stuff ;-) Explanation:
            //
            // Currently Nifty remembers any mouse down event internally as "mouse button now down" regardless of it
            // hitting a Nifty element. As long as it does not receive a mouse up event, Nifty will think of the mouse
            // button as being pressed.
            //
            // The original code:
            // -> if (niftyOwnsDragging[button] || inputManager.isCursorVisible()){
            //
            // forwarded mouse up events to Nifty when:
            // a) Nifty owns dragging, e.g. there was a mouse down event that actually hit a Nifty element before OR
            // b) when the jme mouse cursor is visible.
            //
            // That's ok but the "Nifty remembers the mouse down event" thing had the following consequences in one
            // special case:
            // 1) You click on the jme scene (not Nifty) and Nifty will correctly return false (event not consumed) but
            //    internally it remembers: "mouse button is now down". Note that the jme mouse cursor is now hidden.
            // 2) You release the mouse button but the mouse down event will not be forwarded to Nifty because it
            //    owned the mouse and the jme mouse cursor is not visible.
            //
            // Nifty now still thinks that the mouse button is down, but it's not. The result is that the next click
            // on any Nifty element will not be recognized as an initial click by Nifty. So you need an additional click
            // on the Nifty element to activate it correctly. In case of drag and drop, this additional click was quite
            // irritating.
            //
            // To fix that we'll now forward the mouse button up event ALWAYS to Nifty regardless of it owning the mouse
            // or the jme mouse cursor visibility.
            //
            // Please note: Compared to the original version a side effect is that jme will now always send mouse move
            // events to Nifty even when the mouse cursor is hidden. So in theory it could happen that input events are
            // handled by both: jme and Nifty when f.i. you move around your scene with the mouse cursor hidden and that
            // invisible cursor is moved over some Nifty element. I've not been able to reproduce that case though,
            // which is good ;-) If that ever happens to someone there is an easy fix possible:
            // nifty.setIgnoreMouseEvents() to completely stop Nifty from processing events.

            boolean consumed = nic.processMouseEvent(x, y, 0, button, false);

            // Only consume event if it ORIGINATED in nifty!
            if (niftyOwnsDragging[button] && consumed) {
                evt.setConsumed();
                processSoftKeyboard();
            }

            niftyOwnsDragging[button] = false;
            //System.out.format("niftyMouse(%d, %d, %d, false) = %b\n", x, y, button, consumed);
        }
    }

    private void onTouchEventQueued(TouchEvent evt, NiftyInputConsumer nic) {
        if (inputManager.isSimulateMouse()) {
            return;
        }

        x = (int) evt.getX();
        y = (int) (height - evt.getY());

        // Input manager will not convert touch events to mouse events,
        // so we must do it ourselves.
        switch (evt.getType()) {
            case DOWN:
                if (inputPointerId != -1) {
                    // Another touch was done by the user
                    // while the other interacts with nifty, ignore.
                    break;
                }

                inputPointerId = evt.getPointerId();
                handleMouseEvent(0, true, nic, evt);

                break;
            case UP:
                if (inputPointerId != evt.getPointerId()) {
                    // Another touch was done by the user
                    // while the other interacts with nifty, ignore.
                    break;
                }

                inputPointerId = -1;
                handleMouseEvent(0, false, nic, evt);

                break;
        }
    }

    private void onMouseMotionEventQueued(MouseMotionEvent evt, NiftyInputConsumer nic) {
        int wheel = evt.getDeltaWheel() / 120;
        x = evt.getX();
        y = height - evt.getY();
        nic.processMouseEvent(x, y, wheel, -1, false);
//        if (nic.processMouseEvent(niftyEvt) /*|| nifty.getCurrentScreen().isMouseOverElement()*/){
        // Do not consume motion events
        //evt.setConsumed();
//        }
    }

    private void onMouseButtonEventQueued(MouseButtonEvent evt, NiftyInputConsumer nic) {
        x = evt.getX();
        y = height - evt.getY();
        handleMouseEvent(evt.getButtonIndex(), evt.isPressed(), nic, evt);
    }

    private void onKeyEventQueued(KeyInputEvent evt, NiftyInputConsumer nic) {
        int code = evt.getKeyCode();

        if (code == KeyInput.KEY_LSHIFT || code == KeyInput.KEY_RSHIFT) {
            shiftDown = evt.isPressed();
        } else if (code == KeyInput.KEY_LCONTROL || code == KeyInput.KEY_RCONTROL) {
            ctrlDown = evt.isPressed();
        }

        KeyboardInputEvent keyEvt = new KeyboardInputEvent(code,
                evt.getKeyChar(),
                evt.isPressed(),
                shiftDown,
                ctrlDown);

        if (nic.processKeyboardEvent(keyEvt)) {
            evt.setConsumed();
        }
    }

    @Override
    public void onMouseMotionEvent(MouseMotionEvent evt) {
        // Only forward the event if there's actual motion involved.
        if (inputManager.isCursorVisible() && (evt.getDX() != 0
                || evt.getDY() != 0
                || evt.getDeltaWheel() != 0)) {
            inputQueue.add(evt);
        }
    }

    @Override
    public void onMouseButtonEvent(MouseButtonEvent evt) {
        if (evt.getButtonIndex() >= 0 && evt.getButtonIndex() <= 2) {
            if (evt.isReleased() || inputManager.isCursorVisible()) {
                // Always pass mouse button release events to nifty,
                // even if the mouse cursor is invisible.
                inputQueue.add(evt);
            }
        }
    }

    @Override
    public void onJoyAxisEvent(JoyAxisEvent evt) {
    }

    @Override
    public void onJoyButtonEvent(JoyButtonEvent evt) {
    }

    @Override
    public void onKeyEvent(KeyInputEvent evt) {
        inputQueue.add(evt);
    }

    @Override
    public void onTouchEvent(TouchEvent evt) {
        inputQueue.add(evt);
    }

    @Override
    public void forwardEvents(NiftyInputConsumer nic) {
        int queueSize = inputQueue.size();

        for (int i = 0; i < queueSize; i++) {
            InputEvent evt = inputQueue.get(i);
            if (evt instanceof MouseMotionEvent) {
                onMouseMotionEventQueued((MouseMotionEvent) evt, nic);
            } else if (evt instanceof MouseButtonEvent) {
                onMouseButtonEventQueued((MouseButtonEvent) evt, nic);
            } else if (evt instanceof KeyInputEvent) {
                onKeyEventQueued((KeyInputEvent) evt, nic);
            } else if (evt instanceof TouchEvent) {
                onTouchEventQueued((TouchEvent) evt, nic);
            }
        }

        inputQueue.clear();
    }

    private void processSoftKeyboard() {
        SoftTextDialogInput softTextDialogInput = JmeSystem.getSoftTextDialogInput();
        if (softTextDialogInput != null) {
            Element element = nifty.getCurrentScreen().getFocusHandler().getKeyboardFocusElement();
            if (element != null) {
                final TextField textField = element.getNiftyControl(TextField.class);
                if (textField != null) {
                    Logger.getLogger(InputSystemJme.class.getName()).log(Level.FINE, "Current TextField: {0}", textField.getId());
                    String initialValue = textField.getRealText();
                    if (initialValue == null) {
                        initialValue = "";
                    }

                    softTextDialogInput.requestDialog(SoftTextDialogInput.TEXT_ENTRY_DIALOG, "Enter Text", initialValue, new SoftTextDialogInputListener() {

                        @Override
                        public void onSoftText(int action, String text) {
                            if (action == SoftTextDialogInputListener.COMPLETE) {
                                textField.setText(text);
                            }
                        }
                    });
                }
            }
        }

    }
}

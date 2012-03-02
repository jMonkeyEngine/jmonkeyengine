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
import de.lessvoid.nifty.controls.nullobjects.TextFieldNull;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.tools.resourceloader.NiftyResourceLoader;
import de.lessvoid.nifty.input.keyboard.KeyboardInputEvent;
import de.lessvoid.nifty.spi.input.InputSystem;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class InputSystemJme implements InputSystem, RawInputListener {

    private final ArrayList<InputEvent> inputQueue = new ArrayList<InputEvent>();
    private InputManager inputManager;
    private boolean isDragging = false, niftyOwnsDragging = false;
    private boolean pressed = false;
    private int buttonIndex;
    private int x, y;
    private int height;
    private boolean shiftDown = false;
    private boolean ctrlDown = false;
    private Nifty nifty;

    public InputSystemJme(InputManager inputManager) {
        this.inputManager = inputManager;
    }

    public void setResourceLoader(NiftyResourceLoader niftyResourceLoader) {
    }

    public void setNifty(Nifty nifty) {
        this.nifty = nifty;
    }

    /**
     * @param height The height of the viewport. Used to convert
     * buttom-left origin to upper-left origin.
     */
    public void setHeight(int height) {
        this.height = height;
    }

    public void setMousePosition(int x, int y) {
    }

    public void beginInput() {
    }

    public void endInput() {
        boolean result = nifty.update();
    }

    private void onTouchEventQueued(TouchEvent evt, NiftyInputConsumer nic) {
        boolean consumed = false;

        x = (int) evt.getX();
        y = (int) (height - evt.getY());

        if (!inputManager.getSimulateMouse()) {            
            switch (evt.getType()) {
                case DOWN:
                    consumed = nic.processMouseEvent(x, y, 0, 0, true);
                    isDragging = true;
                    niftyOwnsDragging = consumed;
                    if (consumed) {
                        evt.setConsumed();
                    }

                    break;

                case UP:
                    if (niftyOwnsDragging) {
                        consumed = nic.processMouseEvent(x, y, 0, 0, false);
                        if (consumed) {
                            evt.setConsumed();
                        }
                    }

                    isDragging = false;
                    niftyOwnsDragging = false;

                    if (consumed) {
                        processSoftKeyboard();
                    }

                    break;
            }
        }
    }

    private void onMouseMotionEventQueued(MouseMotionEvent evt, NiftyInputConsumer nic) {
        x = evt.getX();
        y = height - evt.getY();
        nic.processMouseEvent(x, y, evt.getDeltaWheel(), buttonIndex, pressed);
//        if (nic.processMouseEvent(niftyEvt) /*|| nifty.getCurrentScreen().isMouseOverElement()*/){
        // Do not consume motion events
        //evt.setConsumed();
//        }
    }

    private void onMouseButtonEventQueued(MouseButtonEvent evt, NiftyInputConsumer nic) {
        boolean wasPressed = pressed;
        boolean forwardToNifty = true;

        buttonIndex = evt.getButtonIndex();
        pressed = evt.isPressed();

        // Mouse button raised. End dragging
        if (wasPressed && !pressed) {
            if (!niftyOwnsDragging) {
                forwardToNifty = false;
            }
            isDragging = false;
            niftyOwnsDragging = false;
        }

        boolean consumed = false;
        if (forwardToNifty) {
            consumed = nic.processMouseEvent(x, y, 0, buttonIndex, pressed);
            if (consumed) {
                evt.setConsumed();
            }
        }

        // Mouse button pressed. Begin dragging
        if (!wasPressed && pressed) {
            isDragging = true;
            niftyOwnsDragging = consumed;
        }

        if (consumed && pressed) {
            processSoftKeyboard();
        }

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

    public void onMouseMotionEvent(MouseMotionEvent evt) {
        // Only forward the event if there's actual motion involved.
        if (inputManager.isCursorVisible() && (evt.getDX() != 0
                || evt.getDY() != 0
                || evt.getDeltaWheel() != 0)) {
            inputQueue.add(evt);
        }
    }

    public void onMouseButtonEvent(MouseButtonEvent evt) {
        if (inputManager.isCursorVisible() && evt.getButtonIndex() >= 0 && evt.getButtonIndex() <= 2) {
            inputQueue.add(evt);
        }
    }

    public void onJoyAxisEvent(JoyAxisEvent evt) {
    }

    public void onJoyButtonEvent(JoyButtonEvent evt) {
    }

    public void onKeyEvent(KeyInputEvent evt) {
        inputQueue.add(evt);
    }

    public void onTouchEvent(TouchEvent evt) {
        inputQueue.add(evt);
    }

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
                if (textField != null && !(textField instanceof TextFieldNull)) {
                    Logger.getLogger(InputSystemJme.class.getName()).log(Level.INFO, "Current TextField: {0}", textField.getId());
                    String initialValue = textField.getText();
                    if (initialValue == null) {
                        initialValue = "";
                    }

                    softTextDialogInput.requestDialog(SoftTextDialogInput.TEXT_ENTRY_DIALOG, "Enter Text", initialValue, new SoftTextDialogInputListener() {

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

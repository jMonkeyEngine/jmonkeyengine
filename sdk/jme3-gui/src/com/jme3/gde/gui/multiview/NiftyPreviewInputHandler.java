/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.gui.multiview;

import de.lessvoid.nifty.NiftyInputConsumer;
import de.lessvoid.nifty.input.keyboard.KeyboardInputEvent;
import de.lessvoid.nifty.input.mouse.MouseInputEvent;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author normenhansen
 */
public class NiftyPreviewInputHandler implements de.lessvoid.nifty.spi.input.InputSystem {

    private List<MouseInputEvent> mouseEvents = new LinkedList<MouseInputEvent>();
    private List<KeyboardInputEvent> keyEvents = new LinkedList<KeyboardInputEvent>();

    public synchronized void forwardEvents(NiftyInputConsumer nic) {
        for (Iterator<MouseInputEvent> it = mouseEvents.iterator(); it.hasNext();) {
            MouseInputEvent mouseInputEvent = it.next();
            nic.processMouseEvent(mouseInputEvent);
            it.remove();
        }
        for (Iterator<KeyboardInputEvent> it = keyEvents.iterator(); it.hasNext();) {
            KeyboardInputEvent keyInputEvent = it.next();
            nic.processKeyboardEvent(keyInputEvent);
            it.remove();
        }
    }

    public synchronized void addMouseEvent(int newMouseX, int newMouseY, boolean mouseDown) {
        MouseInputEvent event = new MouseInputEvent(newMouseX, newMouseY, mouseDown);
        mouseEvents.add(event);
    }
    public synchronized void addKeyEvent(int newKey, char newCharacter, boolean newKeyDown, boolean newShiftDown, boolean newControlDown) {
        KeyboardInputEvent event = new KeyboardInputEvent(newKey, newCharacter, newKeyDown, newShiftDown, newControlDown);
        keyEvents.add(event);
    }

    public void setMousePosition(int i, int i1) {
//        throw new UnsupportedOperationException("Not supported yet.");
    }
}

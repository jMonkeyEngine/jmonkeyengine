/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.scenecomposer.tools.shortcuts;

import com.jme3.gde.scenecomposer.SceneEditTool;
import com.jme3.input.KeyInput;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.math.Vector3f;
import java.util.ArrayList;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author dokthar
 */
@ServiceProvider(service = ShortcutManager.class)
public class ShortcutManager {

    private ShortcutTool currentShortcut;
    private ArrayList<ShortcutTool> shortcutList;
    private boolean ctrlDown = false;
    private boolean shiftDown = false;
    private boolean altDown = false;

    public ShortcutManager() {
        shortcutList = new ArrayList<ShortcutTool>();
        shortcutList.add(new MoveShortcut());
        shortcutList.add(new RotateShortcut());
        shortcutList.add(new ScaleShortcut());
        shortcutList.add(new DuplicateShortcut());
        shortcutList.add(new DeleteShortcut());
    }

    /*
     Methodes 
     */
    /**
     * This MUST be called by the shortcut tool once the modifications are done.
     */
    public void terminate() {
        currentShortcut = null;
    }

    /**
     *
     * @return true if a shortCutTool is active, else return false.
     */
    public boolean isActive() {
        return currentShortcut != null;
    }

    /**
     * @return the ctrlDown
     */
    public boolean isCtrlDown() {
        return ctrlDown;
    }

    /**
     * @return the shiftDown
     */
    public boolean isShiftDown() {
        return shiftDown;
    }

    /**
     * @return the altDown
     */
    public boolean isAltDown() {
        return altDown;
    }

    /**
     * Set the current shortcut to <code>shortcut</code>. cancel the current
     * shortcut if it was still active
     *
     * @param shortcut the ShortCutTool to set
     */
    public void setShortCut(ShortcutTool shortcut) {
        if (isActive()) {
            currentShortcut.cancel();
        }
        currentShortcut = shortcut;
    }

    /**
     * Get the shortcut that can be enable with the given kei, the current
     * shortcut cannot be enable twice. This also check for command key used to
     * provide isCtrlDown(), isShiftDown() and isAltDown().
     *
     * @param kie the KeyInputEvent
     * @return the activable shortcut else return null
     */
    public ShortcutTool getActivableShortcut(KeyInputEvent kie) {
        if (checkCommandeKey(kie)) {
            return null;
        }
        for (ShortcutTool s : shortcutList) {
            if (s != currentShortcut) {
                if (s.isActivableBy(kie)) {
                    return s;
                }
            }
        }
        return null;
    }

    /**
     *
     * @return the current active shortcut
     */
    public ShortcutTool getActiveShortcut() {
        return currentShortcut;
    }

    /**
     *
     * @param kie the KeyInputEvent
     * @return true if the given Kei can enable a sortcut, else false
     */
    public boolean canActivateShortcut(KeyInputEvent kie) {
        return getActivableShortcut(kie) != null;
    }

    /**
     * Set the current shortcut with the shortcut one that can be enable with
     * the given key
     *
     * @param kie the KeyInputEvent
     * @return true is the shortcut changed, else false
     */
    public boolean activateShortcut(KeyInputEvent kie) {
        ShortcutTool newShortcut = getActivableShortcut(kie);
        if (newShortcut != null) {
            currentShortcut = newShortcut;
        }
        return newShortcut != null;
    }

    /**
     * This should be called to trigger the currentShortcut.keyPressed() method.
     * This also check for command key used to provide isCtrlDown(),
     * isShiftDown() and isAltDown().
     *
     * @param kie
     */
    public void doKeyPressed(KeyInputEvent kie) {
        if (checkCommandeKey(kie)) {
            //return;
        } else if (isActive()) {
            currentShortcut.keyPressed(kie);
        }
    }

    private boolean checkCommandeKey(KeyInputEvent kie) {
        if (isCtrlKey(kie)) {
            ctrlDown = kie.isPressed();
            return true;
        } else if (isAltKey(kie)) {
            altDown = kie.isPressed();
            return true;
        } else if (isShiftKey(kie)) {
            shiftDown = kie.isPressed();
            return true;
        }
        return false;
    }

    /*
     STATIC
     */
    /**
     *
     * @param kie
     * @return true if the given kie is KEY_RETURN
     */
    public static boolean isEnterKey(KeyInputEvent kie) {
        return (kie.getKeyCode() == KeyInput.KEY_RETURN);
    }

    /**
     *
     * @param kie
     * @return true if the given kie is KEY_ESCAPE
     */
    public static boolean isEscKey(KeyInputEvent kie) {
        return (kie.getKeyCode() == KeyInput.KEY_ESCAPE);
    }

    /**
     *
     * @param kie
     * @return true if the given kie is KEY_LCONTROL || KEY_RCONTROL
     */
    public static boolean isCtrlKey(KeyInputEvent kie) {
        return (kie.getKeyCode() == KeyInput.KEY_LCONTROL || kie.getKeyCode() == KeyInput.KEY_RCONTROL);
    }

    /**
     *
     * @param kie
     * @return true if the given kie is KEY_LSHIFT || KEY_RSHIFT
     */
    public static boolean isShiftKey(KeyInputEvent kie) {
        return (kie.getKeyCode() == KeyInput.KEY_LSHIFT || kie.getKeyCode() == KeyInput.KEY_RSHIFT);
    }

    /**
     *
     * @param kie
     * @return true if the given kie is KEY_LMENU || KEY_RMENU
     */
    public static boolean isAltKey(KeyInputEvent kie) {
        return (kie.getKeyCode() == KeyInput.KEY_LMENU || kie.getKeyCode() == KeyInput.KEY_RMENU);
    }

    /**
     *
     * @param kie
     * @return
     */
    public static boolean isNumberKey(KeyInputEvent kie) {
        switch (kie.getKeyCode()) {
            case KeyInput.KEY_MINUS:
            case KeyInput.KEY_0:
            case KeyInput.KEY_1:
            case KeyInput.KEY_2:
            case KeyInput.KEY_3:
            case KeyInput.KEY_4:
            case KeyInput.KEY_5:
            case KeyInput.KEY_6:
            case KeyInput.KEY_7:
            case KeyInput.KEY_8:
            case KeyInput.KEY_9:
            case KeyInput.KEY_NUMPAD0:
            case KeyInput.KEY_NUMPAD1:
            case KeyInput.KEY_NUMPAD2:
            case KeyInput.KEY_NUMPAD3:
            case KeyInput.KEY_NUMPAD4:
            case KeyInput.KEY_NUMPAD5:
            case KeyInput.KEY_NUMPAD6:
            case KeyInput.KEY_NUMPAD7:
            case KeyInput.KEY_NUMPAD8:
            case KeyInput.KEY_NUMPAD9:
            case KeyInput.KEY_PERIOD:
                return true;
        }
        return false;
    }

    /**
     * store the number kie into the numberBuilder
     *
     * @param kie the KeiInputEvent to be handled as a number.
     * @param numberBuilder the number builder that will be modified !
     */
    public static void setNumberKey(KeyInputEvent kie, StringBuilder numberBuilder) {
        switch (kie.getKeyCode()) {
            case KeyInput.KEY_MINUS:
                if (numberBuilder.length() > 0) {
                    if (numberBuilder.charAt(0) == '-') {
                        numberBuilder.replace(0, 1, "");
                    } else {
                        numberBuilder.insert(0, '-');
                    }
                } else {
                    numberBuilder.append('-');
                }
                break;
            case KeyInput.KEY_0:
            case KeyInput.KEY_1:
            case KeyInput.KEY_2:
            case KeyInput.KEY_3:
            case KeyInput.KEY_4:
            case KeyInput.KEY_5:
            case KeyInput.KEY_6:
            case KeyInput.KEY_7:
            case KeyInput.KEY_8:
            case KeyInput.KEY_9:
            case KeyInput.KEY_NUMPAD0:
            case KeyInput.KEY_NUMPAD1:
            case KeyInput.KEY_NUMPAD2:
            case KeyInput.KEY_NUMPAD3:
            case KeyInput.KEY_NUMPAD4:
            case KeyInput.KEY_NUMPAD5:
            case KeyInput.KEY_NUMPAD6:
            case KeyInput.KEY_NUMPAD7:
            case KeyInput.KEY_NUMPAD8:
            case KeyInput.KEY_NUMPAD9:
                numberBuilder.append(kie.getKeyChar());
                break;
            case KeyInput.KEY_PERIOD:
                if (numberBuilder.indexOf(".") == -1) { // if it doesn't exist yet
                    if (numberBuilder.length() == 0
                            || (numberBuilder.length() == 1 && numberBuilder.charAt(0) == '-')) {
                        numberBuilder.append("0.");
                    } else {
                        numberBuilder.append(".");
                    }
                }
                break;
        }
    }

    /**
     *
     * @param numberBuilder the StringBuilder storing the float number
     * @return the float value created from the given StringBuilder
     */
    public static float getNumberKey(StringBuilder numberBuilder) {
        if (numberBuilder.length() == 0) {
            return 0;
        } else {
            return new Float(numberBuilder.toString());
        }
    }

    /**
     * Test if the given kie can be handled as en axis input by the getAxisKey()
     * method.
     *
     * @param kie the KeyInputEvent to test
     * @return true is the kie can be handled as an axis input, else false
     */
    public static boolean isAxisKey(KeyInputEvent kie) {
        switch (kie.getKeyCode()) {
            case KeyInput.KEY_X:
            case KeyInput.KEY_Y:
            case KeyInput.KEY_Z:
                return true;
        }
        return false;
    }

    /**
     * Handle the Kie as an axis input : return a Vector3f from the kie keyCode.
     *
     * @param kie the KeyInputEvent to handle as an Axis
     * @return UNIT_X for 'x', UNIT_Y for 'y' and UNIT_Z for 'z' kie.
     */
    public static Vector3f getAxisKey(KeyInputEvent kie) {
        Vector3f result = Vector3f.ZERO;
        switch (kie.getKeyCode()) {
            case KeyInput.KEY_X:
                result = Vector3f.UNIT_X;
                break;
            case KeyInput.KEY_Y:
                result = Vector3f.UNIT_Y;
                break;
            case KeyInput.KEY_Z:
                result = Vector3f.UNIT_Z;
                break;
        }
        return result;
    }

}

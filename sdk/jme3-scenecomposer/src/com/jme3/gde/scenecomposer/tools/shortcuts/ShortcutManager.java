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
        if (checkCtrlHit(kie)) {
            ctrlDown = kie.isPressed();
            return true;
        } else if (checkAltHit(kie)) {
            altDown = kie.isPressed();
            return true;
        } else if (checkShiftHit(kie)) {
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
    public static boolean checkEnterHit(KeyInputEvent kie) {
        if (kie.getKeyCode() == KeyInput.KEY_RETURN) {
            return true;
        }
        return false;
    }

    /**
     *
     * @param kie
     * @return true if the given kie is KEY_ESCAPE
     */
    public static boolean checkEscHit(KeyInputEvent kie) {
        if (kie.getKeyCode() == KeyInput.KEY_ESCAPE) {
            return true;
        }
        return false;
    }

    /**
     *
     * @param kie
     * @return true if the given kie is KEY_LCONTROL || KEY_RCONTROL
     */
    public static boolean checkCtrlHit(KeyInputEvent kie) {
        if (kie.getKeyCode() == KeyInput.KEY_LCONTROL || kie.getKeyCode() == KeyInput.KEY_RCONTROL) {
            return true;
        }
        return false;
    }

    /**
     *
     * @param kie
     * @return true if the given kie is KEY_LSHIFT || KEY_RSHIFT
     */
    public static boolean checkShiftHit(KeyInputEvent kie) {
        if (kie.getKeyCode() == KeyInput.KEY_LSHIFT || kie.getKeyCode() == KeyInput.KEY_RSHIFT) {
            return true;
        }
        return false;
    }

    /**
     *
     * @param kie
     * @return true if the given kie is KEY_LMENU || KEY_RMENU
     */
    public static boolean checkAltHit(KeyInputEvent kie) {
        if (kie.getKeyCode() == KeyInput.KEY_LMENU || kie.getKeyCode() == KeyInput.KEY_RMENU) {
            return true;
        }
        return false;
    }

    /**
     * store the number kie into the numberBuilder
     *
     * @param kie
     * @param numberBuilder
     * @return true if the given kie is handled as a number key event
     */
    public static boolean checkNumberKey(KeyInputEvent kie, StringBuilder numberBuilder) {
        if (kie.getKeyCode() == KeyInput.KEY_MINUS) {
            if (numberBuilder.length() > 0) {
                if (numberBuilder.charAt(0) == '-') {
                    numberBuilder.replace(0, 1, "");
                } else {
                    numberBuilder.insert(0, '-');
                }
            } else {
                numberBuilder.append('-');
            }
            return true;
        } else if (kie.getKeyCode() == KeyInput.KEY_0 || kie.getKeyCode() == KeyInput.KEY_NUMPAD0) {
            numberBuilder.append('0');
            return true;
        } else if (kie.getKeyCode() == KeyInput.KEY_1 || kie.getKeyCode() == KeyInput.KEY_NUMPAD1) {
            numberBuilder.append('1');
            return true;
        } else if (kie.getKeyCode() == KeyInput.KEY_2 || kie.getKeyCode() == KeyInput.KEY_NUMPAD2) {
            numberBuilder.append('2');
            return true;
        } else if (kie.getKeyCode() == KeyInput.KEY_3 || kie.getKeyCode() == KeyInput.KEY_NUMPAD3) {
            numberBuilder.append('3');
            return true;
        } else if (kie.getKeyCode() == KeyInput.KEY_4 || kie.getKeyCode() == KeyInput.KEY_NUMPAD4) {
            numberBuilder.append('4');
            return true;
        } else if (kie.getKeyCode() == KeyInput.KEY_5 || kie.getKeyCode() == KeyInput.KEY_NUMPAD5) {
            numberBuilder.append('5');
            return true;
        } else if (kie.getKeyCode() == KeyInput.KEY_6 || kie.getKeyCode() == KeyInput.KEY_NUMPAD6) {
            numberBuilder.append('6');
            return true;
        } else if (kie.getKeyCode() == KeyInput.KEY_7 || kie.getKeyCode() == KeyInput.KEY_NUMPAD7) {
            numberBuilder.append('7');
            return true;
        } else if (kie.getKeyCode() == KeyInput.KEY_8 || kie.getKeyCode() == KeyInput.KEY_NUMPAD8) {
            numberBuilder.append('8');
            return true;
        } else if (kie.getKeyCode() == KeyInput.KEY_9 || kie.getKeyCode() == KeyInput.KEY_NUMPAD9) {
            numberBuilder.append('9');
            return true;
        } else if (kie.getKeyCode() == KeyInput.KEY_PERIOD) {
            if (numberBuilder.indexOf(".") == -1) { // if it doesn't exist yet
                if (numberBuilder.length() == 0
                        || (numberBuilder.length() == 1 && numberBuilder.charAt(0) == '-')) {
                    numberBuilder.append("0.");
                } else {
                    numberBuilder.append(".");
                }
            }
            return true;
        }

        return false;
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
     * Check for axis input for key X,Y,Z and store the corresponding UNIT_ into
     * the axisStore
     *
     * @param kie
     * @param axisStore
     * @return true if the given kie is handled as a Axis input
     */
    public static boolean checkAxisKey(KeyInputEvent kie, Vector3f axisStore) {
        if (kie.getKeyCode() == KeyInput.KEY_X) {
            axisStore.set(Vector3f.UNIT_X);
            return true;
        } else if (kie.getKeyCode() == KeyInput.KEY_Y) {
            axisStore.set(Vector3f.UNIT_Y);
            return true;
        } else if (kie.getKeyCode() == KeyInput.KEY_Z) {
            axisStore.set(Vector3f.UNIT_Z);
            return true;
        }
        return false;
    }

}

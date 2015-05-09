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

    public ShortcutManager() {
        shortcutList = new ArrayList<ShortcutTool>();
        shortcutList.add(new MoveShortcut());
    }

    /*
     Methodes 
     */
    public void terminate() {
        currentShortcut = null;
    }

    public boolean isActive() {
        return currentShortcut != null;
    }

    public void setShortCut(ShortcutTool shortcut) {
        if (isActive()) {
            currentShortcut.cancel();
        }
        currentShortcut = shortcut;
    }

    public ShortcutTool getActivableShortcut(KeyInputEvent kie) {
        for (ShortcutTool s : shortcutList) {
            if (s != currentShortcut) {
                if (s.isActivableBy(kie)) {
                    return s;
                }
            }
        }
        return null;
    }

    public ShortcutTool getActiveShortcut() {
        return currentShortcut;
    }

    public boolean canActivateShortcut(KeyInputEvent kie) {
        return getActivableShortcut(kie) != null;
    }

    public boolean activateShortcut(KeyInputEvent kie) {
        currentShortcut = getActivableShortcut(kie);
        return isActive();
    }

    public void doKeyPressed(KeyInputEvent kie) {
        ///todo check commande key
        if (isActive()) {
            currentShortcut.keyPressed(kie);
        }
    }

    /*
     STATIC
     */
    public static boolean checkEnterHit(KeyInputEvent kie) {
        if (kie.getKeyCode() == KeyInput.KEY_RETURN) {
            return true;
        }
        return false;
    }

    public static boolean checkEscHit(KeyInputEvent kie) {
        if (kie.getKeyCode() == KeyInput.KEY_ESCAPE) {
            return true;
        }
        return false;
    }

    public static boolean checkCtrlHit(KeyInputEvent kie) {
        if (kie.getKeyCode() == KeyInput.KEY_LCONTROL || kie.getKeyCode() == KeyInput.KEY_RCONTROL) {
            return true;
        }
        return false;
    }

    public static boolean checkShiftHit(KeyInputEvent kie) {
        if (kie.getKeyCode() == KeyInput.KEY_LSHIFT || kie.getKeyCode() == KeyInput.KEY_RSHIFT) {
            return true;
        }
        return false;
    }

    public static boolean checkAltHit(KeyInputEvent kie) {
        if (kie.getKeyCode() == KeyInput.KEY_LMENU || kie.getKeyCode() == KeyInput.KEY_RMENU) {
            return true;
        }
        return false;
    }

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

    public static float getNumberkey(StringBuilder numberBuilder) {
        if (numberBuilder.length() == 0) {
            return 0;
        } else {
            return new Float(numberBuilder.toString());
        }
    }

    public static boolean checkAxisKey(KeyInputEvent kie, Vector3f axisStore) {
        if (kie.getKeyCode() == KeyInput.KEY_X) {
            axisStore = Vector3f.UNIT_X;
            return true;
        } else if (kie.getKeyCode() == KeyInput.KEY_Y) {
            axisStore = Vector3f.UNIT_Y;
            return true;
        } else if (kie.getKeyCode() == KeyInput.KEY_Z) {
            axisStore = Vector3f.UNIT_Z;
            return true;
        }
        return false;
    }

}

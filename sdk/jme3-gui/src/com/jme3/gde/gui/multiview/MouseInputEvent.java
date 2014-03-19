/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jme3.gde.gui.multiview;

/**
 *
 * @author normenhansen
 */
public class MouseInputEvent {
    int x;
    int y;
    int button;
    int huh;
    boolean pressed;

    public MouseInputEvent(int x, int y, int button, int huh, boolean pressed) {
        this.x = x;
        this.y = y;
        this.button = button;
        this.huh = huh;
        this.pressed = pressed;
    }

}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.scenecomposer.tools.shortcuts;

import com.jme3.gde.scenecomposer.SceneEditTool;
import com.jme3.input.event.KeyInputEvent;
import org.openide.util.Lookup;

/**
 *
 * @author dokthar
 */
public abstract class ShortcutTool extends SceneEditTool {

    public abstract boolean isActivableBy(KeyInputEvent kie);

    public abstract void cancel();

    protected final void terminate() {
        Lookup.getDefault().lookup(ShortcutManager.class).terminate();
    }

    @Override
    public abstract void keyPressed(KeyInputEvent kie);
   
}

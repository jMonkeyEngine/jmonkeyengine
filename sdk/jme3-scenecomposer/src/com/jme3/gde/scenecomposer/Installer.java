/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.scenecomposer;

import org.openide.awt.Toolbar;
import org.openide.awt.ToolbarPool;
import org.openide.modules.ModuleInstall;
import org.openide.windows.WindowManager;

public class Installer extends ModuleInstall {

    @Override
    public void restored() {
        WindowManager.getDefault().invokeWhenUIReady(new Runnable() {
            public void run() {
                Toolbar tb = ToolbarPool.getDefault().findToolbar("SceneComposer-Tools");
                if (tb != null) {
                    tb.setVisible(false);
                }
            }
        });
    }
}

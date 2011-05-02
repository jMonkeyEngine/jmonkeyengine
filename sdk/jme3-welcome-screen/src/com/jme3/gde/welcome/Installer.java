/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.welcome;

import org.openide.modules.ModuleInstall;
import org.openide.util.NbPreferences;

/**
 * Manages a module's lifecycle. Remember that an installer is optional and
 * often not needed at all.
 */
public class Installer extends ModuleInstall {

    @Override
    public void restored() {
        if(!"true".equals(NbPreferences.forModule(Installer.class).get("NO_WELCOME_SCREEN", null))){
//            new WelcomeScreen().startScreen();
        }
    }
}

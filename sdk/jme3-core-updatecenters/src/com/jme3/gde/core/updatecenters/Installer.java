/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.core.updatecenters;

import org.openide.modules.ModuleInstall;
import org.openide.util.NbPreferences;

public class Installer extends ModuleInstall {

    @Override
    public void restored() {
        NbPreferences.root().node("org/netbeans/modules/autoupdate/ui").put("plugin_manager_shared_installation", "false");
    }
}

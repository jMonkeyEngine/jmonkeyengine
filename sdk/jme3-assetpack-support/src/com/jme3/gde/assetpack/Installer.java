/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.assetpack;

import java.io.File;
import org.openide.modules.ModuleInstall;
import org.openide.util.NbPreferences;
import org.openide.util.Utilities;

/**
 * Manages a module's lifecycle. Remember that an installer is optional and
 * often not needed at all.
 */
public class Installer extends ModuleInstall {

    @Override
    public void restored() {
        String path = NbPreferences.forModule(Installer.class).get("assetpack_path", null);
        if (path == null) {
            //set extraction dir for platform natives
            if (Utilities.isMac()) {
                String jmpDir = System.getProperty("user.home") + "/Library/Application Support/jmonkeyplatform/assetpacks/";
                NbPreferences.forModule(Installer.class).put("assetpack_path", jmpDir);
                new File(jmpDir).mkdirs();
            } else {
                String jmpDir = System.getProperty("user.home") + File.separator + ".jmonkeyplatform" + File.separator + "assetpacks" + File.separator;
                NbPreferences.forModule(Installer.class).put("assetpack_path", jmpDir);
                new File(jmpDir).mkdirs();
            }
        }
    }
}

/*
 *  Copyright (c) 2009-2010 jMonkeyEngine
 *  All rights reserved.
 * 
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are
 *  met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 
 *  * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *  TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 *  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.gde.core;

import com.jme3.gde.core.scene.SceneApplication;
import java.io.File;
import javax.swing.JPopupMenu;
import org.openide.filesystems.FileChooserBuilder;
import org.openide.modules.ModuleInstall;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;

/**
 * Manages a module's lifecycle. Remember that an installer is optional and
 * often not needed at all.
 */
public class Installer extends ModuleInstall {

    @Override
    public boolean closing() {
        SceneApplication.getApplication().stop();
        return true;
    }

    @Override
    public void restored() {
        //start scene app
        SceneApplication.getApplication();
    }

    static {
        //set http agent
        System.setProperty("http.agent", NbBundle.getBundle("org.netbeans.core.windows.view.ui.Bundle").getString("CTL_MainWindow_Title")
                + " (" + System.getProperty("os.name") + "/" + System.getProperty("os.version") + ")");

        //select project folder
        String projectDir = NbPreferences.forModule(Installer.class).get("projects_path", null);
        if (projectDir == null) {
            javax.swing.JFileChooser fr = new javax.swing.JFileChooser();
            javax.swing.filechooser.FileSystemView fw = fr.getFileSystemView();
            projectDir = fw.getDefaultDirectory().getAbsolutePath();
            FileChooserBuilder builder = new FileChooserBuilder(projectDir);
            builder.setApproveText("Set Project Folder");
            builder.setTitle("Please select folder for storing projects");
            builder.setDirectoriesOnly(true);
            File file = builder.showOpenDialog();
            if (file != null) {
                projectDir = file.getAbsolutePath();
                NbPreferences.forModule(Installer.class).put("projects_path", projectDir);
            }
        }
        System.setProperty("netbeans.projects.dir", projectDir);

        //set extraction dir for platform natives
        String jmpDir = System.getProperty("netbeans.user");
        File file = new File(jmpDir);
        if (!file.exists()) {
            file.mkdirs();
        }
        com.jme3.system.Natives.setExtractionDir(jmpDir);

        //avoid problems with lightweight popups
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jme3.netbeans.plaf.darkmonkey;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.File;
import javax.swing.UIManager;
import org.openide.modules.ModuleInstall;

public class Installer extends ModuleInstall {

    @Override
    public void restored() {
        UIManager.installLookAndFeel(new UIManager.LookAndFeelInfo(
                new DarkMonkeyLookAndFeel().getName(),
                DarkMonkeyLookAndFeel.class.getName()));
        // TODO
        
        String[] fontsToLoad = { 
            "fonts/DejaVuSans.ttf",
            "fonts/DejaVuSans-Bold.ttf",
            "fonts/DejaVuSans-Oblique.ttf",
            "fonts/DejaVuSans-BoldOblique.ttf",
            "fonts/DejaVuSansCondensed.ttf",
            "fonts/DejaVuSansCondensed-Bold.ttf",
            "fonts/DejaVuSansCondensed-Oblique.ttf",
            "fonts/DejaVuSansCondensed-BoldOblique.ttf",
            "fonts/DejaVuSansMono.ttf",
            "fonts/DejaVuSansMono-Bold.ttf",
            "fonts/DejaVuSansMono-Oblique.ttf",
            "fonts/DejaVuSansMono-BoldOblique.ttf"
        };
        DMUtils.loadFontsFromJar(this, fontsToLoad);
    }

}

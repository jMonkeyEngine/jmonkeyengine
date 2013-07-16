/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.obfuscate;

import com.jme3.gde.core.j2seproject.ProjectExtensionManager;
import com.jme3.gde.core.j2seproject.ProjectExtensionProperties;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import javax.swing.JComponent;

import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ui.support.ProjectCustomizer;

import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

/**
 *
 * @author normenhansen
 */
@ProjectCustomizer.CompositeCategoryProvider.Registration(projectType = "org-netbeans-modules-java-j2seproject", category = "BuildCategory", position = 510)
public class ObfuscateCompositeProvider implements ProjectCustomizer.CompositeCategoryProvider {

    private static final String CAT_OBFUSCATION = "Obfuscation"; // NOI18N
    private static ProjectExtensionProperties jwsProps = null;
    private String[] keyList = new String[]{
        "obfuscate",
        "obfuscate.options"
    };
    private static String defaultOpts = "-keep public class * extends com.jme3.app.Application{public *;}\n"
            + "-keep public class * extends com.jme3.system.JmeSystemDelegate{public *;}\n"
            + "-keep public class * implements com.jme3.renderer.Renderer{public *;}\n"
            + "-keep public class * implements com.jme3.asset.AssetLoader{public *;}\n"
            + "-keep public class * implements com.jme3.asset.AssetLocator{public *;}\n"
            + "-keep public class * implements de.lessvoid.nifty.screen.ScreenController{public *;}\n"
            + "-dontwarn\n"
            + "-dontnote\n";

    public ObfuscateCompositeProvider() {
    }

    @Override
    public ProjectCustomizer.Category createCategory(Lookup context) {
        return ProjectCustomizer.Category.create(CAT_OBFUSCATION,
                NbBundle.getMessage(ObfuscateCompositeProvider.class, "LBL_Category_Obfuscate"), null);
    }

    @Override
    public JComponent createComponent(ProjectCustomizer.Category category, Lookup context) {
        jwsProps = new ProjectExtensionProperties(context.lookup(Project.class), keyList);
        if(jwsProps.getProperty("obfuscate.options")==null){
            jwsProps.setProperty("obfuscate.options", defaultOpts);
        }
        ObfuscateCustomizerPanel panel = new ObfuscateCustomizerPanel(jwsProps);
        category.setStoreListener(new SavePropsListener(jwsProps, context.lookup(Project.class)));
        category.setOkButtonListener(panel);
        return panel;
    }

    private class SavePropsListener implements ActionListener {

        private String extensionName = "obfuscate";
        private String extensionVersion = "v0.11";
        private String[] extensionDependencies = new String[]{"-post-jar", "-obfuscate"};
        private ProjectExtensionManager manager = new ProjectExtensionManager(extensionName, extensionVersion, extensionDependencies);
        private ProjectExtensionProperties properties;
        private Project project;

        public SavePropsListener(ProjectExtensionProperties props, Project project) {
            this.properties = props;
            this.project = project;
            manager.setAntTaskLibrary("pro-guard");
        }

        public void actionPerformed(ActionEvent e) {
            if ("true".equals(properties.getProperty("obfuscate"))) {
                manager.loadTargets("nbres:/com/jme3/gde/obfuscate/pro-guard-targets.xml");
                manager.checkExtension(project);
            } else {
                manager.removeExtension(project);
            }
            try {
                properties.store();
            } catch (IOException ioe) {
                Exceptions.printStackTrace(ioe);
            }
        }
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.android;

import com.jme3.gde.core.j2seproject.ProjectExtensionManager;
import com.jme3.gde.core.j2seproject.ProjectExtensionProperties;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import javax.swing.JComponent;

import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ui.support.ProjectCustomizer;
import org.openide.filesystems.FileObject;

import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

/**
 *
 * @author normenhansen
 */
@ProjectCustomizer.CompositeCategoryProvider.Registration(projectType = "org-netbeans-modules-java-j2seproject", category = "Application", position = 400)
public class MobileCompositeProvider implements ProjectCustomizer.CompositeCategoryProvider {

    private static final String CAT_MOBILE = "MobileDeployment"; // NOI18N
    private static ProjectExtensionProperties jwsProps = null;
    private String[] keyList = new String[]{
        "application.title",
        "main.class",
        "mobile.android.enabled",
        "mobile.android.package",
        "mobile.android.target"
    };

    public MobileCompositeProvider() {
    }

    @Override
    public ProjectCustomizer.Category createCategory(Lookup context) {
        return ProjectCustomizer.Category.create(CAT_MOBILE,
                NbBundle.getMessage(MobileCompositeProvider.class, "LBL_Category_Mobile"), null);
    }

    @Override
    public JComponent createComponent(ProjectCustomizer.Category category, Lookup context) {
        jwsProps = new ProjectExtensionProperties(context.lookup(Project.class), keyList);
        MobileCustomizerPanel panel = new MobileCustomizerPanel(jwsProps);
        category.setStoreListener(new SavePropsListener(jwsProps, context.lookup(Project.class)));
        category.setOkButtonListener(panel);
        return panel;
    }

    private class SavePropsListener implements ActionListener {

        private String extensionName = "mobile";
        private String extensionVersion = "v0.20";
        private String[] extensionDependencies = new String[]{"jar", "-mobile-deployment"};
        private ProjectExtensionManager manager = new ProjectExtensionManager(extensionName, extensionVersion, extensionDependencies);
        private ProjectExtensionProperties properties;
        private Project project;

        public SavePropsListener(ProjectExtensionProperties props, Project project) {
            this.properties = props;
            this.project = project;
            manager.setAntTaskLibrary("android-base");
        }

        public void actionPerformed(ActionEvent e) {
            if ("true".equals(properties.getProperty("mobile.android.enabled"))) {
                manager.loadTargets("nbres:/com/jme3/gde/android/mobile-targets.xml");
                manager.checkExtension(project);
                manager.addRunConfiguration(project, "run-android", "Android Device", "run-android", "run-android", "clean clean-android");
                AndroidSdkTool.checkProject(project,
                        properties.getProperty("mobile.android.target"),
                        properties.getProperty("application.title"),
                        "MainActivity",
                        properties.getProperty("mobile.android.package"),
                        properties.getProperty("main.class"));
            } else {
                manager.removeExtension(project);
                try {
                    FileObject folder = project.getProjectDirectory().getFileObject("mobile");
                    if (folder != null) {
                        folder.delete();
                    }
                    project.getProjectDirectory().refresh();
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
            try {
                properties.store();
            } catch (IOException ioe) {
                Exceptions.printStackTrace(ioe);
            }
        }

    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.desktop.executables;

import com.jme3.gde.core.j2seproject.ProjectExtensionManager;
import com.jme3.gde.core.j2seproject.ProjectExtensionProperties;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
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
@ProjectCustomizer.CompositeCategoryProvider.Registration(projectType = "org-netbeans-modules-java-j2seproject", category = "Application", position = 420)
public class DesktopExeCompositeProvider implements ProjectCustomizer.CompositeCategoryProvider {

    private static final String CAT_LWJGL_APPLET = "DesktopExe"; // NOI18N
    private static ProjectExtensionProperties jwsProps = null;
    private final String[] keyList = new String[]{
        "windows-x86.app.enabled",
        "windows-x64.app.enabled",
        "linux-x86.app.enabled",
        "linux-x64.app.enabled",
        "macosx-x64.app.enabled",
        "bundle.jre.enabled"
    };

    public DesktopExeCompositeProvider() {
    }

    @Override
    public ProjectCustomizer.Category createCategory(Lookup context) {
        return ProjectCustomizer.Category.create(CAT_LWJGL_APPLET,
                NbBundle.getMessage(DesktopExeCompositeProvider.class, "LBL_Category_Desktop_EXE"), null);
    }

    @Override
    public JComponent createComponent(ProjectCustomizer.Category category, Lookup context) {
        jwsProps = new ProjectExtensionProperties(context.lookup(Project.class), keyList);
        DesktopExeCustomizerPanel panel = new DesktopExeCustomizerPanel(jwsProps);
        category.setStoreListener(new SavePropsListener(jwsProps, context.lookup(Project.class)));
        category.setOkButtonListener(panel);
        return panel;
    }

    private class SavePropsListener implements ActionListener {

        private final ProjectExtensionManager desktopDeployment;
        private final ProjectExtensionProperties properties;
        private final Project project;

        public SavePropsListener(ProjectExtensionProperties props, Project project) {
            this.properties = props;
            this.project = project;
            desktopDeployment = new ProjectExtensionManager("desktop-deployment", "v1.0", new String[]{"jar", "-desktop-deployment"});
            desktopDeployment.setDataZip("nbres:/com/jme3/gde/desktop/executables/desktop-deployment-data.zip");
        }

        public void actionPerformed(ActionEvent e) {
            if ("true".equals(properties.getProperty("windows-x86.app.enabled"))
                    || "true".equals(properties.getProperty("windows-x64.app.enabled"))
                    || "true".equals(properties.getProperty("linux-x86.app.enabled"))
                    || "true".equals(properties.getProperty("linux-x64.app.enabled"))
                    || "true".equals(properties.getProperty("macosx-x64.app.enabled"))) {
                desktopDeployment.loadTargets("nbres:/com/jme3/gde/desktop/executables/desktop-deployment-targets.xml");
                desktopDeployment.checkExtension(project);
                if("true".equals(properties.getProperty("bundle.jre.enabled"))){
                    checkJreDownloads();
                }
            } else {
                desktopDeployment.removeExtension(project);
                
            }

            try {
                properties.store();
            } catch (IOException ioe) {
                Exceptions.printStackTrace(ioe);
            }
        }

        public void checkJreDownloads() {
            String projectPath = project.getProjectDirectory().getPath();
            if ("true".equals(properties.getProperty("windows-x86.app.enabled"))) {
                String jreName = projectPath + File.separator + "resources"
                        + File.separator + "desktop-deployment"
                        + File.separator + "jre-windows-x86.tar.gz";
                if (!new File(jreName).exists()) {
                    JreDownloader.downloadJre("windows-i586", jreName);
                }
            }
            if ("true".equals(properties.getProperty("windows-x64.app.enabled"))) {
                String jreName = projectPath + File.separator + "resources"
                        + File.separator + "desktop-deployment"
                        + File.separator + "jre-windows-x64.tar.gz";
                if (!new File(jreName).exists()) {
                    JreDownloader.downloadJre("windows-x64", jreName);
                }
            }
            if ("true".equals(properties.getProperty("linux-x86.app.enabled"))) {
                String jreName = projectPath + File.separator + "resources"
                        + File.separator + "desktop-deployment"
                        + File.separator + "jre-linux-x86.tar.gz";
                if (!new File(jreName).exists()) {
                    JreDownloader.downloadJre("linux-i586", jreName);
                }
            }
            if ("true".equals(properties.getProperty("linux-x64.app.enabled"))) {
                String jreName = projectPath + File.separator + "resources"
                        + File.separator + "desktop-deployment"
                        + File.separator + "jre-linux-x64.tar.gz";
                if (!new File(jreName).exists()) {
                    JreDownloader.downloadJre("linux-x64", jreName);
                }
            }
            if ("true".equals(properties.getProperty("macosx-x64.app.enabled"))) {
                String jreName = projectPath + File.separator + "resources"
                        + File.separator + "desktop-deployment"
                        + File.separator + "jre-macosx-x64.tar.gz";
                if (!new File(jreName).exists()) {
                    JreDownloader.downloadJre("macosx-x64", jreName);
                }
            }
        }

    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.desktop.executables;

import com.jme3.gde.core.j2seproject.ProjectExtensionManager;
import com.jme3.gde.core.j2seproject.ProjectExtensionProperties;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.swing.JComponent;

import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ui.support.ProjectCustomizer;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

/**
 *
 * @author normenhansen
 */
@ProjectCustomizer.CompositeCategoryProvider.Registration(projectType = "org-netbeans-modules-java-j2seproject", category = "Application", position = 400)
public class DesktopExeCompositeProvider implements ProjectCustomizer.CompositeCategoryProvider {

    private static final String CAT_LWJGL_APPLET = "DesktopExe"; // NOI18N
    private static ProjectExtensionProperties jwsProps = null;
    private String[] keyList = new String[]{
        "launch4j.exe.enabled",
        "mac.app.enabled",
        "linux.launcher.enabled"
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

        private ProjectExtensionManager launch4j;
        private ProjectExtensionManager macapp;
        private ProjectExtensionManager linux;
        private ProjectExtensionProperties properties;
        private Project project;

        public SavePropsListener(ProjectExtensionProperties props, Project project) {
            this.properties = props;
            this.project = project;
            launch4j = new ProjectExtensionManager("launch4j", "v1.1", new String[]{"jar", "-launch4j-exe"});
            launch4j.setAntTaskLibrary("launch4j");
            macapp = new ProjectExtensionManager("macapp", "v1.1", new String[]{"jar", "-mac-app"});
            linux = new ProjectExtensionManager("linuxlauncher", "v1.0", new String[]{"jar", "-linux-launcher"});
        }

        public void actionPerformed(ActionEvent e) {
            if ("true".equals(properties.getProperty("launch4j.exe.enabled"))) {
                launch4j.loadTargets("nbres:/com/jme3/gde/desktop/executables/launch4j-targets.xml");
                launch4j.checkExtension(project);
            } else {
                launch4j.removeExtension(project);
            }
            if ("true".equals(properties.getProperty("linux.launcher.enabled"))) {
                linux.loadTargets("nbres:/com/jme3/gde/desktop/executables/linux-targets.xml");
                linux.checkExtension(project);
            } else {
                linux.removeExtension(project);
            }
            if ("true".equals(properties.getProperty("mac.app.enabled"))) {
                macapp.loadTargets("nbres:/com/jme3/gde/desktop/executables/macapp-targets.xml");
                macapp.checkExtension(project);
                if (project.getProjectDirectory().getFileObject("osx-stub") == null) {
                    try {
                        unZipFile(new URL("nbres:/com/jme3/gde/desktop/executables/macapp-data.zip").openStream(), project.getProjectDirectory());
                    } catch (Exception ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }
            } else {
                macapp.removeExtension(project);
            }

            try {
                properties.store();
            } catch (IOException ioe) {
                Exceptions.printStackTrace(ioe);
            }
        }

        private void unZipFile(InputStream source, FileObject projectRoot) throws IOException {
            try {
                ZipInputStream str = new ZipInputStream(source);
                ZipEntry entry;
                while ((entry = str.getNextEntry()) != null) {
                    if (entry.isDirectory()) {
                        FileUtil.createFolder(projectRoot, entry.getName());
                    } else {
                        FileObject fo = FileUtil.createData(projectRoot, entry.getName());
                        writeFile(str, fo);
                    }
                }
            } finally {
                source.close();
            }
        }

        private void writeFile(ZipInputStream str, FileObject fo) throws IOException {
            OutputStream out = fo.getOutputStream();
            try {
                FileUtil.copy(str, out);
            } finally {
                out.close();
            }
        }
    }
}

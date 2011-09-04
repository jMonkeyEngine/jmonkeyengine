/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.lwjgl.applet;

import com.jme3.gde.core.j2seproject.ProjectExtensionManager;
import com.jme3.gde.core.j2seproject.ProjectExtensionProperties;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
@ProjectCustomizer.CompositeCategoryProvider.Registration(projectType = "org-netbeans-modules-java-j2seproject", category = "Application", position = 300)
public class LwjglAppletCompositeProvider implements ProjectCustomizer.CompositeCategoryProvider {

    private static final String CAT_LWJGL_APPLET = "LwjglApplet"; // NOI18N
    private static ProjectExtensionProperties jwsProps = null;
    private String[] keyList = new String[]{
        "lwjgl.applet.enabled",
        "lwjgl.applet.width",
        "lwjgl.applet.height"
    };

    public LwjglAppletCompositeProvider() {
    }

    @Override
    public ProjectCustomizer.Category createCategory(Lookup context) {
        return ProjectCustomizer.Category.create(CAT_LWJGL_APPLET,
                NbBundle.getMessage(LwjglAppletCompositeProvider.class, "LBL_Category_LWJGL_Applet"), null);
    }

    @Override
    public JComponent createComponent(ProjectCustomizer.Category category, Lookup context) {
        jwsProps = new ProjectExtensionProperties(context.lookup(Project.class), keyList);
        LwjglAppletCustomizerPanel panel = new LwjglAppletCustomizerPanel(jwsProps);
        category.setStoreListener(new SavePropsListener(jwsProps, context.lookup(Project.class)));
        category.setOkButtonListener(panel);
        return panel;
    }

    private class SavePropsListener implements ActionListener {

        private String extensionName = "lwjglapplet";
        private String extensionVersion = "v0.96";
        private String[] extensionDependencies = new String[]{"jar", "-lwjgl-applet"};
        private ProjectExtensionManager manager = new ProjectExtensionManager(extensionName, extensionVersion, extensionDependencies);
        private ProjectExtensionProperties properties;
        private Project project;

        public SavePropsListener(ProjectExtensionProperties props, Project project) {
            this.properties = props;
            this.project = project;
            manager.setAntTaskLibrary("lwjgl-applet");
        }

        public void actionPerformed(ActionEvent e) {
            if ("true".equals(properties.getProperty("lwjgl.applet.enabled"))) {
                manager.loadTargets("nbres:/com/jme3/gde/lwjgl/applet/lwjgl-applet-targets.xml");
                manager.checkExtension(project);
                if (project.getProjectDirectory().getFileObject("appletlogo.png") == null) {
                    try {
                        unZipFile(new URL("nbres:/com/jme3/gde/lwjgl/applet/applet-data.zip").openStream(), project.getProjectDirectory());
                    } catch (Exception ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }
            } else {
                manager.removeExtension(project);
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

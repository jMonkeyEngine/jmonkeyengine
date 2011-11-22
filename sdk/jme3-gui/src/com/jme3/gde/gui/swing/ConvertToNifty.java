package com.jme3.gde.gui.swing;

import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.gde.core.util.ProjectSelection;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.project.Project;

import org.netbeans.api.project.SourceGroup;

import org.netbeans.api.project.Sources;
import org.openide.awt.ActionRegistration;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionID;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;

@ActionID(category = "NiftyGUI",
id = "com.jme3.gde.gui.swing.ConvertToNifty")
@ActionRegistration(displayName = "#CTL_ConvertToNifty")
@ActionReferences({
    @ActionReference(path = "Menu/Tools/NiftyGUI", position = 0)
})
@Messages("CTL_ConvertToNifty=Convert Project Swing classes to NiftyGUI")
@SuppressWarnings("unchecked")
public final class ConvertToNifty implements ActionListener {

    private final Project context;

    public ConvertToNifty(Project context) {
        this.context = context;
    }

    public void actionPerformed(ActionEvent ev) {
        ProjectAssetManager pm = context.getLookup().lookup(ProjectAssetManager.class);
        if (pm == null) {
            pm = ProjectSelection.getProjectAssetManager("Select target project");
        }
        FileObject folder;
        folder = context.getProjectDirectory();
        if (pm != null) {
            try {
                folder = FileUtil.createFolder(pm.getAssetFolder(), "Interface/Converted");
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
                folder = context.getProjectDirectory();
            }
        }

        Sources sources = context.getLookup().lookup(Sources.class);
        if (sources != null) {
            List<URL> urls = new LinkedList<URL>();
            SourceGroup[] groups = sources.getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA);
            for (SourceGroup sourceGroup : groups) {
                try {
                    ClassPath path = ClassPath.getClassPath(sourceGroup.getRootFolder(), ClassPath.COMPILE);
                    for (Iterator<ClassPath.Entry> it = path.entries().iterator(); it.hasNext();) {
                        ClassPath.Entry entry = it.next();
                        urls.add(entry.getURL());
                    }
                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
            for (SourceGroup sourceGroup : groups) {
                ClassLoader loader = new URLClassLoader(urls.toArray(new URL[urls.size()]), this.getClass().getClassLoader());
                try {
                    Class<?> clazzFactory = loader.loadClass("nl.tygron.niftyconverter.NiftyFactory");
                    Class<?> clazzFile = loader.loadClass("java.io.File");
                    Class<?> clazzString = loader.loadClass("java.lang.String");
                    Class<?> clazzConfig = loader.loadClass("nl.tygron.niftyconverter.util.NiftyConverterConfig");
                    clazzConfig.getDeclaredMethod("setOutputDir", clazzString).invoke(null, folder.getPath());
                    Object string = clazzString.getDeclaredConstructor(clazzString).newInstance(sourceGroup.getRootFolder().getPath());//context.getProjectDirectory().getFileObject("build/classes").getPath());
                    Object file = clazzFile.getDeclaredConstructor(clazzString).newInstance(string);
                    Object factory = clazzFactory.newInstance();
                    clazzFactory.getMethod("loadComponents", clazzFile).invoke(factory, file);
                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }

    }
}

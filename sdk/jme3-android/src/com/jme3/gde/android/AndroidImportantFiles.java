/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.android;

import com.jme3.gde.core.importantfiles.ImportantFiles;
import java.io.InputStream;
import java.util.ArrayList;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.xml.XMLUtil;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 *
 * @author normenhansen
 */
@org.openide.util.lookup.ServiceProvider(service = ImportantFiles.class)
public class AndroidImportantFiles implements ImportantFiles {

    @Override
    public FileObject[] getFiles(Project project) {
        FileObject manifest = project.getProjectDirectory().getFileObject("mobile/AndroidManifest.xml");
        String mainActivity = "mobile/src";
        if (manifest != null) {
            InputStream in = null;
            try {
                in = manifest.getInputStream();
                Document configuration = XMLUtil.parse(new InputSource(in), false, false, null, null);
                mainActivity = "mobile/src/" + configuration.getDocumentElement().getAttribute("package").replaceAll("\\.", "/") + "/MainActivity.java";
                in.close();
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            } finally {
            }
        }
        ArrayList<FileObject> list = new ArrayList<FileObject>();
        FileObject mainAct = project.getProjectDirectory().getFileObject(mainActivity);
        if (mainAct != null) {
            list.add(mainAct);
        }
        FileObject manif = project.getProjectDirectory().getFileObject("mobile/AndroidManifest.xml");
        if (manif != null) {
            list.add(manif);
        }
        FileObject buildProp = project.getProjectDirectory().getFileObject("mobile/ant.properties");
        if (buildProp != null) {
            list.add(buildProp);
        }
        return list.toArray(new FileObject[list.size()]);
    }
}

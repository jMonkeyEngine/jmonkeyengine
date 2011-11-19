/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.core.j2seproject;

import com.jme3.gde.core.importantfiles.ImportantFiles;
import java.util.ArrayList;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;

/**
 *
 * @author normenhansen
 */
@org.openide.util.lookup.ServiceProvider(service = ImportantFiles.class)
public class J2seImportantFiles implements ImportantFiles{

    @Override
    public FileObject[] getFiles(Project project) {
        ArrayList<FileObject> list = new ArrayList<FileObject>();
//        list.add(project.getProjectDirectory().getFileObject("nbproject/project.properties"));
        list.add(project.getProjectDirectory().getFileObject("build.xml"));
        return list.toArray(new FileObject[list.size()]);
    }
    
}

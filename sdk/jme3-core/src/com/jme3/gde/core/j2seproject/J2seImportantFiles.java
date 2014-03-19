/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.core.j2seproject;

import com.jme3.gde.core.importantfiles.ImportantFiles;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;

/**
 *
 * @author normenhansen
 */
@org.openide.util.lookup.ServiceProvider(service = ImportantFiles.class)
public class J2seImportantFiles implements ImportantFiles {

    public Node[] getNodes(Project proj) {
        FileObject obj = proj.getProjectDirectory().getFileObject("build.xml");
        if (obj == null) {
            return new Node[]{Node.EMPTY};
        }
        Node[] nodes = new Node[1];
        try {
            nodes[0] = DataObject.find(obj).getNodeDelegate();
            nodes[0].setDisplayName("Build File");
        } catch (DataObjectNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        }
        return nodes;
    }

    public boolean hasFiles(Project proj) {
        if (proj.getProjectDirectory().getFileObject("build.xml") != null) {
            return true;
        }
        return false;
    }
}

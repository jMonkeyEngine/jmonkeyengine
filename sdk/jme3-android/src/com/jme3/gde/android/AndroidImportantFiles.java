/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.android;

import com.jme3.gde.core.importantfiles.ImportantFiles;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.Node;
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
    public Node[] getNodes(Project project) {
        FileObject manifest = project.getProjectDirectory().getFileObject("mobile/AndroidManifest.xml");
        String mainActivity = "mobile/src";
        if (manifest != null) {
            InputStream in = null;
            try {
                in = manifest.getInputStream();
                Document configuration = XMLUtil.parse(new InputSource(in), false, false, null, null);
                mainActivity = "mobile/src/" + configuration.getDocumentElement().getAttribute("package").replaceAll("\\.", "/") + "/MainActivity.java";
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
        ArrayList<Node> list = new ArrayList<Node>();
        try {
            FileObject mainAct = project.getProjectDirectory().getFileObject(mainActivity);
            if (mainAct != null) {
                Node node = DataObject.find(mainAct).getNodeDelegate();
                node.setDisplayName("Android Main Activity");
                list.add(node);
            }
            FileObject manif = project.getProjectDirectory().getFileObject("mobile/AndroidManifest.xml");
            if (manif != null) {
                Node node = DataObject.find(manif).getNodeDelegate();
                node.setDisplayName("Android Manifest");
                list.add(node);
            }
            FileObject buildProp = project.getProjectDirectory().getFileObject("mobile/ant.properties");
            if (buildProp != null) {
                Node node = DataObject.find(buildProp).getNodeDelegate();
                node.setDisplayName("Android Properties");
                list.add(node);
            }
        } catch (DataObjectNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        }
        return list.toArray(new Node[list.size()]);
    }

    @Override
    public boolean hasFiles(Project proj) {
        if (proj.getProjectDirectory().getFileObject("mobile/AndroidManifest.xml") != null) {
            return true;
        }
        return false;
    }
}

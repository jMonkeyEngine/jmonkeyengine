/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.android;

import java.awt.Image;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.LookupProvider;
import org.netbeans.spi.project.ui.support.NodeFactory;
import org.netbeans.spi.project.ui.support.NodeFactorySupport;
import org.netbeans.spi.project.ui.support.NodeList;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.xml.XMLUtil;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 *
 * @author normenhansen
 */
public class ImportantFilesNode extends AbstractNode {

    private static Image smallImage =
            ImageUtilities.loadImage("com/jme3/gde/android/properties/Phone_16.gif");

    public ImportantFilesNode(Project proj) throws DataObjectNotFoundException {
        super(new ImportantFilesChildren(proj));
    }

    @Override
    public String getDisplayName() {
        return "Android Files";
    }

    @Override
    public Image getIcon(int type) {
        return smallImage;
    }

    @Override
    public Image getOpenedIcon(int type) {
        return smallImage;
    }

    public static class LookupProviderImpl implements LookupProvider {

        public Lookup createAdditionalLookup(Lookup lookup) {

            Project prj = lookup.lookup(Project.class);

            //create node if lookup has important files node
            FileObject folder = prj.getProjectDirectory().getFileObject("mobile");
            if (folder != null && folder.isFolder()) {
                return Lookups.fixed(new ImportantFilesLookupItem(prj));
            }

            return Lookups.fixed();

        }
    }

    public static class ImportantFilesNodeFactoryImpl implements NodeFactory {

        public NodeList createNodes(Project project) {

//        this.proj = project;

            //If our item is in the project's lookup,
            //return a new node in the node list:
            ImportantFilesLookupItem item = project.getLookup().lookup(ImportantFilesLookupItem.class);
            if (item != null) {
                try {
                    ImportantFilesNode nd = new ImportantFilesNode(project);
                    return NodeFactorySupport.fixedNodeList(nd);
                } catch (DataObjectNotFoundException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }

            //If our item isn't in the lookup,
            //then return an empty list of nodes:
            return NodeFactorySupport.fixedNodeList();

        }
    }

    public static class ImportantFilesLookupItem {

        public ImportantFilesLookupItem(Project prj) {
        }
    }

    public static class ImportantFilesChildren extends Children.Keys<FileObject> {

        private Project project;

        public ImportantFilesChildren(Project project) {
            this.project = project;
        }

        protected List<FileObject> createKeys() {
//            package="com.mycompany.mygame"
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
            addFileObject(project.getProjectDirectory().getFileObject(mainActivity), list);
            addFileObject(project.getProjectDirectory().getFileObject("mobile/AndroidManifest.xml"), list);
            addFileObject(project.getProjectDirectory().getFileObject("mobile/build.properties"), list);
            return list;
        }

        private void addFileObject(FileObject file, List<FileObject> list) {
            if (file != null) {
                list.add(file);
            }
        }

        @Override
        protected void addNotify() {
            super.addNotify();
            setKeys(createKeys());
        }

        @Override
        protected Node[] createNodes(FileObject key) {
            try {
                DataObject obj = DataObject.find(key);
                return new Node[]{obj.getNodeDelegate()};
            } catch (DataObjectNotFoundException ex) {
                Exceptions.printStackTrace(ex);
            }
            return new Node[]{};
        }
    }
}
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.android;

import org.netbeans.api.project.Project;
import org.netbeans.spi.project.LookupProvider;
import org.netbeans.spi.project.ui.support.NodeFactory;
import org.netbeans.spi.project.ui.support.NodeFactorySupport;
import org.netbeans.spi.project.ui.support.NodeList;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author normenhansen
 */
public class ImportantFilesNode extends FilterNode {

    public ImportantFilesNode(Project proj) throws DataObjectNotFoundException {
//        super(DataObject.find(proj.getProjectDirectory().getFileObject("mobile/AndroidManifest.xml")).getNodeDelegate());
        super(Node.EMPTY);
    }

    @Override
    public String getDisplayName() {
        return "Android Manifest";
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
}
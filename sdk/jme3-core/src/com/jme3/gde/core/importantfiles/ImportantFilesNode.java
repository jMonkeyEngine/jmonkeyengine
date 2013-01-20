/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.core.importantfiles;

import com.jme3.gde.core.icons.IconList;
import java.awt.Image;
import java.util.ArrayList;
import java.util.List;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ui.support.NodeFactory;
import org.netbeans.spi.project.ui.support.NodeFactorySupport;
import org.netbeans.spi.project.ui.support.NodeList;
import org.openide.filesystems.FileAttributeEvent;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileRenameEvent;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

/**
 *
 * @author normenhansen
 */
public class ImportantFilesNode extends AbstractNode implements FileChangeListener {

    private static Image smallImage = IconList.important.getImage();

    public ImportantFilesNode(Project proj) throws DataObjectNotFoundException {
        super(new ImportantFilesChildren(proj));
        proj.getProjectDirectory().addRecursiveListener(this);
    }

    @Override
    public String getDisplayName() {
        return "Important Files";
    }

    @Override
    public Image getIcon(int type) {
        return smallImage;
    }

    @Override
    public Image getOpenedIcon(int type) {
        return smallImage;
    }

    public void fileFolderCreated(FileEvent fe) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                ((ImportantFilesChildren) getChildren()).addNotify();
            }
        });
    }

    public void fileDataCreated(FileEvent fe) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                ((ImportantFilesChildren) getChildren()).addNotify();
            }
        });
    }

    public void fileChanged(FileEvent fe) {
    }

    public void fileDeleted(FileEvent fe) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                ((ImportantFilesChildren) getChildren()).addNotify();
            }
        });
    }

    public void fileRenamed(FileRenameEvent fre) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                ((ImportantFilesChildren) getChildren()).addNotify();
            }
        });
    }

    public void fileAttributeChanged(FileAttributeEvent fae) {
    }

//    public static class LookupProviderImpl implements LookupProvider {
//
//        public Lookup createAdditionalLookup(Lookup lookup) {
//
//            Project prj = lookup.lookup(Project.class);
//
//            return Lookups.fixed(new ImportantFilesLookupItem(prj));
//        }
//    }
    public static class ImportantFilesNodeFactoryImpl implements NodeFactory {

        public NodeList createNodes(Project project) {

//            ImportantFilesLookupItem item = project.getLookup().lookup(ImportantFilesLookupItem.class);
//            if (item != null) {
            try {
                ImportantFilesNode nd = new ImportantFilesNode(project);
                return NodeFactorySupport.fixedNodeList(nd);
            } catch (DataObjectNotFoundException ex) {
                Exceptions.printStackTrace(ex);
            }
//            }

            return NodeFactorySupport.fixedNodeList();
        }
    }

//    public static class ImportantFilesLookupItem {
//
//        public ImportantFilesLookupItem(Project prj) {
//        }
//    }
    public static class ImportantFilesChildren extends Children.Keys<ImportantFiles> {

        private Project project;

        public ImportantFilesChildren(Project project) {
            this.project = project;
        }

        protected List<ImportantFiles> createKeys() {
            ArrayList<ImportantFiles> list = new ArrayList<ImportantFiles>();
            for (ImportantFiles di : Lookup.getDefault().lookupAll(ImportantFiles.class)) {
                if (di.hasFiles(project)) {
                    list.add(di);
                }
            }
            return list;
        }

        @Override
        protected void addNotify() {
            super.addNotify();
            setKeys(createKeys());
        }

        @Override
        protected Node[] createNodes(ImportantFiles key) {
            Node[] nodes = key.getNodes(project);
            return nodes;
        }
    }
}
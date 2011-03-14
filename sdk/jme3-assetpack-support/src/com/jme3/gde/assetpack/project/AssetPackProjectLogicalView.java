package com.jme3.gde.assetpack.project;

import com.jme3.gde.assetpack.project.actions.PublishAssetPackAction;
import com.jme3.gde.assetpack.browser.nodes.AssetPackBrowserFolder;
import com.jme3.gde.assetpack.project.actions.ConvertOgreBinaryMeshesAction;
import java.awt.Image;
import java.util.LinkedList;
import java.util.List;
import javax.swing.Action;
import org.netbeans.spi.project.ui.LogicalViewProvider;
import org.netbeans.spi.project.ui.support.CommonProjectActions;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.w3c.dom.Element;

class AssetPackProjectLogicalView implements LogicalViewProvider {

    private final AssetPackProject project;

    public AssetPackProjectLogicalView(AssetPackProject project) {
        this.project = project;
    }

    @Override
    public org.openide.nodes.Node createLogicalView() {

        try {
            return new ProjectNode(project);
        } catch (DataObjectNotFoundException donfe) {
            Exceptions.printStackTrace(donfe);
            //Fallback—the directory couldn't be created -
            //read-only filesystem or something evil happened
            return new AbstractNode(Children.LEAF);
        }
    }

    /** This is the node you actually see in the project tab for the project */
    private static final class ProjectNode extends AbstractNode {

        private InstanceContent instanceContent;
        final AssetPackProject project;

        public ProjectNode(AssetPackProject project) throws DataObjectNotFoundException {
            super(new ProjectChildren(project), new ProjectLookup(new InstanceContent()));
            this.project = project;
            instanceContent = ((ProjectLookup) getLookup()).getInstanceContent();
            instanceContent.add(project);
            instanceContent.add(project.getProjectAssetManager());
//            instanceContent.add(project.getProjectCustomizer());
        }

        @Override
        public Action[] getActions(boolean arg0) {
            Action[] nodeActions = new Action[8];
            nodeActions[0] = new PublishAssetPackAction(project);
            nodeActions[1] = new ConvertOgreBinaryMeshesAction(project);
            nodeActions[2] = CommonProjectActions.copyProjectAction();
            nodeActions[3] = CommonProjectActions.deleteProjectAction();
            nodeActions[5] = CommonProjectActions.setAsMainProjectAction();
            nodeActions[6] = CommonProjectActions.closeProjectAction();
            nodeActions[7] = CommonProjectActions.customizeProjectAction();
            return nodeActions;
        }

        @Override
        public Image getIcon(int type) {
            return ImageUtilities.loadImage("com/jme3/gde/assetpack/icons/assetpack.png");
        }

        @Override
        public Image getOpenedIcon(int type) {
            return getIcon(type);
        }

        @Override
        public String getDisplayName() {
            return project.getProjectName();
        }
    }

    public static final class ProjectLookup extends AbstractLookup {

        private static final long serialVersionUID = 1214314412L;
        private InstanceContent instanceContent;

        public ProjectLookup(InstanceContent instanceContent) {
            super(instanceContent);
            this.instanceContent = instanceContent;
            instanceContent.add(this);
        }

        public InstanceContent getInstanceContent() {
            return instanceContent;
        }
    }

    private static final class ProjectChildren extends Children.Keys<Object> {

        AssetPackProject project;
        Node node;

        public ProjectChildren(AssetPackProject project) {
            this.project = project;
            node = project.getAssetPackFolder();
        }

        @Override
        protected void addNotify() {
            super.addNotify();
            setKeys(createKeys());
        }

        protected List<Object> createKeys() {
            LinkedList<Object> ret = new LinkedList<Object>();
            ret.add(new Object());
            return ret;
        }

        @Override
        protected Node[] createNodes(Object key) {
            if (node == null) {
//                try {
//                    ProjectAssetManager manager = project.getProjectAssetManager();
//                    FileObject assets = project.getAssetsFolder();
//                    node = new ProjectAssetsNode(manager, project, DataFolder.find(assets).getNodeDelegate());
//                } catch (DataObjectNotFoundException ex) {
//                    Exceptions.printStackTrace(ex);
//                    node = new AbstractNode(Children.LEAF);
//                    node.setDisplayName("error");
//                }
                node = project.setAssetPackFolder(new AssetPackBrowserFolder(new Element[]{project.getProjectAssets()}, project));
            }
            return new Node[]{node};
        }
    }

    @Override
    public Node findPath(Node root, Object target) {
        //leave unimplemented for now
        return null;
    }
}

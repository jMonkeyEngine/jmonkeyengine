/*
 *  Copyright (c) 2009-2010 jMonkeyEngine
 *  All rights reserved.
 * 
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are
 *  met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 
 *  * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *  TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 *  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.gde.core.sceneexplorer.nodes;

import com.jme3.asset.ModelKey;
import com.jme3.gde.core.icons.IconList;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.gde.core.sceneexplorer.nodes.actions.AddUserDataAction;
import com.jme3.gde.core.sceneexplorer.nodes.actions.NewControlPopup;
import com.jme3.scene.AssetLinkNode;
import java.awt.Image;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import javax.swing.Action;
import org.openide.actions.CopyAction;
import org.openide.actions.CutAction;
import org.openide.actions.DeleteAction;
import org.openide.actions.PasteAction;
import org.openide.actions.RenameAction;
import org.openide.awt.Actions;
import org.openide.loaders.DataObject;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.Sheet;
import org.openide.util.Exceptions;
import org.openide.util.actions.SystemAction;

/**
 *
 * @author normenhansen
 */
@org.openide.util.lookup.ServiceProvider(service = SceneExplorerNode.class)
public class JmeAssetLinkNode extends JmeNode {

    private static Image smallImage = IconList.link.getImage();
    private AssetLinkNode geom;
    private AssetLinkChildren linkChildren;

    public JmeAssetLinkNode() {
    }

    public JmeAssetLinkNode(AssetLinkNode spatial, JmeSpatialChildren children) {
        super(spatial, new AssetLinkChildren(spatial));
        getLookupContents().add(spatial);
        linkChildren = (AssetLinkChildren)getChildren();
        linkChildren.setReadOnly(children.readOnly);
        this.geom = spatial;
     //   setName(spatial.getName());
    }

    @Override
    public Image getIcon(int type) {
        return smallImage;
    }

    @Override
    public Image getOpenedIcon(int type) {
        return smallImage;
    }

    @Override
    protected Sheet createSheet() {
        Sheet sheet = super.createSheet();
        Sheet.Set set = Sheet.createPropertiesSet();
        set.setDisplayName("AssetLinkNode");
        set.setName(AssetLinkNode.class.getName());
        AssetLinkNode obj = geom;//getLookup().lookup(Spatial.class);
        if (obj == null) {
            return sheet;
        }

        sheet.put(set);
        return sheet;

    }

    @Override
    public Action[] getActions(boolean context) {
        if (linkChildren.readOnly) {
            return new Action[]{
                        SystemAction.get(CopyAction.class),};
        } else {
            return new Action[]{
                        new NewControlPopup(this),
                        Actions.alwaysEnabled(new AddUserDataAction(this), "Add User Data", "", false),
                        SystemAction.get(RenameAction.class),
                        SystemAction.get(CopyAction.class),
                        SystemAction.get(CutAction.class),
                        SystemAction.get(PasteAction.class),
                        SystemAction.get(DeleteAction.class)
                    };
        }
    }

    @Override
    public Class getExplorerObjectClass() {
        return AssetLinkNode.class;
    }

    @Override
    public Class getExplorerNodeClass() {
        return JmeAssetLinkNode.class;
    }

    @Override
    public org.openide.nodes.Node[] createNodes(Object key, DataObject key2, boolean cookie) {
        JmeSpatialChildren children = new JmeSpatialChildren((com.jme3.scene.Spatial) key);
        children.setReadOnly(cookie);
        children.setDataObject(key2);
        return new org.openide.nodes.Node[]{new JmeAssetLinkNode((AssetLinkNode) key, children).setReadOnly(cookie)};
    }

    public static class AssetLinkChildren extends JmeSpatialChildren {

        public AssetLinkChildren(AssetLinkNode spatial) {
            super(spatial);
        }

        @Override
        public void refreshChildren(boolean immediate) {
            setKeys(createKeys());
            refresh();
        }

        @Override
        protected List<Object> createKeys() {
            try {
                return SceneApplication.getApplication().enqueue(new Callable<List<Object>>() {

                    public List<Object> call() throws Exception {
                        List<Object> keys = new LinkedList<Object>();
                        if (spatial instanceof AssetLinkNode) {
                            keys.addAll(((AssetLinkNode) spatial).getAssetLoaderKeys());
                            return keys;
                        }
                        return keys;
                    }
                }).get();
            } catch (InterruptedException ex) {
                Exceptions.printStackTrace(ex);
            } catch (ExecutionException ex) {
                Exceptions.printStackTrace(ex);
            }
            return null;
        }

        @Override
        public void setReadOnly(boolean cookie) {
            this.readOnly = cookie;
        }

        @Override
        protected void addNotify() {
            super.addNotify();
            setKeys(createKeys());
        }

        @Override
        protected Node[] createNodes(Object key) {
            if (key instanceof ModelKey) {
                ModelKey assetKey = (ModelKey) key;
                return new Node[]{new JmeAssetLinkChild(assetKey, (AssetLinkNode) spatial)};
            }
            return null;
        }
    }

    public static class JmeAssetLinkChild extends AbstractNode {

        private ModelKey key;
        private AssetLinkNode linkNode;

        public JmeAssetLinkChild(ModelKey key, AssetLinkNode linkNode) {
            super(Children.LEAF);
            this.key = key;
            this.linkNode = linkNode;
            this.setName(key.getName());
        }

        @Override
        public Action[] getActions(boolean context) {
            return new SystemAction[]{
                        SystemAction.get(DeleteAction.class)
                    };
        }

        @Override
        public boolean canDestroy() {
            return true;
        }

        @Override
        public void destroy() throws IOException {
            super.destroy();
            try {
                SceneApplication.getApplication().enqueue(new Callable<Void>() {

                    public Void call() throws Exception {
                        linkNode.detachLinkedChild(key);
                        return null;
                    }
                }).get();
                JmeSpatial node = ((JmeSpatial) getParentNode());
                if (node != null) {
                    node.refresh(false);
                }
            } catch (InterruptedException ex) {
                Exceptions.printStackTrace(ex);
            } catch (ExecutionException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }
}

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

import com.jme3.animation.Bone;
import com.jme3.gde.core.icons.IconList;
import com.jme3.gde.core.scene.SceneApplication;
import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import javax.swing.Action;
import org.openide.awt.Actions;
import org.openide.loaders.DataObject;
import org.openide.nodes.Sheet;
import org.openide.util.Exceptions;

/**
 *
 * @author normenhansen
 */
@org.openide.util.lookup.ServiceProvider(service = SceneExplorerNode.class)
public class JmeBone extends AbstractSceneExplorerNode {

    private static Image smallImage = IconList.bone.getImage();
    private Bone bone;
    private JmeSkeletonControl jmeSkeletonControl;
    protected final DataFlavor BONE_FLAVOR = new DataFlavor(ClipboardSpatial.class, "Bone");

    public JmeBone() {
    }

    public JmeBone(JmeSkeletonControl jmeSkeletonControl, Bone bone, JmeBoneChildren children) {
        super(children);
        this.jmeChildren = children;
        this.jmeSkeletonControl = jmeSkeletonControl;
        getLookupContents().add(bone);
        getLookupContents().add(this);
        super.setName(bone.getName());
        this.bone = bone;
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
        //TODO: multithreading..
        Sheet sheet = super.createSheet();
        Sheet.Set set = Sheet.createPropertiesSet();
        set.setDisplayName("Bone");
        set.setName(Bone.class.getName());
        Bone obj = bone;//getLookup().lookup(Spatial.class);
        if (obj == null) {
            return sheet;
        }
        sheet.put(set);
        return sheet;

    }

    @Override
    public Action[] getActions(boolean context) {

        return new Action[]{
                    Actions.alwaysEnabled(new AttachementNodeActionListener(), "Get attachement Node", "", false),};
    }

    private class AttachementNodeActionListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            fireSave(true);
            try {
                SceneApplication.getApplication().enqueue(new Callable<Void>() {

                    public Void call() throws Exception {
                        jmeSkeletonControl.getSkeletonControl().getAttachmentsNode(bone.getName());
                        return null;
                    }
                }).get();
                ((AbstractSceneExplorerNode) jmeSkeletonControl.getParentNode()).refresh(false);
            } catch (InterruptedException ex) {
                Exceptions.printStackTrace(ex);
            } catch (ExecutionException ex) {
                Exceptions.printStackTrace(ex);
            }

        }
    }

    public Class getExplorerObjectClass() {
        return Bone.class;
    }

    @Override
    public Class getExplorerNodeClass() {
        return JmeBone.class;
    }

    @Override
    public org.openide.nodes.Node[] createNodes(Object key, DataObject key2, boolean cookie) {
        JmeBoneChildren children = new JmeBoneChildren(jmeSkeletonControl, (Bone) key);
        children.setReadOnly(cookie);
        children.setDataObject(key2);
        return new org.openide.nodes.Node[]{new JmeBone(jmeSkeletonControl, (Bone) key, children).setReadOnly(cookie)};
    }
}

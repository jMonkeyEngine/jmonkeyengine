/*
 * Copyright (c) 2009-2010 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.gde.core.assets.actions;

import com.jme3.asset.DesktopAssetManager;
import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.gde.core.assets.SpatialAssetDataObject;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.gde.core.scene.SceneRequest;
import com.jme3.gde.core.sceneexplorer.nodes.JmeNode;
import com.jme3.gde.core.sceneexplorer.nodes.NodeUtility;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.NotifyDescriptor.Confirmation;

public final class OpenModel implements ActionListener {

    private final SpatialAssetDataObject context;

    public OpenModel(SpatialAssetDataObject context) {
        this.context = context;
    }

    public void actionPerformed(ActionEvent ev) {
        final ProjectAssetManager manager = context.getLookup().lookup(ProjectAssetManager.class);
        if (manager == null) {
            return;
        }
        Runnable call = new Runnable() {

            public void run() {
                ProgressHandle progressHandle = ProgressHandleFactory.createHandle("Opening Model");
                progressHandle.start();
                try {
                    manager.clearCache();

                    final Spatial asset = context.loadAsset();

                    if (asset != null) {
                        Node node = null;
                        if (asset instanceof Node) {
                            node = (Node) asset;
                        } else {
                            node = new Node("RootNode");
                            node.attachChild(asset);
                        }
                        JmeNode jmeNode = NodeUtility.createNode(node);
                        SceneApplication app = SceneApplication.getApplication();
                        SceneRequest request = new SceneRequest(app, jmeNode, manager);
                        request.setWindowTitle("OpenGL Window - View Model");
                        app.requestScene(request);
                    } else {
                        Confirmation msg = new NotifyDescriptor.Confirmation(
                                "Error opening " + context.getPrimaryFile().getNameExt(),
                                NotifyDescriptor.OK_CANCEL_OPTION,
                                NotifyDescriptor.ERROR_MESSAGE);
                        DialogDisplayer.getDefault().notify(msg);
                    }
                }catch(Exception e){
                    e.printStackTrace();
                }finally {
                    progressHandle.finish();
                }
            }
        };
        new Thread(call).start();

    }
}

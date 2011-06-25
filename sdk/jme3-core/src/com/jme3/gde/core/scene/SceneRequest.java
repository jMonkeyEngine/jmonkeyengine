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
package com.jme3.gde.core.scene;

import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.gde.core.sceneexplorer.nodes.JmeNode;
import com.jme3.gde.core.sceneexplorer.nodes.NodeUtility;
import org.openide.loaders.DataObject;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author normenhansen
 */
public class SceneRequest {

    private String windowTitle = "";
    private Object requester;
    private JmeNode jmeNode;
    private com.jme3.scene.Spatial rootNode;
    private com.jme3.scene.Node toolNode;
    private ProjectAssetManager manager;
    private boolean displayed = false;
    private DataObject dataObject;
    private HelpCtx helpCtx;

    public SceneRequest(Object requester, JmeNode rootNode, ProjectAssetManager manager) {
        this.requester = requester;
        this.jmeNode = rootNode;
        this.rootNode = rootNode.getLookup().lookup(com.jme3.scene.Node.class);
        this.manager = manager;
    }

    public SceneRequest(Object requester, com.jme3.scene.Spatial rootNode, ProjectAssetManager manager) {
        this.requester = requester;
        this.rootNode = rootNode;
        if (rootNode instanceof com.jme3.scene.Node) {
            this.jmeNode = NodeUtility.createNode((com.jme3.scene.Node) rootNode, true);
        }
        this.manager = manager;
    }

    /**
     * returns the lookup of the root node
     * @return
     */
    public Lookup getLookup() {
        if (jmeNode != null) {
            return jmeNode.getLookup();
        } else {
            return Lookups.singleton(new Object());
        }
    }

    /**
     * @return the windowTitle
     */
    public String getWindowTitle() {
        return windowTitle;
    }

    /**
     * @param windowTitle the windowTitle to set in the SceneViewer window
     */
    public void setWindowTitle(String windowTitle) {
        this.windowTitle = windowTitle;
    }

    /**
     * @return the requester
     */
    public Object getRequester() {
        return requester;
    }

    /**
     * @return the rootNode
     */
    public JmeNode getJmeNode() {
        return jmeNode;
    }

    public com.jme3.scene.Spatial getRootNode() {
        return rootNode;
    }

    /**
     * @return the displayed status
     */
    public boolean isDisplayed() {
        return displayed;
    }

    /**
     * @param displayed the displayed to set
     */
    public void setDisplayed(boolean displayed) {
        this.displayed = displayed;
    }

    /**
     * @return the projectassetmanager (deprecated, use lookup)
     */
    public ProjectAssetManager getManager() {
        return manager;
    }

    /**
     * @return the toolScene
     */
    public com.jme3.scene.Node getToolNode() {
        return toolNode;
    }

    /**
     * Add an additional Node that is not displayed in the SceneExplorer and can be
     * used for displaying in-world tools, templates, previews etc.
     * @param toolScene the toolScene to set
     */
    public void setToolNode(com.jme3.scene.Node toolNode) {
        this.toolNode = toolNode;
    }

    public DataObject getDataObject() {
        return dataObject;
    }

    /**
     * sets the DataObject associated with this scene
     * @param dataObject
     */
    public void setDataObject(DataObject dataObject) {
        this.dataObject = dataObject;
    }

    public HelpCtx getHelpCtx() {
        return helpCtx;
    }

    /**
     * Set the help context for the SceneViewer window
     * @param helpCtx
     */
    public void setHelpCtx(HelpCtx helpCtx) {
        this.helpCtx = helpCtx;
    }
}

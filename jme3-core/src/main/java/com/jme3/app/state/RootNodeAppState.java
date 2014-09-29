/*
 * Copyright (c) 2009-2014 jMonkeyEngine
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
package com.jme3.app.state;

import com.jme3.app.Application;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;

/**
 * AppState that manages and updates a RootNode attached to a ViewPort, the
 * default Application ViewPort is used by default, a RootNode is created by
 * default.
 * @author normenhansen
 */
public class RootNodeAppState extends AbstractAppState {

    protected ViewPort viewPort;
    protected Node rootNode;

    /**
     * Creates the AppState with a new, empty root Node, attaches it to the
     * default Application ViewPort and updates it when attached to the
     * AppStateManager.
     */
    public RootNodeAppState() {
    }

    /**
     * Creates the AppState with the given ViewPort and creates a RootNode
     * that is attached to the given ViewPort and updates it when attached to the
     * AppStateManager.
     * @param viewPort An existing ViewPort
     */
    public RootNodeAppState(ViewPort viewPort) {
        this.viewPort = viewPort;
    }

    /**
     * Creates the AppState with the given root Node, uses the default
     * Application ViewPort and updates the root Node when attached to the
     * AppStateManager.
     * @param rootNode An existing root Node
     */
    public RootNodeAppState(Node rootNode) {
        this.rootNode = rootNode;
    }

    /**
     * Creates the AppState with the given ViewPort and root Node, attaches
     * the root Node to the ViewPort and updates it.
     * @param viewPort An existing ViewPort
     * @param rootNode An existing root Node
     */
    public RootNodeAppState(ViewPort viewPort, Node rootNode) {
        this.viewPort = viewPort;
        this.rootNode = rootNode;
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        if (rootNode == null) {
            rootNode = new Node("Root Node");
        }
        if (viewPort == null) {
            viewPort = app.getViewPort();
        }
        viewPort.attachScene(rootNode);
        super.initialize(stateManager, app);
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);
        rootNode.updateLogicalState(tpf);
        rootNode.updateGeometricState();
    }

    @Override
    public void cleanup() {
        viewPort.detachScene(rootNode);
        super.cleanup();
    }

    /**
     * Returns the managed rootNode.
     * @return The managed rootNode
     */
    public Node getRootNode() {
        return rootNode;
    }

    /**
     * Returns the used ViewPort
     * @return The used ViewPort
     */
    public ViewPort getViewPort() {
        return viewPort;
    }

}

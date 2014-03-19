/*
 * Copyright (c) 2003-2012 jMonkeyEngine
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
package com.jme3.gde.core.appstates;

import com.jme3.app.state.AppState;
import com.jme3.gde.core.scene.FakeApplication.FakeAppStateManager;
import java.util.LinkedList;
import java.util.List;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;

/**
 *
 * @author normenhansen
 */
public class AppStateManagerNode extends AbstractNode {

//    private static Image smallImage =
//            ImageUtilities.loadImage("com/jme3/gde/core/filters/icons/eye.gif");

    public AppStateManagerNode(FakeAppStateManager manager) {
        super(new AppStateChildren(manager));
        setName("Loaded App States");
        manager.setNode(this);
    }

//    @Override
//    public Image getIcon(int type) {
//        return smallImage;
//    }
//
//    @Override
//    public Image getOpenedIcon(int type) {
//        return smallImage;
//    }

    public void refresh() {
        //TODO: external refresh
        ((AppStateChildren) getChildren()).doRefresh();
    }

    public static class AppStateChildren extends Children.Keys<Object> {

        private final FakeAppStateManager manager;

        public AppStateChildren(FakeAppStateManager manager) {
            this.manager = manager;
        }

        @Override
        protected void addNotify() {
            super.addNotify();
            setKeys(createKeys());
        }

        protected void doRefresh() {
            addNotify();
            refresh();
        }

        protected void reorderNotify() {
            setKeys(createKeys());
        }

        protected List<Object> createKeys() {
            List<Object> keys = new LinkedList<Object>();
            List<AppState> states;
            states = manager.getAddedStates();
            keys.addAll(states);
            return keys;
        }

        @Override
        protected Node[] createNodes(Object t) {
            AppStateNode node = new AppStateNode((AppState) t, manager);
            return new Node[]{node};
        }
    }
}

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
package com.jme3.gde.core.filters;

import com.jme3.gde.core.assets.FilterDataObject;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.post.Filter;
import com.jme3.post.FilterPostProcessor;
import java.awt.Image;
import java.util.Iterator;
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
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.actions.SystemAction;

/**
 *
 * @author normenhansen
 */
public class FilterPostProcessorNode extends AbstractNode {

    private FilterDataObject dataObject;
    private static Image smallImage =
            ImageUtilities.loadImage("com/jme3/gde/core/objects_082.gif");
    private FilterPostProcessor fpp;

    public FilterPostProcessorNode(FilterDataObject dataObject) {
        super(new FilterChildren(dataObject));
        this.dataObject = dataObject;
        setName(dataObject.getName());
        ((FilterChildren) getChildren()).setFilterPostProcessorNode(this);
    }

    @Override
    public Image getIcon(int type) {
        return smallImage;
    }

    @Override
    public Image getOpenedIcon(int type) {
        return smallImage;
    }

    public FilterPostProcessor getFilterPostProcessor() {
        if (fpp == null) {
            this.fpp = dataObject.loadAsset();
        }
        return fpp;
    }

    public void refresh() {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                ((FilterChildren) getChildren()).addNotify();
                ((FilterChildren) getChildren()).doRefresh();
            }
        });
    }

    public void addFilter(final Filter filter) {
        SceneApplication.getApplication().enqueue(new Callable<Object>() {

            public Object call() throws Exception {
                getFilterPostProcessor().addFilter(filter);
                return null;
            }
        });
        setModified();
        refresh();
    }

    public void removeFilter(final Filter filter) {
        SceneApplication.getApplication().enqueue(new Callable<Object>() {

            public Object call() throws Exception {
                getFilterPostProcessor().removeFilter(filter);
                return null;
            }
        });
        setModified();
        refresh();
    }

    protected void setModified() {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                dataObject.setModified(true);
            }
        });
    }

    @Override
    public Action[] getActions(boolean context) {
//        return super.getActions(context);
        return new Action[]{
                    new AddFilterAction(this)
                };
    }

    public static class FilterChildren extends Children.Keys<Object> {

        private FilterDataObject dataObject;
        private FilterPostProcessorNode node;
        private boolean readOnly = false;

        public FilterChildren(FilterDataObject dataObject) {
            this.dataObject = dataObject;
        }

        public void setFilterPostProcessorNode(FilterPostProcessorNode node) {
            this.node = node;
        }

        @Override
        protected void addNotify() {
            super.addNotify();
            setKeys(createKeys());
        }

        protected void doRefresh() {
            refresh();
        }

        protected List<Object> createKeys() {
            try {
                return SceneApplication.getApplication().enqueue(new Callable<List<Object>>() {

                    public List<Object> call() throws Exception {
                        List<Object> keys = new LinkedList<Object>();
                        for (Iterator it = node.getFilterPostProcessor().getFilterIterator(); it.hasNext();) {
                            Filter filter = (Filter) it.next();
                            keys.add(filter);
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
        protected Node[] createNodes(Object t) {
            Filter filter = (Filter) t;
            for (FilterNode di : Lookup.getDefault().lookupAll(FilterNode.class)) {
                if (di.getExplorerObjectClass().getName().equals(filter.getClass().getName())) {
                    Node[] ret = di.createNodes(filter, dataObject, readOnly);
                    if (ret != null) {
                        return ret;
                    }
                }
            }
            return new Node[]{};
        }
    }
}

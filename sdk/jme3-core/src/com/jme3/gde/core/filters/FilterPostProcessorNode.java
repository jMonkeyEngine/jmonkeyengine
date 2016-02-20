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
import com.jme3.gde.core.filters.actions.NewFilterPopup;
import com.jme3.gde.core.icons.IconList;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.post.Filter;
import com.jme3.post.FilterPostProcessor;
import java.awt.Image;
import java.awt.datatransfer.Transferable;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.NodeTransfer;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.datatransfer.PasteType;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author normenhansen
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class FilterPostProcessorNode extends AbstractNode {

    private FilterDataObject dataObject;
    private static Image smallImage = IconList.eyeOpen.getImage();
    private FilterPostProcessor fpp;
    private static final Logger logger = Logger.getLogger(FilterPostProcessorNode.class.getName());

    public FilterPostProcessorNode(FilterDataObject dataObject) {
        super(new FilterChildren(dataObject), Lookups.singleton(new FilterIndexSupport()));

        //Lookups.singleton(new FilterIndexSupport((FilterChildren)this.getChildren()));
        this.dataObject = dataObject;
        setName(dataObject.getName());
        getLookup().lookup(FilterIndexSupport.class).setFilterPostProcessorNode(this);
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
            fpp = dataObject.loadAsset();
            if (fpp == null)
                logger.log(Level.SEVERE, "Cannot load Filter. Maybe it's not in the Asset Path?");
        }
        return fpp;
    }

    //this allow the reordering on drop of a Node
    @Override
    public PasteType getDropType(Transferable t, int action, final int index) {

        final Node node = NodeTransfer.node(t, action);
        return new PasteType() {

            @Override
            public Transferable paste() throws IOException {
                FilterIndexSupport indexSupport = getLookup().lookup(FilterIndexSupport.class);
                int nodeIndex = indexSupport.indexOf(node);
                if (nodeIndex < index) {
                    indexSupport.move(index - 1, nodeIndex);
                } else {
                    indexSupport.move(index, nodeIndex);
                }

                return null;
            }
        };
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
                FilterPostProcessor fp = getFilterPostProcessor();
                if (fp != null)
                    fp.addFilter(filter);
                return null;
            }
        });
        setModified();
        refresh();
    }

    public void removeFilter(final Filter filter) {
        SceneApplication.getApplication().enqueue(new Callable<Object>() {

            public Object call() throws Exception {
                FilterPostProcessor fp = getFilterPostProcessor();
                if (fp != null)
                    fp.removeFilter(filter);
                
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
                    new NewFilterPopup(this)
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
            List<Object> keys = createKeys();
            if (keys != null)
                setKeys(keys);
        }

        protected void doRefresh() {
            refresh();
        }

        protected void reorderNotify() {
            setKeys(createKeys());
        }

        protected List<Object> createKeys() {
            try {
                return SceneApplication.getApplication().enqueue(new Callable<List<Object>>() {

                    public List<Object> call() throws Exception {
                        List<Object> keys = new LinkedList<Object>();
                        FilterPostProcessor fp = node.getFilterPostProcessor();
                        if (fp == null) /* e.g. Filter not in Asset Path */
                            return null;
                        
                        for (Iterator it = fp.getFilterIterator(); it.hasNext();) {
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

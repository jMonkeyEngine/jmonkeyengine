package com.jme3.gde.core.filters;

import com.jme3.gde.core.filters.FilterPostProcessorNode.FilterChildren;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.post.Filter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import org.openide.nodes.Index;
import org.openide.nodes.Node;

/**
 *
 * @author Nehon
 */
public class FilterIndexSupport extends Index.Support {

    FilterPostProcessorNode fppNode;
    FilterChildren children;

    public FilterIndexSupport() {
    }

    @Override
    public Node[] getNodes() {
        return fppNode.getChildren().getNodes();
    }

    @Override
    public int getNodesCount() {
        return fppNode.getChildren().getNodesCount();
    }

    public FilterPostProcessorNode getFilterPostProcessorNode() {
        return fppNode;
    }

    public void setFilterPostProcessorNode(FilterPostProcessorNode fppNode) {
        this.fppNode = fppNode;
    }

    @Override
    public void reorder(final int[] perm) {

        SceneApplication.getApplication().enqueue(new Callable<Object>() {

            public Object call() throws Exception {
                List<Filter> filters = new ArrayList<Filter>();
                for (Iterator<Filter> it = fppNode.getFilterPostProcessor().getFilterIterator(); it.hasNext();) {
                    Filter f = it.next();
                    filters.add(f);
                }
                fppNode.getFilterPostProcessor().removeAllFilters();
                for (int i = 0; i < perm.length; i++) {
                    fppNode.getFilterPostProcessor().addFilter(filters.get(perm[i]));
                }
                return null;
            }
        });
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                ((FilterChildren) fppNode.getChildren()).reorderNotify();
                ((FilterChildren) fppNode.getChildren()).doRefresh();
            }
        });

    }
}
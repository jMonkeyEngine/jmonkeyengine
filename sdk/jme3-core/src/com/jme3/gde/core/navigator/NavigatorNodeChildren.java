package com.jme3.gde.core.navigator;

import com.jme3.gde.core.editor.SceneApplication;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;
import org.openide.util.Lookup;

/**
 *
 * @author normenhansen
 */
public class NavigatorNodeChildren extends ChildFactory<Object> {

    private Lookup lookup;

    @Override
    protected boolean createKeys(List<Object> toPopulate) {
        SceneApplication app = lookup.lookup(SceneApplication.class);
        if (app == null) {
            Logger.getLogger(NavigatorNodeChildren.class.getName()).log(Level.WARNING, "Could not access SceneApplication");
            return true;
        }
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected Node[] createNodesForKey(Object key) {
        return super.createNodesForKey(key);
    }
    
}

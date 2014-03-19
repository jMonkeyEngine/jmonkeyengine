 package com.jme3.gde.core.navigator;

import java.io.IOException;
import org.openide.loaders.DataObject;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.util.Lookup;

/**
 *
 * @author normenhansen
 */


public class NavigatorNode extends AbstractNode{
    
    public static NavigatorNode createNode(DataObject obj){
        return null;
    }

    public NavigatorNode(Children children, Lookup lookup) {
        super(children, lookup);
    }

    @Override
    public void destroy() throws IOException {
        super.destroy();
    }
    
    
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jme3.gde.gui.multiview;

import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;

/**
 *
 * @author normenhansen
 */
public class NiftyScreenNode extends AbstractNode{

    public NiftyScreenNode(String name) {
        super(Children.LEAF);
        setName(name);
    }

}

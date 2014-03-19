/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jme3.gde.gui.multiview;

import org.openide.nodes.AbstractNode;
import org.w3c.dom.Element;

/**
 *
 * @author normenhansen
 */
public class NiftyFileNode extends AbstractNode{

    public NiftyFileNode(Element xmlNode) {
        super(new NiftyFileChildren(xmlNode));
    }
    

}

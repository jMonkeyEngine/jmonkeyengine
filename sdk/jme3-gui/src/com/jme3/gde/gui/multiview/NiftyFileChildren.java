/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.gui.multiview;

import java.util.LinkedList;
import java.util.List;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.w3c.dom.Element;

/**
 *
 * @author normenhansen
 */
public class NiftyFileChildren extends Children.Keys<Element> {

    Element xmlNode;

    public NiftyFileChildren(Element xmlNode) {
        this.xmlNode = xmlNode;        
    }

    @Override
    protected void addNotify() {
        super.addNotify();
        setKeys(createKeys());
    }

    protected List<Element> createKeys() {
        LinkedList<Element> ret = new LinkedList<Element>();
        Element curElement = XmlHelper.findFirstChildElement(xmlNode);
        while (curElement != null) {
            if (checkElement(curElement)) {
                ret.add(curElement);
            }
            curElement = XmlHelper.findNextSiblingElement(curElement);
        }

        return ret;
    }

    private boolean checkElement(Element curElement) {
        if (!"screen".equals(curElement.getTagName())) {
            return false;
        }
        return true;
    }

    @Override
    protected Node[] createNodes(Element key) {
        return new Node[]{new NiftyScreenNode(key.getAttribute("id"))};
    }
}

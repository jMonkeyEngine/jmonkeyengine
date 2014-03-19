/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jme3.gde.gui.multiview;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 *
 * @author normenhansen
 */
public class XmlHelper {
    public static Element findFirstChildElement(Element parent) {
        org.w3c.dom.Node ret = parent.getFirstChild();
        while (ret != null && (!(ret instanceof Element))) {
            ret = ret.getNextSibling();
        }
        return (Element) ret;
    }

    public static Element findChildElement(Element parent, String name) {
        if (parent == null) {
            return null;
        }
        org.w3c.dom.Node ret = parent.getFirstChild();
        while (ret != null && (!(ret instanceof Element) || !ret.getNodeName().equals(name))) {
            ret = ret.getNextSibling();
        }
        return (Element) ret;
    }

    public static Element findNextElement(Node ret, String name) {
        ret = ret.getNextSibling();
        while (ret != null && (!(ret instanceof Element) || !ret.getNodeName().equals(name))) {
            ret = ret.getNextSibling();
        }
        return (Element) ret;
    }

    public static Element findChildElementWithAttribute(Element parent, String name, String attribute, String value) {
        if (parent == null) {
            return null;
        }
        org.w3c.dom.Node ret = parent.getFirstChild();
        while (ret != null && (!(ret instanceof Element) || !ret.getNodeName().equals(name) || ((Element)ret).getAttribute(attribute)==null || !((Element)ret).getAttribute(attribute).equals(value))) {
            ret = ret.getNextSibling();
        }
        return (Element) ret;
    }

    public static Element findNextElementWithAttribute(Node ret, String name, String attribute, String value) {
        ret = ret.getNextSibling();
        while (ret != null && (!(ret instanceof Element) || !ret.getNodeName().equals(name) || ((Element)ret).getAttribute(attribute)==null || !((Element)ret).getAttribute(attribute).equals(value))) {
            ret = ret.getNextSibling();
        }
        return (Element) ret;
    }

    public static Element findNextSiblingElement(Element current) {
        org.w3c.dom.Node ret = current.getNextSibling();
        while (ret != null) {
            if (ret instanceof Element) {
                return (Element) ret;
            }
            ret = ret.getNextSibling();
        }
        return null;
    }

}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.export.xml;

import org.w3c.dom.Element;

/**
 * Utilities for reading and writing XML files.
 * 
 * @author codex
 */
public class XMLUtils {    
    
    /**
     * Prefix to every jme xml attribute.
     * <p>
     * This prefix should be appended at the beginning of every
     * xml attribute name. Every access of attributes must
     * append this prefix to the name.
     */
    public static final String PREFIX = "jme-";
    
    /**
     * Sets the attribute in the element under the name.
     * <p>
     * Automatically appends {@link #PREFIX} to the beginning of the name
     * before assigning the attribute to the element.
     * 
     * @param element element to set the attribute in
     * @param name name of the attribute (without prefix)
     * @param attribute attribute to save
     */
    public static void setAttribute(Element element, String name, String attribute) {
        element.setAttribute(PREFIX+name, attribute);
    }
    
    /**
     * Fetches the named attribute from the element.
     * <p>
     * Automatically appends {@link DOMOutputCapsule#PREFIX} to the beginning
     * of the name before looking up the attribute.
     * 
     * @param element XML element to get the attribute from
     * @param name name of the attribute (without prefix)
     * @return named attribute
     */
    public static String getAttribute(Element element, String name) {
        return element.getAttribute(PREFIX+name);
    }
    
    /**
     * Tests if the element contains the named attribute.
     * <p>
     * Automatically appends {@link DOMOutputCapsule#PREFIX} to the beginning
     * of the name before looking up the attribute.
     * 
     * @param element element to test
     * @param name name of the attribute (without prefix)
     * @return true if the element has the named attribute
     */
    public static boolean hasAttribute(Element element, String name) {
        return element.hasAttribute(PREFIX+name);
    }
    
}

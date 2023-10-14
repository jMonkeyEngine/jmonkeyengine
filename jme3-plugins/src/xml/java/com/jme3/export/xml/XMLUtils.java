/*
 * Copyright (c) 2009-2021 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
     * Prefix for every jme xml attribute for format versions 3 and up.
     * <p>
     * This prefix should be appended at the beginning of every xml
     * attribute name. For format versions 3 and up, every name to
     * access an attribute must append this prefix first.
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
     * Automatically appends {@link #PREFIX} to the beginning
     * of the name before looking up the attribute for format versions 3 and up.
     * 
     * @param version format version of the xml
     * @param element XML element to get the attribute from
     * @param name name of the attribute (without prefix)
     * @return named attribute
     */
    public static String getAttribute(int version, Element element, String name) {
        if (version >= 3) {
            return element.getAttribute(PREFIX+name);
        } else {
            return element.getAttribute(name);
        }
    }
    
    /**
     * Tests if the element contains the named attribute.
     * <p>
     * Automatically appends {@link #PREFIX} to the beginning
     * of the name before looking up the attribute for format versions 3 and up.
     * 
     * @param version format version of the xml
     * @param element element to test
     * @param name name of the attribute (without prefix)
     * @return true if the element has the named attribute
     */
    public static boolean hasAttribute(int version, Element element, String name) {
        if (version >= 3) {
            return element.hasAttribute(PREFIX+name);
        } else {
            return element.hasAttribute(name);
        }
    }
    
    /**
     * Denies instantiation of this class.
     */
    private XMLUtils() {
    }
    
}

/*
 * Copyright (c) 2009-2026 jMonkeyEngine
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
package com.jme3.util.xml;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.SAXException;

/** Factory methods for XML parsers that must not resolve external entities. */
public final class SecureXmlFactory {

    private SecureXmlFactory() {
    }

    public static SAXParserFactory createSaxParserFactory()
            throws ParserConfigurationException, SAXException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        setSaxFeature(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
        setSaxFeature(factory, "http://apache.org/xml/features/disallow-doctype-decl", true);
        setSaxFeature(factory, "http://xml.org/sax/features/external-general-entities", false);
        setSaxFeature(factory, "http://xml.org/sax/features/external-parameter-entities", false);
        setSaxFeature(factory, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        return factory;
    }

    public static DocumentBuilderFactory createDocumentBuilderFactory()
            throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        setDocumentFeature(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
        setDocumentFeature(factory, "http://apache.org/xml/features/disallow-doctype-decl", true);
        setDocumentFeature(factory, "http://xml.org/sax/features/external-general-entities", false);
        setDocumentFeature(factory, "http://xml.org/sax/features/external-parameter-entities", false);
        setDocumentFeature(factory, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        setAttribute(factory, XMLConstants.ACCESS_EXTERNAL_DTD, "");
        setAttribute(factory, XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory;
    }

    private static void setSaxFeature(SAXParserFactory factory, String feature, boolean value)
            throws ParserConfigurationException, SAXException {
        factory.setFeature(feature, value);
    }

    private static void setDocumentFeature(DocumentBuilderFactory factory, String feature, boolean value)
            throws ParserConfigurationException {
        factory.setFeature(feature, value);
    }

    private static void setAttribute(DocumentBuilderFactory factory, String name, String value)
            throws ParserConfigurationException {
        try {
            factory.setAttribute(name, value);
        } catch (IllegalArgumentException e) {
            ParserConfigurationException pce = new ParserConfigurationException(
                    "XML parser does not support secure attribute: " + name);
            pce.initCause(e);
            throw pce;
        }
    }
}

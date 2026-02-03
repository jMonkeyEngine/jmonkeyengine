/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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

import com.jme3.export.JmeExporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Part of the jME XML IO system as introduced in the Google Code jmexml project.
 * 
 * @author Kai Rabien (hevee) - original author of the code.google.com jmexml project
 * @author Doug Daniels (dougnukem) - adjustments for jME 2.0 and Java 1.5
 */
public class XMLExporter implements JmeExporter {
    
    public static final String ELEMENT_MAPENTRY = "MapEntry";
    public static final String ELEMENT_KEY = "Key";
    public static final String ELEMENT_VALUE = "Value";
    public static final String ATTRIBUTE_SIZE = "size";
    public static final String ATTRIBUTE_DATA = "data";
    public static final String ATTRIBUTE_CLASS = "class";
    public static final String ATTRIBUTE_REFERENCE_ID = "reference_ID";
    public static final String ATTRIBUTE_REFERENCE = "ref";
    public static final String ATTRIBUTE_SAVABLE_VERSIONS = "savable_versions";

    private DOMOutputCapsule domOut;

    private int indentSpaces = 4;
    
    public XMLExporter() {
       
    }

    @Override
    public void save(Savable object, OutputStream outputStream) throws IOException {
        Document document = null;
        try {
            document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException ex) {
            throw new IOException(ex);
        }
        document.setXmlStandalone(true);    // for some reason the transformer output property below doesn't work unless the document is set to standalone

        // Initialize the DOMOutputCapsule when saving, so we don't retain state of previous exports.
        domOut = new DOMOutputCapsule(document, this);

        domOut.write(object, "savable", null);

        DOMSource source = new DOMSource(domOut.getDoc());
        StreamResult result = new StreamResult(outputStream);

        try {
            TransformerFactory tfFactory = TransformerFactory.newInstance();
            tfFactory.setAttribute("indent-number", indentSpaces);
            // Disable external DTD and stylesheet access to prevent XXE attacks
            tfFactory.setAttribute("accessExternalDTD", "");
            tfFactory.setAttribute("accessExternalStylesheet", "");

            Transformer transformer = tfFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");

            if (indentSpaces > 0) {
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            }

            transformer.transform(source, result);
        } catch (TransformerException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void save(Savable object, File f, boolean createDirectories) throws IOException {
        File parentDirectory = f.getParentFile();
        if (parentDirectory != null && !parentDirectory.exists() && createDirectories) {
            parentDirectory.mkdirs();
        }

        FileOutputStream fos = new FileOutputStream(f);
        try {
            save(object, fos);
        } finally {
            fos.close();
        }
    }

    @Override
    public OutputCapsule getCapsule(Savable object) {
        return domOut;
    }

    /**
     * Sets the number of spaces used to indent nested tags.
     * @param indentSpaces The number of spaces to indent for each level of nesting.  Default is 4.
     * Pass 0 to disable pretty printing and write document without adding any whitespace.
     */
    public void setIndentSpaces(int indentSpaces) {
        this.indentSpaces = indentSpaces;
    }

    public static XMLExporter getInstance() {
        return new XMLExporter();
    }
}

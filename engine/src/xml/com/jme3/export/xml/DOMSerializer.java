/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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

import java.io.*;
import java.nio.charset.Charset;
import org.w3c.dom.*;

/**
 * The DOMSerializer was based primarily off the DOMSerializer.java class from the 
 * "Java and XML" 3rd Edition book by Brett McLaughlin, and Justin Edelson. Some 
 * modifications were made to support formatting of elements and attributes.
 * 
 * @author Brett McLaughlin, Justin Edelson - Original creation for "Java and XML" book.
 * @author Doug Daniels (dougnukem) - adjustments for XML formatting
 * @version $Revision: 4207 $, $Date: 2009-03-29 11:19:16 -0400 (Sun, 29 Mar 2009) $
 */
public class DOMSerializer {

    /** The encoding to use for output (default is UTF-8) */
    private Charset encoding = Charset.forName("utf-8");

    /** The amount of indentation to use (default is 4 spaces). */
    private int indent = 4;

    /** The line separator to use (default is the based on the current system settings). */
    private String lineSeparator = System.getProperty("line.separator", "\n");

    private void escape(Writer writer, String s) throws IOException {
        if (s == null) { return; }
        for (int i = 0, len = s.length(); i < len; i++) {
            char c = s.charAt(i);
            switch (c) {
            case '<':
                writer.write("&lt;");
                break;
            case '>':
                writer.write("&gt;");
                break;
            case '&':
                writer.write("&amp;");
                break;
            case '\r':
                writer.write("&#xD;");
                break;
            default:
                writer.write(c);
            }
        }
    }

    /**
     * Serialize {@code doc} to {@code out}
     * 
     * @param doc the document to serialize.
     * @param file the file to serialize to.
     * @throws IOException
     */
    public void serialize(Document doc, File file) throws IOException {
        serialize(doc, new FileOutputStream(file));
    }

    /**
     * Serialize {@code doc} to {@code out}
     * 
     * @param doc the document to serialize.
     * @param out the stream to serialize to.
     * @throws IOException
     */
    public void serialize(Document doc, OutputStream out) throws IOException {
        Writer writer = new OutputStreamWriter(out, encoding);
        write(doc, writer, 0);
        writer.flush();
    }

    /**
     * Set the encoding used by this serializer.
     * 
     * @param encoding the encoding to use, passing in {@code null} results in the
     *  default encoding (UTF-8) being set.
     * @throws IllegalCharsetNameException if the given charset name is illegal.
     * @throws UnsupportedCharsetException if the given charset is not supported by the
     *  current JVM.
     */
    public void setEncoding(String encoding) {
        this.encoding = Charset.forName(encoding);
    }

    /**
     * Set the number of spaces to use for indentation.
     * <p>
     * The default is to use 4 spaces.
     * 
     * @param indent the number of spaces to use for indentation, values less than or
     *  equal to zero result in no indentation being used.
     */
    public void setIndent(int indent) {
        this.indent = indent >= 0 ? indent : 0;
    }

    /**
     * Set the line separator that will be used when serializing documents.
     * <p>
     * If this is not called then the serializer uses a default based on the
     * {@code line.separator} system property. 
     * 
     * @param lineSeparator the line separator to set.
     */
    public void setLineSeparator(String lineSeparator) {
        this.lineSeparator = lineSeparator;
    }

    private void write(Node node, Writer writer, int depth) throws IOException {
        switch (node.getNodeType()) {
        case Node.DOCUMENT_NODE:
            writeDocument((Document) node, writer);
            break;
        case Node.ELEMENT_NODE:
            writeElement((Element) node, writer, depth);
            break;
        case Node.TEXT_NODE:
            escape(writer, node.getNodeValue());
            break;
        case Node.CDATA_SECTION_NODE:
            writer.write("<![CDATA[");
            escape(writer, node.getNodeValue());
            writer.write("]]>");
            break;
        case Node.COMMENT_NODE:
            for (int i = 0; i < depth; ++i) { writer.append(' '); }
            writer.append("<!-- ").append(node.getNodeValue()).append(" -->").append(lineSeparator);
            break;
        case Node.PROCESSING_INSTRUCTION_NODE:
            String n = node.getNodeName();
            String v = node.getNodeValue();
            for (int i = 0; i < depth; ++i) { writer.append(' '); }
            writer.append("<?").append(n).append(' ').append(v).append("?>").append(lineSeparator);
            break;
        case Node.ENTITY_REFERENCE_NODE:
            writer.append('&').append(node.getNodeName()).append(';');
            break;
        case Node.DOCUMENT_TYPE_NODE:
            writeDocumentType((DocumentType) node, writer, depth);
            break;
        }
    }

    private void writeDocument(Document document, Writer writer) throws IOException {
        String v = document.getXmlVersion();

        writer.append("<?xml ");
        writer.append(" version='").append(v == null ? "1.0" : v).append("'");
        writer.append(" encoding='").append(encoding.name()).append("'");
        if (document.getXmlStandalone()) {
            writer.append(" standalone='yes'");
        }
        writer.append("?>").append(lineSeparator);

        NodeList nodes = document.getChildNodes();
        for (int i = 0, imax = nodes.getLength(); i < imax; ++i) {
            write(nodes.item(i), writer, 0);
        }
    }

    private void writeDocumentType(DocumentType docType, Writer writer, int depth) throws IOException {
        String publicId = docType.getPublicId();
        String internalSubset = docType.getInternalSubset();

        for (int i = 0; i < depth; ++i) { writer.append(' '); }
        writer.append("<!DOCTYPE ").append(docType.getName());
        if (publicId != null) {
            writer.append(" PUBLIC '").append(publicId).append("' ");
        } else {
            writer.write(" SYSTEM ");
        }
        writer.append("'").append(docType.getSystemId()).append("'");
        if (internalSubset != null) {
            writer.append(" [").append(internalSubset).append("]");
        }
        writer.append('>').append(lineSeparator);
    }

    private void writeElement(Element element, Writer writer, int depth) throws IOException {
        for (int i = 0; i < depth; ++i) { writer.append(' '); }
        writer.append('<').append(element.getTagName());
        NamedNodeMap attrs = element.getAttributes();
        for (int i = 0, imax = attrs.getLength(); i < imax; ++i) {
            Attr attr = (Attr) attrs.item(i);
            writer.append(' ').append(attr.getName()).append("='").append(attr.getValue()).append("'");
        }
        NodeList nodes = element.getChildNodes();
        if (nodes.getLength() == 0) {
            // no children, so just close off the element and return
            writer.append("/>").append(lineSeparator);
            return;
        }
        writer.append('>').append(lineSeparator);
        for (int i = 0, imax = nodes.getLength(); i < imax; ++i) {
            Node n = nodes.item(i);
            if (n.getNodeType() == Node.ATTRIBUTE_NODE) { continue; }
            write(n, writer, depth + indent);
        }
        for (int i = 0; i < depth; ++i) { writer.append(' '); }
        writer.append("</").append(element.getTagName()).append('>').append(lineSeparator);
    }

}

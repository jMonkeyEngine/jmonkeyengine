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

import com.jme3.export.InputCapsule;
import com.jme3.export.Savable;
import com.jme3.export.SavableClassUtil;
import com.jme3.util.BufferUtils;
import com.jme3.util.IntMap;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.*;
import java.util.logging.Logger;
import org.w3c.dom.*;

/**
 * Part of the jME XML IO system as introduced in the google code jmexml project.
 *
 * @author Kai Rabien (hevee) - original author of the code.google.com jmexml project
 * @author Doug Daniels (dougnukem) - adjustments for jME 2.0 and Java 1.5
 * @author blaine
 */
public class DOMInputCapsule implements InputCapsule {
    private static final Logger logger =
        Logger.getLogger(DOMInputCapsule.class .getName());

    private Document doc;
    private Element currentElem;
    private XMLImporter importer;
    private boolean isAtRoot = true;
    private Map<String, Savable> referencedSavables = new HashMap<String, Savable>();
    
    private int[] classHierarchyVersions;
    private Savable savable;

    public DOMInputCapsule(Document doc, XMLImporter importer) {
        this.doc = doc;
        this.importer = importer;
        currentElem = doc.getDocumentElement();
        
        String version = currentElem.getAttribute("format_version");
        importer.formatVersion = version.equals("") ? 0 : Integer.parseInt(version);
    }

    public int getSavableVersion(Class<? extends Savable> desiredClass) {
        if (classHierarchyVersions != null){
            return SavableClassUtil.getSavedSavableVersion(savable, desiredClass, 
                                                        classHierarchyVersions, importer.getFormatVersion());
        }else{
            return 0;
        }
    }
    
    private static String decodeString(String s) {
        if (s == null) {
            return null;
        }
        s = s.replaceAll("\\&quot;", "\"").replaceAll("\\&lt;", "<").replaceAll("\\&amp;", "&");
        return s;
    }

    private Element findFirstChildElement(Element parent) {
        Node ret = parent.getFirstChild();
        while (ret != null && (!(ret instanceof Element))) {
            ret = ret.getNextSibling();
        }
        return (Element) ret;
    }

    private Element findChildElement(Element parent, String name) {
        if (parent == null) {
            return null;
        }
        Node ret = parent.getFirstChild();
        while (ret != null && (!(ret instanceof Element) || !ret.getNodeName().equals(name))) {
            ret = ret.getNextSibling();
        }
        return (Element) ret;
    }

    private Element findNextSiblingElement(Element current) {
        Node ret = current.getNextSibling();
        while (ret != null) {
            if (ret instanceof Element) {
                return (Element) ret;
            }
            ret = ret.getNextSibling();
        }
        return null;
    }

    public byte readByte(String name, byte defVal) throws IOException {
        String tmpString = currentElem.getAttribute(name);
        if (tmpString == null || tmpString.length() < 1) return defVal;
        try {
            return Byte.parseByte(tmpString);
        } catch (NumberFormatException nfe) {
            IOException io = new IOException(nfe.toString());
            io.initCause(nfe);
            throw io;
        } catch (DOMException de) {
            IOException io = new IOException(de.toString());
            io.initCause(de);
            throw io;
        }
    }

    public byte[] readByteArray(String name, byte[] defVal) throws IOException {
        try {
            Element tmpEl;
            if (name != null) {
                tmpEl = findChildElement(currentElem, name);
            } else {
                tmpEl = currentElem;
            }
            if (tmpEl == null) {
                return defVal;
            }
            String sizeString = tmpEl.getAttribute("size");
            String[] strings = parseTokens(tmpEl.getAttribute("data"));
            if (sizeString.length() > 0) {
                int requiredSize = Integer.parseInt(sizeString);
                if (strings.length != requiredSize)
                    throw new IOException("Wrong number of bytes for '" + name
                            + "'.  size says " + requiredSize
                            + ", data contains "
                            + strings.length);
            }
            byte[] tmp = new byte[strings.length];
            for (int i = 0; i < strings.length; i++) {
                tmp[i] = Byte.parseByte(strings[i]);
            }
            return tmp;
        } catch (IOException ioe) {
            throw ioe;
        } catch (NumberFormatException nfe) {
            IOException io = new IOException(nfe.toString());
            io.initCause(nfe);
            throw io;
        } catch (DOMException de) {
            IOException io = new IOException(de.toString());
            io.initCause(de);
            throw io;
        }
    }

    public byte[][] readByteArray2D(String name, byte[][] defVal) throws IOException {
        try {
            Element tmpEl;
            if (name != null) {
                tmpEl = findChildElement(currentElem, name);
            } else {
                tmpEl = currentElem;
            }
            if (tmpEl == null) {
                return defVal;
            }

            String sizeString = tmpEl.getAttribute("size");
            NodeList nodes = currentElem.getChildNodes();
            List<byte[]> byteArrays = new ArrayList<byte[]>();

            for (int i = 0; i < nodes.getLength(); i++) {
                        Node n = nodes.item(i);
                                if (n instanceof Element && n.getNodeName().contains("array")) {
                // Very unsafe assumption
                    byteArrays.add(readByteArray(n.getNodeName(), null));
                                }
            }
            if (sizeString.length() > 0) {
                int requiredSize = Integer.parseInt(sizeString);
                if (byteArrays.size() != requiredSize)
                    throw new IOException(
                            "String array contains wrong element count.  "
                            + "Specified size " + requiredSize
                            + ", data contains " + byteArrays.size());
            }
            currentElem = (Element) currentElem.getParentNode();
            return byteArrays.toArray(new byte[0][]);
        } catch (IOException ioe) {
            throw ioe;
        } catch (NumberFormatException nfe) {
            IOException io = new IOException(nfe.toString());
            io.initCause(nfe);
            throw io;
        } catch (DOMException de) {
            IOException io = new IOException(de.toString());
            io.initCause(de);
            throw io;
        }
    }

    public int readInt(String name, int defVal) throws IOException {
        String tmpString = currentElem.getAttribute(name);
        if (tmpString == null || tmpString.length() < 1) return defVal;
        try {
            return Integer.parseInt(tmpString);
        } catch (NumberFormatException nfe) {
            IOException io = new IOException(nfe.toString());
            io.initCause(nfe);
            throw io;
        } catch (DOMException de) {
            IOException io = new IOException(de.toString());
            io.initCause(de);
            throw io;
        }
    }

    public int[] readIntArray(String name, int[] defVal) throws IOException {
        try {
            Element tmpEl;
            if (name != null) {
                tmpEl = findChildElement(currentElem, name);
            } else {
                tmpEl = currentElem;
            }
            if (tmpEl == null) {
                return defVal;
            }
            String sizeString = tmpEl.getAttribute("size");
            String[] strings = parseTokens(tmpEl.getAttribute("data"));
            if (sizeString.length() > 0) {
                int requiredSize = Integer.parseInt(sizeString);
                if (strings.length != requiredSize)
                    throw new IOException("Wrong number of ints for '" + name
                            + "'.  size says " + requiredSize
                            + ", data contains " + strings.length);
            }
            int[] tmp = new int[strings.length];
            for (int i = 0; i < tmp.length; i++) {
                tmp[i] = Integer.parseInt(strings[i]);
            }
            return tmp;
        } catch (IOException ioe) {
            throw ioe;
        } catch (NumberFormatException nfe) {
            IOException io = new IOException(nfe.toString());
            io.initCause(nfe);
            throw io;
        } catch (DOMException de) {
            IOException io = new IOException(de.toString());
            io.initCause(de);
            throw io;
        }
    }

    public int[][] readIntArray2D(String name, int[][] defVal) throws IOException {
        try {
            Element tmpEl;
            if (name != null) {
                tmpEl = findChildElement(currentElem, name);
            } else {
                tmpEl = currentElem;
            }
            if (tmpEl == null) {
                return defVal;
            }
            String sizeString = tmpEl.getAttribute("size");




            NodeList nodes = currentElem.getChildNodes();
            List<int[]> intArrays = new ArrayList<int[]>();

            for (int i = 0; i < nodes.getLength(); i++) {
                        Node n = nodes.item(i);
                                if (n instanceof Element && n.getNodeName().contains("array")) {
                // Very unsafe assumption
                    intArrays.add(readIntArray(n.getNodeName(), null));
                                }
            }
            if (sizeString.length() > 0) {
                int requiredSize = Integer.parseInt(sizeString);
                if (intArrays.size() != requiredSize)
                    throw new IOException(
                            "String array contains wrong element count.  "
                            + "Specified size " + requiredSize
                            + ", data contains " + intArrays.size());
            }
            currentElem = (Element) currentElem.getParentNode();
            return intArrays.toArray(new int[0][]);
        } catch (IOException ioe) {
            throw ioe;
        } catch (NumberFormatException nfe) {
            IOException io = new IOException(nfe.toString());
            io.initCause(nfe);
            throw io;
        } catch (DOMException de) {
            IOException io = new IOException(de.toString());
            io.initCause(de);
            throw io;
        }
    }

    public float readFloat(String name, float defVal) throws IOException {
        String tmpString = currentElem.getAttribute(name);
        if (tmpString == null || tmpString.length() < 1) return defVal;
        try {
            return Float.parseFloat(tmpString);
        } catch (NumberFormatException nfe) {
            IOException io = new IOException(nfe.toString());
            io.initCause(nfe);
            throw io;
        } catch (DOMException de) {
            IOException io = new IOException(de.toString());
            io.initCause(de);
            throw io;
        }
    }

    public float[] readFloatArray(String name, float[] defVal) throws IOException {
        try {
            Element tmpEl;
            if (name != null) {
                tmpEl = findChildElement(currentElem, name);
            } else {
                tmpEl = currentElem;
            }
            if (tmpEl == null) {
                return defVal;
            }
            String sizeString = tmpEl.getAttribute("size");
            String[] strings = parseTokens(tmpEl.getAttribute("data"));
            if (sizeString.length() > 0) {
                int requiredSize = Integer.parseInt(sizeString);
                if (strings.length != requiredSize)
                    throw new IOException("Wrong number of floats for '" + name
                            + "'.  size says " + requiredSize
                            + ", data contains " + strings.length);
            }
            float[] tmp = new float[strings.length];
            for (int i = 0; i < tmp.length; i++) {
                tmp[i] = Float.parseFloat(strings[i]);
            }
            return tmp;
        } catch (IOException ioe) {
            throw ioe;
        } catch (DOMException de) {
            IOException io = new IOException(de.toString());
            io.initCause(de);
            throw io;
        }
    }

    public float[][] readFloatArray2D(String name, float[][] defVal) throws IOException {
        /* Why does this one method ignore the 'size attr.? */
        try {
            Element tmpEl;
            if (name != null) {
                tmpEl = findChildElement(currentElem, name);
            } else {
                tmpEl = currentElem;
            }
            if (tmpEl == null) {
                return defVal;
            }
            int size_outer = Integer.parseInt(tmpEl.getAttribute("size_outer"));
            int size_inner = Integer.parseInt(tmpEl.getAttribute("size_outer"));

            float[][] tmp = new float[size_outer][size_inner];

            String[] strings = parseTokens(tmpEl.getAttribute("data"));
            for (int i = 0; i < size_outer; i++) {
                tmp[i] = new float[size_inner];
                for (int k = 0; k < size_inner; k++) {
                    tmp[i][k] = Float.parseFloat(strings[i]);
                }
            }
            return tmp;
        } catch (NumberFormatException nfe) {
            IOException io = new IOException(nfe.toString());
            io.initCause(nfe);
            throw io;
        } catch (DOMException de) {
            IOException io = new IOException(de.toString());
            io.initCause(de);
            throw io;
        }
    }

    public double readDouble(String name, double defVal) throws IOException {
        String tmpString = currentElem.getAttribute(name);
        if (tmpString == null || tmpString.length() < 1) return defVal;
        try {
            return Double.parseDouble(tmpString);
        } catch (NumberFormatException nfe) {
            IOException io = new IOException(nfe.toString());
            io.initCause(nfe);
            throw io;
        } catch (DOMException de) {
            IOException io = new IOException(de.toString());
            io.initCause(de);
            throw io;
        }
    }

    public double[] readDoubleArray(String name, double[] defVal) throws IOException {
        try {
            Element tmpEl;
            if (name != null) {
                tmpEl = findChildElement(currentElem, name);
            } else {
                tmpEl = currentElem;
            }
            if (tmpEl == null) {
                return defVal;
            }
            String sizeString = tmpEl.getAttribute("size");
            String[] strings = parseTokens(tmpEl.getAttribute("data"));
            if (sizeString.length() > 0) {
                int requiredSize = Integer.parseInt(sizeString);
                if (strings.length != requiredSize)
                    throw new IOException("Wrong number of doubles for '"
                            + name + "'.  size says " + requiredSize
                            + ", data contains " + strings.length);
            }
            double[] tmp = new double[strings.length];
            for (int i = 0; i < tmp.length; i++) {
                tmp[i] = Double.parseDouble(strings[i]);
            }
            return tmp;
        } catch (IOException ioe) {
            throw ioe;
        } catch (NumberFormatException nfe) {
            IOException io = new IOException(nfe.toString());
            io.initCause(nfe);
            throw io;
        } catch (DOMException de) {
            IOException io = new IOException(de.toString());
            io.initCause(de);
            throw io;
        }
    }

    public double[][] readDoubleArray2D(String name, double[][] defVal) throws IOException {
        try {
            Element tmpEl;
            if (name != null) {
                tmpEl = findChildElement(currentElem, name);
            } else {
                tmpEl = currentElem;
            }
            if (tmpEl == null) {
                return defVal;
            }
            String sizeString = tmpEl.getAttribute("size");
            NodeList nodes = currentElem.getChildNodes();
            List<double[]> doubleArrays = new ArrayList<double[]>();

            for (int i = 0; i < nodes.getLength(); i++) {
                        Node n = nodes.item(i);
                                if (n instanceof Element && n.getNodeName().contains("array")) {
                // Very unsafe assumption
                    doubleArrays.add(readDoubleArray(n.getNodeName(), null));
                                }
            }
            if (sizeString.length() > 0) {
                int requiredSize = Integer.parseInt(sizeString);
                if (doubleArrays.size() != requiredSize)
                    throw new IOException(
                            "String array contains wrong element count.  "
                            + "Specified size " + requiredSize
                            + ", data contains " + doubleArrays.size());
            }
            currentElem = (Element) currentElem.getParentNode();
            return doubleArrays.toArray(new double[0][]);
        } catch (IOException ioe) {
            throw ioe;
        } catch (NumberFormatException nfe) {
            IOException io = new IOException(nfe.toString());
            io.initCause(nfe);
            throw io;
        } catch (DOMException de) {
            IOException io = new IOException(de.toString());
            io.initCause(de);
            throw io;
        }
    }

    public long readLong(String name, long defVal) throws IOException {
        String tmpString = currentElem.getAttribute(name);
        if (tmpString == null || tmpString.length() < 1) return defVal;
        try {
            return Long.parseLong(tmpString);
        } catch (NumberFormatException nfe) {
            IOException io = new IOException(nfe.toString());
            io.initCause(nfe);
            throw io;
        } catch (DOMException de) {
            IOException io = new IOException(de.toString());
            io.initCause(de);
            throw io;
        }
    }

    public long[] readLongArray(String name, long[] defVal) throws IOException {
        try {
            Element tmpEl;
            if (name != null) {
                tmpEl = findChildElement(currentElem, name);
            } else {
                tmpEl = currentElem;
            }
            if (tmpEl == null) {
                return defVal;
            }
            String sizeString = tmpEl.getAttribute("size");
            String[] strings = parseTokens(tmpEl.getAttribute("data"));
            if (sizeString.length() > 0) {
                int requiredSize = Integer.parseInt(sizeString);
                if (strings.length != requiredSize)
                    throw new IOException("Wrong number of longs for '" + name
                            + "'.  size says " + requiredSize
                            + ", data contains " + strings.length);
            }
            long[] tmp = new long[strings.length];
            for (int i = 0; i < tmp.length; i++) {
                tmp[i] = Long.parseLong(strings[i]);
            }
            return tmp;
        } catch (IOException ioe) {
            throw ioe;
        } catch (NumberFormatException nfe) {
            IOException io = new IOException(nfe.toString());
            io.initCause(nfe);
            throw io;
        } catch (DOMException de) {
            IOException io = new IOException(de.toString());
            io.initCause(de);
            throw io;
        }
    }

    public long[][] readLongArray2D(String name, long[][] defVal) throws IOException {
        try {
            Element tmpEl;
            if (name != null) {
                tmpEl = findChildElement(currentElem, name);
            } else {
                tmpEl = currentElem;
            }
            if (tmpEl == null) {
                return defVal;
            }
            String sizeString = tmpEl.getAttribute("size");
            NodeList nodes = currentElem.getChildNodes();
            List<long[]> longArrays = new ArrayList<long[]>();

            for (int i = 0; i < nodes.getLength(); i++) {
                        Node n = nodes.item(i);
                                if (n instanceof Element && n.getNodeName().contains("array")) {
                // Very unsafe assumption
                    longArrays.add(readLongArray(n.getNodeName(), null));
                                }
            }
            if (sizeString.length() > 0) {
                int requiredSize = Integer.parseInt(sizeString);
                if (longArrays.size() != requiredSize)
                    throw new IOException(
                            "String array contains wrong element count.  "
                            + "Specified size " + requiredSize
                            + ", data contains " + longArrays.size());
            }
            currentElem = (Element) currentElem.getParentNode();
            return longArrays.toArray(new long[0][]);
        } catch (IOException ioe) {
            throw ioe;
        } catch (NumberFormatException nfe) {
            IOException io = new IOException(nfe.toString());
            io.initCause(nfe);
            throw io;
        } catch (DOMException de) {
            IOException io = new IOException(de.toString());
            io.initCause(de);
            throw io;
        }
    }

    public short readShort(String name, short defVal) throws IOException {
        String tmpString = currentElem.getAttribute(name);
        if (tmpString == null || tmpString.length() < 1) return defVal;
        try {
            return Short.parseShort(tmpString);
        } catch (NumberFormatException nfe) {
            IOException io = new IOException(nfe.toString());
            io.initCause(nfe);
            throw io;
        } catch (DOMException de) {
            IOException io = new IOException(de.toString());
            io.initCause(de);
            throw io;
        }
    }

    public short[] readShortArray(String name, short[] defVal) throws IOException {
         try {
             Element tmpEl;
             if (name != null) {
                 tmpEl = findChildElement(currentElem, name);
             } else {
                 tmpEl = currentElem;
             }
             if (tmpEl == null) {
                 return defVal;
             }
            String sizeString = tmpEl.getAttribute("size");
            String[] strings = parseTokens(tmpEl.getAttribute("data"));
            if (sizeString.length() > 0) {
                int requiredSize = Integer.parseInt(sizeString);
                if (strings.length != requiredSize)
                    throw new IOException("Wrong number of shorts for '"
                            + name + "'.  size says " + requiredSize
                            + ", data contains " + strings.length);
            }
            short[] tmp = new short[strings.length];
            for (int i = 0; i < tmp.length; i++) {
                tmp[i] = Short.parseShort(strings[i]);
            }
            return tmp;
        } catch (IOException ioe) {
            throw ioe;
        } catch (NumberFormatException nfe) {
            IOException io = new IOException(nfe.toString());
            io.initCause(nfe);
            throw io;
        } catch (DOMException de) {
            IOException io = new IOException(de.toString());
            io.initCause(de);
            throw io;
        }
    }

    public short[][] readShortArray2D(String name, short[][] defVal) throws IOException {
        try {
            Element tmpEl;
            if (name != null) {
                tmpEl = findChildElement(currentElem, name);
            } else {
                tmpEl = currentElem;
            }
            if (tmpEl == null) {
                return defVal;
            }

            String sizeString = tmpEl.getAttribute("size");
            NodeList nodes = currentElem.getChildNodes();
            List<short[]> shortArrays = new ArrayList<short[]>();

            for (int i = 0; i < nodes.getLength(); i++) {
                        Node n = nodes.item(i);
                                if (n instanceof Element && n.getNodeName().contains("array")) {
                // Very unsafe assumption
                    shortArrays.add(readShortArray(n.getNodeName(), null));
                                }
            }
            if (sizeString.length() > 0) {
                int requiredSize = Integer.parseInt(sizeString);
                if (shortArrays.size() != requiredSize)
                    throw new IOException(
                            "String array contains wrong element count.  "
                            + "Specified size " + requiredSize
                            + ", data contains " + shortArrays.size());
            }
            currentElem = (Element) currentElem.getParentNode();
            return shortArrays.toArray(new short[0][]);
        } catch (IOException ioe) {
            throw ioe;
        } catch (NumberFormatException nfe) {
            IOException io = new IOException(nfe.toString());
            io.initCause(nfe);
            throw io;
        } catch (DOMException de) {
            IOException io = new IOException(de.toString());
            io.initCause(de);
            throw io;
        }
    }

    public boolean readBoolean(String name, boolean defVal) throws IOException {
        String tmpString = currentElem.getAttribute(name);
        if (tmpString == null || tmpString.length() < 1) return defVal;
        try {
            return Boolean.parseBoolean(tmpString);
        } catch (DOMException de) {
            IOException io = new IOException(de.toString());
            io.initCause(de);
            throw io;
        }
    }

    public boolean[] readBooleanArray(String name, boolean[] defVal) throws IOException {
        try {
            Element tmpEl;
            if (name != null) {
                tmpEl = findChildElement(currentElem, name);
            } else {
                tmpEl = currentElem;
            }
            if (tmpEl == null) {
                return defVal;
            }
            String sizeString = tmpEl.getAttribute("size");
            String[] strings = parseTokens(tmpEl.getAttribute("data"));
            if (sizeString.length() > 0) {
                int requiredSize = Integer.parseInt(sizeString);
                if (strings.length != requiredSize)
                    throw new IOException("Wrong number of bools for '" + name
                            + "'.  size says " + requiredSize
                            + ", data contains " + strings.length);
            }
            boolean[] tmp = new boolean[strings.length];
            for (int i = 0; i < tmp.length; i++) {
                tmp[i] = Boolean.parseBoolean(strings[i]);
            }
            return tmp;
        } catch (IOException ioe) {
            throw ioe;
        } catch (DOMException de) {
            IOException io = new IOException(de.toString());
            io.initCause(de);
            throw io;
        }
    }

    public boolean[][] readBooleanArray2D(String name, boolean[][] defVal) throws IOException {
        try {
            Element tmpEl;
            if (name != null) {
                tmpEl = findChildElement(currentElem, name);
            } else {
                tmpEl = currentElem;
            }
            if (tmpEl == null) {
                return defVal;
            }
            String sizeString = tmpEl.getAttribute("size");
            NodeList nodes = currentElem.getChildNodes();
            List<boolean[]> booleanArrays = new ArrayList<boolean[]>();

            for (int i = 0; i < nodes.getLength(); i++) {
                        Node n = nodes.item(i);
                                if (n instanceof Element && n.getNodeName().contains("array")) {
                // Very unsafe assumption
                    booleanArrays.add(readBooleanArray(n.getNodeName(), null));
                                }
            }
            if (sizeString.length() > 0) {
                int requiredSize = Integer.parseInt(sizeString);
                if (booleanArrays.size() != requiredSize)
                    throw new IOException(
                            "String array contains wrong element count.  "
                            + "Specified size " + requiredSize
                            + ", data contains " + booleanArrays.size());
            }
            currentElem = (Element) currentElem.getParentNode();
            return booleanArrays.toArray(new boolean[0][]);
        } catch (IOException ioe) {
            throw ioe;
        } catch (NumberFormatException nfe) {
            IOException io = new IOException(nfe.toString());
            io.initCause(nfe);
            throw io;
        } catch (DOMException de) {
            IOException io = new IOException(de.toString());
            io.initCause(de);
            throw io;
        }
    }

    public String readString(String name, String defVal) throws IOException {
        String tmpString = currentElem.getAttribute(name);
        if (tmpString == null || tmpString.length() < 1) return defVal;
        try {
            return decodeString(tmpString);
        } catch (DOMException de) {
            IOException io = new IOException(de.toString());
            io.initCause(de);
            throw io;
        }
    }

    public String[] readStringArray(String name, String[] defVal) throws IOException {
         try {
             Element tmpEl;
             if (name != null) {
                 tmpEl = findChildElement(currentElem, name);
             } else {
                 tmpEl = currentElem;
             }
             if (tmpEl == null) {
                 return defVal;
             }
            String sizeString = tmpEl.getAttribute("size");
            NodeList nodes = tmpEl.getChildNodes();
            List<String> strings = new ArrayList<String>();

            for (int i = 0; i < nodes.getLength(); i++) {
                        Node n = nodes.item(i);
                                if (n instanceof Element && n.getNodeName().contains("String")) {
                // Very unsafe assumption
                    strings.add(((Element) n).getAttributeNode("value").getValue());
                                }
            }
            if (sizeString.length() > 0) {
                int requiredSize = Integer.parseInt(sizeString);
                if (strings.size() != requiredSize)
                    throw new IOException(
                            "String array contains wrong element count.  "
                            + "Specified size " + requiredSize
                            + ", data contains " + strings.size());
            }
            return strings.toArray(new String[0]);
        } catch (IOException ioe) {
            throw ioe;
        } catch (NumberFormatException nfe) {
            IOException io = new IOException(nfe.toString());
            io.initCause(nfe);
            throw io;
        } catch (DOMException de) {
            IOException io = new IOException(de.toString());
            io.initCause(de);
            throw io;
        }
    }

    public String[][] readStringArray2D(String name, String[][] defVal) throws IOException {
        try {
            Element tmpEl;
            if (name != null) {
                tmpEl = findChildElement(currentElem, name);
            } else {
                tmpEl = currentElem;
            }
            if (tmpEl == null) {
                return defVal;
            }
            String sizeString = tmpEl.getAttribute("size");
            NodeList nodes = currentElem.getChildNodes();
            List<String[]> stringArrays = new ArrayList<String[]>();

            for (int i = 0; i < nodes.getLength(); i++) {
                        Node n = nodes.item(i);
                                if (n instanceof Element && n.getNodeName().contains("array")) {
                // Very unsafe assumption
                    stringArrays.add(readStringArray(n.getNodeName(), null));
                                }
            }
            if (sizeString.length() > 0) {
                int requiredSize = Integer.parseInt(sizeString);
                if (stringArrays.size() != requiredSize)
                    throw new IOException(
                            "String array contains wrong element count.  "
                            + "Specified size " + requiredSize
                            + ", data contains " + stringArrays.size());
            }
            currentElem = (Element) currentElem.getParentNode();
            return stringArrays.toArray(new String[0][]);
        } catch (IOException ioe) {
            throw ioe;
        } catch (NumberFormatException nfe) {
            IOException io = new IOException(nfe.toString());
            io.initCause(nfe);
            throw io;
        } catch (DOMException de) {
            IOException io = new IOException(de.toString());
            io.initCause(de);
            throw io;
        }
    }

    public BitSet readBitSet(String name, BitSet defVal) throws IOException {
        String tmpString = currentElem.getAttribute(name);
        if (tmpString == null || tmpString.length() < 1) return defVal;
        try {
            BitSet set = new BitSet();
            String[] strings = parseTokens(tmpString);
            for (int i = 0; i < strings.length; i++) {
                int isSet = Integer.parseInt(strings[i]);
                if (isSet == 1) {
                        set.set(i);
                }
            }
            return set;
        } catch (NumberFormatException nfe) {
            IOException io = new IOException(nfe.toString());
            io.initCause(nfe);
            throw io;
        } catch (DOMException de) {
            IOException io = new IOException(de.toString());
            io.initCause(de);
            throw io;
        }
    }

    public Savable readSavable(String name, Savable defVal) throws IOException {
        Savable ret = defVal;
        if (name != null && name.equals(""))
            logger.warning("Reading Savable String with name \"\"?");
        try {
            Element tmpEl = null;
            if (name != null) {
                tmpEl = findChildElement(currentElem, name);
                if (tmpEl == null) {
                    return defVal;
                }
            } else if (isAtRoot) {
                tmpEl = doc.getDocumentElement();
                isAtRoot = false;
            } else {
                tmpEl = findFirstChildElement(currentElem);
            }
            currentElem = tmpEl;
            ret = readSavableFromCurrentElem(defVal);
            if (currentElem.getParentNode() instanceof Element) {
                currentElem = (Element) currentElem.getParentNode();
            } else {
                currentElem = null;
            }
        } catch (IOException ioe) {
            throw ioe;
        } catch (Exception e) {
            IOException io = new IOException(e.toString());
            io.initCause(e);
            throw io;
        }
        return ret;
    }

    private Savable readSavableFromCurrentElem(Savable defVal) throws
            InstantiationException, ClassNotFoundException,
            IOException, IllegalAccessException {
        Savable ret = defVal;
        Savable tmp = null;

        if (currentElem == null || currentElem.getNodeName().equals("null")) {
            return null;
        }
        String reference = currentElem.getAttribute("ref");
        if (reference.length() > 0) {
            ret = referencedSavables.get(reference);
        } else {
            String className = currentElem.getNodeName();
            if (defVal != null) {
                className = defVal.getClass().getName();
            } else if (currentElem.hasAttribute("class")) {
                className = currentElem.getAttribute("class");
            }
            tmp = SavableClassUtil.fromName(className, null);
            
            
            String versionsStr = currentElem.getAttribute("savable_versions");
            if (versionsStr != null && !versionsStr.equals("")){
                String[] versionStr = versionsStr.split(",");
                classHierarchyVersions = new int[versionStr.length];
                for (int i = 0; i < classHierarchyVersions.length; i++){
                    classHierarchyVersions[i] = Integer.parseInt(versionStr[i].trim());
                }
            }else{
                classHierarchyVersions = null;
            }
            
            String refID = currentElem.getAttribute("reference_ID");
            if (refID.length() < 1) refID = currentElem.getAttribute("id");
            if (refID.length() > 0) referencedSavables.put(refID, tmp);
            if (tmp != null) {
                // Allows reading versions from this savable
                savable = tmp;
                tmp.read(importer);
                ret = tmp;
            }
        }
        return ret;
    }

    public Savable[] readSavableArray(String name, Savable[] defVal) throws IOException {
        Savable[] ret = defVal;
        try {
            Element tmpEl = findChildElement(currentElem, name);
            if (tmpEl == null) {
                return defVal;
            }

            String sizeString = tmpEl.getAttribute("size");
            List<Savable> savables = new ArrayList<Savable>();
            for (currentElem = findFirstChildElement(tmpEl);
                    currentElem != null;
                    currentElem = findNextSiblingElement(currentElem)) {
                savables.add(readSavableFromCurrentElem(null));
            }
            if (sizeString.length() > 0) {
                int requiredSize = Integer.parseInt(sizeString);
                if (savables.size() != requiredSize)
                    throw new IOException("Wrong number of Savables for '"
                            + name + "'.  size says " + requiredSize
                            + ", data contains " + savables.size());
            }
            ret = savables.toArray(new Savable[0]);
            currentElem = (Element) tmpEl.getParentNode();
            return ret;
        } catch (IOException ioe) {
            throw ioe;
        } catch (Exception e) {
            IOException io = new IOException(e.toString());
            io.initCause(e);
            throw io;
        }
    }

    public Savable[][] readSavableArray2D(String name, Savable[][] defVal) throws IOException {
        Savable[][] ret = defVal;
        try {
            Element tmpEl = findChildElement(currentElem, name);
            if (tmpEl == null) {
                return defVal;
            }

            int size_outer = Integer.parseInt(tmpEl.getAttribute("size_outer"));
            int size_inner = Integer.parseInt(tmpEl.getAttribute("size_outer"));

            Savable[][] tmp = new Savable[size_outer][size_inner];
            currentElem = findFirstChildElement(tmpEl);
            for (int i = 0; i < size_outer; i++) {
                for (int j = 0; j < size_inner; j++) {
                    tmp[i][j] = (readSavableFromCurrentElem(null));
                    if (i == size_outer - 1 && j == size_inner - 1) {
                        break;
                    }
                    currentElem = findNextSiblingElement(currentElem);
                }
            }
            ret = tmp;
            currentElem = (Element) tmpEl.getParentNode();
            return ret;
        } catch (IOException ioe) {
            throw ioe;
        } catch (Exception e) {
            IOException io = new IOException(e.toString());
            io.initCause(e);
            throw io;
        }
    }

    public ArrayList<Savable> readSavableArrayList(String name, ArrayList defVal) throws IOException {
        try {
            Element tmpEl = findChildElement(currentElem, name);
            if (tmpEl == null) {
                return defVal;
            }

            String sizeString = tmpEl.getAttribute("size");
            ArrayList<Savable> savables = new ArrayList<Savable>();
            for (currentElem = findFirstChildElement(tmpEl);
                    currentElem != null;
                    currentElem = findNextSiblingElement(currentElem)) {
                savables.add(readSavableFromCurrentElem(null));
            }
            if (sizeString.length() > 0) {
                int requiredSize = Integer.parseInt(sizeString);
                if (savables.size() != requiredSize)
                    throw new IOException(
                            "Wrong number of Savable arrays for '" + name
                            + "'.  size says " + requiredSize
                            + ", data contains " + savables.size());
            }
            currentElem = (Element) tmpEl.getParentNode();
            return savables;
        } catch (IOException ioe) {
            throw ioe;
        } catch (Exception e) {
            IOException io = new IOException(e.toString());
            io.initCause(e);
            throw io;
        }
    }

    public ArrayList<Savable>[] readSavableArrayListArray(
            String name, ArrayList[] defVal) throws IOException {
        try {
            Element tmpEl = findChildElement(currentElem, name);
            if (tmpEl == null) {
                return defVal;
            }
            currentElem = tmpEl;

            String sizeString = tmpEl.getAttribute("size");
            int requiredSize = (sizeString.length() > 0)
                             ? Integer.parseInt(sizeString)
                             : -1;

            ArrayList<Savable> sal;
            List<ArrayList<Savable>> savableArrayLists =
                    new ArrayList<ArrayList<Savable>>();
            int i = -1;
            while (true) {
                sal = readSavableArrayList("SavableArrayList_" + ++i, null);
                if (sal == null && savableArrayLists.size() >= requiredSize)
                    break;
                savableArrayLists.add(sal);
            }

            if (requiredSize > -1 && savableArrayLists.size() != requiredSize)
                throw new IOException(
                        "String array contains wrong element count.  "
                        + "Specified size " + requiredSize
                        + ", data contains " + savableArrayLists.size());
            currentElem = (Element) tmpEl.getParentNode();
            return savableArrayLists.toArray(new ArrayList[0]);
        } catch (IOException ioe) {
            throw ioe;
        } catch (NumberFormatException nfe) {
            IOException io = new IOException(nfe.toString());
            io.initCause(nfe);
            throw io;
        } catch (DOMException de) {
            IOException io = new IOException(de.toString());
            io.initCause(de);
            throw io;
        }
    }

    public ArrayList<Savable>[][] readSavableArrayListArray2D(String name, ArrayList[][] defVal) throws IOException {
        try {
            Element tmpEl = findChildElement(currentElem, name);
            if (tmpEl == null) {
                return defVal;
            }
            currentElem = tmpEl;
            String sizeString = tmpEl.getAttribute("size");

            ArrayList<Savable>[] arr;
            List<ArrayList<Savable>[]> sall = new ArrayList<ArrayList<Savable>[]>();
            int i = -1;
            while ((arr = readSavableArrayListArray(
                    "SavableArrayListArray_" + ++i, null)) != null) sall.add(arr);
            if (sizeString.length() > 0) {
                int requiredSize = Integer.parseInt(sizeString);
                if (sall.size() != requiredSize)
                    throw new IOException(
                            "String array contains wrong element count.  "
                            + "Specified size " + requiredSize
                            + ", data contains " + sall.size());
            }
            currentElem = (Element) tmpEl.getParentNode();
            return sall.toArray(new ArrayList[0][]);
        } catch (IOException ioe) {
            throw ioe;
        } catch (Exception e) {
            IOException io = new IOException(e.toString());
            io.initCause(e);
            throw io;
        }
    }

    public ArrayList<FloatBuffer> readFloatBufferArrayList(
            String name, ArrayList<FloatBuffer> defVal) throws IOException {
        try {
            Element tmpEl = findChildElement(currentElem, name);
            if (tmpEl == null) {
                return defVal;
            }

            String sizeString = tmpEl.getAttribute("size");
            ArrayList<FloatBuffer> tmp = new ArrayList<FloatBuffer>();
            for (currentElem = findFirstChildElement(tmpEl);
                    currentElem != null;
                    currentElem = findNextSiblingElement(currentElem)) {
                tmp.add(readFloatBuffer(null, null));
            }
            if (sizeString.length() > 0) {
                int requiredSize = Integer.parseInt(sizeString);
                if (tmp.size() != requiredSize)
                    throw new IOException(
                            "String array contains wrong element count.  "
                            + "Specified size " + requiredSize
                            + ", data contains " + tmp.size());
            }
            currentElem = (Element) tmpEl.getParentNode();
            return tmp;
        } catch (IOException ioe) {
            throw ioe;
        } catch (NumberFormatException nfe) {
            IOException io = new IOException(nfe.toString());
            io.initCause(nfe);
            throw io;
        } catch (DOMException de) {
            IOException io = new IOException(de.toString());
            io.initCause(de);
            throw io;
        }
    }

    public Map<? extends Savable, ? extends Savable> readSavableMap(String name, Map<? extends Savable, ? extends Savable> defVal) throws IOException {
        Map<Savable, Savable> ret;
        Element tempEl;

        if (name != null) {
                tempEl = findChildElement(currentElem, name);
        } else {
                tempEl = currentElem;
        }
        ret = new HashMap<Savable, Savable>();

        NodeList nodes = tempEl.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
                Node n = nodes.item(i);
            if (n instanceof Element && n.getNodeName().equals("MapEntry")) {
                Element elem = (Element) n;
                        currentElem = elem;
                        Savable key = readSavable(XMLExporter.ELEMENT_KEY, null);
                        Savable val = readSavable(XMLExporter.ELEMENT_VALUE, null);
                        ret.put(key, val);
                }
        }
        currentElem = (Element) tempEl.getParentNode();
        return ret;
    }

    public Map<String, ? extends Savable> readStringSavableMap(String name, Map<String, ? extends Savable> defVal) throws IOException {
        Map<String, Savable> ret = null;
        Element tempEl;

        if (name != null) {
                tempEl = findChildElement(currentElem, name);
        } else {
                tempEl = currentElem;
        }
        if (tempEl != null) {
                ret = new HashMap<String, Savable>();

                NodeList nodes = tempEl.getChildNodes();
                    for (int i = 0; i < nodes.getLength(); i++) {
                                Node n = nodes.item(i);
                                if (n instanceof Element && n.getNodeName().equals("MapEntry")) {
                                        Element elem = (Element) n;
                                        currentElem = elem;
                                        String key = currentElem.getAttribute("key");
                                        Savable val = readSavable("Savable", null);
                                        ret.put(key, val);
                                }
                        }
        } else {
                return defVal;
            }
        currentElem = (Element) tempEl.getParentNode();
        return ret;
    }

    public IntMap<? extends Savable> readIntSavableMap(String name, IntMap<? extends Savable> defVal) throws IOException {
        IntMap<Savable> ret = null;
        Element tempEl;

        if (name != null) {
                tempEl = findChildElement(currentElem, name);
        } else {
                tempEl = currentElem;
        }
        if (tempEl != null) {
                ret = new IntMap<Savable>();

                NodeList nodes = tempEl.getChildNodes();
                    for (int i = 0; i < nodes.getLength(); i++) {
                                Node n = nodes.item(i);
                                if (n instanceof Element && n.getNodeName().equals("MapEntry")) {
                                        Element elem = (Element) n;
                                        currentElem = elem;
                                        int key = Integer.parseInt(currentElem.getAttribute("key"));
                                        Savable val = readSavable("Savable", null);
                                        ret.put(key, val);
                                }
                        }
        } else {
                return defVal;
            }
        currentElem = (Element) tempEl.getParentNode();
        return ret;
    }

    /**
     * reads from currentElem if name is null
     */
    public FloatBuffer readFloatBuffer(String name, FloatBuffer defVal) throws IOException {
        try {
            Element tmpEl;
            if (name != null) {
                tmpEl = findChildElement(currentElem, name);
            } else {
                tmpEl = currentElem;
            }
            if (tmpEl == null) {
                return defVal;
            }
            String sizeString = tmpEl.getAttribute("size");
            String[] strings = parseTokens(tmpEl.getAttribute("data"));
            if (sizeString.length() > 0) {
                int requiredSize = Integer.parseInt(sizeString);
                if (strings.length != requiredSize)
                    throw new IOException("Wrong number of float buffers for '"
                            + name + "'.  size says " + requiredSize
                            + ", data contains " + strings.length);
            }
            FloatBuffer tmp = BufferUtils.createFloatBuffer(strings.length);
            for (String s : strings) tmp.put(Float.parseFloat(s));
            tmp.flip();
            return tmp;
        } catch (IOException ioe) {
            throw ioe;
        } catch (NumberFormatException nfe) {
            IOException io = new IOException(nfe.toString());
            io.initCause(nfe);
            throw io;
        } catch (DOMException de) {
            IOException io = new IOException(de.toString());
            io.initCause(de);
            throw io;
        }
    }

    public IntBuffer readIntBuffer(String name, IntBuffer defVal) throws IOException {
        try {
            Element tmpEl = findChildElement(currentElem, name);
            if (tmpEl == null) {
                return defVal;
            }

            String sizeString = tmpEl.getAttribute("size");
            String[] strings = parseTokens(tmpEl.getAttribute("data"));
            if (sizeString.length() > 0) {
                int requiredSize = Integer.parseInt(sizeString);
                if (strings.length != requiredSize)
                    throw new IOException("Wrong number of int buffers for '"
                            + name + "'.  size says " + requiredSize
                            + ", data contains " + strings.length);
            }
            IntBuffer tmp = BufferUtils.createIntBuffer(strings.length);
            for (String s : strings) tmp.put(Integer.parseInt(s));
            tmp.flip();
            return tmp;
        } catch (IOException ioe) {
            throw ioe;
        } catch (NumberFormatException nfe) {
            IOException io = new IOException(nfe.toString());
            io.initCause(nfe);
            throw io;
        } catch (DOMException de) {
            IOException io = new IOException(de.toString());
            io.initCause(de);
            throw io;
        }
    }

    public ByteBuffer readByteBuffer(String name, ByteBuffer defVal) throws IOException {
        try {
            Element tmpEl = findChildElement(currentElem, name);
            if (tmpEl == null) {
                return defVal;
            }

            String sizeString = tmpEl.getAttribute("size");
            String[] strings = parseTokens(tmpEl.getAttribute("data"));
            if (sizeString.length() > 0) {
                int requiredSize = Integer.parseInt(sizeString);
                if (strings.length != requiredSize)
                    throw new IOException("Wrong number of byte buffers for '"
                            + name + "'.  size says " + requiredSize
                            + ", data contains " + strings.length);
            }
            ByteBuffer tmp = BufferUtils.createByteBuffer(strings.length);
            for (String s : strings) tmp.put(Byte.valueOf(s));
            tmp.flip();
            return tmp;
        } catch (IOException ioe) {
            throw ioe;
        } catch (NumberFormatException nfe) {
            IOException io = new IOException(nfe.toString());
            io.initCause(nfe);
            throw io;
        } catch (DOMException de) {
            IOException io = new IOException(de.toString());
            io.initCause(de);
            throw io;
        }
    }

    public ShortBuffer readShortBuffer(String name, ShortBuffer defVal) throws IOException {
        try {
            Element tmpEl = findChildElement(currentElem, name);
            if (tmpEl == null) {
                return defVal;
            }

            String sizeString = tmpEl.getAttribute("size");
            String[] strings = parseTokens(tmpEl.getAttribute("data"));
            if (sizeString.length() > 0) {
                int requiredSize = Integer.parseInt(sizeString);
                if (strings.length != requiredSize)
                    throw new IOException("Wrong number of short buffers for '"
                            + name + "'.  size says " + requiredSize
                            + ", data contains " + strings.length);
            }
            ShortBuffer tmp = BufferUtils.createShortBuffer(strings.length);
            for (String s : strings) tmp.put(Short.valueOf(s));
            tmp.flip();
            return tmp;
        } catch (IOException ioe) {
            throw ioe;
        } catch (NumberFormatException nfe) {
            IOException io = new IOException(nfe.toString());
            io.initCause(nfe);
            throw io;
        } catch (DOMException de) {
            IOException io = new IOException(de.toString());
            io.initCause(de);
            throw io;
        }
    }

        public ArrayList<ByteBuffer> readByteBufferArrayList(String name, ArrayList<ByteBuffer> defVal) throws IOException {
        try {
            Element tmpEl = findChildElement(currentElem, name);
            if (tmpEl == null) {
                return defVal;
            }

            String sizeString = tmpEl.getAttribute("size");
            ArrayList<ByteBuffer> tmp = new ArrayList<ByteBuffer>();
            for (currentElem = findFirstChildElement(tmpEl);
                    currentElem != null;
                    currentElem = findNextSiblingElement(currentElem)) {
                tmp.add(readByteBuffer(null, null));
            }
            if (sizeString.length() > 0) {
                int requiredSize = Integer.parseInt(sizeString);
                if (tmp.size() != requiredSize)
                    throw new IOException("Wrong number of short buffers for '"
                            + name + "'.  size says " + requiredSize
                            + ", data contains " + tmp.size());
            }
            currentElem = (Element) tmpEl.getParentNode();
            return tmp;
        } catch (IOException ioe) {
            throw ioe;
        } catch (NumberFormatException nfe) {
            IOException io = new IOException(nfe.toString());
            io.initCause(nfe);
            throw io;
        } catch (DOMException de) {
            IOException io = new IOException(de.toString());
            io.initCause(de);
            throw io;
        }
        }

        public <T extends Enum<T>> T readEnum(String name, Class<T> enumType,
                        T defVal) throws IOException {
        T ret = defVal;
        try {
            String eVal = currentElem.getAttribute(name);
            if (eVal != null && eVal.length() > 0) {
                ret = Enum.valueOf(enumType, eVal);
            }
        } catch (Exception e) {
            IOException io = new IOException(e.toString());
            io.initCause(e);
            throw io;
        }
        return ret;
        }

    private static final String[] zeroStrings = new String[0];

    protected String[] parseTokens(String inString) {
        String[] outStrings = inString.split("\\s+");
        return (outStrings.length == 1 && outStrings[0].length() == 0)
               ? zeroStrings
               : outStrings;
    }
}
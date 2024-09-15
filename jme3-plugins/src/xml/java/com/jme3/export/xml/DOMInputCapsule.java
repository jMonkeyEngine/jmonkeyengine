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

import com.jme3.export.InputCapsule;
import com.jme3.export.Savable;
import com.jme3.export.SavableClassUtil;
import com.jme3.util.BufferUtils;
import com.jme3.util.IntMap;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.*;
import java.util.logging.Logger;
import org.w3c.dom.*;

/**
 * Part of the jME XML IO system as introduced in the Google Code jmexml project.
 *
 * @author Kai Rabien (hevee) - original author of the code.google.com jmexml project
 * @author Doug Daniels (dougnukem) - adjustments for jME 2.0 and Java 1.5
 * @author blaine
 */
public class DOMInputCapsule implements InputCapsule {
    private static final Logger logger =
        Logger.getLogger(DOMInputCapsule.class .getName());

    private Document doc;
    private Element currentElement;
    private XMLImporter importer;
    private boolean isAtRoot = true;
    private Map<String, Savable> referencedSavables = new HashMap<>();
    
    private int[] classHierarchyVersions;
    private Savable savable;

    public DOMInputCapsule(Document doc, XMLImporter importer) {
        this.doc = doc;
        this.importer = importer;
        currentElement = doc.getDocumentElement();
        
        // file version is always unprefixed for backwards compatibility
        String version = currentElement.getAttribute("format_version");
        importer.formatVersion = version.equals("") ? 0 : Integer.parseInt(version);
    }

    @Override
    public int getSavableVersion(Class<? extends Savable> desiredClass) {
        if (classHierarchyVersions != null){
            return SavableClassUtil.getSavedSavableVersion(savable, desiredClass, 
                                                        classHierarchyVersions, importer.getFormatVersion());
        }else{
            return 0;
        }
    }

    private Element findFirstChildElement(Element parent) {
        Node ret = parent.getFirstChild();
        while (ret != null && (!(ret instanceof Element))) {
            ret = ret.getNextSibling();
        }
        return (Element) ret;
    }

    private Element findChildElement(String name) {
        if (currentElement == null) {
            return null;
        }
        Node ret = currentElement.getFirstChild();
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

    // helper method to reduce duplicate code.  checks that number of tokens in the "data" attribute matches the "size" attribute
    // and returns an array of parsed primitives.
    private Object readPrimitiveArrayHelper(Element element, String primType) throws IOException {
        if (element == null) {
            return null;
        }

        String sizeString = XMLUtils.getAttribute(importer.getFormatVersion(), element, "size");

        if (sizeString.isEmpty()) {
            return null;
        }

        String[] tokens = parseTokens(XMLUtils.getAttribute(importer.getFormatVersion(), element, "data"));

        if(!sizeString.isEmpty()) {
            try {
                int requiredSize = Integer.parseInt(sizeString);
                if (tokens.length != requiredSize) {
                    throw new IOException("Wrong token count for '" + element.getTagName()
                            + "'.  size says " + requiredSize
                            + ", data contains "
                            + tokens.length);
                }
            } catch (NumberFormatException ex) {
                throw new IOException("Invalid size for '" + element.getTagName() + "': " + sizeString);
            }
        }

        try {
            switch (primType) {
                case "byte":
                    byte[] byteArray = new byte[tokens.length];
                    for (int i = 0; i < tokens.length; i++) byteArray[i] = Byte.parseByte(tokens[i]);
                    return byteArray;
                case "short":
                    short[] shortArray = new short[tokens.length];
                    for (int i = 0; i < tokens.length; i++) shortArray[i] = Short.parseShort(tokens[i]);
                    return shortArray;
                case "int":
                    int[] intArray = new int[tokens.length];
                    for (int i = 0; i < tokens.length; i++) intArray[i] = Integer.parseInt(tokens[i]);
                    return intArray;
                case "long":
                    long[] longArray = new long[tokens.length];
                    for (int i = 0; i < tokens.length; i++) longArray[i] = Long.parseLong(tokens[i]);
                    return longArray;
                case "float":
                    float[] floatArray = new float[tokens.length];
                    for (int i = 0; i < tokens.length; i++) floatArray[i] = Float.parseFloat(tokens[i]);
                    return floatArray;
                case "double":
                    double[] doubleArray = new double[tokens.length];
                    for (int i = 0; i < tokens.length; i++) doubleArray[i] = Double.parseDouble(tokens[i]);
                    return doubleArray;
                case "boolean":
                    boolean[] booleanArray = new boolean[tokens.length];
                    for (int i = 0; i < tokens.length; i++) booleanArray[i] = Boolean.parseBoolean(tokens[i]);
                    return booleanArray;
                default:
                    throw new IOException();    // will never happen
            }
        } catch(NumberFormatException nfe) {
            throw new IOException(nfe);
        }
    }

    // helper method to reduce duplicate code.  checks that number of child elements matches the "size" attribute
    // and returns a convenient list of child elements.
    private List<Element> getObjectArrayElements(Element element) throws IOException {
        if (element == null) {
            return null;
        }

        String sizeString = XMLUtils.getAttribute(importer.getFormatVersion(), element, "size");

        if (sizeString.isEmpty()) {
            return null;
        }

        NodeList nodes = element.getChildNodes();

        List<Element> elements = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element) {
                elements.add((Element) node);
            }
        }

        try {
            int requiredSize = Integer.parseInt(sizeString);
            if (elements.size() != requiredSize) {
                throw new IOException("DOMInputCapsule.getObjectArrayElements(): Wrong element count for '" + element.getTagName()
                        + "'.  size says " + requiredSize
                        + ", data contains "
                        + elements.size());
            }
        } catch (NumberFormatException ex) {
            throw new IOException("Invalid size for '" + element.getTagName() + "': " + sizeString);
        }

        return elements;
    }

    @Override
    public byte readByte(String name, byte defVal) throws IOException {
        String attribute = XMLUtils.getAttribute(importer.getFormatVersion(), currentElement, name);

        if (attribute.isEmpty()) {
            return defVal;
        }

        try {
            return Byte.parseByte(attribute);
        } catch (NumberFormatException nfe) {
            throw new IOException(nfe);
        }
    }

    @Override
    public byte[] readByteArray(String name, byte[] defVal) throws IOException {
        byte[] array = (byte[]) readPrimitiveArrayHelper(findChildElement(name), "byte");
        return array != null ? array : defVal;
    }

    @Override
    public byte[][] readByteArray2D(String name, byte[][] defVal) throws IOException {
        List<Element> arrayEntryElements = getObjectArrayElements(findChildElement(name));

        if (arrayEntryElements == null) {
            return defVal;
        }

        byte[][] arrays = new byte[arrayEntryElements.size()][];
        for (int i = 0; i < arrayEntryElements.size(); i++) {
            arrays[i] = (byte[]) readPrimitiveArrayHelper(arrayEntryElements.get(i), "byte");
        }

        return arrays;
    }

    @Override
    public short readShort(String name, short defVal) throws IOException {
        String attribute = XMLUtils.getAttribute(importer.getFormatVersion(), currentElement, name);
        
        if (attribute.isEmpty()) {
            return defVal;
        }

        try {
            return Short.parseShort(attribute);
        } catch (NumberFormatException nfe) {
            throw new IOException(nfe);
        }
    }

    @Override
    public short[] readShortArray(String name, short[] defVal) throws IOException {
        short[] array = (short[]) readPrimitiveArrayHelper(findChildElement(name), "short");
        return array != null ? array : defVal;
    }

    @Override
    public short[][] readShortArray2D(String name, short[][] defVal) throws IOException {
        List<Element> arrayEntryElements = getObjectArrayElements(findChildElement(name));

        if (arrayEntryElements == null) {
            return defVal;
        }

        short[][] arrays = new short[arrayEntryElements.size()][];
        for (int i = 0; i < arrayEntryElements.size(); i++) {
            arrays[i] = (short[]) readPrimitiveArrayHelper(arrayEntryElements.get(i), "short");
        }

        return arrays;
    }

    @Override
    public int readInt(String name, int defVal) throws IOException {
        String attribute = XMLUtils.getAttribute(importer.getFormatVersion(), currentElement, name);
        
        if (attribute.isEmpty()) {
            return defVal;
        }

        try {
            return Integer.parseInt(attribute);
        } catch (NumberFormatException nfe) {
            throw new IOException(nfe);
        }
    }

    @Override
    public int[] readIntArray(String name, int[] defVal) throws IOException {
        int[] array = (int[]) readPrimitiveArrayHelper(findChildElement(name), "int");
        return array != null ? array : defVal;
    }

    @Override
    public int[][] readIntArray2D(String name, int[][] defVal) throws IOException {
        List<Element> arrayEntryElements = getObjectArrayElements(findChildElement(name));

        if (arrayEntryElements == null) {
            return defVal;
        }

        int[][] arrays = new int[arrayEntryElements.size()][];
        for (int i = 0; i < arrayEntryElements.size(); i++) {
            arrays[i] = (int[]) readPrimitiveArrayHelper(arrayEntryElements.get(i), "int");
        }

        return arrays;
    }

    @Override
    public long readLong(String name, long defVal) throws IOException {
        String attribute = XMLUtils.getAttribute(importer.getFormatVersion(), currentElement, name);
        
        if (attribute.isEmpty()) {
            return defVal;
        }

        try {
            return Long.parseLong(attribute);
        } catch (NumberFormatException nfe) {
            throw new IOException(nfe);
        }
    }

    @Override
    public long[] readLongArray(String name, long[] defVal) throws IOException {
        long[] array = (long[]) readPrimitiveArrayHelper(findChildElement(name), "long");
        return array != null ? array : defVal;
    }

    @Override
    public long[][] readLongArray2D(String name, long[][] defVal) throws IOException {
        List<Element> arrayEntryElements = getObjectArrayElements(findChildElement(name));

        if (arrayEntryElements == null) {
            return defVal;
        }

        long[][] arrays = new long[arrayEntryElements.size()][];
        for (int i = 0; i < arrayEntryElements.size(); i++) {
            arrays[i] = (long[]) readPrimitiveArrayHelper(arrayEntryElements.get(i), "long");
        }

        return arrays;
    }

    @Override
    public float readFloat(String name, float defVal) throws IOException {
        String attribute = XMLUtils.getAttribute(importer.getFormatVersion(), currentElement, name);
        
        if (attribute.isEmpty()) {
            return defVal;
        }

        try {
            return Float.parseFloat(attribute);
        } catch (NumberFormatException nfe) {
            throw new IOException(nfe);
        }
    }

    @Override
    public float[] readFloatArray(String name, float[] defVal) throws IOException {
        float[] array = (float[]) readPrimitiveArrayHelper(findChildElement(name), "float");
        return array != null ? array : defVal;
    }

    @Override
    public float[][] readFloatArray2D(String name, float[][] defVal) throws IOException {
        List<Element> arrayEntryElements = getObjectArrayElements(findChildElement(name));

        if (arrayEntryElements == null) {
            return defVal;
        }

        float[][] arrays = new float[arrayEntryElements.size()][];
        for (int i = 0; i < arrayEntryElements.size(); i++) {
            arrays[i] = (float[]) readPrimitiveArrayHelper(arrayEntryElements.get(i), "float");
        }

        return arrays;
    }

    @Override
    public double readDouble(String name, double defVal) throws IOException {
        String attribute = XMLUtils.getAttribute(importer.getFormatVersion(), currentElement, name);
        
        if (attribute.isEmpty()) {
            return defVal;
        }

        try {
            return Double.parseDouble(attribute);
        } catch (NumberFormatException nfe) {
            throw new IOException(nfe);
        }
    }

    @Override
    public double[] readDoubleArray(String name, double[] defVal) throws IOException {
        double[] array = (double[]) readPrimitiveArrayHelper(findChildElement(name), "double");
        return array != null ? array : defVal;
    }

    @Override
    public double[][] readDoubleArray2D(String name, double[][] defVal) throws IOException {
        List<Element> arrayEntryElements = getObjectArrayElements(findChildElement(name));

        if (arrayEntryElements == null) {
            return defVal;
        }

        double[][] arrays = new double[arrayEntryElements.size()][];
        for (int i = 0; i < arrayEntryElements.size(); i++) {
            arrays[i] = (double[]) readPrimitiveArrayHelper(arrayEntryElements.get(i), "double");
        }

        return arrays;
    }

    @Override
    public boolean readBoolean(String name, boolean defVal) throws IOException {
        String attribute = XMLUtils.getAttribute(importer.getFormatVersion(), currentElement, name);
        
        if (attribute.isEmpty()) {
            return defVal;
        }

        //parseBoolean doesn't throw anything, just returns false if the string isn't "true", ignoring case.
        return Boolean.parseBoolean(attribute);
    }

    @Override
    public boolean[] readBooleanArray(String name, boolean[] defVal) throws IOException {
        boolean[] array = (boolean[]) readPrimitiveArrayHelper(findChildElement(name), "boolean");
        return array != null ? array : defVal;
    }

    @Override
    public boolean[][] readBooleanArray2D(String name, boolean[][] defVal) throws IOException {
        List<Element> arrayEntryElements = getObjectArrayElements(findChildElement(name));

        if (arrayEntryElements == null) {
            return defVal;
        }

        boolean[][] arrays = new boolean[arrayEntryElements.size()][];
        for (int i = 0; i < arrayEntryElements.size(); i++) {
            arrays[i] = (boolean[]) readPrimitiveArrayHelper(arrayEntryElements.get(i), "boolean");
        }

        return arrays;
    }

    @Override
    public String readString(String name, String defVal) throws IOException {
        String attribute = null;
        try {
            // Element.getAttribute() returns an empty string if the specified attribute does not exist.
            // see https://www.w3.org/2003/01/dom2-javadoc/org/w3c/dom/Element.html#getAttribute_java.lang.String_
            // somewhat confusing since the w3c JS api equivalent returns null as one would expect.
            // https://www.w3schools.com/jsref/met_element_getattribute.asp
            if (XMLUtils.hasAttribute(importer.getFormatVersion(), currentElement, name)) {
                attribute = XMLUtils.getAttribute(importer.getFormatVersion(), currentElement, name);
            }
        } catch (DOMException de) {
            throw new IOException(de.toString(), de);
        }

        if (attribute == null) {
            return defVal;
        }

        return attribute;
    }

    @Override
    public String[] readStringArray(String name, String[] defVal) throws IOException {
        List<Element> arrayElements = getObjectArrayElements(findChildElement(name));

        if (arrayElements == null) {
            return defVal;
        }

        Element oldElement = currentElement;

        String[] array = new String[arrayElements.size()];
        for (int i = 0; i < arrayElements.size(); i++) {
            currentElement = arrayElements.get(i);
            array[i] = readString("value", null);
        }

        currentElement = oldElement;

        return array;
    }

    @Override
    public String[][] readStringArray2D(String name, String[][] defVal) throws IOException {
        Element outerArrayElement = findChildElement(name);
        List<Element> innerArrayElements = getObjectArrayElements(outerArrayElement);

        if (innerArrayElements == null) {
            return defVal;
        }

        currentElement = outerArrayElement;

        String[][] arrays = new String[innerArrayElements.size()][];
        for (int i = 0; i < innerArrayElements.size(); i++) {
            arrays[i] = readStringArray(innerArrayElements.get(i).getTagName(), null);
        }

        currentElement = (Element) currentElement.getParentNode();

        return arrays;
    }

    @Override
    public <T extends Enum<T>> T readEnum(String name, Class<T> enumType, T defVal) throws IOException {
        String attribute = XMLUtils.getAttribute(importer.getFormatVersion(), currentElement, name);

        if (attribute.isEmpty()) {
            return defVal;
        }

        try {
            return Enum.valueOf(enumType, attribute);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IOException(e);
        }
    }

    @Override
    public BitSet readBitSet(String name, BitSet defVal) throws IOException {
        String attribute = null;
        try {
            attribute = XMLUtils.getAttribute(importer.getFormatVersion(), currentElement, name);
        } catch (DOMException ex) {
            throw new IOException(ex.toString(), ex);
        }

        if (attribute == null || attribute.isEmpty()) {
            return defVal;
        }
    
        String[] strings = parseTokens(attribute);
        BitSet bitSet = new BitSet();
        for (int i = 0; i < strings.length; i++) {
            if (strings[i].equals("1")) {
                bitSet.set(i);
            }
        }

        return bitSet;
    }

    @Override
    public Savable readSavable(String name, Savable defVal) throws IOException {
        Savable ret = defVal;
        if (name != null && name.equals(""))
            logger.warning("Reading Savable String with name \"\"?");
        try {
            Element tmpEl = null;
            if (name != null) {
                tmpEl = findChildElement(name);
                if (tmpEl == null) {
                    return defVal;
                }
            } else if (isAtRoot) {
                tmpEl = doc.getDocumentElement();
                isAtRoot = false;
            } else {
                tmpEl = findFirstChildElement(currentElement);
            }
            currentElement = tmpEl;
            ret = readSavableFromcurrentElement(defVal);
            if (currentElement.getParentNode() instanceof Element) {
                currentElement = (Element) currentElement.getParentNode();
            } else {
                currentElement = null;
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

    private Savable readSavableFromcurrentElement(Savable defVal) throws
            InstantiationException, ClassNotFoundException,
            NoSuchMethodException, InvocationTargetException,
            IOException, IllegalAccessException {
        Savable ret = defVal;
        Savable tmp = null;

        if (currentElement == null || currentElement.getNodeName().equals("null")) {
            return null;
        }
        String reference = XMLUtils.getAttribute(importer.getFormatVersion(), currentElement, "ref");
        if (reference.length() > 0) {
            ret = referencedSavables.get(reference);
        } else {
            String className = currentElement.getNodeName();
            if (XMLUtils.hasAttribute(importer.getFormatVersion(), currentElement, "class")) {
                className = XMLUtils.getAttribute(importer.getFormatVersion(), currentElement, "class");
            } else if (defVal != null) {
                className = defVal.getClass().getName();
            }
            tmp = SavableClassUtil.fromName(className);
            
            String versionsStr = XMLUtils.getAttribute(importer.getFormatVersion(), currentElement, "savable_versions");
            if (versionsStr != null && !versionsStr.equals("")){
                String[] versionStr = versionsStr.split(",");
                classHierarchyVersions = new int[versionStr.length];
                for (int i = 0; i < classHierarchyVersions.length; i++){
                    classHierarchyVersions[i] = Integer.parseInt(versionStr[i].trim());
                }
            }else{
                classHierarchyVersions = null;
            }
            
            String refID = XMLUtils.getAttribute(importer.getFormatVersion(), currentElement, "reference_ID");
            if (refID.length() < 1) refID = XMLUtils.getAttribute(importer.getFormatVersion(), currentElement, "id");
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

    @Override
    public Savable[] readSavableArray(String name, Savable[] defVal) throws IOException {
        Savable[] ret = defVal;
        try {
            Element tmpEl = findChildElement(name);
            if (tmpEl == null) {
                return defVal;
            }

            String sizeString = XMLUtils.getAttribute(importer.getFormatVersion(), tmpEl, "size");
            List<Savable> savables = new ArrayList<>();
            for (currentElement = findFirstChildElement(tmpEl);
                    currentElement != null;
                    currentElement = findNextSiblingElement(currentElement)) {
                savables.add(readSavableFromcurrentElement(null));
            }
            if (sizeString.length() > 0) {
                int requiredSize = Integer.parseInt(sizeString);
                if (savables.size() != requiredSize)
                    throw new IOException("Wrong number of Savables for '"
                            + name + "'.  size says " + requiredSize
                            + ", data contains " + savables.size());
            }
            ret = savables.toArray(new Savable[0]);
            currentElement = (Element) tmpEl.getParentNode();
            return ret;
        } catch (IOException ioe) {
            throw ioe;
        } catch (Exception e) {
            IOException io = new IOException(e.toString());
            io.initCause(e);
            throw io;
        }
    }

    @Override
    public Savable[][] readSavableArray2D(String name, Savable[][] defVal) throws IOException {
        Savable[][] ret = defVal;
        try {
            Element tmpEl = findChildElement(name);
            if (tmpEl == null) {
                return defVal;
            }

            int size_outer = Integer.parseInt(XMLUtils.getAttribute(importer.getFormatVersion(), tmpEl, "size_outer"));
            int size_inner = Integer.parseInt(XMLUtils.getAttribute(importer.getFormatVersion(), tmpEl, "size_outer"));

            Savable[][] tmp = new Savable[size_outer][size_inner];
            currentElement = findFirstChildElement(tmpEl);
            for (int i = 0; i < size_outer; i++) {
                for (int j = 0; j < size_inner; j++) {
                    tmp[i][j] = (readSavableFromcurrentElement(null));
                    if (i == size_outer - 1 && j == size_inner - 1) {
                        break;
                    }
                    currentElement = findNextSiblingElement(currentElement);
                }
            }
            ret = tmp;
            currentElement = (Element) tmpEl.getParentNode();
            return ret;
        } catch (IOException ioe) {
            throw ioe;
        } catch (Exception e) {
            IOException io = new IOException(e.toString());
            io.initCause(e);
            throw io;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public ArrayList<Savable> readSavableArrayList(String name, ArrayList defVal) throws IOException {
        try {
            Element tmpEl = findChildElement(name);
            if (tmpEl == null) {
                return defVal;
            }

            String sizeString = XMLUtils.getAttribute(importer.getFormatVersion(), tmpEl, "size");
            ArrayList<Savable> savables = new ArrayList<>();
            for (currentElement = findFirstChildElement(tmpEl);
                    currentElement != null;
                    currentElement = findNextSiblingElement(currentElement)) {
                savables.add(readSavableFromcurrentElement(null));
            }
            if (sizeString.length() > 0) {
                int requiredSize = Integer.parseInt(sizeString);
                if (savables.size() != requiredSize)
                    throw new IOException(
                            "Wrong number of Savable arrays for '" + name
                            + "'.  size says " + requiredSize
                            + ", data contains " + savables.size());
            }
            currentElement = (Element) tmpEl.getParentNode();
            return savables;
        } catch (IOException ioe) {
            throw ioe;
        } catch (Exception e) {
            IOException io = new IOException(e.toString());
            io.initCause(e);
            throw io;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public ArrayList<Savable>[] readSavableArrayListArray(
            String name, ArrayList[] defVal) throws IOException {
        try {
            Element tmpEl = findChildElement(name);
            if (tmpEl == null) {
                return defVal;
            }
            currentElement = tmpEl;

            String sizeString = XMLUtils.getAttribute(importer.getFormatVersion(), tmpEl, "size");
            int requiredSize = (sizeString.length() > 0)
                             ? Integer.parseInt(sizeString)
                             : -1;

            ArrayList<Savable> sal;
            List<ArrayList<Savable>> savableArrayLists =
                    new ArrayList<>();
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
            currentElement = (Element) tmpEl.getParentNode();
            return savableArrayLists.toArray(new ArrayList[0]);
        } catch (IOException ioe) {
            throw ioe;
        } catch (NumberFormatException | DOMException nfe) {
            IOException io = new IOException(nfe.toString());
            io.initCause(nfe);
            throw io;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public ArrayList<Savable>[][] readSavableArrayListArray2D(String name, ArrayList[][] defVal) throws IOException {
        try {
            Element tmpEl = findChildElement(name);
            if (tmpEl == null) {
                return defVal;
            }
            currentElement = tmpEl;
            String sizeString = XMLUtils.getAttribute(importer.getFormatVersion(), tmpEl, "size");

            ArrayList<Savable>[] arr;
            List<ArrayList<Savable>[]> sall = new ArrayList<>();
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
            currentElement = (Element) tmpEl.getParentNode();
            return sall.toArray(new ArrayList[0][]);
        } catch (IOException ioe) {
            throw ioe;
        } catch (Exception e) {
            IOException io = new IOException(e.toString());
            io.initCause(e);
            throw io;
        }
    }

    @Override
    public ArrayList<FloatBuffer> readFloatBufferArrayList(
            String name, ArrayList<FloatBuffer> defVal) throws IOException {
        try {
            Element tmpEl = findChildElement(name);
            if (tmpEl == null) {
                return defVal;
            }

            String sizeString = XMLUtils.getAttribute(importer.getFormatVersion(), tmpEl, "size");
            ArrayList<FloatBuffer> tmp = new ArrayList<>();
            for (currentElement = findFirstChildElement(tmpEl);
                    currentElement != null;
                    currentElement = findNextSiblingElement(currentElement)) {
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
            currentElement = (Element) tmpEl.getParentNode();
            return tmp;
        } catch (IOException ioe) {
            throw ioe;
        } catch (NumberFormatException | DOMException nfe) {
            IOException io = new IOException(nfe.toString());
            io.initCause(nfe);
            throw io;
        }
    }

    @Override
    public Map<? extends Savable, ? extends Savable> readSavableMap(String name, Map<? extends Savable, ? extends Savable> defVal) throws IOException {
        Map<Savable, Savable> ret;
        Element tempEl;

        if (name != null) {
                tempEl = findChildElement(name);
        } else {
                tempEl = currentElement;
        }
        ret = new HashMap<Savable, Savable>();

        NodeList nodes = tempEl.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
                Node n = nodes.item(i);
            if (n instanceof Element && n.getNodeName().equals("MapEntry")) {
                Element elem = (Element) n;
                        currentElement = elem;
                        Savable key = readSavable(XMLExporter.ELEMENT_KEY, null);
                        Savable val = readSavable(XMLExporter.ELEMENT_VALUE, null);
                        ret.put(key, val);
                }
        }
        currentElement = (Element) tempEl.getParentNode();
        return ret;
    }

    @Override
    public Map<String, ? extends Savable> readStringSavableMap(String name, Map<String, ? extends Savable> defVal) throws IOException {
        Map<String, Savable> ret = null;
        Element tempEl;

        if (name != null) {
                tempEl = findChildElement(name);
        } else {
                tempEl = currentElement;
        }
        if (tempEl != null) {
                ret = new HashMap<String, Savable>();

                NodeList nodes = tempEl.getChildNodes();
                    for (int i = 0; i < nodes.getLength(); i++) {
                                Node n = nodes.item(i);
                                if (n instanceof Element && n.getNodeName().equals("MapEntry")) {
                                        Element elem = (Element) n;
                                        currentElement = elem;
                                        String key = XMLUtils.getAttribute(importer.getFormatVersion(), currentElement, "key");
                                        Savable val = readSavable("Savable", null);
                                        ret.put(key, val);
                                }
                        }
        } else {
                return defVal;
            }
        currentElement = (Element) tempEl.getParentNode();
        return ret;
    }

    @Override
    public IntMap<? extends Savable> readIntSavableMap(String name, IntMap<? extends Savable> defVal) throws IOException {
        IntMap<Savable> ret = null;
        Element tempEl;

        if (name != null) {
                tempEl = findChildElement(name);
        } else {
                tempEl = currentElement;
        }
        if (tempEl != null) {
                ret = new IntMap<Savable>();

                NodeList nodes = tempEl.getChildNodes();
                    for (int i = 0; i < nodes.getLength(); i++) {
                                Node n = nodes.item(i);
                                if (n instanceof Element && n.getNodeName().equals("MapEntry")) {
                                        Element elem = (Element) n;
                                        currentElement = elem;
                                        int key = Integer.parseInt(XMLUtils.getAttribute(importer.getFormatVersion(), currentElement, "key"));
                                        Savable val = readSavable("Savable", null);
                                        ret.put(key, val);
                                }
                        }
        } else {
                return defVal;
            }
        currentElement = (Element) tempEl.getParentNode();
        return ret;
    }

    @Override
    public ByteBuffer readByteBuffer(String name, ByteBuffer defVal) throws IOException {
        byte[] array = readByteArray(name, null);

        if (array == null) {
            return defVal;
        }

        return (ByteBuffer) BufferUtils.createByteBuffer(array.length).put(array).rewind();
    }

    @Override
    public ShortBuffer readShortBuffer(String name, ShortBuffer defVal) throws IOException {
        short[] array = readShortArray(name, null);

        if (array == null) {
            return defVal;
        }

        return (ShortBuffer) BufferUtils.createShortBuffer(array.length).put(array).rewind();
    }

    @Override
    public IntBuffer readIntBuffer(String name, IntBuffer defVal) throws IOException {
        int[] array = readIntArray(name, null);

        if (array == null) {
            return defVal;
        }

        return (IntBuffer) BufferUtils.createIntBuffer(array.length).put(array).rewind();
    }

    @Override
    public FloatBuffer readFloatBuffer(String name, FloatBuffer defVal) throws IOException {
        float[] array = readFloatArray(name, null);

        if (array == null) {
            return defVal;
        }

        return (FloatBuffer) BufferUtils.createFloatBuffer(array.length).put(array).rewind();
    }

    @Override
        public ArrayList<ByteBuffer> readByteBufferArrayList(String name, ArrayList<ByteBuffer> defVal) throws IOException {
        try {
            Element tmpEl = findChildElement(name);
            if (tmpEl == null) {
                return defVal;
            }

            String sizeString = XMLUtils.getAttribute(importer.getFormatVersion(), tmpEl, "size");
            ArrayList<ByteBuffer> tmp = new ArrayList<>();
            for (currentElement = findFirstChildElement(tmpEl);
                    currentElement != null;
                    currentElement = findNextSiblingElement(currentElement)) {
                tmp.add(readByteBuffer(null, null));
            }
            if (sizeString.length() > 0) {
                int requiredSize = Integer.parseInt(sizeString);
                if (tmp.size() != requiredSize)
                    throw new IOException("Wrong number of short buffers for '"
                            + name + "'.  size says " + requiredSize
                            + ", data contains " + tmp.size());
            }
            currentElement = (Element) tmpEl.getParentNode();
            return tmp;
        } catch (IOException ioe) {
            throw ioe;
        } catch (NumberFormatException | DOMException nfe) {
            IOException io = new IOException(nfe.toString());
            io.initCause(nfe);
            throw io;
        }
        }

    private static final String[] zeroStrings = new String[0];

    protected String[] parseTokens(String inString) {
        String[] outStrings = inString.split("\\s+");
        return (outStrings.length == 1 && outStrings[0].length() == 0)
               ? zeroStrings
               : outStrings;
    }
}

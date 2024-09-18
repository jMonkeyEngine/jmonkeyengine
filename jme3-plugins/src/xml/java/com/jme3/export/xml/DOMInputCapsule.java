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
    private static final Logger logger = Logger.getLogger(DOMInputCapsule.class .getName());

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
            return SavableClassUtil.getSavedSavableVersion(savable, desiredClass, classHierarchyVersions, importer.getFormatVersion());
        }else{
            return 0;
        }
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

    // helper method to reduce duplicate code.  checks that number of tokens in the "data" attribute matches the "size" attribute
    // and returns an array of parsed primitives.
    private Object readPrimitiveArrayHelper(Element element, String primType) throws IOException {
        if (element == null) {
            return null;
        }

        String sizeString = XMLUtils.getAttribute(importer.getFormatVersion(), element, XMLExporter.ATTRIBUTE_SIZE);

        if (sizeString.isEmpty()) {
            return null;
        }

        String[] tokens = parseTokens(XMLUtils.getAttribute(importer.getFormatVersion(), element, XMLExporter.ATTRIBUTE_DATA));

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

        String sizeString = XMLUtils.getAttribute(importer.getFormatVersion(), element, XMLExporter.ATTRIBUTE_SIZE);

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

        // Element.getAttribute() returns an empty string if the specified attribute does not exist.
        // see https://www.w3.org/2003/01/dom2-javadoc/org/w3c/dom/Element.html#getAttribute_java.lang.String_
        // somewhat confusing since the w3c JS api equivalent returns null as one would expect.
        // https://www.w3schools.com/jsref/met_element_getattribute.asp
        if (XMLUtils.hasAttribute(importer.getFormatVersion(), currentElement, name)) {
            attribute = XMLUtils.getAttribute(importer.getFormatVersion(), currentElement, name);
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
        
        attribute = XMLUtils.getAttribute(importer.getFormatVersion(), currentElement, name);

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

    private Savable readSavableFromCurrentElement(Savable defVal) throws IOException {
        if (currentElement == null || !currentElement.hasAttributes()) {
            return defVal;
        }

        String reference = XMLUtils.getAttribute(importer.getFormatVersion(), currentElement, XMLExporter.ATTRIBUTE_REFERENCE);
        if (!reference.isEmpty()) {
            return referencedSavables.get(reference);
        } else {
            // for backwards compatibility with old XML files.  previous version of DOMOutputCapsule would only write the class attribute
            // if the element name wasn't the class name.
            String className = currentElement.getNodeName();
            if (XMLUtils.hasAttribute(importer.getFormatVersion(), currentElement, XMLExporter.ATTRIBUTE_CLASS)) {
                className = XMLUtils.getAttribute(importer.getFormatVersion(), currentElement, XMLExporter.ATTRIBUTE_CLASS);
            } else if (defVal != null) {
                className = defVal.getClass().getName();
            }

            Savable tmp = null;
            try {
                tmp = SavableClassUtil.fromName(className);
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
                throw new IOException(e);
            }
            
            String versionsStr = XMLUtils.getAttribute(importer.getFormatVersion(), currentElement, XMLExporter.ATTRIBUTE_SAVABLE_VERSIONS);
            if (versionsStr != null && !versionsStr.equals("")){
                String[] versionStr = versionsStr.split(",");
                classHierarchyVersions = new int[versionStr.length];
                try {
                    for (int i = 0; i < classHierarchyVersions.length; i++) {
                        classHierarchyVersions[i] = Integer.parseInt(versionStr[i].trim());
                    }
                } catch (NumberFormatException nfe) {
                    throw new IOException(nfe);
                }
            }else{
                classHierarchyVersions = null;
            }
            
            String refID = XMLUtils.getAttribute(importer.getFormatVersion(), currentElement, XMLExporter.ATTRIBUTE_REFERENCE_ID);

            // what does this line do?  guessing backwards compatibility?
            if (refID.isEmpty()) refID = XMLUtils.getAttribute(importer.getFormatVersion(), currentElement, "id");

            if (!refID.isEmpty()) referencedSavables.put(refID, tmp);
            
            if (tmp != null) {
                // Allows reading versions from this savable
                savable = tmp;

                tmp.read(importer);

                return tmp;
            } else {
                return defVal;
            }
        }
    }

    @Override
    public Savable readSavable(String name, Savable defVal) throws IOException {
        Savable ret = defVal;

        if (name != null && name.equals(""))
            logger.warning("Reading Savable String with name \"\"?");

        Element old = currentElement;

        if (isAtRoot) {
            currentElement = doc.getDocumentElement();
            isAtRoot = false;
        } else {
            currentElement = findChildElement(name);
        }

        ret = readSavableFromCurrentElement(defVal);

        currentElement = old;

        return ret;
    }

    @Override
    public Savable[] readSavableArray(String name, Savable[] defVal) throws IOException {
        Element arrayElement = findChildElement(name);

        if (arrayElement == null || !arrayElement.hasAttributes()) {
            return defVal;
        }

        List<Element> arrayElements = getObjectArrayElements(arrayElement);

        Savable[] savableArray = new Savable[arrayElements.size()];

        Element old = currentElement;

        for (int i = 0; i < arrayElements.size(); i++) {
            currentElement = arrayElements.get(i);
            savableArray[i] = readSavableFromCurrentElement(null);
        }

        currentElement = old;

        return savableArray;
    }

    @Override
    public Savable[][] readSavableArray2D(String name, Savable[][] defVal) throws IOException {
        Element outerArrayElement = findChildElement(name);

        if (outerArrayElement == null || !outerArrayElement.hasAttributes()) {
            return defVal;
        }

        List<Element> innerArrayElements = getObjectArrayElements(outerArrayElement);

        Savable[][] savableArray2D = new Savable[innerArrayElements.size()][];

        Element old = currentElement;

        for (int i = 0; i < innerArrayElements.size(); i++) {
            List<Element> savableElements = getObjectArrayElements(innerArrayElements.get(i));

            if (savableElements == null) {
                continue;
            }

            savableArray2D[i] = new Savable[savableElements.size()];
            for (int j = 0; j < savableElements.size(); j++) {
                currentElement = savableElements.get(j);
                savableArray2D[i][j] = readSavableFromCurrentElement(null);
            }
        }

        currentElement = old;

        return savableArray2D;
    }

    @Override
    public ByteBuffer readByteBuffer(String name, ByteBuffer defVal) throws IOException {
        Element element = findChildElement(name);

        byte[] array = (byte[]) readPrimitiveArrayHelper(element, "byte");

        if (array == null) {
            return defVal;
        }

        int position = 0;
        String positionString = XMLUtils.getAttribute(importer.getFormatVersion(), element, XMLExporter.ATTRIBUTE_POSITION);
        if (!positionString.isEmpty()) {
            try {
                position = Integer.parseInt(positionString);
            } catch (NumberFormatException nfe) {
                throw new IOException(nfe);
            }
        }

        return (ByteBuffer) BufferUtils.createByteBuffer(array.length).put(array).position(position);
    }

    @Override
    public ShortBuffer readShortBuffer(String name, ShortBuffer defVal) throws IOException {
        Element element = findChildElement(name);

        short[] array = (short[]) readPrimitiveArrayHelper(element, "short");

        if (array == null) {
            return defVal;
        }

        int position = 0;
        String positionString = XMLUtils.getAttribute(importer.getFormatVersion(), element, XMLExporter.ATTRIBUTE_POSITION);
        if (!positionString.isEmpty()) {
            try {
                position = Integer.parseInt(positionString);
            } catch (NumberFormatException nfe) {
                throw new IOException(nfe);
            }
        }

        return (ShortBuffer) BufferUtils.createShortBuffer(array.length).put(array).position(position);
    }

    @Override
    public IntBuffer readIntBuffer(String name, IntBuffer defVal) throws IOException {
        Element element = findChildElement(name);

        int[] array = (int[]) readPrimitiveArrayHelper(element, "int");

        if (array == null) {
            return defVal;
        }

        int position = 0;
        String positionString = XMLUtils.getAttribute(importer.getFormatVersion(), element, XMLExporter.ATTRIBUTE_POSITION);
        if (!positionString.isEmpty()) {
            try {
                position = Integer.parseInt(positionString);
            } catch (NumberFormatException nfe) {
                throw new IOException(nfe);
            }
        }

        return (IntBuffer) BufferUtils.createIntBuffer(array.length).put(array).position(position);
    }

    @Override
    public FloatBuffer readFloatBuffer(String name, FloatBuffer defVal) throws IOException {
        Element element = findChildElement(name);

        float[] array = (float[]) readPrimitiveArrayHelper(element, "float");

        if (array == null) {
            return defVal;
        }

        int position = 0;
        String positionString = XMLUtils.getAttribute(importer.getFormatVersion(), element, XMLExporter.ATTRIBUTE_POSITION);
        if (!positionString.isEmpty()) {
            try {
                position = Integer.parseInt(positionString);
            } catch (NumberFormatException nfe) {
                throw new IOException(nfe);
            }
        }

        return (FloatBuffer) BufferUtils.createFloatBuffer(array.length).put(array).position(position);
    }

    @Override
    public ArrayList<ByteBuffer> readByteBufferArrayList(String name, ArrayList<ByteBuffer> defVal) throws IOException {
        byte[][] byteArray2D = readByteArray2D(name, null);

        if (byteArray2D == null) {
            return defVal;
        }

        ArrayList<ByteBuffer> byteBufferList = new ArrayList<>(byteArray2D.length);
        for (byte[] byteArray : byteArray2D) {
            if (byteArray == null) {
                byteBufferList.add(null);
            } else {
                byteBufferList.add((ByteBuffer) BufferUtils.createByteBuffer(byteArray.length).put(byteArray).rewind());
            }
        }

        return byteBufferList;
    }

    @Override
    public ArrayList<FloatBuffer> readFloatBufferArrayList(String name, ArrayList<FloatBuffer> defVal) throws IOException {
        float[][] floatArray2D = readFloatArray2D(name, null);

        if (floatArray2D == null) {
            return defVal;
        }

        ArrayList<FloatBuffer> floatBufferList = new ArrayList<>(floatArray2D.length);
        for (float[] floatArray : floatArray2D) {
            if (floatArray == null) {
                floatBufferList.add(null);
            } else {
                floatBufferList.add((FloatBuffer) BufferUtils.createFloatBuffer(floatArray.length).put(floatArray).rewind());
            }
        }

        return floatBufferList;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ArrayList<Savable> readSavableArrayList(String name, ArrayList defVal) throws IOException {
        Savable[] savableArray = readSavableArray(name, null);

        if (savableArray == null) {
            return defVal;
        }

        return new ArrayList(Arrays.asList(savableArray));
    }

    @Override
    @SuppressWarnings("unchecked")
    public ArrayList<Savable>[] readSavableArrayListArray(String name, ArrayList[] defVal) throws IOException {
        Savable[][] savableArray2D = readSavableArray2D(name, null);

        if (savableArray2D == null) {
            return defVal;
        }

        ArrayList<Savable>[] savableArrayListArray = new ArrayList[savableArray2D.length];
        for (int i = 0; i < savableArray2D.length; i++) {
            if (savableArray2D[i] != null) {
                savableArrayListArray[i] = new ArrayList(Arrays.asList(savableArray2D[i]));
            }
        }

        return savableArrayListArray;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ArrayList<Savable>[][] readSavableArrayListArray2D(String name, ArrayList[][] defVal) throws IOException {
        Element outerArrayElement = findChildElement(name);

        if (outerArrayElement == null) {
            return defVal;
        }

        List<Element> innerArrayElements = getObjectArrayElements(outerArrayElement);

        ArrayList<Savable>[][] savableArrayListArray2D = new ArrayList[innerArrayElements.size()][];

        Element old = currentElement;
        currentElement = outerArrayElement;

        for (int i = 0; i < innerArrayElements.size(); i++) {
            if (innerArrayElements.get(i) != null) {

                savableArrayListArray2D[i] = readSavableArrayListArray(innerArrayElements.get(i).getTagName(), null);
            }
        }

        currentElement = old;

        return savableArrayListArray2D;
    }

    @Override
    public Map<? extends Savable, ? extends Savable> readSavableMap(String name, Map<? extends Savable, ? extends Savable> defVal) throws IOException {
        Element mapElement = findChildElement(name);

        if (mapElement == null) {
            return defVal;
        }

        Map<Savable, Savable> ret = new HashMap<Savable, Savable>();

        NodeList nodes = mapElement.getChildNodes();
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

        currentElement = (Element) mapElement.getParentNode();

        return ret;
    }

    @Override
    public Map<String, ? extends Savable> readStringSavableMap(String name, Map<String, ? extends Savable> defVal) throws IOException {
        Element mapElement = findChildElement(name);

        if (mapElement == null) {
            return defVal;
        }

        Map<String, Savable> ret = new HashMap<String, Savable>();

        NodeList nodes = mapElement.getChildNodes();
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

        currentElement = (Element) mapElement.getParentNode();

        return ret;
    }

    @Override
    public IntMap<? extends Savable> readIntSavableMap(String name, IntMap<? extends Savable> defVal) throws IOException {
        Element mapElement = findChildElement(name);

        if (mapElement == null) {
            return defVal;
        }

        IntMap<Savable> ret = new IntMap<Savable>();

        NodeList nodes = mapElement.getChildNodes();
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

        currentElement = (Element) mapElement.getParentNode();

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

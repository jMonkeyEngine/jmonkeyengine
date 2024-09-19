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

import com.jme3.export.FormatVersion;
import com.jme3.export.JmeExporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.export.SavableClassUtil;
import com.jme3.util.IntMap;
import com.jme3.util.IntMap.Entry;
import java.lang.reflect.Array;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Part of the jME XML IO system as introduced in the Google Code jmexml project.
 *
 * @author Kai Rabien (hevee) - original author of the code.google.com jmexml project
 * @author Doug Daniels (dougnukem) - adjustments for jME 2.0 and Java 1.5
 */
public class DOMOutputCapsule implements OutputCapsule {
    private Document doc;
    private Element currentElement;
    private JmeExporter exporter;
    private Map<Savable, Element> writtenSavables = new IdentityHashMap<>();

    public DOMOutputCapsule(Document doc, JmeExporter exporter) {
        this.doc = doc;
        this.exporter = exporter;
        currentElement = null;
    }

    public Document getDoc() {
        return doc;
    }

    /**
     * appends a new Element with the given name to currentElement, sets
     * currentElement to be new Element, and returns the new Element as well
     */
    private Element appendElement(String name) {
        Element ret = doc.createElement(name);
        if (currentElement == null) {
            // file version is always unprefixed for backwards compatibility
            ret.setAttribute("format_version", Integer.toString(FormatVersion.VERSION));
            doc.appendChild(ret);
        } else {
            currentElement.appendChild(ret);
        }
        currentElement = ret;
        return ret;
    }

    // helper function to reduce duplicate code.  uses reflection to write an array of any primitive type.
    // also has optional position argument for writing buffers.
    private void writePrimitiveArrayHelper(Object value, String name) throws IOException {
        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < Array.getLength(value); i++) {
            sb.append(Array.get(value, i));
            sb.append(" ");
        }

        // remove last space
        sb.setLength(Math.max(0, sb.length() - 1));

        appendElement(name);
        XMLUtils.setAttribute(currentElement, XMLExporter.ATTRIBUTE_SIZE, String.valueOf(Array.getLength(value)));
        XMLUtils.setAttribute(currentElement, XMLExporter.ATTRIBUTE_DATA, sb.toString());

        currentElement = (Element) currentElement.getParentNode();
    }

    // helper function to reduce duplicate code.  uses the above helper to write a 2d array of any primitive type.
    private void writePrimitiveArray2DHelper(Object[] value, String name) throws IOException {
        appendElement(name);

        XMLUtils.setAttribute(currentElement, XMLExporter.ATTRIBUTE_SIZE, String.valueOf(value.length));

        // tag names are not used for anything by DOMInputCapsule, but for the sake of readability, it's nice to know what type of array this is.
        String childNamePrefix = value.getClass().getComponentType().getSimpleName().toLowerCase();
        childNamePrefix = childNamePrefix.replace("[]", "_array_");

        for (int i = 0; i < value.length; i++) {
            String childName = childNamePrefix + i;

            if (value[i] != null) {
                writePrimitiveArrayHelper(value[i], childName);
            } else {
                // empty tag
                appendElement(childName);
                currentElement = (Element) currentElement.getParentNode();
            }
        }

        currentElement = (Element) currentElement.getParentNode();
    }

    @Override
    public void write(byte value, String name, byte defVal) throws IOException {
        if (value != defVal) {
            XMLUtils.setAttribute(currentElement, name, String.valueOf(value));
        }
    }

    @Override
    public void write(byte[] value, String name, byte[] defVal) throws IOException {
        if (!Arrays.equals(value, defVal)) {
            writePrimitiveArrayHelper(value, name);
        }
    }

    @Override
    public void write(byte[][] value, String name, byte[][] defVal) throws IOException {
        if (!Arrays.deepEquals(value, defVal)) {
            writePrimitiveArray2DHelper(value, name);
        }
    }

    @Override
    public void write(short value, String name, short defVal) throws IOException {
        if (value != defVal) {
            XMLUtils.setAttribute(currentElement, name, String.valueOf(value));
        }
    }

    @Override
    public void write(short[] value, String name, short[] defVal) throws IOException {
        if (!Arrays.equals(value, defVal)) {
            writePrimitiveArrayHelper(value, name);
        }
    }

    @Override
    public void write(short[][] value, String name, short[][] defVal) throws IOException {
        if (!Arrays.deepEquals(value, defVal)) {
            writePrimitiveArray2DHelper(value, name);
        }
    }

    @Override
    public void write(int value, String name, int defVal) throws IOException {
        if (value != defVal) {
            XMLUtils.setAttribute(currentElement, name, String.valueOf(value));
        }
    }

    @Override
    public void write(int[] value, String name, int[] defVal) throws IOException {
        if (!Arrays.equals(value, defVal)) {
            writePrimitiveArrayHelper(value, name);
        }
    }

    @Override
    public void write(int[][] value, String name, int[][] defVal) throws IOException {
        if (!Arrays.deepEquals(value, defVal)) {
            writePrimitiveArray2DHelper(value, name);
        }
    }

    @Override
    public void write(long value, String name, long defVal) throws IOException {
        if (value != defVal) {
            XMLUtils.setAttribute(currentElement, name, String.valueOf(value));
        }
    }

    @Override
    public void write(long[] value, String name, long[] defVal) throws IOException {
        if (!Arrays.equals(value, defVal)) {
            writePrimitiveArrayHelper(value, name);
        }
    }

    @Override
    public void write(long[][] value, String name, long[][] defVal) throws IOException {
        if (!Arrays.deepEquals(value, defVal)) {
            writePrimitiveArray2DHelper(value, name);
        }
    }

    @Override
    public void write(float value, String name, float defVal) throws IOException {
        if (value != defVal) {
            XMLUtils.setAttribute(currentElement, name, String.valueOf(value));
        }
    }

    @Override
    public void write(float[] value, String name, float[] defVal) throws IOException {
        if (!Arrays.equals(value, defVal)) {
            writePrimitiveArrayHelper(value, name);
        }
    }

    @Override
    public void write(float[][] value, String name, float[][] defVal) throws IOException {
        if (!Arrays.deepEquals(value, defVal)) {
            writePrimitiveArray2DHelper(value, name);
        }
    }

    @Override
    public void write(double value, String name, double defVal) throws IOException {
        if (value != defVal) {
            XMLUtils.setAttribute(currentElement, name, String.valueOf(value));
        }
    }

    @Override
    public void write(double[] value, String name, double[] defVal) throws IOException {
        if (!Arrays.equals(value, defVal)) {
            writePrimitiveArrayHelper(value, name);
        }
    }

    @Override
    public void write(double[][] value, String name, double[][] defVal) throws IOException {
        if (!Arrays.deepEquals(value, defVal)) {
            writePrimitiveArray2DHelper(value, name);
        }
    }

    @Override
    public void write(boolean value, String name, boolean defVal) throws IOException {
        if (value != defVal) {
            XMLUtils.setAttribute(currentElement, name, String.valueOf(value));
        }
    }

    @Override
    public void write(boolean[] value, String name, boolean[] defVal) throws IOException {
        if (!Arrays.equals(value, defVal)) {
            writePrimitiveArrayHelper(value, name);
        }
    }

    @Override
    public void write(boolean[][] value, String name, boolean[][] defVal) throws IOException {
        if (!Arrays.deepEquals(value, defVal)) {
            writePrimitiveArray2DHelper(value, name);
        }
    }

    @Override
    public void write(String value, String name, String defVal) throws IOException {
        if (value != null && !value.equals(defVal)) {
            XMLUtils.setAttribute(currentElement, name, value);
        }
    }

    @Override
    public void write(String[] value, String name, String[] defVal) throws IOException {
        if (value == null || Arrays.equals(value, defVal)) {
            return;
        }

        appendElement(name);

        XMLUtils.setAttribute(currentElement, XMLExporter.ATTRIBUTE_SIZE, String.valueOf(value.length));

        for (int i = 0; i < value.length; i++) {
            appendElement("string_" + i);

            if (value[i] != null) {
                XMLUtils.setAttribute(currentElement, "value", value[i]);
            }
            
            currentElement = (Element) currentElement.getParentNode();
        }

        currentElement = (Element) currentElement.getParentNode();
    }

    @Override
    public void write(String[][] value, String name, String[][] defVal) throws IOException {
        if (value == null || Arrays.deepEquals(value, defVal)) {
            return;
        }

        appendElement(name);

        XMLUtils.setAttribute(currentElement, XMLExporter.ATTRIBUTE_SIZE, String.valueOf(value.length));

        for (int i = 0; i < value.length; i++) {
            String childName = "string_array_" + i;

            if (value[i] != null) {
                write(value[i], childName, defVal != null ? defVal[i] : null);
            } else {
                // empty tag
                appendElement(childName);
                currentElement = (Element) currentElement.getParentNode();
            }
        }

        currentElement = (Element) currentElement.getParentNode();
    }

    @Override
    public void write(Enum value, String name, Enum defVal) throws IOException {
        if (value != null && !value.equals(defVal)) {
            XMLUtils.setAttribute(currentElement, name, String.valueOf(value));
        }
    }

    @Override
    public void write(BitSet value, String name, BitSet defVal) throws IOException {
        if (value == null || value.equals(defVal)) {
            return;
        }

        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < value.size(); i++) {
            buf.append(value.get(i) ? "1 " : "0 ");
        }
        
        if (buf.length() > 0) {
            //remove last space
            buf.setLength(buf.length() - 1);
        }
        
        XMLUtils.setAttribute(currentElement, name, buf.toString());
    }

    @Override
    public void write(Savable value, String name, Savable defVal) throws IOException {
        if (value == null || value.equals(defVal)) {
            return;
        }

        Element old = currentElement;

        // no longer tries to use class name as element name.  that makes things unnecessarily complicated.

        Element refElement = writtenSavables.get(value);
        // this object has already been written, so make an element that refers to the existing one.
        if (refElement != null) {
            String refID = XMLUtils.getAttribute(FormatVersion.VERSION, refElement, XMLExporter.ATTRIBUTE_REFERENCE_ID);

            // add the reference_ID to the referenced element if it didn't already have it
            if (refID.isEmpty()) {
                refID = value.getClass().getName() + "@" + value.hashCode();
                XMLUtils.setAttribute(refElement, XMLExporter.ATTRIBUTE_REFERENCE_ID, refID);
            }

            appendElement(name);
            XMLUtils.setAttribute(currentElement, XMLExporter.ATTRIBUTE_REFERENCE, refID);
        } else {
            appendElement(name);

            // this now always writes the class attribute even if the class name is also the element name.
            // for backwards compatibility, DOMInputCapsule will still try to get the class name from the element name if the
            // attribute isn't found.
            XMLUtils.setAttribute(currentElement, XMLExporter.ATTRIBUTE_CLASS, value.getClass().getName());
            
            // jME3 NEW: Append version number(s)
            int[] versions = SavableClassUtil.getSavableVersions(value.getClass());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < versions.length; i++){
                sb.append(versions[i]);
                if (i != versions.length - 1){
                    sb.append(", ");
                }
            }
            XMLUtils.setAttribute(currentElement, XMLExporter.ATTRIBUTE_SAVABLE_VERSIONS, sb.toString());
            
            value.write(exporter);

            writtenSavables.put(value, currentElement);
        }

        currentElement = old;
    }

    @Override
    public void write(Savable[] value, String name, Savable[] defVal) throws IOException {
        if (value == null || Arrays.equals(value, defVal)) {
            return;
        }

        Element old = currentElement;
        
        appendElement(name);

        XMLUtils.setAttribute(currentElement, XMLExporter.ATTRIBUTE_SIZE, String.valueOf(value.length));
        for (int i = 0; i < value.length; i++) {
            Savable o = value[i];
            String elementName = "savable_" + i;
            if(o == null){
                // renderStateList has special loading code, so we can leave out the null values
                if(!name.equals("renderStateList")){
                    Element before = currentElement;
                    appendElement(elementName);
                    currentElement = before;
                }
            }else{
                write(o, elementName, null);
            }
        }

        currentElement = old;
    }

    @Override
    public void write(Savable[][] value, String name, Savable[][] defVal) throws IOException {
        if (value == null || Arrays.deepEquals(value, defVal)) {
            return;
        }

        Element old = currentElement;

        appendElement(name);

        XMLUtils.setAttribute(currentElement, XMLExporter.ATTRIBUTE_SIZE, String.valueOf(value.length));
        for (int i = 0; i < value.length; i++) {
            String childName = "savable_array_" + i;
            if (value[i] != null) {
                write(value[i], childName, null);
            } else {
                appendElement(childName);
                currentElement = (Element) currentElement.getParentNode();
            }
        }

        currentElement = old;
    }

    @Override
    public void write(ByteBuffer value, String name, ByteBuffer defVal) throws IOException {
        if (value == null || value.equals(defVal)) {
            return;
        }
        
        int position = value.position();
        value.rewind();
        byte[] array = new byte[value.remaining()];
        value.get(array);
        value.position(position);

        writePrimitiveArrayHelper(array, name);
    }

    @Override
    public void write(ShortBuffer value, String name, ShortBuffer defVal) throws IOException {
        if (value == null || value.equals(defVal)) {
            return;
        }

        int position = value.position();
        value.rewind();
        short[] array = new short[value.remaining()];
        value.get(array);
        value.position(position);

        writePrimitiveArrayHelper(array, name);
    }

    @Override
    public void write(IntBuffer value, String name, IntBuffer defVal) throws IOException {
        if (value == null || value.equals(defVal)) {
            return;
        }

        int position = value.position();
        value.rewind();
        int[] array = new int[value.remaining()];
        value.get(array);
        value.position(position);

        writePrimitiveArrayHelper(array, name);
    }

    @Override
    public void write(FloatBuffer value, String name, FloatBuffer defVal) throws IOException {
        if (value == null || value.equals(defVal)) {
            return;
        }

        int position = value.position();
        value.rewind();
        float[] array = new float[value.remaining()];
        value.get(array);
        value.position(position);

        writePrimitiveArrayHelper(array, name);
    }

    @Override
    public void writeByteBufferArrayList(ArrayList<ByteBuffer> value, String name, ArrayList<ByteBuffer> defVal) throws IOException {
        if (value == null || value.equals(defVal)) {
            return;
        }

        appendElement(name);

        XMLUtils.setAttribute(currentElement, XMLExporter.ATTRIBUTE_SIZE, String.valueOf(value.size()));
        for (int i = 0; i < value.size(); i++) {
            String childName = "byte_buffer_" + i;
            if (value.get(i) != null) {
                write(value.get(i), childName, null);
            } else {
                appendElement(childName);
                currentElement = (Element) currentElement.getParentNode();
            }
        }

        currentElement = (Element) currentElement.getParentNode();
    }

    @Override
    public void writeFloatBufferArrayList(ArrayList<FloatBuffer> value, String name, ArrayList<FloatBuffer> defVal) throws IOException {
        if (value == null || value.equals(defVal)) {
            return;
        }

        appendElement(name);

        XMLUtils.setAttribute(currentElement, XMLExporter.ATTRIBUTE_SIZE, String.valueOf(value.size()));
        for (int i = 0; i < value.size(); i++) {
            String childName = "float_buffer_" + i;
            if (value.get(i) != null) {
                write(value.get(i), childName, null);
            } else {
                appendElement(childName);
                currentElement = (Element) currentElement.getParentNode();
            }
        }

        currentElement = (Element) currentElement.getParentNode();
    }

    @Override
    public void writeSavableArrayList(ArrayList value, String name, ArrayList defVal) throws IOException {
        if (value == null || value.equals(defVal)) {
            return;
        }

        Savable[] savableArray = new Savable[value.size()];
        for (int i = 0; i < value.size(); i++) {
            Object o = value.get(i);

            if (o != null && !(o instanceof Savable)) {
                throw new IOException(new ClassCastException("Not a Savable instance: " + o));
            }

            savableArray[i] = (Savable) o;
        }

        write(savableArray, name, null);
    }

    @Override
    public void writeSavableArrayListArray(ArrayList[] value, String name, ArrayList[] defVal) throws IOException {
        if (value == null || Arrays.equals(value, defVal)) {
            return;
        }

        Element old = currentElement;

        appendElement(name);
        XMLUtils.setAttribute(currentElement, XMLExporter.ATTRIBUTE_SIZE, String.valueOf(value.length));
        for (int i = 0; i < value.length; i++) {
            String childName = "savable_list_" + i;
            if(value[i] != null){
                writeSavableArrayList(value[i], childName, null);
            }else{
                appendElement(childName);
                currentElement = (Element) currentElement.getParentNode();
            }
        }

        currentElement = old;
    }

    @Override
    public void writeSavableArrayListArray2D(ArrayList[][] value, String name, ArrayList[][] defVal) throws IOException {
        if (value == null || Arrays.equals(value, defVal)) {
            return;
        }

        Element old = currentElement;

        appendElement(name);

        XMLUtils.setAttribute(currentElement, XMLExporter.ATTRIBUTE_SIZE, String.valueOf(value.length));
        for (int i = 0; i < value.length; i++) {
            String childName = "savable_list_array_" + i;
            if(value[i] != null){
                writeSavableArrayListArray(value[i], childName, null);
            }else{
                appendElement(childName);
                currentElement = (Element) currentElement.getParentNode();
            }
        }

        currentElement = old;
    }

    @Override
    public void writeSavableMap(Map<? extends Savable, ? extends Savable> map, String name, Map<? extends Savable, ? extends Savable> defVal) throws IOException {
        if (map == null || map.equals(defVal)) {
            return;
        }

        Element stringMap = appendElement(name);

        Iterator<? extends Savable> keyIterator = map.keySet().iterator();
        while(keyIterator.hasNext()) {
            Savable key = keyIterator.next();
            Element mapEntry = appendElement(XMLExporter.ELEMENT_MAPENTRY);
            write(key, XMLExporter.ELEMENT_KEY, null);
            Savable value = map.get(key);
            write(value, XMLExporter.ELEMENT_VALUE, null);
            currentElement = stringMap;
        }

        currentElement = (Element) stringMap.getParentNode();
    }

    @Override
    public void writeStringSavableMap(Map<String, ? extends Savable> map, String name, Map<String, ? extends Savable> defVal) throws IOException {
        if (map == null || map.equals(defVal)) {
            return;
        }

        Element stringMap = appendElement(name);

        Iterator<String> keyIterator = map.keySet().iterator();
        while(keyIterator.hasNext()) {
            String key = keyIterator.next();
            Element mapEntry = appendElement("MapEntry");
            XMLUtils.setAttribute(mapEntry, "key", key);
            Savable s = map.get(key);
            write(s, "Savable", null);
            currentElement = stringMap;
        }

        currentElement = (Element) stringMap.getParentNode();
    }

    @Override
    public void writeIntSavableMap(IntMap<? extends Savable> map, String name, IntMap<? extends Savable> defVal) throws IOException {
        if (map == null || map.equals(defVal)) {
            return;
        }

        Element stringMap = appendElement(name);

        for(Entry<? extends Savable> entry : map) {
            int key = entry.getKey();
            Element mapEntry = appendElement("MapEntry");
            XMLUtils.setAttribute(mapEntry, "key", Integer.toString(key));
            Savable s = entry.getValue();
            write(s, "Savable", null);
            currentElement = stringMap;
        }

        currentElement = (Element) stringMap.getParentNode();
    }
}
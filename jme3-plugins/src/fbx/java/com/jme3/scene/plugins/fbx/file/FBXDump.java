/*
 * Copyright (c) 2009-2015 jMonkeyEngine
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
package com.jme3.scene.plugins.fbx.file;

import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import static org.omg.IOP.IORHelper.id;

/**
 * Quick n' dirty dumper of FBX binary files.
 * 
 * Outputs a format similar to an ASCII FBX file.
 * 
 * @author Kirill Vainer
 */
public final class FBXDump {
    
    private static final DecimalFormat DECIMAL_FORMAT 
            = new DecimalFormat("0.0000000000");
    
    private FBXDump() { }
    
    /**
     * Creates a map between object UIDs and the objects themselves.
     * 
     * @param file The file to create the mappings for.
     * @return The UID to object map.
     */
    private static Map<Long, FBXElement> createUidToObjectMap(FBXFile file) {
        Map<Long, FBXElement> uidToObjectMap = new HashMap<Long, FBXElement>();
        for (FBXElement rootElement : file.rootElements) {
            if (rootElement.id.equals("Objects")) {
                for (FBXElement fbxObj : rootElement.children) {
                    if (fbxObj.propertiesTypes[0] != 'L') {
                        continue; // error
                    }
                    Long uid = (Long) fbxObj.properties.get(0);
                    uidToObjectMap.put(uid, fbxObj);
                }
            }
        }
        return uidToObjectMap;
    }
    
    /**
     * Dump FBX to standard output.
     * 
     * @param file the file to dump.
     */
    public static void dumpFBX(FBXFile file) {
        dumpFBX(file, System.out);
    }
    
    /**
     * Dump FBX to the given output stream.
     * 
     * @param file the file to dump.
     * @param out the output stream where to output.
     */
    public static void dumpFBX(FBXFile file, OutputStream out) {
        Map<Long, FBXElement> uidToObjectMap = createUidToObjectMap(file);
        PrintStream ps = new PrintStream(out);
        for (FBXElement rootElement : file.rootElements) {
            dumpFBXElement(rootElement, ps, 0, uidToObjectMap);
        }
    }
    
    private static String indent(int amount) {
        return "                        ".substring(0, amount);
    }
    
    /**
     * Convert FBX string - this replaces all instances of
     * <code>\x00\x01</code> to "::".
     * 
     * @param string The string to convert
     * @return 
     */
    private static String convertFBXString(String string) {
        return string.replaceAll("\u0000\u0001", "::");
    }

    protected static void dumpFBXProperty(String id, char propertyType, 
                                          Object property, PrintStream ps, 
                                          Map<Long, FBXElement> uidToObjectMap) {
        switch (propertyType) {
            case 'S':
                // String
                String str = (String) property;
                ps.print("\"" + convertFBXString(str) + "\"");
                break;
            case 'R':
                // RAW data.
                byte[] bytes = (byte[]) property;
                ps.print("[");
                for (int j = 0; j < bytes.length; j++) {
                    ps.print(String.format("%02X", bytes[j] & 0xff));
                    if (j != bytes.length - 1) {
                        ps.print(" ");
                    }
                }
                ps.print("]");
                break;
            case 'D':
            case 'F':
                // Double, Float.
                if (property instanceof Double) {
                    ps.print(DECIMAL_FORMAT.format((Double)property));
                } else if (property instanceof Float) {
                    ps.print(DECIMAL_FORMAT.format((Float)property));
                } else {
                    ps.print(property);
                }
                break;
            case 'I':
            case 'Y':
                // Integer, Signed Short.
                ps.print(property);
                break;
            case 'C':
                // Boolean
                ps.print((Boolean)property ? "1" : "0");
                break;
            case 'L':
                // Long
                // If this is a connection, decode UID into object name.
                if (id.equals("C")) {
                    Long uid = (Long) property;
                    FBXElement element = uidToObjectMap.get(uid);
                    if (element != null) {
                        String name = (String) element.properties.get(1);
                        ps.print("\"" + convertFBXString(name) + "\"");
                    } else {
                        ps.print(property);
                    }
                } else {
                    ps.print(property);
                }
                break;
            case 'd':
            case 'i':
            case 'l':
            case 'f':
                // Arrays of things..
                int length = Array.getLength(property);
                for (int j = 0; j < length; j++) {
                    Object arrayEntry = Array.get(property, j);
                    dumpFBXProperty(id, Character.toUpperCase(propertyType), arrayEntry, ps, uidToObjectMap);
                    if (j != length - 1) {
                        ps.print(",");
                    }
                }
                break;
            default: 
                throw new UnsupportedOperationException("" + propertyType);
        }
    }
    
    protected static void dumpFBXElement(FBXElement el, PrintStream ps, 
                                         int indent, Map<Long, FBXElement> uidToObjectMap) {
        // 4 spaces per tab should be OK.
        String indentStr = indent(indent * 4);
        String textId = el.id;
        
        // Properties are called 'P' and connections are called 'C'.
        if (el.id.equals("P")) {
            textId = "Property";
        } else if (el.id.equals("C")) {
            textId = "Connect";
        }
        
        ps.print(indentStr + textId + ": ");
        for (int i = 0; i < el.properties.size(); i++) {
            Object property = el.properties.get(i);
            char propertyType = el.propertiesTypes[i];
            dumpFBXProperty(el.id, propertyType, property, ps, uidToObjectMap);
            if (i != el.properties.size() - 1) {
                ps.print(", ");
            }
        }
        if (el.children.isEmpty()) {
            ps.println();
        } else {
            ps.println(" {");
            for (FBXElement childElement : el.children) {
                dumpFBXElement(childElement, ps, indent + 1, uidToObjectMap);
            }
            ps.println(indentStr + "}");
        }
    }
}

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
package com.jme3.scene.plugins.fbx.file;

import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Quick n' dirty dumper of FBX binary files.
 * 
 * Outputs a format similar to an ASCII FBX file.
 * 
 * @author Kirill Vainer
 */
public final class FbxDump {
    
    private static final Logger logger = Logger.getLogger(FbxDump.class.getName());
    
    private static final DecimalFormat DECIMAL_FORMAT 
            = new DecimalFormat("0.0000000000");
    
    private FbxDump() { }
    
    /**
     * Creates a map between object UIDs and the objects themselves.
     * 
     * @param file The file to create the mappings for.
     * @return The UID to object map.
     */
    private static Map<FbxId, FbxElement> createUidToObjectMap(FbxFile file) {
        Map<FbxId, FbxElement> uidToObjectMap = new HashMap<>();
        for (FbxElement rootElement : file.rootElements) {
            if (rootElement.id.equals("Objects")) {
                for (FbxElement fbxObj : rootElement.children) {
                    FbxId uid = FbxId.getObjectId(fbxObj);
                    if (uid != null) {
                        uidToObjectMap.put(uid, fbxObj);
                    } else {
                        logger.log(Level.WARNING, "Cannot determine ID for object: {0}", fbxObj);
                    }
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
    public static void dumpFile(FbxFile file) {
        dumpFile(file, System.out);
    }
    
    /**
     * Dump FBX to the given output stream.
     * 
     * @param file the file to dump.
     * @param out the output stream where to output.
     */
    public static void dumpFile(FbxFile file, OutputStream out) {
        Map<FbxId, FbxElement> uidToObjectMap = createUidToObjectMap(file);
        PrintStream ps = new PrintStream(out);
        for (FbxElement rootElement : file.rootElements) {
            dumpElement(rootElement, ps, 0, uidToObjectMap);
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

    protected static void dumpProperty(String id, char propertyType, 
                                          Object property, PrintStream ps, 
                                          Map<FbxId, FbxElement> uidToObjectMap) {
        switch (propertyType) {
            case 'S':
                // String
                String str = (String) property;
                ps.print("\"" + convertFBXString(str) + "\"");
                break;
            case 'R':
                // RAW data.
                byte[] bytes = (byte[]) property;
                int numToPrint = Math.min(10 * 1024, bytes.length);
                ps.print("(size = ");
                ps.print(bytes.length);
                ps.print(") [");
                for (int j = 0; j < numToPrint; j++) {
                    ps.print(String.format("%02X", bytes[j] & 0xff));
                    if (j != bytes.length - 1) {
                        ps.print(" ");
                    }
                }
                if (numToPrint < bytes.length) {
                    ps.print(" ...");
                }
                ps.print("]");
                break;
            case 'D':
            case 'F':
                // Double, Float.
                if (property instanceof Double) {
                    ps.print(DECIMAL_FORMAT.format(property));
                } else if (property instanceof Float) {
                    ps.print(DECIMAL_FORMAT.format(property));
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
                    FbxElement element = uidToObjectMap.get(FbxId.create(uid));
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
                // Arrays of things.
                int length = Array.getLength(property);
                for (int j = 0; j < length; j++) {
                    Object arrayEntry = Array.get(property, j);
                    dumpProperty(id, Character.toUpperCase(propertyType), arrayEntry, ps, uidToObjectMap);
                    if (j != length - 1) {
                        ps.print(",");
                    }
                }
                break;
            default: 
                throw new UnsupportedOperationException("" + propertyType);
        }
    }
    
    protected static void dumpElement(FbxElement el, PrintStream ps, 
                                         int indent, Map<FbxId, FbxElement> uidToObjectMap) {
        // 4 spaces per tab should be OK.
        String indentStr = indent(indent * 4);
        String textId = el.id;
        
        // Properties are called 'P' and connections are called 'C'.
//        if (el.id.equals("P")) {
//            textId = "Property";
//        } else if (el.id.equals("C")) {
//            textId = "Connect";
//        }
        
        ps.print(indentStr + textId + ": ");
        for (int i = 0; i < el.properties.size(); i++) {
            Object property = el.properties.get(i);
            char propertyType = el.propertiesTypes[i];
            dumpProperty(el.id, propertyType, property, ps, uidToObjectMap);
            if (i != el.properties.size() - 1) {
                ps.print(", ");
            }
        }
        if (el.children.isEmpty()) {
            ps.println();
        } else {
            ps.println(" {");
            for (FbxElement childElement : el.children) {
                dumpElement(childElement, ps, indent + 1, uidToObjectMap);
            }
            ps.println(indentStr + "}");
        }
    }
}

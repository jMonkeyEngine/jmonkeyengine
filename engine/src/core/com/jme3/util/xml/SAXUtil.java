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

package com.jme3.util.xml;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * Utility methods for parsing XML data using SAX.
 */
public final class SAXUtil {

    /**
     * Parses an integer from a string, if the string is null returns
     * def.
     * 
     * @param i
     * @param def
     * @return
     * @throws SAXException 
     */
    public static int parseInt(String i, int def) throws SAXException{
        if (i == null)
            return def;
        else{
            try {
                return Integer.parseInt(i);
            } catch (NumberFormatException ex){
                throw new SAXException("Expected an integer, got '"+i+"'");
            }
        }
    }

    public static int parseInt(String i) throws SAXException{
        if (i == null)
            throw new SAXException("Expected an integer");
        else{
            try {
                return Integer.parseInt(i);
            } catch (NumberFormatException ex){
                throw new SAXException("Expected an integer, got '"+i+"'");
            }
        }
    }

    public static float parseFloat(String f, float def) throws SAXException{
        if (f == null)
            return def;
        else{
            try {
                return Float.parseFloat(f);
            } catch (NumberFormatException ex){
                throw new SAXException("Expected a decimal, got '"+f+"'");
            }
        }
    }

    public static float parseFloat(String f) throws SAXException{
        if (f == null)
            throw new SAXException("Expected a decimal");
        else{
            try {
                return Float.parseFloat(f);
            } catch (NumberFormatException ex){
                throw new SAXException("Expected a decimal, got '"+f+"'");
            }
        }
    }

    public static boolean parseBool(String bool, boolean def) throws SAXException{
        if (bool == null || bool.equals(""))
            return def;
        else
            return Boolean.valueOf(bool); 
        //else
        //else
        //    throw new SAXException("Expected a boolean, got'"+bool+"'");
    }

    public static String parseString(String str, String def){
        if (str == null)
            return def;
        else
            return str;
    }

    public static String parseString(String str) throws SAXException{
        if (str == null)
            throw new SAXException("Expected a string");
        else
            return str;
    }

    public static Vector3f parseVector3(Attributes attribs) throws SAXException{
        float x = parseFloat(attribs.getValue("x"));
        float y = parseFloat(attribs.getValue("y"));
        float z = parseFloat(attribs.getValue("z"));
        return new Vector3f(x,y,z);
    }

    public static ColorRGBA parseColor(Attributes attribs) throws SAXException{
        float r = parseFloat(attribs.getValue("r"));
        float g = parseFloat(attribs.getValue("g"));
        float b = parseFloat(attribs.getValue("b"));
        return new ColorRGBA(r, g, b, 1f);
    }

}

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

import java.util.ArrayList;
import java.util.List;

public class FbxElement {

    public String id;
    public List<Object> properties;
    /*
     * Y - signed short
     * C - boolean
     * I - signed integer
     * F - float
     * D - double
     * L - long
     * R - byte array
     * S - string
     * f - array of floats
     * i - array of ints
     * d - array of doubles
     * l - array of longs
     * b - array of booleans
     * c - array of unsigned bytes (represented as array of ints)
     */
    public char[] propertiesTypes;
    public List<FbxElement> children = new ArrayList<>();

    public FbxElement(int propsCount) {
        this.properties = new ArrayList<Object>(propsCount);
        this.propertiesTypes = new char[propsCount];
    }
        
        public FbxElement getChildById(String name) {
            for (FbxElement element : children) {
                if (element.id.equals(name)) {
                    return element;
                }
            }
            return null;
        }
        
        public List<FbxElement> getFbxProperties() {
            List<FbxElement> props = new ArrayList<>();
            FbxElement propsElement = null;
            boolean legacy = false;
            
            for (FbxElement element : children) {
                if (element.id.equals("Properties70")) {
                    propsElement = element;
                    break;
                } else if (element.id.equals("Properties60")) {
                    legacy = true;
                    propsElement = element;
                    break;
                }
            }
            
            if (propsElement == null) { 
                return props;
            }
            
            for (FbxElement prop : propsElement.children) {
                if (prop.id.equals("P") || prop.id.equals("Property")) {
                    if (legacy) {
                        char[] types = new char[prop.propertiesTypes.length + 1];
                        types[0] = prop.propertiesTypes[0];
                        types[1] = prop.propertiesTypes[0];
                        System.arraycopy(prop.propertiesTypes, 1, types, 2, types.length - 2);
                        
                        List<Object> values = new ArrayList<>(prop.properties);
                        values.add(1, values.get(0));
                        
                        FbxElement dummyProp = new FbxElement(types.length);
                        dummyProp.children = prop.children;
                        dummyProp.id = prop.id;
                        dummyProp.propertiesTypes = types;
                        dummyProp.properties = values;
                        props.add(dummyProp);
                    } else {
                        props.add(prop);
                    }
                }
            }
            
            return props;
        }
        
        @Override
        public String toString() {
            return "FBXElement[id=" + id + ", numProps=" + properties.size() + ", numChildren=" + children.size() + "]";
        }
}

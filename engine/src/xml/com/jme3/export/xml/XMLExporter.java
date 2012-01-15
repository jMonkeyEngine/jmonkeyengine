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

import com.jme3.export.JmeExporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Part of the jME XML IO system as introduced in the google code jmexml project.
 * 
 * @author Kai Rabien (hevee) - original author of the code.google.com jmexml project
 * @author Doug Daniels (dougnukem) - adjustments for jME 2.0 and Java 1.5
 */
public class XMLExporter implements JmeExporter {
    
    public static final String ELEMENT_MAPENTRY = "MapEntry";	
    public static final String ELEMENT_KEY = "Key";	
    public static final String ELEMENT_VALUE = "Value";
    public static final String ELEMENT_FLOATBUFFER = "FloatBuffer";
    public static final String ATTRIBUTE_SIZE = "size";		

    private DOMOutputCapsule domOut;
    
    public XMLExporter() {
       
    }

    public boolean save(Savable object, OutputStream f) throws IOException {
        try {
            //Initialize Document when saving so we don't retain state of previous exports
            this.domOut = new DOMOutputCapsule(DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument(), this);
            domOut.write(object, object.getClass().getName(), null);
            DOMSerializer serializer = new DOMSerializer();
            serializer.serialize(domOut.getDoc(), f);
            f.flush();
            return true;
        } catch (Exception ex) {
            IOException e = new IOException();
            e.initCause(ex);
            throw e;
        }
    }

    public boolean save(Savable object, File f) throws IOException {
        return save(object, new FileOutputStream(f));
    }

    public OutputCapsule getCapsule(Savable object) {
        return domOut;
    }

    public static XMLExporter getInstance() {
            return new XMLExporter();
    }
    
}

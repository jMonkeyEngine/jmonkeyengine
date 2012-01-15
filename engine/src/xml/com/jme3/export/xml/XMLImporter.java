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

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetManager;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeImporter;
import com.jme3.export.Savable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
 * Part of the jME XML IO system as introduced in the google code jmexml project.
 * @author Kai Rabien (hevee) - original author of the code.google.com jmexml project
 * @author Doug Daniels (dougnukem) - adjustments for jME 2.0 and Java 1.5
 */
public class XMLImporter implements JmeImporter {

    private AssetManager assetManager;
    private DOMInputCapsule domIn;
    int formatVersion = 0;
    
    public XMLImporter() {
    }

    public int getFormatVersion() {
        return formatVersion;
    }
    
    public AssetManager getAssetManager(){
        return assetManager;
    }

    public void setAssetManager(AssetManager assetManager){
        this.assetManager = assetManager;
    }

    public Object load(AssetInfo info) throws IOException{
        assetManager = info.getManager();
        InputStream in = info.openStream();
        Savable obj = load(in);
        in.close();
        return obj;
    }
    
    public Savable load(File f) throws IOException {
        FileInputStream fis = null; 
        try {
            fis = new FileInputStream(f);
            Savable sav = load(fis);
            return sav;
        } finally {
            if (fis != null) fis.close();
        }
    }

    public Savable load(InputStream f) throws IOException {
        try {
            domIn = new DOMInputCapsule(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f), this);
            return domIn.readSavable(null, null);
        } catch (SAXException e) {
            IOException ex = new IOException();
            ex.initCause(e);
            throw ex;
        } catch (ParserConfigurationException e) {
            IOException ex = new IOException();
            ex.initCause(e);
            throw ex;
        }
    }

    public InputCapsule getCapsule(Savable id) {
        return domIn;
    }

    public static XMLImporter getInstance() {
        return new XMLImporter();
    }

}

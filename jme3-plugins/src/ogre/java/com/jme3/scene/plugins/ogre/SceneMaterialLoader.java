/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
package com.jme3.scene.plugins.ogre;

import com.jme3.asset.AssetManager;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.material.MaterialList;
import com.jme3.scene.plugins.ogre.matext.OgreMaterialKey;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This is a utility class to load a {@link MaterialList} from a 
 * .scene file. It is only needed because the parsing method
 * used by the SceneLoader doesn't support reading bottom XML nodes
 * before reading the top nodes.
 * 
 * @author Kirill Vainer
 */
class SceneMaterialLoader extends DefaultHandler {
    
    private static final Logger logger = Logger.getLogger(SceneMaterialLoader.class.getName());
    private Stack<String> elementStack = new Stack<String>();
    private String folderName;
    private MaterialList materialList;
    private AssetManager assetManager;
    private boolean ignoreItem = false;
    
    private void reset(){
        elementStack.clear();
        materialList = null;
        ignoreItem = false;
    }
    
    private void checkTopNode(String topNode) throws SAXException{
        if (!elementStack.peek().equals(topNode)){
            throw new SAXException("dotScene parse error: Expected parent node to be " + topNode);
        }
    }
    
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attribs) throws SAXException {
        if (qName.equals("externals")) {
            checkTopNode("scene");
            
            // Has an externals block, create material list.
            materialList = new MaterialList();
        } else if (qName.equals("item")) {
            checkTopNode("externals");
            if (!attribs.getValue("type").equals("material")) {
                // This is not a material external. Ignore it.
                ignoreItem = true;
            }
        } else if (qName.equals("file")) {
            checkTopNode("item");

            if (!ignoreItem) {
                String materialPath = attribs.getValue("name");
                String materialName = new File(materialPath).getName();
                String matFile = folderName + materialName;
                try {
                    MaterialList loadedMaterialList = (MaterialList) assetManager.loadAsset(new OgreMaterialKey(matFile));
                    materialList.putAll(loadedMaterialList);
                } catch (AssetNotFoundException ex) {
                    logger.log(Level.WARNING, "Cannot locate material file: {0}", matFile);
                }
            }
        }
        elementStack.push(qName);
    }
    
    @Override
    public void endElement(String uri, String name, String qName) throws SAXException {
        if (qName.equals("item") && ignoreItem) {
            ignoreItem = false;
        }
        checkTopNode(qName);
        elementStack.pop();
    }
    
    public MaterialList load(AssetManager assetManager, String folderName, InputStream in) throws IOException {
        try {
            this.assetManager = assetManager;
            this.folderName = folderName;
            
            reset();
            
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);  
            XMLReader xr = factory.newSAXParser().getXMLReader();  

            xr.setContentHandler(this);
            xr.setErrorHandler(this);

            InputStreamReader r = null;

            try {
                r = new InputStreamReader(in);
                xr.parse(new InputSource(r));
            } finally {
                if (r != null){
                    r.close();
                }
            }
            
            return materialList;
        } catch (SAXException ex) {
            IOException ioEx = new IOException("Error while parsing Ogre3D dotScene");
            ioEx.initCause(ex);
            throw ioEx;
        } catch (ParserConfigurationException ex) {
            IOException ioEx = new IOException("Error while parsing Ogre3D dotScene");
            ioEx.initCause(ex);
            throw ioEx;
        }
    }
}

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

package com.jme3.export.binary;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetManager;
import com.jme3.export.*;
import com.jme3.math.FastMath;
import java.io.*;
import java.net.URL;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Joshua Slack
 * @author Kirill Vainer - Version number, Fast buffer reading
 */
public final class BinaryImporter implements JmeImporter {
    private static final Logger logger = Logger.getLogger(BinaryImporter.class
            .getName());

    private AssetManager assetManager;

    //Key - alias, object - bco
    private HashMap<String, BinaryClassObject> classes
             = new HashMap<String, BinaryClassObject>();
    //Key - id, object - the savable
    private HashMap<Integer, Savable> contentTable
            = new HashMap<Integer, Savable>();
    //Key - savable, object - capsule
    private IdentityHashMap<Savable, BinaryInputCapsule> capsuleTable
             = new IdentityHashMap<Savable, BinaryInputCapsule>();
    //Key - id, opject - location in the file
    private HashMap<Integer, Integer> locationTable
             = new HashMap<Integer, Integer>();

    public static boolean debug = false;

    private byte[] dataArray;
    private int aliasWidth;
    private int formatVersion;

    private static final boolean fastRead = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
    
    public BinaryImporter() {
    }
    
    public int getFormatVersion(){
        return formatVersion;
    }
    
    public static boolean canUseFastBuffers(){
        return fastRead;
    }

    public static BinaryImporter getInstance() {
        return new BinaryImporter();
    }

    public void setAssetManager(AssetManager manager){
        this.assetManager = manager;
    }

    public AssetManager getAssetManager(){
        return assetManager;
    }

    public Object load(AssetInfo info){
//        if (!(info.getKey() instanceof ModelKey))
//            throw new IllegalArgumentException("Model assets must be loaded using a ModelKey");

        assetManager = info.getManager();

        InputStream is = null;
        try {
            is = info.openStream();
            Savable s = load(is);
            
            return s;
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "An error occured while loading jME binary object", ex);
        } finally {
            if (is != null){
                try {
                    is.close();
                } catch (IOException ex) {}
            }
        }
        return null;
    }

    public Savable load(InputStream is) throws IOException {
        return load(is, null, null);
    }

    public Savable load(InputStream is, ReadListener listener) throws IOException {
        return load(is, listener, null);
    }

    public Savable load(InputStream is, ReadListener listener, ByteArrayOutputStream baos) throws IOException {
        contentTable.clear();
        BufferedInputStream bis = new BufferedInputStream(is);
        
        int numClasses;
        
        // Try to read signature
        int maybeSignature = ByteUtils.readInt(bis);
        if (maybeSignature == FormatVersion.SIGNATURE){
            // this is a new version J3O file
            formatVersion = ByteUtils.readInt(bis);
            numClasses = ByteUtils.readInt(bis);
            
            // check if this binary is from the future
            if (formatVersion > FormatVersion.VERSION){
                throw new IOException("The binary file is of newer version than expected! " + 
                                      formatVersion + " > " + FormatVersion.VERSION);
            }
        }else{
            // this is an old version J3O file
            // the signature was actually the class count
            numClasses = maybeSignature;
            
            // 0 indicates version before we started adding
            // version numbers
            formatVersion = 0; 
        }
        
        int bytes = 4;
        aliasWidth = ((int)FastMath.log(numClasses, 256) + 1);

        classes.clear();
        for(int i = 0; i < numClasses; i++) {
            String alias = readString(bis, aliasWidth);
            
            // jME3 NEW: Read class version number
            int[] classHierarchyVersions;
            if (formatVersion >= 1){
                int classHierarchySize = bis.read();
                classHierarchyVersions = new int[classHierarchySize];
                for (int j = 0; j < classHierarchySize; j++){
                    classHierarchyVersions[j] = ByteUtils.readInt(bis);
                }
            }else{
                classHierarchyVersions = new int[]{ 0 };
            }
            
            // read classname and classname size
            int classLength = ByteUtils.readInt(bis);
            String className = readString(bis, classLength);
            
            BinaryClassObject bco = new BinaryClassObject();
            bco.alias = alias.getBytes();
            bco.className = className;
            bco.classHierarchyVersions = classHierarchyVersions;
            
            int fields = ByteUtils.readInt(bis);
            bytes += (8 + aliasWidth + classLength);

            bco.nameFields = new HashMap<String, BinaryClassField>(fields);
            bco.aliasFields = new HashMap<Byte, BinaryClassField>(fields);
            for (int x = 0; x < fields; x++) {
                byte fieldAlias = (byte)bis.read();
                byte fieldType = (byte)bis.read();

                int fieldNameLength = ByteUtils.readInt(bis);
                String fieldName = readString(bis, fieldNameLength);
                BinaryClassField bcf = new BinaryClassField(fieldName, fieldAlias, fieldType);
                bco.nameFields.put(fieldName, bcf);
                bco.aliasFields.put(fieldAlias, bcf);
                bytes += (6 + fieldNameLength);
            }
            classes.put(alias, bco);
        }
        if (listener != null) listener.readBytes(bytes);

        int numLocs = ByteUtils.readInt(bis);
        bytes = 4;

        capsuleTable.clear();
        locationTable.clear();
        for(int i = 0; i < numLocs; i++) {
            int id = ByteUtils.readInt(bis);
            int loc = ByteUtils.readInt(bis);
            locationTable.put(id, loc);
            bytes += 8;
        }

        @SuppressWarnings("unused")
        int numbIDs = ByteUtils.readInt(bis); // XXX: NOT CURRENTLY USED
        int id = ByteUtils.readInt(bis);
        bytes += 8;
        if (listener != null) listener.readBytes(bytes);

        if (baos == null) {
                baos = new ByteArrayOutputStream(bytes);
        } else {
                baos.reset();
        }
        int size = -1;
        byte[] cache = new byte[4096];
        while((size = bis.read(cache)) != -1) {
            baos.write(cache, 0, size);
            if (listener != null) listener.readBytes(size);
        }
        bis = null;

        dataArray = baos.toByteArray();
        baos = null;

        Savable rVal = readObject(id);
        if (debug) {
            logger.info("Importer Stats: ");
            logger.log(Level.INFO, "Tags: {0}", numClasses);
            logger.log(Level.INFO, "Objects: {0}", numLocs);
            logger.log(Level.INFO, "Data Size: {0}", dataArray.length);
        }
        dataArray = null;
        return rVal;
    }

    public Savable load(URL f) throws IOException {
        return load(f, null);
    }

    public Savable load(URL f, ReadListener listener) throws IOException {
        InputStream is = f.openStream();
        Savable rVal = load(is, listener);
        is.close();
        return rVal;
    }

    public Savable load(File f) throws IOException {
        return load(f, null);
    }

    public Savable load(File f, ReadListener listener) throws IOException {
        FileInputStream fis = new FileInputStream(f);
        Savable rVal = load(fis, listener);
        fis.close();
        return rVal;
    }

    public Savable load(byte[] data) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        Savable rVal = load(bais);
        bais.close();
        return rVal;
    }

    @Override
    public InputCapsule getCapsule(Savable id) {
        return capsuleTable.get(id);
    }

    protected String readString(InputStream f, int length) throws IOException {
        byte[] data = new byte[length];
        for(int j = 0; j < length; j++) {
            data[j] = (byte)f.read();
        }

        return new String(data);
    }

    protected String readString(int length, int offset) throws IOException {
        byte[] data = new byte[length];
        for(int j = 0; j < length; j++) {
            data[j] = dataArray[j+offset];
        }

        return new String(data);
    }

    public Savable readObject(int id) {

        if(contentTable.get(id) != null) {
            return contentTable.get(id);
        }

        try {
            int loc = locationTable.get(id);

            String alias = readString(aliasWidth, loc);
            loc+=aliasWidth;

            BinaryClassObject bco = classes.get(alias);

            if(bco == null) {
                logger.logp(Level.SEVERE, this.getClass().toString(), "readObject(int id)", "NULL class object: " + alias);
                return null;
            }

            int dataLength = ByteUtils.convertIntFromBytes(dataArray, loc);
            loc+=4;

            Savable out = null;
            if (assetManager != null) {
                out = SavableClassUtil.fromName(bco.className, assetManager.getClassLoaders());
            } else {
                out = SavableClassUtil.fromName(bco.className);
            }

            BinaryInputCapsule cap = new BinaryInputCapsule(this, out, bco);
            cap.setContent(dataArray, loc, loc+dataLength);

            capsuleTable.put(out, cap);
            contentTable.put(id, out);

            out.read(this);

            capsuleTable.remove(out);

            return out;

        } catch (IOException e) {
            logger.logp(Level.SEVERE, this.getClass().toString(), "readObject(int id)", "Exception", e);
            return null;
        } catch (ClassNotFoundException e) {
            logger.logp(Level.SEVERE, this.getClass().toString(), "readObject(int id)", "Exception", e);
            return null;
        } catch (InstantiationException e) {
            logger.logp(Level.SEVERE, this.getClass().toString(), "readObject(int id)", "Exception", e);
            return null;
        } catch (IllegalAccessException e) {
            logger.logp(Level.SEVERE, this.getClass().toString(), "readObject(int id)", "Exception", e);
            return null;
        }
    }
}
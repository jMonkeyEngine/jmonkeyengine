/*
 * Copyright (c) 2009-2025 jMonkeyEngine
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
import com.jme3.export.FormatVersion;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeImporter;
import com.jme3.export.ReadListener;
import com.jme3.export.Savable;
import com.jme3.export.SavableClassUtil;
import com.jme3.math.FastMath;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The `BinaryImporter` class is responsible for loading jME binary object
 * files (`.j3o` files) into Java objects.
 *
 * @author Joshua Slack
 * @author Kirill Vainer
 */
public final class BinaryImporter implements JmeImporter {

    private static final Logger logger = Logger.getLogger(BinaryImporter.class.getName());

    private AssetManager assetManager;

    // Key - alias, object - bco
    private final HashMap<String, BinaryClassObject> classes = new HashMap<>();
    // Key - id, object - the savable
    private final HashMap<Integer, Savable> contentTable = new HashMap<>();
    // Key - savable, object - capsule
    private final IdentityHashMap<Savable, BinaryInputCapsule> capsuleTable = new IdentityHashMap<>();
    // Key - id, object - location in the file
    private final HashMap<Integer, Integer> locationTable = new HashMap<>();

    /**
     * A flag to enable debug logging for importer statistics.
     */
    public static boolean debug = false;

    private byte[] dataArray;
    private int aliasWidth;
    private int formatVersion;

    private static final boolean fastRead = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;

    /**
     * Creates a new {@code BinaryImporter}.
     */
    public BinaryImporter() {
    }
    
    @Override
    public int getFormatVersion(){
        return formatVersion;
    }

    /**
     * Checks if fast buffer reading can be used. Fast buffer reading is possible
     * if the native byte order is little-endian.
     * @return true if fast buffer reading can be used, false otherwise.
     */
    public static boolean canUseFastBuffers(){
        return fastRead;
    }

    /**
     * Returns a new instance of {@code BinaryImporter}.
     *
     * @return A new {@code BinaryImporter} instance.
     */
    public static BinaryImporter getInstance() {
        return new BinaryImporter();
    }

    /**
     * Sets the `AssetManager` to be used by this importer.
     * @param assetManager The `AssetManager` to set.
     */
    public void setAssetManager(AssetManager assetManager){
        this.assetManager = assetManager;
    }

    /**
     * Returns the `AssetManager` currently associated with this importer.
     * @return The `AssetManager` used by this importer.
     */
    @Override
    public AssetManager getAssetManager() {
        return assetManager;
    }

    /**
     * Loads a `Savable` object from the provided `AssetInfo`.
     * This method is typically called by the `AssetManager` to load assets.
     *
     * @param info The `AssetInfo` containing details about the asset to load,
     * including an `InputStream` to its data.
     * @return The loaded `Savable` object, or `null` if an error occurred during loading.
     */
    @Override
    public Object load(AssetInfo info) {
        assetManager = info.getManager();

        try (InputStream is = info.openStream()) {
            return load(is);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "An error occurred while loading jME binary object", ex);
        }
        return null;
    }

    /**
     * Loads a `Savable` object from the given `InputStream`.
     *
     * @param is The `InputStream` to read the binary data from.
     * @return The loaded `Savable` object.
     * @throws IOException If an I/O error occurs during reading.
     */
    public Savable load(InputStream is) throws IOException {
        return load(is, null, null);
    }

    /**
     * Loads a `Savable` object from the given `InputStream`, reporting read progress.
     *
     * @param is       The `InputStream` to read the binary data from.
     * @param listener An optional `ReadListener` to report read progress. Can be `null`.
     * @return The loaded `Savable` object.
     * @throws IOException If an I/O error occurs during reading.
     */
    public Savable load(InputStream is, ReadListener listener) throws IOException {
        return load(is, listener, null);
    }

    /**
     * Loads a `Savable` object from the given `InputStream`, reporting read progress
     * and optionally writing the read data to a `ByteArrayOutputStream`.
     *
     * @param is       The `InputStream` to read the binary data from.
     * @param listener An optional `ReadListener` to report read progress. Can be `null`.
     * @param baos     An optional `ByteArrayOutputStream` to which the read data will
     *                 be copied. If `null`, the data is not copied.
     * @return The loaded `Savable` object.
     * @throws IOException If an I/O error occurs during reading or if the file
     *                     format version is newer than supported by this importer.
     */
    public Savable load(InputStream is, ReadListener listener, ByteArrayOutputStream baos) throws IOException {
        contentTable.clear();
        BufferedInputStream bis = new BufferedInputStream(is);

        int numClasses;

        // Try to read signature
        int maybeSignature = ByteUtils.readInt(bis);
        if (maybeSignature == FormatVersion.SIGNATURE) {
            // this is a new version J3O file
            formatVersion = ByteUtils.readInt(bis);
            numClasses = ByteUtils.readInt(bis);

            // check if this binary is from the future
            if (formatVersion > FormatVersion.VERSION) {
                throw new IOException("The binary file is of newer version than expected! " +
                        formatVersion + " > " + FormatVersion.VERSION);
            }
        } else {
            // this is an old version J3O file
            // the signature was actually the class count
            numClasses = maybeSignature;

            // 0 indicates version before we started adding
            // version numbers
            formatVersion = 0;
        }

        int bytes = 4;
        aliasWidth = ((int) FastMath.log(numClasses, 256) + 1);

        classes.clear();
        for (int i = 0; i < numClasses; i++) {
            String alias = readString(bis, aliasWidth);

            // jME3 NEW: Read class version number
            int[] classHierarchyVersions;
            if (formatVersion >= 1) {
                int classHierarchySize = bis.read();
                classHierarchyVersions = new int[classHierarchySize];
                for (int j = 0; j < classHierarchySize; j++) {
                    classHierarchyVersions[j] = ByteUtils.readInt(bis);
                }
            } else {
                classHierarchyVersions = new int[]{0};
            }

            // read class name and class name size
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
                byte fieldAlias = (byte) bis.read();
                byte fieldType = (byte) bis.read();

                int fieldNameLength = ByteUtils.readInt(bis);
                String fieldName = readString(bis, fieldNameLength);
                BinaryClassField bcf = new BinaryClassField(fieldName, fieldAlias, fieldType);
                bco.nameFields.put(fieldName, bcf);
                bco.aliasFields.put(fieldAlias, bcf);
                bytes += (6 + fieldNameLength);
            }
            classes.put(alias, bco);
        }
        if (listener != null) {
            listener.readBytes(bytes);
        }

        int numLocs = ByteUtils.readInt(bis);
        bytes = 4;

        capsuleTable.clear();
        locationTable.clear();
        for (int i = 0; i < numLocs; i++) {
            int id = ByteUtils.readInt(bis);
            int loc = ByteUtils.readInt(bis);
            locationTable.put(id, loc);
            bytes += 8;
        }

        int numbIDs = ByteUtils.readInt(bis); // XXX: NOT CURRENTLY USED
        int id = ByteUtils.readInt(bis);
        bytes += 8;
        if (listener != null) {
            listener.readBytes(bytes);
        }

        if (baos == null) {
            baos = new ByteArrayOutputStream(bytes);
        } else {
            baos.reset();
        }
        int size = -1;
        byte[] cache = new byte[4096];
        while ((size = bis.read(cache)) != -1) {
            baos.write(cache, 0, size);
            if (listener != null) listener.readBytes(size);
        }

        dataArray = baos.toByteArray();

        Savable rVal = readObject(id);
        if (debug) {
            logger.log(Level.INFO, "BinaryImporter Stats:" +
                    "\n * Tags: {0}" +
                    "\n * Objects: {1}" +
                    "\n * Data: {2} bytes",
                    new Object[] {numClasses, numLocs, dataArray.length});
        }
        dataArray = null;
        return rVal;
    }

    /**
     * Loads a `Savable` object from the given `URL`.
     *
     * @param url The `URL` to the binary file.
     * @return The loaded `Savable` object.
     * @throws IOException If an I/O error occurs during reading from the URL.
     */
    public Savable load(URL url) throws IOException {
        return load(url, null);
    }

    /**
     * Loads a `Savable` object from the given `URL`, reporting read progress.
     *
     * @param url The `URL` to the binary file.
     * @param listener An optional `ReadListener` to report read progress. Can be `null`.
     * @return The loaded `Savable` object.
     * @throws IOException If an I/O error occurs during reading from the URL.
     */
    public Savable load(URL url, ReadListener listener) throws IOException {
        InputStream is = url.openStream();
        Savable rVal = load(is, listener);
        is.close();
        return rVal;
    }

    /**
     * Loads a `Savable` object from the given `File`.
     *
     * @param f The `File` object representing the binary file.
     * @return The loaded `Savable` object.
     * @throws IOException If an I/O error occurs during reading from the file.
     */
    public Savable load(File f) throws IOException {
        return load(f, null);
    }

    /**
     * Loads a `Savable` object from the given `File`, reporting read progress.
     *
     * @param f        The `File` object representing the binary file.
     * @param listener An optional `ReadListener` to report read progress. Can be `null`.
     * @return The loaded `Savable` object.
     * @throws IOException If an I/O error occurs during reading from the file.
     */
    public Savable load(File f, ReadListener listener) throws IOException {
        try (FileInputStream fis = new FileInputStream(f)) {
            return load(fis, listener);
        }
    }

    /**
     * Loads a `Savable` object from the given byte array.
     *
     * @param data The byte array containing the binary data.
     * @return The loaded `Savable` object.
     * @throws IOException If an I/O error occurs during reading from the byte array.
     */
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

    private String readString(InputStream is, int length) throws IOException {
        byte[] data = new byte[length];
        for (int j = 0; j < length; j++) {
            data[j] = (byte) is.read();
        }
        return new String(data);
    }

    private String readString(int length, int offset) {
        byte[] data = new byte[length];
        for (int j = 0; j < length; j++) {
            data[j] = dataArray[j + offset];
        }
        return new String(data);
    }

    /**
     * Reads and reconstructs a `Savable` object from the loaded binary data using its ID.
     * This method handles object graph traversal and ensures that objects are
     * only created once, using `contentTable` to store already-read objects.
     *
     * @param id The unique integer ID of the `Savable` object to read.
     * @return The reconstructed `Savable` object, or `null` if an error occurs
     * (e.g., class not found, invalid alias).
     */
    public Savable readObject(int id) {

        if (contentTable.get(id) != null) {
            return contentTable.get(id);
        }

        try {
            int loc = locationTable.get(id);

            String alias = readString(aliasWidth, loc);
            loc += aliasWidth;

            BinaryClassObject bco = classes.get(alias);

            if (bco == null) {
                logger.logp(Level.SEVERE, this.getClass().toString(), "readObject(int id)", "NULL class object: " + alias);
                return null;
            }

            int dataLength = ByteUtils.convertIntFromBytes(dataArray, loc);
            loc += 4;

            Savable out = SavableClassUtil.fromName(bco.className);

            BinaryInputCapsule cap = new BinaryInputCapsule(this, out, bco);
            cap.setContent(dataArray, loc, loc + dataLength);

            capsuleTable.put(out, cap);
            contentTable.put(id, out);

            out.read(this);

            capsuleTable.remove(out);

            return out;

        } catch (Exception e) {
            logger.logp(Level.SEVERE, this.getClass().toString(), "readObject(int id)", "Exception", e);
            return null;
        }
    }
}

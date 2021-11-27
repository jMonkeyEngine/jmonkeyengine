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
package com.jme3.export.binary;

import com.jme3.asset.AssetManager;
import com.jme3.export.FormatVersion;
import com.jme3.export.JmeExporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.export.SavableClassUtil;
import com.jme3.math.FastMath;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Exports savable objects in jMonkeyEngine's native binary format.
 *
 * <p>Format description: (Each numbered item
 * describes a series of bytes that follow sequentially, one after the other.)
 *
 * <p>1. "signature" - 4 bytes - 0x4A4D4533
 *
 * <p>2. "version" - 4 bytes - 0x00000002
 *
 * <p>3. "number of classes" - 4 bytes - number of entries in the class table
 *
 * <p>CLASS TABLE: X blocks, each consisting of items 4 thru 11,
 * where X = the number of Savable classes from item 3
 *
 * <p>4. "class alias" - X bytes, where X = ((int) FastMath.log(aliasCount,
 * 256) + 1)
 * - a numeric ID used to refer to a Savable class when reading or writing
 *
 * <p>5. "full class-name size" - 4 bytes - the number of bytes in item 6
 *
 * <p>6. "full class name" - X bytes of text, where X = the size from item 5
 * - the fully qualified class name of the Savable class,
 * e.g. <code>"com.jme.math.Vector3f"</code>
 *
 * <p>7. "number of fields" - 4 bytes
 * - the number of saved fields in the Savable class
 *
 * <p>8. "field alias" - 1 byte
 * - a numeric ID used to refer to a saved field when reading or writing.
 * Because field aliases are only a single byte,
 * no Savable class can save more than 256 fields.
 *
 * <p>9. "field type" - 1 byte
 * - the type of data in the saved field. Values are defined in
 * <code>com.jme.util.export.binary.BinaryClassField</code>.
 *
 * <p>10. "field-name size" - 4 bytes - the number of bytes in item 11
 *
 * <p>11. "field name" - X bytes of text, where X = the size from item 10
 * - the tag specified when reading or writing the saved field
 *
 * <p>12. "number of capsules" - 4 bytes
 * - the number of capsules in this stream
 *
 * <p>LOCATION TABLE: X blocks, each consisting of items 13 and 14,
 * where X = the number of capsules from item 12
 *
 * <p>13. "data id" - 4 bytes
 * - numeric ID of an object that was saved to this stream
 *
 * <p>14. "data location" - 4 bytes
 * - the offset in the capsule-data section where the savable object identified
 * in item 13 is stored
 *
 * <p>15. "future use" - 4 bytes - 0x00000001
 *
 * <p>16. "root id" - 4 bytes - numeric ID of the top-level savable object
 *
 * CAPSULE-DATA SECTION: X blocks, each consisting of items 17
 * thru 19, where X = the number of capsules from item 12
 *
 * <p>17. "class alias" - 4 bytes - see item 4
 *
 * <p>18. "capsule length" - 4 bytes - the length in bytes of item 19
 *
 * <p>19. "capsule data" - X bytes of data, where X = the number of bytes from
 * item 18
 *
 * @author Joshua Slack
 */

public class BinaryExporter implements JmeExporter {
    private static final Logger logger = Logger.getLogger(BinaryExporter.class
            .getName());

    protected int aliasCount = 1;
    protected int idCount = 1;

    final private IdentityHashMap<Savable, BinaryIdContentPair> contentTable
             = new IdentityHashMap<>();

    protected HashMap<Integer, Integer> locationTable
             = new HashMap<>();

    // key - class name, value = bco
    final private HashMap<String, BinaryClassObject> classes
             = new HashMap<>();

    final private ArrayList<Savable> contentKeys = new ArrayList<>();

    public static boolean debug = false;
    public static boolean useFastBufs = true;

    public BinaryExporter() {
    }

    public static BinaryExporter getInstance() {
        return new BinaryExporter();
    }

    /**
     * Saves the object into memory then loads it from memory.
     *
     * Used by tests to check if the persistence system is working.
     *
     * @param <T> The type of savable.
     * @param assetManager AssetManager to load assets from.
     * @param object The object to save and then load.
     * @return A new instance that has been saved and loaded from the
     * original object.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Savable> T saveAndLoad(AssetManager assetManager, T object) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            BinaryExporter exporter = new BinaryExporter();
            exporter.save(object, baos);
            BinaryImporter importer = new BinaryImporter();
            importer.setAssetManager(assetManager);
            return (T) importer.load(baos.toByteArray());
        } catch (IOException ex) {
            // Should never happen.
            throw new AssertionError(ex);
        }
    }

    @Override
    public void save(Savable object, OutputStream os) throws IOException {
        // reset some vars
        aliasCount = 1;
        idCount = 1;
        classes.clear();
        contentTable.clear();
        locationTable.clear();
        contentKeys.clear();

        // write signature and version
        os.write(ByteUtils.convertToBytes(FormatVersion.SIGNATURE)); // 1. "signature"
        os.write(ByteUtils.convertToBytes(FormatVersion.VERSION));   // 2. "version"

        int id = processBinarySavable(object);

        // write out tag table
        int classTableSize = 0;
        int classNum = classes.keySet().size();
        int aliasSize = ((int) FastMath.log(classNum, 256) + 1); // make all
                                                                  // aliases a
                                                                  // fixed width

        os.write(ByteUtils.convertToBytes(classNum)); // 3. "number of classes"
        for (String key : classes.keySet()) {
            BinaryClassObject bco = classes.get(key);

            // write alias
            byte[] aliasBytes = fixClassAlias(bco.alias,
                    aliasSize);
            os.write(aliasBytes);                     // 4. "class alias"
            classTableSize += aliasSize;

            // jME3 NEW: Write class hierarchy version numbers
            os.write( bco.classHierarchyVersions.length );
            for (int version : bco.classHierarchyVersions){
                os.write(ByteUtils.convertToBytes(version));
            }
            classTableSize += 1 + bco.classHierarchyVersions.length * 4;

            // write classname size & classname
            byte[] classBytes = key.getBytes();
            os.write(ByteUtils.convertToBytes(classBytes.length)); // 5. "full class-name size"
            os.write(classBytes);                                  // 6. "full class name"
            classTableSize += 4 + classBytes.length;

            // for each field, write alias, type, and name
            os.write(ByteUtils.convertToBytes(bco.nameFields.size())); // 7. "number of fields"
            for (String fieldName : bco.nameFields.keySet()) {
                BinaryClassField bcf = bco.nameFields.get(fieldName);
                os.write(bcf.alias);                                   // 8. "field alias"
                os.write(bcf.type);                                    // 9. "field type"

                byte[] fNameBytes = fieldName.getBytes();
                os.write(ByteUtils.convertToBytes(fNameBytes.length)); // 10. "field-name size"
                os.write(fNameBytes);                                  // 11. "field name"
                classTableSize += 2 + 4 + fNameBytes.length;
            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // write out data to a separate stream
        int location = 0;
        // keep track of location for each piece
        HashMap<String, ArrayList<BinaryIdContentPair>> alreadySaved = new HashMap<>(
                contentTable.size());
        for (Savable savable : contentKeys) {
            // look back at previous written data for matches
            String savableName = savable.getClass().getName();
            BinaryIdContentPair pair = contentTable.get(savable);
            ArrayList<BinaryIdContentPair> bucket = alreadySaved
                    .get(savableName + getChunk(pair));
            int prevLoc = findPrevMatch(pair, bucket);
            if (prevLoc != -1) {
                locationTable.put(pair.getId(), prevLoc);
                continue;
            }

            locationTable.put(pair.getId(), location);
            if (bucket == null) {
                bucket = new ArrayList<BinaryIdContentPair>();
                alreadySaved.put(savableName + getChunk(pair), bucket);
            }
            bucket.add(pair);
            byte[] aliasBytes = fixClassAlias(classes.get(savableName).alias, aliasSize);
            out.write(aliasBytes);            // 17. "class alias"
            location += aliasSize;
            BinaryOutputCapsule cap = contentTable.get(savable).getContent();
            out.write(ByteUtils.convertToBytes(cap.bytes.length)); // 18. "capsule length"
            location += 4; // length of bytes
            out.write(cap.bytes);             // 19. "capsule data"
            location += cap.bytes.length;
        }

        // write out location table
        // tag/location
        int numLocations = locationTable.keySet().size();
        os.write(ByteUtils.convertToBytes(numLocations)); // 12. "number of capsules"
        int locationTableSize = 0;
        for (Integer key : locationTable.keySet()) {
            os.write(ByteUtils.convertToBytes(key));                    // 13. "data id"
            os.write(ByteUtils.convertToBytes(locationTable.get(key))); // 14. "data location"
            locationTableSize += 8;
        }

        // write out number of root ids - hardcoded 1 for now
        os.write(ByteUtils.convertToBytes(1));  // 15. "future use"

        // write out root id
        os.write(ByteUtils.convertToBytes(id)); // 16. "root id"

        // append stream to the output stream
        out.writeTo(os);


        out = null;
        os = null;

        if (debug) {
            logger.fine("Stats:");
            logger.log(Level.FINE, "classes: {0}", classNum);
            logger.log(Level.FINE, "class table: {0} bytes", classTableSize);
            logger.log(Level.FINE, "objects: {0}", numLocations);
            logger.log(Level.FINE, "location table: {0} bytes", locationTableSize);
            logger.log(Level.FINE, "data: {0} bytes", location);
        }
    }

    private String getChunk(BinaryIdContentPair pair) {
        return new String(pair.getContent().bytes, 0, Math.min(64, pair
                .getContent().bytes.length));
    }

    private int findPrevMatch(BinaryIdContentPair oldPair,
            ArrayList<BinaryIdContentPair> bucket) {
        if (bucket == null)
            return -1;
        for (int x = bucket.size(); --x >= 0;) {
            BinaryIdContentPair pair = bucket.get(x);
            if (pair.getContent().equals(oldPair.getContent()))
                return locationTable.get(pair.getId());
        }
        return -1;
    }

    protected byte[] fixClassAlias(byte[] bytes, int width) {
        if (bytes.length != width) {
            byte[] newAlias = new byte[width];
            for (int x = width - bytes.length; x < width; x++)
                newAlias[x] = bytes[x - bytes.length];
            return newAlias;
        }
        return bytes;
    }

    @Override
    public void save(Savable object, File f) throws IOException {
        File parentDirectory = f.getParentFile();
        if (parentDirectory != null && !parentDirectory.exists()) {
            parentDirectory.mkdirs();
        }

        try (FileOutputStream fos = new FileOutputStream(f);
                BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            save(object, bos);
        }
    }

    @Override
    public OutputCapsule getCapsule(Savable object) {
        return contentTable.get(object).getContent();
    }

    private BinaryClassObject createClassObject(Class<? extends Savable> clazz) throws IOException{
        BinaryClassObject bco = new BinaryClassObject();
        bco.alias = generateTag();
        bco.nameFields = new HashMap<>();
        bco.classHierarchyVersions = SavableClassUtil.getSavableVersions(clazz);

        classes.put(clazz.getName(), bco);

        return bco;
    }

    public int processBinarySavable(Savable object) throws IOException {
        if (object == null) {
            return -1;
        }
        Class<? extends Savable> clazz = object.getClass();
        BinaryClassObject bco = classes.get(object.getClass().getName());
        // is this class been looked at before? in tagTable?
        if (bco == null) {
            bco = createClassObject(object.getClass());
        }

        // is object in contentTable?
        if (contentTable.get(object) != null) {
            return (contentTable.get(object).getId());
        }
        BinaryIdContentPair newPair = generateIdContentPair(bco);
        BinaryIdContentPair old = contentTable.put(object, newPair);
        if (old == null) {
            contentKeys.add(object);
        }
        object.write(this);
        newPair.getContent().finish();
        return newPair.getId();

    }

    protected byte[] generateTag() {
        int width = ((int) FastMath.log(aliasCount, 256) + 1);
        int count = aliasCount;
        aliasCount++;
        byte[] bytes = new byte[width];
        for (int x = width - 1; x >= 0; x--) {
            int pow = (int) FastMath.pow(256, x);
            int factor = count / pow;
            bytes[width - x - 1] = (byte) factor;
            count %= pow;
        }
        return bytes;
    }

    private BinaryIdContentPair generateIdContentPair(BinaryClassObject bco) {
        BinaryIdContentPair pair = new BinaryIdContentPair(idCount++,
                new BinaryOutputCapsule(this, bco));
        return pair;
    }
}
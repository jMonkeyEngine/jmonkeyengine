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

import com.jme3.export.FormatVersion;
import com.jme3.export.JmeExporter;
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
 * Exports to the jME Binary Format. Format descriptor: (each numbered item
 * denotes a series of bytes that follows sequentially one after the next.)
 * <p>
 * 1. "number of classes" - four bytes - int value representing the number of
 * entries in the class lookup table.
 * </p>
 * <p>
 * CLASS TABLE: There will be X blocks each consisting of numbers 2 thru 9,
 * where X = the number read in 1.
 * </p>
 * <p>
 * 2. "class alias" - 1...X bytes, where X = ((int) FastMath.log(aliasCount,
 * 256) + 1) - an alias used when writing object data to match an object to its
 * appropriate object class type.
 * </p>
 * <p>
 * 3. "full class name size" - four bytes - int value representing number of
 * bytes to read in for next field.
 * </p>
 * <p>
 * 4. "full class name" - 1...X bytes representing a String value, where X = the
 * number read in 3. The String is the fully qualified class name of the Savable
 * class, eg "<code>com.jme.math.Vector3f</code>"
 * </p>
 * <p>
 * 5. "number of fields" - four bytes - int value representing number of blocks
 * to read in next (numbers 6 - 9), where each block represents a field in this
 * class.
 * </p>
 * <p>
 * 6. "field alias" - 1 byte - the alias used when writing out fields in a
 * class. Because it is a single byte, a single class can not save out more than
 * a total of 256 fields.
 * </p>
 * <p>
 * 7. "field type" - 1 byte - a value representing the type of data a field
 * contains. This value is taken from the static fields of
 * <code>com.jme.util.export.binary.BinaryClassField</code>.
 * </p>
 * <p>
 * 8. "field name size" - 4 bytes - int value representing the size of the next
 * field.
 * </p>
 * <p>
 * 9. "field name" - 1...X bytes representing a String value, where X = the
 * number read in 8. The String is the full String value used when writing the
 * current field.
 * </p>
 * <p>
 * 10. "number of unique objects" - four bytes - int value representing the
 * number of data entries in this file.
 * </p>
 * <p>
 * DATA LOOKUP TABLE: There will be X blocks each consisting of numbers 11 and
 * 12, where X = the number read in 10.
 * </p>
 * <p>
 * 11. "data id" - four bytes - int value identifying a single unique object
 * that was saved in this data file.
 * </p>
 * <p>
 * 12. "data location" - four bytes - int value representing the offset in the
 * object data portion of this file where the object identified in 11 is
 * located.
 * </p>
 * <p>
 * 13. "future use" - four bytes - hardcoded int value 1.
 * </p>
 * <p>
 * 14. "root id" - four bytes - int value identifying the top level object.
 * </p>
 * <p>
 * OBJECT DATA SECTION: There will be X blocks each consisting of numbers 15
 * thru 19, where X = the number of unique location values named in 12.
 * <p>
 * 15. "class alias" - see 2.
 * </p>
 * <p>
 * 16. "data length" - four bytes - int value representing the length in bytes
 * of data stored in fields 17 and 18 for this object.
 * </p>
 * <p>
 * FIELD ENTRY: There will be X blocks each consisting of numbers 18 and 19
 * </p>
 * <p>
 * 17. "field alias" - see 6.
 * </p>
 * <p>
 * 18. "field data" - 1...X bytes representing the field data. The data length
 * is dependent on the field type and contents.
 * </p>
 *
 * @author Joshua Slack
 */

public class BinaryExporter implements JmeExporter {
    private static final Logger logger = Logger.getLogger(BinaryExporter.class
            .getName());

    protected int aliasCount = 1;
    protected int idCount = 1;

    protected IdentityHashMap<Savable, BinaryIdContentPair> contentTable
             = new IdentityHashMap<Savable, BinaryIdContentPair>();

    protected HashMap<Integer, Integer> locationTable
             = new HashMap<Integer, Integer>();

    // key - class name, value = bco
    private HashMap<String, BinaryClassObject> classes
             = new HashMap<String, BinaryClassObject>();

    private ArrayList<Savable> contentKeys = new ArrayList<Savable>();

    public static boolean debug = false;
    public static boolean useFastBufs = true;
      
    public BinaryExporter() {
    }

    public static BinaryExporter getInstance() {
        return new BinaryExporter();
    }

    public boolean save(Savable object, OutputStream os) throws IOException {
        // reset some vars
        aliasCount = 1;
        idCount = 1;
        classes.clear();
        contentTable.clear();
        locationTable.clear();
        contentKeys.clear();
        
        // write signature and version
        os.write(ByteUtils.convertToBytes(FormatVersion.SIGNATURE));
        os.write(ByteUtils.convertToBytes(FormatVersion.VERSION));
        
        int id = processBinarySavable(object);

        // write out tag table
        int classTableSize = 0;
        int classNum = classes.keySet().size();
        int aliasSize = ((int) FastMath.log(classNum, 256) + 1); // make all
                                                                  // aliases a
                                                                  // fixed width
        
        os.write(ByteUtils.convertToBytes(classNum));
        for (String key : classes.keySet()) {
            BinaryClassObject bco = classes.get(key);

            // write alias
            byte[] aliasBytes = fixClassAlias(bco.alias,
                    aliasSize);
            os.write(aliasBytes);
            classTableSize += aliasSize;
            
            // jME3 NEW: Write class hierarchy version numbers
            os.write( bco.classHierarchyVersions.length );
            for (int version : bco.classHierarchyVersions){
                os.write(ByteUtils.convertToBytes(version));
            }
            classTableSize += 1 + bco.classHierarchyVersions.length * 4;
            
            // write classname size & classname
            byte[] classBytes = key.getBytes();
            os.write(ByteUtils.convertToBytes(classBytes.length));
            os.write(classBytes);
            classTableSize += 4 + classBytes.length;
            
            // for each field, write alias, type, and name
            os.write(ByteUtils.convertToBytes(bco.nameFields.size()));
            for (String fieldName : bco.nameFields.keySet()) {
                BinaryClassField bcf = bco.nameFields.get(fieldName);
                os.write(bcf.alias);
                os.write(bcf.type);

                // write classname size & classname
                byte[] fNameBytes = fieldName.getBytes();
                os.write(ByteUtils.convertToBytes(fNameBytes.length));
                os.write(fNameBytes);
                classTableSize += 2 + 4 + fNameBytes.length;
            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // write out data to a seperate stream
        int location = 0;
        // keep track of location for each piece
        HashMap<String, ArrayList<BinaryIdContentPair>> alreadySaved = new HashMap<String, ArrayList<BinaryIdContentPair>>(
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
            out.write(aliasBytes);
            location += aliasSize;
            BinaryOutputCapsule cap = contentTable.get(savable).getContent();
            out.write(ByteUtils.convertToBytes(cap.bytes.length));
            location += 4; // length of bytes
            out.write(cap.bytes);
            location += cap.bytes.length;
        }

        // write out location table
        // tag/location
        int numLocations = locationTable.keySet().size();
        os.write(ByteUtils.convertToBytes(numLocations));
        int locationTableSize = 0;
        for (Integer key : locationTable.keySet()) {
            os.write(ByteUtils.convertToBytes(key));
            os.write(ByteUtils.convertToBytes(locationTable.get(key)));
            locationTableSize += 8;
        }

        // write out number of root ids - hardcoded 1 for now
        os.write(ByteUtils.convertToBytes(1));

        // write out root id
        os.write(ByteUtils.convertToBytes(id));

        // append stream to the output stream
        out.writeTo(os);


        out = null;
        os = null;

        if (debug ) {
            logger.info("Stats:");
            logger.log(Level.INFO, "classes: {0}", classNum);
            logger.log(Level.INFO, "class table: {0} bytes", classTableSize);
            logger.log(Level.INFO, "objects: {0}", numLocations);
            logger.log(Level.INFO, "location table: {0} bytes", locationTableSize);
            logger.log(Level.INFO, "data: {0} bytes", location);
        }

        return true;
    }

    protected String getChunk(BinaryIdContentPair pair) {
        return new String(pair.getContent().bytes, 0, Math.min(64, pair
                .getContent().bytes.length));
    }

    protected int findPrevMatch(BinaryIdContentPair oldPair,
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

    public boolean save(Savable object, File f) throws IOException {
        File parentDirectory = f.getParentFile();
        if(parentDirectory != null && !parentDirectory.exists()) {
            parentDirectory.mkdirs();
        }

        FileOutputStream fos = new FileOutputStream(f);
        boolean rVal = save(object, fos);
        fos.close();
        return rVal;
    }

    public BinaryOutputCapsule getCapsule(Savable object) {
        return contentTable.get(object).getContent();
    }

    private BinaryClassObject createClassObject(Class clazz) throws IOException{
        BinaryClassObject bco = new BinaryClassObject();
        bco.alias = generateTag();
        bco.nameFields = new HashMap<String, BinaryClassField>();
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

    protected BinaryIdContentPair generateIdContentPair(BinaryClassObject bco) {
        BinaryIdContentPair pair = new BinaryIdContentPair(idCount++,
                new BinaryOutputCapsule(this, bco));
        return pair;
    }
}
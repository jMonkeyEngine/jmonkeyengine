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
package com.jme3.scene.plugins.blender.file;

import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * A class representing a single structure in the file.
 * @author Marcin Roguski
 */
public class Structure implements Cloneable {

    /** The blender context. */
    private BlenderContext blenderContext;
    /** The address of the block that fills the structure. */
    private transient Long oldMemoryAddress;
    /** The type of the structure. */
    private String type;
    /**
     * The fields of the structure. Each field consists of a pair: name-type.
     */
    private Field[] fields;

    /**
     * Constructor that copies the data of the structure.
     * @param structure
     *        the structure to copy.
     * @param blenderContext
     *        the blender context of the structure
     * @throws CloneNotSupportedException
     *         this exception should never be thrown
     */
    private Structure(Structure structure, BlenderContext blenderContext) throws CloneNotSupportedException {
        type = structure.type;
        fields = new Field[structure.fields.length];
        for (int i = 0; i < fields.length; ++i) {
            fields[i] = (Field) structure.fields[i].clone();
        }
        this.blenderContext = blenderContext;
        this.oldMemoryAddress = structure.oldMemoryAddress;
    }

    /**
     * Constructor. Loads the structure from the given stream during instance creation.
     * @param inputStream
     *        the stream we read the structure from
     * @param names
     *        the names from which the name of structure and its fields will be taken
     * @param types
     *        the names of types for the structure
     * @param blenderContext
     *        the blender context
     * @throws BlenderFileException
     *         this exception occurs if the amount of fields, defined in the file, is negative
     */
    public Structure(BlenderInputStream inputStream, String[] names, String[] types, BlenderContext blenderContext) throws BlenderFileException {
        int nameIndex = inputStream.readShort();
        type = types[nameIndex];
        this.blenderContext = blenderContext;
        int fieldsAmount = inputStream.readShort();
        if (fieldsAmount < 0) {
            throw new BlenderFileException("The amount of fields of " + this.type + " structure cannot be negative!");
        }
        if (fieldsAmount > 0) {
            fields = new Field[fieldsAmount];
            for (int i = 0; i < fieldsAmount; ++i) {
                int typeIndex = inputStream.readShort();
                nameIndex = inputStream.readShort();
                fields[i] = new Field(names[nameIndex], types[typeIndex], blenderContext);
            }
        }
        this.oldMemoryAddress = Long.valueOf(-1L);
    }

    /**
     * This method fills the structure with data.
     * @param inputStream
     *        the stream we read data from, its read cursor should be placed at the start position of the data for the
     *        structure
     * @throws BlenderFileException
     *         an exception is thrown when the blend file is somehow invalid or corrupted
     */
    public void fill(BlenderInputStream inputStream) throws BlenderFileException {
        int position = inputStream.getPosition();
        inputStream.setPosition(position - 8 - inputStream.getPointerSize());
        this.oldMemoryAddress = Long.valueOf(inputStream.readPointer());
        inputStream.setPosition(position);
        for (Field field : fields) {
            field.fill(inputStream);
        }
    }

    /**
     * This method returns the value of the filed with a given name.
     * @param fieldName
     *        the name of the field
     * @return the value of the field or null if no field with a given name is found
     */
    public Object getFieldValue(String fieldName) {
        for (Field field : fields) {
            if (field.name.equalsIgnoreCase(fieldName)) {
                return field.value;
            }
        }
        return null;
    }

    /**
     * This method returns the value of the filed with a given name. The structure is considered to have flat fields
     * only (no substructures).
     * @param fieldName
     *        the name of the field
     * @return the value of the field or null if no field with a given name is found
     */
    public Object getFlatFieldValue(String fieldName) {
        for (Field field : fields) {
            Object value = field.value;
            if (field.name.equalsIgnoreCase(fieldName)) {
                return value;
            } else if (value instanceof Structure) {
                value = ((Structure) value).getFlatFieldValue(fieldName);
                if (value != null) {//we can compare references here, since we use one static object as a NULL field value
                    return value;
                }
            }
        }
        return null;
    }

    /**
     * This methos should be used on structures that are of a 'ListBase' type. It creates a List of structures that are
     * held by this structure within the blend file.
     * @param blenderContext
     *        the blender context
     * @return a list of filled structures
     * @throws BlenderFileException
     *         this exception is thrown when the blend file structure is somehow invalid or corrupted
     * @throws IllegalArgumentException
     *         this exception is thrown if the type of the structure is not 'ListBase'
     */
    public List<Structure> evaluateListBase(BlenderContext blenderContext) throws BlenderFileException {
        if (!"ListBase".equals(this.type)) {
            throw new IllegalStateException("This structure is not of type: 'ListBase'");
        }
        Pointer first = (Pointer) this.getFieldValue("first");
        Pointer last = (Pointer) this.getFieldValue("last");
        long currentAddress = 0;
        long lastAddress = last.getOldMemoryAddress();
        List<Structure> result = new LinkedList<Structure>();
        while (currentAddress != lastAddress) {
            currentAddress = first.getOldMemoryAddress();
            Structure structure = first.fetchData(blenderContext.getInputStream()).get(0);
            result.add(structure);
            first = (Pointer) structure.getFlatFieldValue("next");
        }
        return result;
    }

    /**
     * This method returns the type of the structure.
     * @return the type of the structure
     */
    public String getType() {
        return type;
    }

    /**
     * This method returns the amount of fields for the current structure.
     * @return the amount of fields for the current structure
     */
    public int getFieldsAmount() {
        return fields.length;
    }

    /**
     * This method returns the field name of the given index.
     * @param fieldIndex
     *        the index of the field
     * @return the field name of the given index
     */
    public String getFieldName(int fieldIndex) {
        return fields[fieldIndex].name;
    }

    /**
     * This method returns the field type of the given index.
     * @param fieldIndex
     *        the index of the field
     * @return the field type of the given index
     */
    public String getFieldType(int fieldIndex) {
        return fields[fieldIndex].type;
    }

    /**
     * This method returns the address of the structure. The strucutre should be filled with data otherwise an exception
     * is thrown.
     * @return the address of the feature stored in this structure
     */
    public Long getOldMemoryAddress() {
        if (oldMemoryAddress.longValue() == -1L) {
            throw new IllegalStateException("Call the 'fill' method and fill the structure with data first!");
        }
        return oldMemoryAddress;
    }

/**
	 * This method returns the name of the structure. If the structure has an ID field then the name is returned.
	 * Otherwise the name does not exists and the method returns null.
	 * @return the name of the structure read from the ID field or null
	 */
	public String getName() {
		Object fieldValue = this.getFieldValue("ID");
		if(fieldValue instanceof Structure) {
			Structure id = (Structure)fieldValue;
			return id == null ? null : id.getFieldValue("name").toString().substring(2);//blender adds 2-charactes as a name prefix
		}
		return null;
	}
	
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("struct ").append(type).append(" {\n");
        for (int i = 0; i < fields.length; ++i) {
            result.append(fields[i].toString()).append('\n');
        }
        return result.append('}').toString();
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return new Structure(this, blenderContext);
    }

    /**
     * This enum enumerates all known data types that can be found in the blend file.
     * @author Marcin Roguski
     */
    /*package*/
    static enum DataType {

        CHARACTER, SHORT, INTEGER, LONG, FLOAT, DOUBLE, VOID, STRUCTURE, POINTER;
        /** The map containing the known primary types. */
        private static final Map<String, DataType> PRIMARY_TYPES = new HashMap<String, DataType>(10);

        static {
            PRIMARY_TYPES.put("char", CHARACTER);
            PRIMARY_TYPES.put("uchar", CHARACTER);
            PRIMARY_TYPES.put("short", SHORT);
            PRIMARY_TYPES.put("ushort", SHORT);
            PRIMARY_TYPES.put("int", INTEGER);
            PRIMARY_TYPES.put("long", LONG);
            PRIMARY_TYPES.put("ulong", LONG);
            PRIMARY_TYPES.put("uint64_t", LONG);
            PRIMARY_TYPES.put("float", FLOAT);
            PRIMARY_TYPES.put("double", DOUBLE);
            PRIMARY_TYPES.put("void", VOID);
        }

        /**
         * This method returns the data type that is appropriate to the given type name. WARNING! The type recognition
         * is case sensitive!
         * @param type
         *        the type name of the data
         * @param blenderContext
         *        the blender context
         * @return appropriate enum value to the given type name
         * @throws BlenderFileException
         *         this exception is thrown if the given type name does not exist in the blend file
         */
        public static DataType getDataType(String type, BlenderContext blenderContext) throws BlenderFileException {
            DataType result = PRIMARY_TYPES.get(type);
            if (result != null) {
                return result;
            }
            if (blenderContext.getDnaBlockData().hasStructure(type)) {
                return STRUCTURE;
            }
            throw new BlenderFileException("Unknown data type: " + type);
        }
    }
}

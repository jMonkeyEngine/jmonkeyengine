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
import java.util.Map;

/**
 * The data block containing the description of the file.
 * @author Marcin Roguski
 */
public class DnaBlockData {

    private static final int SDNA_ID = 'S' << 24 | 'D' << 16 | 'N' << 8 | 'A';	//SDNA
    private static final int NAME_ID = 'N' << 24 | 'A' << 16 | 'M' << 8 | 'E';	//NAME
    private static final int TYPE_ID = 'T' << 24 | 'Y' << 16 | 'P' << 8 | 'E';	//TYPE
    private static final int TLEN_ID = 'T' << 24 | 'L' << 16 | 'E' << 8 | 'N';	//TLEN
    private static final int STRC_ID = 'S' << 24 | 'T' << 16 | 'R' << 8 | 'C';	//STRC
    /** Structures available inside the file. */
    private final Structure[] structures;
    /** A map that helps finding a structure by type. */
    private final Map<String, Structure> structuresMap;

    /**
     * Constructor. Loads the block from the given stream during instance creation.
     * @param inputStream
     *        the stream we read the block from
     * @param blenderContext
     *        the blender context
     * @throws BlenderFileException
     *         this exception is throw if the blend file is invalid or somehow corrupted
     */
    public DnaBlockData(BlenderInputStream inputStream, BlenderContext blenderContext) throws BlenderFileException {
        int identifier;

        //reading 'SDNA' identifier
        identifier = inputStream.readByte() << 24 | inputStream.readByte() << 16
                | inputStream.readByte() << 8 | inputStream.readByte();

        if (identifier != SDNA_ID) {
            throw new BlenderFileException("Invalid identifier! '" + this.toString(SDNA_ID) + "' expected and found: " + this.toString(identifier));
        }

        //reading names
        identifier = inputStream.readByte() << 24 | inputStream.readByte() << 16
                | inputStream.readByte() << 8 | inputStream.readByte();
        if (identifier != NAME_ID) {
            throw new BlenderFileException("Invalid identifier! '" + this.toString(NAME_ID) + "' expected and found: " + this.toString(identifier));
        }
        int amount = inputStream.readInt();
        if (amount <= 0) {
            throw new BlenderFileException("The names amount number should be positive!");
        }
        String[] names = new String[amount];
        for (int i = 0; i < amount; ++i) {
            names[i] = inputStream.readString();
        }

        //reding types
        inputStream.alignPosition(4);
        identifier = inputStream.readByte() << 24 | inputStream.readByte() << 16
                | inputStream.readByte() << 8 | inputStream.readByte();
        if (identifier != TYPE_ID) {
            throw new BlenderFileException("Invalid identifier! '" + this.toString(TYPE_ID) + "' expected and found: " + this.toString(identifier));
        }
        amount = inputStream.readInt();
        if (amount <= 0) {
            throw new BlenderFileException("The types amount number should be positive!");
        }
        String[] types = new String[amount];
        for (int i = 0; i < amount; ++i) {
            types[i] = inputStream.readString();
        }

        //reading lengths
        inputStream.alignPosition(4);
        identifier = inputStream.readByte() << 24 | inputStream.readByte() << 16
                | inputStream.readByte() << 8 | inputStream.readByte();
        if (identifier != TLEN_ID) {
            throw new BlenderFileException("Invalid identifier! '" + this.toString(TLEN_ID) + "' expected and found: " + this.toString(identifier));
        }
        int[] lengths = new int[amount];//theamount is the same as int types
        for (int i = 0; i < amount; ++i) {
            lengths[i] = inputStream.readShort();
        }

        //reading structures
        inputStream.alignPosition(4);
        identifier = inputStream.readByte() << 24 | inputStream.readByte() << 16
                | inputStream.readByte() << 8 | inputStream.readByte();
        if (identifier != STRC_ID) {
            throw new BlenderFileException("Invalid identifier! '" + this.toString(STRC_ID) + "' expected and found: " + this.toString(identifier));
        }
        amount = inputStream.readInt();
        if (amount <= 0) {
            throw new BlenderFileException("The structures amount number should be positive!");
        }
        structures = new Structure[amount];
        structuresMap = new HashMap<String, Structure>(amount);
        for (int i = 0; i < amount; ++i) {
            structures[i] = new Structure(inputStream, names, types, blenderContext);
            if (structuresMap.containsKey(structures[i].getType())) {
                throw new BlenderFileException("Blend file seems to be corrupted! The type " + structures[i].getType() + " is defined twice!");
            }
            structuresMap.put(structures[i].getType(), structures[i]);
        }
    }

    /**
     * This method returns the amount of the structures.
     * @return the amount of the structures
     */
    public int getStructuresCount() {
        return structures.length;
    }

    /**
     * This method returns the structure of the given index.
     * @param index
     *        the index of the structure
     * @return the structure of the given index
     */
    public Structure getStructure(int index) {
        try {
            return (Structure) structures[index].clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Structure should be clonable!!!", e);
        }
    }

    /**
     * This method returns a structure of the given name. If the name does not exists then null is returned.
     * @param name
     *        the name of the structure
     * @return the required structure or null if the given name is inapropriate
     */
    public Structure getStructure(String name) {
        try {
            return (Structure) structuresMap.get(name).clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /**
     * This method indicates if the structure of the given name exists.
     * @param name
     *        the name of the structure
     * @return true if the structure exists and false otherwise
     */
    public boolean hasStructure(String name) {
        return structuresMap.containsKey(name);
    }

    /**
     * This method converts the given identifier code to string.
     * @param code
     *        the code taht is to be converted
     * @return the string value of the identifier
     */
    private String toString(int code) {
        char c1 = (char) ((code & 0xFF000000) >> 24);
        char c2 = (char) ((code & 0xFF0000) >> 16);
        char c3 = (char) ((code & 0xFF00) >> 8);
        char c4 = (char) (code & 0xFF);
        return String.valueOf(c1) + c2 + c3 + c4;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("=============== ").append(SDNA_ID).append('\n');
        for (Structure structure : structures) {
            stringBuilder.append(structure.toString()).append('\n');
        }
        return stringBuilder.append("===============").toString();
    }
}

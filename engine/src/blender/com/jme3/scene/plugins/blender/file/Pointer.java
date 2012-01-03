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
package com.jme3.scene.plugins.blender.file;

import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import java.util.ArrayList;
import java.util.List;

/**
 * A class that represents a pointer of any level that can be stored in the file.
 * @author Marcin Roguski
 */
public class Pointer {

    /** The blender context. */
    private BlenderContext blenderContext;
    /** The level of the pointer. */
    private int pointerLevel;
    /** The address in file it points to. */
    private long oldMemoryAddress;
    /** This variable indicates if the field is a function pointer. */
    public boolean function;

    /**
     * Constructr. Stores the basic data about the pointer.
     * @param pointerLevel
     *        the level of the pointer
     * @param function
     *        this variable indicates if the field is a function pointer
     * @param blenderContext
     *        the repository f data; used in fetching the value that the pointer points
     */
    public Pointer(int pointerLevel, boolean function, BlenderContext blenderContext) {
        this.pointerLevel = pointerLevel;
        this.function = function;
        this.blenderContext = blenderContext;
    }

    /**
     * This method fills the pointer with its address value (it doesn't get the actual data yet. Use the 'fetch' method
     * for this.
     * @param inputStream
     *        the stream we read the pointer value from
     */
    public void fill(BlenderInputStream inputStream) {
        oldMemoryAddress = inputStream.readPointer();
    }

    /**
     * This method fetches the data stored under the given address.
     * @param inputStream
     *        the stream we read data from
     * @return the data read from the file
     * @throws BlenderFileException
     *         this exception is thrown when the blend file structure is somehow invalid or corrupted
     */
    public List<Structure> fetchData(BlenderInputStream inputStream) throws BlenderFileException {
        if (oldMemoryAddress == 0) {
            throw new NullPointerException("The pointer points to nothing!");
        }
        List<Structure> structures = null;
        FileBlockHeader dataFileBlock = blenderContext.getFileBlock(oldMemoryAddress);
        if (pointerLevel > 1) {
            int pointersAmount = dataFileBlock.getSize() / inputStream.getPointerSize() * dataFileBlock.getCount();
            for (int i = 0; i < pointersAmount; ++i) {
                inputStream.setPosition(dataFileBlock.getBlockPosition() + inputStream.getPointerSize() * i);
                long oldMemoryAddress = inputStream.readPointer();
                if (oldMemoryAddress != 0L) {
                    Pointer p = new Pointer(pointerLevel - 1, this.function, blenderContext);
                    p.oldMemoryAddress = oldMemoryAddress;
                    if (structures == null) {
                        structures = p.fetchData(inputStream);
                    } else {
                        structures.addAll(p.fetchData(inputStream));
                    }
                }
            }
        } else {
            inputStream.setPosition(dataFileBlock.getBlockPosition());
            structures = new ArrayList<Structure>(dataFileBlock.getCount());
            for (int i = 0; i < dataFileBlock.getCount(); ++i) {
                Structure structure = blenderContext.getDnaBlockData().getStructure(dataFileBlock.getSdnaIndex());
                structure.fill(inputStream);
                structures.add(structure);
            }
            return structures;
        }
        return structures;
    }

    /**
     * This method indicates if this pointer points to a function.
     * @return <b>true</b> if this is a function pointer and <b>false</b> otherwise
     */
    public boolean isFunction() {
        return function;
    }

    /**
     * This method indicates if this is a null-pointer or not.
     * @return <b>true</b> if the pointer is null and <b>false</b> otherwise
     */
    public boolean isNull() {
        return oldMemoryAddress == 0;
    }
    
    /**
     * This method indicates if this is a null-pointer or not.
     * @return <b>true</b> if the pointer is not null and <b>false</b> otherwise
     */
    public boolean isNotNull() {
        return oldMemoryAddress != 0;
    }

    /**
     * This method returns the old memory address of the structure pointed by the pointer.
     * @return the old memory address of the structure pointed by the pointer
     */
    public long getOldMemoryAddress() {
        return oldMemoryAddress;
    }

    @Override
    public String toString() {
        return oldMemoryAddress == 0 ? "{$null$}" : "{$" + oldMemoryAddress + "$}";
    }

    @Override
    public int hashCode() {
        return 31 + (int) (oldMemoryAddress ^ oldMemoryAddress >>> 32);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        Pointer other = (Pointer) obj;
        if (oldMemoryAddress != other.oldMemoryAddress) {
            return false;
        }
        return true;
    }
}

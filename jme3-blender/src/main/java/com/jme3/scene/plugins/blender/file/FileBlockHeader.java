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

import java.util.logging.Logger;

import com.jme3.scene.plugins.blender.BlenderContext;

/**
 * A class that holds the header data of a file block. The file block itself is not implemented. This class holds its
 * start position in the stream and using this the structure can fill itself with the proper data.
 * @author Marcin Roguski
 */
public class FileBlockHeader {
    private static final Logger LOGGER = Logger.getLogger(FileBlockHeader.class.getName());

    /** Identifier of the file-block [4 bytes]. */
    private BlockCode           code;
    /** Total length of the data after the file-block-header [4 bytes]. */
    private int                 size;
    /**
     * Memory address the structure was located when written to disk [4 or 8 bytes (defined in file header as a pointer
     * size)].
     */
    private long                oldMemoryAddress;
    /** Index of the SDNA structure [4 bytes]. */
    private int                 sdnaIndex;
    /** Number of structure located in this file-block [4 bytes]. */
    private int                 count;
    /** Start position of the block's data in the stream. */
    private int                 blockPosition;

    /**
     * Constructor. Loads the block header from the given stream during instance creation.
     * @param inputStream
     *            the stream we read the block header from
     * @param blenderContext
     *            the blender context
     * @throws BlenderFileException
     *             this exception is thrown when the pointer size is neither 4 nor 8
     */
    public FileBlockHeader(BlenderInputStream inputStream, BlenderContext blenderContext) throws BlenderFileException {
        inputStream.alignPosition(4);
        code = BlockCode.valueOf(inputStream.readByte() << 24 | inputStream.readByte() << 16 | inputStream.readByte() << 8 | inputStream.readByte());
        size = inputStream.readInt();
        oldMemoryAddress = inputStream.readPointer();
        sdnaIndex = inputStream.readInt();
        count = inputStream.readInt();
        blockPosition = inputStream.getPosition();
        if (BlockCode.BLOCK_DNA1 == code) {
            blenderContext.setBlockData(new DnaBlockData(inputStream, blenderContext));
        } else {
            inputStream.setPosition(blockPosition + size);
            blenderContext.addFileBlockHeader(Long.valueOf(oldMemoryAddress), this);
        }
    }

    /**
     * This method returns the structure described by the header filled with appropriate data.
     * @param blenderContext
     *            the blender context
     * @return structure filled with data
     * @throws BlenderFileException
     */
    public Structure getStructure(BlenderContext blenderContext) throws BlenderFileException {
        blenderContext.getInputStream().setPosition(blockPosition);
        Structure structure = blenderContext.getDnaBlockData().getStructure(sdnaIndex);
        structure.fill(blenderContext.getInputStream());
        return structure;
    }

    /**
     * This method returns the code of this data block.
     * @return the code of this data block
     */
    public BlockCode getCode() {
        return code;
    }

    /**
     * This method returns the size of the data stored in this block.
     * @return the size of the data stored in this block
     */
    public int getSize() {
        return size;
    }

    /**
     * This method returns the sdna index.
     * @return the sdna index
     */
    public int getSdnaIndex() {
        return sdnaIndex;
    }

    /**
     * This data returns the number of structure stored in the data block after this header.
     * @return the number of structure stored in the data block after this header
     */
    public int getCount() {
        return count;
    }

    /**
     * This method returns the start position of the data block in the blend file stream.
     * @return the start position of the data block
     */
    public int getBlockPosition() {
        return blockPosition;
    }

    /**
     * This method indicates if the block is the last block in the file.
     * @return true if this block is the last one in the file nad false otherwise
     */
    public boolean isLastBlock() {
        return BlockCode.BLOCK_ENDB == code;
    }

    /**
     * This method indicates if the block is the SDNA block.
     * @return true if this block is the SDNA block and false otherwise
     */
    public boolean isDnaBlock() {
        return BlockCode.BLOCK_DNA1 == code;
    }

    @Override
    public String toString() {
        return "FILE BLOCK HEADER [" + code.toString() + " : " + size + " : " + oldMemoryAddress + " : " + sdnaIndex + " : " + count + "]";
    }

    public static enum BlockCode {
        BLOCK_ME00('M' << 24 | 'E' << 16), // mesh
        BLOCK_CA00('C' << 24 | 'A' << 16), // camera
        BLOCK_LA00('L' << 24 | 'A' << 16), // lamp
        BLOCK_OB00('O' << 24 | 'B' << 16), // object
        BLOCK_MA00('M' << 24 | 'A' << 16), // material
        BLOCK_SC00('S' << 24 | 'C' << 16), // scene
        BLOCK_WO00('W' << 24 | 'O' << 16), // world
        BLOCK_TX00('T' << 24 | 'X' << 16), // texture
        BLOCK_IP00('I' << 24 | 'P' << 16), // ipo
        BLOCK_AC00('A' << 24 | 'C' << 16), // action
        BLOCK_IM00('I' << 24 | 'M' << 16), // image
        BLOCK_TE00('T' << 24 | 'E' << 16), BLOCK_WM00('W' << 24 | 'M' << 16), BLOCK_SR00('S' << 24 | 'R' << 16), BLOCK_SN00('S' << 24 | 'N' << 16), BLOCK_BR00('B' << 24 | 'R' << 16), BLOCK_LS00('L' << 24 | 'S' << 16), BLOCK_GLOB('G' << 24 | 'L' << 16 | 'O' << 8 | 'B'), BLOCK_REND('R' << 24 | 'E' << 16 | 'N' << 8 | 'D'), BLOCK_DATA('D' << 24 | 'A' << 16 | 'T' << 8 | 'A'), BLOCK_DNA1('D' << 24 | 'N' << 16 | 'A' << 8 | '1'), BLOCK_ENDB('E' << 24 | 'N' << 16 | 'D' << 8 | 'B'), BLOCK_TEST('T' << 24 | 'E' << 16
                | 'S' << 8 | 'T'), BLOCK_UNKN(0);

        private int code;

        private BlockCode(int code) {
            this.code = code;
        }

        public static BlockCode valueOf(int code) {
            for (BlockCode blockCode : BlockCode.values()) {
                if (blockCode.code == code) {
                    return blockCode;
                }
            }
            byte[] codeBytes = new byte[] { (byte) (code >> 24 & 0xFF), (byte) (code >> 16 & 0xFF), (byte) (code >> 8 & 0xFF), (byte) (code & 0xFF) };
            LOGGER.warning("Unknown block header: " + new String(codeBytes));
            return BLOCK_UNKN;
        }
    }
}

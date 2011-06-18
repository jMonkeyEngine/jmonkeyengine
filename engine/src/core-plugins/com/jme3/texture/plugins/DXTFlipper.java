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
package com.jme3.texture.plugins;

import com.jme3.math.FastMath;
import com.jme3.texture.Image.Format;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * DXTFlipper is a utility class used to flip along Y axis DXT compressed textures.
 * 
 * @author Kirill Vainer
 */
public class DXTFlipper {

    private static final ByteBuffer bb = ByteBuffer.allocate(8);

    static {
        bb.order(ByteOrder.LITTLE_ENDIAN);
    }

    private static long readCode5(long data, int x, int y){
        long shift =   (4 * y + x) * 3;
        long mask = 0x7;
        mask <<= shift;
        long code = data & mask;
        code >>= shift;
        return code;
    }

    private static long writeCode5(long data, int x, int y, long code){
        long shift =  (4 * y + x) * 3;
        long mask = 0x7;
        code = (code & mask) << shift;
        mask <<= shift;
        mask = ~mask;
        data &= mask;
        data |= code; // write new code
        return data;
    }

    private static void flipDXT5Block(byte[] block, int h){
        if (h == 1)
            return;

        byte c0 = block[0];
        byte c1 = block[1];

        bb.clear();
        bb.put(block, 2, 6).flip();
        bb.clear();
        long l = bb.getLong();
        long n = l;

        if (h == 2){
            n = writeCode5(n, 0, 0, readCode5(l, 0, 1));
            n = writeCode5(n, 1, 0, readCode5(l, 1, 1));
            n = writeCode5(n, 2, 0, readCode5(l, 2, 1));
            n = writeCode5(n, 3, 0, readCode5(l, 3, 1));

            n = writeCode5(n, 0, 1, readCode5(l, 0, 0));
            n = writeCode5(n, 1, 1, readCode5(l, 1, 0));
            n = writeCode5(n, 2, 1, readCode5(l, 2, 0));
            n = writeCode5(n, 3, 1, readCode5(l, 3, 0));
        }else{
            n = writeCode5(n, 0, 0, readCode5(l, 0, 3));
            n = writeCode5(n, 1, 0, readCode5(l, 1, 3));
            n = writeCode5(n, 2, 0, readCode5(l, 2, 3));
            n = writeCode5(n, 3, 0, readCode5(l, 3, 3));

            n = writeCode5(n, 0, 1, readCode5(l, 0, 2));
            n = writeCode5(n, 1, 1, readCode5(l, 1, 2));
            n = writeCode5(n, 2, 1, readCode5(l, 2, 2));
            n = writeCode5(n, 3, 1, readCode5(l, 3, 2));

            n = writeCode5(n, 0, 2, readCode5(l, 0, 1));
            n = writeCode5(n, 1, 2, readCode5(l, 1, 1));
            n = writeCode5(n, 2, 2, readCode5(l, 2, 1));
            n = writeCode5(n, 3, 2, readCode5(l, 3, 1));

            n = writeCode5(n, 0, 3, readCode5(l, 0, 0));
            n = writeCode5(n, 1, 3, readCode5(l, 1, 0));
            n = writeCode5(n, 2, 3, readCode5(l, 2, 0));
            n = writeCode5(n, 3, 3, readCode5(l, 3, 0));
        }
            
        bb.clear();
        bb.putLong(n);
        bb.clear();
        bb.get(block, 2, 6).flip();

        assert c0 == block[0] && c1 == block[1];
    }

    private static void flipDXT3Block(byte[] block, int h){
        if (h == 1)
            return;

        // first row
        byte tmp0 = block[0];
        byte tmp1 = block[1];

        if (h == 2){
            block[0] = block[2];
            block[1] = block[3];

            block[2] = tmp0;
            block[3] = tmp1;
        }else{
            // write last row to first row
            block[0] = block[6];
            block[1] = block[7];

            // write first row to last row
            block[6] = tmp0;
            block[7] = tmp1;

            // 2nd row
            tmp0 = block[2];
            tmp1 = block[3];

            // write 3rd row to 2nd
            block[2] = block[4];
            block[3] = block[5];

            // write 2nd row to 3rd
            block[4] = tmp0;
            block[5] = tmp1;
        }
    }

    /**
     * Flips a DXT color block or a DXT3 alpha block
     * @param block
     * @param h
     */
    private static void flipDXT1orDXTA3Block(byte[] block, int h){
        byte tmp;
        switch (h){
            case 1:
                return;
            case 2:
                // keep header intact (the two colors)
                // header takes 4 bytes

                // flip only two top rows
                tmp = block[4+1];
                block[4+1] = block[4+0];
                block[4+0] = tmp;
                return;
            default:
                // keep header intact (the two colors)
                // header takes 4 bytes

                // flip first & fourth row
                tmp = block[4+3];
                block[4+3] = block[4+0];
                block[4+0] = tmp;

                // flip second and third row
                tmp = block[4+2];
                block[4+2] = block[4+1];
                block[4+1] = tmp;
                return;
        }
    }

    public static ByteBuffer flipDXT(ByteBuffer img, int w, int h, Format format){
        int blocksX = (int) FastMath.ceil((float)w / 4f);
        int blocksY = (int) FastMath.ceil((float)h / 4f);

        int type;
        switch (format){
            case DXT1:
            case DXT1A:
                type = 1;
                break;
            case DXT3:
                type = 2;
                break;
            case DXT5:
                type = 3;
                break;
            case LATC:
                type = 4;
                break;
            case LTC:
                type = 5;
                break;
            default:
                throw new IllegalArgumentException();
        }

        // DXT1 uses 8 bytes per block,
        // DXT3, DXT5, LATC use 16 bytes per block
        int bpb = type == 1 || type == 5 ? 8 : 16;

        ByteBuffer retImg = BufferUtils.createByteBuffer(blocksX * blocksY * bpb);

        if (h == 1){
            retImg.put(img);
            retImg.rewind();
            return retImg;
        }else if (h == 2){
            byte[] colorBlock = new byte[8];
            byte[] alphaBlock = type != 1 && type != 5 ? new byte[8] : null;
            for (int x = 0; x < blocksX; x++){
                // prepeare for block reading
                int blockByteOffset = x * bpb;
                img.position(blockByteOffset);
                img.limit(blockByteOffset + bpb);

                img.get(colorBlock);
                if (type == 4 || type == 5)
                    flipDXT5Block(colorBlock, h);
                else
                    flipDXT1orDXTA3Block(colorBlock, h);

                // write block (no need to flip block indexes, only pixels
                // inside block
                retImg.put(colorBlock);

                if (alphaBlock != null){
                    img.get(alphaBlock);
                    switch (type){
                        case 2:
                            flipDXT3Block(alphaBlock, h); break;
                        case 3:
                        case 4: 
                            flipDXT5Block(alphaBlock, h);
                            break;
                    }
                    retImg.put(alphaBlock);
                }
            }
            retImg.rewind();
            return retImg;
        }else if (h >= 4){
            byte[] colorBlock = new byte[8];
            byte[] alphaBlock = type != 1 && type != 5 ? new byte[8] : null;
            for (int y = 0; y < blocksY; y++){
                for (int x = 0; x < blocksX; x++){
                    // prepeare for block reading
                    int blockIdx = y * blocksX + x;
                    int blockByteOffset = blockIdx * bpb;

                    img.position(blockByteOffset);
                    img.limit(blockByteOffset + bpb);

                    blockIdx = (blocksY - y - 1) * blocksX + x;
                    blockByteOffset = blockIdx * bpb;

                    retImg.position(blockByteOffset);
                    retImg.limit(blockByteOffset + bpb);

                    if (alphaBlock != null){
                        img.get(alphaBlock);
                        switch (type){
                            case 2:
                                flipDXT3Block(alphaBlock, h);
                                break;
                            case 3:
                            case 4:
                                flipDXT5Block(alphaBlock, h);
                                break;
                        }
                        retImg.put(alphaBlock);
                    }

                    img.get(colorBlock);
                    if (type == 4 || type == 5)
                        flipDXT5Block(colorBlock, h);
                    else
                        flipDXT1orDXTA3Block(colorBlock, h);
                    
                    retImg.put(colorBlock);
                }
            }
            retImg.limit(retImg.capacity());
            retImg.position(0);
            return retImg;
        }else{
            return null;
        }
    }

}

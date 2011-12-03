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

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;
import com.jme3.asset.TextureKey;
import com.jme3.math.FastMath;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.util.BufferUtils;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * <code>TextureManager</code> provides static methods for building a
 * <code>Texture</code> object. Typically, the information supplied is the
 * filename and the texture properties.
 * 
 * @author Mark Powell
 * @author Joshua Slack - cleaned, commented, added ability to read 16bit true color and color-mapped TGAs.
 * @author Kirill Vainer - ported to jME3
 * @version $Id: TGALoader.java 4131 2009-03-19 20:15:28Z blaine.dev $
 */
public final class TGALoader implements AssetLoader {

    // 0 - no image data in file
    public static final int TYPE_NO_IMAGE = 0;

    // 1 - uncompressed, color-mapped image
    public static final int TYPE_COLORMAPPED = 1;

    // 2 - uncompressed, true-color image
    public static final int TYPE_TRUECOLOR = 2;

    // 3 - uncompressed, black and white image
    public static final int TYPE_BLACKANDWHITE = 3;

    // 9 - run-length encoded, color-mapped image
    public static final int TYPE_COLORMAPPED_RLE = 9;

    // 10 - run-length encoded, true-color image
    public static final int TYPE_TRUECOLOR_RLE = 10;

    // 11 - run-length encoded, black and white image
    public static final int TYPE_BLACKANDWHITE_RLE = 11;

    public Object load(AssetInfo info) throws IOException{
        if (!(info.getKey() instanceof TextureKey))
            throw new IllegalArgumentException("Texture assets must be loaded using a TextureKey");

        boolean flip = ((TextureKey)info.getKey()).isFlipY();
        InputStream in = null;
        try {
            in = info.openStream();
            Image img = load(in, flip);
            return img;
        } finally {
            if (in != null){
                in.close();
            }
        }
    }

    /**
     * <code>loadImage</code> is a manual image loader which is entirely
     * independent of AWT. OUT: RGB888 or RGBA8888 Image object
     * 
     * @return <code>Image</code> object that contains the
     *         image, either as a RGB888 or RGBA8888
     * @param flip
     *            Flip the image vertically
     * @param exp32
     *            Add a dummy Alpha channel to 24b RGB image.
     * @param fis
     *            InputStream of an uncompressed 24b RGB or 32b RGBA TGA
     * @throws java.io.IOException
     */
    public static Image load(InputStream in, boolean flip) throws IOException {
        boolean flipH = false;

        // open a stream to the file
        DataInputStream dis = new DataInputStream(new BufferedInputStream(in));

        // ---------- Start Reading the TGA header ---------- //
        // length of the image id (1 byte)
        int idLength = dis.readUnsignedByte();

        // Type of color map (if any) included with the image
        // 0 - no color map data is included
        // 1 - a color map is included
        int colorMapType = dis.readUnsignedByte();

        // Type of image being read:
        int imageType = dis.readUnsignedByte();

        // Read Color Map Specification (5 bytes)
        // Index of first color map entry (if we want to use it, uncomment and remove extra read.)
//        short cMapStart = flipEndian(dis.readShort());
        dis.readShort();
        // number of entries in the color map
        short cMapLength = flipEndian(dis.readShort());
        // number of bits per color map entry
        int cMapDepth = dis.readUnsignedByte();

        // Read Image Specification (10 bytes)
        // horizontal coordinate of lower left corner of image. (if we want to use it, uncomment and remove extra read.)
//        int xOffset = flipEndian(dis.readShort());
        dis.readShort();
        // vertical coordinate of lower left corner of image. (if we want to use it, uncomment and remove extra read.)
//        int yOffset = flipEndian(dis.readShort());
        dis.readShort();
        // width of image - in pixels
        int width = flipEndian(dis.readShort());
        // height of image - in pixels
        int height = flipEndian(dis.readShort());
        // bits per pixel in image.
        int pixelDepth = dis.readUnsignedByte();
        int imageDescriptor = dis.readUnsignedByte();
        if ((imageDescriptor & 32) != 0) // bit 5 : if 1, flip top/bottom ordering
            flip = !flip;
        if ((imageDescriptor & 16) != 0) // bit 4 : if 1, flip left/right ordering
            flipH = !flipH;
        
        // ---------- Done Reading the TGA header ---------- //
        
        // Skip image ID
        if (idLength > 0)
            in.skip(idLength);
        
        ColorMapEntry[] cMapEntries = null;
        if (colorMapType != 0) {
            // read the color map.
            int bytesInColorMap = (cMapDepth * cMapLength) >> 3;
            int bitsPerColor = Math.min(cMapDepth/3 , 8);
            
            byte[] cMapData = new byte[bytesInColorMap];
            in.read(cMapData);
            
            // Only go to the trouble of constructing the color map
            // table if this is declared a color mapped image.
            if (imageType == TYPE_COLORMAPPED || imageType == TYPE_COLORMAPPED_RLE) {
                cMapEntries = new ColorMapEntry[cMapLength];
                int alphaSize = cMapDepth - (3*bitsPerColor);
                float scalar = 255f / (FastMath.pow(2, bitsPerColor) - 1);
                float alphaScalar = 255f / (FastMath.pow(2, alphaSize) - 1);
                for (int i = 0; i < cMapLength; i++) {
                    ColorMapEntry entry = new ColorMapEntry();
                    int offset = cMapDepth * i;
                    entry.red = (byte)(int)(getBitsAsByte(cMapData, offset, bitsPerColor) * scalar);
                    entry.green = (byte)(int)(getBitsAsByte(cMapData, offset+bitsPerColor, bitsPerColor) * scalar);
                    entry.blue = (byte)(int)(getBitsAsByte(cMapData, offset+(2*bitsPerColor), bitsPerColor) * scalar);
                    if (alphaSize <= 0)
                        entry.alpha = (byte)255;
                    else
                        entry.alpha = (byte)(int)(getBitsAsByte(cMapData, offset+(3*bitsPerColor), alphaSize) * alphaScalar);
                    
                    cMapEntries[i] = entry;
                }
            }
        }
        
        
        // Allocate image data array
        Format format;
        byte[] rawData = null;
        int dl;
        if (pixelDepth == 32) {
            rawData = new byte[width * height * 4];
            dl = 4;
        } else {
            rawData = new byte[width * height * 3];
            dl = 3;
        }
        int rawDataIndex = 0;

        if (imageType == TYPE_TRUECOLOR) {
            byte red = 0;
            byte green = 0;
            byte blue = 0;
            byte alpha = 0;
            
            // Faster than doing a 16-or-24-or-32 check on each individual pixel,
            // just make a seperate loop for each.
            if (pixelDepth == 16) {
                byte[] data = new byte[2];
                float scalar = 255f/31f;
                for (int i = 0; i <= (height - 1); i++) {
                    if (!flip)
                        rawDataIndex = (height - 1 - i) * width * dl;
                    for (int j = 0; j < width; j++) {
                        data[1] = dis.readByte();
                        data[0] = dis.readByte();
                        rawData[rawDataIndex++] = (byte)(int)(getBitsAsByte(data, 1, 5) * scalar);
                        rawData[rawDataIndex++] = (byte)(int)(getBitsAsByte(data, 6, 5) * scalar);
                        rawData[rawDataIndex++] = (byte)(int)(getBitsAsByte(data, 11, 5) * scalar);
                        if (dl == 4) {
                            // create an alpha channel
                            alpha = getBitsAsByte(data, 0, 1);
                            if (alpha == 1) alpha = (byte)255;
                            rawData[rawDataIndex++] = alpha;
                        }
                    }
                }

                format = dl == 4 ? Format.RGBA8 : Format.RGB8;
            } else if (pixelDepth == 24){
                for (int y = 0; y < height; y++) {
                    if (!flip)
                        rawDataIndex = (height - 1 - y) * width * dl;
                    else
                        rawDataIndex = y * width * dl;

                    dis.readFully(rawData, rawDataIndex, width * dl);
//                    for (int x = 0; x < width; x++) {
                        //read scanline
//                        blue = dis.readByte();
//                        green = dis.readByte();
//                        red = dis.readByte();
//                        rawData[rawDataIndex++] = red;
//                        rawData[rawDataIndex++] = green;
//                        rawData[rawDataIndex++] = blue;
//                    }
                }
                format = Format.BGR8;
            } else if (pixelDepth == 32){
                for (int i = 0; i <= (height - 1); i++) {
                    if (!flip)
                        rawDataIndex = (height - 1 - i) * width * dl;

                    for (int j = 0; j < width; j++) {
                        blue = dis.readByte();
                        green = dis.readByte();
                        red = dis.readByte();
                        alpha = dis.readByte();
                        rawData[rawDataIndex++] = red;
                        rawData[rawDataIndex++] = green;
                        rawData[rawDataIndex++] = blue;
                        rawData[rawDataIndex++] = alpha;
                    }
                }
                format = Format.RGBA8;
            }else{
                throw new IOException("Unsupported TGA true color depth: "+pixelDepth);
            }
        } else if( imageType == TYPE_TRUECOLOR_RLE ) {
            byte red = 0;
            byte green = 0;
            byte blue = 0;
            byte alpha = 0;
            // Faster than doing a 16-or-24-or-32 check on each individual pixel,
            // just make a seperate loop for each.
            if( pixelDepth == 32 ){
                for( int i = 0; i <= ( height - 1 ); ++i ){
                    if( !flip ){
                        rawDataIndex = ( height - 1 - i ) * width * dl;
                    }

                    for( int j = 0; j < width; ++j ){
                        // Get the number of pixels the next chunk covers (either packed or unpacked)
                        int count = dis.readByte();
                        if( ( count & 0x80 ) != 0 ){
                            // Its an RLE packed block - use the following 1 pixel for the next <count> pixels
                            count &= 0x07f;
                            j += count;
                            blue = dis.readByte();
                            green = dis.readByte();
                            red = dis.readByte();
                            alpha = dis.readByte();
                            while( count-- >= 0 ){
                                rawData[rawDataIndex++] = red;
                                rawData[rawDataIndex++] = green;
                                rawData[rawDataIndex++] = blue;
                                rawData[rawDataIndex++] = alpha;
                            }
                        } else{
                            // Its not RLE packed, but the next <count> pixels are raw.
                            j += count;
                            while( count-- >= 0 ){
                                blue = dis.readByte();
                                green = dis.readByte();
                                red = dis.readByte();
                                alpha = dis.readByte();
                                rawData[rawDataIndex++] = red;
                                rawData[rawDataIndex++] = green;
                                rawData[rawDataIndex++] = blue;
                                rawData[rawDataIndex++] = alpha;
                            }
                        }
                    }
                }
                format = Format.RGBA8;
            } else if( pixelDepth == 24 ){
                for( int i = 0; i <= ( height - 1 ); i++ ){
                    if( !flip ){
                        rawDataIndex = ( height - 1 - i ) * width * dl;
                    }
                    for( int j = 0; j < width; ++j ){
                        // Get the number of pixels the next chunk covers (either packed or unpacked)
                        int count = dis.readByte();
                        if( ( count & 0x80 ) != 0 ){
                            // Its an RLE packed block - use the following 1 pixel for the next <count> pixels
                            count &= 0x07f;
                            j += count;
                            blue = dis.readByte();
                            green = dis.readByte();
                            red = dis.readByte();
                            while( count-- >= 0 ){
                                rawData[rawDataIndex++] = red;
                                rawData[rawDataIndex++] = green;
                                rawData[rawDataIndex++] = blue;
                            }
                        } else{
                            // Its not RLE packed, but the next <count> pixels are raw.
                            j += count;
                            while( count-- >= 0 ){
                                blue = dis.readByte();
                                green = dis.readByte();
                                red = dis.readByte();
                                rawData[rawDataIndex++] = red;
                                rawData[rawDataIndex++] = green;
                                rawData[rawDataIndex++] = blue;
                            }
                        }
                    }
                }
                format = Format.RGB8;
            } else if( pixelDepth == 16 ){
                byte[] data = new byte[ 2 ];
                float scalar = 255f / 31f;
                for( int i = 0; i <= ( height - 1 ); i++ ){
                    if( !flip ){
                        rawDataIndex = ( height - 1 - i ) * width * dl;
                    }
                    for( int j = 0; j < width; j++ ){
                        // Get the number of pixels the next chunk covers (either packed or unpacked)
                        int count = dis.readByte();
                        if( ( count & 0x80 ) != 0 ){
                            // Its an RLE packed block - use the following 1 pixel for the next <count> pixels
                            count &= 0x07f;
                            j += count;
                            data[1] = dis.readByte();
                            data[0] = dis.readByte();
                            blue = (byte) (int) ( getBitsAsByte( data, 1, 5 ) * scalar );
                            green = (byte) (int) ( getBitsAsByte( data, 6, 5 ) * scalar );
                            red = (byte) (int) ( getBitsAsByte( data, 11, 5 ) * scalar );
                            while( count-- >= 0 ){
                                rawData[rawDataIndex++] = red;
                                rawData[rawDataIndex++] = green;
                                rawData[rawDataIndex++] = blue;
                            }
                        } else{
                            // Its not RLE packed, but the next <count> pixels are raw.
                            j += count;
                            while( count-- >= 0 ){
                                data[1] = dis.readByte();
                                data[0] = dis.readByte();
                                blue = (byte) (int) ( getBitsAsByte( data, 1, 5 ) * scalar );
                                green = (byte) (int) ( getBitsAsByte( data, 6, 5 ) * scalar );
                                red = (byte) (int) ( getBitsAsByte( data, 11, 5 ) * scalar );
                                rawData[rawDataIndex++] = red;
                                rawData[rawDataIndex++] = green;
                                rawData[rawDataIndex++] = blue;
                            }
                        }
                    }
                }
                format = Format.RGB8;
            } else{
                throw new IOException( "Unsupported TGA true color depth: " + pixelDepth );
            }

        } else if( imageType == TYPE_COLORMAPPED ){
            int bytesPerIndex = pixelDepth / 8;
            
            if (bytesPerIndex == 1) {
                for (int i = 0; i <= (height - 1); i++) {
                    if (!flip)
                        rawDataIndex = (height - 1 - i) * width * dl;
                    for (int j = 0; j < width; j++) {
                        int index = dis.readUnsignedByte();
                        if (index >= cMapEntries.length || index < 0)
                            throw new IOException("TGA: Invalid color map entry referenced: "+index);

                        ColorMapEntry entry = cMapEntries[index];
                        rawData[rawDataIndex++] = entry.red;
                        rawData[rawDataIndex++] = entry.green;
                        rawData[rawDataIndex++] = entry.blue;
                        if (dl == 4) {
                            rawData[rawDataIndex++] = entry.alpha;
                        }
    
                    }
                }
            } else if (bytesPerIndex == 2) {
                for (int i = 0; i <= (height - 1); i++) {
                    if (!flip)
                        rawDataIndex = (height - 1 - i) * width * dl;
                    for (int j = 0; j < width; j++) {
                        int index = flipEndian(dis.readShort());
                        if (index >= cMapEntries.length || index < 0)
                            throw new IOException("TGA: Invalid color map entry referenced: "+index);

                        ColorMapEntry entry = cMapEntries[index];
                        rawData[rawDataIndex++] = entry.red;
                        rawData[rawDataIndex++] = entry.green;
                        rawData[rawDataIndex++] = entry.blue;
                        if (dl == 4) {
                            rawData[rawDataIndex++] = entry.alpha;
                        }
                    }
                }
            } else {
                throw new IOException("TGA: unknown colormap indexing size used: "+bytesPerIndex);
            }

            format = dl == 4 ? Format.RGBA8 : Format.RGB8;
        } else {
            throw new IOException("Grayscale TGA not supported");
        }
        
        
        in.close();
        // Get a pointer to the image memory
        ByteBuffer scratch = BufferUtils.createByteBuffer(rawData.length);
        scratch.clear();
        scratch.put(rawData);
        scratch.rewind();
        // Create the Image object
        Image textureImage = new Image();
        textureImage.setFormat(format);
        textureImage.setWidth(width);
        textureImage.setHeight(height);
        textureImage.setData(scratch);
        return textureImage;
    }

    private static byte getBitsAsByte(byte[] data, int offset, int length) {
        int offsetBytes = offset / 8;
        int indexBits = offset % 8;
        int rVal = 0;
        
        // start at data[offsetBytes]...  spill into next byte as needed.
        for (int i = length; --i >=0;) {
            byte b = data[offsetBytes];
            int test = indexBits == 7 ? 1 : 2 << (6-indexBits);
            if ((b & test) != 0) {
                if (i == 0)
                    rVal++;
                else
                    rVal += (2 << i-1);
            }
            indexBits++;
            if (indexBits == 8) {
                indexBits = 0;
                offsetBytes++;
            }
        }
        
        return (byte)rVal;
    }

    /**
     * <code>flipEndian</code> is used to flip the endian bit of the header
     * file.
     * 
     * @param signedShort
     *            the bit to flip.
     * @return the flipped bit.
     */
    private static short flipEndian(short signedShort) {
        int input = signedShort & 0xFFFF;
        return (short) (input << 8 | (input & 0xFF00) >>> 8);
    }

    static class ColorMapEntry {
        byte red, green, blue, alpha;
        
        @Override
        public String toString() {
            return "entry: "+red+","+green+","+blue+","+alpha;
        }
    }
}

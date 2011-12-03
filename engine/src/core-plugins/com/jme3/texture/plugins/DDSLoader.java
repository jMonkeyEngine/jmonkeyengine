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
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture.Type;
import com.jme3.util.BufferUtils;
import com.jme3.util.LittleEndien;
import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * <code>DDSLoader</code> is an image loader that reads in a DirectX DDS file.
 * Supports DXT1, DXT3, DXT5, RGB, RGBA, Grayscale, Alpha pixel formats.
 * 2D images, mipmapped 2D images, and cubemaps.
 * 
 * @author Gareth Jenkins-Jones
 * @author Kirill Vainer
 * @version $Id: DDSLoader.java,v 2.0 2008/8/15
 */
public class DDSLoader implements AssetLoader {

    private static final Logger logger = Logger.getLogger(DDSLoader.class.getName());
    private static final boolean forceRGBA = false;
    private static final int DDSD_MANDATORY = 0x1007;
    private static final int DDSD_MANDATORY_DX10 = 0x6;
    private static final int DDSD_MIPMAPCOUNT = 0x20000;
    private static final int DDSD_LINEARSIZE = 0x80000;
    private static final int DDSD_DEPTH = 0x800000;
    private static final int DDPF_ALPHAPIXELS = 0x1;
    private static final int DDPF_FOURCC = 0x4;
    private static final int DDPF_RGB = 0x40;
    // used by compressonator to mark grayscale images, red channel mask is used for data and bitcount is 8
    private static final int DDPF_GRAYSCALE = 0x20000;
    // used by compressonator to mark alpha images, alpha channel mask is used for data and bitcount is 8
    private static final int DDPF_ALPHA = 0x2;
    // used by NVTextureTools to mark normal images.
    private static final int DDPF_NORMAL = 0x80000000;
    private static final int SWIZZLE_xGxR = 0x78477852;
    private static final int DDSCAPS_COMPLEX = 0x8;
    private static final int DDSCAPS_TEXTURE = 0x1000;
    private static final int DDSCAPS_MIPMAP = 0x400000;
    private static final int DDSCAPS2_CUBEMAP = 0x200;
    private static final int DDSCAPS2_VOLUME = 0x200000;
    private static final int PF_DXT1 = 0x31545844;
    private static final int PF_DXT3 = 0x33545844;
    private static final int PF_DXT5 = 0x35545844;
    private static final int PF_ATI1 = 0x31495441;
    private static final int PF_ATI2 = 0x32495441; // 0x41544932;
    private static final int PF_DX10 = 0x30315844; // a DX10 format
    private static final int DX10DIM_BUFFER = 0x1,
            DX10DIM_TEXTURE1D = 0x2,
            DX10DIM_TEXTURE2D = 0x3,
            DX10DIM_TEXTURE3D = 0x4;
    private static final int DX10MISC_GENERATE_MIPS = 0x1,
            DX10MISC_TEXTURECUBE = 0x4;
    private static final double LOG2 = Math.log(2);
    private int width;
    private int height;
    private int depth;
    private int flags;
    private int pitchOrSize;
    private int mipMapCount;
    private int caps1;
    private int caps2;
    private boolean directx10;
    private boolean compressed;
    private boolean texture3D;
    private boolean grayscaleOrAlpha;
    private boolean normal;
    private Format pixelFormat;
    private int bpp;
    private int[] sizes;
    private int redMask, greenMask, blueMask, alphaMask;
    private DataInput in;

    public DDSLoader() {
    }

    public Object load(AssetInfo info) throws IOException {
        if (!(info.getKey() instanceof TextureKey)) {
            throw new IllegalArgumentException("Texture assets must be loaded using a TextureKey");
        }

        InputStream stream = null;
        try {
            stream = info.openStream();
            in = new LittleEndien(stream);
            loadHeader();
            if (texture3D) {
                ((TextureKey) info.getKey()).setTextureTypeHint(Type.ThreeDimensional);
            } else if (depth > 1) {
                ((TextureKey) info.getKey()).setTextureTypeHint(Type.CubeMap);
            }
            ArrayList<ByteBuffer> data = readData(((TextureKey) info.getKey()).isFlipY());
            return new Image(pixelFormat, width, height, depth, data, sizes);
        } finally {
            if (stream != null){
                stream.close();
            }
        }
    }

    public Image load(InputStream stream) throws IOException {
        in = new LittleEndien(stream);
        loadHeader();
        ArrayList<ByteBuffer> data = readData(false);
        return new Image(pixelFormat, width, height, depth, data, sizes);
    }

    private void loadDX10Header() throws IOException {
        int dxgiFormat = in.readInt();
        if (dxgiFormat != 83) {
            throw new IOException("Only DXGI_FORMAT_BC5_UNORM "
                    + "is supported for DirectX10 DDS! Got: " + dxgiFormat);
        }
        pixelFormat = Format.LATC;
        bpp = 8;
        compressed = true;

        int resDim = in.readInt();
        if (resDim == DX10DIM_TEXTURE3D) {
            texture3D = true;
        }
        int miscFlag = in.readInt();
        int arraySize = in.readInt();
        if (is(miscFlag, DX10MISC_TEXTURECUBE)) {
            // mark texture as cube
            if (arraySize != 6) {
                throw new IOException("Cubemaps should consist of 6 images!");
            }
        }

        in.skipBytes(4); // skip reserved value
    }

    /**
     * Reads the header (first 128 bytes) of a DDS File
     */
    private void loadHeader() throws IOException {
        if (in.readInt() != 0x20534444 || in.readInt() != 124) {
            throw new IOException("Not a DDS file");
        }

        flags = in.readInt();

        if (!is(flags, DDSD_MANDATORY) && !is(flags, DDSD_MANDATORY_DX10)) {
            throw new IOException("Mandatory flags missing");
        }

        height = in.readInt();
        width = in.readInt();
        pitchOrSize = in.readInt();
        depth = in.readInt();
        mipMapCount = in.readInt();
        in.skipBytes(44);
        pixelFormat = null;
        directx10 = false;
        readPixelFormat();
        caps1 = in.readInt();
        caps2 = in.readInt();
        in.skipBytes(12);
        texture3D = false;

        if (!directx10) {
            if (!is(caps1, DDSCAPS_TEXTURE)) {
                throw new IOException("File is not a texture");
            }

            if (depth <= 0) {
                depth = 1;
            }

            if (is(caps2, DDSCAPS2_CUBEMAP)) {
                depth = 6; // somewhat of a hack, force loading 6 textures if a cubemap
            }

            if (is(caps2, DDSCAPS2_VOLUME)) {
                texture3D = true;
            }
        }

        int expectedMipmaps = 1 + (int) Math.ceil(Math.log(Math.max(height, width)) / LOG2);

        if (is(caps1, DDSCAPS_MIPMAP)) {
            if (!is(flags, DDSD_MIPMAPCOUNT)) {
                mipMapCount = expectedMipmaps;
            } else if (mipMapCount != expectedMipmaps) {
                // changed to warning- images often do not have the required amount,
                // or specify that they have mipmaps but include only the top level..
                logger.log(Level.WARNING, "Got {0} mipmaps, expected {1}",
                        new Object[]{mipMapCount, expectedMipmaps});
            }
        } else {
            mipMapCount = 1;
        }

        if (directx10) {
            loadDX10Header();
        }

        loadSizes();
    }

    /**
     * Reads the PixelFormat structure in a DDS file
     */
    private void readPixelFormat() throws IOException {
        int pfSize = in.readInt();
        if (pfSize != 32) {
            throw new IOException("Pixel format size is " + pfSize + ", not 32");
        }

        int pfFlags = in.readInt();
        normal = is(pfFlags, DDPF_NORMAL);

        if (is(pfFlags, DDPF_FOURCC)) {
            compressed = true;
            int fourcc = in.readInt();
            int swizzle = in.readInt();
            in.skipBytes(16);

            switch (fourcc) {
                case PF_DXT1:
                    bpp = 4;
                    if (is(pfFlags, DDPF_ALPHAPIXELS)) {
                        pixelFormat = Image.Format.DXT1A;
                    } else {
                        pixelFormat = Image.Format.DXT1;
                    }
                    break;
                case PF_DXT3:
                    bpp = 8;
                    pixelFormat = Image.Format.DXT3;
                    break;
                case PF_DXT5:
                    bpp = 8;
                    pixelFormat = Image.Format.DXT5;
                    if (swizzle == SWIZZLE_xGxR) {
                        normal = true;
                    }
                    break;
                case PF_ATI1:
                    bpp = 4;
                    pixelFormat = Image.Format.LTC;
                    break;
                case PF_ATI2:
                    bpp = 8;
                    pixelFormat = Image.Format.LATC;
                    break;
                case PF_DX10:
                    compressed = false;
                    directx10 = true;
                    // exit here, the rest of the structure is not valid
                    // the real format will be available in the DX10 header
                    return;
                default:
                    throw new IOException("Unknown fourcc: " + string(fourcc) + ", " + Integer.toHexString(fourcc));
            }

            int size = ((width + 3) / 4) * ((height + 3) / 4) * bpp * 2;

            if (is(flags, DDSD_LINEARSIZE)) {
                if (pitchOrSize == 0) {
                    logger.warning("Must use linear size with fourcc");
                    pitchOrSize = size;
                } else if (pitchOrSize != size) {
                    logger.log(Level.WARNING, "Expected size = {0}, real = {1}",
                            new Object[]{size, pitchOrSize});
                }
            } else {
                pitchOrSize = size;
            }
        } else {
            compressed = false;

            // skip fourCC
            in.readInt();

            bpp = in.readInt();
            redMask = in.readInt();
            greenMask = in.readInt();
            blueMask = in.readInt();
            alphaMask = in.readInt();

            if (is(pfFlags, DDPF_RGB)) {
                if (is(pfFlags, DDPF_ALPHAPIXELS)) {
                    pixelFormat = Format.RGBA8;
                } else {
                    pixelFormat = Format.RGB8;
                }
            } else if (is(pfFlags, DDPF_GRAYSCALE) && is(pfFlags, DDPF_ALPHAPIXELS)) {
                switch (bpp) {
                    case 16:
                        pixelFormat = Format.Luminance8Alpha8;
                        break;
                    case 32:
                        pixelFormat = Format.Luminance16Alpha16;
                        break;
                    default:
                        throw new IOException("Unsupported GrayscaleAlpha BPP: " + bpp);
                }
                grayscaleOrAlpha = true;
            } else if (is(pfFlags, DDPF_GRAYSCALE)) {
                switch (bpp) {
                    case 8:
                        pixelFormat = Format.Luminance8;
                        break;
                    case 16:
                        pixelFormat = Format.Luminance16;
                        break;
                    default:
                        throw new IOException("Unsupported Grayscale BPP: " + bpp);
                }
                grayscaleOrAlpha = true;
            } else if (is(pfFlags, DDPF_ALPHA)) {
                switch (bpp) {
                    case 8:
                        pixelFormat = Format.Alpha8;
                        break;
                    case 16:
                        pixelFormat = Format.Alpha16;
                        break;
                    default:
                        throw new IOException("Unsupported Alpha BPP: " + bpp);
                }
                grayscaleOrAlpha = true;
            } else {
                throw new IOException("Unknown PixelFormat in DDS file");
            }

            int size = (bpp / 8 * width);

            if (is(flags, DDSD_LINEARSIZE)) {
                if (pitchOrSize == 0) {
                    logger.warning("Linear size said to contain valid value but does not");
                    pitchOrSize = size;
                } else if (pitchOrSize != size) {
                    logger.log(Level.WARNING, "Expected size = {0}, real = {1}",
                            new Object[]{size, pitchOrSize});
                }
            } else {
                pitchOrSize = size;
            }
        }
    }

    /**
     * Computes the sizes of each mipmap level in bytes, and stores it in sizes_[].
     */
    private void loadSizes() {
        int mipWidth = width;
        int mipHeight = height;

        sizes = new int[mipMapCount];
        int outBpp = pixelFormat.getBitsPerPixel();
        for (int i = 0; i < mipMapCount; i++) {
            int size;
            if (compressed) {
                size = ((mipWidth + 3) / 4) * ((mipHeight + 3) / 4) * outBpp * 2;
            } else {
                size = mipWidth * mipHeight * outBpp / 8;
            }

            sizes[i] = ((size + 3) / 4) * 4;

            mipWidth = Math.max(mipWidth / 2, 1);
            mipHeight = Math.max(mipHeight / 2, 1);
        }
    }

    /**
     * Flips the given image data on the Y axis.
     * @param data Data array containing image data (without mipmaps)
     * @param scanlineSize Size of a single scanline = width * bytesPerPixel
     * @param height Height of the image in pixels
     * @return The new data flipped by the Y axis
     */
    public byte[] flipData(byte[] data, int scanlineSize, int height) {
        byte[] newData = new byte[data.length];

        for (int y = 0; y < height; y++) {
            System.arraycopy(data, y * scanlineSize,
                    newData, (height - y - 1) * scanlineSize,
                    scanlineSize);
        }

        return newData;
    }

    /**
     * Reads a grayscale image with mipmaps from the InputStream
     * @param flip Flip the loaded image by Y axis
     * @param totalSize Total size of the image in bytes including the mipmaps
     * @return A ByteBuffer containing the grayscale image data with mips.
     * @throws java.io.IOException If an error occured while reading from InputStream
     */
    public ByteBuffer readGrayscale2D(boolean flip, int totalSize) throws IOException {
        ByteBuffer buffer = BufferUtils.createByteBuffer(totalSize);

        if (bpp == 8) {
            logger.finest("Source image format: R8");
        }

        assert bpp == pixelFormat.getBitsPerPixel();

        int mipWidth = width;
        int mipHeight = height;

        for (int mip = 0; mip < mipMapCount; mip++) {
            byte[] data = new byte[sizes[mip]];
            in.readFully(data);
            if (flip) {
                data = flipData(data, mipWidth * bpp / 8, mipHeight);
            }
            buffer.put(data);

            mipWidth = Math.max(mipWidth / 2, 1);
            mipHeight = Math.max(mipHeight / 2, 1);
        }

        return buffer;
    }

    /**
     * Reads an uncompressed RGB or RGBA image.
     *
     * @param flip Flip the image on the Y axis
     * @param totalSize Size of the image in bytes including mipmaps
     * @return ByteBuffer containing image data with mipmaps in the format specified by pixelFormat_
     * @throws java.io.IOException If an error occured while reading from InputStream
     */
    public ByteBuffer readRGB2D(boolean flip, int totalSize) throws IOException {
        int redCount = count(redMask),
                blueCount = count(blueMask),
                greenCount = count(greenMask),
                alphaCount = count(alphaMask);

        if (redMask == 0x00FF0000 && greenMask == 0x0000FF00 && blueMask == 0x000000FF) {
            if (alphaMask == 0xFF000000 && bpp == 32) {
                logger.finest("Data source format: BGRA8");
            } else if (bpp == 24) {
                logger.finest("Data source format: BGR8");
            }
        }

        int sourcebytesPP = bpp / 8;
        int targetBytesPP = pixelFormat.getBitsPerPixel() / 8;

        ByteBuffer dataBuffer = BufferUtils.createByteBuffer(totalSize);

        int mipWidth = width;
        int mipHeight = height;

        int offset = 0;
        byte[] b = new byte[sourcebytesPP];
        for (int mip = 0; mip < mipMapCount; mip++) {
            for (int y = 0; y < mipHeight; y++) {
                for (int x = 0; x < mipWidth; x++) {
                    in.readFully(b);

                    int i = byte2int(b);

                    byte red = (byte) (((i & redMask) >> redCount));
                    byte green = (byte) (((i & greenMask) >> greenCount));
                    byte blue = (byte) (((i & blueMask) >> blueCount));
                    byte alpha = (byte) (((i & alphaMask) >> alphaCount));

                    if (flip) {
                        dataBuffer.position(offset + ((mipHeight - y - 1) * mipWidth + x) * targetBytesPP);
                    }
                    //else
                    //    dataBuffer.position(offset + (y * width + x) * targetBytesPP);

                    if (alphaMask == 0) {
                        dataBuffer.put(red).put(green).put(blue);
                    } else {
                        dataBuffer.put(red).put(green).put(blue).put(alpha);
                    }
                }
            }

            offset += mipWidth * mipHeight * targetBytesPP;

            mipWidth = Math.max(mipWidth / 2, 1);
            mipHeight = Math.max(mipHeight / 2, 1);
        }

        return dataBuffer;
    }

    /**
     * Reads a DXT compressed image from the InputStream
     *
     * @param totalSize Total size of the image in bytes, including mipmaps
     * @return ByteBuffer containing compressed DXT image in the format specified by pixelFormat_
     * @throws java.io.IOException If an error occured while reading from InputStream
     */
    public ByteBuffer readDXT2D(boolean flip, int totalSize) throws IOException {
        logger.finest("Source image format: DXT");

        ByteBuffer buffer = BufferUtils.createByteBuffer(totalSize);

        int mipWidth = width;
        int mipHeight = height;

        for (int mip = 0; mip < mipMapCount; mip++) {
            if (flip) {
                byte[] data = new byte[sizes[mip]];
                in.readFully(data);
                ByteBuffer wrapped = ByteBuffer.wrap(data);
                wrapped.rewind();
                ByteBuffer flipped = DXTFlipper.flipDXT(wrapped, mipWidth, mipHeight, pixelFormat);
                buffer.put(flipped);
            } else {
                byte[] data = new byte[sizes[mip]];
                in.readFully(data);
                buffer.put(data);
            }

            mipWidth = Math.max(mipWidth / 2, 1);
            mipHeight = Math.max(mipHeight / 2, 1);
        }
        buffer.rewind();

        return buffer;
    }

    /**
     * Reads a grayscale image with mipmaps from the InputStream
     * @param flip Flip the loaded image by Y axis
     * @param totalSize Total size of the image in bytes including the mipmaps
     * @return A ByteBuffer containing the grayscale image data with mips.
     * @throws java.io.IOException If an error occured while reading from InputStream
     */
    public ByteBuffer readGrayscale3D(boolean flip, int totalSize) throws IOException {
        ByteBuffer buffer = BufferUtils.createByteBuffer(totalSize * depth);

        if (bpp == 8) {
            logger.finest("Source image format: R8");
        }

        assert bpp == pixelFormat.getBitsPerPixel();


        for (int i = 0; i < depth; i++) {
            int mipWidth = width;
            int mipHeight = height;

            for (int mip = 0; mip < mipMapCount; mip++) {
                byte[] data = new byte[sizes[mip]];
                in.readFully(data);
                if (flip) {
                    data = flipData(data, mipWidth * bpp / 8, mipHeight);
                }
                buffer.put(data);

                mipWidth = Math.max(mipWidth / 2, 1);
                mipHeight = Math.max(mipHeight / 2, 1);
            }
        }
        buffer.rewind();
        return buffer;
    }

    /**
     * Reads an uncompressed RGB or RGBA image.
     *
     * @param flip Flip the image on the Y axis
     * @param totalSize Size of the image in bytes including mipmaps
     * @return ByteBuffer containing image data with mipmaps in the format specified by pixelFormat_
     * @throws java.io.IOException If an error occured while reading from InputStream
     */
    public ByteBuffer readRGB3D(boolean flip, int totalSize) throws IOException {
        int redCount = count(redMask),
                blueCount = count(blueMask),
                greenCount = count(greenMask),
                alphaCount = count(alphaMask);

        if (redMask == 0x00FF0000 && greenMask == 0x0000FF00 && blueMask == 0x000000FF) {
            if (alphaMask == 0xFF000000 && bpp == 32) {
                logger.finest("Data source format: BGRA8");
            } else if (bpp == 24) {
                logger.finest("Data source format: BGR8");
            }
        }

        int sourcebytesPP = bpp / 8;
        int targetBytesPP = pixelFormat.getBitsPerPixel() / 8;

        ByteBuffer dataBuffer = BufferUtils.createByteBuffer(totalSize * depth);

        for (int k = 0; k < depth; k++) {
            //   ByteBuffer dataBuffer = BufferUtils.createByteBuffer(totalSize);
            int mipWidth = width;
            int mipHeight = height;
            int offset = k * totalSize;
            byte[] b = new byte[sourcebytesPP];
            for (int mip = 0; mip < mipMapCount; mip++) {
                for (int y = 0; y < mipHeight; y++) {
                    for (int x = 0; x < mipWidth; x++) {
                        in.readFully(b);

                        int i = byte2int(b);

                        byte red = (byte) (((i & redMask) >> redCount));
                        byte green = (byte) (((i & greenMask) >> greenCount));
                        byte blue = (byte) (((i & blueMask) >> blueCount));
                        byte alpha = (byte) (((i & alphaMask) >> alphaCount));

                        if (flip) {
                            dataBuffer.position(offset + ((mipHeight - y - 1) * mipWidth + x) * targetBytesPP);
                        }
                        //else
                        //    dataBuffer.position(offset + (y * width + x) * targetBytesPP);

                        if (alphaMask == 0) {
                            dataBuffer.put(red).put(green).put(blue);
                        } else {
                            dataBuffer.put(red).put(green).put(blue).put(alpha);
                        }
                    }
                }

                offset += (mipWidth * mipHeight * targetBytesPP);

                mipWidth = Math.max(mipWidth / 2, 1);
                mipHeight = Math.max(mipHeight / 2, 1);
            }
        }
        dataBuffer.rewind();
        return dataBuffer;
    }

    /**
     * Reads a DXT compressed image from the InputStream
     *
     * @param totalSize Total size of the image in bytes, including mipmaps
     * @return ByteBuffer containing compressed DXT image in the format specified by pixelFormat_
     * @throws java.io.IOException If an error occured while reading from InputStream
     */
    public ByteBuffer readDXT3D(boolean flip, int totalSize) throws IOException {
        logger.finest("Source image format: DXT");

        ByteBuffer bufferAll = BufferUtils.createByteBuffer(totalSize * depth);

        for (int i = 0; i < depth; i++) {
            ByteBuffer buffer = BufferUtils.createByteBuffer(totalSize);
            int mipWidth = width;
            int mipHeight = height;
            for (int mip = 0; mip < mipMapCount; mip++) {
                if (flip) {
                    byte[] data = new byte[sizes[mip]];
                    in.readFully(data);
                    ByteBuffer wrapped = ByteBuffer.wrap(data);
                    wrapped.rewind();
                    ByteBuffer flipped = DXTFlipper.flipDXT(wrapped, mipWidth, mipHeight, pixelFormat);
                    flipped.rewind();
                    buffer.put(flipped);
                } else {
                    byte[] data = new byte[sizes[mip]];
                    in.readFully(data);
                    buffer.put(data);
                }

                mipWidth = Math.max(mipWidth / 2, 1);
                mipHeight = Math.max(mipHeight / 2, 1);
            }
            buffer.rewind();
            bufferAll.put(buffer);
        }

        return bufferAll;
    }

    /**
     * Reads the image data from the InputStream in the required format.
     * If the file contains a cubemap image, it is loaded as 6 ByteBuffers
     * (potentially containing mipmaps if they were specified), otherwise
     * a single ByteBuffer is returned for a 2D image.
     *
     * @param flip Flip the image data or not.
     *        For cubemaps, each of the cubemap faces is flipped individually.
     *        If the image is DXT compressed, no flipping is done.
     * @return An ArrayList containing a single ByteBuffer for a 2D image, or 6 ByteBuffers for a cubemap.
     *         The cubemap ByteBuffer order is PositiveX, NegativeX, PositiveY, NegativeY, PositiveZ, NegativeZ.
     *
     * @throws java.io.IOException If an error occured while reading from the stream.
     */
    public ArrayList<ByteBuffer> readData(boolean flip) throws IOException {
        int totalSize = 0;

        for (int i = 0; i < sizes.length; i++) {
            totalSize += sizes[i];
        }

        ArrayList<ByteBuffer> allMaps = new ArrayList<ByteBuffer>();
        if (depth > 1 && !texture3D) {
            for (int i = 0; i < depth; i++) {
                if (compressed) {
                    allMaps.add(readDXT2D(flip, totalSize));
                } else if (grayscaleOrAlpha) {
                    allMaps.add(readGrayscale2D(flip, totalSize));
                } else {
                    allMaps.add(readRGB2D(flip, totalSize));
                }
            }
        } else if (texture3D) {
            if (compressed) {
                allMaps.add(readDXT3D(flip, totalSize));
            } else if (grayscaleOrAlpha) {
                allMaps.add(readGrayscale3D(flip, totalSize));
            } else {
                allMaps.add(readRGB3D(flip, totalSize));
            }

        } else {
            if (compressed) {
                allMaps.add(readDXT2D(flip, totalSize));
            } else if (grayscaleOrAlpha) {
                allMaps.add(readGrayscale2D(flip, totalSize));
            } else {
                allMaps.add(readRGB2D(flip, totalSize));
            }
        }

        return allMaps;
    }

    /**
     * Checks if flags contains the specified mask
     */
    private static boolean is(int flags, int mask) {
        return (flags & mask) == mask;
    }

    /**
     * Counts the amount of bits needed to shift till bitmask n is at zero
     * @param n Bitmask to test
     */
    private static int count(int n) {
        if (n == 0) {
            return 0;
        }

        int i = 0;
        while ((n & 0x1) == 0) {
            n = n >> 1;
            i++;
            if (i > 32) {
                throw new RuntimeException(Integer.toHexString(n));
            }
        }

        return i;
    }

    /**
     * Converts a 1 to 4 sized byte array to an integer
     */
    private static int byte2int(byte[] b) {
        if (b.length == 1) {
            return b[0] & 0xFF;
        } else if (b.length == 2) {
            return (b[0] & 0xFF) | ((b[1] & 0xFF) << 8);
        } else if (b.length == 3) {
            return (b[0] & 0xFF) | ((b[1] & 0xFF) << 8) | ((b[2] & 0xFF) << 16);
        } else if (b.length == 4) {
            return (b[0] & 0xFF) | ((b[1] & 0xFF) << 8) | ((b[2] & 0xFF) << 16) | ((b[3] & 0xFF) << 24);
        } else {
            return 0;
        }
    }

    /**
     * Converts a int representing a FourCC into a String
     */
    private static String string(int value) {
        StringBuilder buf = new StringBuilder();

        buf.append((char) (value & 0xFF));
        buf.append((char) ((value & 0xFF00) >> 8));
        buf.append((char) ((value & 0xFF0000) >> 16));
        buf.append((char) ((value & 0xFF00000) >> 24));

        return buf.toString();
    }
}

/*
 * Copyright (c) 2009-2015 jMonkeyEngine
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
package com.jme3.texture.plugins.ktx;

import com.jme3.renderer.Caps;
import com.jme3.renderer.opengl.GLImageFormat;
import com.jme3.renderer.opengl.GLImageFormats;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.Texture3D;
import com.jme3.texture.TextureArray;
import com.jme3.texture.TextureCubeMap;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * This class allows one to write a KTX file.
 * It doesn't support compressed data yet.
 * 
 * @author Nehon
 */
public class KTXWriter {

    private final static Logger log = Logger.getLogger(KTXWriter.class.getName());
    
    private final String filePath;

    private final static byte[] fileIdentifier = {
        (byte) 0xAB, (byte) 0x4B, (byte) 0x54, (byte) 0x58, (byte) 0x20, (byte) 0x31, (byte) 0x31, (byte) 0xBB, (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A
    };

    /**
     * Creates a KTXWriter that will write files in the given path
     * @param path 
     */
    public KTXWriter(String path) {
        filePath = path;
    }

    /**
     * Writes a 2D image from the given image in a KTX file named from the fileName param
     * Note that the fileName should contain the extension (.ktx sounds like a wise choice)
     * @param image the image to write
     * @param fileName the name of the file to write
     */
    public void write(Image image, String fileName) {
        write(image, Texture2D.class, fileName);
    }

    /**
     * Writes an image with the given params
     * 
     * textureType, allows one to write textureArrays, Texture3D, and TextureCubeMaps.
     * Texture2D will write a 2D image.
     * Note that the fileName should contain the extension (.ktx sounds like a wise choice)
     * @param image the image to write
     * @param textureType the texture type
     * @param fileName the name of the file to write
     */
    public void write(Image image, Class<? extends Texture> textureType, String fileName) {

        FileOutputStream outs = null;
        try {
            File file = new File(filePath + "/" + fileName);            
            outs = new FileOutputStream(file);

            DataOutput out = new DataOutputStream(outs);

            //fileID
            out.write(fileIdentifier);
            //endianness
            out.writeInt(0x04030201);
            GLImageFormat format = getGlFormat(image.getFormat());
            //glType
            out.writeInt(format.dataType);
            //glTypeSize
            out.writeInt(1);
            //glFormat
            out.writeInt(format.format);
            //glInernalFormat
            out.writeInt(format.internalFormat);
            //glBaseInternalFormat
            out.writeInt(format.format);
            //pixelWidth
            out.writeInt(image.getWidth());
            //pixelHeight
            out.writeInt(image.getHeight());

            int pixelDepth = 1;
            int numberOfArrayElements = 1;
            int numberOfFaces = 1;
            if (image.getDepth() > 1) {
                //pixelDepth
                if (textureType == Texture3D.class) {
                    pixelDepth = image.getDepth();
                }
            }
            if(image.getData().size()>1){
                //numberOfArrayElements
                if (textureType == TextureArray.class) {
                    numberOfArrayElements = image.getData().size();
                }
                //numberOfFaces                
                if (textureType == TextureCubeMap.class) {
                    numberOfFaces = image.getData().size();
                }
            }
            out.writeInt(pixelDepth);
            out.writeInt(numberOfArrayElements);
            out.writeInt(numberOfFaces);

            int numberOfMipmapLevels = 1;
            //numberOfMipmapLevels
            if (image.hasMipmaps()) {
                numberOfMipmapLevels = image.getMipMapSizes().length;
            }
            out.writeInt(numberOfMipmapLevels);

            //bytesOfKeyValueData
            String keyValues = "KTXorientation\0S=r,T=u\0";
            int bytesOfKeyValueData = keyValues.length() + 4;
            int padding = 3 - ((bytesOfKeyValueData + 3) % 4);
            bytesOfKeyValueData += padding;
            out.writeInt(bytesOfKeyValueData);

            //keyAndValueByteSize
            out.writeInt(bytesOfKeyValueData - 4 - padding);
            //values
            out.writeBytes(keyValues);
            pad(padding, out);

            int offset = 0;
            //iterate over data
            for (int mipLevel = 0; mipLevel < numberOfMipmapLevels; mipLevel++) {

                int width = Math.max(1, image.getWidth() >> mipLevel);
                int height = Math.max(1, image.getHeight() >> mipLevel);
                
                int imageSize;

                if (image.hasMipmaps()) {
                    imageSize = image.getMipMapSizes()[mipLevel];
                } else {
                    imageSize = width * height * image.getFormat().getBitsPerPixel() / 8;
                }
                out.writeInt(imageSize);

                for (int arrayElem = 0; arrayElem < numberOfArrayElements; arrayElem++) {
                    for (int face = 0; face < numberOfFaces; face++) {
                        int nbPixelWritten = 0;
                        for (int depth = 0; depth < pixelDepth; depth++) {
                            ByteBuffer byteBuffer = image.getData(getSlice(face, arrayElem));
                            // BufferUtils.ensureLargeEnough(byteBuffer, imageSize);
                            log.log(Level.FINE, "position {0}", byteBuffer.position());
                            byteBuffer.position(offset);
                            byte[] b = getByteBufferArray(byteBuffer, imageSize);
                            out.write(b);

                            nbPixelWritten = b.length;
                        }
                        //cube padding
                        if (numberOfFaces == 6 && numberOfArrayElements == 0) {
                            padding = 3 - ((nbPixelWritten + 3) % 4);
                            pad(padding, out);
                        }
                    }
                }
                //mip padding
                log.log(Level.FINE, "skipping {0}", (3 - ((imageSize + 3) % 4)));
                padding = 3 - ((imageSize + 3) % 4);
                pad(padding, out);
                offset += imageSize;
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(KTXWriter.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(KTXWriter.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if(outs != null){
                    outs.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(KTXWriter.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * writes padding data to the output padding times.
     * @param padding
     * @param out
     * @throws IOException 
     */
    private void pad(int padding, DataOutput out) throws IOException {
        //padding
        for (int i = 0; i < padding; i++) {
            out.write('\0');
        }
    }

    /**
     * Get a byte array from a byte buffer.
     * @param byteBuffer the  byte buffer
     * @param size the size of the resulting array
     * @return 
     */
    private byte[] getByteBufferArray(ByteBuffer byteBuffer, int size) {
        byte[] b;
        if (byteBuffer.hasArray()) {
            b = byteBuffer.array();
        } else {
            b = new byte[size];
            byteBuffer.get(b, 0, size);
        }
        return b;
    }

    /**
     * get the glformat from JME image Format
     * @param format
     * @return 
     */
    private GLImageFormat getGlFormat(Image.Format format) {
        EnumSet<Caps> caps = EnumSet.allOf(Caps.class);
        GLImageFormat[][] formats = GLImageFormats.getFormatsForCaps(caps);
        return formats[0][format.ordinal()];
    }

    /**
     * get a slice from the face and the array index
     * @param face
     * @param arrayElem
     * @return 
     */
    private static int getSlice(int face,int arrayElem) {
        return Math.max(face,  arrayElem);
    }
}

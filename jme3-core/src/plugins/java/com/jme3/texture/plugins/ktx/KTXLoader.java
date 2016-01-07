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

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;
import com.jme3.asset.TextureKey;
import com.jme3.renderer.Caps;
import com.jme3.renderer.opengl.GLImageFormat;
import com.jme3.renderer.opengl.GLImageFormats;
import com.jme3.texture.Image;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import com.jme3.util.LittleEndien;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * A KTX file loader
 * KTX file format is an image container defined by the Kronos group
 * See specs here https://www.khronos.org/opengles/sdk/tools/KTX/file_format_spec/
 * 
 * This loader doesn't support compressed files yet.
 * 
 * @author Nehon
 */
public class KTXLoader implements AssetLoader {
    
    private final static Logger log = Logger.getLogger(KTXLoader.class.getName());
 
    private final static byte[] fileIdentifier = {
        (byte) 0xAB, (byte) 0x4B, (byte) 0x54, (byte) 0x58, (byte) 0x20, (byte) 0x31, (byte) 0x31, (byte) 0xBB, (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A
    };
    private boolean slicesInside = false;

    @Override
    public Object load(AssetInfo info) throws IOException {
        if (!(info.getKey() instanceof TextureKey)) {
            throw new IllegalArgumentException("Texture assets must be loaded using a TextureKey");
        }

        InputStream in = null;
        try {
            in = info.openStream();
            Image img = load(in);
            return img;
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    private Image load(InputStream stream) {

        byte[] fileId = new byte[12];

        DataInput in = new DataInputStream(stream);
        try {
            stream.read(fileId, 0, 12);
            if (!checkFileIdentifier(fileId)) {
                throw new IllegalArgumentException("Unrecognized ktx file identifier : " + new String(fileId) + " should be " + new String(fileIdentifier));
            }

            int endianness = in.readInt();
            //opposite endianness
            if (endianness == 0x01020304) {
                in = new LittleEndien(stream);
            }
            int glType = in.readInt();
            int glTypeSize = in.readInt();
            int glFormat = in.readInt();
            int glInternalFormat = in.readInt();
            int glBaseInternalFormat = in.readInt();
            int pixelWidth = in.readInt();
            int pixelHeight = in.readInt();
            int pixelDepth = in.readInt();
            int numberOfArrayElements = in.readInt();
            int numberOfFaces = in.readInt();
            int numberOfMipmapLevels = in.readInt();
            int bytesOfKeyValueData = in.readInt();

            log.log(Level.FINE, "glType = {0}", glType);
            log.log(Level.FINE, "glTypeSize = {0}", glTypeSize);
            log.log(Level.FINE, "glFormat = {0}", glFormat);
            log.log(Level.FINE, "glInternalFormat = {0}", glInternalFormat);
            log.log(Level.FINE, "glBaseInternalFormat = {0}", glBaseInternalFormat);
            log.log(Level.FINE, "pixelWidth = {0}", pixelWidth);
            log.log(Level.FINE, "pixelHeight = {0}", pixelHeight);
            log.log(Level.FINE, "pixelDepth = {0}", pixelDepth);
            log.log(Level.FINE, "numberOfArrayElements = {0}", numberOfArrayElements);
            log.log(Level.FINE, "numberOfFaces = {0}", numberOfFaces);
            log.log(Level.FINE, "numberOfMipmapLevels = {0}", numberOfMipmapLevels);
            log.log(Level.FINE, "bytesOfKeyValueData = {0}", bytesOfKeyValueData);
            
            if((numberOfFaces >1 && pixelDepth >1) || (numberOfFaces >1 && numberOfArrayElements >1) ||  (pixelDepth >1 && numberOfArrayElements >1)){
                throw new UnsupportedOperationException("jME doesn't support cube maps of 3D textures or arrays of 3D texture or arrays of cube map of 3d textures");
            }
            

            PixelReader pixelReader = parseMetaData(bytesOfKeyValueData, in);
            if (pixelReader == null){
                pixelReader = new SrTuRoPixelReader(); 
            }
            
            //some of the values may be 0 we need them at least to be 1
            pixelDepth = Math.max(1, pixelDepth);
            numberOfArrayElements = Math.max(1, numberOfArrayElements);
            numberOfFaces = Math.max(1, numberOfFaces);
            numberOfMipmapLevels = Math.max(1, numberOfMipmapLevels);
            
            int nbSlices = Math.max(numberOfFaces,numberOfArrayElements);

            Image.Format imgFormat = getImageFormat(glFormat, glInternalFormat, glType);
            log.log(Level.FINE, "img format {0}", imgFormat.toString());
            
           
            int bytePerPixel = imgFormat.getBitsPerPixel() / 8;            
            int byteBuffersSize = computeBuffersSize(numberOfMipmapLevels, pixelWidth, pixelHeight, bytePerPixel, pixelDepth);
            log.log(Level.FINE, "data size {0}", byteBuffersSize);
            
            int[] mipMapSizes = new int[numberOfMipmapLevels];
            
            Image image = createImage(nbSlices, byteBuffersSize, imgFormat, pixelWidth, pixelHeight, pixelDepth);
            
            byte[] pixelData = new byte[bytePerPixel];            
            
            int offset = 0;
            //iterate over data
            for (int mipLevel = 0; mipLevel < numberOfMipmapLevels; mipLevel++) {
                //size of the image in byte.
                //this value is bogus in many example, when using mipmaps.
                //instead we compute the theorical size and display a warning when it does not match.
                int fileImageSize = in.readInt();
                
                int width = Math.max(1, pixelWidth >> mipLevel);
                int height = Math.max(1, pixelHeight >> mipLevel);
               
                int imageSize = width * height * bytePerPixel;
                mipMapSizes[mipLevel] = imageSize;
                log.log(Level.FINE, "current mip size {0}", imageSize);
                if(fileImageSize != imageSize){
                    log.log(Level.WARNING, "Mip map size is wrong in the file for mip level {0} size is {1} should be {2}", new Object[]{mipLevel, fileImageSize, imageSize});
                }
                
                for (int arrayElem = 0; arrayElem < numberOfArrayElements; arrayElem++) {
                    for (int face = 0; face < numberOfFaces; face++) {
                        int nbPixelRead = 0;
                        for (int depth = 0; depth < pixelDepth; depth++) {
                            ByteBuffer byteBuffer = image.getData(getSlice(face, arrayElem));    

                            log.log(Level.FINE, "position {0}", byteBuffer.position());
                            byteBuffer.position(offset);                                                        
                            nbPixelRead = pixelReader.readPixels(width, height, pixelData, byteBuffer, in);
                        }
                        //cube padding
                        if (numberOfFaces == 6 && numberOfArrayElements == 0) {
                            in.skipBytes(3 - ((nbPixelRead + 3) % 4));
                        }
                    }
                }
                //mip padding
                log.log(Level.FINE, "skipping {0}", (3 - ((imageSize + 3) % 4)));
                in.skipBytes(3 - ((imageSize + 3) % 4));
                offset+=imageSize;
            }
            //there are loaded mip maps we set the sizes
            if(numberOfMipmapLevels >1){
                image.setMipMapSizes(mipMapSizes);
            }
            //if 3D texture and slices' orientation is inside, we reverse the data array.
            if(pixelDepth > 1 && slicesInside){
                Collections.reverse(image.getData());
            }
            return image;

        } catch (IOException ex) {
            Logger.getLogger(KTXLoader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * returns the slice from the face and the array index
     * @param face the face
     * @param arrayElem the array index
     * @return 
     */
    private static int getSlice(int face, int arrayElem) {
        return Math.max(face, arrayElem);
    }

    /**
     * Computes a buffer size from given parameters
     * @param numberOfMipmapLevels 
     * @param pixelWidth
     * @param pixelHeight
     * @param bytePerPixel
     * @param pixelDepth
     * @return 
     */
    private int computeBuffersSize(int numberOfMipmapLevels, int pixelWidth, int pixelHeight, int bytePerPixel, int pixelDepth) {
        int byteBuffersSize = 0;
        for (int mipLevel = 0; mipLevel < numberOfMipmapLevels; mipLevel++) {
            int width = Math.max(1, pixelWidth >> mipLevel);
            int height = Math.max(1, pixelHeight >> mipLevel);
            byteBuffersSize += width * height * bytePerPixel;
            log.log(Level.FINE, "mip level size : {0} : {1}", new Object[]{mipLevel, width * height * bytePerPixel});
        }
        return byteBuffersSize * pixelDepth;
    }

    /**
     * Create an image with given parameters
     * @param nbSlices
     * @param byteBuffersSize
     * @param imgFormat
     * @param pixelWidth
     * @param pixelHeight
     * @param depth
     * @return 
     */
    private Image createImage(int nbSlices, int byteBuffersSize, Image.Format imgFormat, int pixelWidth, int pixelHeight, int depth) {
        ArrayList<ByteBuffer> imageData = new ArrayList<ByteBuffer>(nbSlices);
        for (int i = 0; i < nbSlices; i++) {
            imageData.add(BufferUtils.createByteBuffer(byteBuffersSize));
        }
        Image image = new Image(imgFormat, pixelWidth, pixelHeight, depth, imageData, ColorSpace.sRGB);
        return image;
    }

    /**
     * Parse the file metaData to select the PixelReader that suits the file 
     * coordinates orientation
     * @param bytesOfKeyValueData
     * @param in
     * @return
     * @throws IOException 
     */
    private PixelReader parseMetaData(int bytesOfKeyValueData, DataInput in) throws IOException {
        PixelReader pixelReader = null;
        for (int i = 0; i < bytesOfKeyValueData;) {
            //reading key values
            int keyAndValueByteSize = in.readInt();            
            byte[] keyValue = new byte[keyAndValueByteSize];
            in.readFully(keyValue);
            
            
            //parsing key values
            String[] kv = new String(keyValue).split("\0");            
            for (int j = 0; j < kv.length; j += 2) {
                System.err.println("key : " + kv[j]);
                System.err.println("value : " + kv[j + 1]);
                if(kv[j].equalsIgnoreCase("KTXorientation")){
                    if(kv[j + 1].startsWith("S=r,T=d") ){
                        pixelReader = new SrTdRiPixelReader();
                    }else{
                        pixelReader = new SrTuRoPixelReader();
                    }
                    if(kv[j + 1].contains("R=i")){
                        slicesInside = true;
                    }
                }
            }
            
            //padding
            int padding = 3 - ((keyAndValueByteSize + 3) % 4);            
            if (padding > 0) {
                in.skipBytes(padding);
            }
            i += 4 + keyAndValueByteSize + padding;
        }
        return pixelReader;
    }

    /**
     * Chacks the file id
     * @param b
     * @return 
     */
    private boolean checkFileIdentifier(byte[] b) {
        boolean check = true;
        for (int i = 0; i < 12; i++) {
            if (b[i] != fileIdentifier[i]) {
                check = false;
            }
        }
        return check;
    }

    /**
     * returns the JME image format from gl formats and types.
     * @param glFormat
     * @param glInternalFormat
     * @param glType
     * @return 
     */
    private Image.Format getImageFormat(int glFormat, int glInternalFormat, int glType) {
        EnumSet<Caps> caps = EnumSet.allOf(Caps.class);
        GLImageFormat[][] formats = GLImageFormats.getFormatsForCaps(caps);
        for (GLImageFormat[] format : formats) {
            for (int j = 0; j < format.length; j++) {
                GLImageFormat glImgFormat = format[j];
                if (glImgFormat != null) {
                    if (glImgFormat.format == glFormat && glImgFormat.dataType == glType) {
                        if (glFormat == glInternalFormat || glImgFormat.internalFormat == glInternalFormat) {
                            return Image.Format.values()[j];
                        }
                    }
                }
            }
        }
        return null;
    }

}

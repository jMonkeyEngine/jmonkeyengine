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
import com.jme3.util.BufferUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Logger;

public class PFMLoader implements AssetLoader {

    private static final Logger logger = Logger.getLogger(PFMLoader.class.getName());

    private String readString(InputStream is) throws IOException{
        StringBuilder sb = new StringBuilder();
        while (true){
            int i = is.read();
            if (i == 0x0a || i == -1) // new line or EOF
                return sb.toString();

            sb.append((char)i);
        }
    }

    private void flipScanline(byte[] scanline){
        for (int i = 0; i < scanline.length; i+=4){
            // flip first and fourth bytes
            byte tmp = scanline[i+3];
            scanline[i+3] = scanline[i+0];
            scanline[i+0] = tmp;

            // flip second and third bytes
            tmp = scanline[i+2];
            scanline[i+2] = scanline[i+1];
            scanline[i+1] = tmp;
        }
    }
    
    private Image load(InputStream in, boolean needYFlip) throws IOException{
        Format format = null;

        String fmtStr = readString(in);
        if (fmtStr.equals("PF")){
            format = Format.RGB32F;
        }else if (fmtStr.equals("Pf")){
            format = Format.Luminance32F;
        }else{
            throw new IOException("File is not PFM format");
        }

        String sizeStr = readString(in);
        int spaceIdx = sizeStr.indexOf(" ");
        if (spaceIdx <= 0 || spaceIdx >= sizeStr.length() - 1)
            throw new IOException("Invalid size syntax in PFM file");

        int width = Integer.parseInt(sizeStr.substring(0,spaceIdx));
        int height = Integer.parseInt(sizeStr.substring(spaceIdx+1));

        if (width <= 0 || height <= 0)
            throw new IOException("Invalid size specified in PFM file");
        
        String scaleStr = readString(in);
        float scale = Float.parseFloat(scaleStr);
        ByteOrder order = scale < 0 ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
        boolean needEndienFlip = order != ByteOrder.nativeOrder();

        // make sure all unneccessary stuff gets deleted from heap
        // before allocating large amount of memory
        System.gc();

        int bytesPerPixel = format.getBitsPerPixel() / 8;
        int scanLineBytes = bytesPerPixel * width;

        ByteBuffer imageData = BufferUtils.createByteBuffer(width * height * bytesPerPixel);
        byte[] scanline = new byte[width * bytesPerPixel];

        for (int y = height - 1; y >= 0; y--) {
            if (!needYFlip)
                imageData.position(scanLineBytes * y);

            int read = 0;
            int off = 0;
            do {
                read = in.read(scanline, off, scanline.length - off);
                off += read;
            } while (read > 0);

            if (needEndienFlip){
                flipScanline(scanline);
            }

            imageData.put(scanline);
        }
        imageData.rewind();

        return new Image(format, width, height, imageData);
    }

    public Object load(AssetInfo info) throws IOException {
        if (!(info.getKey() instanceof TextureKey))
            throw new IllegalArgumentException("Texture assets must be loaded using a TextureKey");

        InputStream in = null;
        try {
            in = info.openStream();
            return load(in, ((TextureKey)info.getKey()).isFlipY());
        } finally {
            if (in != null){
                in.close();
            }
        }
        
    }

}

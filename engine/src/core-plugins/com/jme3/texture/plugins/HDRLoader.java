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
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HDRLoader implements AssetLoader {

    private static final Logger logger = Logger.getLogger(HDRLoader.class.getName());

    private boolean writeRGBE = false;
    private ByteBuffer rleTempBuffer;
    private ByteBuffer dataStore;
    private final float[] tempF = new float[3];

    public HDRLoader(boolean writeRGBE){
        this.writeRGBE = writeRGBE;
    }

    public HDRLoader(){
    }
    
    public static void convertFloatToRGBE(byte[] rgbe, float red, float green, float blue){
        double max = red;
        if (green > max) max = green;
        if (blue > max) max = blue;
        if (max < 1.0e-32){
            rgbe[0] = rgbe[1] = rgbe[2] = rgbe[3] = 0;
        }else{
            double exp = Math.ceil( Math.log10(max) / Math.log10(2) );
            double divider = Math.pow(2.0, exp);
            rgbe[0] = (byte) ((red   / divider) * 255.0);
            rgbe[1] = (byte) ((green / divider) * 255.0);
            rgbe[2] = (byte) ((blue  / divider) * 255.0);
            rgbe[3] = (byte) (exp + 128.0);
      }
    }

    public static void convertRGBEtoFloat(byte[] rgbe, float[] rgbf){
        int R = rgbe[0] & 0xFF, 
            G = rgbe[1] & 0xFF,
            B = rgbe[2] & 0xFF, 
            E = rgbe[3] & 0xFF;
        
        float e = (float) Math.pow(2f, E - (128 + 8) );
        rgbf[0] = R * e;
        rgbf[1] = G * e;
        rgbf[2] = B * e;
    }

    public static void convertRGBEtoFloat2(byte[] rgbe, float[] rgbf){
        int R = rgbe[0] & 0xFF,
            G = rgbe[1] & 0xFF,
            B = rgbe[2] & 0xFF,
            E = rgbe[3] & 0xFF;

        float e = (float) Math.pow(2f, E - 128);
        rgbf[0] = (R / 256.0f) * e;
        rgbf[1] = (G / 256.0f) * e;
        rgbf[2] = (B / 256.0f) * e;
    }

    public static void convertRGBEtoFloat3(byte[] rgbe, float[] rgbf){
        int R = rgbe[0] & 0xFF,
            G = rgbe[1] & 0xFF,
            B = rgbe[2] & 0xFF,
            E = rgbe[3] & 0xFF;

        float e = (float) Math.pow(2f, E - (128 + 8) );
        rgbf[0] = R * e;
        rgbf[1] = G * e;
        rgbf[2] = B * e;
    }

    private short flip(int in){
        return (short) ((in << 8 & 0xFF00) | (in >> 8));
    }
    
    private void writeRGBE(byte[] rgbe){
        if (writeRGBE){
            dataStore.put(rgbe);
        }else{
            convertRGBEtoFloat(rgbe, tempF);
            dataStore.putShort(FastMath.convertFloatToHalf(tempF[0]))
                     .putShort(FastMath.convertFloatToHalf(tempF[1])).
                      putShort(FastMath.convertFloatToHalf(tempF[2]));
        }
    }
    
    private String readString(InputStream is) throws IOException{
        StringBuilder sb = new StringBuilder();
        while (true){
            int i = is.read();
            if (i == 0x0a || i == -1) // new line or EOF
                return sb.toString();
            
            sb.append((char)i);
        }
    }
    
    private boolean decodeScanlineRLE(InputStream in, int width) throws IOException{
        // must deocde RLE data into temp buffer before converting to float
        if (rleTempBuffer == null){
            rleTempBuffer = BufferUtils.createByteBuffer(width * 4);
        }else{
            rleTempBuffer.clear();
            if (rleTempBuffer.remaining() < width * 4)
                rleTempBuffer = BufferUtils.createByteBuffer(width * 4);
        }
        
	// read each component seperately
        for (int i = 0; i < 4; i++) {
            // read WIDTH bytes for the channel
            for (int j = 0; j < width;) {
                int code = in.read();
                if (code > 128) { // run
                    code -= 128;
                    int val = in.read();
                    while ((code--) != 0) {
                        rleTempBuffer.put( (j++) * 4 + i , (byte)val);
                        //scanline[j++][i] = val;
                    }
                } else {	// non-run
                    while ((code--) != 0) {
                        int val = in.read();
                        rleTempBuffer.put( (j++) * 4 + i, (byte)val);
                        //scanline[j++][i] = in.read();
                    }
                }
            }
        }
        
        rleTempBuffer.rewind();
        byte[] rgbe = new byte[4];
//        float[] temp = new float[3];
            
        // decode temp buffer into float data
        for (int i = 0; i < width; i++){
            rleTempBuffer.get(rgbe);
            writeRGBE(rgbe);
        }
        
        return true;
    }
    
    private boolean decodeScanlineUncompressed(InputStream in, int width) throws IOException{
        byte[] rgbe = new byte[4];
        
        for (int i = 0; i < width; i+=3){
            if (in.read(rgbe) < 1)
                return false;

            writeRGBE(rgbe);
        }
        return true;
    }
    
    private void decodeScanline(InputStream in, int width) throws IOException{
        if (width < 8 || width > 0x7fff){
            // too short/long for RLE compression
            decodeScanlineUncompressed(in, width);
        }
        
        // check format
        byte[] data = new byte[4];
        in.read(data);
        if (data[0] != 0x02 || data[1] != 0x02 || (data[2] & 0x80) != 0){
            // not RLE data
            decodeScanlineUncompressed(in, width-1);
        }else{
            // check scanline width
            int readWidth = (data[2] & 0xFF) << 8 | (data[3] & 0xFF);
            if (readWidth != width)
                throw new IOException("Illegal scanline width in HDR file: "+width+" != "+readWidth);
            
            // RLE data
            decodeScanlineRLE(in, width);
        }
    }

    public Image load(InputStream in, boolean flipY) throws IOException{
        float gamma = -1f;
        float exposure = -1f;
        float[] colorcorr = new float[]{ -1f, -1f, -1f };

        int width = -1, height = -1;
        boolean verifiedFormat = false;

        while (true){
            String ln = readString(in);
            ln = ln.trim();
            if (ln.startsWith("#") || ln.equals("")){
                if (ln.equals("#?RADIANCE") || ln.equals("#?RGBE"))
                    verifiedFormat = true;

                continue; // comment or empty statement
            } else if (ln.startsWith("+") || ln.startsWith("-")){
                // + or - mark image resolution and start of data
                String[] resData = ln.split("\\s");
                if (resData.length != 4){
                    throw new IOException("Invalid resolution string in HDR file");
                }

                if (!resData[0].equals("-Y") || !resData[2].equals("+X")){
                    logger.warning("Flipping/Rotating attributes ignored!");
                }

                //if (resData[0].endsWith("X")){
                    // first width then height
                //    width = Integer.parseInt(resData[1]);
                //    height = Integer.parseInt(resData[3]);
                //}else{
                    width = Integer.parseInt(resData[3]);
                    height = Integer.parseInt(resData[1]);
                //}

                break;
            } else {
                // regular command
                int index = ln.indexOf("=");
                if (index < 1){
                    logger.log(Level.FINE, "Ignored string: {0}", ln);
                    continue;
                }

                String var = ln.substring(0, index).trim().toLowerCase();
                String value = ln.substring(index+1).trim().toLowerCase();
                if (var.equals("format")){
                    if (!value.equals("32-bit_rle_rgbe") && !value.equals("32-bit_rle_xyze")){
                        throw new IOException("Unsupported format in HDR picture");
                    }
                }else if (var.equals("exposure")){
                    exposure = Float.parseFloat(value);
                }else if (var.equals("gamma")){
                    gamma = Float.parseFloat(value);
                }else{
                    logger.log(Level.WARNING, "HDR Command ignored: {0}", ln);
                }
            }
        }

        assert width != -1 && height != -1;

        if (!verifiedFormat)
            logger.warning("Unsure if specified image is Radiance HDR");

        // some HDR images can get pretty big
        System.gc();

        // each pixel times size of component times # of components
        Format pixelFormat;
        if (writeRGBE){
            pixelFormat = Format.RGBA8;
        }else{
            pixelFormat = Format.RGB16F;
        }

        dataStore = BufferUtils.createByteBuffer(width * height * pixelFormat.getBitsPerPixel());

        int bytesPerPixel = pixelFormat.getBitsPerPixel() / 8;
        int scanLineBytes = bytesPerPixel * width;
        for (int y = height - 1; y >= 0; y--) {
            if (flipY)
                dataStore.position(scanLineBytes * y);

            decodeScanline(in, width);
        }
        in.close();

        dataStore.rewind();
        return new Image(pixelFormat, width, height, dataStore);
    }

    public Object load(AssetInfo info) throws IOException {
        if (!(info.getKey() instanceof TextureKey))
            throw new IllegalArgumentException("Texture assets must be loaded using a TextureKey");

        boolean flip = ((TextureKey) info.getKey()).isFlipY();
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

}

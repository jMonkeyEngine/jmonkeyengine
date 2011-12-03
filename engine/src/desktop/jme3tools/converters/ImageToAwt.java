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

package jme3tools.converters;

import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.plugins.AWTLoader;
import com.jme3.util.BufferUtils;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.EnumMap;

public class ImageToAwt {

    private static final EnumMap<Format, DecodeParams> params
            = new EnumMap<Format, DecodeParams>(Format.class);

    private static class DecodeParams {

        final int bpp, am, rm, gm, bm, as, rs, gs, bs, im, is;

        public DecodeParams(int bpp, int am, int rm, int gm, int bm, int as, int rs, int gs, int bs, int im, int is) {
            this.bpp = bpp;
            this.am = am;
            this.rm = rm;
            this.gm = gm;
            this.bm = bm;
            this.as = as;
            this.rs = rs;
            this.gs = gs;
            this.bs = bs;
            this.im = im;
            this.is = is;
        }

        public DecodeParams(int bpp, int rm, int rs, int im, int is, boolean alpha){
            this.bpp = bpp;
            if (alpha){
                this.am = rm;
                this.as = rs;
                this.rm = 0;
                this.rs = 0;
            }else{
                this.rm = rm;
                this.rs = rs;
                this.am = 0;
                this.as = 0;
            }
            
            this.gm = 0;
            this.bm = 0;
            this.gs = 0;
            this.bs = 0;
            this.im = im;
            this.is = is;
        }

        public DecodeParams(int bpp, int rm, int rs, int im, int is){
            this(bpp, rm, rs, im, is, false);
        }
    }

    static {
        final int mx___ = 0xff000000;
        final int m_x__ = 0x00ff0000;
        final int m__x_ = 0x0000ff00;
        final int m___x = 0x000000ff;
        final int sx___ = 24;
        final int s_x__ = 16;
        final int s__x_ = 8;
        final int s___x = 0;
        final int mxxxx = 0xffffffff;
        final int sxxxx = 0;

        final int m4x___ = 0xf000;
        final int m4_x__ = 0x0f00;
        final int m4__x_ = 0x00f0;
        final int m4___x = 0x000f;
        final int s4x___ = 12;
        final int s4_x__ = 8;
        final int s4__x_ = 4;
        final int s4___x = 0;

        final int m5___  = 0xf800;
        final int m_5__  = 0x07c0;
        final int m__5_  = 0x003e;
        final int m___1  = 0x0001;

        final int s5___  = 11;
        final int s_5__  = 6;
        final int s__5_  = 1;
        final int s___1  = 0;

        final int m5__   = 0xf800;
        final int m_6_   = 0x07e0;
        final int m__5   = 0x001f;

        final int s5__   = 11;
        final int s_6_   = 5;
        final int s__5   = 0;

        final int mxx__  = 0xffff0000;
        final int sxx__  = 32;
        final int m__xx  = 0x0000ffff;
        final int s__xx  = 0;

        // note: compressed, depth, or floating point formats not included here..
        
        params.put(Format.ABGR8,    new DecodeParams(4, mx___, m___x, m__x_, m_x__,
                                                        sx___, s___x, s__x_, s_x__,
                                                        mxxxx, sxxxx));
        params.put(Format.ARGB4444, new DecodeParams(2, m4x___, m4_x__, m4__x_, m4___x,
                                                        s4x___, s4_x__, s4__x_, s4___x,
                                                        mxxxx, sxxxx));
        params.put(Format.Alpha16,  new DecodeParams(2, mxxxx, sxxxx, mxxxx, sxxxx, true));
        params.put(Format.Alpha8,   new DecodeParams(1, mxxxx, sxxxx, mxxxx, sxxxx, true));
        params.put(Format.BGR8,     new DecodeParams(3, 0,     m___x, m__x_, m_x__,
                                                        0,     s___x, s__x_, s_x__,
                                                        mxxxx, sxxxx));
        params.put(Format.Luminance16, new DecodeParams(2, mxxxx, sxxxx, mxxxx, sxxxx, false));
        params.put(Format.Luminance8,  new DecodeParams(1, mxxxx, sxxxx, mxxxx, sxxxx, false));
        params.put(Format.Luminance16Alpha16, new DecodeParams(4, m__xx, mxx__, 0, 0,
                                                                  s__xx, sxx__, 0, 0,
                                                                  mxxxx, sxxxx));
        params.put(Format.Luminance16F, new DecodeParams(2, mxxxx, sxxxx, mxxxx, sxxxx, false));
        params.put(Format.Luminance16FAlpha16F, new DecodeParams(4, m__xx, mxx__, 0, 0,
                                                                    s__xx, sxx__, 0, 0,
                                                                    mxxxx, sxxxx));
        params.put(Format.Luminance32F, new DecodeParams(4, mxxxx, sxxxx, mxxxx, sxxxx, false));
        params.put(Format.Luminance8,   new DecodeParams(1, mxxxx, sxxxx, mxxxx, sxxxx, false));
        params.put(Format.RGB5A1,       new DecodeParams(2, m___1, m5___, m_5__, m__5_,
                                                            s___1, s5___, s_5__, s__5_,
                                                            mxxxx, sxxxx));
        params.put(Format.RGB565,       new DecodeParams(2, 0,     m5__ , m_6_ , m__5,
                                                            0,     s5__ , s_6_ , s__5,
                                                            mxxxx, sxxxx));
        params.put(Format.RGB8,         new DecodeParams(3, 0,     m_x__, m__x_, m___x,
                                                            0,     s_x__, s__x_, s___x,
                                                            mxxxx, sxxxx));
        params.put(Format.RGBA8,        new DecodeParams(4, m___x, mx___, m_x__, m__x_,
                                                            s___x, sx___, s_x__, s__x_,
                                                            mxxxx, sxxxx));
    }

    private static int Ix(int x, int y, int w){
        return y * w + x;
    }

    private static int readPixel(ByteBuffer buf, int idx, int bpp){
        buf.position(idx);
        int original = buf.get() & 0xff;
        while ((--bpp) > 0){
            original = (original << 8) | (buf.get() & 0xff);
        }
        return original;
    }

    private static void writePixel(ByteBuffer buf, int idx, int pixel, int bpp){
        buf.position(idx);
        while ((--bpp) >= 0){
//            pixel = pixel >> 8;
            byte bt = (byte) ((pixel >> (bpp * 8)) & 0xff);
//            buf.put( (byte) (pixel & 0xff) );
            buf.put(bt);
        }
    }


    /**
     * Convert an AWT image to jME image.
     */
    public static void convert(BufferedImage image, Format format, ByteBuffer buf){
        DecodeParams p = params.get(format);
        if (p == null)
            throw new UnsupportedOperationException("Image format " + format + " is not supported");

        int width = image.getWidth();
        int height = image.getHeight();

        boolean alpha = true;
        boolean luminance = false;

        int reductionA = 8 - Integer.bitCount(p.am);
        int reductionR = 8 - Integer.bitCount(p.rm);
        int reductionG = 8 - Integer.bitCount(p.gm);
        int reductionB = 8 - Integer.bitCount(p.bm);

        int initialPos = buf.position();
        for (int y = 0; y < height; y++){
            for (int x = 0; x < width; x++){
                // Get ARGB
                int argb = image.getRGB(x, y);

                // Extract color components
                int a = (argb & 0xff000000) >> 24;
                int r = (argb & 0x00ff0000) >> 16;
                int g = (argb & 0x0000ff00) >> 8;
                int b = (argb & 0x000000ff);

                // Remove anything after 8 bits
                a = a & 0xff;
                r = r & 0xff;
                g = g & 0xff;
                b = b & 0xff;

                // Set full alpha if target image has no alpha
                if (!alpha)
                    a = 0xff;

                // Convert color to luminance if target
                // image is in luminance format
                if (luminance){
                    // convert RGB to luminance
                }

                // Do bit reduction, assumes proper rounding has already been
                // done.
                a = a >> reductionA;
                r = r >> reductionR;
                g = g >> reductionG;
                b = b >> reductionB;
                
                // Put components into appropriate positions
                a = (a << p.as) & p.am;
                r = (r << p.rs) & p.rm;
                g = (g << p.gs) & p.gm;
                b = (b << p.bs) & p.bm;

                int outputPixel = ((a | r | g | b) << p.is) & p.im;
                int i = initialPos + (Ix(x,y,width) * p.bpp);
                writePixel(buf, i, outputPixel, p.bpp);
            }
        }
    }

    private static final double LOG2 = Math.log(2);

    public static void createData(Image image, boolean mipmaps){
        int bpp = image.getFormat().getBitsPerPixel();
        int w = image.getWidth();
        int h = image.getHeight();
        if (!mipmaps){
            image.setData(BufferUtils.createByteBuffer(w*h*bpp/8));
            return;
        }
        int expectedMipmaps = 1 + (int) Math.ceil(Math.log(Math.max(h, w)) / LOG2);
        int[] mipMapSizes = new int[expectedMipmaps];
        int total = 0;
        for (int i = 0; i < mipMapSizes.length; i++){
            int size = (w * h * bpp) / 8;
            total += size;
            mipMapSizes[i] = size;
            w /= 2;
            h /= 2;
        }
        image.setMipMapSizes(mipMapSizes);
        image.setData(BufferUtils.createByteBuffer(total));
    }

    /**
     * Convert the image from the given format to the output format.
     * It is assumed that both images have buffers with the appropriate
     * number of elements and that both have the same dimensions.
     *
     * @param input
     * @param output
     */
    public static void convert(Image input, Image output){
        DecodeParams inParams  = params.get(input.getFormat());
        DecodeParams outParams = params.get(output.getFormat());

        if (inParams == null || outParams == null)
            throw new UnsupportedOperationException();

        int width  = input.getWidth();
        int height = input.getHeight();

        if (width != output.getWidth() || height != output.getHeight())
            throw new IllegalArgumentException();

        ByteBuffer inData = input.getData(0);

        boolean inAlpha = false;
        boolean inLum = false;
        boolean inRGB = false;
        if (inParams.am != 0) {
            inAlpha = true;
        }

        if (inParams.rm != 0 && inParams.gm == 0 && inParams.bm == 0) {
            inLum = true;
        } else if (inParams.rm != 0 && inParams.gm != 0 && inParams.bm != 0) {
            inRGB = true;
        }

        int expansionA = 8 - Integer.bitCount(inParams.am);
        int expansionR = 8 - Integer.bitCount(inParams.rm);
        int expansionG = 8 - Integer.bitCount(inParams.gm);
        int expansionB = 8 - Integer.bitCount(inParams.bm);

        int inputPixel;
        for (int y = 0; y < height; y++){
            for (int x = 0; x < width; x++){
                int i = Ix(x, y, width) * inParams.bpp;
                inputPixel = (readPixel(inData, i, inParams.bpp) & inParams.im) >> inParams.is;
                
                int a = (inputPixel & inParams.am) >> inParams.as;
                int r = (inputPixel & inParams.rm) >> inParams.rs;
                int g = (inputPixel & inParams.gm) >> inParams.gs;
                int b = (inputPixel & inParams.bm) >> inParams.bs;

                r = r & 0xff;
                g = g & 0xff;
                b = b & 0xff;
                a = a & 0xff;

                a = a << expansionA;
                r = r << expansionR;
                g = g << expansionG;
                b = b << expansionB;

                if (inLum)
                    b = g = r;

                if (!inAlpha)
                    a = 0xff;

//                int argb = (a << 24) | (r << 16) | (g << 8) | b;
//                out.setRGB(x, y, argb);
            }
        }
    }

    public static BufferedImage convert(Image image, boolean do16bit, boolean fullalpha, int mipLevel){
        Format format = image.getFormat();
        DecodeParams p = params.get(image.getFormat());
        if (p == null)
            throw new UnsupportedOperationException();

        int width = image.getWidth();
        int height = image.getHeight();

        int level = mipLevel;
        while (--level >= 0){
            width  /= 2;
            height /= 2;
        }

        ByteBuffer buf = image.getData(0);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        BufferedImage out;

        boolean alpha = false;
        boolean luminance = false;
        boolean rgb = false;
        if (p.am != 0)
            alpha = true;

        if (p.rm != 0 && p.gm == 0 && p.bm == 0)
            luminance = true;
        else if (p.rm != 0 && p.gm != 0 && p.bm != 0)
            rgb = true;

        // alpha OR luminance but not both
        if ( (alpha && !rgb && !luminance) || (luminance && !alpha && !rgb) ){
            out = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        }else if ( (rgb && alpha) || (luminance && alpha) ){
            if (do16bit){
                if (fullalpha){
                    ColorModel model = AWTLoader.AWT_RGBA4444;
                    WritableRaster raster = model.createCompatibleWritableRaster(width, width);
                    out = new BufferedImage(model, raster, false, null);
                }else{
                    // RGB5_A1
                    ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
                    int[] nBits = {5, 5, 5, 1};
                    int[] bOffs = {0, 1, 2, 3};
                    ColorModel colorModel = new ComponentColorModel(cs, nBits, true, false,
                                                                    Transparency.BITMASK,
                                                                    DataBuffer.TYPE_BYTE);
                    WritableRaster raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE,
                                                                           width, height,
                                                                           width*2, 2,
                                                                           bOffs, null);
                    out = new BufferedImage(colorModel, raster, false, null);
                }
            }else{
                out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            }
        }else{
            if (do16bit){
                out = new BufferedImage(width, height, BufferedImage.TYPE_USHORT_565_RGB);
            }else{
                out = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            }
        }

        int expansionA = 8 - Integer.bitCount(p.am);
        int expansionR = 8 - Integer.bitCount(p.rm);
        int expansionG = 8 - Integer.bitCount(p.gm);
        int expansionB = 8 - Integer.bitCount(p.bm);
        
        if (expansionR < 0){
            expansionR = 0;
        }
        
        int mipPos = 0;
        for (int i = 0; i < mipLevel; i++){
            mipPos += image.getMipMapSizes()[i];
        }
        int inputPixel;
        for (int y = 0; y < height; y++){
            for (int x = 0; x < width; x++){
                int i = mipPos + (Ix(x,y,width) * p.bpp);
                inputPixel = (readPixel(buf,i,p.bpp) & p.im) >> p.is;
                int a = (inputPixel & p.am) >> p.as;
                int r = (inputPixel & p.rm) >> p.rs;
                int g = (inputPixel & p.gm) >> p.gs;
                int b = (inputPixel & p.bm) >> p.bs;

                r = r & 0xff;
                g = g & 0xff;
                b = b & 0xff;
                a = a & 0xff;

                a = a << expansionA;
                r = r << expansionR;
                g = g << expansionG;
                b = b << expansionB;
                
                if (luminance)
                    b = g = r;

                if (!alpha)
                    a = 0xff;

                int argb = (a << 24) | (r << 16) | (g << 8) | b;
                out.setRGB(x, y, argb);
            }
        }

        return out;
    }

}

package com.jme3.texture.image;

import java.nio.ByteBuffer;

class BitMaskImageCodec extends ImageCodec {
    
    // Shifts
    final int as, rs, gs, bs;
    boolean be = false;
    
    public BitMaskImageCodec(int bpp, int flags, int ac, int rc, int gc, int bc, int as, int rs, int gs, int bs) {
        super(bpp, flags,
                (int) (((long) 1 << ac) - 1),
                (int) (((long) 1 << rc) - 1),
                (int) (((long) 1 << gc) - 1),
                (int) (((long) 1 << bc) - 1));

        if (bpp > 4) {
            throw new UnsupportedOperationException("Use ByteAlignedImageCodec for codecs with pixel sizes larger than 4 bytes");
        }
        
        this.as = as;
        this.rs = rs;
        this.gs = gs;
        this.bs = bs;
    }
    
    private static int readPixelRaw(ByteBuffer buf, int idx, int bpp) {
        idx += bpp;
        int original = buf.get(--idx) & 0xff;
        while ((--bpp) > 0) {
            original = (original << 8) | (buf.get(--idx) & 0xff);
        }
        return original;
    }
    
    private void writePixelRaw(ByteBuffer buf, int idx, int pixel, int bpp){
//        buf.position(idx);
//        if (!be){
            while ((--bpp) >= 0){
                byte bt = (byte) ((pixel >> (bpp * 8)) & 0xff);
                buf.put(idx + bpp, bt);
            }
//        } else {
//            for (int i = bpp - 1; i >= 0; i--) {
//                byte bt = (byte) ((pixel >> (i * 8)) & 0xff);
//                buf.put(idx + i, bt);
//            }
//        }
    }

    @Override
    public void readComponents(ByteBuffer buf, int x, int y, int width, int[] components, byte[] tmp) {
        int inputPixel = readPixelRaw(buf, (x + y * width) * bpp, bpp);
        components[0] = (inputPixel >> as) & maxAlpha;
        components[1] = (inputPixel >> rs) & maxRed;
        components[2] = (inputPixel >> gs) & maxGreen;
        components[3] = (inputPixel >> bs) & maxBlue;
    }

    public void writeComponents(ByteBuffer buf, int x, int y, int width, int[] components, byte[] tmp) {
        // Shift components then mask them
        // Map all components into a single bitspace
        int outputPixel = ((components[0] & maxAlpha) << as)
                        | ((components[1] & maxRed) << rs)
                        | ((components[2] & maxGreen) << gs)
                        | ((components[3] & maxBlue) << bs);
        
        // Find index in image where to write pixel.
        // Write the resultant bitspace into the pixel.
        writePixelRaw(buf, (x + y * width) * bpp, outputPixel, bpp);
    }
}

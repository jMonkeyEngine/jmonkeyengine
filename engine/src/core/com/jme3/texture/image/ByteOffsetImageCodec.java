package com.jme3.texture.image;

import java.nio.ByteBuffer;

public class ByteOffsetImageCodec extends ImageCodec {

    private int redPos, greenPos, bluePos, alphaPos;
    
    public ByteOffsetImageCodec(int bpp, int flags,  int alphaPos, int redPos, int greenPos, int bluePos) {
        super(bpp, flags, alphaPos != -1 ? 255 : 0,
                          redPos != -1 ? 255 : 0,
                          greenPos != -1 ? 255 : 0,
                          bluePos != -1 ? 255 : 0);
        this.alphaPos = alphaPos;
        this.redPos = redPos;
        this.greenPos = greenPos;
        this.bluePos = bluePos;
    }
    
    @Override
    public void readComponents(ByteBuffer buf, int x, int y, int width, int[] components, byte[] tmp) {
        int i = (y * width + x) * bpp;
        buf.position(i);
        buf.get(tmp, 0, bpp);
        if (alphaPos != -1) {
            components[0] = tmp[alphaPos] & 0xff;
        }
        if (redPos != -1) {
            components[1] = tmp[redPos] & 0xff;
        }
        if (greenPos != -1) {
            components[2] = tmp[greenPos] & 0xff;
        }
        if (bluePos != -1) {
            components[3] = tmp[bluePos] & 0xff;
        }
    }

    @Override
    public void writeComponents(ByteBuffer buf, int x, int y, int width, int[] components, byte[] tmp) {
        int i = (y * width + x) * bpp;
        if (alphaPos != -1) {
            tmp[alphaPos] = (byte) components[0];
        }
        if (redPos != -1) {
            tmp[redPos] = (byte) components[1];
        }
        if (greenPos != -1) {
            tmp[greenPos] = (byte) components[2];
        }
        if (bluePos != -1) {
            tmp[bluePos] = (byte) components[3];
        }
        buf.position(i);
        buf.put(tmp, 0, bpp);
    }
    
}

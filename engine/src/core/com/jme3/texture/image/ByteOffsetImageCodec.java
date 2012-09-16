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
        if (alphaPos != -1) {
            components[0] = buf.get(i + alphaPos) & 0xff;
        }
        if (redPos != -1) {
            components[1] = buf.get(i + redPos) & 0xff;
        }
        if (greenPos != -1) {
            components[2] = buf.get(i + greenPos) & 0xff;
        }
        if (bluePos != -1) {
            components[3] = buf.get(i + bluePos) & 0xff;
        }
    }

    @Override
    public void writeComponents(ByteBuffer buf, int x, int y, int width, int[] components, byte[] tmp) {
        int i = (y * width + x) * bpp;
        if (alphaPos != -1) {
            buf.put(i + alphaPos, (byte) components[0]);
        }
        if (redPos != -1) {
            buf.put(i + redPos, (byte) components[1]);
        }
        if (greenPos != -1) {
            buf.put(i + greenPos, (byte) components[2]);
        }
        if (bluePos != -1) {
            buf.put(i + bluePos, (byte) components[3]);
        }
    }
    
}

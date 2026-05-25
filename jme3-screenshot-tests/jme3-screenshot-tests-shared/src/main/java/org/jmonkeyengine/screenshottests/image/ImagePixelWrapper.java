package org.jmonkeyengine.screenshottests.image;

import com.jme3.texture.Image;

import java.nio.ByteBuffer;

public class ImagePixelWrapper {

    private final Image underlyingImage;
    ByteBuffer bytes;
    int sizeX;
    int sizeY;

    public ImagePixelWrapper(Image underlyingImage) {
        this.underlyingImage = underlyingImage;
        if(underlyingImage.getFormat() != Image.Format.RGBA8){
            throw new RuntimeException("Expected RGBA8 but was " + underlyingImage.getFormat());
        }
        this.bytes = underlyingImage.getData(0);
        this.sizeX = underlyingImage.getWidth();
        this.sizeY = underlyingImage.getHeight();
    }

    public int getARGB(int x, int y){
        int idx = getStartIndex(x, y);
        int r = bytes.get(idx)     & 0xFF;
        int g = bytes.get(idx + 1) & 0xFF;
        int b = bytes.get(idx + 2) & 0xFF;
        int a = bytes.get(idx + 3) & 0xFF;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public int getR(int x, int y){
        int startIndex = getStartIndex(x,y);
        return bytes.get(startIndex) & 0xFF;
    }

    public int getG(int x, int y){
        int startIndex = getStartIndex(x,y);
        return bytes.get(startIndex+1) & 0xFF;
    }

    public int getB(int x, int y){
        int startIndex = getStartIndex(x,y);
        return bytes.get(startIndex+2) & 0xFF;
    }

    public int getA(int x, int y){
        int startIndex = getStartIndex(x,y);
        return bytes.get(startIndex+3) & 0xFF;
    }

    public void setARGB(int x, int y, int packedColour) {
        int idx = getStartIndex(x, y);
        bytes.put(idx,     (byte) ((packedColour >> 16) & 0xFF)); // R (bits 16-23)
        bytes.put(idx + 1, (byte) ((packedColour >> 8)  & 0xFF)); // G (bits 8-15)
        bytes.put(idx + 2, (byte) (packedColour & 0xFF));        // B (bits 0-7)
        bytes.put(idx + 3, (byte) ((packedColour >> 24) & 0xFF)); // A (bits 24-31)
    }

    private int getStartIndex(int x, int y) {
        if (x < 0 || x >= sizeX || y < 0 || y >= sizeY) {
            throw new IndexOutOfBoundsException("x: " + x + ", y: " + y);
        }
        return (y * sizeX + x) * 4; // 4 bytes per pixel (RGBA8)
    }

    public Image getUnderlyingImage() {
        return underlyingImage;
    }
}

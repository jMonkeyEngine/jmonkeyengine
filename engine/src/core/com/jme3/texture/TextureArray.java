package com.jme3.texture;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class implements a Texture array
 * warning, this feature is only supported on opengl 3.0 version.
 * To check if a hardware supports TextureArray check : 
 * renderManager.getRenderer().getCaps().contains(Caps.TextureArray)
 * @author phate666
 */
public class TextureArray extends Texture {

    private WrapMode wrapS = WrapMode.EdgeClamp;
    private WrapMode wrapT = WrapMode.EdgeClamp;

    /**
     * Construct a TextureArray
     * warning, this feature is only supported on opengl 3.0 version.
     * To check if a hardware supports TextureArray check : 
     * renderManager.getRenderer().getCaps().contains(Caps.TextureArray)
     */
    public TextureArray() {
        super();
    }

    /**
     * Construct a TextureArray from the given list of images
     * warning, this feature is only supported on opengl 3.0 version.
     * To check if a hardware supports TextureArray check : 
     * renderManager.getRenderer().getCaps().contains(Caps.TextureArray)
     * @param images 
     */
    public TextureArray(List<Image> images) {
        super();        
        int width = images.get(0).getWidth();
        int height = images.get(0).getHeight();
        Image arrayImage = new Image(images.get(0).getFormat(), width, height,
                null);

        for (Image img : images) {
            if (img.getHeight() != height || img.getWidth() != width) {
                Logger.getLogger(TextureArray.class.getName()).log(
                        Level.WARNING,
                        "all images must have the same width/height");
                continue;
            }
            arrayImage.addData(img.getData(0));
        }
        setImage(arrayImage);
    }

    @Override
    public Texture createSimpleClone() {
        TextureArray clone = new TextureArray();
        createSimpleClone(clone);
        return clone;
    }

    @Override
    public Texture createSimpleClone(Texture rVal) {
        rVal.setWrap(WrapAxis.S, wrapS);
        rVal.setWrap(WrapAxis.T, wrapT);
        return super.createSimpleClone(rVal);
    }

    @Override
    public Type getType() {
        return Type.TwoDimensionalArray;
    }

    @Override
    public WrapMode getWrap(WrapAxis axis) {
        switch (axis) {
            case S:
                return wrapS;
            case T:
                return wrapT;
            default:
                throw new IllegalArgumentException("invalid WrapAxis: " + axis);
        }
    }

    @Override
    public void setWrap(WrapAxis axis, WrapMode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("mode can not be null.");
        } else if (axis == null) {
            throw new IllegalArgumentException("axis can not be null.");
        }
        switch (axis) {
            case S:
                this.wrapS = mode;
                break;
            case T:
                this.wrapT = mode;
                break;
            default:
                throw new IllegalArgumentException("Not applicable for 2D textures");
        }
    }

    @Override
    public void setWrap(WrapMode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("mode can not be null.");
        }
        this.wrapS = mode;
        this.wrapT = mode;
    }
}
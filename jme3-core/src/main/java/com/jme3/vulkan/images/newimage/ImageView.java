package com.jme3.vulkan.images.newimage;

import com.jme3.util.natives.Destructor;
import com.jme3.util.natives.Destructable;
import com.jme3.vulkan.formats.EnumInterpreter;
import com.jme3.vulkan.formats.Format;
import com.jme3.vulkan.images.ColorSwizzle;
import com.jme3.vulkan.util.Flag;

public class ImageView <T extends EngineImage> implements Destructable {

    public enum Type {

        OneDimensional,
        TwoDimensional,
        ThreeDimensional,
        OneDimensionalArray,
        TwoDimensionalArray,
        Cube,
        CubeArray;

        public int getEnum(EnumInterpreter interpreter) {
            return interpreter.getImageViewType(this);
        }

    }

    private final T image;
    private final long handle;
    private final Format format;
    private final ColorSwizzle swizzle;
    private final Flag<EngineImage.Aspect> aspects;
    private final int baseMipLevel, mipLevels;
    private final int baseArrayLayer, arrayLayers;
    private final Destructor destructor;

    public ImageView(T image, long handle, Format format, ColorSwizzle swizzle, Flag<EngineImage.Aspect> aspects,
                     int baseMipLevel, int mipLevels, int baseArrayLayer, int arrayLayers, Runnable destroy) {
        this.handle = handle;
        this.image = image;
        this.format = format;
        this.swizzle = swizzle;
        this.aspects = aspects;
        this.baseMipLevel = baseMipLevel;
        this.mipLevels = mipLevels;
        this.baseArrayLayer = baseArrayLayer;
        this.arrayLayers = arrayLayers;
        this.destructor = image.getDestructor().addDependent(Destructor.run(this, destroy));
    }

    public ImageView(T image, long handle, Runnable destroy) {
        this(image, handle, image.getFormat(), ColorSwizzle.NORMAL, image.getFormat().getAspects().getImageAspect(),
                0, image.getMipLevels(), 0, image.getArrayLayers(), destroy);
    }

    @Override
    public Destructor getDestructor() {
        return destructor;
    }

    public T getImage() {
        return image;
    }

    public long getHandle() {
        return handle;
    }

    public Format getFormat() {
        return format;
    }

    public ColorSwizzle getSwizzle() {
        return swizzle;
    }

    public Flag<EngineImage.Aspect> getAspects() {
        return aspects;
    }

    public int getBaseMipLevel() {
        return baseMipLevel;
    }

    public int getMipLevels() {
        return mipLevels;
    }

    public int getBaseArrayLayer() {
        return baseArrayLayer;
    }

    public int getArrayLayers() {
        return arrayLayers;
    }

}

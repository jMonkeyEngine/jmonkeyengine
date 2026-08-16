package com.jme3.vulkan.images.newimage;

import com.jme3.math.FastMath;
import com.jme3.math.IntVector;
import com.jme3.vulkan.formats.Format;
import com.jme3.vulkan.util.Flag;

public class ImageInfo {

    private static final int COMPUTE_OPTIMAL_MIPS = -1;

    private EngineImage.Type type = EngineImage.Type.TwoDimensional;
    private Format format = Format.RGBA8_SRGB;
    private EngineImage.Layout layout = EngineImage.Layout.Undefined;
    private final IntVector size = new IntVector(1, 1, 1);
    private int samples = 1;
    private int mipLevels = 1;
    private int arrayLayers = 1;
    private EngineImage.Tiling tiling = EngineImage.Tiling.Optimal;
    private Flag<EngineImage.Role> roles = Flag.empty();
    private Flag<EngineImage.Create> createFlags = Flag.empty();

    public ImageInfo() {
    }

    public ImageInfo(EngineImage.Type type) {
        this.type = type;
    }

    public ImageInfo addRoles(Flag<EngineImage.Role> roles) {
        this.roles = this.roles.add(roles);
        return this;
    }

    public ImageInfo addCreateFlags(Flag<EngineImage.Create> createFlags) {
        this.createFlags = this.createFlags.add(createFlags);
        return this;
    }

    public ImageInfo useOptimalMipLevels() {
        mipLevels = COMPUTE_OPTIMAL_MIPS;
        return this;
    }

    public ImageInfo setCubeCompatible() {
        addCreateFlags(EngineImage.Create.CubeCompatible);
        return this;
    }

    public ImageInfo setType(EngineImage.Type type) {
        this.type = type;
        return this;
    }

    public ImageInfo setFormat(Format format) {
        this.format = format;
        return this;
    }

    public ImageInfo setLayout(EngineImage.Layout layout) {
        this.layout = layout;
        return this;
    }

    public ImageInfo setSize(IntVector size) {
        this.size.set(size);
        return this;
    }

    public ImageInfo setSize(int width, int height) {
        this.size.set(width, height);
        return this;
    }

    public ImageInfo setSize(int width, int height, int depth) {
        this.size.set(width, height, depth);
        return this;
    }

    public ImageInfo setSizeSquare(int size) {
        this.size.set(size, size);
        return this;
    }

    public ImageInfo setSizeCube(int size) {
        this.size.set(size, size, size);
        return this;
    }

    public ImageInfo setWidth(int width) {
        size.x = width;
        return this;
    }

    public ImageInfo setHeight(int height) {
        size.y = height;
        return this;
    }

    public ImageInfo setDepth(int depth) {
        size.z = depth;
        return this;
    }

    public ImageInfo setSamples(int samples) {
        this.samples = samples;
        return this;
    }

    public ImageInfo setMipLevels(int mipLevels) {
        this.mipLevels = mipLevels;
        return this;
    }

    public ImageInfo setArrayLayers(int arrayLayers) {
        this.arrayLayers = arrayLayers;
        return this;
    }

    public ImageInfo setTiling(EngineImage.Tiling tiling) {
        this.tiling = tiling;
        return this;
    }

    public ImageInfo setRoles(Flag<EngineImage.Role> roles) {
        this.roles = roles;
        return this;
    }

    public ImageInfo setCreateFlags(Flag<EngineImage.Create> createFlags) {
        this.createFlags = createFlags;
        return this;
    }

    public EngineImage.Type getType() {
        return type;
    }

    public Format getFormat() {
        return format;
    }

    public EngineImage.Layout getLayout() {
        return layout;
    }

    public IntVector getSize() {
        return size;
    }

    public int getSamples() {
        return samples;
    }

    public int getMipLevels() {
        return mipLevels != COMPUTE_OPTIMAL_MIPS ? mipLevels : FastMath.log2(Math.max(Math.max(size.x, size.y), size.z));
    }

    public int getArrayLayers() {
        return arrayLayers;
    }

    public EngineImage.Tiling getTiling() {
        return tiling;
    }

    public Flag<EngineImage.Role> getRoles() {
        return roles;
    }

    public Flag<EngineImage.Create> getCreateFlags() {
        return createFlags;
    }

}

package com.jme3.vulkan.images.newimage;

import com.jme3.vulkan.images.AddressMode;
import com.jme3.vulkan.images.BorderColor;
import com.jme3.vulkan.images.FilterMode;
import com.jme3.vulkan.images.MipmapMode;
import com.jme3.vulkan.pipeline.CompareOp;

public class SamplerInfo {

    private MipmapMode mipmapMode = MipmapMode.Nearest;
    private FilterMode minFilter = FilterMode.Linear;
    private FilterMode magFilter = FilterMode.Linear;
    private final AddressMode[] edgeModes = {AddressMode.ClampToEdge, AddressMode.ClampToEdge, AddressMode.ClampToEdge};
    private float anisotropy = Float.NaN;
    private BorderColor border = BorderColor.FloatOpaqueBlack;
    private CompareOp compare = CompareOp.Always;
    private float lodBias = 0f;
    private float minLod = Float.NaN;
    private float maxLod = Float.NaN;
    private boolean unnormalizedCoords = false;

    public SamplerInfo setMipmapMode(MipmapMode mipmapMode) {
        this.mipmapMode = mipmapMode;
        return this;
    }

    public SamplerInfo setMinFilter(FilterMode minFilter) {
        this.minFilter = minFilter;
        return this;
    }

    public SamplerInfo setMagFilter(FilterMode magFilter) {
        this.magFilter = magFilter;
        return this;
    }

    public SamplerInfo setEdgeMode(AddressMode u, AddressMode v, AddressMode w) {
        edgeModes[Sampler.U_AXIS] = u;
        edgeModes[Sampler.V_AXIS] = v;
        edgeModes[Sampler.W_AXIS] = w;
        return this;
    }

    public SamplerInfo setEdgeMode(AddressMode u, AddressMode v) {
        edgeModes[Sampler.U_AXIS] = u;
        edgeModes[Sampler.V_AXIS] = v;
        return this;
    }

    public SamplerInfo setEdgeModeU(AddressMode u) {
        edgeModes[Sampler.U_AXIS] = u;
        return this;
    }

    public SamplerInfo setEdgeModeV(AddressMode v) {
        edgeModes[Sampler.V_AXIS] = v;
        return this;
    }

    public SamplerInfo setEdgeModeW(AddressMode w) {
        edgeModes[Sampler.W_AXIS] = w;
        return this;
    }

    public SamplerInfo setAnisotropy(float anisotropy) {
        this.anisotropy = anisotropy;
        return this;
    }

    public SamplerInfo setBorderColor(BorderColor border) {
        this.border = border;
        return this;
    }

    public SamplerInfo setCompare(CompareOp compare) {
        this.compare = compare;
        return this;
    }

    public SamplerInfo setLodBias(float lodBias) {
        this.lodBias = lodBias;
        return this;
    }

    public SamplerInfo setMinLod(float minLod) {
        this.minLod = minLod;
        return this;
    }

    public SamplerInfo setMaxLod(float maxLod) {
        this.maxLod = maxLod;
        return this;
    }

    public SamplerInfo setUnnormalizedCoords(boolean unnormalizedCoords) {
        this.unnormalizedCoords = unnormalizedCoords;
        return this;
    }

    public MipmapMode getMipmapMode() {
        return mipmapMode;
    }

    public FilterMode getMinFilter() {
        return minFilter;
    }

    public FilterMode getMagFilter() {
        return magFilter;
    }

    public AddressMode[] getEdgeModes() {
        return edgeModes;
    }

    public AddressMode getEdgeModeU() {
        return edgeModes[Sampler.U_AXIS];
    }

    public AddressMode getEdgeModeV() {
        return edgeModes[Sampler.V_AXIS];
    }

    public AddressMode getEdgeModeW() {
        return edgeModes[Sampler.W_AXIS];
    }

    public float getAnisotropy() {
        return anisotropy;
    }

    public BorderColor getBorderColor() {
        return border;
    }

    public CompareOp getCompare() {
        return compare;
    }

    public float getLodBias() {
        return lodBias;
    }

    public float getMinLod() {
        return minLod;
    }

    public float getMaxLod() {
        return maxLod;
    }

    public boolean isUnnormalizedCoords() {
        return unnormalizedCoords;
    }

}

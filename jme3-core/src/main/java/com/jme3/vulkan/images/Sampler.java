package com.jme3.vulkan.images;

import com.jme3.util.AbstractNativeBuilder;
import com.jme3.util.natives.AbstractNative;
import com.jme3.util.natives.DisposableManager;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.pipeline.CompareOp;
import com.jme3.vulkan.util.IntEnum;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;
import org.lwjgl.vulkan.VkSamplerCreateInfo;

import java.nio.LongBuffer;
import java.util.Objects;
import java.util.function.Consumer;

import static com.jme3.renderer.vulkan.VulkanUtils.check;
import static org.lwjgl.vulkan.VK10.*;

public class Sampler extends AbstractNative<Long> {

    public static final int U = 0, V = 1, W = 2;
    public static final float DISABLE_ANISOTROPY = 0f;

    private final LogicalDevice<?> device;
    private IntEnum<MipmapMode> mipmapMode = MipmapMode.Nearest;
    private IntEnum<Filter> min = Filter.Linear;
    private IntEnum<Filter> mag = Filter.Linear;
    private final IntEnum[] edgeModes = {AddressMode.ClampToEdge, AddressMode.ClampToEdge, AddressMode.ClampToEdge};
    private float anisotropy = Float.MAX_VALUE;
    private IntEnum<BorderColor> borderColor = BorderColor.FloatOpaqueBlack;
    private IntEnum<CompareOp> compare = CompareOp.Always;
    private float mipLodBias = 0f;
    private float minLod = 0f;
    private float maxLod = VK_LOD_CLAMP_NONE;
    private boolean unnormalizedCoords = false;

    public Sampler(LogicalDevice<?> device) {
        this.device = device;
    }

    @Override
    public Runnable createDestroyer() {
        return () -> vkDestroySampler(device.getNativeObject(), object, null);
    }

    public LogicalDevice<?> getDevice() {
        return device;
    }

    public IntEnum<MipmapMode> getMipmapMode() {
        return mipmapMode;
    }

    public IntEnum<Filter> getMinFilter() {
        return min;
    }

    public IntEnum<Filter> getMagFilter() {
        return mag;
    }

    public IntEnum[] getEdgeModes() {
        return edgeModes;
    }

    public float getAnisotropy() {
        return anisotropy;
    }

    public boolean isAnisotropyEnabled() {
        return anisotropy > DISABLE_ANISOTROPY;
    }

    public IntEnum<BorderColor> getBorderColor() {
        return borderColor;
    }

    public IntEnum<CompareOp> getCompare() {
        return compare;
    }

    public float getMipLodBias() {
        return mipLodBias;
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

    public static Sampler build(LogicalDevice<?> device, Consumer<Builder> config) {
        Builder b = new Sampler(device).new Builder();
        config.accept(b);
        return b.build();
    }

    public class Builder extends AbstractNativeBuilder<Sampler> {

        private Builder() {}

        @Override
        protected Sampler construct() {
            VkPhysicalDeviceProperties props = device.getPhysicalDevice().getProperties();
            VkSamplerCreateInfo create = VkSamplerCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO)
                    .minFilter(min.getEnum())
                    .magFilter(mag.getEnum())
                    .addressModeU(edgeModes[U].getEnum())
                    .addressModeV(edgeModes[V].getEnum())
                    .addressModeW(edgeModes[W].getEnum())
                    .anisotropyEnable(anisotropy > DISABLE_ANISOTROPY)
                    .maxAnisotropy(Math.min(anisotropy, props.limits().maxSamplerAnisotropy()))
                    .borderColor(borderColor.getEnum())
                    .unnormalizedCoordinates(unnormalizedCoords)
                    .compareEnable(compare != null)
                    .compareOp(IntEnum.get(compare, CompareOp.Always).getEnum())
                    .mipmapMode(mipmapMode.getEnum())
                    .mipLodBias(mipLodBias)
                    .minLod(minLod)
                    .maxLod(maxLod);
            LongBuffer idBuf = stack.mallocLong(1);
            check(vkCreateSampler(device.getNativeObject(), create, null, idBuf),
                    "Failed to create sampler.");
            object = idBuf.get(0);
            ref = DisposableManager.reference(Sampler.this);
            device.getReference().addDependent(ref);
            return Sampler.this;
        }

        public void setMipmapMode(IntEnum<MipmapMode> m) {
            mipmapMode = Objects.requireNonNull(m);
        }

        public void setMinFilter(IntEnum<Filter> f) {
            min = Objects.requireNonNull(f);
        }

        public void setMagFilter(IntEnum<Filter> f) {
            mag = Objects.requireNonNull(f);
        }

        public void setMinMagFilters(IntEnum<Filter> min, IntEnum<Filter> mag) {
            setMagFilter(min);
            setMagFilter(mag);
        }

        public void setEdgeMode(int axis, IntEnum<AddressMode> a) {
            if (axis < 0 || axis >= edgeModes.length) {
                throw new IndexOutOfBoundsException("Invalid axis index (" + axis + "). " +
                        "Must be 0 (U), 1 (V), or 2 (W).");
            }
            edgeModes[axis] = Objects.requireNonNull(a);
        }

        public void setEdgeModeU(IntEnum<AddressMode> u) {
            setEdgeMode(U, u);
        }

        public void setEdgeModeV(IntEnum<AddressMode> v) {
            setEdgeMode(V, v);
        }

        public void setEdgeModeW(IntEnum<AddressMode> w) {
            setEdgeMode(W, w);
        }

        public void setEdgeModes(IntEnum<AddressMode> u, IntEnum<AddressMode> v, IntEnum<AddressMode> w) {
            setEdgeMode(U, u);
            setEdgeMode(V, v);
            setEdgeMode(W, w);
        }

        public void setEdgeModes(IntEnum<AddressMode> e) {
            setEdgeMode(U, e);
            setEdgeMode(V, e);
            setEdgeMode(W, e);
        }

        public void setAnisotropy(float a) {
            anisotropy = a;
        }

        public void maxAnisotropy() {
            setAnisotropy(Float.MAX_VALUE);
        }

        public void disableAnisotropy() {
            setAnisotropy(DISABLE_ANISOTROPY);
        }

        public void setBorderColor(IntEnum<BorderColor> c) {
            borderColor = Objects.requireNonNull(c);
        }

        public void setCompare(IntEnum<CompareOp> c) {
            compare = Objects.requireNonNull(c);
        }

        public void setMipLodBias(float bias) {
            mipLodBias = bias;
        }

        public void setMinLod(float lod) {
            minLod = lod;
        }

        public void setMaxLod(float lod) {
            maxLod = lod;
        }

        public void disableMaxLod() {
            setMaxLod(VK_LOD_CLAMP_NONE);
        }

        public void setUnnormalizedCoords(boolean u) {
            unnormalizedCoords = u;
        }

    }

}

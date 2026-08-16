package com.jme3.vulkan.images;

import com.jme3.util.AbstractNativeBuilder;
import com.jme3.util.natives.DisposableManager;
import com.jme3.vulkan.pipeline.CompareOp;
import com.jme3.vulkan.util.IntEnum;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;
import org.lwjgl.vulkan.VkSamplerCreateInfo;

import java.nio.LongBuffer;

import static com.jme3.renderer.vulkan.VulkanUtils.check;
import static org.lwjgl.vulkan.VK10.*;

@Deprecated
public class SamplerInfo {

    public static final int U = 0, V = 1, W = 2;
    public static final float DISABLE_ANISOTROPY = 0f;

    private MipmapMode mipmapMode = MipmapMode.Nearest;
    private FilterMode min = FilterMode.Linear;
    private FilterMode mag = FilterMode.Linear;
    private final AddressMode[] edgeModes = {AddressMode.ClampToEdge, AddressMode.ClampToEdge, AddressMode.ClampToEdge};
    private float anisotropy = Float.MAX_VALUE;
    private BorderColor borderColor = BorderColor.FloatOpaqueBlack;
    private CompareOp compare = CompareOp.Always;
    private float mipLodBias = 0f;
    private float minLod = 0f;
    private float maxLod = VK_LOD_CLAMP_NONE;
    private boolean unnormalizedCoords = false;

    public MipmapMode getMipmapMode() {
        return mipmapMode;
    }

    public FilterMode getMinFilter() {
        return min;
    }

    public FilterMode getMagFilter() {
        return mag;
    }

    public AddressMode[] getEdgeModes() {
        return edgeModes;
    }

    public float getAnisotropy() {
        return anisotropy;
    }

    public boolean isAnisotropyEnabled() {
        return anisotropy > DISABLE_ANISOTROPY;
    }

    public BorderColor getBorderColor() {
        return borderColor;
    }

    public CompareOp getCompare() {
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

    @Deprecated
    public class Builder extends AbstractNativeBuilder<SamplerInfo> {

        private Builder() {}

        @Override
        protected SamplerInfo construct() {
            VkPhysicalDeviceProperties props = device.getPhysicalDevice().getProperties();
            VkSamplerCreateInfo create = VkSamplerCreateInfo.calloc(stack)
                    .sType$Default()
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
            ref = DisposableManager.reference(SamplerInfo.this);
            device.getReference().addDependent(ref);
            return SamplerInfo.this;
        }

    }

}

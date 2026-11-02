package com.jme3.vulkan.descriptors;

import com.jme3.vulkan.alloc.BufferDescription;
import com.jme3.vulkan.buffer.GpuBuffer;
import com.jme3.vulkan.images.newimage.EngineImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import java.util.List;

import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_UNDEFINED;
import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;

/**
 * Supplies the resources bound to one layout binding. Each time a {@link DescriptorSet}
 * is {@link DescriptorSet#update() updated}, {@link #emit} is called for every
 * descriptor of the binding; any artifact not {@link Object#equals equal} to the one
 * currently bound in that set is then written with {@link #populate}.
 * <p>
 * Artifacts should therefore be immutable snapshots of what the device needs, such as
 * {@link BufferArtifact} or {@link ImageArtifact}, rather than the live resource:
 * a resource that changed internally (e.g. reallocated its buffer) would otherwise
 * still compare equal to itself.
 *
 * @param <T> artifact type
 */
public interface BindingLogic<E, T> {

    T emit(E value);

    /**
     * Fills in the resource info of {@code write} for a run of consecutive descriptors.
     * Everything else about {@code write} is filled in by the caller.
     *
     * @param stack     stack to allocate resource info on
     * @param write     write being populated
     * @param artifacts artifacts of the run, in descriptor order (elements may be null)
     */
    void populate(MemoryStack stack, VkWriteDescriptorSet write, List<T> artifacts);

    static BindingLogic<BufferDescription, BufferArtifact> buffers() {
        return new BufferBindingLogic();
    }

    BindingLogic<EngineImage, ImageArtifact> TEXTURE = new BindingLogic<EngineImage, ImageArtifact>() {
        @Override
        public ImageArtifact emit(EngineImage artifact) {
            return artifact != null ? new ImageArtifact(artifact.getView().getHandle(), artifact.getSampler().getHandle(),
                    artifact.getImage().getLayout().getEnum()) : null;
        }

        @Override
        public void populate(MemoryStack stack, VkWriteDescriptorSet write, List<ImageArtifact> artifacts) {
            VkDescriptorImageInfo.Buffer info = VkDescriptorImageInfo.calloc(artifacts.size(), stack);
            for (ImageArtifact a : artifacts) {
                if (a != null) {
                    info.get().imageView(a.getView()).sampler(a.getSampler()).imageLayout(a.getLayout());
                } else {
                    info.get().imageView(VK_NULL_HANDLE).sampler(VK_NULL_HANDLE).imageLayout(VK_IMAGE_LAYOUT_UNDEFINED);
                }
            }
            write.pImageInfo(info.flip());
        }
    };

    BindingLogic<BufferDescription, BufferArtifact> BUFFER = new BindingLogic<BufferDescription, BufferArtifact>() {

        @Override
        public BufferArtifact emit(BufferDescription value) {
            GpuBuffer b = value != null ? value.getBuffer() : null;
            return b != null ? new BufferArtifact(b.getHandle(), b.offset() + value.offset(), value.size()) : null;
        }

        @Override
        public void populate(MemoryStack stack, VkWriteDescriptorSet write, List<BufferArtifact> artifacts) {
            VkDescriptorBufferInfo.Buffer info = VkDescriptorBufferInfo.calloc(artifacts.size(), stack);
            for (BufferArtifact a : artifacts) {
                if (a != null) {
                    info.get().buffer(a.getHandle()).offset(a.getOffset()).range(a.getRange());
                } else {
                    info.get().buffer(VK_NULL_HANDLE);
                }
            }
            write.pBufferInfo(info.flip());
        }

    };

}

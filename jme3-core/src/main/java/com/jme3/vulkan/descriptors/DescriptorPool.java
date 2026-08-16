package com.jme3.vulkan.descriptors;

import com.jme3.util.natives.*;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.util.ErrorCodes;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;
import java.util.*;

import static com.jme3.renderer.vulkan.VulkanUtils.check;
import static org.lwjgl.vulkan.VK14.*;

public class DescriptorPool implements NativeHandle<Long> {

    public enum Create implements Flag<Create> {

        FreeDescriptorSets(VK_DESCRIPTOR_POOL_CREATE_FREE_DESCRIPTOR_SET_BIT);

        private final int vkEnum;

        Create(int vkEnum) {
            this.vkEnum = vkEnum;
        }

        @Override
        public int bits() {
            return vkEnum;
        }

    }

    private final LogicalDevice<?> device;
    private final Flag<Create> flags;
    private final long object;
    private final Destructor destructor;

    public DescriptorPool(LogicalDevice<?> device, int sets, PoolSize... sizes) {
        this(device, sets, Flag.empty(), sizes);
    }

    public DescriptorPool(LogicalDevice<?> device, int sets, Flag<Create> flags, PoolSize... sizes) {
        this.device = device;
        this.flags = flags;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkDescriptorPoolCreateInfo create = VkDescriptorPoolCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
                .pPoolSizes(PoolSize.aggregate(stack, sizes))
                .maxSets(sets)
                .flags(this.flags.bits());
            LongBuffer idBuf = stack.mallocLong(1);
            check(vkCreateDescriptorPool(device.getHandle(), create, null, idBuf),
                    "Failed to create descriptor pool.");
            object = idBuf.get(0);
        }
        VkDevice deviceHandle = device.getHandle();
        destructor = device.getDestructor().addDependent(new Destructor(this) {
            @Override
            protected void runDestroy() {
                vkDestroyDescriptorPool(deviceHandle, object, null);
            }
        });
    }

    @Override
    public Destructor getDestructor() {
        return destructor;
    }

    @Override
    public Long getHandle() {
        return object;
    }

    /**
     * Allocates a {@link DescriptorSet} for each {@link DescriptorSetLayout} provided.
     *
     * @param layouts layouts to allocate DescriptorSets with
     * @return allocated DescriptorSets, in the same order as {@code layouts}
     */
    public Optional<DescriptorSet[]> allocateSets(DescriptorSetLayout... layouts) {
        return allocateSets(Arrays.asList(layouts));
    }

    /**
     * Allocates a {@link DescriptorSet} for each {@link DescriptorSetLayout} provided.
     *
     * @param layouts layouts to allocate DescriptorSets with
     * @return allocated sets in the same order as {@code layouts}, or {@link Optional#empty()} if the
     * pool ran out of memory
     * @throws RuntimeException if an error occurs allocating the sets from vulkan other than the pool
     * running out of memory
     */
    public Optional<DescriptorSet[]> allocateSets(List<DescriptorSetLayout> layouts) {
        if (layouts.isEmpty()) {
            return Optional.of(new DescriptorSet[0]);
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer layoutBuf = stack.mallocLong(layouts.size());
            for (DescriptorSetLayout l : layouts) {
                layoutBuf.put(l.getHandle());
            }
            layoutBuf.flip();
            VkDescriptorSetAllocateInfo allocate = VkDescriptorSetAllocateInfo.calloc(stack)
                .sType$Default()
                .descriptorPool(object)
                .pSetLayouts(layoutBuf);
            LongBuffer setBuf = stack.mallocLong(layouts.size());
            int result = vkAllocateDescriptorSets(device.getHandle(), allocate, setBuf);
            if (result == VK_ERROR_OUT_OF_POOL_MEMORY) {
                return Optional.empty();
            }
            if (result != VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate requested descriptor sets (Vulkan returned " + ErrorCodes.findName(result) + ").");
            }
            DescriptorSet[] sets = new DescriptorSet[layouts.size()];
            for (ListIterator<DescriptorSetLayout> it = layouts.listIterator(); it.hasNext();) {
                sets[it.nextIndex()] = new DescriptorSet(this, it.next(), setBuf.get());
            }
            return Optional.of(sets);
        }
    }

    /**
     * Resets this pool. All allocated descriptor sets are freed.
     */
    public void free() {
        vkResetDescriptorPool(device.getHandle(), object, 0);
    }

    /**
     * Frees all descriptor sets in {@code set}. Requires that this pool be created with
     * {@link Create#FreeDescriptorSets}.
     *
     * @param sets sets to free
     */
    public void free(Collection<DescriptorSet> sets) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer setBuf = stack.mallocLong(sets.size());
            for (DescriptorSet s : sets) {
                if (s.getPool() != this) {
                    throw new IllegalArgumentException("Descriptor set not allocated from pool: " + s);
                }
                setBuf.put(s.getHandle());
            }
            vkFreeDescriptorSets(device.getHandle(), object, setBuf.flip());
        }
    }

    /**
     * Frees all descriptor sets in {@code sets}.
     *
     * @param sets sets to free
     */
    public void free(DescriptorSet... sets) {
        free(Arrays.asList(sets));
    }

    public Flag<Create> getFlags() {
        return flags;
    }

    public LogicalDevice<?> getDevice() {
        return device;
    }

}

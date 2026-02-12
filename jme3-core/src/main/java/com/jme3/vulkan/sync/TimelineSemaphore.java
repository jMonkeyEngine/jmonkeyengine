package com.jme3.vulkan.sync;

import com.jme3.util.natives.AbstractNative;
import com.jme3.util.natives.DisposableManager;
import com.jme3.vulkan.devices.LogicalDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreTypeCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreWaitInfo;

import java.nio.LongBuffer;
import java.util.concurrent.TimeUnit;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK12.*;

public class TimelineSemaphore extends AbstractNative<Long> implements Semaphore {

    private final LogicalDevice<?> device;
    private long payload;

    public TimelineSemaphore(LogicalDevice<?> device) {
        this(device, 0);
    }

    public TimelineSemaphore(LogicalDevice<?> device, long payload) {
        this.device = device;
        this.payload = payload;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSemaphoreTypeCreateInfo type = VkSemaphoreTypeCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SEMAPHORE_TYPE_CREATE_INFO)
                    .semaphoreType(VK_SEMAPHORE_TYPE_TIMELINE)
                    .initialValue(payload);
            VkSemaphoreCreateInfo create = VkSemaphoreCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO)
                    .pNext(type);
            object = getLong(stack, ptr -> check(vkCreateSemaphore(device.getNativeObject(), create, null, ptr),
                    "Failed to create timeline semaphore."));
        }
        ref = DisposableManager.reference(this);
        device.getReference().addDependent(ref);
    }

    @Override
    public long getSemaphoreObject() {
        return object;
    }

    @Override
    public long getTargetPayload() {
        return payload;
    }

    @Override
    public Runnable createDestroyer() {
        return () -> vkDestroySemaphore(device.getNativeObject(), object, null);
    }

    public TimelineSemaphore setTargetPayload(long payload) {
        this.payload = payload;
        return this;
    }

    public TimelineSemaphore incrementTargetPayload() {
        payload++;
        return this;
    }

    public long getNativePayload() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            return getNativePayload(stack);
        }
    }

    public long getNativePayload(MemoryStack stack) {
        LongBuffer buf = stack.mallocLong(1);
        getNativePayload(buf);
        return buf.get(0);
    }

    public void getNativePayload(LongBuffer buffer) {
        vkGetSemaphoreCounterValue(device.getNativeObject(), object, buffer);
    }

    public SignalEvent createEvent() {
        return new SignalEvent(payload);
    }

    public class SignalEvent {

        private final long payload;

        private SignalEvent(long payload) {
            this.payload = payload;
        }

        /**
         * Returns true if this event has been signaled.
         *
         * @return true if signaled
         * @see #signaled(MemoryStack)
         */
        public boolean signaled() {
            return awaitSignal(0L);
        }

        /**
         * Returns true if this event has been signaled.
         *
         * @param stack memory stack
         * @return true if signaled
         */
        public boolean signaled(MemoryStack stack) {
            return awaitSignal(stack, 0L);
        }

        /**
         * Blocks for the specified amount of time until this event is signaled. Note that
         * if the time is set to zero, this method returns immediately.
         *
         * @param waitMillis milliseconds to wait before a timeout occurs
         * @return true if the event was signaled inside the specified wait time
         * @see #awaitSignal(MemoryStack, long)
         */
        public boolean awaitSignal(long waitMillis) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                return awaitSignal(stack, waitMillis);
            }
        }

        /**
         * Blocks for the specified amount of time until this event is signaled. Note that
         * if the time is set to zero, this method returns immediately.
         *
         * @param stack memory stack
         * @param waitTime milliseconds to wait before a timeout occurs
         * @return true if the event was signaled inside the specified wait time
         */
        public boolean awaitSignal(MemoryStack stack, long waitTime) {
            LongBuffer sem = stack.mallocLong(1);
            LongBuffer vals = stack.mallocLong(1);
            sem.put(getNativeObject()).flip();
            vals.put(payload).flip();
            VkSemaphoreWaitInfo wait = VkSemaphoreWaitInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SEMAPHORE_WAIT_INFO)
                    .pSemaphores(sem)
                    .pValues(vals);
            return check(vkWaitSemaphores(device.getNativeObject(), wait, TimeUnit.MICROSECONDS.toNanos(waitTime)),
                    "Error on semaphore wait.") == VK_SUCCESS;
        }

    }

}

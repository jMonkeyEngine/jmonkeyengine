package com.jme3.vulkan.commands;

import com.jme3.renderer.ScissorArea;
import com.jme3.renderer.ViewPortArea;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkViewport;

import java.util.ArrayDeque;
import java.util.Deque;

import static org.lwjgl.vulkan.VK10.*;

public abstract class CommandSetting <T> {

    private final Deque<T> states = new ArrayDeque<>();
    private boolean updateNeeded = false;

    public void push(T value) {
        updateNeeded = updateNeeded || states.isEmpty() || !states.peek().equals(value);
        states.push(value);
    }

    public void pop() {
        T current = states.pop();
        updateNeeded = updateNeeded || (!states.isEmpty() && !current.equals(states.peek()));
    }

    public void apply(CommandBuffer cmd) {
        if (updateNeeded) {
            apply(cmd, states.peek());
            updateNeeded = false;
        }
    }

    protected abstract void apply(CommandBuffer cmd, T value);

    public static CommandSetting<ViewPortArea> viewPort() {
        return new ViewPortImpl();
    }

    public static CommandSetting<ScissorArea> scissor() {
        return new ScissorImpl();
    }

    private static class ViewPortImpl extends CommandSetting<ViewPortArea> {

        @Override
        protected void apply(CommandBuffer cmd, ViewPortArea value) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkViewport.Buffer vp = VkViewport.malloc(1, stack)
                        .x(value.getX())
                        .y(value.getY())
                        .width(value.getWidth())
                        .height(value.getHeight())
                        .minDepth(value.getMinDepth())
                        .maxDepth(value.getMaxDepth());
                vkCmdSetViewport(cmd.getBuffer(), 0, vp);
            }
        }

    }

    private static class ScissorImpl extends CommandSetting<ScissorArea> {

        @Override
        protected void apply(CommandBuffer cmd, ScissorArea value) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkRect2D.Buffer scissor = VkRect2D.malloc(1, stack);
                scissor.offset().set(value.getX(), value.getY());
                scissor.extent().set(value.getWidth(), value.getHeight());
                vkCmdSetScissor(cmd.getBuffer(), 0, scissor);
            }
        }

    }

}

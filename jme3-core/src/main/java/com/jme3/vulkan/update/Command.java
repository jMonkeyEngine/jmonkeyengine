package com.jme3.vulkan.update;

import com.jme3.vulkan.commands.CommandBuffer;

public interface Command {

    /**
     * Checks if this command currently requires a {@link CommandBuffer}
     * on {@link #run(CommandBuffer, int) run}. If no commands in the
     * {@link CommandBatch batch} require a CommandBuffer, then {@code null}
     * is passed as the CommandBuffer on run.
     *
     * @param frame current frame index
     * @return true if this command requires a CommandBuffer on run
     */
    boolean requiresCommandBuffer(int frame);

    /**
     * Runs this command.
     *
     * <p>If no commands in the {@link CommandBatch} {@link #requiresCommandBuffer(int)
     * require a CommandBuffer} on run, the null is passed as {@code cmd}.</p>
     *
     * @param cmd CommandBuffer to submit Vulkan commands to, or null if no
     *            commands require a CommandBuffer.
     * @param frame current frame index
     */
    void run(CommandBuffer cmd, int frame);

    /**
     * Checks if this command should be removed if {@code command}
     * is submitted for removal.
     *
     * @param command command asserted for removal
     * @return true if this command should be removed
     */
    default boolean removeByCommand(Command command) {
        return this == command;
    }

}

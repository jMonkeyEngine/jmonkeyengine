package com.jme3.vulkan.concurrent;

import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.pipeline.PipelineStage;
import com.jme3.vulkan.sync.TimelineSemaphore;
import com.jme3.vulkan.util.Flag;

import java.util.Collection;
import java.util.LinkedList;

/**
 * Tracks the most recent GPU accesses made to a single resource (a buffer region, an
 * image, etc.) so that a command buffer about to access it can be ordered after the
 * command buffers it would otherwise race with, using semaphores instead of a CPU-side
 * wait.
 * <p>
 * Usage is two calls per command buffer, bracketing its submission:
 * <ol>
 *     <li>{@link #await(CommandBuffer, Access, Flag)}, called while recording, inserts
 *     a wait on whatever earlier access this one would race with.</li>
 *     <li>{@link #record(CommandBuffer, Access)}, called only after that command buffer
 *     has actually been submitted, makes it the new most recent access for the next
 *     caller.</li>
 * </ol>
 * Reads never wait on other reads, so independent command buffers that only read the
 * resource can still run concurrently on the device. A write waits on every read
 * recorded since the last write (and then supersedes them), and any access, read or
 * write, waits on the most recent write.
 */
@Deprecated
public class CommandDataAccess {

    private TimelineSemaphore.SignalEvent pendingWrite;
    private final Collection<TimelineSemaphore.SignalEvent> pendingReads = new LinkedList<>();

    /**
     * Inserts a wait on {@code cmd} for every previously recorded access that
     * {@code access} would race with. Must be called while {@code cmd} is being
     * recorded, before it is submitted, and before {@link #record(CommandBuffer, Access)}
     * is called for this same access.
     *
     * @param cmd command buffer about to access the resource
     * @param access the kind of access {@code cmd} will perform
     * @param stageMask pipeline stage(s) of {@code cmd} that perform the access
     */
    public void await(CommandBuffer cmd, Access access, Flag<PipelineStage> stageMask) {
        if (pendingWrite != null) {
            cmd.await(pendingWrite.getSemaphore(), stageMask);
        }
        if (access.isWrite()) {
            pruneCompleted();
            for (TimelineSemaphore.SignalEvent read : pendingReads) {
                cmd.await(read.getSemaphore(), stageMask);
            }
        }
    }

    /**
     * Records {@code cmd} as the most recent accessor of the resource. Must only be
     * called after {@code cmd} has been submitted, so its completion signal's target
     * payload reflects this specific submission rather than some earlier or later one.
     *
     * @param cmd command buffer that just accessed the resource
     * @param access the kind of access {@code cmd} performed
     */
    public void record(CommandBuffer cmd, Access access) {
        pruneCompleted();
        TimelineSemaphore.SignalEvent signal = cmd.getCompletionSignal().createEvent();
        if (access.isWrite()) {
            pendingWrite = signal;
            pendingReads.clear();
        } else {
            pendingReads.add(signal);
        }
    }

    public boolean isFree() {
        pruneCompleted();
        return pendingWrite == null && pendingReads.isEmpty();
    }

    private void pruneCompleted() {
        if (pendingWrite.signaled()) {
            pendingWrite = null;
        }
        pendingReads.removeIf(TimelineSemaphore.SignalEvent::signaled);
    }

}

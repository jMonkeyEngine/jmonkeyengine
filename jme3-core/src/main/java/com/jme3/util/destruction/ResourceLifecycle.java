package com.jme3.util.destruction;

import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.concurrent.Access;
import com.jme3.vulkan.pipeline.PipelineStage;
import com.jme3.vulkan.sync.TimelineSemaphore;
import com.jme3.vulkan.util.Flag;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tracks one resource's outstanding GPU access and drives its destruction.
 * <p>
 * A resource owns one node, constructed with a condition that reports when the resource
 * itself is safe to destroy for reasons outside of GPU access (e.g. not currently bound
 * as a swapchain image) and the raw, unconditional destroyer (e.g. a
 * {@code vkDestroy*}/{@code vmaDestroy*} call). Whenever a command buffer reads or writes
 * the resource, bracket that access with {@link #awaitPreviousAccesses} (while recording, before
 * submission) and {@link #recordDeviceAccess} (after submission); this node will then refuse to run
 * its raw destroyer until that access has completed on the device.
 * <p>
 * If other resources are created from this one and must not outlive it (e.g. buffers
 * allocated from a device), register their nodes with {@link #addDependent} at the point
 * they are created. Calling {@link #destroy()} immediately calls {@code destroy()} on
 * every registered dependent (cascading their own deferral), then schedules this node's
 * raw destroyer to run once this node's own {@code ready} condition is satisfied, its
 * tracked GPU access has settled, and every dependent has actually finished running its
 * raw destroyer.
 * <p>
 * If {@link #destroy()} is never called and the resource becomes unreachable,
 * {@link #flush()} catches it via its {@code referent}'s phantom reachability, logs a
 * warning, and runs it through the same destruction path as a backstop against leaks.
 * <p>
 * Registering a dependent and calling {@code destroy()} on the same node are assumed to
 * happen from a single thread (or under external synchronization); only {@link #flush()}
 * is safe to call concurrently with those.
 */
public class ResourceLifecycle {

    private static final Logger logger = Logger.getLogger(ResourceLifecycle.class.getName());

    private static final ReferenceQueue<Object> leaked = new ReferenceQueue<>();
    private static final Map<Reference<?>, ResourceLifecycle> alive = new ConcurrentHashMap<>();
    private static final Collection<ResourceLifecycle> scheduled = new ConcurrentLinkedDeque<>();

    private static final BooleanSupplier TRUE = () -> true;

    /**
     * Runs every scheduled node whose destruction conditions are now satisfied, and
     * catches any node whose referent was garbage-collected without an explicit
     * {@link #destroy()} call. Must be called periodically (e.g. once per frame); none
     * of this class's deferred destruction or GC backstop happens on its own otherwise.
     */
    public static void flush() {
        while (scheduled.removeIf(ResourceLifecycle::tryRunDestroyer));
        for (Reference<?> ref; (ref = leaked.poll()) != null; ) {
            ResourceLifecycle node = alive.remove(ref);
            if (node != null && !node.destroying.get()) {
                logger.log(Level.WARNING, "Resource was garbage-collected without an explicit destroy() call.");
                node.destroy();
            }
        }
    }

    private final BooleanSupplier condition;
    private final Runnable rawDestroy;

    /** Nodes this one requires to exist; notified to decrement their {@link #pendingDependents} once this node's raw destroyer has actually run. */
    private final List<ResourceLifecycle> dependencies = new ArrayList<>(0);
    /** Nodes that require this one to exist; destroyed (cascaded) as soon as this node is. */
    private final List<ResourceLifecycle> dependents = new ArrayList<>(0);
    /** Count of {@link #dependents} whose raw destroyer has not yet run. */
    private final AtomicInteger pendingDependents = new AtomicInteger();

    private final AtomicBoolean destroying = new AtomicBoolean(false);
    private final Reference<Object> leakRef;

    private TimelineSemaphore.SignalEvent pendingDeviceWrite;
    private final Collection<TimelineSemaphore.SignalEvent> pendingDeviceReads = new LinkedList<>();

    /**
     * @param referent the actual resource object, watched for premature collection; must
     *                  not be strongly reachable from {@code ready} or {@code rawDestroy},
     *                  or it can never become phantom-reachable and the GC backstop will
     *                  never trigger for it
     * @param condition reports true once this resource is safe to destroy for reasons outside
     *              of GPU access (tracked separately via {@link #awaitPreviousAccesses}/{@link #recordDeviceAccess})
     *              and outside of {@link #dependents}; pass {@code () -> true} if there
     *              is no such extra condition
     * @param destructor the actual, unconditional native destroy call for this resource
     */
    public ResourceLifecycle(Object referent, BooleanSupplier condition, Runnable destructor) {
        this.condition = condition;
        this.rawDestroy = destructor;
        this.leakRef = new PhantomReference<>(referent, leaked);
        alive.put(leakRef, this);
    }

    public ResourceLifecycle(Object referent, Runnable destructor) {
        this(referent, TRUE, destructor);
    }

    /**
     * Registers {@code dependent} so that destroying this node also destroys
     * {@code dependent}, and so that this node's own raw destroyer does not run until
     * {@code dependent}'s has. Must be called before this node's own {@link #destroy()}:
     * the cascade in {@code destroy()} only reaches dependents registered before it ran,
     * so registering one afterward would leak it (its parent would wait forever for a
     * decrement that never comes).
     *
     * @param dependent node of a resource that must not outlive this one
     * @throws IllegalStateException if this node's destruction has already begun
     */
    public void addDependent(ResourceLifecycle dependent) {
        if (destroying.get()) {
            throw new IllegalStateException("Cannot add a dependent to a node that is already being destroyed.");
        }
        dependents.add(dependent);
        pendingDependents.incrementAndGet();
        dependent.dependencies.add(this);
    }

    /**
     * Marks this resource for destruction. Cascades immediately to every registered
     * dependent (each begins its own deferred destruction), then schedules this node's
     * raw destroyer to run on the next {@link #flush()} that finds it ready. Calling this
     * more than once on the same node has no additional effect.
     */
    public void destroy() {
        if (!destroying.getAndSet(true)) {
            alive.remove(leakRef);
            leakRef.clear();
            for (ResourceLifecycle dependent : dependents) {
                dependent.destroy();
            }
            scheduled.add(this);
        }
    }

    /**
     * Runs this node's raw destroyer and notifies its dependencies if every condition
     * this node was waiting on is currently satisfied.
     *
     * @return true if the raw destroyer ran, so this node can be dropped from the
     *         schedule
     */
    private boolean tryRunDestroyer() {
        if (!isReadyToDestroy()) {
            return false;
        }
        rawDestroy.run();
        for (ResourceLifecycle dependency : dependencies) {
            dependency.pendingDependents.decrementAndGet();
        }
        return true;
    }

    private boolean isReadyToDestroy() {
        return condition.getAsBoolean() && isFree() && pendingDependents.get() == 0;
    }

    /**
     * Returns true if the device access tracked via {@link #awaitPreviousAccesses}/{@link #recordDeviceAccess} has
     * fully settled, i.e. no in-flight command buffer still reads or writes this
     * resource.
     *
     * @return true if no GPU access on this resource is currently outstanding
     */
    public boolean isFree() {
        pruneCompletedDeviceAccesses();
        return pendingDeviceWrite == null && pendingDeviceReads.isEmpty();
    }

    /**
     * Inserts a wait on {@code cmd} for every previously recorded access that
     * {@code access} would race with. Must be called while {@code cmd} is being
     * recorded, before it is submitted, and before {@link #recordDeviceAccess(CommandBuffer, Access)}
     * is called for this same access.
     * <p>
     * Reads never wait on other reads, so independent command buffers that only read the
     * resource can still run concurrently on the device. A write waits on every read
     * recorded since the last write, and any access, read or write, waits on the most
     * recent write.
     *
     * @param cmd command buffer about to access the resource
     * @param access the kind of access {@code cmd} will perform
     * @param stageMask pipeline stage(s) of {@code cmd} that perform the access
     */
    public void awaitPreviousAccesses(CommandBuffer cmd, Access access, Flag<PipelineStage> stageMask) {
        pruneCompletedDeviceAccesses();
        if (pendingDeviceWrite != null) {
            cmd.await(pendingDeviceWrite.getSemaphore(), stageMask);
        }
        if (access.isWrite()) for (TimelineSemaphore.SignalEvent read : pendingDeviceReads) {
            cmd.await(read.getSemaphore(), stageMask);
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
    public void recordDeviceAccess(CommandBuffer cmd, Access access) {
        pruneCompletedDeviceAccesses();
        TimelineSemaphore.SignalEvent signal = cmd.getCompletionSignal().createEvent();
        if (access.isWrite()) {
            pendingDeviceWrite = signal;
            pendingDeviceReads.clear();
        } else {
            pendingDeviceReads.add(signal);
        }
    }

    /**
     * Drops any tracked access whose completion signal has already fired.
     */
    private void pruneCompletedDeviceAccesses() {
        if (pendingDeviceWrite != null && pendingDeviceWrite.signaled()) {
            pendingDeviceWrite = null;
        }
        pendingDeviceReads.removeIf(TimelineSemaphore.SignalEvent::signaled);
    }

}

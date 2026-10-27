package com.jme3.util.struct;

import com.jme3.vulkan.buffer.BufferCopy;
import com.jme3.vulkan.buffer.DataBuffer;
import com.jme3.vulkan.buffer.GpuBuffer;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.commands.OpLocation;
import com.jme3.vulkan.commands.Commands;

import java.util.Iterator;
import java.util.function.IntFunction;

/**
 * A {@link StructArray} that owns its own backing {@link GpuBuffer} and grows it on
 * demand, the way an {@link java.util.ArrayList} grows its backing array.
 * <p>
 * {@link #length()} reports how many elements have actually been {@link #add() added}
 * (like {@code ArrayList.size()}), which may be less than how many elements currently
 * fit in the attached buffer. When {@link #add()} would exceed that physical capacity,
 * a new, larger buffer is allocated via the {@code bufferFactory} given at construction,
 * the elements in use are copied from the old buffer into it, the old buffer is
 * destroyed, and the new buffer becomes the one this array is attached to.
 * <p>
 * <b>Device coherency on resize.</b> The copy from the old buffer to the new one is
 * requested with {@link OpLocation#PreferHost}, not {@link OpLocation#DontCare}, which
 * matters more than it looks. When both buffers are host-accessible,
 * {@link CommandBuffer#cmdCopy(GpuBuffer, GpuBuffer, BufferCopy, OpLocation)} performs
 * {@code PreferHost} as an immediate, synchronous host memory copy, so by the time
 * {@link #grow(int)} returns the new buffer already holds every element that was in the
 * old one, and it is safe to write new elements into it right away. {@code DontCare}
 * would not guarantee that: {@code cmdCopy} is free to decide a recorded device copy is
 * more appropriate even when both buffers are mappable (its heuristic is about cache
 * locality, not about whether a host copy is possible), and a device copy does not
 * actually run until some later point on the device. A direct host write into the new
 * buffer's mapped memory made before then has no ordering against that copy; if the
 * copy then executed afterward, it would silently overwrite the write with the old,
 * stale data. Forcing the host path whenever one is actually available closes this off.
 * <p>
 * When at least one of the two buffers is not host-accessible, {@code cmdCopy} still
 * falls back to a recorded device copy, but there is no mapped memory for a caller to
 * race against directly in that case: the new buffer's {@link #cache()} would throw, so
 * the only way to touch it at all is through further command-buffer-recorded
 * operations, which order correctly against the resize copy via
 * {@link CommandBuffer#useResource}. That same registration is also what makes it safe
 * to {@link GpuBuffer#destroy() destroy} the old buffer immediately after submitting
 * the copy: destruction is deferred by its {@link com.jme3.util.destruction.ResourceLifecycle}
 * until the recorded read against it settles, so its memory cannot be freed out from
 * under a copy the device hasn't gotten to yet.
 *
 * @param <T> struct type stored in this array
 */
public class StructList <T extends Struct> implements StructArray<T> {

    private final T struct;
    private final IntFunction<GpuBuffer> bufferFactory;

    private GpuBuffer buffer;
    private int alignment, stride;
    private int capacity;
    private int length;
    private int currentIndex = -1;

    /**
     * @param struct reusable view bound to whichever element {@link #index(int)} is
     *               asked for; also determines this array's element layout
     * @param initialCapacity number of elements the initial buffer has room for
     * @param bufferFactory allocates a new buffer of the given byte capacity; called once
     *                      up front and again every time this array outgrows its current
     *                      buffer
     */
    public StructList(T struct, int initialCapacity, IntFunction<GpuBuffer> bufferFactory) {
        this.struct = struct;
        this.capacity = Math.max(initialCapacity, 1);
        this.bufferFactory = bufferFactory;
    }

    @Override
    public void setupLayout(StructLayout layout) {
        struct.setupLayout(layout);
        alignment = layout.getArrayElementAlignment(struct.alignment());
        stride = layout.getArrayElementSize(alignment, struct.size());
        buffer = bufferFactory.apply(capacity * stride);
    }

    /**
     * Unsupported: this array allocates and grows its own buffer, so it cannot also be
     * bound at some offset into a buffer owned by something else — growing would then
     * require resizing whatever that buffer is part of, which this class has no way to
     * do.
     */
    @Override
    public void bind(GpuBuffer buffer, int offset) {
        throw new UnsupportedOperationException(
                "GrowableStructArray manages its own buffer and cannot be bound into another.");
    }

    @Override
    public void unbind() {
        // Nothing external is bound; destroy() releases the buffer this array owns.
    }

    @Override
    public DataBuffer cache() {
        return buffer.cache().slice(0, length * stride);
    }

    @Override
    public int alignment() {
        return alignment;
    }

    @Override
    public int size() {
        return length * stride;
    }

    @Override
    public T index(int i) {
        checkIndex(i);
        return bindAt(i, struct);
    }

    @Override
    public T index(int i, T struct) {
        checkIndex(i);
        return bindAt(i, struct);
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public int stride() {
        return stride;
    }

    @Override
    public T getStruct() {
        return struct;
    }

    /**
     * Gets the number of elements the attached buffer currently has room for, growing
     * only when {@link #add()} needs more than this.
     *
     * @return current element capacity
     */
    public int capacity() {
        return capacity;
    }

    /**
     * Gets the buffer this array is currently attached to. The returned reference may
     * become stale the next time this array grows; callers that need to bind this
     * array's storage elsewhere (e.g. into a descriptor set) should call this again
     * immediately before doing so rather than caching the result.
     *
     * @return the currently attached buffer
     */
    public GpuBuffer getBuffer() {
        return buffer;
    }

    /**
     * Appends a new element, growing the attached buffer first if it has no room left,
     * and returns {@code struct} (the view given at construction) bound to it.
     * <p>
     * Structs are views over native memory rather than plain values, so unlike
     * {@code ArrayList.add}, this does not take a value to insert; it hands back a
     * mutable view over the new slot for the caller to write into.
     *
     * @return this array's struct view, bound to the newly appended element
     */
    public T add() {
        return add(struct);
    }

    /**
     * Equivalent to {@link #add()}, but binds {@code struct} to the new slot instead of
     * this array's own view, the same distinction {@link #index(int, Struct)} makes
     * from {@link #index(int)}.
     *
     * @param struct view to bind to the newly appended element
     * @return {@code struct}, bound to the newly appended element
     */
    public T add(T struct) {
        ensureCapacity(length + 1);
        T bound = bindAt(length, struct);
        length++;
        return bound;
    }

    /**
     * Resets {@link #length()} to zero without shrinking the attached buffer's capacity,
     * the same way {@code ArrayList.clear()} leaves its backing array's capacity alone.
     */
    public void clear() {
        length = 0;
    }

    /**
     * Grows the attached buffer, if necessary, so that it has room for at least
     * {@code minCapacity} elements.
     *
     * @param minCapacity minimum element capacity required
     */
    public void ensureCapacity(int minCapacity) {
        if (minCapacity <= capacity) {
            return;
        }
        grow(Math.max(minCapacity, capacity + (capacity >> 1) + 1));
    }

    public void ensureLength(int minLength) {
        if (minLength > length) {
            ensureCapacity(minLength);
            length = minLength;
        }
    }

    private T bindAt(int i, T target) {
        if (i != currentIndex) {
            target.bind(buffer, i * stride);
            currentIndex = i;
        }
        return target;
    }

    private void checkIndex(int i) {
        if (i < 0 || i >= length) {
            throw new IndexOutOfBoundsException("Index " + i + " out of bounds for length " + length);
        }
    }

    /**
     * Allocates a new, larger buffer, copies every element currently in use into it,
     * and destroys the old buffer. See this class's documentation for why destroying
     * the old buffer immediately after submitting the copy is safe even when the copy
     * is actually performed asynchronously on the device.
     *
     * @param newCapacity element capacity of the new buffer
     */
    private void grow(int newCapacity) {
        GpuBuffer old = buffer;
        GpuBuffer grown = bufferFactory.apply(newCapacity * stride);
        int usedBytes = length * stride;
        if (usedBytes > 0) {
            Commands.get().cmdCopy(old, grown, new BufferCopy().add(0, 0, usedBytes), OpLocation.PreferHost);
        }
        buffer = grown;
        capacity = newCapacity;
        currentIndex = -1;
        struct.unbind();
        old.destroy();
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {

            private int i = 0;

            @Override
            public boolean hasNext() {
                return i < length;
            }

            @Override
            public T next() {
                return index(i++);
            }

        };
    }

}

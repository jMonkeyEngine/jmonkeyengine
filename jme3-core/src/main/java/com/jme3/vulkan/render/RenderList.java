package com.jme3.vulkan.render;

import com.jme3.util.collections.IntComparator;
import com.jme3.util.collections.PowerSort;

import java.util.Arrays;
import java.util.function.IntPredicate;

/**
 * Ordered set of node ids to render, built to be kept from pass to pass without allocating.
 * <p>
 * Each node is in the list at most once. Membership is tracked with one bit per node id, so
 * {@link #contains(int)} is constant time and {@link #add(int)} skips nodes already present.
 * Instead of being cleared and refilled each pass, which would lose the order the previous pass
 * sorted the nodes in, the list is meant to be updated: {@link #removeIf(IntPredicate)} drops the
 * nodes no longer to be rendered while keeping the order of the rest, and {@link #add(int)} appends
 * the new ones, which the next sort places for little cost. E.g. with {@code visible} being this
 * pass's culled nodes:
 * <pre>{@code
 * list.removeIf(node -> !visible.contains(node));
 * visible.intStream().forEach(list::add);
 * }</pre>
 * <p>
 * Nodes are handed to techniques through {@link Section sections}: a section is a range of the
 * list that a technique may sort in place and {@link Section#markRendered(int) mark} the nodes of
 * that it rendered. {@link Section#commit() Committing} the section stably partitions it so the
 * rendered nodes occupy the front of the range, followed by the nodes the technique left alone,
 * each group keeping the order the technique left it in.
 * <p>
 * Techniques are expected to render a section's nodes in the order they sort them in, so the
 * section order of the rendered nodes is their render order. (Only positions are marked, not the
 * order of marking.) Giving each technique in turn a section of what the previous ones left
 * therefore leaves each range holding its rendered nodes first, in render order and grouped by
 * technique, followed by the nodes no technique rendered. Kept for the next pass from a similar
 * view, the list is then already almost sorted, which powersort takes advantage of to sort it in
 * close to linear time:
 * <pre>{@code
 * int start = bucketStart;
 * for (ShadingTechnique t : techniques) {
 *     RenderList.Section s = list.open(start, bucketEnd);
 *     t.render(cmd, vp, s);       // sorts s, renders nodes, marks them
 *     start += s.commit();        // [start, start + rendered) now holds what t rendered
 * }
 * }</pre>
 * Only one section can be open at a time, and the list must not be modified while one is open.
 * <p>
 * Memory: besides the node array, the list keeps one bit per node id up to the highest id added
 * for membership, one bit per node of the largest section opened so far for rendered marks, and one scratch array, shared between sorting and partitioning, of
 * roughly the length of that section. Both only grow, and are reused every frame; {@link #trimToSize()}
 * releases them.
 */
public final class RenderList {

    private static final int[] EMPTY = new int[0];
    private static final long[] EMPTY_MARKS = new long[0];

    private int[] nodes;
    private int size = 0;
    private int[] scratch = EMPTY;
    private long[] marks = EMPTY_MARKS;
    private long[] members = EMPTY_MARKS; // bit per node id
    private final Section section = new Section();

    public RenderList() {
        this(64);
    }

    public RenderList(int initialCapacity) {
        nodes = initialCapacity > 0 ? new int[initialCapacity] : EMPTY;
    }

    /**
     * Appends {@code node} if it is not already in the list.
     *
     * @param node node id, at least 0
     * @return true if {@code node} was added
     */
    public boolean add(int node) {
        checkClosed();
        if (!addMember(node)) {
            return false;
        }
        if (size == nodes.length) {
            nodes = Arrays.copyOf(nodes, Math.max(16, size << 1));
        }
        nodes[size++] = node;
        return true;
    }

    /**
     * Replaces the node at {@code index} with {@code node}.
     *
     * @param index index to replace
     * @param node node id, at least 0
     * @throws IllegalArgumentException if {@code node} is already in the list at another index
     */
    public void set(int index, int node) {
        checkClosed();
        checkIndex(index, size);
        int old = nodes[index];
        if (old == node) {
            return;
        }
        if (!addMember(node)) {
            throw new IllegalArgumentException("Node " + node + " is already in the list.");
        }
        removeMember(old);
        nodes[index] = node;
    }

    /**
     * @param node node id
     * @return true if {@code node} is in the list
     */
    public boolean contains(int node) {
        int word = node >>> 6; // negative ids land far out of range
        return word < members.length && (members[word] & (1L << node)) != 0;
    }

    /**
     * Removes {@code node}, keeping the order of the remaining nodes. Linear time; to remove many
     * nodes, {@link #removeIf(IntPredicate)} is faster.
     *
     * @param node node id
     * @return true if {@code node} was in the list
     */
    public boolean remove(int node) {
        checkClosed();
        if (!contains(node)) {
            return false;
        }
        for (int i = 0; i < size; i++) {
            if (nodes[i] == node) {
                System.arraycopy(nodes, i + 1, nodes, i, size - i - 1);
                size--;
                removeMember(node);
                return true;
            }
        }
        throw new AssertionError("Membership out of sync with nodes.");
    }

    /**
     * Removes every node {@code filter} accepts in one linear pass, keeping the order of the
     * remaining nodes. If {@code filter} throws, the nodes it has already accepted are removed
     * and the list stays consistent.
     *
     * @param filter accepts the nodes to remove
     * @return number of nodes removed
     */
    public int removeIf(IntPredicate filter) {
        checkClosed();
        int[] a = nodes;
        int write = 0, read = 0;
        try {
            for (; read < size; read++) {
                int node = a[read];
                if (filter.test(node)) {
                    removeMember(node);
                } else {
                    a[write++] = node;
                }
            }
        } finally {
            if (read < size) {
                // filter threw: keep the untested nodes
                System.arraycopy(a, read, a, write, size - read);
                write += size - read;
            }
            read = size - write;
            size = write;
        }
        return read;
    }

    public int get(int index) {
        checkIndex(index, size);
        return nodes[index];
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Removes every node, keeping all allocated memory. Clearing loses the order the nodes were
     * sorted in, so a list kept across passes should be updated with {@link #removeIf(IntPredicate)}
     * instead.
     */
    public void clear() {
        checkClosed();
        for (int i = 0; i < size; i++) {
            removeMember(nodes[i]);
        }
        size = 0;
    }

    /**
     * Ensures {@code capacity} nodes fit without growing, e.g. before filling the list with a
     * known number of nodes.
     *
     * @param capacity minimum capacity
     */
    public void ensureCapacity(int capacity) {
        if (capacity > nodes.length) {
            nodes = Arrays.copyOf(nodes, capacity);
        }
    }

    /**
     * Shrinks the node array to the current size and the membership bits to the highest node id
     * present, and releases the scratch and mark memory, e.g. after a spike in rendered nodes.
     * Scratch and marks are allocated again by the next section that needs them.
     */
    public void trimToSize() {
        checkClosed();
        if (nodes.length != size) {
            nodes = size > 0 ? Arrays.copyOf(nodes, size) : EMPTY;
        }
        int words = members.length;
        while (words > 0 && members[words - 1] == 0) {
            words--;
        }
        if (words != members.length) {
            members = words > 0 ? Arrays.copyOf(members, words) : EMPTY_MARKS;
        }
        scratch = EMPTY;
        marks = EMPTY_MARKS;
    }

    /**
     * Stably sorts {@code [fromIndex, toIndex)} with powersort.
     *
     * @param fromIndex index of the first node to sort
     * @param toIndex index after the last node to sort
     * @param comparator node order
     */
    public void sort(int fromIndex, int toIndex, IntComparator comparator) {
        checkClosed();
        checkRange(fromIndex, toIndex);
        ensureScratch(toIndex - fromIndex);
        PowerSort.sort(nodes, fromIndex, toIndex, comparator, scratch);
    }

    /**
     * Opens a section over {@code [fromIndex, toIndex)} to hand to a technique. The returned object
     * is reused by every call, so it is only valid until {@link Section#commit()}.
     *
     * @param fromIndex index of the section's first node
     * @param toIndex index after the section's last node
     * @return the open section
     * @throws IllegalStateException if a section is already open
     */
    public Section open(int fromIndex, int toIndex) {
        checkClosed();
        checkRange(fromIndex, toIndex);
        int length = toIndex - fromIndex;
        ensureScratch(length);
        int words = (length + 63) >>> 6;
        if (marks.length < words) {
            marks = new long[words];
        } else {
            Arrays.fill(marks, 0, words, 0L);
        }
        section.open(fromIndex, length);
        return section;
    }

    /**
     * Sets {@code node}'s membership bit.
     *
     * @return false if it was already set
     */
    private boolean addMember(int node) {
        if (node < 0) {
            throw new IllegalArgumentException("Node id must be at least 0: " + node);
        }
        int word = node >>> 6;
        if (word >= members.length) {
            members = Arrays.copyOf(members, Math.max(word + 1, members.length << 1));
        }
        long bit = 1L << node;
        if ((members[word] & bit) != 0) {
            return false;
        }
        members[word] |= bit;
        return true;
    }

    private void removeMember(int node) {
        members[node >>> 6] &= ~(1L << node);
    }

    /**
     * Grows the scratch array to fit both sorting and partitioning {@code length} nodes.
     */
    private void ensureScratch(int length) {
        int required = Math.max(length, PowerSort.scratchLength(length));
        if (scratch.length < required) {
            // grow geometrically so a slowly growing scene doesn't reallocate every frame
            scratch = new int[Math.max(required, Math.min(scratch.length << 1, nodes.length))];
        }
    }

    private void checkClosed() {
        if (section.open) {
            throw new IllegalStateException("A section is open.");
        }
    }

    private void checkRange(int fromIndex, int toIndex) {
        if (fromIndex < 0 || toIndex > size || fromIndex > toIndex) {
            throw new IndexOutOfBoundsException("from " + fromIndex + ", to " + toIndex + ", size " + size);
        }
    }

    private static void checkIndex(int index, int length) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException("index " + index + ", length " + length);
        }
    }

    /**
     * Range of a {@link RenderList} handed to one technique. Indices are relative to the start
     * of the section. The technique may reorder the section's nodes freely (with {@link #sort}
     * or {@link #swap}), then renders nodes in section order and {@link #markRendered marks} each
     * node it renders; whoever opened the section then {@link #commit() commits} it.
     * <p>
     * {@link #partition(IntPredicate)} narrows a section down to a subsection. Subsections start
     * at the same position as the section opened from the list, so an index means the same node
     * in all of them, and they share its rendered marks. A subsection stays usable until the
     * section opened from the list is committed.
     */
    public final class Section {

        private final Section root; // the section opened from the list, which holds the state shared with its subsections
        private int from, length;
        private int generation; // for a subsection, the opening of root it belongs to
        private int rendered; // root only
        private boolean open = false; // root only

        private Section() {
            this.root = this;
        }

        private Section(Section root, int from, int length) {
            this.root = root;
            this.from = from;
            this.length = length;
            this.generation = root.generation;
        }

        private void open(int from, int length) {
            this.from = from;
            this.length = length;
            this.rendered = 0;
            this.open = true;
            generation++;
        }

        /**
         * @return number of nodes in this section
         */
        public int size() {
            return length;
        }

        public boolean isEmpty() {
            return length == 0;
        }

        /**
         * @param index index within this section
         * @return node at {@code index}
         */
        public int get(int index) {
            checkOpen();
            checkIndex(index, length);
            return nodes[from + index];
        }

        /**
         * Stably sorts the whole section with powersort. Rendered marks belong to positions, not
         * nodes, so sorting should happen before any node is marked.
         *
         * @param comparator node order
         */
        public void sort(IntComparator comparator) {
            sort(0, length, comparator);
        }

        /**
         * Stably sorts {@code [fromIndex, toIndex)} of this section with powersort, e.g. to sort
         * each pipeline's group separately.
         *
         * @param fromIndex section index of the first node to sort
         * @param toIndex section index after the last node to sort
         * @param comparator node order
         */
        public void sort(int fromIndex, int toIndex, IntComparator comparator) {
            checkOpen();
            if (fromIndex < 0 || toIndex > length || fromIndex > toIndex) {
                throw new IndexOutOfBoundsException("from " + fromIndex + ", to " + toIndex + ", length " + length);
            }
            checkUnmarked();
            PowerSort.sort(nodes, from + fromIndex, from + toIndex, comparator, scratch);
        }

        /**
         * Swaps two nodes of this section.
         */
        public void swap(int i, int j) {
            checkOpen();
            checkIndex(i, length);
            checkIndex(j, length);
            checkUnmarked();
            int t = nodes[from + i];
            nodes[from + i] = nodes[from + j];
            nodes[from + j] = t;
        }

        /**
         * Stably partitions this section so the nodes {@code filter} accepts come first, followed
         * by the rest, each group keeping its order, and gets a subsection of just the accepted
         * nodes, e.g. to set aside the nodes a technique can't render. Index {@code i} of the
         * subsection is index {@code i} of this section. If {@code filter} throws, every node is
         * kept: the accepted ones first, then the untested and rejected ones.
         *
         * @param filter accepts the nodes the subsection contains
         * @return subsection of the accepted nodes
         * @throws IllegalStateException if any node is already marked rendered
         */
        public Section partition(IntPredicate filter) {
            checkOpen();
            checkUnmarked();
            int[] a = nodes;
            int end = from + length, read = from, write = from, rejected = 0;
            try {
                for (; read < end; read++) {
                    int node = a[read];
                    if (filter.test(node)) {
                        a[write++] = node;
                    } else {
                        scratch[rejected++] = node;
                    }
                }
            } finally {
                // only non-zero if filter threw
                int untested = end - read;
                System.arraycopy(a, read, a, write, untested);
                System.arraycopy(scratch, 0, a, write + untested, rejected);
            }
            return new Section(root, from, write - from);
        }

        /**
         * Marks the node at {@code index} as rendered. Marking a node twice has no further effect.
         *
         * @param index index within this section
         */
        public int markRendered(int index) {
            checkOpen();
            checkIndex(index, length);
            long bit = 1L << index; // shifts use only the low 6 bits
            int word = index >>> 6;
            if ((marks[word] & bit) == 0) {
                marks[word] |= bit;
                root.rendered++;
            }
            return nodes[from + index];
        }

        /**
         * Marks every node in {@code [fromIndex, toIndex)} as rendered, e.g. after drawing them
         * in one instanced call.
         */
        public void markRendered(int fromIndex, int toIndex) {
            checkOpen();
            if (fromIndex < 0 || toIndex > length || fromIndex > toIndex) {
                throw new IndexOutOfBoundsException("from " + fromIndex + ", to " + toIndex + ", length " + length);
            }
            for (int i = fromIndex; i < toIndex;) {
                int word = i >>> 6;
                int end = Math.min(toIndex, (word + 1) << 6);
                // bits i through end - 1 of this word
                long mask = (-1L << i) & (end - (word << 6) == 64 ? -1L : ~(-1L << end));
                root.rendered += Long.bitCount(mask & ~marks[word]);
                marks[word] |= mask;
                i = end;
            }
        }

        public boolean isRendered(int index) {
            checkOpen();
            checkIndex(index, length);
            return (marks[index >>> 6] & (1L << index)) != 0;
        }

        /**
         * @return number of nodes of this section marked rendered so far
         */
        public int getRenderedCount() {
            if (root == this) {
                return rendered;
            }
            checkOpen();
            int count = 0, words = length >>> 6;
            for (int w = 0; w < words; w++) {
                count += Long.bitCount(marks[w]);
            }
            if ((length & 63) != 0) {
                count += Long.bitCount(marks[words] & ~(-1L << length));
            }
            return count;
        }

        /**
         * Closes this section, stably moving the nodes marked rendered to the front of its range:
         * afterwards, list indices {@code [start, start + count)} hold exactly the rendered nodes and
         * {@code [start + count, end)} the rest, both in the order they had in the section.
         *
         * @return number of rendered nodes
         * @throws IllegalStateException if this is a {@link #partition subsection}
         */
        public int commit() {
            checkOpen();
            if (root != this) {
                throw new IllegalStateException("Only the section opened from the list can be committed.");
            }
            open = false;
            int r = rendered;
            if (r == 0 || r == length) {
                return r;
            }
            int[] a = nodes;
            long[] m = marks;
            // the leading rendered nodes are already in place
            int i = 0;
            while ((m[i >>> 6] & (1L << i)) != 0) {
                i++;
            }
            int write = from + i, skipped = 0;
            for (; i < length; i++) {
                int node = a[from + i];
                if ((m[i >>> 6] & (1L << i)) != 0) {
                    a[write++] = node;
                } else {
                    scratch[skipped++] = node;
                }
            }
            System.arraycopy(scratch, 0, a, write, skipped);
            return r;
        }

        private void checkOpen() {
            if (!root.open || generation != root.generation) {
                throw new IllegalStateException("Section is not open.");
            }
        }

        private void checkUnmarked() {
            if (root.rendered > 0) {
                throw new IllegalStateException("Cannot reorder a section after nodes are marked rendered.");
            }
        }

    }

}

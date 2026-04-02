package com.jme3.vulkan.util;

import com.jme3.backend.GraphicsAPI;

import java.util.Iterator;

public interface Flag <T extends Flag> extends Iterable<Integer> {

    int bits();

    default int lowestBit() {
        return Integer.lowestOneBit(bits());
    }

    default int bitCount() {
        return Integer.bitCount(bits());
    }

    default Flag<T> add(Flag flag) {
        if (!contains(flag)) {
            return new FlagImpl<>(bits() | flag.bits());
        } else return this;
    }

    default Flag<T> add(Flag... flags) {
        int result = bits();
        for (Flag f : flags) {
            result |= f.bits();
        }
        return new FlagImpl<>(result);
    }

    default Flag<T> addIf(boolean condition, Flag flag) {
        if (!condition) return this;
        else return add(flag);
    }

    default Flag<T> addIf(boolean condition, Flag... flag) {
        if (!condition) return this;
        else return add(flag);
    }

    default Flag<T> remove(Flag flag) {
        if (containsAny(flag)) {
            return new FlagImpl<>(bits() & ~flag.bits());
        } else return this;
    }

    default Flag<T> remove(Flag... flags) {
        int result = bits();
        for (Flag f : flags) {
            result &= ~f.bits();
        }
        return new FlagImpl<>(result);
    }

    default Flag<T> removeIf(boolean condition, Flag flag) {
        if (condition) return remove(flag);
        else return this;
    }

    default Flag<T> removeIf(boolean condition, Flag... flag) {
        if (condition) return remove(flag);
        else return this;
    }

    default Flag<T> containOnlyIf(boolean condition, Flag flag) {
        if (condition) return add(flag);
        else return remove(flag);
    }

    default Flag<T> containOnlyIf(boolean condition, Flag... flag) {
        if (condition) return add(flag);
        else return remove(flag);
    }

    default Flag<T> and(Flag flag) {
        return new FlagImpl<>(bits() & flag.bits());
    }

    default Flag<T> and(Flag... flag) {
        int bits = bits();
        for (Flag f : flag) {
            bits &= f.bits();
        }
        return new FlagImpl<>(bits);
    }

    default Flag<T> and(int bits) {
        return new FlagImpl<>(bits() & bits);
    }

    default boolean contains(Flag flag) {
        return contains(flag.bits());
    }

    default boolean contains(Flag... flags) {
        for (Flag f : flags) {
            if (!contains(f)) {
                return false;
            }
        }
        return false;
    }

    default boolean contains(int bits) {
        return (bits() & bits) == bits;
    }

    default boolean containsAny(Flag flag) {
        return containsAny(flag.bits());
    }

    default boolean containsAny(Flag... flags) {
        for (Flag f : flags) {
            if (containsAny(f.bits())) {
                return true;
            }
        }
        return false;
    }

    default boolean containsAny(int bits) {
        return (bits() & bits) > 0;
    }

    default boolean isEmpty() {
        return bits() == 0;
    }

    default boolean is(Flag flag) {
        return bits() == flag.bits();
    }

    default boolean is(int bits) {
        return bits() == bits;
    }

    @Override
    @Deprecated
    default Iterator<Integer> iterator() {
        return new IteratorImpl(bits());
    }

    static <T extends Flag> Flag<T> of(int bits) {
        return new FlagImpl<>(bits);
    }

    static <T extends Flag> Flag<T> empty() {
        return of(0);
    }

    static <T extends Flag> Flag<T> of(Flag... flags) {
        return new FlagImpl<>(flags);
    }

    static int bitsOf(Flag... flags) {
        int result = 0;
        for (Flag f : flags) {
            result |= f.bits();
        }
        return result;
    }

    static boolean is(Flag f1, Flag f2) {
        return f1 == f2 || (f1 != null && f2 != null && f1.is(f2));
    }

    static <T extends Flag> Flag<T> combine(Flag... flags) {
        int bits = 0;
        for (Flag f : flags) {
            if (f != null) bits |= f.bits();
        }
        return of(bits);
    }

    class FlagImpl <T extends Flag> implements Flag<T> {

        private final int bits;

        public FlagImpl(int bits) {
            this.bits = bits;
        }

        public FlagImpl(Flag... flags) {
            this.bits = bitsOf(flags);
        }

        @Override
        public int bits() {
            return bits;
        }

    }

    @Deprecated
    class IteratorImpl implements Iterator<Integer> {

        private int bits;

        public IteratorImpl(int bits) {
            this.bits = bits;
        }

        @Override
        public boolean hasNext() {
            return bits != 0;
        }

        @Override
        public Integer next() {
            int bit = Integer.lowestOneBit(bits);
            bits &= ~bit;
            return bit;
        }

    }

}

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

    default boolean contains(Flag flag) {
        return contains(flag.bits());
    }

    default boolean contains(int bits) {
        return (bits() & bits) == bits;
    }

    default boolean containsAny(Flag flag) {
        return containsAny(flag.bits());
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

    static <T extends Flag> int bitsOf(Flag... flags) {
        int result = 0;
        for (Flag f : flags) {
            result |= f.bits();
        }
        return result;
    }

    static boolean is(Flag f1, Flag f2) {
        return f1 == f2 || (f1 != null && f1.is(f2));
    }

    static <T extends Flag> Flag<T> combine(Flag... flags) {
        int bits = 0;
        for (Flag f : flags) {
            if (f != null) bits |= f.bits();
        }
        return of(bits);
    }

    @Deprecated
    static <T extends Flag> AgnosticFlag<T> agnostic() {
        return new AgnosticFlag<>();
    }

    @Deprecated
    static <T extends Flag> AgnosticFlag<T> agnostic(int... bits) {
        return new AgnosticFlag<>(bits);
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

    class AgnosticFlag <T extends Flag> implements Flag<T> {

        private final int[] bits;

        public AgnosticFlag() {
            this.bits = new int[GraphicsAPI.values().length];
        }

        public AgnosticFlag(int... bits) {
            if (bits.length == GraphicsAPI.values().length) {
                this.bits = bits;
            } else {
                this.bits = new int[GraphicsAPI.values().length];
                System.arraycopy(bits, 0, this.bits, 0, bits.length);
            }
        }

        public AgnosticFlag<T> set(GraphicsAPI api, int bits) {
            this.bits[api.ordinal()] = bits;
            return this;
        }

        @Override
        public int bits() {
            return bits[GraphicsAPI.getActiveAPI().ordinal()];
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

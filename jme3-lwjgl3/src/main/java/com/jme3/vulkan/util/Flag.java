package com.jme3.vulkan.util;

public interface Flag <T extends Flag> {

    int bits();

    default Flag<T> add(Flag<T> flag) {
        return new FlagImpl<>(bits() | flag.bits());
    }

    default Flag<T> add(Flag... flags) {
        int result = bits();
        for (Flag f : flags) {
            result |= f.bits();
        }
        return new FlagImpl<>(result);
    }

    default Flag<T> remove(Flag<T> flag) {
        return new FlagImpl<>(bits() & ~flag.bits());
    }

    default Flag<T> remove(Flag... flags) {
        int result = bits();
        for (Flag f : flags) {
            result &= ~f.bits();
        }
        return new FlagImpl<>(result);
    }

    default boolean contains(Flag<T> flag) {
        int bits = flag.bits();
        return (bits() & bits) == bits;
    }

    default boolean containsOneOf(Flag<T> flag) {
        return (bits() & flag.bits()) > 0;
    }

    static <T extends Flag> Flag<T> of(int bits) {
        return new FlagImpl<>(bits);
    }

    static <T extends Flag> Flag<T> none() {
        return of(0);
    }

    @SafeVarargs
    static <T extends Flag> Flag<T> of(Flag<T>... flags) {
        return new FlagImpl<>(flags);
    }

    @SafeVarargs
    static <T extends Flag> int bitsOf(Flag<T>... flags) {
        int result = 0;
        for (Flag<T> f : flags) {
            result |= f.bits();
        }
        return result;
    }

    class FlagImpl <T extends Flag> implements Flag<T> {

        private final int bits;

        public FlagImpl(int bits) {
            this.bits = bits;
        }

        @SafeVarargs
        public FlagImpl(Flag<T>... flags) {
            this.bits = bitsOf(flags);
        }

        @Override
        public int bits() {
            return bits;
        }

    }

}

package com.jme3.vulkan.util;

public interface Flag <T extends Flag> {

    int bits();

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

}

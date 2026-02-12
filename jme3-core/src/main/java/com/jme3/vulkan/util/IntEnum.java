package com.jme3.vulkan.util;

import com.jme3.backend.GraphicsAPI;

import java.util.Objects;

/**
 * Wrapper over an integer enum to enforce type safety.
 *
 * @param <T> type to enforce on the enum
 */
public interface IntEnum <T extends IntEnum> {

    int getEnum();

    default boolean is(IntEnum other) {
        return other != null && is(other.getEnum());
    }

    default boolean is(int value) {
        return getEnum() == value;
    }

    static <T extends IntEnum> IntEnum<T> get(IntEnum<T> intEnum, IntEnum<T> defEnum) {
        return intEnum != null ? intEnum : defEnum;
    }

    static int get(IntEnum intEnum, int defEnum) {
        return intEnum != null ? intEnum.getEnum() : defEnum;
    }

    static <T extends IntEnum> IntEnum<T> of(int intEnum) {
        return new EnumImpl<>(intEnum);
    }

    static boolean is(IntEnum e1, IntEnum e2) {
        return e1 == e2 || (e1 != null && e1.is(e2));
    }

    @Deprecated
    static <T extends IntEnum> AgnosticEnum<T> agnostic() {
        return new AgnosticEnum<>();
    }

    @Deprecated
    static <T extends IntEnum> AgnosticEnum<T> agnostic(int... enums) {
        return new AgnosticEnum<>(enums);
    }

    class EnumImpl <T extends IntEnum> implements IntEnum<T> {

        private final int intEnum;

        public EnumImpl(int intEnum) {
            this.intEnum = intEnum;
        }

        @Override
        public int getEnum() {
            return intEnum;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof IntEnum)) return false;
            IntEnum<?> that = (IntEnum<?>) o;
            return intEnum == that.getEnum();
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(intEnum);
        }

    }

    @Deprecated
    class AgnosticEnum <T extends IntEnum> implements IntEnum<T> {

        private final int[] enums;

        public AgnosticEnum() {
            this.enums = new int[GraphicsAPI.values().length];
        }

        public AgnosticEnum(int... enums) {
            if (enums.length == GraphicsAPI.values().length) {
                this.enums = enums;
            } else {
                this.enums = new int[GraphicsAPI.values().length];
                System.arraycopy(enums, 0, this.enums, 0, enums.length);
            }
        }

        public AgnosticEnum<T> set(GraphicsAPI api, int e) {
            enums[api.ordinal()] = e;
            return this;
        }

        @Override
        public int getEnum() {
            return enums[GraphicsAPI.getActiveAPI().ordinal()];
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof IntEnum)) return false;
            IntEnum<?> that = (IntEnum<?>)o;
            return enums[GraphicsAPI.getActiveAPI().ordinal()] == that.getEnum();
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(enums[GraphicsAPI.getActiveAPI().ordinal()]);
        }

    }

}

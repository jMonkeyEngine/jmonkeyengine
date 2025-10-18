package com.jme3.vulkan.util;

import java.util.Objects;

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
            if (o == null || getClass() != o.getClass()) return false;
            EnumImpl<?> that = (EnumImpl<?>) o;
            return intEnum == that.intEnum;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(intEnum);
        }

    }

}

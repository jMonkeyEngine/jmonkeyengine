package com.jme3.vulkan.util;

public interface IntEnum<T extends IntEnum> {

    int getEnum();

    default boolean is(IntEnum<T> intEnum) {
        return intEnum != null && is(intEnum.getEnum());
    }

    default boolean is(int intEnum) {
        return getEnum() == intEnum;
    }

    static <T extends IntEnum> IntEnum<T> get(IntEnum<T> intEnum, IntEnum<T> defEnum) {
        return intEnum != null ? intEnum : defEnum;
    }

    static <T extends IntEnum> IntEnum<T> of(int libEnum) {
        return new EnumImpl<>(libEnum);
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

    }

}

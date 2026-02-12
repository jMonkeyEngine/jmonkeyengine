package com.jme3.vulkan.struct;

public interface StdLayoutType {

    /**
     * Returns the number of bytes a single value of this type takes up.
     *
     * @return size in bytes
     */
    int getSize();

    /**
     * Gets the base alignment of this type.
     *
     * @return base alignment in bytes
     */
    int getAlignment();

    /**
     * Gets the number of values represented by this type. If count is
     * greater than one, this type should be treated as an array.
     *
     * @return element count (must be positive)
     */
    default int getCount() {
        return 1;
    }

    /**
     * Returns true if this type is an array. That is, {@link #getCount()} is greater
     * than one.
     *
     * @return true if is an array
     */
    default boolean isArray() {
        return getCount() > 1;
    }

    StdLayoutType scalar = new BaseType(4, 4);
    StdLayoutType vec2 = new BaseType(8, 8);
    StdLayoutType vec4 = new BaseType(16, 16);
    StdLayoutType mat3 = new BaseType(12, 16, 3);
    StdLayoutType mat4 = new BaseType(16, 16, 4);

    /**
     * It is not recommended to use this type in std140 structs as
     * implementations do not always follow the std140 spec exactly
     * on how vec3's are packed.
     */
    StdLayoutType vec3 = new BaseType(12, 16);

    static StdLayoutType array(StdLayoutType type, int count) {
        return new ArrayType(type, count);
    }

    class BaseType implements StdLayoutType {

        private final int size, alignment, count;

        public BaseType(int size, int alignment) {
            this(size, alignment, 1);
        }

        public BaseType(int size, int alignment, int count) {
            this.size = size;
            this.alignment = alignment;
            this.count = count;
        }

        @Override
        public int getSize() {
            return size;
        }

        @Override
        public int getAlignment() {
            return alignment;
        }

        @Override
        public int getCount() {
            return count;
        }

    }

    class ArrayType implements StdLayoutType {

        private final StdLayoutType type;
        private final int count;

        public ArrayType(StdLayoutType type, int count) {
            this.type = type;
            this.count = count;
        }

        @Override
        public int getSize() {
            return type.getSize();
        }

        @Override
        public int getAlignment() {
            return type.getAlignment();
        }

        @Override
        public int getCount() {
            return type.getCount() * count;
        }

    }

}

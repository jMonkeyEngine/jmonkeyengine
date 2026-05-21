package com.jme3.vulkan.alloc;

import com.jme3.math.Vector4f;
import com.jme3.util.struct.Struct;
import com.jme3.util.struct.StructField;
import com.jme3.util.struct.SubStructArrayField;
import com.jme3.util.struct.SubStructField;
import com.jme3.vulkan.buffernew.GpuBuffer;

public interface MemoryPointer extends Memory {

    /**
     * Binds this pointer to another memory object so that it references that
     * memory object for future operations. The extent of deferring to the bound memory
     * is up to the implementation. If this pointer is already bound to a memory object,
     * that object should be unbound. If {@code memory} is null, the currently bound memory
     * object should be unbound as normal and nothing should be bound.
     *
     * @param memory memory to bind
     */
    void bind(Memory memory);

    Memory getBoundMemory();

    // todo: remove api test code
    @Deprecated
    public static void exp() {

        StructArray<MyStruct> array = new StructArray<>(new MyStruct(), 100); // struct automatically bound to array
        GpuBuffer data;

        // bind array to data so that mappings/reads/writes will draw from data
        array.bind(data);

        // begin an arena with array mapped
        try (MappingArena arena = new MappingArena(array)) {

            // iterate over struct array
            for (MyStruct s : array) {
                s.value.set(1f);
                s.sub.vec.set(Vector4f.UNIT_X);
                for (Sub e : s.array) {
                    e.vec.set(Vector4f.UNIT_Y);
                }
            }

            // set by index
            array.index(10).value.set(2f);

            // iterate over float field
            FieldArray<StructField<Float>> fArray = array.field(s -> s.value);
            fArray.map(arena); // ensure fArray is mapped (redundent since array is mapped)
            for (StructField<Float> f : fArray) {
                f.set(1f);
            }

            // set by index
            fArray.index(5).set(5f);

            // bind new struct so that value from 10 can be transfered to value from 15
            array.index(15, new MyStruct()).value.set(array.index(10).value.get()); // warning: read operation can be slow depending on memory properties!

            // iterate over struct array field
            for (SubStructArrayField<Sub> e : array.field(s -> s.arrayField)) {
                for (Sub sub : e.get()) {
                    sub.vec.set(Vector4f.UNIT_XYZW);
                }
                e.index(7).vec.set(Vector4f.UNIT_W);
            }
        }

        MyStruct struct = new MyStruct();
        struct.bind(data); // bind struct to data

        try (MappingArena arena = new MappingArena(struct)) {
            struct.value.set(1f);
            struct.sub.get().vec.set(Vector4f.ZERO);
            struct.array.index(6).vec.set(Vector4f.UNIT_Z);
        }

        NativeResource.destroy(array);

    }

    @Deprecated
    class MyStruct extends Struct {

        public final StructField<Float> value = new Field<>(0f);
        public final Sub sub = new Sub();
        public final StructArray<Sub> array = new StructArray<>(new Sub(), 10);

        public final SubStructField<Sub> subField = new SubStructField<>(sub);
        public final SubStructArrayField<Sub> arrayField = new SubStructArrayField<>(array);

        public MyStruct() {
            addFields(value, subField, arrayField);
        }

    }

    @Deprecated
    class Sub extends Struct {

        public final Field<Vector4f> vec = new Field<>(new Vector4f());

        public Sub() {
            addFields(vec);
        }

    }

}

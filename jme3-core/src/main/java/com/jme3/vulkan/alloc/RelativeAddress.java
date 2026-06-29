package com.jme3.vulkan.alloc;

import com.jme3.math.Vector4f;
import com.jme3.util.struct.Struct;
import com.jme3.util.struct.StructField;
import com.jme3.util.struct.SubStructArrayField;
import com.jme3.util.struct.SubStructField;
import com.jme3.vulkan.buffer.EngineBuffer;

public interface RelativeAddress extends MemoryAddress {

    /**
     * Binds this address to a parent address to which this address is relative to.
     *
     * @param parent memory to bind
     */
    void bind(MemoryAddress parent);

    /**
     * Gets the address this address is relative to.
     *
     * @return parent address
     */
    MemoryAddress getParentAddress();

    // todo: remove api test code
    @Deprecated
    public static void exp() {

        StructArray<MyStruct> array = new StructArray<>(new MyStruct(), 100);
        EngineBuffer data = null;

        // bind array to data so that mappings/reads/writes will draw from data
        array.bind(data);

        // pull outside changes
        array.getSourceBuffer().stageAll();
        array.getSourceBuffer().pullStaged();

        // iterate over struct array using a shared struct
        for (MyStruct s : array) {
            s.value.set(1f);
            s.sub.vec.set(Vector4f.UNIT_X);
            for (Sub e : s.array) {
                e.vec.set(Vector4f.UNIT_Y);
            }
        }

        // set by index using a shared struct
        array.index(10).value.set(4f);

        // iterate over float field using a shared struct
        StructArray.Field<StructField<Float>> fArray = array.field(s -> s.value);
        for (StructField<Float> f : fArray) {
            f.set(1f);
        }

        // set by index
        fArray.index(5).set(5f);

        // bind new struct so that value from 10 can be transfered to value from 15
        array.index(15, new MyStruct()).value.set(array.index(10).value.get()); // warning: read operation can be slow depending on memory properties!

        // push changes to outside if necessary
        array.getSourceBuffer().pushStaged();

        MyStruct struct = new MyStruct();
        struct.bind(data); // bind struct to data
        struct.value.set(1f);
        struct.sub.vec.set(Vector4f.ZERO);
        struct.array.index(6).vec.set(Vector4f.UNIT_Z);

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

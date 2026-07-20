package com.jme3.vulkan.material.experimental;

import com.jme3.math.ColorRGBA;
import com.jme3.util.natives.Disposable;
import com.jme3.util.natives.DisposableManager;
import com.jme3.util.natives.DisposableReference;
import com.jme3.util.struct.*;

public class PBR implements ShadingInterface, Disposable {

    public static class Data extends Struct<StructField> {

        public final ObjectArrayField<ColorRGBA> color;
        public final FloatArrayField metallic;
        public final FloatArrayField roughness;

        public Data(int size) {
            color = addField(new ObjectArrayField<>(size, ColorRGBA.class, ColorRGBA[]::new));
            metallic = addField(new FloatArrayField(size));
            roughness = addField(new FloatArrayField(size));
            bind(StructLayout.std140);
        }

    }

    private static final Data data;

    private final DisposableReference ref;
    private final int dataIndex;

    public PBR() {
        dataIndex = data.acquireIndex();
        ref = DisposableManager.reference(this);
    }

    @Override
    public Runnable createDestroyer() {
        return () -> data.releaseIndex(dataIndex);
    }

    @Override
    public DisposableReference getReference() {
        return ref;
    }

}

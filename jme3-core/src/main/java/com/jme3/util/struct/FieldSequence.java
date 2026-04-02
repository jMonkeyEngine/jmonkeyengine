package com.jme3.util.struct;

import java.util.Iterator;

public class FieldSequence <T> implements StructSequence<Struct> {

    private final StructSequence sequence;
    private final StructField<T> field;

    public FieldSequence(StructSequence sequence, StructField<T> field) {
        this.sequence = sequence;
        this.field = field;
    }

    public StructField<T> field() {
        return field;
    }

    @Override
    public Struct get() {
        return sequence.get();
    }

    @Override
    public void sample(int index) {
        sequence.sample(index);
    }

    @Override
    public void increment() {
        sequence.increment();
    }

    @Override
    public void decrement() {
        sequence.decrement();
    }

    @Override
    public Iterator<Integer> iterator() {
        return sequence.iterator();
    }

}

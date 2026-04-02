package com.jme3.vulkan.mesh.attributes;

import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.util.struct.Struct;
import com.jme3.util.struct.StructField;
import com.jme3.util.struct.StructMapping;
import com.jme3.util.struct.StructSequence;
import com.jme3.vulkan.mesh.VertexBuffer;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class AttributeMapping implements StructSequence<AttributeMapping.StructImpl>, AutoCloseable {

    private final int elements;
    private final Collection<StructMapping> mappings;
    private final Queue<StructField> fields;
    private final StructImpl struct;

    public AttributeMapping(int elements, Collection<VertexBuffer> buffers, Queue<StructField> fields) {
        this.elements = elements;
        this.mappings = buffers.stream().map(v -> v.map(0, elements)).collect(Collectors.toCollection(ArrayList::new));
        this.fields = fields;
        this.struct = new StructImpl(fields);
    }

    @Override
    public void close() {
        for (StructMapping m : mappings) {
            m.close();
        }
    }

    @Override
    public Iterator<Integer> iterator() {
        return new IteratorImpl(elements, mappings);
    }

    @Override
    public StructImpl get() {
        return struct;
    }

    @Override
    public void sample(int i) {
        for (StructMapping m : mappings) {
            m.sample(i);
        }
    }

    @Override
    public void increment() {
        for (StructMapping m : mappings) {
            m.increment();
        }
    }

    @Override
    public void decrement() {
        for (StructMapping m : mappings) {
            m.increment();
        }
    }

    public <T extends StructField> T poll() {
        return (T)fields.poll();
    }

    public static class StructImpl extends Struct {

        private StructImpl(Collection<StructField> fields) {
            addFields(fields.toArray(new StructField[0]));
        }

        @Override
        public void write(JmeExporter ex) throws IOException {
            throw new UnsupportedOperationException("Unable to save struct.");
        }

        @Override
        public void read(JmeImporter im) throws IOException {
            throw new UnsupportedOperationException("Unable to read struct.");
        }

    }

    private static class IteratorImpl implements Iterator<Integer> {

        private final int elements;
        private final Collection<StructMapping> structs;
        private int index = 0;

        public IteratorImpl(int elements, Collection<StructMapping> structs) {
            this.elements = elements;
            this.structs = structs;
        }

        @Override
        public boolean hasNext() {
            return index < elements;
        }

        @Override
        public Integer next() {
            for (StructMapping m : structs) {
                m.sample(index);
            }
            return index++;
        }

    }

}

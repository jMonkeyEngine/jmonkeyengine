package com.jme3.vulkan.mesh.attribute;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class Vertex <T extends Vertex> implements Iterable<T>, Iterator<T> {

    private final Collection<Iterator<?>> iterators = new ArrayList<>();

    protected void add(Iterable<?> it) {
        iterators.add(it.iterator());
    }

    @Override
    public boolean hasNext() {
        return iterators.stream().allMatch(Iterator::hasNext);
    }

    @Override
    public T next() {
        iterators.forEach(Iterator::next);
        return (T)this;
    }

    @Override
    public Iterator<T> iterator() {
        return this;
    }

    private static class TestVertex extends Vertex<TestVertex> {

        public final Vector3f position = new Vector3f();
        public final Vector2f texCoord = new Vector2f();

        public TestVertex(Attribute<Vector3f> position, Attribute<Vector2f> texCoord) {
            add(position.readWrite(this.position));
            add(texCoord.readWrite(this.texCoord));
        }

    }

}

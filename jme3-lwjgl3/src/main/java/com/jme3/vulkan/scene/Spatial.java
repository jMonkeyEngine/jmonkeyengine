package com.jme3.vulkan.scene;

import java.util.*;

public abstract class Spatial implements Iterable<Spatial> {

    private Node parent;

    public void removeFromParent() {

    }

    public Node getParent() {
        return parent;
    }

    protected void setParent(Node parent) {
        this.parent = parent;
    }

    protected abstract void findNextIteration(GraphIterator iterator);

    @Override
    public Iterator<Spatial> iterator() {
        return new GraphIterator(this);
    }

    public static class GraphIterator implements Iterator<Spatial> {

        private final Stack<Integer> childIndices = new Stack<>();
        private Spatial current;
        private int currentIndex = 0;
        private int iteration = -1;

        public GraphIterator(Spatial start) {
            current = Objects.requireNonNull(start);
        }

        @Override
        public boolean hasNext() {
            return current != null;
        }

        @Override
        public Spatial next() {
            if (++iteration > 0) {
                current.findNextIteration(this);
            }
            return current;
        }

        @Override
        public void remove() {
            if (current.getParent() != null) {
                current.removeFromParent();
                moveUp();
                currentIndex--;
            }
        }

        protected void moveUp() {
            if (!childIndices.isEmpty()) {
                current = current.getParent();
                currentIndex = childIndices.pop();
                if (current != null) {
                    current.findNextIteration(this);
                }
            } else {
                current = null;
            }
        }

        protected void moveDown(Spatial node) {
            if (node.getParent() != current) {
                throw new IllegalArgumentException("Next node must be a child of the current node.");
            }
            current = node;
            childIndices.push(currentIndex);
            currentIndex = 0;
        }

        protected int advanceIndex() {
            return currentIndex++;
        }

        protected int getCurrentIndex() {
            return currentIndex;
        }

        public void skipChildren() {
            moveUp();
        }

        public int getDepth() {
            return childIndices.size();
        }

        public int getIteration() {
            return iteration;
        }

    }

}

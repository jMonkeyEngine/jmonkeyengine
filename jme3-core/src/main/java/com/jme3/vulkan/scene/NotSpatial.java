package com.jme3.vulkan.scene;

import java.util.*;

public abstract class NotSpatial implements Iterable<NotSpatial> {

    private NotNode parent;

    public void removeFromParent() {

    }

    public NotNode getParent() {
        return parent;
    }

    protected void setParent(NotNode parent) {
        this.parent = parent;
    }

    protected abstract void findNextIteration(GraphIterator iterator);

    @Override
    public Iterator<NotSpatial> iterator() {
        return new GraphIterator(this);
    }

    public static class GraphIterator implements Iterator<NotSpatial> {

        private final Stack<Integer> childIndices = new Stack<>();
        private NotSpatial current;
        private int currentIndex = 0;
        private int iteration = -1;

        public GraphIterator(NotSpatial start) {
            current = Objects.requireNonNull(start);
        }

        @Override
        public boolean hasNext() {
            return current != null;
        }

        @Override
        public NotSpatial next() {
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

        protected void moveDown(NotSpatial node) {
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

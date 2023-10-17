/*
 * Copyright (c) 2023 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.scene;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Iterates over the scene graph with the depth-first traversal method.
 * <p>
 * This method of scene traversal allows for more control of the iteration
 * process than {@link Spatial#depthFirstTraversal(com.jme3.scene.SceneGraphVisitor)}
 * because it implements {@link Iterator} (enabling it to be used in for-loops).
 * 
 * @author codex
 */
public class SceneGraphIterator implements Iterable<Spatial>, Iterator<Spatial> {
    
    private Spatial current;
    private Spatial main;
    private int depth = 0;
    private final LinkedList<PathNode> path = new LinkedList<>();
    
    /**
     * Instantiates a new {@code SceneGraphIterator} instance that
     * starts iterating at the given main spatial.
     * 
     * @param main the main spatial to start iteration from
     */
    public SceneGraphIterator(Spatial main) {
        if (main instanceof Node) {
            path.add(new PathNode((Node)main));
            depth++;
        }
        this.main = main;
    }

    @Override
    public Iterator<Spatial> iterator() {
        return this;
    }
    
    @Override
    public boolean hasNext() {
        if (main != null) {
            return true;
        }
        trim();
        return !path.isEmpty();
    }
    
    @Override
    public Spatial next() {
        if (main != null) {
            current = main;
            main = null;
        } else {
            current = path.getLast().iterator.next();
            if (current instanceof Node) {
                Node n = (Node)current;
                if (!n.getChildren().isEmpty()) {
                    path.addLast(new PathNode(n));
                    depth++;
                }
            }
        }
        return current;
    }
    
    /**
     * Gets the spatial the iterator is currently on.
     * 
     * @return current spatial
     */
    public Spatial current() {
        return current;
    }
    
    /**
     * Makes this iterator ignore all children of the current spatial.
     * The children of the current spatial will not be iterated through.
     */
    public void ignoreChildren() {
        if (current instanceof Node) {
            path.removeLast();
            depth--;
        }
    }
    
    /**
     * Gets the current depth of the iterator.
     * <p>
     * The depth is how far away from the main spatial the
     * current spatial is. So, the main spatial's depth is 0,
     * all its children's depths is 1, and all <em>their</em>
     * children's depths is 2, etc.
     * 
     * @return current depth, or distance from the main spatial.
     */
    public int getDepth() {
        // The depth field is not an accurate indicator of depth.
        // Whenever the current spatial is an iterable node, the depth
        // value is exactly 1 greater than it should be.
        return !path.isEmpty() && current == path.getLast().node ? depth-1 : depth;
    }
    
    /**
     * Trims the path to the first unexhausted node.
     */
    private void trim() {
        if (!path.isEmpty() && !path.getLast().iterator.hasNext()) {
            path.removeLast();
            depth--;
            trim();
        }
    }
    
    private static class PathNode {

        Node node;
        Iterator<Spatial> iterator;

        PathNode(Node node) {
            this.node = node;
            iterator = this.node.getChildren().iterator();
        }
        
    }
    
}

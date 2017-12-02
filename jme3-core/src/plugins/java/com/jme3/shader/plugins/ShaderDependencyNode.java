/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
package com.jme3.shader.plugins;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class ShaderDependencyNode {

    private String extensions;
    private String shaderSource;
    private String shaderName;

    private final List<ShaderDependencyNode> dependencies = new ArrayList<ShaderDependencyNode>();
    private final List<Integer> dependencyInjectIndices   = new ArrayList<Integer>();
    private final List<ShaderDependencyNode> dependOnMe   = new ArrayList<ShaderDependencyNode>();

    public ShaderDependencyNode(String shaderName){
        this.shaderName = shaderName;
    }

    public String getSource() {
        return shaderSource;
    }

    public void setSource(String shaderSource) {
        this.shaderSource = shaderSource;
    }

    public String getName() {
        return shaderName;
    }

    public void setName(String shaderName) {
        this.shaderName = shaderName;
    }

    public String getExtensions() {
        return extensions;
    }

    public void setExtensions(String extensions) {
        this.extensions = extensions;
    }

    public void addDependency(int index, ShaderDependencyNode node){
        if (this.dependencies.contains(node)) {
            // already contains dependency ..
            return;
        } 

        this.dependencies.add(node);
        this.dependencyInjectIndices.add(index);
        node.dependOnMe.add(this);
    }
    
    public void removeDependency(ShaderDependencyNode node) {
        int positionInList = this.dependencies.indexOf(node);
        if (positionInList == -1) {
            throw new IllegalArgumentException("The given node " + 
                                               node.getName() + 
                                               " is not in this node's (" + 
                                               getName() + 
                                               ") dependency list");
        }
        
        this.dependencies.remove(positionInList);
        this.dependencyInjectIndices.remove(positionInList);
    }
    
    public List<ShaderDependencyNode> getDependOnMe() {
        return Collections.unmodifiableList(dependOnMe);
    }
    
    public List<ShaderDependencyNode> getDependencies() {
        return Collections.unmodifiableList(dependencies);
    }

    public List<Integer> getDependencyInjectIndices() {
        return Collections.unmodifiableList(dependencyInjectIndices);
    }
}

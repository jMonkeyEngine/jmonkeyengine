package com.jme3.shader.plugins;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class ShaderDependencyNode {
    
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

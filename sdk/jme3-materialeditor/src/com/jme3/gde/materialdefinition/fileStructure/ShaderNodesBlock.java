/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.materialdefinition.fileStructure;

import com.jme3.util.blockparser.Statement;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.List;
import org.openide.util.WeakListeners;

/**
 *
 * @author Nehon
 */
public class ShaderNodesBlock extends UberStatement implements PropertyChangeListener {
    
    protected ShaderNodesBlock(int lineNumber, String line) {
        super(lineNumber, line);
    }
    
    public ShaderNodesBlock(Statement sta) {
        this(sta.getLineNumber(), sta.getLine());
        for (Statement statement : sta.getContents()) {
            ShaderNodeBlock b = new ShaderNodeBlock(statement);
            b.addPropertyChangeListener(WeakListeners.propertyChange(this, b));
            addStatement(b);
        }
    }
    
    public List<ShaderNodeBlock> getShaderNodes() {        
        return getBlocks(ShaderNodeBlock.class);
    }
    
    public void addShaderNode(ShaderNodeBlock shaderNodeBlock) {
        addStatement(shaderNodeBlock);      
        shaderNodeBlock.addPropertyChangeListener(WeakListeners.propertyChange(this, shaderNodeBlock));
    }
    
    public boolean removeShaderNode(ShaderNodeBlock shaderNodeBlock) {
        return contents.remove(shaderNodeBlock);
    }
    
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("order")) {
            sort();
        }        
    }
    
    public void sort() {
        List<ShaderNodeBlock> list = getShaderNodes();
        Collections.sort(list);        
        contents.clear();
        contents.addAll(list);
        fire("reorder", null, null);
    }
}

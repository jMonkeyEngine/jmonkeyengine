/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.materialdefinition.fileStructure;

import com.jme3.gde.materialdefinition.fileStructure.leaves.WorldParamBlock;
import com.jme3.util.blockparser.Statement;
import java.util.List;

/**
 *
 * @author Nehon
 */
public class WorldParametersBlock extends UberStatement {
    
    protected WorldParametersBlock(int lineNumber, String line) {
        super(lineNumber, line);
    }
    
    public WorldParametersBlock(Statement sta) {
        this(sta.getLineNumber(), sta.getLine());
        for (Statement statement : sta.getContents()) {
            addStatement(new WorldParamBlock(statement));
        }
    }
    
    public List<WorldParamBlock> getWorldParams() {
        return getBlocks(WorldParamBlock.class);
    }
    
    public void addWorldParam(WorldParamBlock block) {
        contents.add(block);
    }
    
    public void removeWorldParam(WorldParamBlock block) {
        contents.remove(block);
    }
}

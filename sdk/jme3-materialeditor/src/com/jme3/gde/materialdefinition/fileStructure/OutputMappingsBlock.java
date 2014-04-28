/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.materialdefinition.fileStructure;

import com.jme3.gde.materialdefinition.fileStructure.leaves.OutputMappingBlock;
import com.jme3.util.blockparser.Statement;
import java.util.List;

/**
 *
 * @author Nehon
 */
public class OutputMappingsBlock extends UberStatement {

    protected OutputMappingsBlock(int lineNumber, String line) {
        super(lineNumber, line);
    }

    public OutputMappingsBlock(Statement sta, String nodeName) {
        this(sta.getLineNumber(), sta.getLine());
        for (Statement statement : sta.getContents()) {
            addStatement(new OutputMappingBlock(statement, nodeName));
        }
    }

    public List<OutputMappingBlock> getMappings() {
        return getBlocks(OutputMappingBlock.class);
    }

    public void addMapping(OutputMappingBlock mapping) {
        addStatement(mapping);
    }
    
    public void removeMapping(OutputMappingBlock mapping) {
        contents.remove(mapping);
    }
}

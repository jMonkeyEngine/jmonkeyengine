/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.materialdefinition.fileStructure;

import com.jme3.gde.materialdefinition.fileStructure.*;
import com.jme3.gde.materialdefinition.fileStructure.leaves.InputMappingBlock;
import com.jme3.gde.materialdefinition.fileStructure.leaves.MatParamBlock;
import com.jme3.gde.materialdefinition.fileStructure.leaves.OutputMappingBlock;
import com.jme3.util.blockparser.Statement;
import java.util.List;

/**
 *
 * @author Nehon
 */
public class InputMappingsBlock extends UberStatement {

    protected InputMappingsBlock(int lineNumber, String line) {
        super(lineNumber, line);
    }

    public InputMappingsBlock(Statement sta, String nodeName) {
        this(sta.getLineNumber(), sta.getLine());
        for (Statement statement : sta.getContents()) {
            addStatement(new InputMappingBlock(statement, nodeName));
        }
    }

    public List<InputMappingBlock> getMappings() {
        return getBlocks(InputMappingBlock.class);
    }

    public void addMapping(InputMappingBlock mapping) {
        addStatement(mapping);
    }

    public void removeMapping(InputMappingBlock mapping) {
        contents.remove(mapping);
    }
}

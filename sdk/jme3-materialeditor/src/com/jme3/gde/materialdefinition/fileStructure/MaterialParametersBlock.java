/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.materialdefinition.fileStructure;

import com.jme3.gde.materialdefinition.fileStructure.leaves.MatParamBlock;
import com.jme3.util.blockparser.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Nehon
 */
public class MaterialParametersBlock extends UberStatement {

    protected MaterialParametersBlock(int lineNumber, String line) {
        super(lineNumber, line);
    }

    public MaterialParametersBlock(Statement sta) {
        this(sta.getLineNumber(), sta.getLine());
        for (Statement statement : sta.getContents()) {
            addStatement(new MatParamBlock(statement));
        }
    }

    public List<MatParamBlock> getMatParams() {        
        return getBlocks(MatParamBlock.class);
    }
    
    public void addMatParam(MatParamBlock matParam){
         addStatement(matParam);        
    }
    
    public void removeMatParam(MatParamBlock matParam){
         contents.remove(matParam);        
    }
}

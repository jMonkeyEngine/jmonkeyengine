/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.materialdefinition.fileStructure;

import com.jme3.gde.materialdefinition.fileStructure.leaves.MatParamBlock;
import com.jme3.gde.materialdefinition.fileStructure.leaves.UnsupportedStatement;
import com.jme3.util.blockparser.Statement;
import java.util.List;

/**
 *
 * @author Nehon
 */
public class MatDefBlock extends UberStatement {

    public static final String ADD_MAT_PARAM = "addMatParam";
    public static final String REMOVE_MAT_PARAM = "removeMatParam";
    protected String name;

    protected MatDefBlock(int lineNumber, String line) {
        super(lineNumber, line);
    }

    public MatDefBlock(Statement sta) {
        this(sta.getLineNumber(), sta.getLine());
        for (Statement statement : sta.getContents()) {
            if (statement.getLine().trim().startsWith("MaterialParameters")) {
                addStatement(new MaterialParametersBlock(statement));
            } else if (statement.getLine().trim().startsWith("Technique")) {
                addStatement(new TechniqueBlock(statement));
            } else {
                addStatement(new UnsupportedStatement(statement));
            }
        }
        String[] s = line.split("\\s");
        name = s[1];
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        String oldName = this.name;
        this.name = name;
        line = "MaterialDef " + name;
        fire("name", oldName, name);
    }

    protected MaterialParametersBlock getMaterialParameters() {
        return getBlock(MaterialParametersBlock.class);
    }

    public List<MatParamBlock> getMatParams() {
        return getMaterialParameters().getMatParams();
    }

    public void addMatParam(MatParamBlock matParam) {
        MaterialParametersBlock mpBlock = getMaterialParameters();
        if (mpBlock == null) {
            mpBlock = new MaterialParametersBlock(0, "MaterialParameters");
            addStatement(0, mpBlock);
        }
        mpBlock.addMatParam(matParam);
        fire(ADD_MAT_PARAM, null, matParam);
    }

    public void removeMatParam(MatParamBlock matParam) {
        MaterialParametersBlock mpBlock = getMaterialParameters();
        if (mpBlock == null) {
            return;
        }
        mpBlock.removeMatParam(matParam);

        for (TechniqueBlock techniqueBlock : getTechniques()) {
            VertexShaderNodesBlock vblock = techniqueBlock.getBlock(VertexShaderNodesBlock.class);
            FragmentShaderNodesBlock fblock = techniqueBlock.getBlock(FragmentShaderNodesBlock.class);
            techniqueBlock.cleanMappings(vblock, "MatParam", matParam.getName());
            techniqueBlock.cleanMappings(fblock, "MatParam", matParam.getName());
        }
        fire(REMOVE_MAT_PARAM, matParam, null);
    }

    public List<TechniqueBlock> getTechniques() {
        return getBlocks(TechniqueBlock.class);
    }

    public void addTechnique(TechniqueBlock techniqueBlock) {
        addStatement(techniqueBlock);
        fire("technique", null, techniqueBlock);
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.materialdefinition.fileStructure.leaves;

import com.jme3.util.blockparser.Statement;

/**
 *
 * @author Nehon
 */
public class WorldParamBlock extends LeafStatement {

    protected String name;

    protected WorldParamBlock(int lineNumber, String line) {
        super(lineNumber, line);
    }

    public WorldParamBlock(Statement sta) {
        this(sta.getLineNumber(), sta.getLine());
        parse(sta);
         updateLine();
    }

    public WorldParamBlock(String name) {
        super(0, "");
        this.name = name;
        updateLine();
    }

    private void updateLine() {
        this.line = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        updateLine();
    }

    private void parse(Statement sta) {
        name = sta.getLine().trim();
    }
    
      @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof WorldParamBlock)){
            return false;
        }
        return ((WorldParamBlock)obj).getName().equals(name);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + (this.name != null ? this.name.hashCode() : 0);
        return hash;
    }
}

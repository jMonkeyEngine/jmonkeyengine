/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.materialdefinition.fileStructure.leaves;

import com.jme3.util.blockparser.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Nehon
 */
public class ConditionBlock extends LeafStatement {

    protected String condition;    

    protected ConditionBlock(int lineNumber, String line) {
        super(lineNumber, line);
    }

    public ConditionBlock(Statement sta) {
        this(sta.getLineNumber(), sta.getLine());
        parse(sta);
        updateLine();
    }

    public ConditionBlock(String condition) {
        super(0, "");
        this.condition = condition;       
        updateLine();
    }

    private void updateLine() {
        this.line = "Condition : " + condition;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
        updateLine();
    }

    private void parse(Statement sta) {
        try {
            String[] split = sta.getLine().split(":");       
            condition = split[1].trim();

        } catch (ArrayIndexOutOfBoundsException e) {
            Logger.getLogger(ConditionBlock.class.getName()).log(Level.WARNING, "Parsing error line " + sta.getLineNumber() + " : " + sta.getLine());
        }
    }
}

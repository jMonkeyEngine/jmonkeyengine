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
public class OutputMappingBlock extends MappingBlock {

    protected OutputMappingBlock(int lineNumber, String line) {
        super(lineNumber, line);
    }

    public OutputMappingBlock(Statement sta, String name) {
        this(sta.getLineNumber(), sta.getLine());
        parse(sta);
        rightNameSpace = name;
        updateLine();
    }

    public OutputMappingBlock(String leftVar, String rightVar, String leftVarSwizzle, String rightVarSwizzle, String leftNameSpace, String rightNameSpace, String condition) {
        super(0, "");
        this.leftVar = leftVar;
        this.rightVar = rightVar;
        this.leftVarSwizzle = leftVarSwizzle;
        this.rightVarSwizzle = rightVarSwizzle;
        this.leftNameSpace = leftNameSpace;
        this.rightNameSpace = rightNameSpace;
        this.condition = condition;
        updateLine();
    }

    @Override
    protected final void updateLine() {
        this.line = leftNameSpace + "." + leftVar + (leftVarSwizzle == null ? "" : "." + leftVarSwizzle) + " = " + rightVar + (rightVarSwizzle == null ? "" : "." + rightVarSwizzle) + (condition != null ? " : " + condition : "");
    }

    private void parse(Statement sta) {
        try {
            String[] split = sta.getLine().split(":");
            if (split.length > 1) {
                condition = split[1].trim();
            }
            String mapping = split[0].trim();
            String[] s = mapping.split("=");

            String[] left = s[0].split("\\.");
            leftNameSpace = left[0].trim();
            leftVar = left[1].trim();
            if (left.length > 2) {
                leftVarSwizzle = left[2].trim();
            }

            String[] right = s[1].split("\\.");

            rightVar = right[0].trim();
            if (right.length > 1) {
                rightVarSwizzle = right[1].trim();
            }


        } catch (ArrayIndexOutOfBoundsException e) {
            Logger.getLogger(OutputMappingBlock.class.getName()).log(Level.WARNING, "Parsing error line {0} : {1}", new Object[]{sta.getLineNumber(), sta.getLine()});
        }
    }
}

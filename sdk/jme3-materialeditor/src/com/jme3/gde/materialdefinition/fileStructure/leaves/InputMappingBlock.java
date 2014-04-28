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
public class InputMappingBlock extends MappingBlock {

    protected InputMappingBlock(int lineNumber, String line) {
        super(lineNumber, line);
    }

    public InputMappingBlock(Statement sta, String name) {
        this(sta.getLineNumber(), sta.getLine());
        parse(sta);
        leftNameSpace = name;
        updateLine();
    }

    public InputMappingBlock(String leftVar, String rightVar, String leftVarSwizzle, String rightVarSwizzle, String leftNameSpace,String rightNameSpace, String condition) {
        super(0, "");
        this.leftVar = leftVar;
        this.rightVar = rightVar;
        this.leftVarSwizzle = leftVarSwizzle;
        this.leftNameSpace = leftNameSpace;
        this.rightVarSwizzle = rightVarSwizzle;
        this.rightNameSpace = rightNameSpace;        
        this.condition = condition;
        updateLine();
    }

    @Override
    protected final void updateLine() {
        this.line = leftVar + (leftVarSwizzle == null ? "" : "." + leftVarSwizzle) + " = " + rightNameSpace + "." + rightVar + (rightVarSwizzle == null ? "" : "." + rightVarSwizzle) + (condition != null ? " : " + condition : "");
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
            leftVar = left[0].trim();
            if (left.length > 1) {
                leftVarSwizzle = left[1].trim();
            }
            String[] right = s[1].split("\\.");
            rightNameSpace = right[0].trim();
            rightVar = right[1].trim();
            if (right.length > 2) {
                rightVarSwizzle = right[2].trim();
            }

        } catch (ArrayIndexOutOfBoundsException e) {
            Logger.getLogger(InputMappingBlock.class.getName()).log(Level.WARNING, "Parsing error line " + sta.getLineNumber() + " : " + sta.getLine());
        }
    }
}

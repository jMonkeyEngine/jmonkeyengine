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
public class UnsupportedStatement extends Statement {

    public UnsupportedStatement(int lineNumber, String line) {
        super(lineNumber, line);
    }

    public UnsupportedStatement(Statement sta) {
        super(sta.getLineNumber(), sta.getLine());
    }
}

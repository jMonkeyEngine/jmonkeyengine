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
public class ShaderFileBlock extends LeafStatement {

    protected String language;
    protected String path;

    protected ShaderFileBlock(int lineNumber, String line) {
        super(lineNumber, line);
    }

    public ShaderFileBlock(Statement sta) {
        this(sta.getLineNumber(), sta.getLine());
        parse(sta);
        updateLine();
    }

    public ShaderFileBlock(String language, String path, int lineNumber, String line) {
        super(lineNumber, line);
        this.language = language;
        this.path = path;
        updateLine();
    }

    protected void updateLine() {
        this.line = "Shader " + language + ": " + path;
    }

    private void parse(Statement sta) {
        try {
            String[] split = sta.getLine().split("\\s");
            language = split[1].trim();
            path = split[2].trim();

        } catch (ArrayIndexOutOfBoundsException e) {
            Logger.getLogger(ShaderFileBlock.class.getName()).log(Level.WARNING, "Parsing error line " + sta.getLineNumber() + " : " + sta.getLine());
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(ShaderFileBlock.class.getName()).log(Level.WARNING, "Invalid light mode : " + sta.getLineNumber() + " : " + sta.getLine());
        }
    }
}

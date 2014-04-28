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
public class DefinitionBlock extends LeafStatement {

    protected String name;
    protected String path;

    protected DefinitionBlock(int lineNumber, String line) {
        super(lineNumber, line);
    }

    public DefinitionBlock(Statement sta) {
        this(sta.getLineNumber(), sta.getLine());
        parse(sta);
         updateLine();
    }

    public DefinitionBlock(String name, String path) {
        super(0, "");
        this.name = name;
        this.path = path;
        updateLine();
    }

    private void updateLine() {
        this.line = "Definition : " + name + (path == null ? "" : " : " + path);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        updateLine();
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
        updateLine();
    }

    private void parse(Statement sta) {
        try {
            String[] split = sta.getLine().split(":");
            if (split.length > 2) {
                path = split[2].trim();
            }
            name = split[1].trim();


        } catch (ArrayIndexOutOfBoundsException e) {
            Logger.getLogger(DefinitionBlock.class.getName()).log(Level.WARNING, "Parsing error line " + sta.getLineNumber() + " : " + sta.getLine());
        }
    }
}

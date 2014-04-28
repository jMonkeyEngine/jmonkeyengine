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
public class MatParamBlock extends LeafStatement {

    protected String type;
    protected String name;
    protected String fixedFuncBinding;
    protected String defaultValue;

    protected MatParamBlock(int lineNumber, String line) {
        super(lineNumber, line);
    }

    public MatParamBlock(Statement sta) {
        this(sta.getLineNumber(), sta.getLine());
        parse(sta);     
         updateLine();
    }

    public MatParamBlock(String type, String name, String fixedPipelineBinding, String defaultValue) {
        super(0, "");
        this.type = type;
        this.name = name;
        this.fixedFuncBinding = fixedPipelineBinding;
        this.defaultValue = defaultValue;
        updateLine();
    }

    private void updateLine() {
        this.line = type + " " + name + (fixedFuncBinding != null ? " (" + fixedFuncBinding + ") " : "") + (defaultValue != null ? " : " + defaultValue : "");
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
        updateLine();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        updateLine();
    }

    public String getFixedPipelineBinding() {
        return fixedFuncBinding;
    }

    public void setFixedPipelineBinding(String fixedPipelineBinding) {
        this.fixedFuncBinding = fixedPipelineBinding;
        updateLine();
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
        updateLine();
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof MatParamBlock)){
            return false;
        }
        return ((MatParamBlock)obj).getName().equals(name);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + (this.name != null ? this.name.hashCode() : 0);
        return hash;
    }
    
    private void parse(Statement sta) {
        try {
            String[] split = sta.getLine().split(":");
            String statement = split[0].trim();

            // Parse default val
            if (split.length > 1) {
                
                defaultValue = split[1].trim();
            }

            // Parse ffbinding
            int startParen = statement.indexOf("(");
            if (startParen != -1) {
                // get content inside parentheses
                int endParen = statement.indexOf(")", startParen);
                fixedFuncBinding = statement.substring(startParen + 1, endParen).trim();
                statement = statement.substring(0, startParen);
            }

            // Parse type + name
            split = statement.split("\\p{javaWhitespace}+");

            type = split[0];
            name = split[1];


        } catch (Exception e) {
            e.printStackTrace();
            Logger.getLogger(MatParamBlock.class.getName()).log(Level.WARNING, "Parsing error line " + sta.getLineNumber() + " : " + sta.getLine());
        }
    }
}

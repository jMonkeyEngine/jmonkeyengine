package com.jme3.util.blockparser;

import java.util.ArrayList;
import java.util.List;

public class Statement {
    
    private int lineNumber;
    private String line;
    private List<Statement> contents = new ArrayList<Statement>();

    Statement(int lineNumber, String line) {
        this.lineNumber = lineNumber;
        this.line = line;
    }
    
    void addStatement(Statement statement){
//        if (contents == null){
//            contents = new ArrayList<Statement>();
//        }
        contents.add(statement);
    }

    public int getLineNumber(){
        return lineNumber;
    }
    
    public String getLine() {
        return line;
    }

    public List<Statement> getContents() {
        return contents;
    }
    
    private String getIndent(int indent){
        return "                               ".substring(0, indent);
    }
    
    private String toString(int indent){
        StringBuilder sb = new StringBuilder();
        sb.append(getIndent(indent));
        sb.append(line);
        if (contents != null){
            sb.append(" {\n");
            for (Statement statement : contents){
                sb.append(statement.toString(indent+4));
                sb.append("\n");
            }
            sb.append(getIndent(indent));
            sb.append("}");
        }
        return sb.toString();
    }
    
    @Override
    public String toString(){
        return toString(0);
    }
    
}

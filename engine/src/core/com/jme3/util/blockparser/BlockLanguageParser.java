package com.jme3.util.blockparser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class BlockLanguageParser {
    
    private Reader reader;
    private ArrayList<Statement> statementStack = new ArrayList<Statement>();
    private Statement lastStatement;
    private int lineNumber = 1;
    
    private BlockLanguageParser(){
    }
    
    private void reset(){
        statementStack.clear();
        statementStack.add(new Statement(0, "<root>"));
        lastStatement = null;
        lineNumber = 1;
    }
    
    private void pushStatement(StringBuilder buffer){
        String content = buffer.toString().trim();
        if (content.length() > 0){
            // push last statement onto the list
            lastStatement = new Statement(lineNumber, content);

            Statement parent = statementStack.get(statementStack.size()-1);
            parent.addStatement(lastStatement);

            buffer.setLength(0);
        }
    }
    
    private void load(InputStream in) throws IOException{
        reset();
        
        reader = new InputStreamReader(in);
        
        StringBuilder buffer = new StringBuilder();
        boolean insideComment = false;
        char lastChar = '\0';
        
        while (true){
            int ci = reader.read();
            char c = (char) ci;
            if (c == '\r'){
                continue;
            }
            if (insideComment && c == '\n'){
                insideComment = false;
            }else if (c == '/' && lastChar == '/'){
                buffer.deleteCharAt(buffer.length()-1);
                insideComment = true;
                pushStatement(buffer);
                lastChar = '\0';
            }else if (!insideComment){
                if (ci == -1 || c == '{' || c == '}' || c == '\n' || c == ';'){
                    pushStatement(buffer);
                    lastChar = '\0';
                    if (c == '{'){
                        // push last statement onto the stack
                        statementStack.add(lastStatement);
                        continue;
                    }else if (c == '}'){
                        // pop statement from stack
                        statementStack.remove(statementStack.size()-1);
                        continue;
                    }else if (c == '\n'){
                        lineNumber++;
                    }else if (ci == -1){
                        break;
                    }
                }else{
                    buffer.append(c);
                    lastChar = c;
                }
            }
        }
    }
    
    public static List<Statement> parse(InputStream in) throws IOException {
        BlockLanguageParser parser = new BlockLanguageParser();
        parser.load(in);
        return parser.statementStack.get(0).getContents();
    }
}

/*
 * Copyright (c) 2009-2012 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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
        
        reader = new InputStreamReader(in, "UTF-8");
        
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
                lineNumber++;
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

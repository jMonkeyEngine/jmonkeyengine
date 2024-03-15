/*
 * Copyright (c) 2009-2022 jMonkeyEngine
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
package jme3tools.shader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GLSL Preprocessor
 * 
 * @author Riccardo Balbo
 */
public class Preprocessor {

    public static InputStream apply(InputStream in) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte chunk[] = new byte[1024];
        int read;
        while ((read = in.read(chunk)) != -1) {
            bos.write(chunk, 0, read);
        }
        bos.close();
        in.close();

        String code = bos.toString("UTF-8");

        code = Preprocessor.forMacro(code);
        code = Preprocessor.structMacro(code);

        return new ByteArrayInputStream(code.getBytes("UTF-8"));
    }

    /**
     * #for i=0..100 ( #ifdef ENABLE_INPUT_$i $0 #endif ) 
     *      do something with $i
     * #endfor
     */
    private static final Pattern FOR_REGEX = Pattern.compile("([^=]+)=\\s*([0-9]+)\\s*\\.\\.\\s*([0-9]+)\\s*\\((.+)\\)");

    public static String forMacro(String code) {
        StringBuilder expandedCode = new StringBuilder();
        StringBuilder currentFor = null;
        String forDec = null;
        int skip = 0;
        String codel[] = code.split("\n");
        boolean captured = false;
        for (String l : codel) {
            if (!captured) {
                String ln = l.trim();
                if (ln.startsWith("#for")) {
                    if (skip == 0) {
                        forDec = ln;
                        currentFor = new StringBuilder();
                        skip++;
                        continue;
                    }
                    skip++;
                } else if (ln.startsWith("#endfor")) {
                    skip--;
                    if (skip == 0) {
                        forDec = forDec.substring("#for ".length()).trim();

                        Matcher matcher = FOR_REGEX.matcher(forDec);
                        if (matcher.matches()) {
                            String varN = "$" + matcher.group(1);
                            int start = Integer.parseInt(matcher.group(2));
                            int end = Integer.parseInt(matcher.group(3));
                            String inj = matcher.group(4);
                            if (inj.trim().isEmpty()) inj = "$0";
                            String inCode = currentFor.toString();
                            currentFor = null;
                            for (int i = start; i < end; i++) {
                                expandedCode.append("\n").append(inj.replace("$0", "\n" + inCode ).replace(varN, "" + i)).append("\n");
                            }
                            captured = true;
                            continue;
                        }
                    }
                }
            }
            if (currentFor != null) currentFor.append(l).append("\n");
            else expandedCode.append(l).append("\n");
        }
        code = expandedCode.toString();
        if (captured) code = forMacro(code);
        return code;
    }

    /**
     * <code>
     * #struct MyStruct extends BaseStruct, BaseStruct2
     *  int i; 
     *  int b; 
     * #endstruct
     * </code>
     */
    // match #struct MyStruct extends BaseStruct, BaseStruct2
    // extends is optional
    // private static final Pattern FOR_REGEX = Pattern
    // .compile("([^=]+)=\\s*([0-9]+)\\s*\\.\\.\\s*([0-9]+)\\s*\\((.+)\\)");

    private static final Pattern STRUCT_REGEX = Pattern
            .compile("(\\w+)(?:\\s+extends\\s+(\\w+(?:,\\s*\\w+)*))?");

    public static String structMacro(String code) {
        StringBuilder expandedCode = new StringBuilder();
        StringBuilder currentStruct = null;
        String structDec = null;
        int skip = 0;
        String[] codeLines = code.split("\n");
        boolean captured = false;
        for (String line : codeLines) {
            if (!captured) {
                String trimmedLine = line.trim();
                if (trimmedLine.startsWith("#struct")) {
                    if (skip == 0) {
                        structDec = trimmedLine;
                        currentStruct = new StringBuilder();
                        skip++;
                        continue;
                    }
                    skip++;
                } else if (trimmedLine.startsWith("#endstruct")) {
                    skip--;
                    if (skip == 0) {
                        structDec = structDec.substring("#struct ".length()).trim();

                        Matcher matcher = STRUCT_REGEX.matcher(structDec);
                        if (matcher.matches()) {
                            String structName = matcher.group(1);
                            if (structName == null) structName = "";

                            String extendsStructs = matcher.group(2);
                            String extendedStructs[];
                            if (extendsStructs != null) {
                                extendedStructs = extendsStructs.split(",\\s*");
                            } else {
                                extendedStructs = new String[0];
                            }
                            String structBody = currentStruct.toString();
                            if (structBody == null) structBody = "";
                            else {
                                // remove tail spaces
                                structBody = structBody.replaceAll("\\s+$", "");
                            }

                            currentStruct = null;
                            expandedCode.append("#define STRUCT_").append(structName).append(" \\\n");
                            for (String extendedStruct : extendedStructs) {
                                expandedCode.append("STRUCT_").append(extendedStruct).append(" \\\n");
                            }
                            String structBodyLines[] = structBody.split("\n");
                            for (int i = 0; i < structBodyLines.length; i++) {
                                String structBodyLine = structBodyLines[i];
                                structBodyLine = structBodyLine.trim();
                                if (structBodyLine == "") continue;
                                // remove comments if any
                                int commentIndex = structBodyLine.indexOf("//");
                                if (commentIndex >= 0)
                                    structBodyLine = structBodyLine.substring(0, commentIndex);
                                expandedCode.append(structBodyLine);
                                if (i < structBodyLines.length - 1) expandedCode.append(" \\");
                                expandedCode.append("\n");
                            }
                            expandedCode.append("struct ").append(structName).append(" { \nSTRUCT_")
                                    .append(structName).append("\n};\n");
                            captured = true;
                            continue;
                        }
                    }
                }
            }
            if (currentStruct != null) currentStruct.append(line).append("\n");
            else expandedCode.append(line).append("\n");
        }
        code = expandedCode.toString();
        if (captured) code = structMacro(code);
        return code;
    }

}
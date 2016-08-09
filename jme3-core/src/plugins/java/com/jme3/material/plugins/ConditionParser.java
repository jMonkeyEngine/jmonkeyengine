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
package com.jme3.material.plugins;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An utility class that allows to parse a define condition in a glsl language
 * style.
 *
 * extractDefines is able to get a list of defines in an expression and update
 * the formatter expression with uppercased defines
 *
 * @author Nehon
 */
public class ConditionParser {

    private String formattedExpression = "";

    public static void main(String argv[]) {
        ConditionParser parser = new ConditionParser();
        List<String> defines = parser.extractDefines("(LightMap && SeparateTexCoord) || !ColorMap");

        for (String string : defines) {
            System.err.println(string);
        }
        System.err.println(parser.formattedExpression);

        defines = parser.extractDefines("#if (defined(LightMap) && defined(SeparateTexCoord)) || !defined(ColorMap)");

        for (String string : defines) {
            System.err.println(string);
        }
        System.err.println(parser.formattedExpression);


//        System.err.println(parser.getFormattedExpression());
//        
//        parser.parse("ShaderNode.var.xyz");
//        parser.parse("var.xyz");
//        parser.parse("ShaderNode.var");
//        parser.parse("var");
    }

    /**
     * parse a condition and returns the list of defines of this condition.
     * additionally this methods updates the formattedExpression with uppercased
     * defines names
     * 
     * supported expression syntax example: 
     * <code>
     * "(LightMap && SeparateTexCoord) || !ColorMap"
     * "#if (defined(LightMap) && defined(SeparateTexCoord)) || !defined(ColorMap)"
     * "#ifdef LightMap"
     * "#ifdef (LightMap && SeparateTexCoord) || !ColorMap"
     * </code>
     * 
     * @param expression the expression to parse
     * @return the list of defines
     */
    public List<String> extractDefines(String expression) {
        List<String> defines = new ArrayList<String>();
        expression = expression.replaceAll("#ifdef", "").replaceAll("#if", "").replaceAll("defined", "");
        Pattern pattern = Pattern.compile("(\\w+)");
        formattedExpression = expression;
        Matcher m = pattern.matcher(expression);
        while (m.find()) {
            String match = m.group();
            defines.add(match);
            formattedExpression = formattedExpression.replaceAll(match, "defined(" + match.toUpperCase() + ")");
        }
        return defines;
    }

    /**
     * 
     * @return the formatted expression previously updated by extractDefines
     */
    public String getFormattedExpression() {
        return formattedExpression;
    }
}
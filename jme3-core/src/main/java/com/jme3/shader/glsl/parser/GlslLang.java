package com.jme3.shader.glsl.parser;

import java.util.HashSet;
import java.util.Set;

/**
 * The class with language constants.
 *
 * @author JavaSaBr
 */
public class GlslLang {

    public static final String PR_TYPE_IF = "if";
    public static final String PR_TYPE_IFDEF = "ifdef";
    public static final String PR_TYPE_IFNDEF = "ifndef";
    public static final String PR_TYPE_ELIF = "elif";

    public static final String PR_TYPE_DEFINE = "define";
    public static final String PR_TYPE_UNDEF = "undef";
    public static final String PR_TYPE_ELSE = "else";
    public static final String PR_TYPE_ENDIF = "endif";
    public static final String PR_TYPE_ERROR = "error";
    public static final String PR_TYPE_PRAGMA = "pragma";
    public static final String PR_TYPE_EXTENSION = "extension";
    public static final String PR_TYPE_IMPORT = "import";
    public static final String PR_TYPE_VERSION = "version";
    public static final String PR_TYPE_LINE = "line";


    public static final String KW_IF = "if";
    public static final String KW_ELSE = "else";
    public static final String KW_DISCARD = "discard";
    public static final String KW_FOR = "for";

    public static final Set<String> KEYWORDS = new HashSet<>();

    static {
        KEYWORDS.add("uniform");
        KEYWORDS.add("in");
        KEYWORDS.add("out");
        KEYWORDS.add("varying");
        KEYWORDS.add("attribute");
        KEYWORDS.add("discard");
        KEYWORDS.add("if");
        KEYWORDS.add("elif");
        KEYWORDS.add("endif");
        KEYWORDS.add("defined");
        KEYWORDS.add("define");
        KEYWORDS.add("else");
        KEYWORDS.add("ifdef");
        KEYWORDS.add("ifndef");
        KEYWORDS.add("const");
        KEYWORDS.add("discard");
        KEYWORDS.add("break");
        KEYWORDS.add("continue");
        KEYWORDS.add("do");
        KEYWORDS.add("for");
        KEYWORDS.add("while");
        KEYWORDS.add("inout");
        KEYWORDS.add("struct");
        KEYWORDS.add("import");
    }

    public static final Set<String> PREPROCESSOR = new HashSet<>();
    public static final Set<String> PREPROCESSOR_WITH_CONDITION = new HashSet<>();

    static {
        PREPROCESSOR_WITH_CONDITION.add(PR_TYPE_IF);
        PREPROCESSOR_WITH_CONDITION.add(PR_TYPE_IFDEF);
        PREPROCESSOR_WITH_CONDITION.add(PR_TYPE_IFNDEF);
        PREPROCESSOR_WITH_CONDITION.add(PR_TYPE_ELIF);

        PREPROCESSOR.addAll(PREPROCESSOR_WITH_CONDITION);
        PREPROCESSOR.add(PR_TYPE_DEFINE);
        PREPROCESSOR.add(PR_TYPE_UNDEF);
        PREPROCESSOR.add(PR_TYPE_ELSE);
        PREPROCESSOR.add(PR_TYPE_ENDIF);
        PREPROCESSOR.add(PR_TYPE_ERROR);
        PREPROCESSOR.add(PR_TYPE_PRAGMA);
        PREPROCESSOR.add(PR_TYPE_EXTENSION);
        PREPROCESSOR.add(PR_TYPE_VERSION);
        PREPROCESSOR.add(PR_TYPE_LINE);
    }
}

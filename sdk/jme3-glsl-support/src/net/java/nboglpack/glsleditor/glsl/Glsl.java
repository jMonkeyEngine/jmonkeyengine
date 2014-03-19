/*
 * Glsl.java
 *
 * Created on 24.09.2007, 00:46:53
 *
 */
package net.java.nboglpack.glsleditor.glsl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.text.AbstractDocument;
import net.java.nboglpack.glsleditor.lexer.GlslTokenId;
import net.java.nboglpack.glsleditor.vocabulary.GLSLElementDescriptor;
import org.netbeans.api.languages.ASTItem;
import org.netbeans.api.languages.ASTNode;
import org.netbeans.api.languages.ASTToken;
import org.netbeans.api.languages.SyntaxContext;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.api.lexer.TokenUtilities;

/**
 * Utility methods called from GLSL.nbs.
 * @author Michael Bien
 */
public final class Glsl {

    private final static String KEYWORD_FONT_COLOR = "<font color=808080>";
    public final static Map<String, GLSLElementDescriptor> declaredFunctions = new HashMap<String, GLSLElementDescriptor>();

    private Glsl() {
    }

    /**
     * Assembles a human readable String containing the declaraton of a function.
     * Asumes that the current token of the SyntaxContext represents the function name.
     */
    public static final String createFunctionDeclarationString(SyntaxContext context) {

        AbstractDocument document = (AbstractDocument) context.getDocument();

        StringBuilder sb = new StringBuilder();

        try {

            document.readLock();

            TokenSequence sequence = TokenHierarchy.get(context.getDocument()).tokenSequence();
            sequence.move(context.getOffset());
            sequence.moveNext();

            sb.append("<html>");

            int moved = 0;
            while (sequence.movePrevious() && isIgnoredToken(sequence.token())) {
                moved++;
            }

            String type = sequence.token().toString();
            while (moved-- >= 0) {
                sequence.moveNext();
            }

            // append function name
            sb.append(sequence.token().text());

            while (!TokenUtilities.equals(sequence.token().text(), "(")) {
                sequence.moveNext();
            }

            sb.append("(");

            Token token;
            boolean first = true;
            while (sequence.moveNext() && !TokenUtilities.equals(sequence.token().text(), ")")) {

                token = sequence.token();

                if (!isIgnoredToken(token)) {

                    if (first) {
                        sb.append(KEYWORD_FONT_COLOR);
                    } else if (token.id() != GlslTokenId.COMMA && token.id() != GlslTokenId.BRACKET && token.id() != GlslTokenId.INTEGER_LITERAL) {
                        sb.append(" ");
                    }

                    if (!TokenUtilities.equals(token.text(), "void")) {

                        moved = 0;
                        while (sequence.moveNext() && isIgnoredToken(sequence.token())) {
                            moved++;
                        }

                        if (sequence.token().id() == GlslTokenId.COMMA || TokenUtilities.equals(sequence.token().text(), ")")) {
                            sb.append("</font>");
                        }

                        while (moved-- >= 0) {
                            sequence.movePrevious();
                        }

                        sb.append(token.text());

                        if (token.id() == GlslTokenId.COMMA) {
                            sb.append(KEYWORD_FONT_COLOR);
                        }
                    }

                    first = false;
                }

            }

            sb.append("</font>)");

            if (!"void".equals(type)) {
                sb.append(" : ");
                sb.append(KEYWORD_FONT_COLOR);
                sb.append(type);
                sb.append("</font>");
            }
            sb.append("</html>");

        } finally {
            document.readUnlock();
        }

        return sb.toString();
    }

    /**
     * Assambles a human readable String containing the declaraton of a field and the field name itself.
     * Asumes that the current token of the SyntaxContext represents the field name.
     */
    public static final String createFieldDeclarationString(SyntaxContext context) {

        AbstractDocument document = (AbstractDocument) context.getDocument();

        StringBuilder sb = new StringBuilder();

        try {

            document.readLock();

            TokenSequence sequence = TokenHierarchy.get(context.getDocument()).tokenSequence();
            sequence.move(context.getOffset());
            sequence.moveNext();

            sb.append("<html>");
            sb.append(sequence.token().text());
            sb.append(KEYWORD_FONT_COLOR);
            sb.append(" :");

            int insertIndex = sb.length();

            // read forward
            int moved = 0;
            Token token;
            while (sequence.moveNext()
                    && sequence.token().id() != GlslTokenId.SEMICOLON
                    && sequence.token().id() != GlslTokenId.COMMA
                    && sequence.token().id() != GlslTokenId.EQ) {
                token = sequence.token();
                if (!isIgnoredToken(token)) {
                    sb.append(token);
                }
                moved++;
            }
            while (moved-- >= 0) {
                sequence.movePrevious();
            }

            // read backwards throw the declaration
            boolean skipToken = false;

            while (sequence.movePrevious()
                    && sequence.token().id() != GlslTokenId.SEMICOLON
                    && sequence.token().id() != GlslTokenId.END_OF_LINE) {

                token = sequence.token();

                if (!isIgnoredToken(token)) {

                    // we have a struct declaration; skip everything between { }
                    if (token.id() == GlslTokenId.BRACE && TokenUtilities.equals(token.text(), "}")) {
                        movePreviousUntil(sequence, GlslTokenId.BRACE, "}", "{");
                        continue;
                    }

                    // skip token in case of an comma seperated identifier list
                    if (skipToken) {
                        if (token.id() == GlslTokenId.BRACKET
                                && TokenUtilities.equals(token.text(), "]")) {
                            movePreviousUntil(sequence, GlslTokenId.BRACKET, "]", "[");
                            skipToken = false;
                        } else {
                            skipToken = false;
                        }
                        continue;
                    }

                    if (token.id() == GlslTokenId.COMMA) {
                        skipToken = true;
                        continue;
                    }

                    if (!TokenUtilities.equals(token.text(), "struct")) {
                        sb.insert(insertIndex, token.text());
                        sb.insert(insertIndex, " ");
                    }
                }

            }

            sb.append("</font></html>");
        } finally {
            document.readUnlock();
        }


        return sb.toString();
    }

    public static final String createPreprocessorString(SyntaxContext context) {

        ASTNode node = (ASTNode) context.getASTPath().getLeaf();
        List<ASTItem> children = node.getChildren();

        String str = null;

        for (ASTItem item : children) {
            if (isTokenType(item, GlslTokenId.PREPROCESSOR.name())) {
                str = ((ASTToken) item).getIdentifier();
            }
        }


        for (int i = 0; i < str.length(); i++) {

            char c = str.charAt(i);

            if (c != '#' && !Character.isWhitespace(c)) {
                for (int j = str.length() - 1; j > i; j--) {
                    if (!Character.isWhitespace(str.charAt(j))) {
                        return str.substring(i, j + 1);
                    }
                }
            }

        }

        return str;
    }

    /**
     * Called from withen GLSL_*.nbs each time the document has been modified.
     */
    public static void process(SyntaxContext context) {

        AbstractDocument document = (AbstractDocument) context.getDocument();
        try {
            document.readLock();

            // remember all declared functions for auto-completion
            synchronized (declaredFunctions) {
                declaredFunctions.clear();
            }

            List<ASTItem> declarations = context.getASTPath().getLeaf().getChildren();

            for (ASTItem declaration : declarations) {

                for (ASTItem declarationItem : declaration.getChildren()) {

                    if (isNode(declarationItem, "function")) {

                        List<ASTItem> functionItems = declarationItem.getChildren();

                        if (functionItems.size() < 3) {
                            break;
                        }

                        ASTItem nameToken = functionItems.get(0);

                        if (isTokenType(nameToken, GlslTokenId.FUNCTION.name())) {

                            // determine return type
                            StringBuilder returnType = new StringBuilder();
                            for (ASTItem typeItem : declaration.getChildren()) {

                                if (isNode(typeItem, "function")) {
                                    break;
                                }

                                if (typeItem instanceof ASTNode) {
                                    returnType.append(((ASTNode) typeItem).getAsText().trim());
                                } else if (typeItem instanceof ASTToken) {
                                    final ASTToken t = (ASTToken) typeItem;
                                    returnType.append(t.getIdentifier().trim());
                                }

                            }

                            // determine name and parameter list
                            StringBuilder name = new StringBuilder();

                            name.append("(");
                            ASTItem parameterList = functionItems.get(2);
                            if (isNode(parameterList, "parameter_declaration_list")) {
                                name.append(((ASTNode) parameterList).getAsText());
                            }
                            name.append(")");

                            GLSLElementDescriptor elementDesc = new GLSLElementDescriptor(
                                    GLSLElementDescriptor.Category.USER_FUNC,
                                    "",
                                    "",
                                    name.toString(),
                                    returnType.toString());

                            name.insert(0, ((ASTToken) nameToken).getIdentifier());

                            synchronized (declaredFunctions) {
                                declaredFunctions.put(name.toString(), elementDesc);
                            }

//                            System.out.println("|"+returnType.toString()+"|"+name.toString()+"|");
                        }

                        break;
                    }
                }
            }
        } finally {
            document.readUnlock();
        }


    }

    private static void movePreviousUntil(TokenSequence sequence, GlslTokenId id, String countToken, String stopToken) {
        int counter = 1;
        while (sequence.movePrevious() && counter > 0) {
            if (sequence.token().id() == id) {
                if (TokenUtilities.equals(sequence.token().text(), stopToken)) {
                    counter--;
                } else if (TokenUtilities.equals(sequence.token().text(), countToken)) {
                    counter++;
                }
            }
        }
    }

    private static boolean isIgnoredToken(Token token) {
        return token.id() == GlslTokenId.WHITESPACE
                || token.id() == GlslTokenId.COMMENT
                || token.id() == GlslTokenId.PREPROCESSOR;
    }

    private static boolean isNode(ASTItem item, String nodeToken) {
        return item != null && item instanceof ASTNode && ((ASTNode) item).getNT().equals(nodeToken);
    }

    private static boolean isToken(ASTItem item, String id) {
        return item != null && item instanceof ASTToken && ((ASTToken) item).getIdentifier().equals(id);
    }

    private static boolean isTokenType(ASTItem item, String type) {
        return item != null && item instanceof ASTToken && ((ASTToken) item).getTypeName().equals(type);
    }
}

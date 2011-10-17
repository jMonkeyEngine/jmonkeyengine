/*
 * GlslLexer.java
 * 
 * Created on 19.08.2007, 18:31:16
 * 
 */

package net.java.nboglpack.glsleditor.lexer;

import net.java.nboglpack.glsleditor.GlslVocabularyManager;
import net.java.nboglpack.glsleditor.vocabulary.GLSLElementDescriptor;
import org.netbeans.api.lexer.Token;
import org.netbeans.spi.lexer.Lexer;
import org.netbeans.spi.lexer.LexerInput;
import org.netbeans.spi.lexer.LexerRestartInfo;
import org.netbeans.spi.lexer.TokenFactory;


/**
 * Lexer for the OpenGL Shading Language.
 * @author Michael Bien
 */
public class GlslLexer implements Lexer<GlslTokenId> {
 
 private final LexerInput input;
 private final TokenFactory<GlslTokenId> factory;
 private final GlslVocabularyManager manager;
 private final StringBuilder stringBuilder;
 
 
 public GlslLexer(LexerRestartInfo<GlslTokenId> info, GlslVocabularyManager manager) {
     this.input = info.input();
     this.factory = info.tokenFactory();
     this.manager = manager;
     this.stringBuilder = new StringBuilder();
 }

    @SuppressWarnings("fallthrough")
    public Token<GlslTokenId> nextToken() {
        
        int character = input.read();
        
        switch(character) {
            
            case '(':
            case ')':
                return factory.createToken(GlslTokenId.PAREN);
            case '{':
            case '}':
                return factory.createToken(GlslTokenId.BRACE);
            case '[':
            case ']':
                return factory.createToken(GlslTokenId.BRACKET);
            case '.':
                return factory.createToken(GlslTokenId.DOT);
            case ',':
                return factory.createToken(GlslTokenId.COMMA);
            case ':':
                return factory.createToken(GlslTokenId.COLON);
            case ';':
                return factory.createToken(GlslTokenId.SEMICOLON);
            case '?':
                return factory.createToken(GlslTokenId.QUESTION);
            case '~':
                return factory.createToken(GlslTokenId.TILDE);
            case '*':
                if(input.read() == '=') {
                    return factory.createToken(GlslTokenId.MUL_ASSIGN);
                }else{
                    input.backup(1);
                    return factory.createToken(GlslTokenId.STAR);
                }
            case '%':
                if(input.read() == '=') {
                    return factory.createToken(GlslTokenId.MOD_ASSIGN);
                }else{
                    input.backup(1);
                    return factory.createToken(GlslTokenId.PERCENT);
                }
            case '!':
                if(input.read() == '=') {
                    return factory.createToken(GlslTokenId.NE);
                }else{
                    input.backup(1);
                    return factory.createToken(GlslTokenId.BANG);
                }
            case '=':
                if(input.read() == '=') {
                    return factory.createToken(GlslTokenId.EQEQ);
                }else{
                    input.backup(1);
                    return factory.createToken(GlslTokenId.EQ);
                }
            case '^':
                switch(input.read()) {
                    case('^'):
                        return factory.createToken(GlslTokenId.CARETCARET);
                    case('='):
                        return factory.createToken(GlslTokenId.XOR_ASSIGN);
                    default:
                        input.backup(1);
                        return factory.createToken(GlslTokenId.CARET);
                }
            case '+':
                switch(input.read()) {
                    case('+'):
                        return factory.createToken(GlslTokenId.PLUSPLUS);
                    case('='):
                        return factory.createToken(GlslTokenId.ADD_ASSIGN);
                    default:
                        input.backup(1);
                        return factory.createToken(GlslTokenId.PLUS);
                }
            case '-':
                switch(input.read()) {
                    case('-'):
                        return factory.createToken(GlslTokenId.MINUSMINUS);
                    case('='):
                        return factory.createToken(GlslTokenId.SUB_ASSIGN);
                    default:
                        input.backup(1);
                        return factory.createToken(GlslTokenId.MINUS);
                }
            case '&':
                switch(input.read()) {
                    case('&'):
                        return factory.createToken(GlslTokenId.AMPAMP);
                    case('='):
                        return factory.createToken(GlslTokenId.AND_ASSIGN);
                    default:
                        input.backup(1);
                        return factory.createToken(GlslTokenId.AMP);
                }
            case '|':
                switch(input.read()) {
                    case('|'):
                        return factory.createToken(GlslTokenId.BARBAR);
                    case ('='):
                        return factory.createToken(GlslTokenId.OR_ASSIGN);
                    default:
                        input.backup(1);
                        return factory.createToken(GlslTokenId.BAR);
                }
            case '<':
                switch(input.read()) {
                    case('<'):
                        if(input.read() == '=') {
                            return factory.createToken(GlslTokenId.LEFT_BITSHIFT_ASSIGN);
                        }else{
                            input.backup(1);
                            return factory.createToken(GlslTokenId.LEFT_BITSHIFT);
                        }
                    case('='):
                        return factory.createToken(GlslTokenId.LE);
                    default:
                        input.backup(1);
                        return factory.createToken(GlslTokenId.LEFT_ANGLE);
                }
            case '>':
                switch(input.read()) {
                    case('>'):
                        if(input.read() == '=') {
                            return factory.createToken(GlslTokenId.RIGHT_BITSHIFT_ASSIGN);
                        }else{
                            input.backup(1);
                            return factory.createToken(GlslTokenId.RIGHT_BITSHIFT);
                        }
                    case('='):
                        return factory.createToken(GlslTokenId.GE);
                    default:
                        input.backup(1);
                        return factory.createToken(GlslTokenId.RIGHT_ANGLE);
                }
            case '/':
                int c = input.read();
                if(c == '/') {
                    readRemainingLine();
                    return factory.createToken(GlslTokenId.COMMENT);
                }else if(c == '*') {
                    return tokenizeMLComment();
                }else if(c == '=') {
                    return factory.createToken(GlslTokenId.DIV_ASSIGN);
                }else{
                    input.backup(1);
                    return factory.createToken(GlslTokenId.SLASH);
                }
            case '#':
                readRemainingLine();
                return factory.createToken(GlslTokenId.PREPROCESSOR);
            case ' ':
            case '\t':
                do{
                    character = input.read();
                }while(character == ' ' || character == '\t');
                input.backup(1);                
                return factory.createToken(GlslTokenId.WHITESPACE);
            case '\r':
                input.consumeNewline();
            case LexerInput.EOF:
                if(input.readLength() == 0)
                    return null;
            case '\n':
                return factory.createToken(GlslTokenId.END_OF_LINE);
            default:
                if(Character.isDigit(character)) {
                    return tokenizeNumber();
                }else if(Character.isUnicodeIdentifierStart(character)) {
                    return tokenizeName();
                }else{
                    return factory.createToken(GlslTokenId.error);
                }
        }
      
    }
    
    @SuppressWarnings("fallthrough")
    private final void readRemainingLine() {
        
        int character = input.read();
        
        while(character != LexerInput.EOF) {
            switch (character) {
                case '\r':
                    input.consumeNewline();
                case '\n':
                case LexerInput.EOF:
                    return;
            }
            character = input.read();
        }
        
    }
    
    private final Token<GlslTokenId> tokenizeMLComment() {
        
        int character = input.read();
        
        while(character != LexerInput.EOF) {
            if(character == '*' && input.read() == '/') {
                return factory.createToken(GlslTokenId.ML_COMMENT);
            }
            character = input.read();
        }
        return factory.createToken(GlslTokenId.ML_COMMENT);
        
    }
    
    private final Token<GlslTokenId> tokenizeNumber() {
        
        int character;
        
        do{
            character = input.read();
        }while(Character.isDigit(character));
        
        if(character == '.') {
            
            do{
                character = input.read();
            }while(Character.isDigit(character));
            
            if(character != 'f' && character != 'F')
                input.backup(1);
            
            return factory.createToken(GlslTokenId.FLOAT_LITERAL);
            
        }else{

            if(character != 'u' && character != 'U')
                input.backup(1);
            
//            input.backup(1);
            return factory.createToken(GlslTokenId.INTEGER_LITERAL);
            
        }
    }
    
    private final Token<GlslTokenId> tokenizeName() {
        
        if(stringBuilder.length() > 0)
            stringBuilder.delete(0, stringBuilder.length());
        
        // backup everything read
        input.backup(input.readLength()); 
        
        // assamble token
        char c;
        while(Character.isUnicodeIdentifierPart(c = ((char)input.read())))        
            stringBuilder.append(c);
        
        if(stringBuilder.length() > 0)
            input.backup(1);
                
        // categorise token
        GLSLElementDescriptor[] desc = manager.getDesc(stringBuilder.toString());
        
        if(desc != null) {
            
            if(desc[0].category != null) {
                
                if(desc[0].category == GLSLElementDescriptor.Category.BUILD_IN_FUNC) 
                    return factory.createToken(GlslTokenId.BUILD_IN_FUNC);

                if(desc[0].category == GLSLElementDescriptor.Category.BUILD_IN_VAR) 
                    return factory.createToken(GlslTokenId.BUILD_IN_VAR);
            
                return factory.createToken(GlslTokenId.KEYWORD);
                
            }
        }
        
        // check if token = function name
        int tokenEnd = input.readLength();
        int character = input.read();
        
        while(true) {
            switch (character) {
                case ' ':
                case '\t':
                case '\r':
                case '\n':
                    character = input.read();
                    break;
                case '(':
                    input.backup(input.readLength()-tokenEnd);
                    return factory.createToken(GlslTokenId.FUNCTION);
                default:
                    input.backup(input.readLength()-tokenEnd);
                    return factory.createToken(GlslTokenId.IDENTIFIER);
            }
        }
        
    }

    public Object state() {
        // we don't need states
        return null;
    }

    public void release() {
    }

}

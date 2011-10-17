/*
 * VertexLanguage.java
 * 
 * Created on 19.08.2007, 18:25:24
 * 
 */

package net.java.nboglpack.glsleditor.lexer;

import java.util.Collection;
import java.util.EnumSet;
import net.java.nboglpack.glsleditor.GlslVocabularyManager;
import net.java.nboglpack.glsleditor.dataobject.GlslFragmentShaderDataLoader;
import net.java.nboglpack.glsleditor.dataobject.GlslGeometryShaderDataLoader;
import net.java.nboglpack.glsleditor.dataobject.GlslVertexShaderDataLoader;
import org.netbeans.api.lexer.Language;
import org.netbeans.spi.lexer.LanguageHierarchy;
import org.netbeans.spi.lexer.Lexer;
import org.netbeans.spi.lexer.LexerRestartInfo;

/**
 * OpenGL Shading Language.
 * @author Michael Bien
 */
public class Glsl extends LanguageHierarchy<GlslTokenId> {
 
 public static final Glsl VERTEX_LANGUAGE = new Glsl(GlslVertexShaderDataLoader.REQUIRED_MIME);
 public static final Glsl GEOMETRY_LANGUAGE = new Glsl(GlslGeometryShaderDataLoader.REQUIRED_MIME);
 public static final Glsl FRAGMENT_LANGUAGE = new Glsl(GlslFragmentShaderDataLoader.REQUIRED_MIME);
 
 private final String mimeType;
 
 private Glsl(String mimeType) {
     this.mimeType = mimeType;
 }
 
    @Override
    protected String mimeType() {
        return mimeType;
    }

    @Override
    protected Collection<GlslTokenId> createTokenIds() {
        return EnumSet.allOf(GlslTokenId.class);
    }
    
    @Override
    protected Lexer<GlslTokenId> createLexer(LexerRestartInfo<GlslTokenId> info) {
        return new GlslLexer(info, GlslVocabularyManager.getInstance(mimeType()));
    }
    
    public static Language<GlslTokenId> vertexLanguage(){
        return VERTEX_LANGUAGE.language();
    }
    public static Language<GlslTokenId> fragmentLanguage(){
        return FRAGMENT_LANGUAGE.language();
    }
    public static Language<GlslTokenId> geometryLanguage(){
        return GEOMETRY_LANGUAGE.language();
    }
}

/*
 * GLSLVocabulary.java
 *
 * Created on 03.06.2007, 23:03:57
 *
 */
package net.java.nboglpack.glsleditor.vocabulary;

import java.util.HashMap;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Michael Bien
 */
@XmlRootElement
public class GLSLVocabulary {

    public final HashMap<String, GLSLElementDescriptor[]> mainVocabulary;
    public final HashMap<String, GLSLElementDescriptor[]> fragmentShaderVocabulary;
    public final HashMap<String, GLSLElementDescriptor[]> vertexShaderVocabulary;
    public final HashMap<String, GLSLElementDescriptor[]> geometryShaderVocabulary;

    public GLSLVocabulary() {
        mainVocabulary = new HashMap<String, GLSLElementDescriptor[]>();
        vertexShaderVocabulary = new HashMap<String, GLSLElementDescriptor[]>();
        fragmentShaderVocabulary = new HashMap<String, GLSLElementDescriptor[]>();
        geometryShaderVocabulary = new HashMap<String, GLSLElementDescriptor[]>();
    }
}

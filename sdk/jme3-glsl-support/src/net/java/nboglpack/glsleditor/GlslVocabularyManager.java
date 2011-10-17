/*
 * GlslVocabularyManager.java
 *
 * Created on 12. Februar 2006, 03:37
 *
 */

package net.java.nboglpack.glsleditor;

import java.io.InputStream;
import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import net.java.nboglpack.glsleditor.dataobject.GlslFragmentShaderDataLoader;
import net.java.nboglpack.glsleditor.dataobject.GlslGeometryShaderDataLoader;
import net.java.nboglpack.glsleditor.dataobject.GlslVertexShaderDataLoader;
import net.java.nboglpack.glsleditor.vocabulary.GLSLElementDescriptor;
import net.java.nboglpack.glsleditor.vocabulary.GLSLVocabulary;
import org.openide.ErrorManager;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 *
 * @author mhenze
 * @author Michael Bien
 */
public class GlslVocabularyManager {
    
 private static final ErrorManager LOGGER = ErrorManager.getDefault().getInstance(GlslVocabularyManager.class.getName());
 private static final HashMap<String, GlslVocabularyManager> instances = new HashMap<String, GlslVocabularyManager>();
 private static GLSLVocabulary vocabulary = null;

 private final String mimetype;
 private final Set<String> keySet;
 private final Map<String, GLSLElementDescriptor[]> vocabularyExtention;
    
    /** Creates a new instance of GlslVocabularyManager */
    private GlslVocabularyManager(String mimetype) {
        
        if(    !mimetype.equals(GlslFragmentShaderDataLoader.REQUIRED_MIME)
            && !mimetype.equals(GlslVertexShaderDataLoader.REQUIRED_MIME)
            && !mimetype.equals(GlslGeometryShaderDataLoader.REQUIRED_MIME)) {
            throw new IllegalArgumentException(mimetype+" is no GLSL mime type");
        }
        
        
        this.mimetype = mimetype;
        
        if(vocabulary == null)
            loadVocabulary();
        
        if(mimetype.equals(GlslFragmentShaderDataLoader.REQUIRED_MIME)) {
            vocabularyExtention = vocabulary.fragmentShaderVocabulary;
        }else if(mimetype.equals(GlslVertexShaderDataLoader.REQUIRED_MIME)) {
            vocabularyExtention = vocabulary.vertexShaderVocabulary;
        }else {
            vocabularyExtention = vocabulary.geometryShaderVocabulary;
        }
        
        // merges two views
        keySet = new AbstractSet<String>() {
            
            private final Set<String> mainSet = vocabulary.mainVocabulary.keySet();
            private final Set<String> extSet = vocabularyExtention.keySet();
            
            
            public Iterator<String> iterator() {
                return new Iterator<String>(){
                    
                    Iterator<String> mainIt = mainSet.iterator();
                    Iterator<String> extIt = extSet.iterator();
                    
                    public boolean hasNext() {
                        return mainIt.hasNext() || extIt.hasNext();
                    }
                    
                    public String next() {
                        if(mainIt.hasNext())
                            return mainIt.next();
                        else
                            return extIt.next();
                    }
                    
                    public void remove() {
                        throw new UnsupportedOperationException("Not supported yet.");
                    }
                    
                };
            }
            
            public int size() {
                return mainSet.size()+extSet.size();
            }
            
        };
        
    }
    
    public static GlslVocabularyManager getInstance(String mimetype) {
        
        GlslVocabularyManager instance = instances.get(mimetype);
        
        if(instance == null) {
            instance = new GlslVocabularyManager(mimetype);
            instances.put(mimetype,instance);
        }
        
        return instance;
    }
    
    private void loadVocabulary() {
        
        FileObject vocabularyfile = FileUtil.getConfigFile("Editors/"+mimetype+"/vocabulary.xml");
        
        if (vocabularyfile != null) {
            
            InputStream is = null;
            
            try {
                
                is = vocabularyfile.getInputStream();
                
                // workaround; nb does not allow usage of jdk's jaxb implementation
                // => we have to provide JAXB as library wrapper module and load it via module classloader
                JAXBContext jc = JAXBContext.newInstance("net.java.nboglpack.glsleditor.vocabulary", this.getClass().getClassLoader());

                Unmarshaller unmarshaller = jc.createUnmarshaller();
                vocabulary = (GLSLVocabulary)unmarshaller.unmarshal(is);

            } catch (Exception ex) {
                
                // create empty vocab
                vocabulary = new GLSLVocabulary();
                LOGGER.notify(ex);
                
            } finally {
                
                if(is != null) {
                    try {
                        is.close();
                    } catch (Exception e) {
                        LOGGER.notify(e);
                    }
                }
                
            }
            
        }
        
    }
    
    public Set getKeys() {
        return keySet;
    }
    
    public GLSLElementDescriptor[] getDesc(String key) {
        
        GLSLElementDescriptor[] desc = vocabulary.mainVocabulary.get(key);
        
        if(desc == null)
            desc = vocabularyExtention.get(key);
        
        return desc;
    }
    
    
}

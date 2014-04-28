package net.java.nboglpack.glsleditor.dataobject;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import javax.swing.text.Document;
import net.java.nboglpack.glsleditor.GlslShaderFileObserver;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;
import org.openide.nodes.CookieSet;
import org.openide.nodes.Node;
import org.openide.text.CloneableEditorSupport;
import org.openide.util.Lookup;
import org.openide.text.DataEditorSupport;

/**
 * @author Michael Bien
 */
public class GlslGeometryShaderDataObject extends MultiDataObject {

    private GlslShaderFileObserver observer;

    public GlslGeometryShaderDataObject(FileObject pf, GlslGeometryShaderDataLoader loader) throws DataObjectExistsException, IOException {

        super(pf, loader);

        CookieSet cookies = getCookieSet();
        observer = new GlslShaderFileObserver(this);

        final CloneableEditorSupport support = DataEditorSupport.create(this, getPrimaryEntry(), cookies);
        support.addPropertyChangeListener(new PropertyChangeListenerImpl(support));
        cookies.add((Node.Cookie) support);
    }

    @Override
    protected Node createNodeDelegate() {
        return new GlslGeometryShaderDataNode(this, getLookup());
    }

    @Override
    public Lookup getLookup() {
        return getCookieSet().getLookup();
    }

    private class PropertyChangeListenerImpl implements PropertyChangeListener {

        private final CloneableEditorSupport support;

        public PropertyChangeListenerImpl(CloneableEditorSupport support) {
            this.support = support;
        }

        public void propertyChange(PropertyChangeEvent event) {
            if ("document".equals(event.getPropertyName())) {
                if (event.getNewValue() != null) {
                    support.getDocument().addDocumentListener(observer);
                    observer.runCompileTask();
                } else if (event.getOldValue() != null) {
                    // cylab: I think this is never called.
                    // But I don't know if unregistering the observer makes any difference...
                    ((Document) event.getOldValue()).removeDocumentListener(observer);
                }
            }
        }
    }
}

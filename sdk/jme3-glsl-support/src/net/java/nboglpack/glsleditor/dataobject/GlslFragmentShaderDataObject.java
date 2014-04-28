package net.java.nboglpack.glsleditor.dataobject;

import net.java.nboglpack.glsleditor.GlslShaderFileObserver;
import java.beans.PropertyChangeEvent;
import java.io.IOException;
import java.beans.PropertyChangeListener;
import javax.swing.text.Document;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;
import org.openide.nodes.CookieSet;
import org.openide.nodes.Node;
import org.openide.text.CloneableEditorSupport;
import org.openide.text.DataEditorSupport;

public class GlslFragmentShaderDataObject extends MultiDataObject {

    private final GlslShaderFileObserver observer;

    public GlslFragmentShaderDataObject(FileObject pf, GlslFragmentShaderDataLoader loader) throws DataObjectExistsException, IOException {

        super(pf, loader);

        CookieSet cookies = getCookieSet();
        observer = new GlslShaderFileObserver(this);

        final CloneableEditorSupport support = DataEditorSupport.create(this, getPrimaryEntry(), cookies);
        support.addPropertyChangeListener(
                new PropertyChangeListenerImpl(support));
        cookies.add((Node.Cookie) support);
    }

    @Override
    protected Node createNodeDelegate() {
        return new GlslFragmentShaderDataNode(this);
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

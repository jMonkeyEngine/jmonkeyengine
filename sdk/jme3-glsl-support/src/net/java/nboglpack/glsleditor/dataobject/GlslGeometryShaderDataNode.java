package net.java.nboglpack.glsleditor.dataobject;

import org.openide.loaders.DataNode;
import org.openide.nodes.Children;
import org.openide.util.Lookup;


/**
 * @author Michael Bien
 */
public class GlslGeometryShaderDataNode extends DataNode {
    
    public GlslGeometryShaderDataNode(GlslGeometryShaderDataObject obj) {
        super(obj, Children.LEAF);
        setIconBaseWithExtension(GlslGeometryShaderDataLoaderBeanInfo.IMAGE_ICON_BASE);
    }
    GlslGeometryShaderDataNode(GlslGeometryShaderDataObject obj, Lookup lookup) {
        super(obj, Children.LEAF, lookup);
        setIconBaseWithExtension(GlslGeometryShaderDataLoaderBeanInfo.IMAGE_ICON_BASE);
    }
    
    //    /** Creates a property sheet. */
    //    protected Sheet createSheet() {
    //        Sheet s = super.createSheet();
    //        Sheet.Set ss = s.get(Sheet.PROPERTIES);
    //        if (ss == null) {
    //            ss = Sheet.createPropertiesSet();
    //            s.put(ss);
    //        }
    //        // TODO add some relevant properties: ss.put(...)
    //        return s;
    //    }
    
}

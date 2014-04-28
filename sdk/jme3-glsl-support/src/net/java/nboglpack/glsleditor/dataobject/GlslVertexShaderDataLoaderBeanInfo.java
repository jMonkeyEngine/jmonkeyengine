package net.java.nboglpack.glsleditor.dataobject;

import java.awt.Image;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.SimpleBeanInfo;
import org.openide.loaders.UniFileLoader;
import org.openide.util.ImageUtilities;

public class GlslVertexShaderDataLoaderBeanInfo extends SimpleBeanInfo {

    public static final String IMAGE_ICON_BASE = "net/java/nboglpack/glsleditor/resources/VertexShaderIcon.gif";

    @Override
    public BeanInfo[] getAdditionalBeanInfo() {
        try {
            return new BeanInfo[]{Introspector.getBeanInfo(UniFileLoader.class)};
        } catch (IntrospectionException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public Image getIcon(int type) {
        if (type == BeanInfo.ICON_COLOR_16x16 || type == BeanInfo.ICON_MONO_16x16) {
            return ImageUtilities.loadImage(IMAGE_ICON_BASE);
        } else {
            return null;
        }
    }
}

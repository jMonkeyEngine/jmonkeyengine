/*
 * JavaSourceFileLayerPaletteFactory.java
 *
 * Created on Jun 4, 2007, 12:33:34 PM
 *
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jme3.gde.gui.palette;

import jada.ngeditor.guiviews.DND.WidgetData;
import java.io.IOException;
import javax.swing.Action;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.spi.palette.DragAndDropHandler;
import org.netbeans.spi.palette.PaletteActions;
import org.netbeans.spi.palette.PaletteController;
import org.netbeans.spi.palette.PaletteFactory;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.datatransfer.ExTransferable;

/**
 *
 * @author gw152771
 */
public class NiftyGUIPaletteFactory {
    //TODO: Create TopComponent for layout and use Palette:
    //http://blogs.sun.com/geertjan/entry/convert_your_topcomponent_to_a
    //http://www.javanb.com/netbeans/1/19785.html
    
    public static final String PALETTE_FOLDER = "NiftyPalette";
    private static PaletteController palette = null;
    
    public NiftyGUIPaletteFactory() {
    }
   @MimeRegistration(mimeType = "text/x-niftygui+xml", service = PaletteController.class)
    public static PaletteController createPalette() {
       
            if (null == palette){
                AbstractNode paletteRoot = new AbstractNode(Children.create(new CategoryChildFactory(), true));
                paletteRoot.setName("Palette Root");
                palette = PaletteFactory.createPalette( paletteRoot,new MyActions(),null,new MyHandler());
            }
        
            return palette;
    }
    
    private static class MyActions extends PaletteActions {
        
        //Add new buttons to the Palette Manager here:
        public Action[] getImportActions() {
            return null;
        }
        
        //Add new contextual menu items to the palette here:
        public Action[] getCustomPaletteActions() {
            return null;
        }
        
        //Add new contextual menu items to the categories here:
        public Action[] getCustomCategoryActions(Lookup arg0) {
            return null;
        }
        
        //Add new contextual menu items to the items here:
        public Action[] getCustomItemActions(Lookup arg0) {
            return null;
        }
        
        //Define the default action here:
        public Action getPreferredAction(Lookup arg0) {
            return null;
        }
        
    }

    private static class MyHandler extends DragAndDropHandler {

        public MyHandler() {
        }

        @Override
        public void customize(ExTransferable t, Lookup item) {
            t.remove(WidgetData.POINTFLAVOR);
        }
    }
    
}

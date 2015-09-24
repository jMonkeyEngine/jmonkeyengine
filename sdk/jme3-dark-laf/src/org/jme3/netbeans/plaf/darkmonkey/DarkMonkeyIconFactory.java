/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jme3.netbeans.plaf.darkmonkey;

import com.nilo.plaf.nimrod.NimRODIconFactory;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferFloat;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.Serializable;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.plaf.UIResource;

/**
 * This class provides for overrides on the system Icons from the
 * NimROD look and feel
 * @author charles
 */
public class DarkMonkeyIconFactory extends NimRODIconFactory{
    private static Icon treeCollapsedIcon;
    private static Icon treeExpandedIcon;
    
    public static Icon getTreeCollapsedIcon(){
        if(treeCollapsedIcon == null){
            treeCollapsedIcon = new TreeCollapsedIcon();
        }
        
        return treeCollapsedIcon;
    }
    public static Icon getTreeExpandedIcon(){
        if(treeExpandedIcon == null){
            treeExpandedIcon = new TreeExpandedIcon();
        }
        
        return treeExpandedIcon;
    }

   
    private static class TreeCollapsedIcon implements Icon, UIResource, Serializable{
        private int w, h;
        ImageIcon preProcessed;
        {
            w = 18;
            h = 18;
            preProcessed = null;
        } 
        
        public TreeCollapsedIcon(){ //maybe THIS is all I need, eh?
            w = 18;
            h = 18;
            preProcessed = null;
        }
        
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            if(preProcessed != null){
                preProcessed.paintIcon(c, g, x, y);
                return;
            }       

            //process for first time, unless this gets "uninitialized" by
            // UIResource calls;
            BufferedImage bi = DMUtils.loadImagefromJar(this, "icons/nehonC2.png");
            // start the experiments!
            
            Color[] normColorSet = {null, DarkMonkeyLookAndFeel.getWhite(), 
                null, DarkMonkeyLookAndFeel.getPrimaryControl()};
            bi = DMUtils.paletteSwapARGB8(normColorSet, bi);
            // end experiment, back to old code
            ImageIcon ii = new ImageIcon(bi);
            Image scaled = ii.getImage();
            ImageIcon preProcess = new ImageIcon(scaled.getScaledInstance(w, h, Image.SCALE_SMOOTH));
            preProcess.paintIcon(c, g, x, y);
            preProcessed = preProcess;
        }

        @Override
        public int getIconWidth() {
            return w; 
        }

        @Override
        public int getIconHeight() {
            return h; 
        }
        
    }
    
    private static class TreeExpandedIcon implements Icon, UIResource, Serializable{
        private int w, h;
        ImageIcon preProcessed;
        {
            w = 18;
            h = 18;
            preProcessed = null;
        } 

         
        public TreeExpandedIcon(){ //maybe THIS is all I need, eh?
            w = 18;
            h = 18;
            preProcessed = null;
        }
        
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            if(preProcessed != null){
                preProcessed.paintIcon(c, g, x, y);
                return;
            }       

            //process for first time, unless this gets "uninitialized" by
            // UIResource calls;
            
            BufferedImage bi = DMUtils.loadImagefromJar(this, "icons/nehonE2.png");
            Color[] normColorSet = { DarkMonkeyLookAndFeel.getWhite(), 
                null, DarkMonkeyLookAndFeel.getPrimaryControl()};
            bi = DMUtils.paletteSwapARGB8(normColorSet, bi);
            
            ImageIcon ii = new ImageIcon(bi);
            Image scaled = ii.getImage();
            
            ImageIcon preProcess = new ImageIcon(scaled.getScaledInstance(w, h, Image.SCALE_DEFAULT));
            
            preProcess.paintIcon(c, g, x, y);
            preProcessed = preProcess;
        }
        
        @Override
        public int getIconWidth() {
            return w; 
        }

        @Override
        public int getIconHeight() {
            return h; 
        }
        
    }
    
 

}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.gui.nodes;

import jada.ngeditor.model.GUI;
import jada.ngeditor.model.elements.specials.GUseControls;
import jada.ngeditor.model.elements.specials.GUseStyle;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;

/**
 *
 * @author cris
 */
public class GUINode extends AbstractNode{
    private final GUI gui;
    private static final String basePath="com/jme3/gde/gui/multiview/icons";
    public GUINode(GUI gui) {
        super(Children.create(new ScreenChildFactory(gui),false));
        this.gui = gui;
        this.setIconBaseWithExtension(basePath+"/"+"game-monitor"+".png");
        
    }

    /**
     * @return the gui
     */
    public GUI getGui() {
        return gui;
    }

    @Override
    public Action[] getActions(boolean context) {
        if(true){
        return new Action[]{new AddStyleAction(),new AddControlAction()}; //To change body of generated methods, choose Tools | Templates.
        }else
            return super.getActions();
   }

    
    
    @Override
    public String getName() {
        return super.getName()+" "+this.gui; //To change body of generated methods, choose Tools | Templates.
    }

    private class AddStyleAction extends AbstractAction {

        public AddStyleAction() {
            super("Add style");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
        JFileChooser chooser = new JFileChooser(gui.getAssetFolder());
        int result = chooser.showSaveDialog(null);
        if(result == JFileChooser.APPROVE_OPTION){
            GUseStyle style = new GUseStyle();
           
       
                style.setFilename(createReletive(chooser.getSelectedFile()));
                gui.addLoadUseStyle(style);
           
            
        }
       }
           
    }
     private class AddControlAction extends AbstractAction {

        public  AddControlAction() {
            super("Add Control");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
        JFileChooser chooser = new JFileChooser(gui.getAssetFolder());
        int result = chooser.showSaveDialog(null);
        if(result == JFileChooser.APPROVE_OPTION){
             GUseControls controls = new GUseControls();
           
       
                controls.setFilename(createReletive(chooser.getSelectedFile()));
                gui.addLoadUseControls(controls);
           
            
        }
        }
        
      
        
    }
     
     private String createReletive(File selected) {
        File assets = gui.getAssetFolder();
        String res = "";
       String parentPath = selected.getParent();
       String absAssets = assets.getAbsolutePath();
       if (!parentPath.contains(absAssets)) {
           try {
               absAssets = assets.getCanonicalPath();
               if (!parentPath.contains(absAssets)) {
                   JOptionPane.showMessageDialog(null, "Sorry you can't relativize this file. Tip : the file must be inside the assets folder");
               } else {
                   res = assets.toURI().relativize(selected.toURI()).getPath();
               }
           } catch (IOException ex) {
               JOptionPane.showMessageDialog(null, "Sorry you can't relativize this file");
           }
       } else {
           res = assets.toURI().relativize(selected.toURI()).getPath();
       }
       return res;      
    }
    
}

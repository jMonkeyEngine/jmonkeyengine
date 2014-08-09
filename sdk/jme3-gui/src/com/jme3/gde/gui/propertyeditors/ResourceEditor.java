/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.gui.propertyeditors;

import jada.ngeditor.controller.CommandProcessor;
import jada.ngeditor.guiviews.editors.FileChooserEditor;
import jada.ngeditor.model.GuiEditorModel;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyEditorSupport;
import java.io.File;
import java.util.Observable;
import java.util.Observer;
import javax.swing.JFileChooser;

/**
 *
 * @author cris
 */
public class ResourceEditor extends PropertyEditorSupport implements Observer, ActionListener{
    private final FileChooserEditor fileChooserEditor;
    private File assetFolder;
    

    public ResourceEditor() {
      CommandProcessor.getInstance().getObservable().addObserver(this);
     
      GuiEditorModel model = (GuiEditorModel)  CommandProcessor.getInstance().getObservable();
      this.assetFolder = model.getCurrent().getAssetFolder();
      fileChooserEditor = new FileChooserEditor(this.assetFolder);
      fileChooserEditor.getFileChooser().addActionListener(this);
    }
    
    
    @Override
    public Component getCustomEditor() {
   
        return fileChooserEditor.getFileChooser(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean supportsCustomEditor() {
        return true; //To change body of generated methods, choose Tools | Templates.
    }

    

    @Override
    public void update(Observable o, Object arg) {
       if(o instanceof GuiEditorModel){
           GuiEditorModel model = (GuiEditorModel) o;
           this.assetFolder = model.getCurrent().getAssetFolder();
       }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (JFileChooser.APPROVE_SELECTION.equals(e.getActionCommand())) {
            this.setValue(fileChooserEditor.traslateFile());
        }
    }
    
    
    
    
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.textureeditor;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;

public final class OpenTexture implements ActionListener {

    private final DataObject context;

    public OpenTexture(DataObject context) {
        this.context = context;
    }

    public void actionPerformed(ActionEvent ev) {
        FileObject file = context.getPrimaryFile();
        ImageEditorTopComponent display = new ImageEditorTopComponent();
        try {
            display.setEditedImage(file);
            display.open();
            display.requestActive();
        } catch (URISyntaxException ex) {
            Exceptions.printStackTrace(ex);
        } catch (FileNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
}

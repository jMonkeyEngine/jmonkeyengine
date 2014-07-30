/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.textureeditor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import org.openide.awt.*;
import org.openide.loaders.DataObject;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.WindowManager;

@ActionID(
		category = "Images",
		id = "com.jme3.gde.textureeditor.ConvertTextureAction"
)
@ActionRegistration(
		displayName = "#CTL_ConvertTextureAction"
)
@ActionReferences({
	@ActionReference(path = "Menu/Tools/Textures", position = 0),
	@ActionReference(path = "Loaders/image/x-jmetexture/Actions", position = 100),
	@ActionReference(path = "Loaders/image/png-gif-jpeg-bmp/Actions", position = 250)
})
@Messages("CTL_ConvertTextureAction=Convert Texture")
public final class ConvertTextureAction implements ActionListener {

	private final List<DataObject> context;

	public ConvertTextureAction(List<DataObject> context) {
		this.context = context;
	}

	@Override
	public void actionPerformed(ActionEvent ev) {
		ConvertTextureDialog dialog = new ConvertTextureDialog(WindowManager.getDefault().getMainWindow(), true);
		dialog.setTextures(context);
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
	}
}

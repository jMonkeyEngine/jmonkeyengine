/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jme3.gde.textureeditor;

import dds.jogl.DDSImage;
import dds.model.*;
import gr.zdimensions.jsquish.Squish;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.*;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;

/**
 *
 * @author Sebastian Weiss
 */
public class ConvertTextureDialog extends javax.swing.JDialog {
	private static final Logger LOG = Logger.getLogger(ConvertTextureDialog.class.getName());
	/**
	 * A return status code - returned if Cancel button has been pressed
	 */
	public static final int RET_CANCEL = 0;
	/**
	 * A return status code - returned if OK button has been pressed
	 */
	public static final int RET_OK = 1;
	
	private List<DataObject> textures;

	/**
	 * Creates new form ConvertTextureDialog
	 */
	public ConvertTextureDialog(java.awt.Frame parent, boolean modal) {
		super(parent, modal);
		initComponents();

		// Close the dialog when Esc is pressed
		String cancelName = "cancel";
		InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), cancelName);
		ActionMap actionMap = getRootPane().getActionMap();
		actionMap.put(cancelName, new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				doClose(RET_CANCEL);
			}
		});
	}
	
	public void setTextures(List<DataObject> textures) {
		this.textures = textures;
		DefaultListModel<String> model = new DefaultListModel<String>();
		for (DataObject tex : textures) {
			model.addElement(tex.getPrimaryFile().getNameExt());
		}
		filesList.setModel(model);
	}
	
	private void convert() {
		//init progress
		progressBar.setMaximum(textures.size());
		
		//start thread
		Converter c = new Converter();
		c.execute();
		
		//disable buttons
		cancelButton.setEnabled(false);
		compressionComboBox.setEnabled(false);
		fileFormatComboBox.setEnabled(false);
		mipmapsCheckBox.setEnabled(false);
		okButton.setEnabled(false);
		overwriteCheckBox.setEnabled(false);
	}
	
	private class Converter extends SwingWorker<Void, Integer> {

		@Override
		protected Void doInBackground() throws Exception {
			IOModule io = IOModule.create();
			String extension = (String) fileFormatComboBox.getSelectedItem();
			
			int i = 0;
			for (DataObject tex : textures) {
				convert(tex, io, extension);

				//increase progress
				++i;
				publish(i);
			}
			return null;
		}

		@Override
		protected void process(List<Integer> chunks) {
			int i = chunks.get(chunks.size()-1);
			progressBar.setValue(i);
		}

		@Override
		protected void done() {
			doClose(RET_OK);
		}
		
		private void convert(DataObject tex, IOModule io, String extension) {
			try {
				//load source
				File source = FileUtil.toFile(tex.getPrimaryFile());
				File parent = source.getParentFile();
				BufferedImage sourceImg = io.load(tex.getPrimaryFile());

				//create target file
				File target = new File(parent, tex.getPrimaryFile().getName() + "." + extension);
				if (target.exists() && !overwriteCheckBox.isSelected()) {
					//confirm overwriting
					int a = JOptionPane.showConfirmDialog(ConvertTextureDialog.this, "Overwrite existing file? \n"
							+ target.getPath(),
							"Confirm", JOptionPane.YES_NO_OPTION);
					if (a != JOptionPane.NO_OPTION) {
						//Cancel
						return;
					}
				}

				//save file
				if ("dds".equals(extension)) {
					//save dds
					storeDDS(sourceImg, target);
				} else {
					io.store(sourceImg, extension, target);
				}

			} catch (IOException ex) {
				Exceptions.printStackTrace(ex);
			} catch (URISyntaxException ex) {
				Exceptions.printStackTrace(ex);
			}
		}
	}
	
	private void storeDDS(BufferedImage img, File file) throws IOException {
		boolean compressed = compressionComboBox.getSelectedIndex() > 0 && img.getWidth()==img.getHeight();
		if (!compressed) {
			//swap red and blue channel
			Image image = Toolkit.getDefaultToolkit().createImage(
						new FilteredImageSource(img.getSource(), new IOModule.RedBlueSwapFilter()));
			img = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_4BYTE_ABGR);
			Graphics g = img.getGraphics();
			g.drawImage(image, 0, 0, null);
			g.dispose();
		} else {
			if (img.getType()!=BufferedImage.TYPE_4BYTE_ABGR) {
				//convert
				BufferedImage image = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
				Graphics g = image.getGraphics();
				g.drawImage(img, 0, 0, null);
				g.dispose();
				img = image;
			}
		}
		//create dds data
		TextureMap tex;
		if (mipmapsCheckBox.isSelected()) {
			tex = new MipMaps();
			((MipMaps) tex).generateMipMaps(img);
		} else {
			tex = new SingleTextureMap(img);
		}
		//create dds image
		ByteBuffer[] data = null;
		int format = 0;
		if (!compressed) {
			//no compression or image not square			
			data = tex.getUncompressedBuffer(); 
			format = DDSImage.D3DFMT_A8R8G8B8;
		} else {
			//compression
			switch (compressionComboBox.getSelectedIndex()) {
				case 1:
					format = DDSImage.D3DFMT_DXT1;
					data = tex.getDXTCompressedBuffer(Squish.CompressionType.DXT1);
					break;
				case 2:
					format = DDSImage.D3DFMT_DXT3;
					data = tex.getDXTCompressedBuffer(Squish.CompressionType.DXT3);
					break;
				case 3:
					format = DDSImage.D3DFMT_DXT5;
					data = tex.getDXTCompressedBuffer(Squish.CompressionType.DXT5);
					break;
				default:
					throw new IllegalArgumentException("unknown compression type "+
							compressionComboBox.getSelectedItem());
			}
		}
		DDSImage dds = DDSImage.createFromData(format, img.getWidth(), img.getHeight(), data);
		//save asset
		dds.write(file);
	}

	/**
	 * @return the return status of this dialog - one of RET_OK or RET_CANCEL
	 */
	public int getReturnStatus() {
		return returnStatus;
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        filesList = new javax.swing.JList();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        overwriteCheckBox = new javax.swing.JCheckBox();
        mipmapsCheckBox = new javax.swing.JCheckBox();
        compressionComboBox = new javax.swing.JComboBox();
        fileFormatComboBox = new javax.swing.JComboBox();
        progressBar = new javax.swing.JProgressBar();

        setTitle(org.openide.util.NbBundle.getMessage(ConvertTextureDialog.class, "ConvertTextureDialog.title")); // NOI18N
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(okButton, org.openide.util.NbBundle.getMessage(ConvertTextureDialog.class, "ConvertTextureDialog.okButton.text")); // NOI18N
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(cancelButton, org.openide.util.NbBundle.getMessage(ConvertTextureDialog.class, "ConvertTextureDialog.cancelButton.text")); // NOI18N
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(ConvertTextureDialog.class, "ConvertTextureDialog.jLabel1.text")); // NOI18N

        filesList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane1.setViewportView(filesList);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(ConvertTextureDialog.class, "ConvertTextureDialog.jLabel2.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel3, org.openide.util.NbBundle.getMessage(ConvertTextureDialog.class, "ConvertTextureDialog.jLabel3.text")); // NOI18N
        jLabel3.setToolTipText(org.openide.util.NbBundle.getMessage(ConvertTextureDialog.class, "ConvertTextureDialog.jLabel3.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel5, org.openide.util.NbBundle.getMessage(ConvertTextureDialog.class, "ConvertTextureDialog.jLabel5.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(overwriteCheckBox, org.openide.util.NbBundle.getMessage(ConvertTextureDialog.class, "ConvertTextureDialog.overwriteCheckBox.text")); // NOI18N

        mipmapsCheckBox.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(mipmapsCheckBox, org.openide.util.NbBundle.getMessage(ConvertTextureDialog.class, "ConvertTextureDialog.mipmapsCheckBox.text")); // NOI18N

        compressionComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "no", "DXT1", "DXT3", "DXT5" }));

        fileFormatComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "dds", "tga", "png", "jpeg" }));
        fileFormatComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fileFormatEvent(evt);
            }
        });

        progressBar.setStringPainted(true);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator1, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 376, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel3)
                                    .addComponent(jLabel2))
                                .addGap(22, 22, 22)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(compressionComboBox, 0, 82, Short.MAX_VALUE)
                                    .addComponent(fileFormatComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel5)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(mipmapsCheckBox))
                            .addComponent(overwriteCheckBox))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(progressBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(okButton, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton)))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelButton, okButton});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(fileFormatComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(compressionComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel5)
                    .addComponent(mipmapsCheckBox))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(overwriteCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cancelButton)
                    .addComponent(okButton))
                .addContainerGap())
        );

        getRootPane().setDefaultButton(okButton);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
		convert();
//		doClose(RET_OK);
    }//GEN-LAST:event_okButtonActionPerformed

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
		doClose(RET_CANCEL);
    }//GEN-LAST:event_cancelButtonActionPerformed

	/**
	 * Closes the dialog
	 */
    private void closeDialog(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_closeDialog
		doClose(RET_CANCEL);
    }//GEN-LAST:event_closeDialog

    private void fileFormatEvent(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fileFormatEvent
        //disable options when a file format other than DDS is selected
		boolean enabled = fileFormatComboBox.getSelectedIndex() == 0;
		compressionComboBox.setEnabled(enabled);
		mipmapsCheckBox.setEnabled(enabled);
    }//GEN-LAST:event_fileFormatEvent
	
	private void doClose(int retStatus) {
		returnStatus = retStatus;
		setVisible(false);
		dispose();
	}

	/**
	 * @param args the command line arguments
	 */
	public static void main(String args[]) {
		/* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
		 * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
		 */
		try {
			for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					javax.swing.UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}
		} catch (ClassNotFoundException ex) {
			java.util.logging.Logger.getLogger(ConvertTextureDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		} catch (InstantiationException ex) {
			java.util.logging.Logger.getLogger(ConvertTextureDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		} catch (IllegalAccessException ex) {
			java.util.logging.Logger.getLogger(ConvertTextureDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		} catch (javax.swing.UnsupportedLookAndFeelException ex) {
			java.util.logging.Logger.getLogger(ConvertTextureDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		}
        //</editor-fold>

		/* Create and display the dialog */
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				ConvertTextureDialog dialog = new ConvertTextureDialog(new javax.swing.JFrame(), true);
				dialog.addWindowListener(new java.awt.event.WindowAdapter() {
					@Override
					public void windowClosing(java.awt.event.WindowEvent e) {
						System.exit(0);
					}
				});
				dialog.setVisible(true);
			}
		});
	}

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelButton;
    private javax.swing.JComboBox compressionComboBox;
    private javax.swing.JComboBox fileFormatComboBox;
    private javax.swing.JList filesList;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JCheckBox mipmapsCheckBox;
    private javax.swing.JButton okButton;
    private javax.swing.JCheckBox overwriteCheckBox;
    private javax.swing.JProgressBar progressBar;
    // End of variables declaration//GEN-END:variables

	private int returnStatus = RET_CANCEL;
}

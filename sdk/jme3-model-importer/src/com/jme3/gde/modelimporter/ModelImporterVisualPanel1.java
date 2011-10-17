/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.modelimporter;

import com.jme3.asset.AssetEventListener;
import com.jme3.asset.AssetKey;
import com.jme3.gde.core.assets.AssetData;
import com.jme3.gde.core.assets.AssetDataObject;
import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.gde.core.scene.OffScenePanel;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JPanel;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.NotifyDescriptor.Message;
import org.openide.WizardDescriptor;
import org.openide.explorer.propertysheet.PropertySheet;
import org.openide.filesystems.FileChooserBuilder;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;

@SuppressWarnings({"unchecked", "serial"})
public final class ModelImporterVisualPanel1 extends JPanel implements AssetEventListener {

    private OffScenePanel offPanel;
    private String currentPath;
    private String currentModelPath;
    private Spatial currentModel;
    private List<AssetKey> requestedAssets = new LinkedList<AssetKey>();
    private AssetKey mainKey;
    private PropertySheet ps;
    private AtomicBoolean loading = new AtomicBoolean(false);

    /** Creates new form ModelImporterVisualPanel1 */
    public ModelImporterVisualPanel1() {
        initComponents();
        offPanel = new OffScenePanel(320, 320);
        offPanel.startPreview();
        jPanel1.add(offPanel);
        ps = new PropertySheet();
        ps.setNodes(new Node[]{});
        jPanel2.add(ps);
    }

    @Override
    public String getName() {
        return "Preview Model";
    }

    public void loadSettings(WizardDescriptor wiz) {
//        manager = (ProjectAssetManager) wiz.getProperty("manager");
    }

    public void applySettings(WizardDescriptor wiz) {
        wiz.putProperty("path", currentPath);
        wiz.putProperty("assetlist", requestedAssets);
        wiz.putProperty("mainkey", mainKey);
        if (mainKey != null) {
            wiz.putProperty("destpath", "Models/" + mainKey.getName().replaceAll(mainKey.getExtension(), "").replaceAll("\\.", "") + "/");
        }
    }

    public synchronized void loadModel(File path) {
        loadModel(path, null);
    }

    public synchronized void loadModel(File path, AssetKey modelKey) {
        ProjectAssetManager manager = new ProjectAssetManager(FileUtil.toFileObject(path).getParent());
        manager.setAssetEventListener(this);
        try {
            if (modelKey != mainKey) {
                requestedAssets.clear();
            }
            if (currentModel != null) {
                offPanel.detach(currentModel);
                currentModel = null;
            }
            currentModelPath = path.getPath();
            currentPath = path.getParent();
            DataObject obj = DataObject.find(FileUtil.toFileObject(path));
            AssetData data = obj != null ? obj.getLookup().lookup(AssetData.class) : null;
            if (data != null) {
                if (modelKey == null) {
                    ((AssetDataObject) obj).getLookupContents().add(manager);
                    modelKey = data.getAssetKey();
                    currentModel = (Spatial) data.loadAsset();
                } else {
                    ((AssetDataObject) obj).getLookupContents().add(manager);
                    data.setAssetKey(modelKey);
                    currentModel = (Spatial) data.loadAsset();
                }
            }
            mainKey = modelKey;
            updateProperties(mainKey);
            if (currentModel != null) {
                offPanel.attach(currentModel);
            } else {
                Message msg = new NotifyDescriptor.Message(
                        "Cannot import this file!",
                        NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notifyLater(msg);
            }
        } catch (Exception e) {
            Message msg = new NotifyDescriptor.Message(
                    "Error importing file!\n"
                    + "(" + e + ")",
                    NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notifyLater(msg);
            Exceptions.printStackTrace(e);
        }
        manager.setAssetEventListener(null);
        manager.clearCache();
    }

    private void updateProperties(final AssetKey key) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                if (key == null) {
                    ps.setNodes(new Node[]{});
                } else {
                    ps.setNodes(new Node[]{new ImportKeyNode(key)});
                }
            }
        });

    }

    public void assetRequested(AssetKey ak) {
        if (!"j3md".equalsIgnoreCase(ak.getExtension())
                && !"glsllib".equalsIgnoreCase(ak.getExtension())
                && !"frag".equalsIgnoreCase(ak.getExtension())
                && !"vert".equalsIgnoreCase(ak.getExtension())) {
            if (!requestedAssets.contains(ak)) {
                requestedAssets.add(ak);
            }
        }
    }

    public void assetLoaded(AssetKey ak) {
    }

    public void cleanup() {
        offPanel.stopPreview();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jTextField1 = new javax.swing.JTextField();
        jToolBar1 = new javax.swing.JToolBar();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        jButton3 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jButton6 = new javax.swing.JButton();
        jButton5 = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();

        jPanel1.setPreferredSize(new java.awt.Dimension(320, 320));
        jPanel1.setLayout(new javax.swing.BoxLayout(jPanel1, javax.swing.BoxLayout.LINE_AXIS));

        jPanel2.setLayout(new javax.swing.BoxLayout(jPanel2, javax.swing.BoxLayout.LINE_AXIS));

        jTextField1.setEditable(false);
        jTextField1.setText(org.openide.util.NbBundle.getMessage(ModelImporterVisualPanel1.class, "ModelImporterVisualPanel1.jTextField1.text")); // NOI18N

        jToolBar1.setFloatable(false);
        jToolBar1.setRollover(true);

        org.openide.awt.Mnemonics.setLocalizedText(jButton1, org.openide.util.NbBundle.getMessage(ModelImporterVisualPanel1.class, "ModelImporterVisualPanel1.jButton1.text")); // NOI18N
        jButton1.setFocusable(false);
        jButton1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton1.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jToolBar1.add(jButton1);

        org.openide.awt.Mnemonics.setLocalizedText(jButton2, org.openide.util.NbBundle.getMessage(ModelImporterVisualPanel1.class, "ModelImporterVisualPanel1.jButton2.text")); // NOI18N
        jButton2.setFocusable(false);
        jButton2.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton2.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });
        jToolBar1.add(jButton2);
        jToolBar1.add(jSeparator1);

        org.openide.awt.Mnemonics.setLocalizedText(jButton3, org.openide.util.NbBundle.getMessage(ModelImporterVisualPanel1.class, "ModelImporterVisualPanel1.jButton3.text")); // NOI18N
        jButton3.setFocusable(false);
        jButton3.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton3.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });
        jToolBar1.add(jButton3);

        org.openide.awt.Mnemonics.setLocalizedText(jButton4, org.openide.util.NbBundle.getMessage(ModelImporterVisualPanel1.class, "ModelImporterVisualPanel1.jButton4.text")); // NOI18N
        jButton4.setFocusable(false);
        jButton4.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton4.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });
        jToolBar1.add(jButton4);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 310, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 21, Short.MAX_VALUE)
        );

        jToolBar1.add(jPanel3);

        org.openide.awt.Mnemonics.setLocalizedText(jButton6, org.openide.util.NbBundle.getMessage(ModelImporterVisualPanel1.class, "ModelImporterVisualPanel1.jButton6.text")); // NOI18N
        jButton6.setFocusable(false);
        jButton6.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton6.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton6ActionPerformed(evt);
            }
        });
        jToolBar1.add(jButton6);

        org.openide.awt.Mnemonics.setLocalizedText(jButton5, org.openide.util.NbBundle.getMessage(ModelImporterVisualPanel1.class, "ModelImporterVisualPanel1.jButton5.text")); // NOI18N
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(ModelImporterVisualPanel1.class, "ModelImporterVisualPanel1.jLabel1.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jTextField1, javax.swing.GroupLayout.DEFAULT_SIZE, 424, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton5))
            .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 559, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 331, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, 228, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addComponent(jToolBar1, javax.swing.GroupLayout.DEFAULT_SIZE, 559, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, 340, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 340, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton5)))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        offPanel.zoomCamera(-.1f);
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        if (loading.get()) {
            return;
        }
        loading.set(true);
        FileChooserBuilder builder = new FileChooserBuilder(this.getClass());
        builder.setTitle("Select Model File");
        final File file = builder.showOpenDialog();
        if (file != null) {
            jTextField1.setText(file.getAbsolutePath());
            new Thread(new Runnable() {

                public void run() {
                    ProgressHandle handle = ProgressHandleFactory.createHandle("Opening Model..");
                    handle.start();
                    loadModel(file);
                    handle.finish();
                    loading.set(false);
                }
            }).start();
        }
    }//GEN-LAST:event_jButton5ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        offPanel.zoomCamera(.1f);
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        offPanel.rotateCamera(Vector3f.UNIT_Y, .1f);
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        offPanel.rotateCamera(Vector3f.UNIT_Y, -.1f);
    }//GEN-LAST:event_jButton4ActionPerformed

private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
    if (currentModelPath != null) {
        if (loading.get()) {
            return;
        }
        loading.set(true);
        final String modelPath = currentModelPath;
        final AssetKey key = mainKey;
        new Thread(new Runnable() {

            public void run() {
                ProgressHandle handle = ProgressHandleFactory.createHandle("Opening Model..");
                handle.start();
                loadModel(new File(modelPath), key);
                handle.finish();
                loading.set(false);
            }
        }).start();
    }
}//GEN-LAST:event_jButton6ActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JToolBar jToolBar1;
    // End of variables declaration//GEN-END:variables
}

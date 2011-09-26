/*
 * Copyright (c) 2009-2010 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.gde.terraineditor;

import com.jme3.gde.core.assets.AssetDataObject;
import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.gde.core.scene.PreviewRequest;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.gde.core.scene.SceneListener;
import com.jme3.gde.core.scene.SceneRequest;
import com.jme3.gde.core.sceneexplorer.nodes.JmeNode;
import com.jme3.gde.core.sceneexplorer.nodes.JmeSpatial;
import com.jme3.gde.core.sceneexplorer.nodes.JmeTerrainQuad;
import com.jme3.gde.core.sceneexplorer.nodes.NodeUtility;
import com.jme3.gde.core.properties.TexturePropertyEditor;
import com.jme3.gde.core.properties.preview.DDSPreview;
import com.jme3.gde.core.sceneviewer.SceneViewerTopComponent;
import com.jme3.gde.core.util.DataObjectSaveNode;
import com.jme3.gde.core.util.ToggleButtonGroup;
import com.jme3.gde.terraineditor.sky.SkyboxWizardAction;
import com.jme3.gde.terraineditor.tools.EraseTerrainTool;
import com.jme3.gde.terraineditor.tools.LevelTerrainTool;
import com.jme3.gde.terraineditor.tools.LowerTerrainTool;
import com.jme3.gde.terraineditor.tools.PaintTerrainTool;
import com.jme3.gde.terraineditor.tools.RaiseTerrainTool;
import com.jme3.gde.terraineditor.tools.SmoothTerrainTool;
import com.jme3.gde.terraineditor.tools.TerrainTool;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.terrain.ProgressMonitor;
import com.jme3.terrain.Terrain;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.texture.Texture;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.swing.AbstractCellEditor;
import javax.swing.DefaultListSelectionModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import jme3tools.converters.ImageToAwt;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.nodes.NodeEvent;
import org.openide.nodes.NodeMemberEvent;
import org.openide.nodes.NodeReorderEvent;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import org.openide.util.ImageUtilities;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.NotifyDescriptor.Confirmation;
import org.openide.WizardDescriptor;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.ExplorerUtils;
import org.openide.nodes.NodeListener;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup.Result;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(dtd = "-//com.jme3.gde.terraineditor//TerrainEditor//EN",
autostore = false)
public final class TerrainEditorTopComponent extends TopComponent implements SceneListener, LookupListener {

    private static TerrainEditorTopComponent instance;
    /** path to the icon used by the component and its open action */
    static final String ICON_PATH = "com/jme3/gde/terraineditor/TerraMonkey.png";
    private static final String PREFERRED_ID = "TerrainEditorTopComponent";
    private final Result<JmeSpatial> result;
    TerrainCameraController camController;
    TerrainToolController toolController;
    TerrainEditorController editorController;
    private SceneRequest currentRequest;
    private boolean alreadyChoosing = false; // used for texture table selection
    private CreateTerrainWizardAction terrainWizard;
    private SkyboxWizardAction skyboxWizard;
    private JmeSpatial selectedSpat;
    private TerrainNodeListener terrainDeletedNodeListener;
    private boolean availableNormalTextures;
    private HelpCtx ctx = new HelpCtx("sdk.terrain_editor");
    private DDSPreview ddsPreview;
    private Map<String, JButton> buttons = new HashMap<String, JButton>();

    public TerrainEditorTopComponent() {
        initComponents();
        setName(NbBundle.getMessage(TerrainEditorTopComponent.class, "CTL_TerrainEditorTopComponent"));
        setToolTipText(NbBundle.getMessage(TerrainEditorTopComponent.class, "HINT_TerrainEditorTopComponent"));
        associateLookup(ExplorerUtils.createLookup(new ExplorerManager(), getActionMap()));
        setIcon(ImageUtilities.loadImage(ICON_PATH, true));
        result = Utilities.actionsGlobalContext().lookupResult(JmeSpatial.class);
    }

    class EntropyCalcProgressMonitor implements ProgressMonitor {

        private ProgressHandle progressHandle;
        private float progress = 0;
        private float max = 0;
        private final Object lock = new Object();

        public void incrementProgress(float f) {
            progress += f;
            progressHandle.progress((int) progress);
        }

        public void setMonitorMax(float f) {
            max = f;
//            java.awt.EventQueue.invokeLater(new Runnable() {
//                public void run() {
//                    synchronized(lock){
            if (progressHandle == null) {
                progressHandle = ProgressHandleFactory.createHandle("Calculating terrain entropies...");
                progressHandle.start((int) max);
            }
//                    }
//                }
//            });
        }

        public float getMonitorMax() {
            return max;
        }

        public void progressComplete() {
//            SwingUtilities.invokeLater(new Runnable() {
//                public void run() {
            progressHandle.finish();
//                }
//            });
        }
    }

    private void setHintText(String text) {
        hintTextArea.setText(text);
    }

    private void setHintText(TerrainTool tool) {
        if (tool != null) {
            hintTextArea.setText(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, tool.getToolHintTextKey()));
        } else {
            hintTextArea.setText(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.toolHint.default"));
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        terrainModButtonGroup = new ToggleButtonGroup();
        textureFileChooser = new javax.swing.JFileChooser();
        jPanel1 = new javax.swing.JPanel();
        hintPanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        hintTextArea = new javax.swing.JTextArea();
        toolSettingsPanel = new javax.swing.JPanel();
        radiusLabel = new javax.swing.JLabel();
        radiusSlider = new javax.swing.JSlider();
        heightLabel = new javax.swing.JLabel();
        heightSlider = new javax.swing.JSlider();
        paintingPanel = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        textureTable = new javax.swing.JTable();
        remainingTexTitleLabel = new javax.swing.JLabel();
        remainingTexturesLabel = new javax.swing.JLabel();
        addTextureButton = new javax.swing.JButton();
        removeTextureButton = new javax.swing.JButton();
        triPlanarCheckBox = new javax.swing.JCheckBox();
        terrainOpsPanel = new javax.swing.JPanel();
        genEntropiesButton = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
        jToolBar1 = new javax.swing.JToolBar();
        createTerrainButton = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        raiseTerrainButton = new javax.swing.JToggleButton();
        lowerTerrainButton = new javax.swing.JToggleButton();
        smoothTerrainButton = new javax.swing.JToggleButton();
        roughTerrainButton = new javax.swing.JToggleButton();
        levelTerrainButton = new javax.swing.JToggleButton();
        jSeparator2 = new javax.swing.JToolBar.Separator();
        paintButton = new javax.swing.JToggleButton();
        eraseButton = new javax.swing.JToggleButton();

        textureFileChooser.setApproveButtonText(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.textureFileChooser.approveButtonText_1")); // NOI18N
        textureFileChooser.setCurrentDirectory(new java.io.File("/Assets/Textures"));
        textureFileChooser.setDialogTitle(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.textureFileChooser.dialogTitle_1")); // NOI18N
        textureFileChooser.setFileFilter(new ImageFilter());

        hintPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.hintPanel.border.title"))); // NOI18N

        hintTextArea.setColumns(20);
        hintTextArea.setEditable(false);
        hintTextArea.setLineWrap(true);
        hintTextArea.setRows(2);
        hintTextArea.setTabSize(4);
        hintTextArea.setWrapStyleWord(true);
        hintTextArea.setFocusable(false);
        hintTextArea.setRequestFocusEnabled(false);
        jScrollPane1.setViewportView(hintTextArea);

        javax.swing.GroupLayout hintPanelLayout = new javax.swing.GroupLayout(hintPanel);
        hintPanel.setLayout(hintPanelLayout);
        hintPanelLayout.setHorizontalGroup(
            hintPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 139, Short.MAX_VALUE)
        );
        hintPanelLayout.setVerticalGroup(
            hintPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 158, Short.MAX_VALUE)
        );

        toolSettingsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.toolSettingsPanel.border.title"))); // NOI18N
        toolSettingsPanel.setLayout(new javax.swing.BoxLayout(toolSettingsPanel, javax.swing.BoxLayout.PAGE_AXIS));

        org.openide.awt.Mnemonics.setLocalizedText(radiusLabel, org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.radiusLabel.text")); // NOI18N
        toolSettingsPanel.add(radiusLabel);

        radiusSlider.setMajorTickSpacing(5);
        radiusSlider.setMaximum(20);
        radiusSlider.setMinorTickSpacing(1);
        radiusSlider.setPaintTicks(true);
        radiusSlider.setSnapToTicks(true);
        radiusSlider.setToolTipText(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.radiusSlider.toolTipText")); // NOI18N
        radiusSlider.setValue(5);
        radiusSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                radiusSliderStateChanged(evt);
            }
        });
        toolSettingsPanel.add(radiusSlider);

        org.openide.awt.Mnemonics.setLocalizedText(heightLabel, org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.heightLabel.text")); // NOI18N
        toolSettingsPanel.add(heightLabel);

        heightSlider.setMajorTickSpacing(20);
        heightSlider.setMaximum(200);
        heightSlider.setPaintTicks(true);
        heightSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                heightSliderStateChanged(evt);
            }
        });
        toolSettingsPanel.add(heightSlider);

        paintingPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.paintingPanel.border.title"))); // NOI18N

        textureTable.setModel(new TextureTableModel());
        textureTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        textureTable.setCellEditor(new TextureCellRendererEditor());
        textureTable.setColumnSelectionAllowed(true);
        textureTable.setSelectionModel(new TableSelectionModel());
        textureTable.getTableHeader().setReorderingAllowed(false);
        jScrollPane2.setViewportView(textureTable);
        textureTable.getColumnModel().getSelectionModel().setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        org.openide.awt.Mnemonics.setLocalizedText(remainingTexTitleLabel, org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.remainingTexTitleLabel.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(remainingTexturesLabel, org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.remainingTexturesLabel.text")); // NOI18N

        addTextureButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/jme3/gde/terraineditor/icon_terrain-add-texture.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(addTextureButton, org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.addTextureButton.text")); // NOI18N
        addTextureButton.setToolTipText(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.addTextureButton.toolTipText")); // NOI18N
        addTextureButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addTextureButtonActionPerformed(evt);
            }
        });

        removeTextureButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/jme3/gde/terraineditor/icon_terrain-remove-texture.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(removeTextureButton, org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.removeTextureButton.text")); // NOI18N
        removeTextureButton.setToolTipText(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.removeTextureButton.toolTipText")); // NOI18N
        removeTextureButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeTextureButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(triPlanarCheckBox, org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.triPlanarCheckBox.text")); // NOI18N
        triPlanarCheckBox.setToolTipText(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.triPlanarCheckBox.toolTipText")); // NOI18N
        triPlanarCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                triPlanarCheckBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout paintingPanelLayout = new javax.swing.GroupLayout(paintingPanel);
        paintingPanel.setLayout(paintingPanelLayout);
        paintingPanelLayout.setHorizontalGroup(
            paintingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(paintingPanelLayout.createSequentialGroup()
                .addGroup(paintingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, paintingPanelLayout.createSequentialGroup()
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 291, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(paintingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(removeTextureButton, 0, 0, Short.MAX_VALUE)
                            .addComponent(addTextureButton, javax.swing.GroupLayout.PREFERRED_SIZE, 37, Short.MAX_VALUE)))
                    .addGroup(paintingPanelLayout.createSequentialGroup()
                        .addComponent(remainingTexTitleLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(remainingTexturesLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 19, Short.MAX_VALUE)
                        .addComponent(triPlanarCheckBox)))
                .addContainerGap())
        );
        paintingPanelLayout.setVerticalGroup(
            paintingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(paintingPanelLayout.createSequentialGroup()
                .addGroup(paintingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(paintingPanelLayout.createSequentialGroup()
                        .addComponent(addTextureButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(removeTextureButton))
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 131, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(paintingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(remainingTexTitleLabel)
                    .addComponent(remainingTexturesLabel)
                    .addComponent(triPlanarCheckBox)))
        );

        terrainOpsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.terrainOpsPanel.border.title"))); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(genEntropiesButton, org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.genEntropiesButton.text")); // NOI18N
        genEntropiesButton.setToolTipText(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.genEntropiesButton.toolTipText")); // NOI18N
        genEntropiesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                genEntropiesButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jButton1, org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.jButton1.text")); // NOI18N
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout terrainOpsPanelLayout = new javax.swing.GroupLayout(terrainOpsPanel);
        terrainOpsPanel.setLayout(terrainOpsPanelLayout);
        terrainOpsPanelLayout.setHorizontalGroup(
            terrainOpsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(terrainOpsPanelLayout.createSequentialGroup()
                .addGroup(terrainOpsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(genEntropiesButton)
                    .addComponent(jButton1))
                .addContainerGap(31, Short.MAX_VALUE))
        );
        terrainOpsPanelLayout.setVerticalGroup(
            terrainOpsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(terrainOpsPanelLayout.createSequentialGroup()
                .addComponent(genEntropiesButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton1)
                .addContainerGap(96, Short.MAX_VALUE))
        );

        jToolBar1.setFloatable(false);
        jToolBar1.setRollover(true);

        createTerrainButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/jme3/gde/terraineditor/icon_terrain-new.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(createTerrainButton, org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.createTerrainButton.text")); // NOI18N
        createTerrainButton.setToolTipText(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.createTerrainButton.toolTipText")); // NOI18N
        createTerrainButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                createTerrainButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(createTerrainButton);
        jToolBar1.add(jSeparator1);

        terrainModButtonGroup.add(raiseTerrainButton);
        raiseTerrainButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/jme3/gde/terraineditor/icon_terrain-up.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(raiseTerrainButton, org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.raiseTerrainButton.text")); // NOI18N
        raiseTerrainButton.setToolTipText(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.raiseTerrainButton.toolTipText")); // NOI18N
        raiseTerrainButton.setActionCommand(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.raiseTerrainButton.actionCommand")); // NOI18N
        raiseTerrainButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                raiseTerrainButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(raiseTerrainButton);

        terrainModButtonGroup.add(lowerTerrainButton);
        lowerTerrainButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/jme3/gde/terraineditor/icon_terrain-down.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(lowerTerrainButton, org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.lowerTerrainButton.text")); // NOI18N
        lowerTerrainButton.setToolTipText(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.lowerTerrainButton.toolTipText")); // NOI18N
        lowerTerrainButton.setActionCommand(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.lowerTerrainButton.actionCommand")); // NOI18N
        lowerTerrainButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lowerTerrainButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(lowerTerrainButton);

        terrainModButtonGroup.add(smoothTerrainButton);
        smoothTerrainButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/jme3/gde/terraineditor/icon_terrain-smooth.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(smoothTerrainButton, org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.smoothTerrainButton.text")); // NOI18N
        smoothTerrainButton.setToolTipText(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.smoothTerrainButton.toolTipText")); // NOI18N
        smoothTerrainButton.setActionCommand(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.smoothTerrainButton.actionCommand")); // NOI18N
        smoothTerrainButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                smoothTerrainButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(smoothTerrainButton);

        terrainModButtonGroup.add(roughTerrainButton);
        roughTerrainButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/jme3/gde/terraineditor/icon_terrain-rough.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(roughTerrainButton, org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.roughTerrainButton.text")); // NOI18N
        roughTerrainButton.setToolTipText(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.roughTerrainButton.toolTipText")); // NOI18N
        roughTerrainButton.setActionCommand(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.roughTerrainButton.actionCommand")); // NOI18N
        roughTerrainButton.setEnabled(false);
        jToolBar1.add(roughTerrainButton);

        terrainModButtonGroup.add(levelTerrainButton);
        levelTerrainButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/jme3/gde/terraineditor/icon_terrain-level.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(levelTerrainButton, org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.levelTerrainButton.text")); // NOI18N
        levelTerrainButton.setToolTipText(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.levelTerrainButton.toolTipText")); // NOI18N
        levelTerrainButton.setFocusable(false);
        levelTerrainButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        levelTerrainButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        levelTerrainButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                levelTerrainButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(levelTerrainButton);
        jToolBar1.add(jSeparator2);

        terrainModButtonGroup.add(paintButton);
        paintButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/jme3/gde/terraineditor/icon_terrain-paint-circle.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(paintButton, org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.paintButton.text")); // NOI18N
        paintButton.setToolTipText(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.paintButton.toolTipText")); // NOI18N
        paintButton.setFocusable(false);
        paintButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        paintButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        paintButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                paintButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(paintButton);

        terrainModButtonGroup.add(eraseButton);
        eraseButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/jme3/gde/terraineditor/icon_terrain-erase-circle.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(eraseButton, org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.eraseButton.text")); // NOI18N
        eraseButton.setToolTipText(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.eraseButton.toolTipText")); // NOI18N
        eraseButton.setFocusable(false);
        eraseButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        eraseButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        eraseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                eraseButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(eraseButton);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(toolSettingsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(paintingPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(terrainOpsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(hintPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addComponent(jToolBar1, javax.swing.GroupLayout.DEFAULT_SIZE, 891, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(toolSettingsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 186, Short.MAX_VALUE)
                    .addComponent(paintingPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(hintPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(terrainOpsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void createTerrainButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_createTerrainButtonActionPerformed
        addSpatial("Terrain");
    }//GEN-LAST:event_createTerrainButtonActionPerformed

    private void raiseTerrainButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_raiseTerrainButtonActionPerformed

        //toolController.setShowGrid(true);

        if (raiseTerrainButton.isSelected()) {
            RaiseTerrainTool tool = new RaiseTerrainTool();
            toolController.setTerrainEditButtonState(tool);
            setHintText(tool);
        } else {
            toolController.setTerrainEditButtonState(null);
            setHintText((TerrainTool) null);
        }
    }//GEN-LAST:event_raiseTerrainButtonActionPerformed

    private void lowerTerrainButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lowerTerrainButtonActionPerformed
        if (lowerTerrainButton.isSelected()) {
            LowerTerrainTool tool = new LowerTerrainTool();
            toolController.setTerrainEditButtonState(tool);
            setHintText(tool);
        } else {
            toolController.setTerrainEditButtonState(null);
            setHintText((TerrainTool) null);
        }
    }//GEN-LAST:event_lowerTerrainButtonActionPerformed

    private void genEntropiesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_genEntropiesButtonActionPerformed
        if (editorController != null) {
            setHintText("Run entropy generation when you are finished modifying the terrain's height. It is a slow process but required for some LOD operations.");
            EntropyCalcProgressMonitor monitor = new EntropyCalcProgressMonitor();
            editorController.generateEntropies(monitor);
        }
    }//GEN-LAST:event_genEntropiesButtonActionPerformed

    private void paintButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_paintButtonActionPerformed
        if (paintButton.isSelected()) {
            PaintTerrainTool tool = new PaintTerrainTool();
            toolController.setTerrainEditButtonState(tool);
            setHintText(tool);
        } else {
            toolController.setTerrainEditButtonState(null);
            setHintText((TerrainTool) null);
        }
    }//GEN-LAST:event_paintButtonActionPerformed

    private void addTextureButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addTextureButtonActionPerformed
        if (editorController == null || editorController.getTerrain(null) == null) {
            return;
        }
        int index = getTableModel().getRowCount(); // get the last row
        addNewTextureLayer(index);
        //  editorController.enableTextureButtons();
    }//GEN-LAST:event_addTextureButtonActionPerformed

    protected void enableAddTextureButton(boolean enabled) {
        addTextureButton.setEnabled(enabled);
    }

    protected void enableRemoveTextureButton(boolean enabled) {
        removeTextureButton.setEnabled(enabled);
    }

    protected void updateTextureCountLabel(int count) {
        remainingTexturesLabel.setText("" + count);
    }

    protected void setAddNormalTextureEnabled(boolean enabled) {
        availableNormalTextures = enabled;
    }

    private void removeTextureButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeTextureButtonActionPerformed
        if (editorController == null || editorController.getTerrain(null) == null) {
            return;
        }
        if (getTableModel().getRowCount() == 0) {
            return;
        }
        int index = getTableModel().getRowCount() - 1; // get the last row
        removeTextureLayer(index);
        buttons.remove(index + "-" + 1);
        buttons.remove(index + "-" + 2);
        editorController.enableTextureButtons();
    }//GEN-LAST:event_removeTextureButtonActionPerformed

    private void eraseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_eraseButtonActionPerformed
        if (eraseButton.isSelected()) {
            EraseTerrainTool tool = new EraseTerrainTool();
            toolController.setTerrainEditButtonState(tool);
            setHintText(tool);
        } else {
            toolController.setTerrainEditButtonState(null);
            setHintText((TerrainTool) null);
        }
    }//GEN-LAST:event_eraseButtonActionPerformed

    private void triPlanarCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_triPlanarCheckBoxActionPerformed
        editorController.setTriPlanarEnabled(triPlanarCheckBox.isSelected());
        ((TextureTableModel) textureTable.getModel()).updateScales();
        if (triPlanarCheckBox.isSelected()) {
            setHintText("Make sure your scale is a power of 2, (1/2^n), when in tri-planar mode");
        }
    }//GEN-LAST:event_triPlanarCheckBoxActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        addSpatial("Skybox");
    }//GEN-LAST:event_jButton1ActionPerformed

    private void levelTerrainButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_levelTerrainButtonActionPerformed
        if (levelTerrainButton.isSelected()) {
            LevelTerrainTool tool = new LevelTerrainTool();
            toolController.setTerrainEditButtonState(tool);
            setHintText(tool);
        } else {
            toolController.setTerrainEditButtonState(null);
            setHintText((TerrainTool) null);
        }
    }//GEN-LAST:event_levelTerrainButtonActionPerformed

    private void radiusSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_radiusSliderStateChanged
        if (toolController != null) {
            toolController.setHeightToolRadius((float) radiusSlider.getValue() / (float) radiusSlider.getMaximum());
        }
    }//GEN-LAST:event_radiusSliderStateChanged

    private void heightSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_heightSliderStateChanged
        if (toolController != null) {
            toolController.setHeightToolHeight((float) heightSlider.getValue() / (float) heightSlider.getMaximum());
        }
    }//GEN-LAST:event_heightSliderStateChanged

    private void smoothTerrainButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_smoothTerrainButtonActionPerformed
        if (smoothTerrainButton.isSelected()) {
            SmoothTerrainTool tool = new SmoothTerrainTool();
            toolController.setTerrainEditButtonState(tool);
            setHintText(tool);
        } else {
            toolController.setTerrainEditButtonState(null);
            setHintText((TerrainTool) null);
        }
    }//GEN-LAST:event_smoothTerrainButtonActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addTextureButton;
    private javax.swing.JButton createTerrainButton;
    private javax.swing.JToggleButton eraseButton;
    private javax.swing.JButton genEntropiesButton;
    private javax.swing.JLabel heightLabel;
    private javax.swing.JSlider heightSlider;
    private javax.swing.JPanel hintPanel;
    private javax.swing.JTextArea hintTextArea;
    private javax.swing.JButton jButton1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JToolBar.Separator jSeparator2;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JToggleButton levelTerrainButton;
    private javax.swing.JToggleButton lowerTerrainButton;
    private javax.swing.JToggleButton paintButton;
    private javax.swing.JPanel paintingPanel;
    private javax.swing.JLabel radiusLabel;
    private javax.swing.JSlider radiusSlider;
    private javax.swing.JToggleButton raiseTerrainButton;
    private javax.swing.JLabel remainingTexTitleLabel;
    private javax.swing.JLabel remainingTexturesLabel;
    private javax.swing.JButton removeTextureButton;
    private javax.swing.JToggleButton roughTerrainButton;
    private javax.swing.JToggleButton smoothTerrainButton;
    private javax.swing.ButtonGroup terrainModButtonGroup;
    private javax.swing.JPanel terrainOpsPanel;
    private javax.swing.JFileChooser textureFileChooser;
    private javax.swing.JTable textureTable;
    private javax.swing.JPanel toolSettingsPanel;
    private javax.swing.JCheckBox triPlanarCheckBox;
    // End of variables declaration//GEN-END:variables

    /**
     * Gets default instance. Do not use directly: reserved for *.settings files only,
     * i.e. deserialization routines; otherwise you could get a non-deserialized instance.
     * To obtain the singleton instance, use {@link #findInstance}.
     */
    public static synchronized TerrainEditorTopComponent getDefault() {
        if (instance == null) {
            instance = new TerrainEditorTopComponent();
        }
        return instance;
    }

    public void addSpatial(final String name) {
        if (selectedSpat == null) {

            Confirmation msg = new NotifyDescriptor.Confirmation(
                    "You must select a Node to add the " + name + " to in the Scene Explorer window",
                    NotifyDescriptor.OK_CANCEL_OPTION,
                    NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(msg);
            return;
        }

        final Spatial node = selectedSpat.getLookup().lookup(Spatial.class);
        if (node != null) {
            if ("Terrain".equals(name)) {
                if (terrainWizard == null) {
                    terrainWizard = new CreateTerrainWizardAction(this);
                }
                terrainWizard.performAction();
            } else if ("Skybox".equals(name)) {
                if (skyboxWizard == null) {
                    skyboxWizard = new SkyboxWizardAction(this);
                }
                skyboxWizard.performAction();
            } else if ("Ocean".equals(name)) {
            }
        }

    }

    protected void generateTerrain(final WizardDescriptor wizardDescriptor) {
        final Spatial node = selectedSpat.getLookup().lookup(Spatial.class);

        int totalSize = (Integer) wizardDescriptor.getProperty("totalSize");
        int patchSize = (Integer) wizardDescriptor.getProperty("patchSize");
        int alphaTextureSize = (Integer) wizardDescriptor.getProperty("alphaTextureSize");

        float[] heightmapData = null;
        AbstractHeightMap heightmap = (AbstractHeightMap) wizardDescriptor.getProperty("abstractHeightMap");
        if (heightmap != null) {
            heightmap.load(); // can take a while
            heightmapData = heightmap.getHeightMap();
        }

        // eg. Scenes/newScene1.j3o
        String[] split1 = currentRequest.getWindowTitle().split("/");
        String[] split2 = split1[split1.length - 1].split("\\.");

        Terrain terrain = null;
        try {
            terrain = editorController.createTerrain((Node) node,
                    totalSize,
                    patchSize,
                    alphaTextureSize,
                    heightmapData,
                    split2[0],
                    selectedSpat);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

        addSaveNode(selectedSpat);

        editorController.setNeedsSave(true);

        editorController.enableTextureButtons();

        reinitTextureTable(); // update the UI

        refreshSelected();

        //createTerrainButton.setEnabled(false); // only let the user add one terrain

    }

    public void generateSkybox(WizardDescriptor wiz) {

        Spatial sky = null;
        final Spatial node = selectedSpat.getLookup().lookup(Spatial.class);

        if ((Boolean) wiz.getProperty("multipleTextures")) {
            Texture south = (Texture) wiz.getProperty("textureSouth");
            Texture north = (Texture) wiz.getProperty("textureNorth");
            Texture east = (Texture) wiz.getProperty("textureEast");
            Texture west = (Texture) wiz.getProperty("textureWest");
            Texture top = (Texture) wiz.getProperty("textureTop");
            Texture bottom = (Texture) wiz.getProperty("textureBottom");
            Vector3f normalScale = (Vector3f) wiz.getProperty("normalScale");
            sky = editorController.createSky((Node) node, west, east, north, south, top, bottom, normalScale);
        } else {
            Texture textureSingle = (Texture) wiz.getProperty("textureSingle");
            Vector3f normalScale = (Vector3f) wiz.getProperty("normalScale");
            boolean useSpheremap = (Boolean) wiz.getProperty("useSpheremap");
            sky = editorController.createSky((Node) node, textureSingle, useSpheremap, normalScale);
        }

        editorController.setNeedsSave(true);
        refreshSelected();
    }

    private void refreshSelected() {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                if (selectedSpat != null) {
                    selectedSpat.refresh(false);
                }
            }
        });

    }

    /**
     * listener for node selection changes
     */
    public void resultChanged(LookupEvent ev) {
        if (currentRequest == null || !currentRequest.isDisplayed()) {
            return;
        }
        Collection<JmeSpatial> items = (Collection<JmeSpatial>) result.allInstances();
        for (JmeSpatial spatial : items) {
            selectSpatial(spatial);
            return;
        }
    }

    private void selectSpatial(JmeSpatial spatial) {
        selectedSpat = spatial;
        editorController.setSelectedSpat(spatial);
        /*if (selectedSpat instanceof JmeTerrainQuad) { //TODO shouldn't be terrainQuad, should be a generic JmeTerrain
            selectedSpat.removeNodeListener(terrainDeletedNodeListener); // remove it if it exists, no way to check if it is there already
            selectedSpat.addNodeListener(terrainDeletedNodeListener); // add it back
        }*/
    }

    /**
     * When the terrain is deleted, enable the 'add terrain' button again
     * and reinitialize the texture table
     */
    private class TerrainNodeListener implements NodeListener {

        public void childrenAdded(NodeMemberEvent nme) {
        }

        public void childrenRemoved(NodeMemberEvent nme) {
        }

        public void childrenReordered(NodeReorderEvent nre) {
        }

        public void nodeDestroyed(NodeEvent ne) {
            createTerrainButton.setEnabled(true);
            reinitTextureTable();
        }

        public void propertyChange(PropertyChangeEvent evt) {
        }
    }

    /*
     *
     *******************************************************************/
    /**
     * Obtain the TerrainEditorTopComponent instance. Never call {@link #getDefault} directly!
     */
    public static synchronized TerrainEditorTopComponent findInstance() {
        TopComponent win = WindowManager.getDefault().findTopComponent(PREFERRED_ID);
        if (win == null) {
            Logger.getLogger(TerrainEditorTopComponent.class.getName()).warning(
                    "Cannot find " + PREFERRED_ID + " component. It will not be located properly in the window system.");
            return getDefault();
        }
        if (win instanceof TerrainEditorTopComponent) {
            return (TerrainEditorTopComponent) win;
        }
        Logger.getLogger(TerrainEditorTopComponent.class.getName()).warning(
                "There seem to be multiple components with the '" + PREFERRED_ID
                + "' ID. That is a potential source of errors and unexpected behavior.");
        return getDefault();
    }

    @Override
    public int getPersistenceType() {
        return TopComponent.PERSISTENCE_ALWAYS;
    }

    @Override
    public void componentOpened() {
        super.componentOpened();
        if (currentRequest == null) {
            close();
        }
    }

    @Override
    public void componentClosed() {
        super.componentClosed();
        if (currentRequest != null) {
            SceneApplication.getApplication().closeScene(currentRequest);
        }

    }

    @Override
    protected void componentActivated() {
        SceneViewerTopComponent.findInstance().requestVisible();
    }
    
    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        // TODO store your settings
    }

    Object readProperties(java.util.Properties p) {
        if (instance == null) {
            instance = this;
        }
        instance.readPropertiesImpl(p);
        return instance;
    }

    private void readPropertiesImpl(java.util.Properties p) {
        String version = p.getProperty("version");
        // TODO read your settings according to their version
    }

    @Override
    protected String preferredID() {
        return PREFERRED_ID;
    }

    @Override
    public HelpCtx getHelpCtx() {
        return ctx;
    }

    public void openScene(Spatial spat, AssetDataObject file, ProjectAssetManager manager) {
        cleanupControllers();
        SceneApplication.getApplication().addSceneListener(this);
        result.addLookupListener(this);
        //TODO: handle request change
        Node node;
        if (spat instanceof Node) {
            node = (Node) spat;
        } else {
            node = new Node();
            node.attachChild(spat);
        }
        JmeNode jmeNode = NodeUtility.createNode(node, file, false);
        SceneRequest request = new SceneRequest(this, jmeNode, manager);
        request.setDataObject(file);
        request.setHelpCtx(ctx);

        addSaveNode(jmeNode);

        //SceneUndoRedoManager m = Lookup.getDefault().lookup(SceneUndoRedoManager.class);//TODO remove this line

        Logger.getLogger(TerrainEditorTopComponent.class.getName()).finer("Terrain openScene " + file.getName());

        if (editorController != null) {
            editorController.cleanup();
        }
        editorController = new TerrainEditorController(jmeNode, file, this);
        this.currentRequest = request;
        request.setWindowTitle("TerrainEditor - " + manager.getRelativeAssetPath(file.getPrimaryFile().getPath()));
        request.setToolNode(new Node("TerrainEditorToolNode"));
        SceneApplication.getApplication().requestScene(request);

        terrainDeletedNodeListener = new TerrainNodeListener();
        editorController.enableTextureButtons();

    }

    // run on GL thread
    public void sceneRequested(SceneRequest request) {

        if (request.equals(currentRequest)) {
            Logger.getLogger(TerrainEditorTopComponent.class.getName()).finer("Terrain sceneRequested " + request.getWindowTitle());

            setSceneInfo(currentRequest.getJmeNode(), true);

            //editorController.doGetAlphaSaveDataObject(this);

            // if the opened scene has terrain, add it to a save node
            Terrain terrain = (Terrain) editorController.getTerrain(null);
            if (terrain != null) {
                // add the terrain root save node

                // ugh! wtf, why is this fixing the material problem?
                ((Node) terrain).setMaterial(terrain.getMaterial());
                // it appears when loading the actual applied material on the terrain
                // does not reflect the material that we get from the terrain.

                refreshSelected();
            }

            if (camController != null) {
                camController.disable();
            }
            if (toolController != null) {
                toolController.cleanup();
            }

            //for (int i=0; i<textureTable.getModel().getRowCount(); i++)
            //    ((TextureTableModel)textureTable.getModel()).removeRow(i);

            toolController = new TerrainToolController(currentRequest.getToolNode(), currentRequest.getManager().getManager(), request.getJmeNode());
            camController = new TerrainCameraController(SceneApplication.getApplication().getCamera());
            camController.setMaster(this);
            camController.enable();

            camController.setToolController(toolController);
            camController.setEditorController(editorController);
            toolController.setEditorController(editorController);
            toolController.setCameraController(camController);
            editorController.setToolController(toolController);

            toolController.setHeightToolRadius((float) radiusSlider.getValue() / (float) radiusSlider.getMaximum());
            toolController.setHeightToolHeight((float) heightSlider.getValue() / (float) heightSlider.getMaximum());

            editorController.setTerrainLodCamera();
            
            java.awt.EventQueue.invokeLater(new Runnable() {

                public void run() {
                    reinitTextureTable(); // update the UI
                    if (editorController.getTerrain(null) != null) {
                        //createTerrainButton.setEnabled(false); // only let the user add one terrain
                    }
                }
            });
        }
    }

    protected synchronized void addDataObject(final DataObjectSaveNode dataObject) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                addSaveNode(dataObject);
            }
        });
    }

    protected void addSaveNode(org.openide.nodes.Node node) {
        setActivatedNodes(new org.openide.nodes.Node[]{node});
    }

    private void setSceneInfo(final JmeNode jmeNode, final boolean active) {
        final TerrainEditorTopComponent inst = this;
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                if (jmeNode != null) {
                } else {
                }

                if (!active) {
                    result.removeLookupListener(inst);
                    close();
                } else {
                    open();
                    requestActive();
                }
            }
        });
    }

    public boolean sceneClose(SceneRequest request) {
        if (request.equals(currentRequest)) {
//            if (checkSaved()) {
            SceneApplication.getApplication().removeSceneListener(this);
            setSceneInfo(null, false);
            currentRequest = null;
            java.awt.EventQueue.invokeLater(new Runnable() {

                public void run() {
                    cleanupControllers();
                }
            });
//            }
        }
        return true;
    }

    public void previewRequested(PreviewRequest request) {
    }

    private void cleanupControllers() {
        if (camController != null) {
            camController.disable();
            camController = null;
        }
        if (toolController != null) {
            toolController.cleanup();
            toolController = null;
        }
        if (editorController != null) {
            editorController.cleanup();
            editorController = null;
        }
        setActivatedNodes(new org.openide.nodes.Node[]{});
    }

    /**
     * re-initialize the texture rows in the texture table to match the given terrain.
     */
    protected void reinitTextureTable() {

        clearTextureTable();

        getTableModel().initModel();

        if (textureTable.getRowCount() > 0) {
            toolController.setSelectedTextureIndex(0); // select the first row by default
        } else {
            toolController.setSelectedTextureIndex(-1);
        }

        editorController.enableTextureButtons();
        triPlanarCheckBox.setSelected(editorController.isTriPlanarEnabled());
    }

    protected void clearTextureTable() {
        TextureCellRendererEditor rendererTexturer = new TextureCellRendererEditor();
        textureTable.getColumnModel().getColumn(1).setCellRenderer(rendererTexturer); // diffuse
        textureTable.getColumnModel().getColumn(1).setCellEditor(rendererTexturer);

        NormalCellRendererEditor rendererNormal = new NormalCellRendererEditor();
        textureTable.getColumnModel().getColumn(2).setCellRenderer(rendererNormal); // normal
        textureTable.getColumnModel().getColumn(2).setCellEditor(rendererNormal);

        // empty out the table
        while (textureTable.getModel().getRowCount() > 0)
             ((TextureTableModel) textureTable.getModel()).removeRow(0);

        if (editorController.getTerrain(null) == null) {
            return;
        }
    }
    
    /**
     * Adds another texture layer to the material, sets a default texture for it.
     * Assumes that the new index is in the range of the amount of available textures
     * the material can support.
     * Assumes this is the last index, or else messy stuff in the material will happen.
     * @param the index it is being placed at.
     */
    private void addNewTextureLayer(int newIndex) {
        getTableModel().addNewTexture(newIndex);
    }

    /**
     * Removes the selected index.
     * If the index is -1, it just returns.
     * Assumes this is the last index, or else messy stuff in the material will happen.
     * @param selectedIndex
     */
    private void removeTextureLayer(int selectedIndex) {
        if (selectedIndex < 0) {
            return; // abort
        }
        getTableModel().removeTexture(selectedIndex);
    }

    private TextureTableModel getTableModel() {
        if (textureTable == null) {
            return null;
        }
        return (TextureTableModel) textureTable.getModel();
    }

    /**
     * Holds the table information and relays changes to that data to the actual
     * terrain material. Info such as textures and texture scales.
     */
    public class TextureTableModel extends DefaultTableModel {
        //private Material terrainMaterial;

        public TextureTableModel() {
            super(new String[]{"", "Texture", "Normal", "Scale"}, 0);
        }

        public void initModel() {

            // empty the table
            while (getRowCount() > 0) {
                removeRow(0);
            }

            // fill the table with the proper data
            for (int i = 0; i < editorController.MAX_TEXTURES; i++) {
                if (!editorController.hasTextureAt(i)) {
                    continue;
                }

                Float scale = editorController.getTextureScale(i);
                if (scale == null) {
                    scale = editorController.DEFAULT_TEXTURE_SCALE;
                }
                addRow(new Object[]{"", i, i, scale});
            }
        }

        protected void updateScales() {
            for (int i = 0; i < editorController.getNumUsedTextures(); i++) {
                float scale = editorController.getTextureScale(i);
                setValueAt("" + scale, i, 3); // don't call this one's setValueAt, it will re-set the scales
            }
        }

        // it seems to keep the selection when we delete the row
        @Override
        public void setValueAt(Object aValue, int row, int column) {
            if (row < 0 || row > getRowCount() - 1) {
                return;
            }
            super.setValueAt(aValue, row, column);

            if (column == 3) {
                setTextureScale(row, new Float((String) aValue));
            }
        }

        protected void addNewTexture(int newIndex) {
            float scale = editorController.DEFAULT_TEXTURE_SCALE;

            // add it to the table model
            addRow(new Object[]{"", newIndex, null, scale}); // add to the table model

            // and add it to the actual material
            setTexture(newIndex, (String) null);
            setTextureScale(newIndex, scale);
            editorController.enableTextureButtons();
        }

        protected void setTexture(final int index, final Texture texture) {
            setValueAt(index, index, 1);
            editorController.setDiffuseTexture(index, texture);
        }

        protected void setTexture(final int index, final String texturePath) {
            setValueAt(index, index, 1);
            editorController.setDiffuseTexture(index, texturePath);
        }

        protected void setNormal(final int index, final String texturePath) {
            setValueAt(index, index, 2);
            editorController.setNormalMap(index, texturePath);
            editorController.enableTextureButtons();
        }

        protected void setNormal(final int index, final Texture texture) {
            setValueAt(index, index, 2);
            editorController.setNormalMap(index, texture);
            editorController.enableTextureButtons();
        }

        protected void setTextureScale(int index, float scale) {
            editorController.setTextureScale(index, scale);
        }

        protected void removeTexture(final int index) {
            removeRow(index);
            editorController.removeTextureLayer(index);
            editorController.enableTextureButtons();
        }
    }

    /**
     * signals to the tool controller when a different row is selected
     */
    public class TableSelectionModel extends DefaultListSelectionModel {

        public TableSelectionModel() {
            super.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            addListSelectionListener(new ListSelectionListener() {

                public void valueChanged(ListSelectionEvent e) {
                    if (toolController != null) {
                        toolController.setSelectedTextureIndex(textureTable.getSelectedRow());
                    }
                }
            });
        }
    }

    /**
     * The renderer and editor for the Diffuse and Normal texture buttons in the texture table.
     * Delegates texture changes and queries to the table model, which then delegates to
     * the TerrainEditorController.
     */
    public abstract class CellRendererEditor extends AbstractCellEditor implements TableCellEditor, TableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return getButton(value, row, column);

        }

        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            return getButton(value, row, column);
        }

        protected abstract void setTextureInModel(int row, String path);

        protected abstract void setTextureInModel(int row, Texture tex);

        protected abstract Texture getTextureFromModel(int index);

        protected abstract boolean supportsNullTexture();

        private JButton getButton(Object value, final int row, final int column) {

            JButton button = buttons.get(row + "-" + column);
            if (button == null) {
                final JButton lbl = new JButton();
                buttons.put(row + "-" + column, lbl);

                //TODO check if there is a normal or a texture here at this index
                if (value == null) {
                    value = getTableModel().getValueAt(row, column);
                }

                if (value != null) {
                    int index = 0;
                    // this is messy, fix it so we know what values are coming in from where:
                    if (value instanceof String) {
                        index = new Float((String) value).intValue();
                    } else if (value instanceof Float) {
                        index = ((Float) value).intValue();
                    } else if (value instanceof Integer) {
                        index = (Integer) value;
                    }

                    Texture tex = getTextureFromModel(index); // delegate to sub-class

                    //Texture tex = SceneApplication.getApplication().getAssetManager().loadTexture((String)value);
                    if (tex != null) {
                        String selected = tex.getKey().getName();

                        if (selected.toLowerCase().endsWith(".dds")) {
                            if (ddsPreview == null) {
                                ddsPreview = new DDSPreview((ProjectAssetManager) SceneApplication.getApplication().getAssetManager());
                            }
                            ddsPreview.requestPreview(selected, "", 80, 80, lbl, null);

                        } else {
                            Icon icon = ImageUtilities.image2Icon(ImageToAwt.convert(tex.getImage(), false, true, 0));
                            lbl.setIcon(icon);
                        }
                    }

                }

                lbl.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {

                        if (alreadyChoosing) {
                            return;
                        }

                        alreadyChoosing = true;

                        try {
                            Texture selectedTex = getTextureFromModel(row); // delegates to sub class
                            if (selectedTex == null && !availableNormalTextures) // bail if we are at our texture limit
                            {
                                return;
                            }
                            TexturePropertyEditor editor = new TexturePropertyEditor(selectedTex);
                            Component view = editor.getCustomEditor();
                            view.setVisible(true);
                            Texture tex = (Texture) editor.getValue();
                            if (editor.getValue() != null) {
                                String selected = tex.getKey().getName();

                                if (selected.toLowerCase().endsWith(".dds")) {
                                    if (ddsPreview == null) {
                                        ddsPreview = new DDSPreview((ProjectAssetManager) SceneApplication.getApplication().getAssetManager());
                                    }
                                    ddsPreview.requestPreview(selected, "", 80, 80, lbl, null);

                                } else {
                                    Icon newicon = ImageUtilities.image2Icon(ImageToAwt.convert(tex.getImage(), false, true, 0));
                                    lbl.setIcon(newicon);
                                }
                            } else if (supportsNullTexture()) {
                                lbl.setIcon(null);
                            }
                            setTextureInModel(row, tex);
                        } finally {
                            alreadyChoosing = false;
                        }
                    }
                });

                return lbl;
            }
            return button;
        }
    }

    public class TextureCellRendererEditor extends CellRendererEditor {

        @Override
        public Object getCellEditorValue() {
            int row = textureTable.getSelectedRow();
            if (row < 0) {
                return null;
            }
            return getTableModel().getValueAt(row, 1);
        }

        @Override
        protected void setTextureInModel(int row, String path) {
            if (path != null) {
                getTableModel().setTexture(row, path);
            }
        }

        @Override
        protected void setTextureInModel(int row, Texture tex) {
            if (tex != null) {
                getTableModel().setTexture(row, tex);
            }
        }

        @Override
        protected Texture getTextureFromModel(int index) {
            return editorController.getDiffuseTexture(index);
        }

        @Override
        protected boolean supportsNullTexture() {
            return false;
        }
    }

    public class NormalCellRendererEditor extends CellRendererEditor {

        @Override
        public Object getCellEditorValue() {
            int row = textureTable.getSelectedRow();
            if (row < 0) {
                return null;
            }
            return getTableModel().getValueAt(row, 2);
        }

        @Override
        protected void setTextureInModel(int row, String path) {
            getTableModel().setNormal(row, path);
        }

        @Override
        protected void setTextureInModel(int row, Texture tex) {
            getTableModel().setNormal(row, tex);
        }

        @Override
        protected Texture getTextureFromModel(int index) {
            return editorController.getNormalMap(index);
        }

        @Override
        protected boolean supportsNullTexture() {
            return true;
        }
    }

    /**
     * A file filter to only show images
     */
    public class ImageFilter extends FileFilter {

        Utils utils = new Utils();
        //Accept all directories and all gif, jpg, tiff, or png files.

        public boolean accept(File f) {
            if (f.isDirectory()) {
                return true;
            }

            String extension = utils.getExtension(f);
            if (extension != null) {
                if (extension.equals(utils.tiff)
                        || extension.equals(utils.tif)
                        || extension.equals(utils.gif)
                        || extension.equals(utils.jpeg)
                        || extension.equals(utils.jpg)
                        || extension.equals(utils.png)
                        || extension.equals(utils.dds)) {
                    return true;
                } else {
                    return false;
                }
            }

            return false;
        }

        //The description of this filter
        public String getDescription() {
            return "Just Images";
        }
    }

    /**
     * restricts the file chooser to a specified directory tree, such as the assets folder
     */
    class DirectoryRestrictedFileSystemView extends FileSystemView {

        private final File[] rootDirectories;

        DirectoryRestrictedFileSystemView(File rootDirectory) {
            this.rootDirectories = new File[]{rootDirectory};
        }

        DirectoryRestrictedFileSystemView(File[] rootDirectories) {
            this.rootDirectories = rootDirectories;
        }

        @Override
        public Boolean isTraversable(File f) {
            if (f.getAbsolutePath().indexOf(rootDirectories[0].getAbsolutePath()) >= 0) {
                return Boolean.valueOf(f.isDirectory());
            } else {
                return false;
            }
        }

        @Override
        public File getDefaultDirectory() {
            return rootDirectories[0];
        }

        @Override
        public File getHomeDirectory() {
            return rootDirectories[0];
        }

        @Override
        public File[] getRoots() {
            return rootDirectories;
        }

        @Override
        public File createNewFolder(File containingDir) throws IOException {
            throw new UnsupportedOperationException("Unable to create directory");
        }
        /*
         @Override
         public File[] getRoots()
         {
         return rootDirectories;
         }

         @Override
         public boolean isRoot(File file)
         {
         for (File root : rootDirectories) {
         if (root.equals(file)) {
         return true;
         }
         }
         return false;
         }*/
    }

    public class Utils {

        public final String jpeg = "jpeg";
        public final String jpg = "jpg";
        public final String gif = "gif";
        public final String tiff = "tiff";
        public final String tif = "tif";
        public final String png = "png";
        public final String dds = "dds";

        /*
         * Get the extension of a file.
         */
        public String getExtension(File f) {
            String ext = null;
            String s = f.getName();
            int i = s.lastIndexOf('.');

            if (i > 0 && i < s.length() - 1) {
                ext = s.substring(i + 1).toLowerCase();
            }
            return ext;
        }

        /** Returns an ImageIcon, or null if the path was invalid. */
        protected ImageIcon createImageIcon(String path) {
            java.net.URL imgURL = Utils.class.getResource(path);
            if (imgURL != null) {
                return new ImageIcon(imgURL);
            } else {
                //System.err.println("Couldn't find file: " + path);
                return null;
            }
        }
    }
}

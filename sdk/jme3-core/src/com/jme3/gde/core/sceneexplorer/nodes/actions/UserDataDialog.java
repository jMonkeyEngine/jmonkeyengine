/*
 *  Copyright (c) 2009-2010 jMonkeyEngine
 *  All rights reserved.
 * 
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are
 *  met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 
 *  * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *  TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 *  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/*
 * UserDataDialog.java
 *
 * Created on 29.01.2011, 18:35:53
 */
package com.jme3.gde.core.sceneexplorer.nodes.actions;

import com.jme3.export.Savable;
import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.gde.core.properties.SceneExplorerProperty;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.gde.core.sceneexplorer.nodes.JmeSpatial;
import com.jme3.gde.core.util.PropertyUtils;
import com.jme3.scene.Spatial;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Label;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.explorer.propertysheet.PropertyPanel;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node.Property;
import org.openide.util.Exceptions;

/**
 *
 * @author normenhansen
 */
public class UserDataDialog extends javax.swing.JDialog {

    JmeSpatial spat;
    boolean initialized = false;
    Object userData;

    /** Creates new form UserDataDialog */
    public UserDataDialog(java.awt.Frame parent, boolean modal, JmeSpatial spat, String userDataName) {
        super(parent, modal);
        this.spat = spat;

        setLocationRelativeTo(null);
        initComponents();
        jButton3.setVisible(false);
        if (userDataName != null) {
            jButton3.setVisible(true);
            jTextField2.setEditable(false);
            jComboBox1.setEnabled(false);

            userData = spat.getLookup().lookup(Spatial.class).getUserData(userDataName);
            jTextField2.setText(userDataName);
            jTextField1.setText(userData.toString());
            jPanel1.setVisible(false);
            if (userData instanceof String) {
                jComboBox1.setSelectedItem("String");
            } else if (userData instanceof Integer) {
                jComboBox1.setSelectedItem("Int");
            } else if (userData instanceof Float) {
                jComboBox1.setSelectedItem("Float");
            } else if (userData instanceof Boolean) {
                jComboBox1.setSelectedItem("Boolean");
            } else if (userData instanceof Long) {
                jComboBox1.setSelectedItem("Long");
            } else {
                jComboBox1.setSelectedItem("Custom");

                buildCustomSheet(userData);
            }
        }
    }

    private void buildCustomSheet(Object userData) {
        jPanel1.setVisible(true);
        jTextField1.setEditable(false);
        jTextField1.setText(userData.getClass().getName());
        Class c = userData.getClass();
        if (!initialized) {
            initialized = true;


            GridLayout layout = new GridLayout(0, 2);
            layout.setHgap(10);
            jPanel1.setLayout(layout);
            jPanel1.setPreferredSize(new Dimension(300, c.getDeclaredFields().length * 20));


            for (Field field : c.getDeclaredFields()) {
                PropertyDescriptor prop = PropertyUtils.getPropertyDescriptor(c, field);
                if (prop != null) {
                    PropertyPanel p = new PropertyPanel(makeProperty(userData, prop.getPropertyType(), prop.getReadMethod().getName(), prop.getWriteMethod().getName(), prop.getDisplayName()));
                    jPanel1.add(new Label(field.getName() + " :", Label.RIGHT));
                    jPanel1.add(p);
                }
            }
        }
        setSize(getWidth(), getHeight() + c.getDeclaredFields().length * 20);

    }

    protected Property makeProperty(Object obj, Class returntype, String method, String setter, String name) {
        Property prop = null;
        try {

            prop = new SceneExplorerProperty(obj.getClass().cast(obj), returntype, method, setter);

            prop.setName(name);

        } catch (NoSuchMethodException ex) {
            Exceptions.printStackTrace(ex);
        }
        return prop;
    }

    private void updateSpatial() {
        final String format = (String) jComboBox1.getSelectedItem();
        final String data = jTextField1.getText();
        final String name = jTextField2.getText();
        if (name.trim().length() == 0) {
            return;
        }
        final Spatial spatial = spat.getLookup().lookup(Spatial.class);
        try {
            SceneApplication.getApplication().enqueue(new Callable<Void>() {

                public Void call() throws Exception {

                    if (format.equals("String")) {
                        spatial.setUserData(name, data);
                    } else if (format.equals("Int")) {
                        spatial.setUserData(name, Integer.parseInt(data));
                    } else if (format.equals("Float")) {
                        spatial.setUserData(name, Float.parseFloat(data));
                    } else if (format.equals("Boolean")) {
                        spatial.setUserData(name, Boolean.parseBoolean(data));
                    } else if (format.equals("Long")) {
                        spatial.setUserData(name, Long.parseLong(data));
                    } else if (userData != null) {
                        spatial.setUserData(name, userData);
                    }
                    return null;
                }
            }).get();
        } catch (InterruptedException ex) {
        } catch (ExecutionException ex) {
        }
        spat.refreshProperties();
        spat.getLookup().lookup(DataObject.class).setModified(true);
        setVisible(false);
    }

    public void setUserDataType(String className) {
        ProjectAssetManager manager = (ProjectAssetManager) spat.getLookup().lookup(ProjectAssetManager.class);
        List<ClassLoader> loaders = manager.getClassLoaders();


        Class clazz = null;
        try {
            clazz = getClass().getClassLoader().loadClass(className);
        } catch (ClassNotFoundException ex) {
        }
        for (ClassLoader classLoader : loaders) {
            if (clazz == null) {
                try {
                    clazz = classLoader.loadClass(className);
                } catch (ClassNotFoundException ex) {
                }
            }
        }
        if (clazz != null) {
            try {
                Object obj = clazz.newInstance();
                if (obj instanceof Savable) {
                    userData = obj;
                    buildCustomSheet(userData);
                } else {
                    DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message("This is no ParticleInfluencer class!"));
                }
            } catch (InstantiationException ex) {
                Exceptions.printStackTrace(ex);
                DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message("Error instatiating class!"));
            } catch (IllegalAccessException ex) {
                Exceptions.printStackTrace(ex);
                DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message("Error instatiating class!"));
            }
        } else {
            DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message("Cannot find class: " + className + "\nMake sure the name is correct and the project is compiled,\nbest enable 'Save on Compile' in the project preferences."));
        }

    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jComboBox1 = new javax.swing.JComboBox();
        jTextField1 = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jTextField2 = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jButton3 = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "String", "Int", "Float", "Boolean", "Long", "Custom" }));
        jComboBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox1ActionPerformed(evt);
            }
        });

        jTextField1.setText(org.openide.util.NbBundle.getMessage(UserDataDialog.class, "UserDataDialog.jTextField1.text")); // NOI18N
        jTextField1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField1ActionPerformed(evt);
            }
        });

        jButton1.setText(org.openide.util.NbBundle.getMessage(UserDataDialog.class, "UserDataDialog.jButton1.text")); // NOI18N
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setText(org.openide.util.NbBundle.getMessage(UserDataDialog.class, "UserDataDialog.jButton2.text")); // NOI18N
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jLabel1.setText(org.openide.util.NbBundle.getMessage(UserDataDialog.class, "UserDataDialog.jLabel1.text")); // NOI18N

        jTextField2.setText(org.openide.util.NbBundle.getMessage(UserDataDialog.class, "UserDataDialog.jTextField2.text")); // NOI18N

        jLabel2.setText(org.openide.util.NbBundle.getMessage(UserDataDialog.class, "UserDataDialog.jLabel2.text")); // NOI18N

        jButton3.setText(org.openide.util.NbBundle.getMessage(UserDataDialog.class, "UserDataDialog.jButton3.text")); // NOI18N
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jPanel1.setAutoscrolls(true);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 322, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextField2, javax.swing.GroupLayout.DEFAULT_SIZE, 196, Short.MAX_VALUE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel2)
                                .addGap(18, 18, 18)
                                .addComponent(jTextField1, javax.swing.GroupLayout.DEFAULT_SIZE, 186, Short.MAX_VALUE)))
                        .addGap(18, 18, 18)
                        .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(20, 20, 20))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jButton3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 123, Short.MAX_VALUE)
                        .addComponent(jButton2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton1)
                        .addGap(20, 20, 20))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1)
                    .addComponent(jButton2)
                    .addComponent(jButton3))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jTextField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField1ActionPerformed
        updateSpatial();
    }//GEN-LAST:event_jTextField1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        setVisible(false);
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        updateSpatial();
    }//GEN-LAST:event_jButton1ActionPerformed

private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
    spat.getLookup().lookup(Spatial.class).setUserData(jTextField2.getText(), null);
    spat.refreshProperties();
    spat.getLookup().lookup(DataObject.class).setModified(true);
    setVisible(false);
}//GEN-LAST:event_jButton3ActionPerformed

private void jComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox1ActionPerformed
    if (jComboBox1.getSelectedItem().equals("Custom")) {
        if (userData != null) {
            buildCustomSheet(userData);

        } else {
            UserDataPicker picker = new UserDataPicker(null, true, this, spat);
            picker.setVisible(true);
        }
    } else {
        jPanel1.setVisible(false);
        jTextField1.setEditable(true);
        setSize(getWidth(), getHeight() - jPanel1.getHeight());

    }
}//GEN-LAST:event_jComboBox1ActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JComboBox jComboBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    // End of variables declaration//GEN-END:variables

}

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
 * UserDataPicker.java
 *
 * Created on 20 août 2012, 22:09:04
 */
package com.jme3.gde.core.sceneexplorer.nodes.actions;

import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.gde.core.properties.ParticleInfluencerPropertyEditor;
import com.jme3.gde.core.sceneexplorer.nodes.JmeParticleEmitter;
import java.awt.EventQueue;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.swing.DefaultListModel;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.java.source.ClassIndex;
import org.netbeans.api.java.source.ClassIndex.NameKind;
import org.netbeans.api.java.source.ClassIndex.SearchScope;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.openide.util.Exceptions;

/**
 *
 * @author Nehon
 */
public class ParticleInfluencerPicker extends javax.swing.JDialog {

    JmeParticleEmitter jmePE;
    ParticleInfluencerPropertyEditor editor;
    ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(1);

    /**
     * Creates new form UserDataPicker
     */
    public ParticleInfluencerPicker(java.awt.Frame parent, boolean modal, ParticleInfluencerPropertyEditor editor, JmeParticleEmitter spat) {
        super(parent, modal);
        this.jmePE = spat;
        initComponents();
        this.editor = editor;

        setLocationRelativeTo(null);
      
        jList1.setEnabled(false);
        //loading savable list in a new Thread
        exec.execute(new Runnable() {
            public void run() {
                final DefaultListModel model = getSources();

                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        jList1.setModel(model);
                        jList1.setEnabled(true);
                    }
                });

            }
        });


    }

    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);
        exec.shutdown();
    }
        

    private DefaultListModel getSources() {
        final DefaultListModel model = new DefaultListModel();
        model.addElement("com.jme3.effect.influencers.DefaultParticleInfluencer");
        model.addElement("com.jme3.effect.influencers.NewtonianParticleInfluencer");
        model.addElement("com.jme3.effect.influencers.RadialParticleInfluencer");
        model.addElement("com.jme3.effect.influencers.EmptyParticleInfluencer");
        Sources sources = jmePE.getLookup().lookup(ProjectAssetManager.class).getProject().getLookup().lookup(Sources.class);
        if (sources != null) {
            SourceGroup[] groups = sources.getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA);
            if (groups != null) {
                for (SourceGroup sourceGroup : groups) {
                    ClasspathInfo cpInfo = ClasspathInfo.create(ClassPath.getClassPath(sourceGroup.getRootFolder(), ClassPath.BOOT),
                            ClassPath.getClassPath(sourceGroup.getRootFolder(), ClassPath.COMPILE),
                            ClassPath.getClassPath(sourceGroup.getRootFolder(), ClassPath.SOURCE));

                    HashSet<SearchScope> set = new HashSet<SearchScope>();
                    set.add(ClassIndex.SearchScope.SOURCE);

                    Set<ElementHandle<TypeElement>> types = cpInfo.getClassIndex().getDeclaredTypes("", NameKind.PREFIX, set);
                    for (Iterator<ElementHandle<TypeElement>> it = types.iterator(); it.hasNext();) {
                        final ElementHandle<TypeElement> elementHandle = it.next();
                        JavaSource js = JavaSource.create(cpInfo);
                        try {
                            js.runUserActionTask(new Task<CompilationController>() {
                                public void run(CompilationController control)
                                        throws Exception {
                                    control.toPhase(Phase.RESOLVED);
                                    //TODO: check with proper casting check.. gotta get TypeMirror of Control interface..
//                                    TypeUtilities util = control.getTypeUtilities();//.isCastable(Types., null)
//                                    util.isCastable(null, null);
                                    TypeElement elem = elementHandle.resolve(control);
                                    List<? extends TypeMirror> interfaces = elem.getInterfaces();
                                    for (TypeMirror typeMirror : interfaces) {
                                        String interfaceName = typeMirror.toString();
                                        if ("com.jme3.effect.influencers.ParticleInfluencer".equals(interfaceName)) {
                                            model.addElement(elem.getQualifiedName().toString());
                                        }
                                    }
                                    TypeMirror superClass = elem.getSuperclass();
                                    String superClassName = superClass.toString();
                                    if ("com.jme3.effect.influencers.DefaultParticleInfluencer".equals(superClassName)) {
                                        model.addElement(elem.getQualifiedName().toString());
                                    }

                                }
                            }, false);
                        } catch (Exception ioe) {
                            Exceptions.printStackTrace(ioe);
                        }
                    }

                }
            }
        }
        return model;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jList1 = new javax.swing.JList();
        jTextField1 = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(ParticleInfluencerPicker.class, "ParticleInfluencerPicker.jPanel1.border.title"))); // NOI18N

        jList1.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Loading Savable types..." };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        jList1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jList1MouseClicked(evt);
            }
        });
        jList1.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                jList1ValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(jList1);

        jTextField1.setText(org.openide.util.NbBundle.getMessage(ParticleInfluencerPicker.class, "ParticleInfluencerPicker.jTextField1.text")); // NOI18N

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jTextField1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 323, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 323, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 204, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jButton1.setText(org.openide.util.NbBundle.getMessage(ParticleInfluencerPicker.class, "ParticleInfluencerPicker.jButton1.text")); // NOI18N
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setText(org.openide.util.NbBundle.getMessage(ParticleInfluencerPicker.class, "ParticleInfluencerPicker.jButton2.text")); // NOI18N
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jButton2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 225, Short.MAX_VALUE)
                .addComponent(jButton1)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1)
                    .addComponent(jButton2))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
    setVisible(false);
}//GEN-LAST:event_jButton2ActionPerformed

private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed


    editor.setAsText(jTextField1.getText());
    setVisible(false);

}//GEN-LAST:event_jButton1ActionPerformed

private void jList1ValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_jList1ValueChanged
    jTextField1.setText(jList1.getSelectedValue().toString());
}//GEN-LAST:event_jList1ValueChanged

private void jList1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jList1MouseClicked
    if (evt.getClickCount() == 2) {
        jButton1ActionPerformed(null);
    }
}//GEN-LAST:event_jList1MouseClicked
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JList jList1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextField jTextField1;
    // End of variables declaration//GEN-END:variables
}

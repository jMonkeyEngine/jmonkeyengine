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
package com.jme3.gde.core.appstates;

import com.jme3.export.binary.BinaryExporter;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.gde.core.scene.SceneRequest;
import com.jme3.scene.Spatial;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import javax.swing.filechooser.FileFilter;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.StatusDisplayer;
import org.openide.cookies.SaveCookie;
import org.openide.filesystems.FileChooserBuilder;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;

/**
 * Node for a not yet existing scene, can be saved as j3o
 * @author normenhansen
 */
public class NewSceneSaveNode extends AbstractNode implements SaveCookie {

    private final SceneRequest request;

    public NewSceneSaveNode(SceneRequest nodeToSave) {
        super(Children.LEAF);
        request = nodeToSave;
        getCookieSet().assign(SaveCookie.class, this);

    }

    @Override
    public String toString() {
        return "NewSceneSaveNode(" + getDisplayName() + ")";
    }

    public void save() throws IOException {
        FileChooserBuilder builder = new FileChooserBuilder(NewSceneSaveNode.class);
        builder.addFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                if (f.getName().toLowerCase().trim().endsWith(".j3o")) {
                    return true;
                }
                return false;
            }

            @Override
            public String getDescription() {
                return "j3o Files";
            }
        });

        final File file = builder.showSaveDialog();
        if (file == null) {
            return;
        }
        SceneApplication.getApplication().enqueue(new Callable<Void>() {
            public Void call() throws Exception {
                Spatial node = request.getRootNode();
                if (node == null) {
                    return null;
                }
                if (file.exists()) {
                    NotifyDescriptor.Confirmation mesg = new NotifyDescriptor.Confirmation("File exists, overwrite?",
                            "Not Saved",
                            NotifyDescriptor.YES_NO_OPTION, NotifyDescriptor.WARNING_MESSAGE);
                    DialogDisplayer.getDefault().notify(mesg);
                    if (mesg.getValue() != NotifyDescriptor.Confirmation.YES_OPTION) {
                        return null;
                    }
                }
                ProgressHandle progressHandle = ProgressHandleFactory.createHandle("Saving File..");
                progressHandle.start();
                try {
                    BinaryExporter exp = BinaryExporter.getInstance();
                    exp.save(node, file);
                } catch (Exception e) {
                } finally {
                    FileObject fo = FileUtil.toFileObject(file);
                    if (fo != null) {
                        StatusDisplayer.getDefault().setStatusText(fo + " saved.");
                        fo.getParent().refresh();
                    }
                }
                progressHandle.finish();
                return null;
            }
        });

    }
}

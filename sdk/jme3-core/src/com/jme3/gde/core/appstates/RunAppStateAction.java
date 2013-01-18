/*
 * Copyright (c) 2003-2012 jMonkeyEngine
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
package com.jme3.gde.core.appstates;

import com.jme3.app.state.AppState;
import com.jme3.gde.core.scene.FakeApplication;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.gde.core.scene.SceneRequest;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.swing.Action;
import org.netbeans.api.java.source.CancellableTask;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.SourceUtils;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.util.ContextAwareAction;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;

@ActionID(
    category = "jMonkeyEngine",
id = "com.jme3.gde.core.appstates.RunAppStateAction")
@ActionRegistration(
displayName = "#CTL_RunAppState")
@ActionReferences({
    @ActionReference(path = "Toolbars/Build", position = 325),
    @ActionReference(path = "Loaders/text/x-java/Actions", position = 1050),
    @ActionReference(path = "Editors/text/x-java/Popup", position = 1740)
})
@Messages("CTL_RunAppState=Run AppState")
public class RunAppStateAction implements ContextAwareAction {

    private static final Logger logger = Logger.getLogger(RunAppStateAction.class.getName());
    private String className;
    private JavaSource source;

    public RunAppStateAction() {
        className = null;
        source = null;
    }

    public RunAppStateAction(String className, JavaSource src) {
        this.className = className;
        this.source = src;
    }

    public Action createContextAwareInstance(Lookup actionContext) {
        checkData(actionContext, "com.jme3.app.state.AppState");
        return new RunAppStateAction(className, source);
    }

    public Object getValue(String key) {
        if (Action.NAME.equals(key)) {
            return "Run AppState";
        }
        return null;
    }

    public void putValue(String key, Object value) {
    }

    public void setEnabled(boolean b) {
    }

    public boolean isEnabled() {
        return className != null;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
    }

    public void actionPerformed(ActionEvent e) {
        //TODO: better way to access scene request        
        SceneRequest req = SceneApplication.getApplication().getCurrentSceneRequest();
        if (req != null) {
            FakeApplication app = req.getFakeApp();
            try {
                AppState state = (AppState) app.getClassByName(className).newInstance();
                app.getStateManager().attach(state);
                AppStateExplorerTopComponent.openExplorer();
            } catch (InstantiationException ex) {
                DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message("Error creating AppState, make sure it has an empty constructor!\n" + ex.getMessage()));
                Exceptions.printStackTrace(ex);
            } catch (IllegalAccessException ex) {
                DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message("Error creating AppState, make sure it has an empty constructor!\n" + ex.getMessage()));
                Exceptions.printStackTrace(ex);
            } catch (Exception ex) {
                DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message("Error creating AppState, exception when creating!\n" + ex.getMessage()));
                Exceptions.printStackTrace(ex);
            }
        } else {
            DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message("No Scene opened to attach to,\nopen a j3o scene."));
            logger.log(Level.INFO, "No Scene Request available");
        }
    }
    
    private void checkData(Lookup actionContext, final String name) {
        source = null;
        className = null;
        try {
            DataObject dObj = actionContext.lookup(DataObject.class);
            if (dObj == null) {
                logger.log(Level.FINE, "No DataObject");
                return;
            }
            FileObject fObj = dObj.getPrimaryFile();
            if (fObj == null) {
                logger.log(Level.FINE, "No FileObject");
                return;
            }
            final JavaSource src = JavaSource.forFileObject(fObj);
            if (src == null) {
                logger.log(Level.FINE, "No JavaSource");
                return;
            }
            CancellableTask task = new CancellableTask<CompilationController>() {
                public void run(CompilationController controller) throws IOException {
                    controller.toPhase(JavaSource.Phase.PARSED);
                    CompilationUnitTree cut = controller.getCompilationUnit();
                    TypeElement appStateElement = controller.getElements().getTypeElement(name);
                    if (appStateElement == null) {
                        logger.log(Level.FINE, "No {0} found in classpath", name);
                        return;
                    }
                    TypeMirror appState = appStateElement.asType();
                    if (appState == null) {
                        logger.log(Level.FINE, "No TypeMirror for {0}", appStateElement);
                        return;
                    }
                    for (Tree typeDecl : cut.getTypeDecls()) {
                        if (Tree.Kind.CLASS == typeDecl.getKind()) {
                            ClassTree clazz = (ClassTree) typeDecl;
                            String elementName = cut.getPackageName().toString() + "." + clazz.getSimpleName();
                            TypeElement myElement = controller.getElements().getTypeElement(elementName);
                            if (myElement != null) {
                                TypeMirror elementType = myElement.asType();
                                logger.log(Level.FINE, "Check {0} against {1}", new Object[]{elementType, appState});
                                if (elementType != null && SourceUtils.checkTypesAssignable(controller, elementType, appState)) {
                                    source = src;
                                    className = elementName;
                                }
                            }
                        }
                    }
                }

                public void cancel() {
                }
            };
            src.runUserActionTask(task, true);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
}
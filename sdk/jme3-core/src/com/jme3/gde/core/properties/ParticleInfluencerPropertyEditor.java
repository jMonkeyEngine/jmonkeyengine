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
package com.jme3.gde.core.properties;

import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.influencers.ParticleInfluencer;
import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.gde.core.sceneexplorer.nodes.JmeParticleEmitter;
import com.jme3.gde.core.sceneexplorer.nodes.actions.ParticleInfluencerPicker;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyEditor;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.netbeans.api.project.Project;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Exceptions;

/**
 *
 * @author normenhansen
 */
public class ParticleInfluencerPropertyEditor implements PropertyEditor {

    private LinkedList<PropertyChangeListener> listeners = new LinkedList<PropertyChangeListener>();
    private JmeParticleEmitter jmePe;
    private ParticleInfluencer pi;
    private Project proj;

    public ParticleInfluencerPropertyEditor() {
    }

    public ParticleInfluencerPropertyEditor(JmeParticleEmitter jmePe, Project project) {
        this.jmePe = jmePe;
        this.pi = jmePe.getLookup().lookup(ParticleEmitter.class).getParticleInfluencer();
        this.proj = project;
    }

    public void setValue(Object value) {
        if (value instanceof ParticleInfluencer) {
            pi = (ParticleInfluencer) value;
        }
    }

    public Object getValue() {
        return pi;
    }

    public boolean isPaintable() {
        return false;
    }

    public void paintValue(Graphics gfx, Rectangle box) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getJavaInitializationString() {
        return null;
    }

    public String getAsText() {
        return pi.getClass().getSimpleName();
    }

    public void setAsText(String text) throws IllegalArgumentException {
        if(pi.getClass().getName().equals(text)){
            return;
        }
        ParticleInfluencer old = pi;
        ProjectAssetManager manager = (ProjectAssetManager) proj.getLookup().lookup(ProjectAssetManager.class);
        List<ClassLoader> loaders = manager.getClassLoaders();


        Class clazz = null;
        try {
            clazz = getClass().getClassLoader().loadClass(text);
        } catch (ClassNotFoundException ex) {
        }
        for (ClassLoader classLoader : loaders) {
            if (clazz == null) {
                try {
                    clazz = classLoader.loadClass(text);
                } catch (ClassNotFoundException ex) {
                }
            }
        }
        if (clazz != null) {
            try {
                Object obj = clazz.newInstance();
                if (obj instanceof ParticleInfluencer) {
                    pi = (ParticleInfluencer) obj;
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
            DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message("Cannot find class: " + text + "\nMake sure the name is correct and the project is compiled,\nbest enable 'Save on Compile' in the project preferences."));
        }
        if (pi != old) {
            notifyListeners(old, pi);
        }
    }

    public String[] getTags() {
        return null;
//        List<String> s = getSources();
//        s.add("com.jme3.effect.influencers.DefaultParticleInfluencer");
//        s.add("com.jme3.effect.influencers.NewtonianParticleInfluencer");
//        s.add("com.jme3.effect.influencers.RadialParticleInfluencer");
//        s.add("com.jme3.effect.influencers.EmptyParticleInfluencer");
//        String[] t = new String[s.size()];
//        return s.toArray(t);

    }

//    private List<String> getSources() {
//        Sources sources = proj.getLookup().lookup(Sources.class);
//        final List<String> list = new LinkedList<String>();
//        if (sources != null) {
//            SourceGroup[] groups = sources.getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA);
//            if (groups != null) {
//                for (SourceGroup sourceGroup : groups) {
//                    ClasspathInfo cpInfo = ClasspathInfo.create(ClassPath.getClassPath(sourceGroup.getRootFolder(), ClassPath.BOOT),
//                            ClassPath.getClassPath(sourceGroup.getRootFolder(), ClassPath.COMPILE),
//                            ClassPath.getClassPath(sourceGroup.getRootFolder(), ClassPath.SOURCE));
//
//                    HashSet<SearchScope> set = new HashSet<SearchScope>();
//                    set.add(ClassIndex.SearchScope.SOURCE);
//                    //   set.add(ClassIndex.SearchScope.DEPENDENCIES);
//
//                    Set<ElementHandle<TypeElement>> types = cpInfo.getClassIndex().getDeclaredTypes("", NameKind.PREFIX, set);
//                    for (Iterator<ElementHandle<TypeElement>> it = types.iterator(); it.hasNext();) {
//                        final ElementHandle<TypeElement> elementHandle = it.next();
//                        JavaSource js = JavaSource.create(cpInfo);
//                        try {
//                            js.runUserActionTask(new Task<CompilationController>() {
//
//                                public void run(CompilationController control)
//                                        throws Exception {
//                                    control.toPhase(Phase.RESOLVED);
//                                    //TODO: check with proper casting check.. gotta get TypeMirror of Control interface..
////                                    TypeUtilities util = control.getTypeUtilities();//.isCastable(Types., null)
////                                    util.isCastable(null, null);
//                                    TypeElement elem = elementHandle.resolve(control);
//                                    List<? extends TypeMirror> interfaces = elem.getInterfaces();
//                                    for (TypeMirror typeMirror : interfaces) {
//                                        String interfaceName = typeMirror.toString();
//                                        if ("com.jme3.effect.influencers.ParticleInfluencer".equals(interfaceName)) {
//                                            list.add(elem.getQualifiedName().toString());
//                                        }
//                                    }
//                                    TypeMirror superClass = elem.getSuperclass();
//                                    String superClassName = superClass.toString();
//                                    if ("com.jme3.effect.influencers.DefaultParticleInfluencer".equals(superClassName)) {
//                                        list.add(elem.getQualifiedName().toString());
//                                    }
//                                }
//                            }, false);
//                        } catch (Exception ioe) {
//                            Exceptions.printStackTrace(ioe);
//                        }
//                    }
//
//                }
//            }
//        }
//        return list;
//    }
    public Component getCustomEditor() {
        return new ParticleInfluencerPicker(null, true, this, jmePe);
    }

    public boolean supportsCustomEditor() {
        return true;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        listeners.add(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(ParticleInfluencer before, ParticleInfluencer after) {
        for (Iterator<PropertyChangeListener> it = listeners.iterator(); it.hasNext();) {
            PropertyChangeListener propertyChangeListener = it.next();
            //TODO: check what the "programmatic name" is supposed to be here.. for now its Quaternion
            propertyChangeListener.propertyChange(new PropertyChangeEvent(this, null, before, after));
        }
    }
}

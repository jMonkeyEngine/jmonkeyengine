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
package com.jme3.gde.core.sceneexplorer.nodes;

import com.jme3.animation.AnimControl;
import com.jme3.animation.Animation;
import com.jme3.gde.core.scene.SceneApplication;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import org.openide.loaders.DataObject;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;

/**
 *
 * @author nehon
 */
public class JmeAnimChildren extends Children.Keys<Object> {

    protected JmeAnimControl jmeAnimControl;
    protected boolean readOnly = true;
    protected HashMap<Object, Node> map = new HashMap<Object, Node>();
    private DataObject dataObject;

    public JmeAnimChildren() {
    }

    public JmeAnimChildren(JmeAnimControl jmeAnimControl) {
        this.jmeAnimControl = jmeAnimControl;
    }

    public void refreshChildren(boolean immediate) {
        setKeys(createKeys());
        refresh();
    }

    public void setReadOnly(boolean cookie) {
        this.readOnly = cookie;
    }

    @Override
    protected void addNotify() {
        super.addNotify();
        setKeys(createKeys());
    }

    protected List<Object> createKeys() {
        try {
            return SceneApplication.getApplication().enqueue(new Callable<List<Object>>() {

                public List<Object> call() throws Exception {
                    List<Object> keys = new LinkedList<Object>();
                    AnimControl control = jmeAnimControl.getLookup().lookup(AnimControl.class);
                    if (control != null) {
                        for (String animName : control.getAnimationNames()) {
                            keys.add(control.getAnim(animName));
                        }
                    }

                    return keys;
                }
            }).get();
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ExecutionException ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }

    @Override
    protected Node[] createNodes(Object key) {
//        for (SceneExplorerNode di : Lookup.getDefault().lookupAll(SceneExplorerNode.class)) {
//            if (di.getExplorerObjectClass().getName().equals(key.getClass().getName())) {
//                Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Found {0}", di.getExplorerNodeClass());
//                Node[] ret = di.createNodes(key, dataObject, readOnly);
//                if (ret != null) {
//                    return ret;
//                }
//            }
//        }

        if (key instanceof Animation) {
            JmeTrackChildren children = new JmeTrackChildren();
            children.setReadOnly(readOnly);            
            return new Node[]{new JmeAnimation(jmeAnimControl, (Animation) key, children, dataObject).setReadOnly(readOnly)};
        }
        return new Node[]{Node.EMPTY};
    }

    public void setAnimControl(JmeAnimControl jmeAnimControl) {
        this.jmeAnimControl = jmeAnimControl;
    }

    public DataObject getDataObject() {
        return dataObject;
    }

    public void setDataObject(DataObject dataObject) {
        this.dataObject = dataObject;
    }
}

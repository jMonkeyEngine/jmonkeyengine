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
package com.jme3.gde.core.filters.actions;

/**
 *
 * @author Nehon
 */
import com.jme3.gde.core.filters.AbstractFilterNode;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.post.Filter;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

public class EnableFiterAction implements ActionListener {

    private final AbstractFilterNode context;

    public EnableFiterAction(AbstractFilterNode context) {
        this.context = context;
    }

    public void actionPerformed(ActionEvent ev) {

        SceneApplication.getApplication().enqueue(new Callable<Void>() {

            public Void call() throws Exception {
                Filter filter=context.getFilter();
                filter.setEnabled(!filter.isEnabled());                        
                Logger.getLogger(EnableFiterAction.class.getName()).info( (filter.isEnabled()?"Enabled":"Disabled")+" "+filter.getName());
                context.propertyChange("Enabled", !filter.isEnabled(), filter.isEnabled());
              
                return null;
            }
        });
    }
}

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

package com.jme3.scene.control;

import com.jme3.export.Savable;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;

/**
 * An interface for scene-graph controls. 
 * <p>
 * <code>Control</code>s are used to specify certain update and render logic
 * for a {@link Spatial}. 
 *
 * @author Kirill Vainer
 */
public interface Control extends Savable {

    /**
     * Creates a clone of the Control, the given Spatial is the cloned
     * version of the spatial to which this control is attached to.
     * @param spatial
     * @return A clone of this control for the spatial
     */
    public Control cloneForSpatial(Spatial spatial);

    /**
     * @param spatial the spatial to be controlled. This should not be called
     * from user code.
     */
    public void setSpatial(Spatial spatial);

    /**
     * @param enabled Enable or disable the control. If disabled, update()
     * should do nothing.
     */
    public void setEnabled(boolean enabled);

    /**
     * @return True if enabled, false otherwise.
     * @see Control#setEnabled(boolean)
     */
    public boolean isEnabled();

    /**
     * Updates the control. This should not be called from user code.
     * @param tpf Time per frame.
     */
    public void update(float tpf);

    /**
     * Should be called prior to queuing the spatial by the RenderManager. This
     * should not be called from user code.
     *
     * @param rm
     * @param vp
     */
    public void render(RenderManager rm, ViewPort vp);
}

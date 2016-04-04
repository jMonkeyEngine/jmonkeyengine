/*
 * Copyright (c) 2009-2016 jMonkeyEngine
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
package com.jme3.material;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.scene.Spatial;
import com.jme3.shader.VarType;
import java.io.IOException;

/**
 * <code>MatParamOverride</code> is a mechanism by which
 * {@link MatParam material parameters} can be overridden on the scene graph.
 * <p>
 * A scene branch which has a <code>MatParamOverride</code> applied to it will
 * cause all material parameters with the same name and type to have their value
 * replaced with the value set on the <code>MatParamOverride</code>. If those
 * parameters are mapped to a define, then the define will be overridden as well
 * using the same rules as the ones used for regular material parameters.
 * <p>
 * <code>MatParamOverrides</code> are applied to a {@link Spatial} via the
 * {@link Spatial#addMatParamOverride(com.jme3.material.MatParamOverride)}
 * method. They are propagated to child <code>Spatials</code> via
 * {@link Spatial#updateGeometricState()} similar to how lights are propagated.
 * <p>
 * Example:<br>
 * <pre>
 * {@code
 *
 * Geometry box = new Geometry("Box", new Box(1,1,1));
 * Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
 * mat.setColor("Color", ColorRGBA.Blue);
 * box.setMaterial(mat);
 * rootNode.attachChild(box);
 *
 * // ... later ...
 * MatParamOverride override = new MatParamOverride(Type.Vector4, "Color", ColorRGBA.Red);
 * rootNode.addMatParamOverride(override);
 *
 * // After adding the override to the root node, the box becomes red.
 * }
 * </pre>
 *
 * @author Kirill Vainer
 * @see Spatial#addMatParamOverride(com.jme3.material.MatParamOverride)
 * @see Spatial#getWorldMatParamOverrides()
 */
public final class MatParamOverride extends MatParam {

    private boolean enabled = true;

    /**
     * Serialization only. Do not use.
     */
    public MatParamOverride() {
        super();
    }

    /**
     * Create a new <code>MatParamOverride</code>.
     *
     * Overrides are created enabled by default.
     *
     * @param type The type of parameter.
     * @param name The name of the parameter.
     * @param value The value to set the material parameter to.
     */
    public MatParamOverride(VarType type, String name, Object value) {
        super(type, name, value);
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) && this.enabled == ((MatParamOverride) obj).enabled;
    }

    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 59 * hash + (enabled ? 1 : 0);
        return hash;
    }

    /**
     * Determine if the <code>MatParamOverride</code> is enabled or disabled.
     *
     * @return true if enabled, false if disabled.
     * @see #setEnabled(boolean)
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Enable or disable this <code>MatParamOverride</code>.
     *
     * When disabled, the override will continue to propagate through the scene
     * graph like before, but it will have no effect on materials. Overrides are
     * enabled by default.
     *
     * @param enabled Whether to enable or disable this override.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(enabled, "enabled", true);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        enabled = ic.readBoolean("enabled", true);
    }
}

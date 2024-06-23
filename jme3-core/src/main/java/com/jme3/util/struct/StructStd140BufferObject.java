/*
 * Copyright (c) 2009-2024 jMonkeyEngine
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
package com.jme3.util.struct;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.shader.bufferobject.BufferObject;
import com.jme3.shader.bufferobject.layout.Std140Layout;

/**
 * A BufferObject containing a struct serialized with Std140 layout.
 * 
 * @author Riccardo Balbo
 */
public class StructStd140BufferObject extends BufferObject {
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(StructStd140BufferObject.class.getName());
    private transient Class<? extends Struct> rootStruct;
    private transient List<StructField<?>> resolvedFields;
    private final Std140Layout std140 = new Std140Layout();

    /**
     * Create an empty Struct buffer.
     */
    public StructStd140BufferObject() {
    }

    /**
     * Internal only.
     * 
     * @param id
     */
    public StructStd140BufferObject(int id) {
        super(id);
    }

    /**
     * Create a Struct buffer from a Struct
     * @param str the source struct
     */
    public StructStd140BufferObject(Struct str) {
        this();
        update(str);
    }

    private void loadLayout(Struct struct) {
        ArrayList<Field> classFields = new ArrayList<Field>();
        resolvedFields = StructUtils.getFields(struct, classFields);
        for (Field field : classFields) {
            if (!Modifier.isFinal(field.getModifiers())) throw new RuntimeException("Can't load layout for " + struct + " every field must be final");
        }
        rootStruct = struct.getClass();
        StructUtils.setStd140BufferLayout(resolvedFields, std140, this);
    }

    /**
     * Update data and layout (when needed) using a Struct class.
     * 
     * @param struct
     */
    public void update(Struct struct) {
        boolean forceUpdate = false;
        if (rootStruct != struct.getClass()) {
            if (logger.isLoggable(java.util.logging.Level.FINE)) {
                logger.log(java.util.logging.Level.FINE, "Change in layout {0} =/= {1} ", new Object[] { rootStruct, struct.getClass() });
            }
            loadLayout(struct);
            forceUpdate = true;
        }
        StructUtils.updateBufferData(resolvedFields, forceUpdate, std140, this);
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(rootStruct.getName(), "rootClass", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        try {
            String rootClass = ic.readString("rootClass", null);
            if (rootClass == null) throw new Exception("rootClass is undefined");
            Class<? extends Struct> rootStructClass = (Class<? extends Struct>) Class.forName(rootClass);
            Struct rootStruct = rootStructClass.newInstance();
            loadLayout(rootStruct);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public StructStd140BufferObject clone() {
        StructStd140BufferObject clone = (StructStd140BufferObject) super.clone();
        return clone;
    }

}

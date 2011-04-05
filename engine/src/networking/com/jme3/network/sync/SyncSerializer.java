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

package com.jme3.network.sync;

import com.jme3.network.serializing.Serializer;
import com.jme3.network.sync.Sync.SyncType;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

@Deprecated
class SyncSerializer {

    static class SyncFieldInfo {
        private Field field;
        private boolean init,sync,smooth;
    }

    static class FieldTable extends ArrayList<SyncFieldInfo> {
    }

    private HashMap<Class<?>, FieldTable> classFieldTables
            = new HashMap<Class<?>, FieldTable>();

    private FieldTable generateFieldTable(Class<?> clazz){
        FieldTable table = new FieldTable();
        while (clazz != null){
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields){
                Sync syncAnnot = field.getAnnotation(Sync.class);
                if (syncAnnot == null)
                    continue;

                SyncFieldInfo info = new SyncFieldInfo();
                field.setAccessible(true);
                info.field  = field;
                info.init   = syncAnnot.value() == SyncType.Init
                           || syncAnnot.value() == SyncType.InitOnly;
                info.sync   = syncAnnot.value() != SyncType.InitOnly;
                info.smooth = syncAnnot.smooth();
                table.add(info);
            }
            
            clazz = clazz.getSuperclass();
        }

        return table;
    }

    private FieldTable getTable(Class<?> clazz){
        FieldTable table = classFieldTables.get(clazz);
        if (table == null){
            table = generateFieldTable(clazz);
            classFieldTables.put(clazz, table);
        }
        return table;
    }

    public void read(Object entity, ByteBuffer in, boolean init){
        FieldTable table = getTable(entity.getClass());
        for (SyncFieldInfo fieldInfo : table){
            if ( (init && !fieldInfo.init)
              || (!init && !fieldInfo.sync) )
                continue;

            Field field = fieldInfo.field;
            try {
                Object obj = Serializer.readClassAndObject(in);
                field.set(entity, obj);
            } catch (Exception ex){
                ex.printStackTrace();
            }
        }
    }

    public void write(Object entity, ByteBuffer out, boolean init){
        FieldTable table = getTable(entity.getClass());
        for (SyncFieldInfo fieldInfo : table){
            if ( (init && !fieldInfo.init)
              || (!init && !fieldInfo.sync) )
                continue;

            Field field = fieldInfo.field;
            try {
                Serializer.writeClassAndObject(out, field.get(entity));
            } catch (Exception ex){
                ex.printStackTrace();
            }
        }
    }
}

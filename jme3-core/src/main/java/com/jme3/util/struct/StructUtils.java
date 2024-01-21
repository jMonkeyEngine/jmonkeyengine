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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.jme3.shader.bufferobject.BufferObject;
import com.jme3.shader.bufferobject.BufferRegion;
import com.jme3.shader.bufferobject.layout.BufferLayout;
import com.jme3.shader.bufferobject.layout.Std140Layout;

/**
 * StructUtils
 * 
 * @author Riccardo Balbo
 */
public class StructUtils {
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(StructUtils.class.getName());

    private static final Comparator<StructField<?>> fieldComparator = new Comparator<StructField<?>>() {
        @Override
        public int compare(final StructField<?> a, final StructField<?> b) {
            return a.getPosition() - b.getPosition();
        }
    };

    /**
     * In-place sort a List of StructFields accordingly to their position
     * 
     * @param fields
     *            list to sort
     * @return the passed list
     */
    public static List<StructField<?>> sortFields(List<StructField<?>> fields) {
        fields.sort(fieldComparator);
        return fields;
    }

    /**
     * Get sorted List of StructFields from a Struct object
     * 
     * @param struct
     *            the struct object
     * @return the sorted list
     */
    public static List<StructField<?>> getFields(Struct struct) {
        return getFields(struct, 0, null);
    }

    public static List<StructField<?>> getFields(Struct struct, ArrayList<Field> classFields) {
        return getFields(struct, 0, classFields);
    }

    private static List<StructField<?>> getFields(Struct struct, int depth, ArrayList<Field> classFields) {// ,
                                                                                                           // final
                                                                                                           // List<Field>
                                                                                                           // fieldList)
                                                                                                           // {
        ArrayList<StructField<?>> structFields = new ArrayList<StructField<?>>();

        Class<? extends Struct> structClass = struct.getClass();
        try {
            // for each class field
            // Extract class fields into a StructField List
            // (Note: class methods are iterated in undefined order)
            Field[] fields = structClass.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);

                Object o = field.get(struct);
                if (o instanceof StructField) {
                    if (classFields != null) classFields.add(field);
                    StructField<?> so = (StructField<?>) o;
                    structFields.add(so);
                }
            }

            // Sort by position
            sortFields(structFields);

            ArrayList<StructField<?>> expandedStructFields = new ArrayList<StructField<?>>();

            // Expand sub struct and arrays to flat list
            for (int i = 0; i < structFields.size(); i++) {
                StructField<?> so = structFields.get(i);
                if (so.getValue() instanceof Struct) { // substruct
                    List<StructField<?>> subStruct = getFields((Struct) so.getValue(), depth + 1, classFields);
                    expandedStructFields.addAll(subStruct);
                } else if (so.getValue().getClass().isArray() && Struct.class.isAssignableFrom(so.getValue().getClass().getComponentType())) { // array
                                                                                                                                               // of
                                                                                                                                               // substruct

                    Struct[] subA = (Struct[]) so.getValue();
                    for (int j = 0; j < subA.length; j++) {
                        Struct sub = subA[j];
                        List<StructField<?>> subStruct = getFields(sub, depth + 1, classFields);
                        expandedStructFields.addAll(subStruct);
                    }

                } else {
                    so.setDepth(depth);
                    so.setGroup(struct.hashCode());
                    expandedStructFields.add(so);
                }
            }
            structFields = expandedStructFields;

            // Recompute positions in flat list
            int i = 0;
            for (StructField<?> so : structFields) {
                so.setPosition(i);
                i++;
            }

        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
        assert structFields.size() != 0;
        return structFields;
    }

    public static BufferObject setStd140BufferLayout(List<StructField<?>> fields, Std140Layout serializer, BufferObject out) {// ,
                                                                                                                              // final
                                                                                                                              // List<Field>
                                                                                                                              // fieldList)
                                                                                                                              // {

        int pos = -1;

        List<BufferRegion> regions = new ArrayList<BufferRegion>();

        for (int i = 0; i < fields.size(); i++) {
            StructField<?> f = fields.get(i);
            Object v = f.getValue();

            int basicAlignment = serializer.getBasicAlignment(v);
            int length = serializer.estimateSize(v);

            int start = serializer.align(pos + 1, basicAlignment);
            int end = start + length - 1;

            if ((i == fields.size() - 1) || f.getGroup()!= fields.get(i + 1).getGroup()){// > fields.get(i + 1).getDepth()) {
                end = (serializer.align(end, 16)) - 1;
            }

            BufferRegion r = new BufferRegion(start, end);
            regions.add(r);
            pos = end;
        }

        out.setRegions(regions);

        return out;
    }

    /**
     * Update data using a List of StructFields The current layout will be
     * maintained unless previously invalidated
     * 
     * @param fields
     *            sorted list of struct fields
     */
    public static void updateBufferData(List<StructField<?>> fields, boolean forceUpdate, BufferLayout layout, BufferObject out) {
        boolean updateNeeded = false;
        for (StructField<?> f : fields) {
            if (forceUpdate || f.isUpdateNeeded()) {

                BufferRegion region = out.getRegion(f.getPosition());
                if (logger.isLoggable(java.util.logging.Level.FINER)) {
                    logger.log(java.util.logging.Level.FINER, "Serialize {0} in {1} ", new Object[] { f, region });
                }
                layout.write(region.getData(), f.getValue());
                region.markDirty();
                f.clearUpdateNeeded();
                updateNeeded = true;
            } else {
                if (logger.isLoggable(java.util.logging.Level.FINER)) {
                    logger.log(java.util.logging.Level.FINER, "Already up to date. Skip {0}  ", new Object[] { f });
                }
            }
        }
        if (updateNeeded) out.setUpdateNeeded(false);
    }

}
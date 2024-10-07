/*
 * Copyright (c) 2009-2021 jMonkeyEngine, Java Game Networking
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
package com.jme3.network.serializing.serializers;

import com.jme3.network.serializing.Serializer;
import com.jme3.network.serializing.SerializerException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The field serializer is the default serializer used for custom class.
 *
 * @author Lars Wesselius, Nathan Sweet
 */
public class FieldSerializer extends Serializer {
    
    private static final Logger log = Logger.getLogger(FieldSerializer.class.getName());

    private static Map<Class, SavedField[]> savedFields = new HashMap<Class, SavedField[]>();
    private static Map<Class, Constructor> savedCtors = new HashMap<Class, Constructor>();

    @SuppressWarnings("unchecked")
    protected void checkClass(Class clazz) {
    
        // See if the class has a public no-arg constructor
        try {
            savedCtors.put(clazz, clazz.getConstructor());
            return;
        } catch( NoSuchMethodException e ) {
            //throw new RuntimeException( "Registration error: no-argument constructor not found on:" + clazz ); 
        }
        
        // See if it has a non-public no-arg constructor
        try {
            Constructor ctor = clazz.getDeclaredConstructor();
            
            // Make sure we can call it later.
            ctor.setAccessible(true);
             
            savedCtors.put(clazz, ctor);
            return;
        } catch( NoSuchMethodException e ) {
        }
        
        throw new RuntimeException( "Registration error: no-argument constructor not found on:" + clazz );  
    }        
    
    @Override
    public void initialize(Class clazz) {

        checkClass(clazz);   
    
        List<Field> fields = new ArrayList<>();

        Class processingClass = clazz;
        while (processingClass != Object.class ) {
            Collections.addAll(fields, processingClass.getDeclaredFields());
            processingClass = processingClass.getSuperclass();
        }

        List<SavedField> cachedFields = new ArrayList<>(fields.size());
        for (Field field : fields) {
            int modifiers = field.getModifiers();
            if (Modifier.isTransient(modifiers)) continue;
            if (Modifier.isStatic(modifiers)) continue;
            if (field.isSynthetic()) continue;
            field.setAccessible(true);

            SavedField cachedField = new SavedField();
            cachedField.field = field;

            if (Modifier.isFinal(field.getType().getModifiers())) {
                // The type of this field is implicit in the outer class
                // definition and because the type is final, it can confidently
                // be determined on the other end.
                // Note: passing false to this method has the side effect that field.getType()
                // will be registered as a real class that can then be read/written
                // directly as any other registered class.  It should be safe to take
                // an ID like this because Serializer.initialize() is only called 
                // during registration... so this is like nested registration and
                // doesn't have any ordering problems.
                // ...well, as long as the order of fields is consistent from one
                // end to the next. 
                cachedField.serializer = Serializer.getSerializer(field.getType(), false);
            }                

            cachedFields.add(cachedField);
        }

        Collections.sort(cachedFields, new Comparator<SavedField>() {
            @Override
            public int compare (SavedField o1, SavedField o2) {
                    return o1.field.getName().compareTo(o2.field.getName());
            }
        });
        savedFields.put(clazz, cachedFields.toArray(new SavedField[cachedFields.size()]));

        
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T readObject(ByteBuffer data, Class<T> c) throws IOException {
    
        // Read the null/non-null marker
        if (data.get() == 0x0)
            return null;
    
        SavedField[] fields = savedFields.get(c);

        T object;
        try {
            Constructor<T> ctor = savedCtors.get(c);
            object = ctor.newInstance();
        } catch (Exception e) {
            throw new SerializerException( "Error creating object of type:" + c, e );
        }

        for (SavedField savedField : fields) {
            Field field = savedField.field;
            Serializer serializer = savedField.serializer;
            if( log.isLoggable(Level.FINER) ) {
                log.log(Level.FINER, "Reading field:{0} using serializer:{1}", new Object[]{field, serializer});
            }
            Object value;

            if (serializer != null) {
                value = serializer.readObject(data, field.getType());
            } else {
                value = Serializer.readClassAndObject(data);
            }
            try {
                field.set(object, value);
            } catch (IllegalAccessException e) {
                throw new SerializerException( "Error reading object", e);
            }
        }
        return object;
    }

    @Override
    public void writeObject(ByteBuffer buffer, Object object) throws IOException {
    
        // Add the null/non-null marker
        buffer.put( (byte)(object != null ? 0x1 : 0x0) );
        if (object == null) {
            // Nothing left to do
            return;
        }
        
        SavedField[] fields = savedFields.get(object.getClass());
        if (fields == null)
            throw new IOException("The " + object.getClass() + " is not registered"
                                + " in the serializer!");

        for (SavedField savedField : fields) {
            Object val = null;
            try {
                val = savedField.field.get(object);
            } catch (IllegalAccessException e) {
                throw new SerializerException("Unable to access field:" + savedField.field + " on:" + object, e);
            }
            Serializer serializer = savedField.serializer;
            if( log.isLoggable(Level.FINER) ) {
                log.log(Level.FINER, "Writing field:{0} using serializer:{1}", new Object[]{savedField.field, serializer});
            }

            try {
                if (serializer != null) {
                    serializer.writeObject(buffer, val);
                } else {
                    Serializer.writeClassAndObject(buffer, val);
                }
            } catch (BufferOverflowException boe) {
                throw boe;
            } catch (Exception e) {
                throw new SerializerException( "Error writing object for field:" + savedField.field, e );
            }
        }
    }

    private final class SavedField {
        public Field field;
        public Serializer serializer;
    }
}

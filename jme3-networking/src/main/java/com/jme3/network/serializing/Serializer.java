/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
package com.jme3.network.serializing;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.WeakHashMap;
import java.util.jar.Attributes;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.math.Vector3f;
import com.jme3.network.message.ChannelInfoMessage;
import com.jme3.network.message.ClientRegistrationMessage;
import com.jme3.network.message.DisconnectMessage;
import com.jme3.network.message.GZIPCompressedMessage;
import com.jme3.network.message.ZIPCompressedMessage;
import com.jme3.network.serializing.serializers.ArraySerializer;
import com.jme3.network.serializing.serializers.BooleanSerializer;
import com.jme3.network.serializing.serializers.ByteSerializer;
import com.jme3.network.serializing.serializers.CharSerializer;
import com.jme3.network.serializing.serializers.CollectionSerializer;
import com.jme3.network.serializing.serializers.DateSerializer;
import com.jme3.network.serializing.serializers.DoubleSerializer;
import com.jme3.network.serializing.serializers.EnumSerializer;
import com.jme3.network.serializing.serializers.FieldSerializer;
import com.jme3.network.serializing.serializers.FloatSerializer;
import com.jme3.network.serializing.serializers.GZIPSerializer;
import com.jme3.network.serializing.serializers.IntSerializer;
import com.jme3.network.serializing.serializers.LongSerializer;
import com.jme3.network.serializing.serializers.MapSerializer;
import com.jme3.network.serializing.serializers.SerializableSerializer;
import com.jme3.network.serializing.serializers.ShortSerializer;
import com.jme3.network.serializing.serializers.StringSerializer;
import com.jme3.network.serializing.serializers.Vector3Serializer;
import com.jme3.network.serializing.serializers.ZIPSerializer;

/**
 * The main serializer class, which will serialize objects such that
 *  they can be sent across the network. Serializing classes should extend
 *  this to provide their own serialization.
 *
 * @author Lars Wesselius
 */
public abstract class Serializer {
    protected static final Logger log = Logger.getLogger( Serializer.class.getName() );

    private static final SerializerRegistration NULL_CLASS = new SerializerRegistration( null, Void.class, (short) -1 );

    private static final Map<Short, SerializerRegistration> idRegistrations = new HashMap<Short, SerializerRegistration>();
    private static final Map<Class, SerializerRegistration> classRegistrations = new HashMap<Class, SerializerRegistration>();
    private static final List<SerializerRegistration> registrations = new ArrayList<SerializerRegistration>();

    private static final Serializer fieldSerializer = new FieldSerializer();
    private static final Serializer serializableSerializer = new SerializableSerializer();
    private static final Serializer arraySerializer = new ArraySerializer();

    private static short nextAvailableId = -2; // historically the first ID was always -2

    private static boolean strictRegistration = true;

    private static volatile boolean locked = false;

    // Registers the classes we already have serializers for.
    static {

        // Preregister some fixed serializers so that they don't move
        // if the list below is modified.  Automatic ID generation will
        // skip these IDs.
        registerClassForId( DisconnectMessage.SERIALIZER_ID, DisconnectMessage.class, new DisconnectMessage.DisconnectSerializer() );
        registerClassForId( ClientRegistrationMessage.SERIALIZER_ID, ClientRegistrationMessage.class,
                        new ClientRegistrationMessage.ClientRegistrationSerializer() );

        registerClass( boolean.class, new BooleanSerializer() );
        registerClass( byte.class, new ByteSerializer() );
        registerClass( char.class, new CharSerializer() );
        registerClass( short.class, new ShortSerializer() );
        registerClass( int.class, new IntSerializer() );
        registerClass( long.class, new LongSerializer() );
        registerClass( float.class, new FloatSerializer() );
        registerClass( double.class, new DoubleSerializer() );

        registerClass( Boolean.class, new BooleanSerializer() );
        registerClass( Byte.class, new ByteSerializer() );
        registerClass( Character.class, new CharSerializer() );
        registerClass( Short.class, new ShortSerializer() );
        registerClass( Integer.class, new IntSerializer() );
        registerClass( Long.class, new LongSerializer() );
        registerClass( Float.class, new FloatSerializer() );
        registerClass( Double.class, new DoubleSerializer() );
        registerClass( String.class, new StringSerializer() );

        registerClass( Vector3f.class, new Vector3Serializer() );

        registerClass( Date.class, new DateSerializer() );

        // all the Collection classes go here
        registerClass( AbstractCollection.class, new CollectionSerializer() );
        registerClass( AbstractList.class, new CollectionSerializer() );
        registerClass( AbstractSet.class, new CollectionSerializer() );
        registerClass( ArrayList.class, new CollectionSerializer() );
        registerClass( HashSet.class, new CollectionSerializer() );
        registerClass( LinkedHashSet.class, new CollectionSerializer() );
        registerClass( LinkedList.class, new CollectionSerializer() );
        registerClass( TreeSet.class, new CollectionSerializer() );
        registerClass( Vector.class, new CollectionSerializer() );

        // All the Map classes go here
        registerClass( AbstractMap.class, new MapSerializer() );
        registerClass( Attributes.class, new MapSerializer() );
        registerClass( HashMap.class, new MapSerializer() );
        registerClass( Hashtable.class, new MapSerializer() );
        registerClass( IdentityHashMap.class, new MapSerializer() );
        registerClass( TreeMap.class, new MapSerializer() );
        registerClass( WeakHashMap.class, new MapSerializer() );

        registerClass( Enum.class, new EnumSerializer() );
        registerClass( GZIPCompressedMessage.class, new GZIPSerializer() );
        registerClass( ZIPCompressedMessage.class, new ZIPSerializer() );

        registerClass( ChannelInfoMessage.class );
    }

    private static List<Class> findClasses( File dir, String pkgName ) throws ClassNotFoundException {
        final List<Class> classes = new ArrayList<Class>();
        if (!dir.exists()) {
            return classes;
        }
        final File[] files = dir.listFiles();
        for (final File file : files) {
            if (file.isDirectory()) {
                assert !file.getName().contains( "." );
                classes.addAll( findClasses( file, pkgName + "." + file.getName() ) );
            }
            else if (file.getName().endsWith( ".class" )) {
                classes.add( Class.forName( pkgName + '.' + file.getName().substring( 0, file.getName().length() - 6 ) ) );
            }
        }
        return classes;
    }

    public static Serializer getExactSerializer( Class cls ) {
        return classRegistrations.get( cls ).getSerializer();
    }

    public static SerializerRegistration getExactSerializerRegistration( Class cls ) {
        return classRegistrations.get( cls );
    }

    public static Serializer getSerializer( Class cls ) {
        return getSerializer( cls, true );
    }

    public static Serializer getSerializer( Class cls, boolean failOnMiss ) {
        return getSerializerRegistration( cls, failOnMiss ).getSerializer();
    }

    public static SerializerRegistration getSerializerRegistration( Class cls ) {
        return getSerializerRegistration( cls, strictRegistration );
    }

    @SuppressWarnings( "unchecked" )
    public static SerializerRegistration getSerializerRegistration( Class cls, boolean failOnMiss ) {
        final SerializerRegistration reg = classRegistrations.get( cls );

        if (reg != null) {
            return reg;
        }

        for (final Map.Entry<Class, SerializerRegistration> entry : classRegistrations.entrySet()) {
            if (entry.getKey().isAssignableFrom( Serializable.class )) {
                continue;
            }
            if (entry.getKey().isAssignableFrom( cls )) {
                return entry.getValue();
            }
        }

        if (cls.isArray()) {
            return registerClass( cls, arraySerializer );
        }

        if (Serializable.class.isAssignableFrom( cls )) {
            return getExactSerializerRegistration( java.io.Serializable.class );
        }

        // See if the class could be safely auto-registered
        if (cls.isAnnotationPresent( Serializable.class )) {
            final Serializable serializable = (Serializable) cls.getAnnotation( Serializable.class );
            final short classId = serializable.id();
            if (classId != 0) {
                // No reason to fail because the ID is fixed
                failOnMiss = false;
            }
        }

        if (failOnMiss) {
            throw new IllegalArgumentException( "Class has not been registered:" + cls );
        }
        return registerClass( cls, fieldSerializer );
    }

    public static Collection<SerializerRegistration> getSerializerRegistrations() {
        return registrations;
    }

    public static boolean isReadOnly() {
        return locked;
    }

    private static short nextId() {

        // If the ID we are about to return is already in use
        // then skip it.
        while (idRegistrations.containsKey( nextAvailableId )) {
            nextAvailableId--;
        }

        // Return the available ID and post-decrement to get
        // ready for next time.
        return nextAvailableId--;
    }

    /**
     * Read the class from given buffer and return its SerializerRegistration.
     *
     * @param buffer The buffer to read from.
     * @return The SerializerRegistration, or null if non-existent.
     */
    public static SerializerRegistration readClass( ByteBuffer buffer ) {
        final short classID = buffer.getShort();
        if (classID == -1) {
            return NULL_CLASS;
        }
        return idRegistrations.get( classID );
    }

    /**
     * Read the class and the object.
     *
     * @param buffer Buffer to read from.
     * @return The Object that was read.
     * @throws IOException If serialization failed.
     */
    @SuppressWarnings( "unchecked" )
    public static Object readClassAndObject( ByteBuffer buffer ) throws IOException {
        final SerializerRegistration reg = readClass( buffer );
        if (reg == NULL_CLASS) {
            return null;
        }
        if (reg == null) {
            throw new SerializerException( "Class not found for buffer data." );
        }
        return reg.getSerializer().readObject( buffer, reg.getType() );
    }

    public static SerializerRegistration registerClass( Class<?> cls ) {
        return registerClass( cls, true );
    }

    /**
     * Registers the specified class. The failOnMiss flag controls whether or
     * not this method returns null for failed registration or throws an exception.
     * @param cls the class to register (must be annotated with {@link Serializable})
     * @param failOnMiss controls if the method should throw an exception, or uses defaults on error
     * @return a SerializerRegistration or null if failOnMiss is set to false
     */
    public static SerializerRegistration registerClass( Class<?> cls, boolean failOnMiss ) {
        if (cls == null) {
            throw new IllegalArgumentException( "cls must not be null" );
        }

        if (cls.isAnnotationPresent( Serializable.class )) {
            final Serializable serializable = cls.getAnnotation( Serializable.class );

            final Class<? extends Serializer> serializerClass = serializable.serializer();
            short classId = serializable.id();
            if (classId == 0) {
                classId = nextId();
            }
            Serializer serializer = null;

            try {
                serializer = serializerClass.newInstance();
            }
            catch (InstantiationException | IllegalAccessException e) {
                if (failOnMiss) {
                    throw new IllegalStateException( e );
                }
                else {
                    log.log( Level.WARNING, "Failed to instantiate serializer (" + serializerClass.getName() + ") for class: " + cls.getName() + " with error "
                                    + e.getMessage() + ". Falling back to FieldSerializer" );
                }
            }

            if (serializer == null) {
                serializer = fieldSerializer;
            }

            final SerializerRegistration existingReg = getExactSerializerRegistration( cls );

            if (existingReg != null) {
                classId = existingReg.getId();
            }

            return registerClassForId( classId, cls, serializer );
        }
        else if (failOnMiss) {
            throw new IllegalArgumentException( "Class is not marked @Serializable:" + cls );
        }
        else {
            return null;
        }
    }

    public static SerializerRegistration registerClass( Class<?> cls, Serializer serializer ) {
        final SerializerRegistration existingReg = getExactSerializerRegistration( cls );

        short id;
        if (existingReg != null) {
            id = existingReg.getId();
        }
        else {
            id = nextId();
        }
        return registerClassForId( id, cls, serializer );
    }

    public static void registerClasses( Class... classes ) {
        for (final Class c : classes) {
            registerClass( c );
        }
    }

    /**
     *  Directly registers a class for a specific ID.  Generally, use the regular
     *  registerClass() method.  This method is intended for framework code that might
     *  be maintaining specific ID maps across client and server.
     */
    public static SerializerRegistration registerClassForId( short id, Class cls, Serializer serializer ) {

        if (locked) {
            throw new RuntimeException( "Serializer registry locked trying to register class:" + cls );
        }

        final SerializerRegistration reg = new SerializerRegistration( serializer, cls, id );

        idRegistrations.put( id, reg );
        classRegistrations.put( cls, reg );

        log.log( Level.FINE, "Registered class[" + id + "]:{0} to:" + serializer, cls );

        serializer.initialize( cls );

        // Add the class after so that dependency order is preserved if the
        // serializer registers its own classes.
        registrations.add( reg );

        return reg;
    }

    /**
     *  @deprecated This cannot be implemented in a reasonable way that works in
     *              all deployment methods.
     */
    @Deprecated
    public static SerializerRegistration[] registerPackage( String pkgName ) {
        try {
            final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            final String path = pkgName.replace( '.', '/' );
            final Enumeration<URL> resources = classLoader.getResources( path );
            final List<File> dirs = new ArrayList<File>();
            while (resources.hasMoreElements()) {
                final URL resource = resources.nextElement();
                dirs.add( new File( resource.getFile() ) );
            }
            final ArrayList<Class> classes = new ArrayList<Class>();
            for (final File directory : dirs) {
                classes.addAll( findClasses( directory, pkgName ) );
            }

            final SerializerRegistration[] registeredClasses = new SerializerRegistration[classes.size()];
            for (int i = 0; i != classes.size(); ++i) {
                final Class clz = classes.get( i );
                registeredClasses[i] = registerClass( clz, false );
            }
            return registeredClasses;
        }
        catch (final Exception e) {
            e.printStackTrace();
        }
        return new SerializerRegistration[0];
    }

    ///////////////////////////////////////////////////////////////////////////////////

    /**
     *  Can put the registry in a read-only state such that additional attempts
     *  to register classes will fail.  This can be used by servers to lock the
     *  registry to avoid accidentally registering classes after a full registry
     *  set has been compiled.
     */
    public static void setReadOnly( boolean b ) {
        locked = b;
    }

    /**
     *  When set to true, classes that do not have intrinsic IDs in their
     *  @Serializable will not be auto-registered during write.  Defaults
     *  to true since this is almost never desired behavior with the way
     *  this code works.  Set to false to get the old permissive behavior.
     */
    public static void setStrictRegistration( boolean b ) {
        strictRegistration = b;
    }

    /**
     * Write a class and return its SerializerRegistration.
     *
     * @param buffer The buffer to write the given class to.
     * @param type The class to write.
     * @return The SerializerRegistration that's registered to the class.
     */
    public static SerializerRegistration writeClass( ByteBuffer buffer, Class type ) throws IOException {
        final SerializerRegistration reg = getSerializerRegistration( type );
        if (reg == null) {
            throw new SerializerException( "Class not registered:" + type );
        }
        buffer.putShort( reg.getId() );
        return reg;
    }

    /**
     * Write the class and object.
     *
     * @param buffer The buffer to write to.
     * @param object The object to write.
     * @throws IOException If serializing fails.
     */
    public static void writeClassAndObject( ByteBuffer buffer, Object object ) throws IOException {
        if (object == null) {
            buffer.putShort( (short) -1 );
            return;
        }
        final SerializerRegistration reg = writeClass( buffer, object.getClass() );
        reg.getSerializer().writeObject( buffer, object );
    }

    /**
     * Registration for when a serializer may need to cache something.
     *
     * Override to use.
     *
     * @param clazz The class that has been registered to the serializer.
     */
    public void initialize( Class clazz ) {
    }

    /**
     * Read an object from the buffer, effectively deserializing it.
     *
     * @param data The buffer to read from.
     * @param c The class of the object.
     * @return The object read.
     * @throws IOException If deserializing fails.
     */
    public abstract <T> T readObject( ByteBuffer data, Class<T> c ) throws IOException;

    /**
     * Write an object to the buffer, effectively serializing it.
     *
     * @param buffer The buffer to write to.
     * @param object The object to serialize.
     * @throws IOException If serializing fails.
     */
    public abstract void writeObject( ByteBuffer buffer, Object object ) throws IOException;
}

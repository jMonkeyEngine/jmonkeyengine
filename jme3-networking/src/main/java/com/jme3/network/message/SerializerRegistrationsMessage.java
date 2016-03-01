/*
 * $Id: SerializerRegistrationsMessage.java 3829 2014-11-24 07:25:43Z pspeed $
 *
 * Copyright (c) 2012, Paul Speed
 * All rights reserved.
 */

package com.jme3.network.message;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;
import com.jme3.network.serializing.Serializer;
import com.jme3.network.serializing.SerializerRegistration;
import com.jme3.network.serializing.serializers.FieldSerializer;
import java.util.*;
import java.util.jar.Attributes;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *  Holds a compiled set of message registration information that
 *  can be sent over the wire.  The received message can then be
 *  used to register all of the classes using the same IDs and 
 *  same ordering, etc..  The intent is that the server compiles
 *  this message once it is sure that all serializable classes have
 *  been registered.  It can then send this to each new client and
 *  they can use it to register all of the classes without requiring
 *  exactly reproducing the same calls that the server did to register
 *  messages.
 *
 *  <p>Normally, JME recommends that apps have a common utility method
 *  that they call on both client and server.  However, this makes 
 *  pluggable services nearly impossible as some central class has to
 *  know about all registered serializers.  This message implementation
 *  gets around by only requiring registration on the server.</p>
 *
 *  @author    Paul Speed
 */
@Serializable
public class SerializerRegistrationsMessage extends AbstractMessage {

    static final Logger log = Logger.getLogger(SerializerRegistrationsMessage.class.getName());

    public static final Set<Class> ignore = new HashSet<Class>();
    static {
        // We could build this automatically but then we
        // risk making a client and server out of date simply because
        // their JME versions are out of date.
        ignore.add(Boolean.class);
        ignore.add(Float.class);
        ignore.add(Boolean.class);
        ignore.add(Byte.class);
        ignore.add(Character.class);
        ignore.add(Short.class);
        ignore.add(Integer.class);
        ignore.add(Long.class);
        ignore.add(Float.class);
        ignore.add(Double.class);
        ignore.add(String.class);
 
        ignore.add(DisconnectMessage.class);
        ignore.add(ClientRegistrationMessage.class);
    
        ignore.add(Date.class);
        ignore.add(AbstractCollection.class);
        ignore.add(AbstractList.class);
        ignore.add(AbstractSet.class);
        ignore.add(ArrayList.class);
        ignore.add(HashSet.class);
        ignore.add(LinkedHashSet.class);
        ignore.add(LinkedList.class);
        ignore.add(TreeSet.class);
        ignore.add(Vector.class);
        ignore.add(AbstractMap.class);
        ignore.add(Attributes.class);
        ignore.add(HashMap.class);
        ignore.add(Hashtable.class);
        ignore.add(IdentityHashMap.class);
        ignore.add(TreeMap.class);
        ignore.add(WeakHashMap.class);        
        ignore.add(Enum.class);
        
        ignore.add(GZIPCompressedMessage.class);
        ignore.add(ZIPCompressedMessage.class);

        ignore.add(ChannelInfoMessage.class);
        
        ignore.add(SerializerRegistrationsMessage.class);
        ignore.add(SerializerRegistrationsMessage.Registration.class);        
    }
 
    public static SerializerRegistrationsMessage INSTANCE;   
    public static Registration[] compiled;
    private static final Serializer fieldSerializer = new FieldSerializer();
    
    private Registration[] registrations;    

    public SerializerRegistrationsMessage() {
        setReliable(true);
    }

    public SerializerRegistrationsMessage( Registration... registrations ) {
        setReliable(true);
        this.registrations = registrations;
    }
    
    public static void compile() {
    
        // Let's just see what they are here
        List<Registration> list = new ArrayList<Registration>();
        for( SerializerRegistration reg : Serializer.getSerializerRegistrations() ) {
            Class type = reg.getType();
            if( ignore.contains(type) )
                continue;
            if( type.isPrimitive() )
                continue;
 
            list.add(new Registration(reg));
        }
            
        if( log.isLoggable(Level.FINE) ) {
            log.log( Level.FINE, "Number of registered classes:{0}", list.size());
            for( Registration reg : list ) { 
                log.log( Level.FINE, "    {0}", reg);
            }
        }
        compiled = list.toArray(new Registration[list.size()]);
        
        INSTANCE = new SerializerRegistrationsMessage(compiled);  
        
        Serializer.setReadOnly(true);                              
    }
 
    public void registerAll() {

        // See if we will have problems because our registry is locked        
        if( Serializer.isReadOnly() ) {
            // Check to see if maybe we are executing this from the
            // same JVM that sent the message, ie: client and server are running on
            // the same JVM
            // There could be more advanced checks than this but for now we'll
            // assume that if the registry was compiled here then it means
            // we are also the server process.  Note that this wouldn't hold true
            // under complicated examples where there are clients of one server
            // that also run their own servers but realistically they would have
            // to disable the ServerSerializerRegistrationsServer anyway.
            if( compiled != null ) {
                log.log(Level.INFO, "Skipping registration as registry is locked, presumably by a local server process.");
                return;
            }
        }
        
        log.log(Level.FINE, "Registering {0} classes...", registrations.length);
        for( Registration reg : registrations ) {
            log.log(Level.INFO, "Registering:{0}", reg);
            reg.register();
        }
        log.log(Level.FINE, "Done registering serializable classes.");
    }
    
    @Serializable
    public static final class Registration {
    
        private short id;
        private String className;
        private String serializerClassName;
        
        public Registration() {
        }
        
        public Registration( SerializerRegistration reg ) {
        
            this.id = reg.getId();
            this.className = reg.getType().getName();
            if( reg.getSerializer().getClass() != FieldSerializer.class ) {
                this.serializerClassName = reg.getSerializer().getClass().getName();
            } 
        }
 
        public void register() {        
            try {
                Class type = Class.forName(className);
                Serializer serializer;
                if( serializerClassName == null ) {
                    serializer = fieldSerializer;
                } else {
                    Class serializerType = Class.forName(serializerClassName);
                    serializer = (Serializer)serializerType.newInstance();                    
                }
                SerializerRegistration result = Serializer.registerClassForId(id, type, serializer);
                log.log(Level.FINE, "   result:{0}", result);                
            } catch( ClassNotFoundException e ) {
                throw new RuntimeException( "Class not found attempting to register:" + this, e );
            } catch( InstantiationException e ) {
                throw new RuntimeException( "Error instantiating serializer registering:" + this, e );
            } catch( IllegalAccessException e ) {
                throw new RuntimeException( "Error instantiating serializer registering:" + this, e );
            }            
        }
        
        @Override
        public String toString() {
            return "Registration[" + id + " = " + className + ", serializer=" + serializerClassName + "]";
        }
    }
}




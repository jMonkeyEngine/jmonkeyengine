/*
 * Copyright (c) 2015-2021 jMonkeyEngine
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

package com.jme3.network.util;

import com.jme3.network.Message;
import com.jme3.network.MessageConnection;
import com.jme3.network.MessageListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *  A MessageListener implementation that will forward messages to methods
 *  of a delegate object.  These methods can be automapped or manually
 *  specified.  Subclasses provide specific implementations for how to
 *  find the actual delegate object.
 *
 *  @author    Paul Speed
 */
public abstract class AbstractMessageDelegator<S extends MessageConnection> 
                                implements MessageListener<S> {
                                
    private static final Logger log = Logger.getLogger(AbstractMessageDelegator.class.getName());                                
                                
    private Class delegateType;
    private Map<Class, Method> methods = new HashMap<>();
    private Class[] messageTypes;
 
    /**
     *  Creates an AbstractMessageDelegator that will forward received
     *  messages to methods of the specified delegate type.  If automap
     *  is true then reflection is used to lookup probably message handling
     *  methods.
     */   
    protected AbstractMessageDelegator( Class delegateType, boolean automap ) {
        this.delegateType = delegateType;
        if( automap ) {
            automap();
        }
    }
 
    /**
     *  Returns the array of messages known to be handled by this message
     *  delegator.
     */
    public Class[] getMessageTypes() {
        if( messageTypes == null ) {
            messageTypes = methods.keySet().toArray(new Class[methods.size()]);
        }
        return messageTypes;
    }
 
    /**
     *  Returns true if the specified method is valid for the specified
     *  message type.  This is used internally during automapping to
     *  provide implementation specific filtering of methods.
     *  This implementation checks for methods that take either the connection and message 
     *  type arguments (in that order) or just the message type.
     */
    protected boolean isValidMethod( Method m, Class messageType ) {
 
        if( log.isLoggable(Level.FINEST) ) {
            log.log(Level.FINEST, "isValidMethod({0}, {1})", new Object[]{m, messageType});
        }
               
        // Parameters must be S and message type or just message type
        Class<?>[] parms = m.getParameterTypes();
        if( parms.length != 2 && parms.length != 1 ) {
            log.finest("Parameter count is not 1 or 2");
            return false;
        }                   
        int messageIndex = 0;
        if( parms.length > 1 ) {
            if( MessageConnection.class.isAssignableFrom(parms[0]) ) {
                messageIndex++;
            } else {
                log.finest("First parameter is not a MessageConnection or subclass.");
                return false;
            }
        }
 
        if( messageType == null && !Message.class.isAssignableFrom(parms[messageIndex]) ) {
            log.finest("Second parameter is not a Message or subclass.");
            return false;
        }
        if( messageType != null && !parms[messageIndex].isAssignableFrom(messageType) ) {
            log.log(Level.FINEST, "Second parameter is not a {0}", messageType);
            return false;
        }
        return true;            
    }
 
    /**
     *  Convenience method that returns the message type as
     *  reflectively determined for a particular method.  This
     *  only works with methods that actually have arguments.
     *  This implementation returns the last element of the method's
     *  getParameterTypes() array, thus supporting both 
     *  method(connection, messageType) and method(messageType)
     *  calling forms.
     */
    protected Class getMessageType( Method m ) {
        Class<?>[] parms = m.getParameterTypes();
        return parms[parms.length-1];    
    }
 
    /**
     *  Goes through all of the delegate type's methods to find
     *  a method of the specified name that may take the specified 
     *  message type.
     */   
    protected Method findDelegate( String name, Class messageType ) {
        // We do an exhaustive search because it's easier to 
        // check for a variety of parameter types, and it's all
        // that Class would be doing in getMethod() anyway.
        for( Method m : delegateType.getDeclaredMethods() ) {
                    
            if( !m.getName().equals(name) ) {
                continue;
            }                
 
            if( isValidMethod(m, messageType) ) {
                return m;
            }
        }
                       
        return null;        
    }
    
    /**
     *  Returns true if the specified method name is allowed.
     *  This is used by automapping to determine if a method
     *  should be rejected purely on name.  Default implementation
     *  always returns true.
     */
    protected boolean allowName( String name ) {
        return true;
    }
 
    /**
     *  Calls the map(Set) method with a null argument causing
     *  all available matching methods to mapped to message types.
     */    
    protected final void automap() {        
        map((Set<String>)null);
        if( methods.isEmpty() ) {
            throw new RuntimeException("No message handling methods found for class:" + delegateType);
        }
    }
 
    /**
     *  Specifically maps the specified methods names, autowiring
     *  the parameters.
     */
    public AbstractMessageDelegator<S> map( String... methodNames ) {
        Set<String> names = new HashSet<>( Arrays.asList(methodNames) );
        map(names);
        return this;
    }
 
    /**
     *  Goes through all of the delegate type's declared methods
     *  mapping methods that match the current constraints.
     *  If the constraints set is null then allowName() is
     *  checked for names otherwise only names in the constraints
     *  set are allowed.
     *  For each candidate method that passes the above checks,
     *  isValidMethod() is called with a null message type argument.
     *  All methods are made accessible thus supporting non-public
     *  methods as well as public methods.    
     */   
    protected void map( Set<String> constraints ) {
        
        if( log.isLoggable(Level.FINEST) ) {
            log.log(Level.FINEST, "map({0})", constraints);
        } 
        for( Method m : delegateType.getDeclaredMethods() ) {        
            if( log.isLoggable(Level.FINEST) ) {
                log.log(Level.FINEST, "Checking method:{0}", m);
            }

            if( constraints == null && !allowName(m.getName()) ) {
                log.finest("Name is not allowed.");
                continue;
            }
            if( constraints != null && !constraints.contains(m.getName()) ) {
                log.finest("Name is not in constraints set.");
                continue;
            }

            if( isValidMethod(m, null) ) {
                if( log.isLoggable(Level.FINEST) ) {
                    log.log(Level.FINEST, "Adding method mapping:{0} = {1}", new Object[]{getMessageType(m), m});
                }
                // Make sure we can access the method even if it's not public or
                // is in a non-public inner class.
                m.setAccessible(true);  
                methods.put(getMessageType(m), m);
            }            
        }
        
        messageTypes = null;        
    }
 
    /**
     *  Manually maps a specified method to the specified message type.
     */   
    public AbstractMessageDelegator<S> map( Class messageType, String methodName ) {
        // Lookup the method 
        Method m = findDelegate( methodName, messageType );
        if( m == null ) { 
            throw new RuntimeException( "Method:" + methodName 
                                        + " not found matching signature (MessageConnection, " 
                                        + messageType.getName() + ")" );
        }                                        
 
        if( log.isLoggable(Level.FINEST) ) {
            log.log(Level.FINEST, "Adding method mapping:{0} = {1}", new Object[]{messageType, m});
        }  
        methods.put( messageType, m );
        messageTypes = null;        
        return this;   
    }
    
    /**
     *  Returns the mapped method for the specified message type.
     */
    protected Method getMethod( Class c ) {
        Method m = methods.get(c);
        return m;
    }

    /**
     *  Implemented by subclasses to provide the actual delegate object
     *  against which the mapped message type methods will be called.
     */ 
    protected abstract Object getSourceDelegate( S source );

    /**
     *  Implementation of the MessageListener's messageReceived()
     *  method that will use the current message type mapping to
     *  find an appropriate message handling method and call it
     *  on the delegate returned by getSourceDelegate().
     */
    @Override
    public void messageReceived( S source, Message msg ) {
        if( msg == null ) {
            return;
        }
 
        Object delegate = getSourceDelegate(source);
        if( delegate == null ) {
            // Means ignore this message/source
            return;
        } 
            
        Method m = getMethod(msg.getClass());
        if( m == null ) {
            throw new RuntimeException("Delegate method not found for message class:" 
                                        + msg.getClass());
        }
 
        try {
            if( m.getParameterTypes().length > 1 ) {           
                m.invoke( delegate, source, msg );
            } else {
                m.invoke( delegate, msg );
            }
        } catch( IllegalAccessException e ) {
            throw new RuntimeException("Error executing:" + m, e);
        } catch( InvocationTargetException e ) {
            throw new RuntimeException("Error executing:" + m, e.getCause());
        }
    }
}



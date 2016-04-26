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
package com.jme3.input;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *  Provides compatibility mapping to different joysticks
 *  that both report their name in a unique way and require
 *  remapping to achieve a proper default layout.
 *
 *  <p>All mappings MUST be defined before the joystick support
 *  has been initialized in the InputManager.</p>
 *
 *  @author    Paul Speed
 */
public class JoystickCompatibilityMappings {

    private static final Logger logger = Logger.getLogger(JoystickCompatibilityMappings.class.getName());

    // List of resource paths to check for the joystick-mapping.properties
    // files.
    private static String[] searchPaths = { "joystick-mapping.properties" };  

    private static Map<String,Map<String,String>> joystickMappings = new HashMap<String,Map<String,String>>();

    static {
        loadDefaultMappings();
    }

    protected static Map<String,String> getMappings( String joystickName, boolean create ) {
        Map<String,String> result = joystickMappings.get(joystickName.trim());
        if( result == null && create ) {
            result = new HashMap<String,String>();
            joystickMappings.put(joystickName.trim(),result);
        }
        return result;          
    }
 
    /**
     *  Returns the remapped version of the axis/button name if there
     *  is a mapping for it otherwise it returns the original name.
     */
    public static String remapComponent( String joystickName, String componentId ) {
        Map<String,String> map = getMappings(joystickName.trim(), false);   
        if( map == null )
            return componentId;
        if( !map.containsKey(componentId) )
            return componentId;
        return map.get(componentId); 
    }       
 
    /**
     *  Returns a set of Joystick axis/button name remappings if they exist otherwise
     *  it returns an empty map.
     */
    public static Map<String,String> getJoystickMappings( String joystickName ) {
        Map<String,String> result = getMappings(joystickName.trim(), false);
        if( result == null )
            return Collections.emptyMap();
        return Collections.unmodifiableMap(result);
    }
    
    /**
     *  Adds a single Joystick axis or button remapping based on the 
     *  joystick's name and axis/button name.  The "remap" value will be
     *  used instead.
     */
    public static void addMapping( String stickName, String sourceComponentId, String remapId ) {
        logger.log(Level.FINE, "addMapping(" + stickName + ", " + sourceComponentId + ", " + remapId + ")" );        
        getMappings(stickName, true).put( sourceComponentId, remapId );
    } 
 
    /**
     *  Adds a preconfigured set of mappings in Properties object
     *  form where the names are dot notation "joystick"."axis/button"
     *  and the values are the remapped component name.  This calls
     *  addMapping(stickName, sourceComponent, remap) for every property
     *  that it is able to parse.
     */
    public static void addMappings( Properties p ) {
        for( Map.Entry<Object,Object> e : p.entrySet() ) {
            String key = String.valueOf(e.getKey()).trim();
            
            int split = key.lastIndexOf( '.' );
            if( split < 0 ) {
                logger.log(Level.WARNING, "Skipping mapping:{0}", e);
                continue;
            }
            
            String stick = key.substring(0, split).trim();
            String component = key.substring(split+1).trim();            
            String value = String.valueOf(e.getValue()).trim();
            addMapping(stick, component, value);           
        }
    }
 
    /**
     *  Loads a set of compatibility mappings from the property file
     *  specified by the given URL.
     */   
    public static void loadMappingProperties( URL u ) throws IOException {
        logger.log(Level.FINE, "Loading mapping properties:{0}", u);
        InputStream in = u.openStream();
        try {        
            Properties p = new Properties();
            p.load(in);
            addMappings(p);            
        } finally {
            in.close();
        } 
    }

    protected static void loadMappings( ClassLoader cl, String path ) throws IOException { 
        logger.log(Level.FINE, "Searching for mappings for path:{0}", path);
        for( Enumeration<URL> en = cl.getResources(path); en.hasMoreElements(); ) {            
            URL u = en.nextElement();
            try { 
                loadMappingProperties(u);
            } catch( IOException e ) {
                logger.log(Level.SEVERE, "Error loading:" + u, e);   
            }                        
        } 
           
    }

    /**
     *  Loads the default compatibility mappings by looking for
     *  joystick-mapping.properties files on the classpath.
     */
    protected static void loadDefaultMappings() {
        for( String s : searchPaths ) {
            try {            
                loadMappings(JoystickCompatibilityMappings.class.getClassLoader(), s);
            } catch( IOException e ) {
                logger.log(Level.SEVERE, "Error searching resource path:{0}", s);
            }
        }
    }     
}

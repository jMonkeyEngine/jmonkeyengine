/*
 * Copyright (c) 2009-2023 jMonkeyEngine
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
package com.jme3.plugins.json;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.RuntimeErrorException;

/**
  * A json parser factory that allows you to set the parser to use.
  * @author Riccardo Balbo
  */
 public class Json {
    public static final String PROPERTY_JSON_PARSER_IMPLEMENTATION = "com.jme3.JsonParserImplementation";
    private static final Logger LOGGER = Logger.getLogger(Json.class.getName());
    private static final String DEFAULT_JSON_PARSER_IMPLEMENTATION = "com.jme3.plugins.gson.GsonParser";

    private static Class<? extends JsonParser> clazz = null;

    /**
     * Set the parser to use.
     * @param clazz a class that implements JsonParser
     */
    public static void setParser(Class<? extends JsonParser> clazz) {
        Json.clazz = clazz;
    }


    /**
     * Create a new JsonParser instance.
     * @return a new JsonParser instance
     */
    @SuppressWarnings("unchecked")
    public static JsonParser create() {
        if (Json.clazz == null) {
            String className = System.getProperty(PROPERTY_JSON_PARSER_IMPLEMENTATION, DEFAULT_JSON_PARSER_IMPLEMENTATION);
            try {
                Json.clazz= (Class<JsonParser>) Class.forName(className);
            } catch (final Throwable e) {
                LOGGER.log(Level.WARNING, "Unable to access {0}", className);
                try{
                    Json.clazz = (Class<JsonParser>) Class.forName(DEFAULT_JSON_PARSER_IMPLEMENTATION);
                } catch (Throwable e2) {
                    throw new RuntimeException("Unable to access default json parser", e2);
                }
            }
        }
               

        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Could not instantiate JsonParser class " + clazz.getName(), e);
        }

    }
}

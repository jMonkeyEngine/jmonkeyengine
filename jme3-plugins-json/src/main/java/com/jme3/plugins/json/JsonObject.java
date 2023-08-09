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

import java.util.Map.Entry;

/**
 * A generic object/map
 * @author Riccardo Balbo
 */
public interface JsonObject extends JsonElement{

    /**
     * Returns the object property as a String
     * @param string name of the property
     * @return  the string
     */
    public JsonArray getAsJsonArray(String string) ;

    /**
     * Returns the object property as a JsonObject
     * @param string name of the property
     * @return the JsonObject
     */
    public JsonObject getAsJsonObject(String string);

    /**
     * Check if the object has a property
     * @param string name of the property
     * @return true if it exists, false otherwise
     */
    public boolean has(String string);

    /**
     * Returns the object property as generic element
     * @param string name of the property
     * @return the element
     */
    public JsonElement get(String string);

    /**
     * Returns the object's key-value pairs
     * @return an array of key-value pairs
     */
    public Entry<String, JsonElement>[] entrySet();

    /**
     * Returns the object property as a wrapped primitive
     * @param string name of the property
     * @return the wrapped primitive
     */
    public JsonPrimitive getAsJsonPrimitive(String string);

}

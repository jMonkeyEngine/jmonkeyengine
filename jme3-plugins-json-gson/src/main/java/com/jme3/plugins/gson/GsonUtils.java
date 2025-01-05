/*
 * Copyright (c) 2009-2025 jMonkeyEngine
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
package com.jme3.plugins.gson;

import com.jme3.plugins.gson.internal.WrapperIterator;
import com.jme3.plugins.json.JsonElement;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Utility class responsible for wrapping the objects extracted from the 
 * <a href="https://github.com/google/gson">GSON</a> library and providing useful 
 * methods for the implementation of module <strong>jme3-plugins-json</strong>.
 * 
 * @author wil
 */
final class GsonUtils {
    
    /**
     * Method responsible for wrapping an iterator with GSON elements to a new 
     * iterator with Json element ({@link com.jme3.plugins.json.JsonElement}).
     * 
     * @param it gson iterator
     * @return the new json iterator
     */
    public static Iterator<JsonElement> wrap(Iterator<com.google.gson.JsonElement> it) {
        return new WrapperIterator<>((com.google.gson.JsonElement object) -> GsonUtils.wrap(object), it);
    }
    
    /**
     * Method in charge of wrapping a {@code Set} with GSON elements to a new {@code Set} 
     * with Json elements ({@link com.jme3.plugins.json.JsonElement}).
     * 
     * @param entry gson Set
     * @return the new json Set
     */
    public static Set<Map.Entry<String, JsonElement>> wrap(Set<Map.Entry<String, com.google.gson.JsonElement>> entry) {
        return new AbstractSet<Map.Entry<String, JsonElement>>() {
            @Override
            public Iterator<Map.Entry<String, JsonElement>> iterator() {
                return new WrapperIterator<>((Map.Entry<String, com.google.gson.JsonElement> object) -> new Map.Entry<String, JsonElement>() {
                    @Override
                    public String getKey() {
                        return object.getKey();
                    }                    
                    @Override
                    public JsonElement getValue() {
                        return GsonUtils.wrap(object.getValue());
                    }                    
                    @Override
                    public JsonElement setValue(JsonElement v) {
                        throw new UnsupportedOperationException("Not supported yet.");
                    }}, entry.iterator());
            }

            @Override
            public int size() {
                return entry.size();
            }
        };
    }
    
    /**
     * Method responsible for wrapping GSON elements to a new jme3 Json element {@link JsonElement}.
     * 
     * @param <T> the type of element
     * @param element the gson element
     * 
     * @return the new element
     */
    @SuppressWarnings("unchecked")
    public static <T extends JsonElement> T wrap(com.google.gson.JsonElement element) {
        if (element == null) {
            return null;
        }
        if (element.isJsonNull()) {
            return (T) GsonNull.NULL;
        } else if (element.isJsonArray()) {
            return (T) new GsonArray(element.getAsJsonArray());
        } else if (element.isJsonObject()) {
            return (T) new GsonObject(element.getAsJsonObject());
        }
        return (T) new GsonPrimitive(element.getAsJsonPrimitive());
    }
    
    private GsonUtils() {}
}

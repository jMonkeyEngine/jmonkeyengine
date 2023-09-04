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
package com.jme3.plugins.gson;

import java.util.Set;
import java.util.Map.Entry;

import com.jme3.plugins.json.JsonArray;
import com.jme3.plugins.json.JsonElement;
import com.jme3.plugins.json.JsonObject;
import com.jme3.plugins.json.JsonPrimitive;

/**
 * GSON implementation of {@link JsonObject}
 */
class GsonObject extends GsonElement implements JsonObject {

    GsonObject(com.google.gson.JsonObject gsonObject) {
        super(gsonObject);
    }
    
    private com.google.gson.JsonObject obj() {
        return (com.google.gson.JsonObject) element;
    }

    @Override
    public JsonArray getAsJsonArray(String string) {
        com.google.gson.JsonArray el = obj().getAsJsonArray(string);
        return isNull(el) ? null : new GsonArray(el);        
    }

    @Override
    public JsonObject getAsJsonObject(String string) {
        com.google.gson.JsonObject el = obj().getAsJsonObject(string);
        return isNull(el) ? null : new GsonObject(el);
    }

    @Override
    public boolean has(String string) {
        return obj().has(string);
    }

    @Override
    public JsonElement get(String string) {
        com.google.gson.JsonElement el = obj().get(string);
        return isNull(el)?null:new GsonElement(el).autoCast();
    }

    @Override
    public Entry<String, JsonElement>[] entrySet() {
        Set<Entry<String, com.google.gson.JsonElement>> entrySet = obj().entrySet();
        Entry<String, JsonElement>[] entries = new Entry[entrySet.size()];
        int i = 0;
        for (Entry<String, com.google.gson.JsonElement> entry : entrySet) {

            Entry<String, JsonElement> e = new Entry<String, JsonElement>() {
                @Override
                public String getKey() {
                    return entry.getKey();
                }

                @Override
                public GsonElement getValue() {
                    return new GsonElement(entry.getValue()).autoCast();
                }

                @Override
                public GsonElement setValue(JsonElement value) {
                    throw new UnsupportedOperationException("Unimplemented method 'setValue'");
                }
            };

            entries[i++] = e;
        }
        return entries;

    }

    @Override
    public JsonPrimitive getAsJsonPrimitive(String string) {
        com.google.gson.JsonPrimitive el= obj().getAsJsonPrimitive(string);
        return isNull(el) ? null : new GsonPrimitive(el);
    }
}
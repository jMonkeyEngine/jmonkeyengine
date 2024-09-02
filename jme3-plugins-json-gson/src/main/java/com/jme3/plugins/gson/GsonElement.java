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

import com.jme3.plugins.json.JsonArray;
import com.jme3.plugins.json.JsonElement;
import com.jme3.plugins.json.JsonObject;
import com.jme3.plugins.json.JsonPrimitive;

import java.util.Objects;

/**
 * GSON implementation of {@link JsonElement}
 */
class GsonElement implements JsonElement {
    com.google.gson.JsonElement element;

    GsonElement(com.google.gson.JsonElement element) {
        this.element = element;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.element);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof GsonElement) {
            GsonElement other = (GsonElement) obj;
            return element.equals(other.element);
        }
        return false;
    }
    
    protected boolean isNull(com.google.gson.JsonElement element) {
        if (element == null) return true;
        if (element.isJsonNull()) return true;
        return false;
    }
    
    @Override
    public String getAsString() {
        return element.getAsString();
    }

    @Override
    public JsonObject getAsJsonObject() {
        return new GsonObject(element.getAsJsonObject());
    }

    @Override
    public float getAsFloat() {
        return element.getAsFloat();
    }

    @Override
    public int getAsInt() {
        return element.getAsInt();
    }

    @Override
    public Number getAsNumber() {
        return element.getAsNumber();        
    }

    @Override
    public boolean getAsBoolean() {
        return element.getAsBoolean();
    }

    @Override
    public JsonArray getAsJsonArray() {
        return new GsonArray(element.getAsJsonArray());
    }

    @Override
    public JsonPrimitive getAsJsonPrimitive() {
        return new GsonPrimitive(element.getAsJsonPrimitive());
    }

    @SuppressWarnings("unchecked")
    public <T extends JsonElement> T autoCast() {
        if(isNull(element)) return null;
        if (element.isJsonArray()) return (T) new GsonArray(element.getAsJsonArray());
        if (element.isJsonObject()) return (T) new GsonObject(element.getAsJsonObject());
        if (element.isJsonPrimitive()) return (T) new GsonPrimitive(element.getAsJsonPrimitive());
        return (T) new GsonElement(element);
    }
    
}

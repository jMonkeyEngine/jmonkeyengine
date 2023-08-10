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

import java.util.Iterator;

import com.jme3.plugins.json.JsonArray;
import com.jme3.plugins.json.JsonElement;

/**
 * GSON implementation of {@link JsonArray}.
 */
class GsonArray extends GsonElement implements JsonArray {

    GsonArray(com.google.gson.JsonElement element) {
        super(element);
    }

    private com.google.gson.JsonArray arr() {
        return element.getAsJsonArray();
    }

    @Override
    public Iterator<JsonElement> iterator() {
        return new Iterator<JsonElement>() {
            Iterator<com.google.gson.JsonElement> it = arr().iterator();

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public JsonElement next() {
                return new GsonElement(it.next());
            }
        };
    }

    @Override
    public JsonElement get(int i) {
        com.google.gson.JsonElement el = arr().get(i);
        return el == null ? null : new GsonElement(el);
    }

    @Override
    public int size() {
        return arr().size();
    }

}

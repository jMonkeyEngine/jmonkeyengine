/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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
package com.jme3.util;

import java.util.Map.Entry;
import org.junit.Test;

/**
 * Check if the {@link ListMap} class is working correctly.
 * 
 * @author Kirill Vainer
 */
public class ListMapTest {

    @Test
    public void testListMap() {
        ListMap<String, String> listMap = new ListMap<>();
        listMap.put("bob", "hello");
        assert "hello".equals(listMap.get("bob"));
        assert "hello".equals(listMap.remove("bob"));
        assert listMap.size() == 0;
        assert listMap.isEmpty();

        listMap.put("abc", "1");
        listMap.put("def", "2");
        listMap.put("ghi", "3");
        listMap.put("jkl", "4");
        listMap.put("mno", "5");
        assert "3".equals(listMap.get("ghi"));
        assert listMap.size() == 5;
        assert !listMap.isEmpty();

        // check iteration order, should be consistent
        for (int i = 0; i < listMap.size(); i++) {
            String expectedValue = Integer.toString(i + 1);
            String key = listMap.getKey(i);
            String value = listMap.getValue(i);
            Entry<String, String> entry = listMap.getEntry(i);
            assert key.equals(entry.getKey());
            assert value.equals(entry.getValue());
            assert expectedValue.equals(value);
        }
    }
}

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
package com.jme3.export.binary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

class BinaryClassObject {

    // nameFields holds the first alias for each field name; the remaining aliases
    // are needed when a dynamic Savable writes that name with another type.
    HashMap<String, BinaryClassField> nameFields;
    HashMap<Byte, BinaryClassField> aliasFields;
    final ArrayList<BinaryClassField> additionalFields = new ArrayList<>();
    final HashSet<String> fieldNamesWithMultipleAliases = new HashSet<>();

    BinaryClassField getField(String name, byte type) {
        BinaryClassField field = nameFields.get(name);
        if (field != null && field.type == type) {
            return field;
        }
        for (BinaryClassField additionalField : additionalFields) {
            if (additionalField.name.equals(name) && additionalField.type == type) {
                return additionalField;
            }
        }
        return null;
    }

    BinaryClassField addField(String name, byte type) {
        // Field aliases are stored as one byte in the binary format.
        byte fieldAlias = (byte) (nameFields.size() + additionalFields.size());
        BinaryClassField field = new BinaryClassField(name, fieldAlias, type);
        if (nameFields.containsKey(name)) {
            additionalFields.add(field);
            fieldNamesWithMultipleAliases.add(name);
        } else {
            nameFields.put(name, field);
        }
        return field;
    }

    int getFieldCount() {
        return nameFields.size() + additionalFields.size();
    }

    ArrayList<BinaryClassField> getFields() {
        ArrayList<BinaryClassField> fields = new ArrayList<>(getFieldCount());
        fields.addAll(nameFields.values());
        fields.addAll(additionalFields);
        return fields;
    }
    
    byte[] alias;
    String className;
    int[] classHierarchyVersions;
}

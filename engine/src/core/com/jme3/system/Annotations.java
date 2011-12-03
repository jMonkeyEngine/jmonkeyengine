/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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
package com.jme3.system;

import checkers.quals.TypeQualifier;
import java.lang.annotation.*;

/**
 * This class contains the Annotation definitions for jME3. Mostly these are used
 * for code error checking.
 * @author normenhansen
 */
public class Annotations {

    /**
     * Annotation used for math primitive fields, method parameters or method return values.
     * Specifies that the primitve is read only and should not be changed.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @TypeQualifier
    @Target({ElementType.FIELD, ElementType.LOCAL_VARIABLE, ElementType.TYPE, ElementType.METHOD})
    public @interface ReadOnly {
    }

    /**
     * Annotation used for methods in math primitives that are destructive to the
     * object (xxxLocal, setXXX etc.).
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @TypeQualifier
    @Target({ElementType.METHOD})
    public @interface Destructive {
    }

    /**
     * Annotation used for public methods that are not to be called by users.
     * Examples include update() methods etc.
     */
    public @interface Internal {
    }
}

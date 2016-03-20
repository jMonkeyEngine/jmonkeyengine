/*
 * Copyright (c) 2016 jMonkeyEngine
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

package com.jme3.util.clone;


/**
 *  Provides custom cloning for a particular object type.  Once
 *  registered with the Cloner, this function object will be called twice
 *  for any cloned object that matches the class for which it was registered.
 *  It will first call cloneObject() to shallow clone the object and then call
 *  cloneFields()  to deep clone the object's values.
 *
 *  <p>This two step process is important because this is what allows
 *  circular references in the cloned object graph.</p>
 *
 *  @author    Paul Speed
 */
public interface CloneFunction<T> {

    /**
     *  Performs a shallow clone of the specified object.  This is similar
     *  to the JmeCloneable.clone() method in semantics and is the first part
     *  of a two part cloning process.  Once the shallow clone is created, it
     *  is cached and CloneFunction.cloneFields() is called.  In this way, 
     *  the CloneFunction interface can completely take over the JmeCloneable
     *  style cloning for an object that doesn't otherwise implement that interface.
     *
     *  @param cloner The cloner performing the cloning operation.
     *  @param original The original object that needs to be cloned.
     */
    public T cloneObject( Cloner cloner, T original );
 
 
    /**
     *  Performs a deep clone of the specified clone's fields.  This is similar
     *  to the JmeCloneable.cloneFields() method in semantics and is the second part
     *  of a two part cloning process.  Once the shallow clone is created, it
     *  is cached and CloneFunction.cloneFields() is called.  In this way, 
     *  the CloneFunction interface can completely take over the JmeCloneable
     *  style cloning for an object that doesn't otherwise implement that interface.
     * 
     *  @param cloner The cloner performing the cloning operation.
     *  @param clone The clone previously returned from cloneObject().
     *  @param original The original object that was cloned.  This is provided for
     *              the very special case where field cloning needs to refer to
     *              the original object.  Mostly the necessary fields should already
     *              be on the clone.
     */
    public void cloneFields( Cloner cloner, T clone, T original );
     
}

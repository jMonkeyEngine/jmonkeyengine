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

import java.util.List;

/**
 *  A CloneFunction implementation that deep clones a list by
 *  creating a new list and cloning its values using the cloner.
 *
 *  @author    Paul Speed
 */
public class ListCloneFunction<T extends List> implements CloneFunction<T> {

    public T cloneObject( Cloner cloner, T object ) {         
        try {
            T clone = cloner.javaClone(object);         
            return clone;
        } catch( CloneNotSupportedException e ) {
            throw new IllegalArgumentException("Clone not supported for type:" + object.getClass(), e);
        }
    }
     
    /**
     *  Clones the elements of the list.
     */    
    @SuppressWarnings("unchecked")
    public void cloneFields( Cloner cloner, T clone, T object ) {
        for( int i = 0; i < clone.size(); i++ ) {
            // Need to clone the clones... because T might
            // have done something special in its clone method that
            // we will have to adhere to.  For example, clone may have nulled
            // out some things or whatever that might be implementation specific.
            // At any rate, if it's a proper clone then the clone will already
            // have shallow versions of the elements that we can clone.
            clone.set(i, cloner.clone(clone.get(i)));
        }
    }
}


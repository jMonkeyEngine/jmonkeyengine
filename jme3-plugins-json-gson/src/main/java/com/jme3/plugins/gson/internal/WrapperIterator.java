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
package com.jme3.plugins.gson.internal;

import java.util.Iterator;

/**
 * A class that can be used as a wrapper for an iterator, where data can be exchanged
 * (wrapped) from the original iterator to a new one using the {@link Wrapper} interface.
 * 
 * @param <A> the original type of the iteraro
 * @param <B> the wrapper type for this iterator
 * 
 * @author wil
 */
public class WrapperIterator<A, B> implements Iterator<B> {

    /** The object in charge of carrying out the exchange (wrapping the objects). */
    private final Wrapper<A, B> wrapper;
    /** The original iterator. */
    private final Iterator<A> it;

    /**
     * Generate a new object <code>WrapperIterator</code>.
     * 
     * @param wrapper object wrapping
     * @param it original iteraro
     */
    public WrapperIterator(Wrapper<A, B> wrapper, Iterator<A> it) {
        this.wrapper = wrapper;
        this.it = it;
    }
    
    /* (non-Javadoc)
     * @see java.util.Iterator#hasNext() 
     */
    @Override
    public boolean hasNext() {
        return it.hasNext();
    }

    /* (non-Javadoc)
     * @see java.util.Iterator#next() 
     */
    @Override
    public B next() {
        return wrapper.wrap(it.next());
    }
}

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
package com.jme3.network;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

/**
 *  Static utility methods pertaining to Filter instances.
 *
 *  @version   $Revision$
 *  @author    Paul Speed
 */
public class Filters 
{
    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private Filters() {
    }

    /**
     *  Creates a filter that returns true for any value in the specified
     *  list of values and false for all other cases.
     */
    @SuppressWarnings("unchecked")
    public static <T> Filter<T> in( T... values )
    {
        return in( new HashSet<T>(Arrays.asList(values)) );
    }
    
    /**
     *  Creates a filter that returns true for any value in the specified
     *  collection and false for all other cases.
     */
    public static <T> Filter<T> in( Collection<? extends T> collection )
    {
        return new InFilter<T>(collection);
    }

    /**
     *  Creates a filter that returns true for any value NOT in the specified
     *  list of values and false for all other cases.  This is the equivalent
     *  of calling not(in(values)).
     */
    @SuppressWarnings("unchecked")
    public static <T> Filter<T> notIn( T... values )
    {
        return not( in( values ) );
    }
    
    /**
     *  Creates a filter that returns true for any value NOT in the specified
     *  collection and false for all other cases.  This is the equivalent
     *  of calling not(in(collection)).
     */
    public static <T> Filter<T> notIn( Collection<? extends T> collection )
    {
        return not( in( collection ) );
    }
    
    /**
     *  Creates a filter that returns true for inputs that are .equals()
     *  equivalent to the specified value.
     */
    public static <T> Filter<T> equalTo( T value )
    {
        return new EqualToFilter<T>(value); 
    }     

    /**
     *  Creates a filter that returns true for inputs that are NOT .equals()
     *  equivalent to the specified value.  This is the equivalent of calling
     *  not(equalTo(value)).
     */
    public static <T> Filter<T> notEqualTo( T value )
    {
        return not(equalTo(value));
    }     

    /**
     *  Creates a filter that returns true when the specified delegate filter
     *  returns false, and vice versa.
     */
    public static <T> Filter<T> not( Filter<T> f )
    {
        return new NotFilter<T>(f);
    }
 
    private static class EqualToFilter<T> implements Filter<T>
    {
        private T value;
        
        public EqualToFilter( T value )
        {
            this.value = value;
        }
        
        @Override
        public boolean apply( T input )
        {
            return value == input || (value != null && value.equals(input));
        }
    }
    
    private static class InFilter<T> implements Filter<T>
    {
        private Collection<? extends T> collection;
        
        public InFilter( Collection<? extends T> collection )
        {
            this.collection = collection;
        }
        
        @Override
        public boolean apply( T input )
        {
            return collection.contains(input);
        } 
    }
    
    private static class NotFilter<T> implements Filter<T>
    {
        private Filter<T> delegate;
        
        public NotFilter( Filter<T> delegate )
        {
            this.delegate = delegate;
        }
        
        @Override
        public boolean apply( T input )
        {
            return !delegate.apply(input);
        }
    } 
}



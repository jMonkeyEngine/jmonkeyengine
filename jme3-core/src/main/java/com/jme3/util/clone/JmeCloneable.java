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
 *  Indicates an object that wishes to more actively participate in the
 *  two-part deep copying process provided by the Cloner.  Objects implementing
 *  this interface can access the already cloned object graph to resolve
 *  their local dependencies in a way that will be equivalent to the
 *  original object graph.  In other words, if two objects in the graph
 *  share the same target reference then the cloned version will share
 *  the cloned reference. 
 *  
 *  <p>For example, if an object wishes to deep clone one of its fields
 *  then it will call cloner.clone(object) instead of object.clone().
 *  The cloner will keep track of any clones already created for 'object'
 *  and return that instead of a new clone.</p>
 *
 *  <p>Cloning of a JmeCloneable object is done in two parts.  First,
 *  the standard Java clone() method is called to create a shallow clone
 *  of the object.  Second, the cloner will call the cloneFields() method
 *  to let the object deep clone any of its fields that should be cloned.</p>
 *
 *  <p>This two part process is necessary to facilitate circular references.
 *  When an object calls cloner.clone() during its cloneFields() method, it
 *  may get only a shallow copy that will be filled in later.</p>
 *
 *  @author    Paul Speed
 */
public interface JmeCloneable extends Cloneable {

    /**
     *  Performs a regular shallow clone of the object.  Some fields
     *  may also be cloned but generally only if they will never be
     *  shared with other objects.  (For example, local Vector3fs and so on.)
     *
     *  <p>This method is separate from the regular clone() method
     *  so that objects might still maintain their own regular java clone()
     *  semantics (perhaps even using Cloner for those methods).  However,
     *  because Java's clone() has specific features in the sense of Object's
     *  clone() implementation, it's usually best to have some path for
     *  subclasses to bypass the public clone() method that might be cloning
     *  fields and instead get at the superclass protected clone() methods.
     *  For example, through super.jmeClone() or another protected clone
     *  method that some base class eventually calls super.clone() in.</p>
     */
    public Object jmeClone();     

    /**
     *  Implemented to perform deep cloning for this object, resolving
     *  local cloned references using the specified cloner.  The object
     *  can call cloner.clone(fieldValue) to deep clone any of its fields.
     * 
     *  <p>Note: during normal clone operations the original object
     *  will not be needed as the clone has already had all of the fields
     *  shallow copied.</p>
     *
     *  @param cloner The cloner that is performing the cloning operation.  The 
     *              cloneFields method can call back into the cloner to make
     *              clones if its subordinate fields.     
     *  @param original The original object from which this object was cloned.
     *              This is provided for the very rare case that this object needs
     *              to refer to its original for some reason.  In general, all of
     *              the relevant values should have been transferred during the
     *              shallow clone and this object need merely clone what it wants.
     */
    public void cloneFields( Cloner cloner, Object original ); 
}

/*
 * Copyright (c) 2009-2014 jMonkeyEngine
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
package com.jme3.collision;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * <code>DummyCollisionResults</code> is an empty "dummy" collection returned as a result of a 
 * collision detection operation done by {@link Collidable}.
 * Its purpose is to avoid allocation of CollisionResults and its content, 
 * when collisionResults are out of our interest.
 * Using the same API Collidable.collideWith() instead of possibly some new countCollideWith() implementation
 * is chosen to keep the collision detection code "DRY", @see <a href="http://en.wikipedia.org/wiki/Don%27t_repeat_yourself">Don't repeat yourself</a>
 * 
 * @author Bebul
 */
public class DummyCollisionResults extends CollisionResults {
  // the singleton for DummyCollisionResults is sufficient
  private static DummyCollisionResults instance = null; 
  
  // exists only to defeat instantiation
  protected DummyCollisionResults() {}
  
  // classic singleton instance
  public static DummyCollisionResults getInstance() {
    if ( instance == null ) {
      instance = new DummyCollisionResults();
    }
    return instance;
  }
  
  // Dummy implementation of CollisionResults
  // All methods can be dummy except for iterator
  @Override public void clear(){}
  // even dummy iterator should return valid iterator
  @Override public Iterator<CollisionResult> iterator() {
    List<CollisionResult> dumbCompiler = Collections.emptyList();            
    return dumbCompiler.iterator();
  }
  @Override public void addCollision(CollisionResult result){}
  @Override public int size(){ return 0; }
  @Override public CollisionResult getClosestCollision(){ return null; }
  @Override public CollisionResult getFarthestCollision(){ return null; }
  @Override public CollisionResult getCollision(int index){ return null; }
  @Override public CollisionResult getCollisionDirect(int index){ return null; }
}

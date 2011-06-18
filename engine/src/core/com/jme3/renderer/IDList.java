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

package com.jme3.renderer;

import java.util.Arrays;

/**
 * A specialized data-structure used to optimize state changes of "slot"
 * based state. 
 */
public class IDList {

    public int[] newList = new int[16];
    public int[] oldList = new int[16];
    public int newLen = 0;
    public int oldLen = 0;

    /**
     * Reset all states to zero
     */
    public void reset(){
        newLen = 0;
        oldLen = 0;
        Arrays.fill(newList, 0);
        Arrays.fill(oldList, 0);
    }

    /**
     * Adds an index to the new list.
     * If the index was not in the old list, false is returned,
     * if the index was in the old list, it is removed from the old
     * list and true is returned.
     * 
     * @param idx The index to move
     * @return True if it existed in old list and was removed
     * from there, false otherwise.
     */
    public boolean moveToNew(int idx){
        if (newLen == 0 || newList[newLen-1] != idx)
            // add item to newList first
            newList[newLen++] = idx;

        // find idx in oldList, if removed successfuly, return true.
        for (int i = 0; i < oldLen; i++){
            if (oldList[i] == idx){
                // found index in slot i
                // delete index from old list
                oldLen --;
                for (int j = i; j < oldLen; j++){
                    oldList[j] = oldList[j+1];
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Copies the new list to the old list, and clears the new list.
     */
    public void copyNewToOld(){
        System.arraycopy(newList, 0, oldList, 0, newLen);
        oldLen = newLen;
        newLen = 0;
    }

    /**
     * Prints the contents of the lists
     */
    public void print(){
        if (newLen > 0){
            System.out.print("New List: ");
            for (int i = 0; i < newLen; i++){
                if (i == newLen -1)
                    System.out.println(newList[i]);
                else
                    System.out.print(newList[i]+", ");
            }
        }
        if (oldLen > 0){
            System.out.print("Old List: ");
            for (int i = 0; i < oldLen; i++){
                if (i == oldLen -1)
                    System.out.println(oldList[i]);
                else
                    System.out.print(oldList[i]+", ");
            }
        }
    }

}

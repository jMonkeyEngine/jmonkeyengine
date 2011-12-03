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

package jme3test.app;

import com.jme3.renderer.IDList;
import java.util.*;

public class TestIDList {

    static class StateCol {

        static Random rand = new Random();

        Map<Integer, Object> objs = new HashMap<Integer, Object>();

        public StateCol(){
            // populate with free ids
            List<Integer> freeIds = new ArrayList();
            for (int i = 0; i < 16; i++){
                freeIds.add(i);
            }

            // create random
            int numStates = rand.nextInt(6) + 1;
            for (int i = 0; i < numStates; i++){
                // remove a random id from free id list
                int idx = rand.nextInt(freeIds.size());
                int id = freeIds.remove(idx);

                objs.put(id, new Object());
            }
        }

        public void print(){
            System.out.println("-----------------");

            Set<Integer> keys = objs.keySet();
            Integer[] keysArr = keys.toArray(new Integer[0]);
            Arrays.sort(keysArr);
            for (int i = 0; i < keysArr.length; i++){
                System.out.println(keysArr[i]+" => "+objs.get(keysArr[i]).hashCode());
            }
        }

    }

    static IDList list = new IDList();
    static int boundSlot = 0;
    
    static Object[] slots = new Object[16];
    static boolean[] enabledSlots = new boolean[16];

    static void enable(int slot){
        System.out.println("Enabled SLOT["+slot+"]");
        if (enabledSlots[slot] == true){
            System.err.println("FAIL! Extra state change");
        }
        enabledSlots[slot] = true;
    }

    static void disable(int slot){
        System.out.println("Disabled SLOT["+slot+"]");
        if (enabledSlots[slot] == false){
            System.err.println("FAIL! Extra state change");
        }
        enabledSlots[slot] = false;
    }

    static void setSlot(int slot, Object val){
        if (!list.moveToNew(slot)){
            enable(slot);
        }
        if (slots[slot] != val){
            System.out.println("SLOT["+slot+"] = "+val.hashCode());
            slots[slot] = val;
        }
    }

    static void checkSlots(StateCol state){
        for (int i = 0; i < 16; i++){
            if (slots[i] != null && enabledSlots[i] == false){
                System.err.println("FAIL! SLOT["+i+"] assigned, but disabled");
            }
            if (slots[i] == null && enabledSlots[i] == true){
                System.err.println("FAIL! SLOT["+i+"] enabled, but not assigned");
            }

            Object val = state.objs.get(i);
            if (val != null){
                if (slots[i] != val)
                    System.err.println("FAIL! SLOT["+i+"] does not contain correct value");
                if (!enabledSlots[i])
                    System.err.println("FAIL! SLOT["+i+"] is not enabled");
            }else{
                if (slots[i] != null)
                    System.err.println("FAIL! SLOT["+i+"] is not set");
                if (enabledSlots[i])
                    System.err.println("FAIL! SLOT["+i+"] is enabled");
            }
        }
    }

    static void clearSlots(){
        for (int i = 0; i < list.oldLen; i++){
            int slot = list.oldList[i];
            disable(slot);
            slots[slot] = null;
        }
        list.copyNewToOld();
//        context.attribIndexList.print();
    }
    
    static void setState(StateCol state){
        state.print();
        for (Map.Entry<Integer, Object> entry : state.objs.entrySet()){
            setSlot(entry.getKey(), entry.getValue());
        }
        clearSlots();
        checkSlots(state);
    }

    public static void main(String[] args){
        StateCol[] states = new StateCol[20];
        for (int i = 0; i < states.length; i++)
            states[i] = new StateCol();

        // shuffle would be useful here..

        for (int i = 0; i < states.length; i++){
            setState(states[i]);
        }
    }

}

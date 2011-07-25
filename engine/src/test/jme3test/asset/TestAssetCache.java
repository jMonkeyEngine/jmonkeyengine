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

package jme3test.asset;

import com.jme3.asset.Asset;
import com.jme3.asset.AssetCache;
import com.jme3.asset.AssetKey;
import java.util.ArrayList;
import java.util.List;

public class TestAssetCache {
    
    /**
     * Keep references to loaded assets
     */
    private final static boolean KEEP_REFERENCES = false;
    
    /**
     * Enable smart cache use
     */
    private final static boolean USE_SMART_CACHE = true;
    
    /**
     * Enable cloneable asset use
     */
    private final static boolean CLONEABLE_ASSET = true;

    private static int counter = 0;
    
    private static class DummyData implements Asset {

        private AssetKey key;
        private byte[] data = new byte[10000];

        public byte[] getData(){
            return data;
        }
        
        public AssetKey getKey() {
            return key;
        }

        public void setKey(AssetKey key) {
            this.key = key;
        }
    }
    
    private static class SmartKey extends AssetKey {
        
        public SmartKey(){
            super(".");
            counter++;
        }
        
        @Override
        public int hashCode(){
            return 0;
        }
        
        @Override
        public boolean equals(Object other){
            return false;
        }
        
        @Override
        public boolean useSmartCache(){
            return true;
        }
        
        @Override
        public Object createClonedInstance(Object asset){
            DummyData data = new DummyData();
            return data;
        }
    }
    
    private static class DumbKey extends AssetKey {
        
        public DumbKey(){
            super(".");
            counter++;
        }
        
        @Override
        public int hashCode(){
            return 0;
        }
        
        @Override
        public boolean equals(Object other){
            return false;
        }
        
        @Override
        public Object createClonedInstance(Object asset){
            if (CLONEABLE_ASSET){
                DummyData data = new DummyData();
                return data;
            }else{
                return asset;
            }
        }
    }
    
    public static void main(String[] args){
        List<Object> refs = new ArrayList<Object>(5000);
        
        AssetCache cache = new AssetCache();
        
        System.gc();
        System.gc();
        System.gc();
        System.gc();
        
        long memory = Runtime.getRuntime().freeMemory();
        
        while (true){
            AssetKey key;
            
            if (USE_SMART_CACHE){
                key = new SmartKey();
            }else{
                key = new DumbKey();
            }
            
            DummyData data = new DummyData();
            cache.addToCache(key, data);
            
            if (KEEP_REFERENCES){
                refs.add(data);
            }
            
            if ((counter % 100) == 0){
                long newMem = Runtime.getRuntime().freeMemory();
                System.out.println("Allocated objects: " + counter);
                System.out.println("Allocated memory: " + ((memory - newMem)/1024) + "K" );
                memory = newMem;
            }
        }
    }
}

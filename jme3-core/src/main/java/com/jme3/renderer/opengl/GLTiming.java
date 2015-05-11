/*
 * Copyright (c) 2009-2015 jMonkeyEngine
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
package com.jme3.renderer.opengl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

public class GLTiming implements InvocationHandler {
    
    private final Object obj;
    private final GLTimingState state;
    
    public GLTiming(Object obj, GLTimingState state) {
        this.obj = obj;
        this.state = state;
    }
    
    public static Object createGLTiming(Object glInterface, GLTimingState state, Class<?> ... glInterfaceClasses) {
        return Proxy.newProxyInstance(glInterface.getClass().getClassLoader(),
                                      glInterfaceClasses, 
                                      new GLTiming(glInterface, state));
    }
    
    private static class CallTimingComparator implements Comparator<Map.Entry<String, Long>> {
        @Override
        public int compare(Map.Entry<String, Long> o1, Map.Entry<String, Long> o2) {
            return (int) (o2.getValue() - o1.getValue());
        }
    }
    
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        if (methodName.equals("resetStats")) {
            if (state.lastPrintOutTime + 1000000000 <= System.nanoTime() && state.sampleCount > 0) {
                state.timeSpentInGL /= state.sampleCount;
                System.out.println("--- TOTAL TIME SPENT IN GL CALLS: " + (state.timeSpentInGL/1000) + "us");
                
                Map.Entry<String, Long>[] callTimes = new Map.Entry[state.callTiming.size()];
                int i = 0;
                for (Map.Entry<String, Long> callTime : state.callTiming.entrySet()) {
                    callTimes[i++] = callTime;
                }
                Arrays.sort(callTimes, new CallTimingComparator());
                int limit = 10;
                for (Map.Entry<String, Long> callTime : callTimes) {
                    long val = callTime.getValue() / state.sampleCount;
                    String name = callTime.getKey();
                    String pad = "                                     ".substring(0, 30 - name.length());
                    System.out.println("\t" + callTime.getKey() + pad + (val/1000) + "us");
                    if (limit-- == 0) break;
                }
                for (Map.Entry<String, Long> callTime : callTimes) {
                    state.callTiming.put(callTime.getKey(), Long.valueOf(0));
                }
                
                state.sampleCount = 0;
                state.timeSpentInGL = 0;
                state.lastPrintOutTime = System.nanoTime();
            } else {
                state.sampleCount++;
            }
            return null;
        } else {
            Long currentTimeObj = state.callTiming.get(methodName);
            long currentTime = 0;
            if (currentTimeObj != null) currentTime = currentTimeObj;
            
            
            long startTime = System.nanoTime();
            Object result = method.invoke(obj, args);
            long delta = System.nanoTime() - startTime;
            
            currentTime += delta;
            state.timeSpentInGL += delta;
            
            state.callTiming.put(methodName, currentTime);
            
            if (delta > 1000000 && !methodName.equals("glClear")) {
                // More than 1ms
                // Ignore glClear as it cannot be avoided.
                System.out.println("GL call " + methodName + " took " + (delta/1000) + "us to execute!");
            }
            
            return result;
        }
    }
    
}

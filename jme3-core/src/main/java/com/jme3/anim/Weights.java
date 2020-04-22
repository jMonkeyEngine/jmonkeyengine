/*
 * Copyright (c) 2009-2020 jMonkeyEngine
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
package com.jme3.anim;

import java.util.ArrayList;

public class Weights {//}  extends Savable, JmeCloneable{


    private final static float MIN_WEIGHT = 0.005f;

    private int[] indices;
    private float[] data;
    private int size;

    public Weights(float[] array, int start, int length) {
        ArrayList<Float> list = new ArrayList<>();
        ArrayList<Integer> idx = new ArrayList<>();

        for (int i = start; i < length; i++) {
            float val = array[i];
            if (val > MIN_WEIGHT) {
                list.add(val);
                idx.add(i);
            }
        }
        size = list.size();
        data = new float[size];
        indices = new int[size];
        for (int i = 0; i < size; i++) {
            data[i] = list.get(i);
            indices[i] = idx.get(i);
        }
    }

    public int getSize() {
        return size;
    }

    //    public Weights(float[] array, int start, int length) {
//        LinkedList<Float> list = new LinkedList<>();
//        LinkedList<Integer> idx = new LinkedList<>();
//        for (int i = start; i < length; i++) {
//            float val = array[i];
//            if (val > MIN_WEIGHT) {
//                int index = insert(list, val);
//                if (idx.size() < index) {
//                    idx.add(i);
//                } else {
//                    idx.add(index, i);
//                }
//            }
//        }
//        data = new float[list.size()];
//        for (int i = 0; i < data.length; i++) {
//            data[i] = list.get(i);
//        }
//
//        indices = new int[idx.size()];
//        for (int i = 0; i < indices.length; i++) {
//            indices[i] = idx.get(i);
//        }
//    }
//
//    private int insert(LinkedList<Float> list, float value) {
//        for (int i = 0; i < list.size(); i++) {
//            float w = list.get(i);
//            if (value > w) {
//                list.add(i, value);
//                return i;
//            }
//        }
//
//        list.add(value);
//        return list.size();
//    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < indices.length; i++) {
            b.append(indices[i]).append(",");
        }
        b.append("\n");
        for (int i = 0; i < data.length; i++) {
            b.append(data[i]).append(",");
        }
        return b.toString();
    }

    public static void main(String... args) {
        // 6 7 4 8
        float values[] = {0, 0, 0, 0, 0.5f, 0.001f, 0.7f, 0.6f, 0.2f, 0, 0, 0};
        Weights w = new Weights(values, 0, values.length);
        System.err.println(w);
    }
}

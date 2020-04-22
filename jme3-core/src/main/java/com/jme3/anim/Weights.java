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

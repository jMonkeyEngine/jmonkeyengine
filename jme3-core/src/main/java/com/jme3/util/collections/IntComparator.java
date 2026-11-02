package com.jme3.util.collections;

import java.util.Comparator;

public interface IntComparator extends Comparator<Integer> {

    int compare(int o1, int o2);

    @Override
    default int compare(Integer o1, Integer o2) {
        return compare((int)o1, (int)o2);
    }

}

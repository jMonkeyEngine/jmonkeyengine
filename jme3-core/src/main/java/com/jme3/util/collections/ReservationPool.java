package com.jme3.util.collections;

/**
 * Reserves keyed slots from a pool.
 */
public interface ReservationPool <K> {

    K acquire();

    void release(K key);

}

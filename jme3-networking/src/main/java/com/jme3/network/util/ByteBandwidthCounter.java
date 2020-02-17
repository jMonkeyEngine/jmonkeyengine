package com.jme3.network.util;

public class ByteBandwidthCounter implements BandwidthCounter {
    private long tx = 0;
    private long rx = 0;
    private boolean overflow = false;

    public long getTx() {
        return tx;
    }

    public long getRx() {
        return rx;
    }

    public void reset() {
        tx = 0;
        rx = 0;
        overflow = false;
    }

    public boolean overflowed() {
        return overflow;
    }

    public void incRx(long val) {
        long prev = rx;
        rx += val;
        if (!overflow && prev > rx) {
            overflow = true;
        }
    }

    public void incTx(long val) {
        long prev = tx;
        tx += val;
        if (!overflow && prev > tx) {
            overflow = true;
        }
    }

}

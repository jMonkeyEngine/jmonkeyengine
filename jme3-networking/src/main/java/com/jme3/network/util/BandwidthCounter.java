package com.jme3.network.util;

public interface BandwidthCounter {

    public long getTx();

    public long getRx();

    public void reset();

    public boolean overflowed();

    public void incRx(long val);

    public void incTx(long val);
}

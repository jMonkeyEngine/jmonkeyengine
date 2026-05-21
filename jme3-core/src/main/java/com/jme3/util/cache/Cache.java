package com.jme3.util.cache;

import java.util.Map;

public interface Cache <K, E> extends Map<K, E> {

    void flush();

}

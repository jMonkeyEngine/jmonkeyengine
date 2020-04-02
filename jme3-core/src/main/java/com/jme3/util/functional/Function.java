package com.jme3.util.functional;


public interface Function<R,T> {
    R eval(T t);
}
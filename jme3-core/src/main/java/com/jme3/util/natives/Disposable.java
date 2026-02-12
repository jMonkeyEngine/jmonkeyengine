package com.jme3.util.natives;

public interface Disposable {

    Runnable createDestroyer();

    DisposableReference getReference();

    default void prematureDestruction() {}

}

package com.jme3.renderer.framegraph;

/**
 * @author JohnKkk
 * @param <T>
 */
public class FGVarSource<T> extends FGSource{
    public static class FGVarBindableProxy<T> extends FGBindable{
        T value;

        public FGVarBindableProxy(T value) {
            this.value = value;
        }

        public T getValue() {
            return value;
        }
    }
    private FGVarBindableProxy<T> varBindableProxy;
    public FGVarSource(String name, T value) {
        super(name);
        varBindableProxy = new FGVarBindableProxy<T>(value);
    }

    public void setValue(T t){
        varBindableProxy.value = t;
    }

    @Override
    public void postLinkValidate() {

    }

    @Override
    public FGBindable yieldBindable() {
        return varBindableProxy;
    }
}

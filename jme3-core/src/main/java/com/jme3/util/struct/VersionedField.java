package com.jme3.util.struct;

import com.jme3.util.VersionedValue;

public class VersionedField <T> extends Struct.Field<T> {

    private long version = -1L;

    public VersionedField(T alias) {
        super(alias);
    }

    public VersionedField(String name, T alias) {
        super(name, alias);
    }

    public void set(T value, long version) {
        if (this.version != version) {
            set(value);
            this.version = version;
        }
    }

    public void set(VersionedValue<T> value) {
        set(value.get(), value.getVersion());
    }

    public void set(long version) {
        if (this.version != version) {
            set();
            this.version = version;
        }
    }

}

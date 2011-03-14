package com.jme3.network.sync;

public final class EntitySyncInfo {

    public static final byte TYPE_NEW = 0x1,
                             TYPE_SYNC = 0x2,
                             TYPE_DELETE = 0x3;
    
    /**
     * NEW, SYNC, or DELETE
     */
    public byte type;
    /**
     * Entity ID
     */
    public int id;
    /**
     * Entity Class Name
     */
    public String className;
    
    /**
     * Vars
     */
    public byte[] data;
}

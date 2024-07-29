/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

/**
 *
 * @author codex
 * @param <T>
 */
public class TicketGroup <T> {
    
    /**
     * Prefix for tickets that are members of a list.
     */
    public static final String LIST = "#list:";
    
    private final String name;
    private ResourceTicket<T>[] array;
    private boolean list = false;

    public TicketGroup(String name) {
        this.name = name;
        this.array = new ResourceTicket[0];
        this.list = true;
    }
    public TicketGroup(String name, int length) {
        this.name = name;
        this.array = new ResourceTicket[length];
    }

    public ResourceTicket<T> create(int i) {
        String tName;
        if (!list) {
            tName = arrayTicketName(name, i);
        } else {
            tName = listTicketName(name);
        }
        return new ResourceTicket<>(tName);
    }

    public ResourceTicket<T> add() {
        ResourceTicket[] temp = new ResourceTicket[array.length+1];
        System.arraycopy(array, 0, temp, 0, array.length);
        array = temp;
        return (array[array.length-1] = create(array.length-1));
    }
    public int remove(ResourceTicket t) {
        requireAsList(true);
        int i = array.length-1;
        for (; i >= 0; i--) {
            if (array[i] == t) break;
        }
        if (i >= 0) {
            ResourceTicket[] temp = new ResourceTicket[array.length-1];
            if (i > 0) {
                System.arraycopy(array, 0, temp, 0, i);
            }
            if (i < array.length-1) {
                System.arraycopy(array, i+1, temp, i, array.length-i-1);
            }
            array = temp;
        }
        return i;
    }

    public void requireAsList(boolean list) {
        if (this.list != list) {
            throw new IllegalStateException("Group must be "+(list ? "a list" : "an array")+" in this context.");
        }
    }
    
    public String getName() {
        return name;
    }
    public ResourceTicket<T>[] getArray() {
        return array;
    }
    public boolean isList() {
        return list;
    }
    
    /**
     * 
     * @param group
     * @param i
     * @return 
     */
    public static String arrayTicketName(String group, int i) {
        return group+'['+i+']';
    }
    /**
     * 
     * @param group
     * @return 
     */
    public static String listTicketName(String group) {
        return LIST+group;
    }
    /**
     * 
     * @param name
     * @return 
     */
    public static boolean isListTicket(String name) {
        return name.startsWith(LIST);
    }
    /**
     * 
     * @param name
     * @return 
     */
    public static String extractGroupName(String name) {
        return name.substring(LIST.length());
    }
    
}

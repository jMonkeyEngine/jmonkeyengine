package com.jme3.input.android;

import com.jme3.math.Vector2f;

public class TouchEvent
{
    public static enum Type {GRABBED,DRAGGED,RELEASED,FLING,TAP,DOUBLETAP,LONGPRESSED,SCALE,OUTSIDE,IDLE}
    public Type type=Type.IDLE;

    public static enum Operation {NOP,STARTED,RUNNING,STOPPED,CANCELED}
    private Operation operation=Operation.NOP;

    public float x;
    public float y;
    public float deltax;
    public float deltay;
    public float[] extra;

    public TouchEvent(Type type, Operation operation, float x, float y, float deltax, float deltay, float[] extra)
    {
        set(type, operation, x, y, deltax, deltay, extra);
    }

    public void set( Type type, Operation operation, float x, float y, float deltax, float deltay, float[] extra)
    {
        this.type=type;
        this.operation=operation;
        this.x=x;
        this.y=y;
        this.deltax=deltax;
        this.deltay=deltay;
        this.extra=extra;
    }


    public Type getType()
    {
        return type;
    }

    public Operation getOperation()
    {
        return operation;
    }


    public float getX()
    {
        return x;
    }

    public float getY()
    {
        return y;
    }

    public float getDeltaX()
    {
        return deltax;
    }

    public float getDeltaY()
    {
        return deltay;
    }

    public float[] getExtra()
    {
        return extra;
    }

    public Vector2f getDelta()
    {
        return new Vector2f(deltax,deltay);
    }
}

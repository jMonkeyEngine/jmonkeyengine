package com.jme3.vulkan.shadergen.vars;

public abstract class MathVar <SELF extends MathVar> extends AbstractVar<SELF> {

    public MathVar(String type, String exp) {
        super(type, exp);
    }

    @Override
    public abstract SELF create(String exp);

    public SELF add(SELF v) {
        return create(fmt("(%s+%s)", exp, v));
    }

    public SELF sub(SELF v) {
        return create(fmt("(%s-%s)", exp, v));
    }

    public SELF mult(SELF v) {
        return create(fmt("(%s*%s)", exp, v));
    }

    public SELF div(SELF v) {
        return create(fmt("(%s/%s)", exp, v));
    }

    public void addLocal(SELF v) {
        emit("%s+=%s;", exp, v);
    }

    public void subLocal(SELF v) {
        emit("%s-=%s;", exp, v);
    }

    public void multLocal(SELF v) {
        emit("%s*=%s;", exp, v);
    }

    public void divLocal(SELF v) {
        emit("%s/=%s;", exp, v);
    }

    public SELF add(Num n) {
        return create(fmt("(%s+%s)", exp, n));
    }

    public SELF sub(Num n) {
        return create(fmt("(%s-%s)", exp, n));
    }

    public SELF mult(Num n) {
        return create(fmt("(%s*%s)", exp, n));
    }

    public SELF div(Num n) {
        return create(fmt("(%s/%s)", exp, n));
    }

    public SELF addLocal(Num n) {
        return create(fmt("%s+=%s;", exp, n));
    }

    public SELF subLocal(Num n) {
        return create(fmt("%s-=%s;", exp, n));
    }

    public SELF multLocal(Num n) {
        return create(fmt("%s*=%s;", exp, n));
    }

    public SELF divLocal(Num n) {
        return create(fmt("%s/=%s;", exp, n));
    }
    
}

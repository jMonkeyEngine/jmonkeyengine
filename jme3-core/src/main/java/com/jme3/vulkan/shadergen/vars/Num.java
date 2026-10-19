package com.jme3.vulkan.shadergen.vars;

public abstract class Num <SELF extends Num, N extends Number> extends MathVar<SELF> {

    public Num(String type, String exp) {
        super(type, exp);
    }

    @Override
    public SELF declare() {
        return super.declare();
    }

    @SuppressWarnings("unchecked")
    public SELF increment() {
        emit("%s++;", exp);
        return (SELF)this;
    }

    @SuppressWarnings("unchecked")
    public SELF decrement() {
        emit("%s--;", exp);
        return (SELF)this;
    }

    public SELF getAndIncrement() {
        return create(fmt("%s++", exp));
    }

    public SELF incrementAndGet() {
        return create(fmt("++%s", exp));
    }

    public SELF getAndDecrement() {
        return create(fmt("%s--", exp));
    }

    public SELF decrementAndGet() {
        return create(fmt("--%s", exp));
    }

    public SELF add(N n) {
        return create(fmt("(%s+%s)", exp, n));
    }

    public SELF sub(N n) {
        return create(fmt("(%s-%s)", exp, n));
    }

    public SELF mult(N n) {
        return create(fmt("(%s*%s)", exp, n));
    }

    public SELF div(N n) {
        return create(fmt("(%s/%s)", exp, n));
    }

    public SELF addLocal(N n) {
        return create(fmt("%s+=%s;", exp, n));
    }

    public SELF subLocal(N n) {
        return create(fmt("%s-=%s;", exp, n));
    }

    public SELF multLocal(N n) {
        return create(fmt("%s*=%s;", exp, n));
    }

    public SELF divLocal(N n) {
        return create(fmt("%s/=%s;", exp, n));
    }

    public Bool eq(N n) {
        return new Bool(fmt("(%s==%s)", exp, n));
    }

    public Bool eq(SELF n) {
        return new Bool(fmt("(%s==%s)", exp, n));
    }

    public Bool lt(N n) {
        return new Bool(fmt("(%s<%s)", exp, n));
    }

    public Bool lt(SELF n) {
        return new Bool(fmt("(%s<%s)", exp, n));
    }

    public Bool lte(N n) {
        return new Bool(fmt("(%s<=%s)", exp, n));
    }

    public Bool lte(SELF n) {
        return new Bool(fmt("(%s<=%s)", exp, n));
    }

    public Bool gt(N n) {
        return new Bool(fmt("(%s>%s)", exp, n));
    }

    public Bool gt(SELF n) {
        return new Bool(fmt("(%s>%s)", exp, n));
    }

    public Bool gte(N n) {
        return new Bool(fmt("(%s>=%s)", exp, n));
    }

    public Bool gte(SELF n) {
        return new Bool(fmt("(%s>=%s)", exp, n));
    }

}

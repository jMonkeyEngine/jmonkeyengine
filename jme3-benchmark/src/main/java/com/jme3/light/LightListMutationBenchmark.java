/*
 * Copyright (c) 2009-2026 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.light;

import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@Fork(2)
@State(Scope.Thread)
public class LightListMutationBenchmark {

    @Param({"8", "64", "256"})
    public int lightCount;

    private Geometry owner;
    private Light[] lights;
    private LightList list;
    private LightList local;
    private LightList parent;

    @Setup(Level.Trial)
    public void setupTrial() {
        owner = new Geometry("owner", new Mesh());
        lights = new Light[lightCount * 2];
        for (int i = 0; i < lights.length; i++) {
            lights[i] = new PointLight(new Vector3f(i, i * 0.5f, -i));
        }
        list = new LightList(owner);
        local = new LightList(owner);
        parent = new LightList(owner);
        for (int i = 0; i < lightCount; i++) {
            local.add(lights[i]);
            parent.add(lights[lightCount + i]);
        }
    }

    @Setup(Level.Invocation)
    public void setupInvocation() {
        list.clear();
        for (int i = 0; i < lightCount; i++) {
            list.add(lights[i]);
        }
    }

    @Benchmark
    public void removeFromFront(Blackhole blackhole) {
        list.remove(0);
        blackhole.consume(list.size());
    }

    @Benchmark
    public void removeFromMiddle(Blackhole blackhole) {
        list.remove(lightCount >>> 1);
        blackhole.consume(list.size());
    }

    @Benchmark
    public void removeFromEnd(Blackhole blackhole) {
        list.remove(lightCount - 1);
        blackhole.consume(list.size());
    }

    @Benchmark
    public void updateFromLocalAndParent(Blackhole blackhole) {
        list.update(local, parent);
        blackhole.consume(list.size());
    }
}

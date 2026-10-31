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
import java.util.Random;
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
public class LightListSortBenchmark {

    @Param({"8", "64", "256"})
    public int lightCount;

    @Param({"1", "8", "1024"})
    public int retainedCapacityMultiplier;

    private Geometry owner;
    private Light[] lights;
    private LightList list;
    private int invocation;

    @Setup(Level.Trial)
    public void setupTrial() {
        owner = new Geometry("owner", new Mesh());
        owner.setLocalTranslation(3f, -7f, 11f);
        owner.updateGeometricState();

        lights = new Light[lightCount];
        Random random = new Random(0x51A7E5L + lightCount);
        for (int i = 0; i < lightCount; i++) {
            switch (i & 3) {
                case 0:
                    lights[i] = new AmbientLight();
                    break;
                case 1:
                    lights[i] = new DirectionalLight(new Vector3f(1f, -1f, 0.25f).normalizeLocal());
                    break;
                default:
                    lights[i] = new PointLight(new Vector3f(
                            random.nextFloat() * 200f - 100f,
                            random.nextFloat() * 200f - 100f,
                            random.nextFloat() * 200f - 100f));
                    break;
            }
        }

        list = new LightList(owner);
        int retainedCapacity = Math.max(lightCount, lightCount * retainedCapacityMultiplier);
        for (int i = 0; i < retainedCapacity; i++) {
            list.add(lights[i % lightCount]);
        }
        list.clear();
    }

    @Setup(Level.Invocation)
    public void setupInvocation() {
        list.clear();
        int offset = (invocation++ & Integer.MAX_VALUE) % lightCount;
        for (int i = 0; i < lightCount; i++) {
            list.add(lights[(i + offset) % lightCount]);
        }
    }

    @Benchmark
    public void sortTransformChanged(Blackhole blackhole) {
        list.sort(true);
        blackhole.consume(list.get(lightCount - 1));
    }
}

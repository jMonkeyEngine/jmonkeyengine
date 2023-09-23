/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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
package jme3test.stress;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Sphere;
import com.jme3.util.TangentBinormalGenerator;

public class TestParallelTangentGeneration {

    final private static ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public static void main(String[] args) {

        for (int i = 0; i < 10; i++) {
            {
                Node root = new Node("Root");
                for (int count = 0; count < 10; count++) {
                    for (int samples = 4; samples < 50; samples++) {
                        Geometry g = new Geometry();
                        g.setMesh(new Sphere(samples, samples, 1.0f));
                        root.attachChild(g);
                    }
                }

                long start = System.currentTimeMillis();
                TangentBinormalGenerator.generate(root);
                System.out.println("Serial " + (System.currentTimeMillis() - start));
            }

            {
                Node root = new Node("Root");
                for (int count = 0; count < 10; count++) {
                    for (int samples = 4; samples < 50; samples++) {
                        Geometry g = new Geometry();
                        g.setMesh(new Sphere(samples, samples, 1.0f));
                        root.attachChild(g);
                    }
                }

                long start = System.currentTimeMillis();
                TangentBinormalGenerator.generateParallel(root, executor);
                System.out.println("Parallel " + (System.currentTimeMillis() - start));
            }

        }
    }
}
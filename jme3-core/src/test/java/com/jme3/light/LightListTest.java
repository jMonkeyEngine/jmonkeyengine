/*
 * Copyright (c) 2026 jMonkeyEngine
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test cases for LightList.
 */
public class LightListTest {

    /**
     * Verify sort() remains correct after the list previously held more lights.
     */
    @Test
    public void testSortAfterRetainedCapacity() {
        Geometry owner = new Geometry("owner", new Mesh());
        LightList list = new LightList(owner);

        for (int i = 0; i < 32; i++) {
            list.add(new PointLight(new Vector3f(100f + i, 0f, 0f)));
        }
        list.sort(true);
        list.clear();

        PointLight far = new PointLight(new Vector3f(10f, 0f, 0f));
        PointLight near = new PointLight(new Vector3f(1f, 0f, 0f));
        PointLight middle = new PointLight(new Vector3f(5f, 0f, 0f));

        list.add(far);
        list.add(near);
        list.add(middle);
        list.sort(true);

        Assertions.assertSame(near, list.get(0));
        Assertions.assertSame(middle, list.get(1));
        Assertions.assertSame(far, list.get(2));
    }

    /**
     * Verify indexed removal preserves order for front, middle, and tail removals.
     */
    @Test
    public void testRemovePreservesOrder() {
        LightList list = new LightList(new Geometry("owner", new Mesh()));
        AmbientLight first = new AmbientLight();
        AmbientLight second = new AmbientLight();
        AmbientLight third = new AmbientLight();
        AmbientLight fourth = new AmbientLight();

        list.add(first);
        list.add(second);
        list.add(third);
        list.add(fourth);

        list.remove(0);
        Assertions.assertEquals(3, list.size());
        Assertions.assertSame(second, list.get(0));
        Assertions.assertSame(third, list.get(1));
        Assertions.assertSame(fourth, list.get(2));

        list.remove(1);
        Assertions.assertEquals(2, list.size());
        Assertions.assertSame(second, list.get(0));
        Assertions.assertSame(fourth, list.get(1));

        list.remove(1);
        Assertions.assertEquals(1, list.size());
        Assertions.assertSame(second, list.get(0));
    }

    /**
     * Verify unfiltered world-list updates preserve local-then-parent ordering.
     */
    @Test
    public void testUpdateCopiesLocalAndParentLights() {
        LightList local = new LightList(new Geometry("local", new Mesh()));
        LightList parent = new LightList(new Geometry("parent", new Mesh()));
        LightList world = new LightList(new Geometry("world", new Mesh()));

        AmbientLight localFirst = new AmbientLight();
        AmbientLight localSecond = new AmbientLight();
        AmbientLight parentFirst = new AmbientLight();
        AmbientLight parentSecond = new AmbientLight();

        local.add(localFirst);
        local.add(localSecond);
        parent.add(parentFirst);
        parent.add(parentSecond);

        world.update(local, parent);

        Assertions.assertEquals(4, world.size());
        Assertions.assertSame(localFirst, world.get(0));
        Assertions.assertSame(localSecond, world.get(1));
        Assertions.assertSame(parentFirst, world.get(2));
        Assertions.assertSame(parentSecond, world.get(3));
    }

    /**
     * Verify filtered world-list updates keep only accepted local lights.
     */
    @Test
    public void testUpdateFiltersLocalLights() {
        LightList local = new LightList(new Geometry("local", new Mesh()));
        LightList parent = new LightList(new Geometry("parent", new Mesh()));
        LightList world = new LightList(new Geometry("world", new Mesh()));

        AmbientLight keep = new AmbientLight();
        AmbientLight discard = new AmbientLight();
        AmbientLight parentLight = new AmbientLight();

        local.add(keep);
        local.add(discard);
        parent.add(parentLight);

        world.update(local, parent, light -> light != discard);

        Assertions.assertEquals(2, world.size());
        Assertions.assertSame(keep, world.get(0));
        Assertions.assertSame(parentLight, world.get(1));
    }
}

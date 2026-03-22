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
package com.jme3.util;

import com.jme3.collision.CollisionResult;
import com.jme3.collision.bih.BIHNode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.Arrays;
import org.hamcrest.CoreMatchers;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.internal.ArrayComparisonFailure;

/**
 *
 * @author parysto
 */
public class TempVarsTest {

    public TempVarsTest() {
    }

    @Test
    public void testStoresValuesUntilRelease() {
        {
            final TempVars tempVars = TempVars.get();
            addAndAssertData(tempVars);
            tempVars.release();
        }

        try (final TempVars tempVars = TempVars.get()) {
            addAndAssertData(tempVars);
        }
    }

    private void addAndAssertData(final TempVars tempVars) throws ArrayComparisonFailure {
        final BIHNode.BIHStackData bihStackData0 = new BIHNode.BIHStackData(null, 0, 0);
        final BIHNode.BIHStackData bihStackData1 = new BIHNode.BIHStackData(null, 0, 0);
        tempVars.bihStack.add(bihStackData0);
        tempVars.bihStack.add(bihStackData1);

        final CollisionResult collisionResult0 = new CollisionResult();
        final CollisionResult collisionResult1 = new CollisionResult();
        final CollisionResult collisionResult2 = new CollisionResult();
        tempVars.collisionResults.addCollision(collisionResult0);
        tempVars.collisionResults.addCollision(collisionResult1);
        tempVars.collisionResults.addCollision(collisionResult2);

        final Node spatial0 = new Node();
        final Node spatial1 = new Node();
        tempVars.spatialStack[0] = spatial0;
        tempVars.spatialStack[3] = spatial1;

        assertArrayEquals("bihStack must contain two matching entries.",
                new BIHNode.BIHStackData[]{bihStackData0, bihStackData1},
                tempVars.bihStack.toArray());

        assertEquals("collisionResults must contain three entries.", 3, tempVars.collisionResults.size());
        assertThat("collisionResults must contain three matching entries.",
                tempVars.collisionResults,
                CoreMatchers.hasItems(collisionResult0, collisionResult1, collisionResult2));

        final Spatial[] expectedSpatialStack = new Spatial[tempVars.spatialStack.length];
        expectedSpatialStack[0] = spatial0;
        expectedSpatialStack[3] = spatial1;
        assertArrayEquals("spatialStack must contain two matching entries.", expectedSpatialStack, tempVars.spatialStack);
    }

    @Test
    public void testRemovesValuesOnReleaseAndClose() {
        {
            final TempVars tempVars = TempVars.get();
            addData(tempVars);
            tempVars.release();

            assertEquals("bihStack must be empty after releasing tempVars.", 0, tempVars.bihStack.size());
            assertEquals("collisionResults must be empty after releasing tempVars.", 0, tempVars.collisionResults.size());
            assertThat("All entries in spatialStack must be null after releasing tempVars.",
                    Arrays.asList(tempVars.spatialStack),
                    everyItem(nullValue(Spatial.class)));
        }

        {
            final TempVars tempVars = TempVars.get();
            assertEquals("bihStack must be empty after releasing and getting the same tempVars.", 0, tempVars.bihStack.size());
            assertEquals("collisionResults must be empty after releasing and getting the same tempVars.", 0, tempVars.collisionResults.size());
            assertThat("All entries in spatialStack must be null after releasing and getting tempVars.",
                    Arrays.asList(tempVars.spatialStack),
                    everyItem(nullValue(Spatial.class)));
            tempVars.release();
        }

        try (final TempVars tempVars = TempVars.get()) {
            addData(tempVars);
        }

        try (final TempVars tempVars = TempVars.get()) {
            assertEquals("bihStack must be empty after closing and getting the same tempVars.", 0, tempVars.bihStack.size());
            assertEquals("collisionResults must be empty after closing and getting the same tempVars.", 0, tempVars.collisionResults.size());
            assertThat("All entries in spatialStack must be null after closing and getting tempVars.",
                    Arrays.asList(tempVars.spatialStack),
                    everyItem(nullValue(Spatial.class)));
        }
    }

    private void addData(final TempVars tempVars) {
        tempVars.bihStack.add(new BIHNode.BIHStackData(null, 0, 0));
        tempVars.bihStack.add(new BIHNode.BIHStackData(null, 0, 0));
        tempVars.collisionResults.addCollision(new CollisionResult());
        tempVars.collisionResults.addCollision(new CollisionResult());
        tempVars.collisionResults.addCollision(new CollisionResult());
        tempVars.spatialStack[0] = new Node();
        tempVars.spatialStack[3] = new Node();
    }

}

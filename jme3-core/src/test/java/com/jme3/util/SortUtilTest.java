/*
 * Copyright (c) 2009-2024 jMonkeyEngine
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

import java.util.Arrays;
import java.util.Comparator;
import org.junit.Assert;
import org.junit.Test;

/**
 * Verifies that the {@link SortUtil} sorting algorithms work correctly.
 */
public class SortUtilTest {

    private static final Comparator<Integer> INT_CMP = Comparator.naturalOrder();

    // -----------------------------------------------------------------------
    // gsort (gnome sort)
    // -----------------------------------------------------------------------

    @Test
    public void testGsortEmptyArray() {
        Integer[] arr = {};
        SortUtil.gsort(arr, INT_CMP);
        Assert.assertEquals(0, arr.length);
    }

    @Test
    public void testGsortSingleElement() {
        Integer[] arr = {42};
        SortUtil.gsort(arr, INT_CMP);
        Assert.assertArrayEquals(new Integer[]{42}, arr);
    }

    @Test
    public void testGsortAlreadySorted() {
        Integer[] arr = {1, 2, 3, 4, 5};
        SortUtil.gsort(arr, INT_CMP);
        Assert.assertArrayEquals(new Integer[]{1, 2, 3, 4, 5}, arr);
    }

    @Test
    public void testGsortReverseSorted() {
        Integer[] arr = {5, 4, 3, 2, 1};
        SortUtil.gsort(arr, INT_CMP);
        Assert.assertArrayEquals(new Integer[]{1, 2, 3, 4, 5}, arr);
    }

    @Test
    public void testGsortRandom() {
        Integer[] arr = {3, 1, 4, 1, 5, 9, 2, 6, 5, 3};
        Integer[] expected = arr.clone();
        Arrays.sort(expected, INT_CMP);
        SortUtil.gsort(arr, INT_CMP);
        Assert.assertArrayEquals(expected, arr);
    }

    @Test
    public void testGsortDuplicates() {
        Integer[] arr = {2, 2, 1, 1, 3, 3};
        SortUtil.gsort(arr, INT_CMP);
        Assert.assertArrayEquals(new Integer[]{1, 1, 2, 2, 3, 3}, arr);
    }

    // -----------------------------------------------------------------------
    // qsort (quicksort on Object[])
    // -----------------------------------------------------------------------

    @Test
    public void testQsortEmptyArray() {
        Integer[] arr = {};
        SortUtil.qsort(arr, INT_CMP);
        Assert.assertEquals(0, arr.length);
    }

    @Test
    public void testQsortSingleElement() {
        Integer[] arr = {7};
        SortUtil.qsort(arr, INT_CMP);
        Assert.assertArrayEquals(new Integer[]{7}, arr);
    }

    @Test
    public void testQsortAlreadySorted() {
        Integer[] arr = {1, 2, 3, 4, 5};
        SortUtil.qsort(arr, INT_CMP);
        Assert.assertArrayEquals(new Integer[]{1, 2, 3, 4, 5}, arr);
    }

    @Test
    public void testQsortReverseSorted() {
        Integer[] arr = {5, 4, 3, 2, 1};
        SortUtil.qsort(arr, INT_CMP);
        Assert.assertArrayEquals(new Integer[]{1, 2, 3, 4, 5}, arr);
    }

    @Test
    public void testQsortRandom() {
        Integer[] arr = {7, 2, 9, 1, 4, 8, 3, 5, 6};
        Integer[] expected = arr.clone();
        Arrays.sort(expected, INT_CMP);
        SortUtil.qsort(arr, INT_CMP);
        Assert.assertArrayEquals(expected, arr);
    }

    // -----------------------------------------------------------------------
    // qsort (quicksort on int[])
    // -----------------------------------------------------------------------

    @Test
    public void testQsortIntArray() {
        int[] arr = {5, 3, 8, 1, 4, 2};
        Comparator<Integer> intCmp = Comparator.naturalOrder();
        SortUtil.qsort(arr, 0, arr.length - 1, intCmp);
        int[] expected = {1, 2, 3, 4, 5, 8};
        Assert.assertArrayEquals(expected, arr);
    }

    @Test
    public void testQsortIntArrayAlreadySorted() {
        int[] arr = {1, 2, 3, 4, 5};
        Comparator<Integer> intCmp = Comparator.naturalOrder();
        SortUtil.qsort(arr, 0, arr.length - 1, intCmp);
        Assert.assertArrayEquals(new int[]{1, 2, 3, 4, 5}, arr);
    }

    // -----------------------------------------------------------------------
    // msort (merge sort)
    // -----------------------------------------------------------------------

    @Test
    public void testMsortBasic() {
        Integer[] src = {3, 1, 4, 1, 5, 9, 2, 6};
        Integer[] dest = new Integer[src.length];
        Integer[] expected = src.clone();
        Arrays.sort(expected, INT_CMP);
        SortUtil.msort(src, dest, INT_CMP);
        // After msort the sorted result ends up in src
        Assert.assertArrayEquals(expected, src);
    }

    @Test
    public void testMsortAlreadySorted() {
        Integer[] src = {1, 2, 3, 4, 5};
        Integer[] dest = new Integer[src.length];
        SortUtil.msort(src, dest, INT_CMP);
        Assert.assertArrayEquals(new Integer[]{1, 2, 3, 4, 5}, src);
    }

    @Test
    public void testMsortSingleElement() {
        Integer[] src = {99};
        Integer[] dest = new Integer[1];
        SortUtil.msort(src, dest, INT_CMP);
        Assert.assertArrayEquals(new Integer[]{99}, src);
    }
}

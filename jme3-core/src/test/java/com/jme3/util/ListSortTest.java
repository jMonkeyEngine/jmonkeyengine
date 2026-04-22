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

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies order of entries sorted by {@link ListSort} and checks if all
 * references added to {@link ListSort#tmpArray} are removed after sorting.
 *
 * @author parysto
 */
public class ListSortTest {

    private Integer[] arrayToSort;

    @BeforeEach
    public void initTestArray() {
        arrayToSort = new Integer[]{36, 10, 16, 9, 14, 32, 35, 22, 1, 27, 18, 11,
            30, 15, 2, 12, 32, 27, 11, 45, 7, 32, 36, 11, 39, 32, 45, 35, 40, 17,
            43, 24, 14, 10, 29, 19, 23, 14, 1, 44, 35, 24, 10, 37, 7, 35, 10, 9,
            43, 48, 40, 47, 29, 8, 48, 7, 22, 6, 46, 46, 10, 31, 35, 45};
    }

    @Test
    public void testBinarySortFirstRun() throws ReflectiveOperationException {
        assertTrue(arrayToSort.length < 128, "Array to sort must be smaller than merge-sort threshhold.");
        sortAndAssert(arrayToSort, false);
    }

    @Test
    public void testBinarySort() throws ReflectiveOperationException {
        assertTrue(arrayToSort.length < 128, "Array to sort must be smaller than merge-sort threshhold.");
        sortAndAssert(arrayToSort, true);
    }

    @Test
    public void testMergeSortFirstRun() throws ReflectiveOperationException {
        Integer[] bigArray = Arrays.copyOf(arrayToSort, arrayToSort.length * 3);
        System.arraycopy(arrayToSort, 0, bigArray, arrayToSort.length, arrayToSort.length);
        System.arraycopy(arrayToSort, 0, bigArray, arrayToSort.length * 2, arrayToSort.length);

        assertTrue(bigArray.length > 128, "Array to sort must be bigger than merge-sort threshhold.");
        sortAndAssert(bigArray, false);
    }

    @Test
    public void testMergeSort() throws ReflectiveOperationException {
        Integer[] bigArray = Arrays.copyOf(arrayToSort, arrayToSort.length * 3);
        System.arraycopy(arrayToSort, 0, bigArray, arrayToSort.length, arrayToSort.length);
        System.arraycopy(arrayToSort, 0, bigArray, arrayToSort.length * 2, arrayToSort.length);

        assertTrue(bigArray.length > 128, "Array to sort must be bigger than merge-sort threshhold.");
        sortAndAssert(bigArray, true);
    }

    /**
     * Calls {@link ListSort#sort(T[], java.util.Comparator) }, and asserts
     * order of entries and all references in {@link ListSort#tmpArray} set to
     * {@code null}.
     *
     * @param array Array to sort by ListSort
     * @param simulateSecondRun If set to {@code true}, simulate list sort being
     * reused by allocating 100 additional entries in tmpArray
     */
    private void sortAndAssert(Integer[] array, boolean simulateSecondRun) throws ReflectiveOperationException {
        final Integer[] expected = array.clone();
        Arrays.sort(expected);

        final ListSort<Integer> listSort = new ListSort<>();
        if (simulateSecondRun) {
            listSort.allocateStack(array.length + 100);
        }
        listSort.allocateStack(array.length);
        listSort.sort(array, Comparator.naturalOrder());

        assertArrayEquals(expected, array);

        final Field tmpArrayField = ListSort.class.getDeclaredField("tmpArray");
        tmpArrayField.setAccessible(true);
        Object[] tmpArray = (Object[]) tmpArrayField.get(listSort);

        assertThat("TmpArray must be cleared after sort(...).",
                Arrays.asList(tmpArray),
                everyItem(nullValue()));
    }

}

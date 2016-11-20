/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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

import java.util.Comparator;

/**
 * Fast, stable sort used to sort geometries
 *
 * It's adapted from Tim Peters's work on list sorting for Python. More details
 * here http://svn.python.org/projects/python/trunk/Objects/listsort.txt
 * 
 * here is the C code from which this class is based 
 * http://svn.python.org/projects/python/trunk/Objects/listobject.c
 *
 * This class was also greatly inspired from java 7 TimSort by Josh Blosh with the
 * difference that the temporary necessary memory space are allocated as the
 * geometry list grows and reused all along the application execution.
 *
 * Usage : ListSort has to be instanciated and kept with the geometry list ( or
 * w/e it may have to sort) Then the allocate method has to be called to
 * allocate necessary tmp space needed for the sort. This should be called once
 * for optimal performance, but can be called several times if the length of the
 * list changes
 *
 * Disclaimer : I was intrigued by the use of val >>> 1 in java 7 Timsort class
 * instead of val / 2 (integer division). Micro benching revealed that val >>> 1
 * is twice faster than val / 2 in java 6 and has similar perf in java 7. The
 * following code uses val >>> 1 when ever a value needs to be divided by 2 and
 * rounded to its floor
 *
 *
 * @author Nehon
 */
public class ListSort<T> {

    /**
     * Threshold for binary sort vs merge. Original algorithm use 64, java7
     * TimSort uses 32 and I used 128, see this post for explanations :
     * http://hub.jmonkeyengine.org/groups/development-discussion-jme3/forum/topic/i-got-that-sorted-out-huhuhu/
     */
    private static final int MIN_SIZE = 128;
    private T[] array;
    private T[] tmpArray;
    private Comparator<T> comparator;
    
    /**
     * attribute temp vars for merging. This was used to unroll the merge_lo &
     * merge_hi function of original implementations that used massive labeled
     * goto branching and was almost unreadable
     */
    int iterA, iterB, dest, lengthA, lengthB;
    
    /**
     * Number of runs to merge
     */
    private int nbRuns = 0;

    /* Try to used a kind of structure like in the original implementation.
     * Ended up using 2 arrays as done in the java 7 Timsort. 
     * Original implementation use a struct, but instanciation of this inner 
     * class + array was a convoluted pain.
     */
    /**
     * array of start indices in the original array for runs : run i sarting
     * index is at runIndices[i]
     */
    private int[] runsIndices = null;
    /**
     * array of runs length in the original array : run i length is at
     * runLength[i]
     */
    private int[] runsLength = null;
    /**
     * Length of the array to sort.(the passed on array is allocated by chunks
     * of 32, so its length may be bigger than the actual useful data to sort)
     */
    private int length = 0;
    /**
     * MIN_GALLOP set to 7 constant as described in listsort.txt. this magic
     * number indicates how many wins should trigger the switch from binary
     * search to gallopping mode
     */
    private static final int MIN_GALLOP = 7;
    /**
     * This variable allows to adjust when switching to galloping mode. lowered
     * when the data are "naturally" structured highered when data are random.
     */
    private int minGallop = MIN_GALLOP;

    /**
     * Creates a ListSort
     */
    public ListSort() {
    }

    /**
     * Allocate temp variables for the given length This method should be called
     * at least once, but only if the length of the list to sort changed before
     * sorting
     *
     * @param len
     */
    public final void allocateStack(int len) {

        length = len;
        /*
         * We allocate a temp array of half the size of the array to sort.
         * the original implementation had a 256 maximum size for this and made 
         * the temp array grow on demand
         * 
         * Timsort consumes half the size of the original array to merge at WORST.
         * But considering we use the same temp array over and over across frames
         * There is a good chance we stumble upon the worst case scenario one 
         * moment or another.
         * So we just always take half of the original array size.
         */        
        int tmpLen = len >>> 1;
     
        //if the array is null or tmpLen is above the actual length we allocate the array
        if (tmpArray == null || tmpLen > tmpArray.length) {
            //has to use Object for temp storage
            tmpArray = (T[]) new Object[tmpLen];         
        }

        /*
         * this part was taken from java 7 TimSort.
         * The original implementation use a stack of length 85, but this seems 
         * to boost up performance for mid sized arrays.
         * I changed the numbers so they fit our MIN_SIZE parameter.
         * 
         * Those numbers can be computed using this formula : 
         * MIN_SIZE * 1.618^n = N
         * Where n is the size of the stack, and N the number element of the array to sort        
         * If MIN_SIZE is changed you have to recompute those values.
         */
        int stackLen = (len < 1400 ? 5
                : len < 15730 ? 10
                : len < 1196194 ? 19 : 40);

        //Same remark as with the temp array
        if (runsIndices == null || stackLen > runsIndices.length) {
            runsIndices = new int[stackLen];
            runsLength = new int[stackLen];           
        }
    }

    /**
     * reset the runs stack to 0
     */
    private void clean() {
        for (int i = 0; i < runsIndices.length; i++) {
            runsIndices[i] = 0;
            runsLength[i] = 0;
        }
    }

    /**
     * Sort the given array given the comparator
     * @param array the array to sort
     * @param comparator the comparator to compare elements of the array
     */
    public void sort(T[] array, Comparator<T> comparator) {
        this.array = array;
        this.comparator = comparator;
        clean();
        int low = 0;
        int high = length;
        int remaining = high - low;

        /*
         * If array's size is bellow min_size we perform a binary insertion sort 
         * but first we check if some existing ordered pattern exists to reduce 
         * the size of data to be sorted
         */
        if (remaining < MIN_SIZE) {
            int runLength = getRunLength(array, low, high, comparator);
            binaryInsertionSort(array, low, high, low + runLength, comparator);
            return;
        }

        /*
         * main iteration : compute minrun length, then iterate through the 
         * array to find runs and merge them until they can be binary sorted 
         * if their length < minLength
         */
        int minLength = mergeComputeMinRun(remaining);
        while (remaining != 0) {
            int runLength = getRunLength(array, low, high, comparator);

            /* if runlength is bellow the threshold we binary sort the remaining 
             * elements
             */
            if (runLength < minLength) {
                int newLength = remaining <= minLength ? remaining : minLength;
                binaryInsertionSort(array, low, low + newLength, low + runLength, comparator);
                runLength = newLength;
            }

            // Add run to pending runs to merge and merge if necessary.
            runsIndices[nbRuns] = low;
            runsLength[nbRuns] = runLength;
            nbRuns++;
            mergeCollapse();

            // Advance to find next run
            low += runLength;
            remaining -= runLength;
        }

        // Merge all remaining runs to complete sort        
        mergeForceCollapse();        

    }

    /**
     * Return the length of the run beginning at lastId, in the slice [lastId,
     * lastId]. firstId &lt; lastId is required on entry. "A run" is the longest
     * ascending sequence, with
     *
     * array[0] <= array[1] <= array[2] <= ...
     *
     * or the longest descending sequence, with
     *
     * array[0] > array[1] > array[2] > ...
     *
     * The original algorithm is returning a "descending" boolean that allow the
     * caller to reverse the array. Here for simplicity we reverse the array
     * when the run is descending
     *
     * @param array the array to search for run length
     * @param firstId index of the first element of the run
     * @param lastId index+1 of the last element of the run
     * @param comparator the comparator
     * @return the length of the run beginning at the specified position in the
     * specified array
     */
    private int getRunLength(T[] array, int firstId, int lastId,
            Comparator<T> comparator) {

        int runEnd = firstId + 1;
        if (runEnd < lastId) {
            // if the range is > 1 we search for the end index of the run
            if (comparator.compare(array[runEnd++], array[firstId]) >= 0) {
                while (runEnd < lastId && comparator.compare(array[runEnd], array[runEnd - 1]) >= 0) {
                    runEnd++;
                }
            } else {
                while (runEnd < lastId && comparator.compare(array[runEnd], array[runEnd - 1]) < 0) {
                    runEnd++;
                }
                // the run's order is descending, it has to be reversed
                // original algorithmm return a descending = 1 value and the 
                //reverse is done in the sort method. Looks good to have it here though
                reverseArray(array, firstId, runEnd);
            }

            return runEnd - firstId;
        }
        //runEnd == lastId -> length = 1
        return 1;
    }

    /**
     * binarysort is the best method for sorting small arrays: it does few
     * compares, but can do data movement quadratic in the number of elements.
     * [firstId, lastId] is a contiguous slice of a list, and is sorted via
     * binary insertion. This sort is stable. On entry, must have firstId <=
     * start <= lastId, and that [firstId, start) is already sorted (pass start
     * == firstId if you don't know!).
     *
     * @param array the array to sort
     * @param firstId the index of the first element to sort
     * @param lastId the index+ of the last element to sort
     * @param start the index of the element to start sorting range
     * [firstId,satrt]is assumed to be already sorted
     * @param comparator the comparator
     */
    private void binaryInsertionSort(T[] array, int firstId, int lastId, int start,
            Comparator<T> comparator) {

        if (firstId == start) {
            start++;
        }

        while (start < lastId) {
            T pivot = array[start];

            //  set left to where start belongs 
            int left = firstId;
            int right = start;

            /* Invariants:
             * pivot >= all in [firstId, left).
             * pivot  < all in [right, start).
             * The second is vacuously true at the start.
             */
            while (left < right) {
                int middle = (left + right) >>> 1;
                if (comparator.compare(pivot, array[middle]) < 0) {
                    right = middle;
                } else {
                    left = middle + 1;
                }
            }

            /*
             * The invariants still hold, so pivot >= all in [firstId, left) and
             * pivot < all in [left, start), so pivot belongs at left.  Note
             * that if there are elements equal to pivot, left points to the
             * first slot after them -- that's why this sort is stable.
             * Slide over to make room.            
             */
            int nbElems = start - left;
            /*
             * grabbed from java7 TimSort, the swich is an optimization to 
             * arraycopy in case there are 1 or 2 elements only to copy
             */
            switch (nbElems) {
                case 2:
                    array[left + 2] = array[left + 1];
                case 1:
                    array[left + 1] = array[left];
                    break;
                default:
                    System.arraycopy(array, left, array, left + 1, nbElems);
            }
            array[left] = pivot;
            start++;
        }
    }

    /**
     * returns the minimum run length for merging
     *
     * see http://svn.python.org/projects/python/trunk/Objects/listobject.c
     * almost exact copy of merge_compute_minrun function
     *
     * If n &lt; MIN_SIZE, return n (it's too small to bother with fancy stuff).
     * Else if n is an exact power of 2, return MIN_SIZE / 2. Else return an int
     * k, MIN_SIZE / 2 &lt;= k &lt;= MIN_SIZE , such that n/k is close to, but
     * strictly less than, an exact power of 2.
     *
     * @param n length of the array
     * @return the minimum run length for
     */
    private int mergeComputeMinRun(int n) {
        int r = 0;      /* becomes 1 if any 1 bits are shifted off */
        while (n >= MIN_SIZE) {
            r |= (n & 1);
            n >>= 1;
        }
        return n + r;
    }

    /**
     * Examine the stack of runs waiting to be merged, merging adjacent runs
     * until the stack invariants are re-established:
     *
     * 1. len[-3] > len[-2] + len[-1] 2. len[-2] > len[-1]
     *
     * See http://svn.python.org/projects/python/trunk/Objects/listobject.c very
     * similar to merge_collapse
     *
     * see http://svn.python.org/projects/python/trunk/Objects/listsort.txt
     * search for The Merge Pattern
     */
    private void mergeCollapse() {
        while (nbRuns > 1) {
            int n = nbRuns - 2;
            //searching for runs to merge from the end of the stack
            if (n > 0 && runsLength[n - 1] <= runsLength[n] + runsLength[n + 1]) {
                if (runsLength[n - 1] < runsLength[n + 1]) {
                    n--;
                }
                mergeRuns(n);
            } else if (runsLength[n] <= runsLength[n + 1]) {
                mergeRuns(n);
            } else {
                break;
            }
        }
    }

    /**
     * Merge all the remaining runs to merge
     */
    private void mergeForceCollapse() {
        while (nbRuns > 1) {
            int n = nbRuns - 2;
            if (n > 0 && runsLength[n - 1] < runsLength[n + 1]) {
                n--;
            }
            mergeRuns(n);
        }
    }

    /**
     * Merge runs A and B where A index in the stack is idx and B index is idx+1
     *
     * @param idx index of the first of two runs to merge
     */
    private void mergeRuns(int idx) {

        int indexA = runsIndices[idx];
        int lenA = runsLength[idx];
        int indexB = runsIndices[idx + 1];
        int lenB = runsLength[idx + 1];

        /*
         * Record the length of the combined runs; if idx is the 3rd-last
         * run now, also slide over the last run (which isn't involved
         * in this merge).  The current run (idx+1) goes away in any case.
         */
        runsLength[idx] = lenA + lenB;
        if (idx == nbRuns - 3) {
            runsIndices[idx + 1] = runsIndices[idx + 2];
            runsLength[idx + 1] = runsLength[idx + 2];
        }
        nbRuns--;

        /* Where does B start in A?  Elements in A before that can be
         * ignored (already in place).
         */
        //didn't find proper naming for k as it's used inthe original implementation
        int k = gallopRight(array[indexB], array, indexA, lenA, 0, comparator);        
        indexA += k;
        lenA -= k;
        if (lenA == 0) {
            return;
        }

        /* Where does A end in B?  Elements in B after that can be
         * ignored (already in place).
         */
        lenB = gallopLeft(array[indexA + lenA - 1], array, indexB, lenB, lenB - 1, comparator);        
        if (lenB == 0) {
            return;
        }

        /* Merge what remains of the runs, using a temp array with
         * min(lengthA, lengthB) elements.
         */
        if (lenA <= lenB) {
            mergeLow(indexA, lenA, indexB, lenB);
        } else {
            mergeHigh(indexA, lenA, indexB, lenB);
        }
    }

    /**
     *
     * Locate the proper position of key in an array; if the array contains an
     * element equal to key, return the position immediately to the left of the
     * leftmost equal element. [gallopRight() does the same except returns the
     * position to the right of the rightmost equal element (if any).]
     *
     * @param key the key to search
     * @param array is a sorted array with n elements, starting at array[0]. n
     * must be > 0.
     * @param idx the index to start
     * @param length the length of the run
     * @param hint is an index at which to begin the search, 0 <= hint < n. The
     * closer hint is to the final result, the faster this runs.
     * @param comparator the comparator used to order the range, and to search
     * @return is the int k in 0..n such that
     *
     * array[k-1] < key <= array[k]
     *
     * pretending that *(a-1) is minus infinity and array[n] is plus infinity.
     * IOW, key belongs at index k; or, IOW, the first k elements of a should
     * precede key, and the last n-k should follow key.
     */
    private int gallopLeft(T key, T[] array, int idx, int length, int hint,
            Comparator<T> comparator) {        
        int lastOffset = 0;
        int offset = 1;
        if (comparator.compare(key, array[idx + hint]) > 0) {
            /* array[hint] < key -- gallop right, until
             * array[hint + lastOffset] < key <= array[hint + offset]
             */
            int maxOffset = length - hint;
            while (offset < maxOffset && comparator.compare(key, array[idx + hint + offset]) > 0) {
                lastOffset = offset;
                offset = (offset << 1) + 1;
                /* int overflow. 
                 * Note : not sure if that can happen but it's here in both 
                 * original and java 7 TimSort implementation
                 */
                if (offset <= 0) {
                    offset = maxOffset;
                }
            }
            if (offset > maxOffset) {
                offset = maxOffset;
            }

            // Translate back to offsets relative to idx. 
            lastOffset += hint;
            offset += hint;
        } else {
            /* key <= array[hint] -- gallop left, until
             * array[hint - offset] < key <= array[hint - lastOffset]
             */
            int maxOffset = hint + 1;
            while (offset < maxOffset && comparator.compare(key, array[idx + hint - offset]) <= 0) {
                lastOffset = offset;
                offset = (offset << 1) + 1;
                /* int overflow. 
                 * Note : not sure if that can happen but it's here in both 
                 * original and java 7 TimSort implementation
                 */
                if (offset <= 0) {
                    offset = maxOffset;
                }
            }
            if (offset > maxOffset) {
                offset = maxOffset;
            }

            // Translate back to positive offsets relative to idx.
            int k = lastOffset;
            lastOffset = hint - offset;
            offset = hint - k;
        }        

        /*
         * Now array[idx+lastOffset] < key <= array[idx+offset], so key belongs somewhere
         * to the right of lastOffset but no farther right than offset.  Do a binary
         * search, with invariant array[idx + lastOffset - 1] < key <= array[idx + offset].
         */
        lastOffset++;
        while (lastOffset < offset) {
            int m = lastOffset + ((offset - lastOffset) >>> 1);

            if (comparator.compare(key, array[idx + m]) > 0) {
                lastOffset = m + 1;  // array[idx + m] < key
            } else {
                offset = m;          // key <= array[idx + m]
            }
        }        
        return offset;
    }

    /**
     * Exactly like gallopLeft(), except that if key already exists in
     * array[0:n], finds the position immediately to the right of the rightmost
     * equal value.
     *
     * The code duplication is massive, but this is enough different given that
     * we're sticking to "<" comparisons that it's much harder to follow if
     * written as one routine with yet another "left or right?" flag.
     *
     * @param key the key to search
     * @param array is a sorted array with n elements, starting at array[0]. n
     * must be > 0.
     * @param idx the index to start
     * @param length the length of the run
     * @param hint is an index at which to begin the search, 0 <= hint < n. The
     * closer hint is to the final result, the faster this runs.
     * @param comparator the comparator used to order the range, and to search
     * @return value is the int k in 0..n such that array[k-1] <= key < array[k]
     */
    private int gallopRight(T key, T[] array, int idx, int length,
            int hint, Comparator<T> comparator) {
        
        int offset = 1;
        int lastOffset = 0;
        if (comparator.compare(key, array[idx + hint]) < 0) {
            /* key < array[hint] -- gallop left, until
             * array[hint - offset] <= key < array[hint - lastOffset]
             */
            int maxOffset = hint + 1;
            while (offset < maxOffset && comparator.compare(key, array[idx + hint - offset]) < 0) {
                lastOffset = offset;
                offset = (offset << 1) + 1;
                /* int overflow. 
                 * Note : not sure if that can happen but it's here in both 
                 * original and java 7 TimSort implementation
                 */
                if (offset <= 0) {
                    offset = maxOffset;
                }
            }
            if (offset > maxOffset) {
                offset = maxOffset;
            }

            // Translate back to offsets relative to idx. 
            int k = lastOffset;
            lastOffset = hint - offset;
            offset = hint - k;
        } else {
            /* array[hint] <= key -- gallop right, until
             * array[hint + lastOffset] <= key < array[hint + offset]
             */
            int maxOffset = length - hint;
            while (offset < maxOffset && comparator.compare(key, array[idx + hint + offset]) >= 0) {
                lastOffset = offset;
                offset = (offset << 1) + 1;
                /* int overflow. 
                 * Note : not sure if that can happen but it's here in both 
                 * original and java 7 TimSort implementation
                 */
                if (offset <= 0) {
                    offset = maxOffset;
                }
            }
            if (offset > maxOffset) {
                offset = maxOffset;
            }

            // Translate back to offsets relative to idx. 
            lastOffset += hint;
            offset += hint;
        }        

        /* Now array[lastOffset] <= key < array[offset], so key belongs somewhere to the
         * right of lastOffset but no farther right than offset.  Do a binary
         * search, with invariant array[lastOffset-1] <= key < array[offset].
         */
        lastOffset++;
        while (lastOffset < offset) {
            int m = lastOffset + ((offset - lastOffset) >>> 1);

            if (comparator.compare(key, array[idx + m]) < 0) {
                offset = m;           //key < array[idx + m] 
            } else {
                lastOffset = m + 1;  // array[idx + m] <= key 
            }
        }        
        return offset;
    }
    /**
     * Merge the lenA elements starting at idxA with the lenB elements starting
     * at idxB in a stable way, in-place. lenA and lenB must be > 0, and idxA +
     * lenA = idxB Must also have that array[idxB] < array[idxA], that
     * array[idxA+lenA - 1] belongs at the end of the merge, and should have
     * lenA <= lenB. See listsort.txt for more info.
     *
     * @param idxA index of first element in run A
     * @param lengthA length of run A
     * @param idxB index of first element in run B
     * @param lengthB length of run B
     */
    private void mergeLow(int idxA, int lenA, int idxB, int lenB) {
        
        lengthA = lenA;
        lengthB = lenB;
        iterA = 0;       // Indexes into tmp array
        iterB = idxB;   // Indexes int a
        dest = idxA;      // Indexes int a
        Comparator<T> comp = this.comparator;

        
        T[] arr = this.array;
        T[] tempArray = tmpArray;
        System.arraycopy(arr, idxA, tempArray, 0, lengthA);

        arr[dest] = arr[iterB];
        dest++;
        iterB++;
        innerMergeLow(comp, arr, tempArray);

        //minGallop shouldn't be < 1
        minGallop = minGallop < 1 ? 1 : minGallop; 

        if (lengthA == 1) {//CopyB label
            System.arraycopy(arr, iterB, arr, dest, lengthB);
            // The last element of run A belongs at the end of the merge.
            arr[dest + lengthB] = tempArray[iterA];
        } else if(lengthA == 0){
            throw new UnsupportedOperationException("Compare function result changed! " +
                                                    "Make sure you do not modify the scene from"
                                                    + " another thread and that the comparisons are not based"
                                                    + " on NaN values.");
        } else {//Fail label
            System.arraycopy(tempArray, iterA, arr, dest, lengthA);
        }
    }

    /**
     * Attempt to unroll "goto" style original implementation.
     * this method uses and change temp attributes of the class
     * @param comp comparator
     * @param arr the array
     * @param tempArray the temp array
     */
    public void innerMergeLow(Comparator<T> comp, T[] arr, T[] tempArray) {
        lengthB--;
        if (lengthB == 0 || lengthA == 1) {
            return;
        }

        while (true) {
            // Number of wins by run A
            int aWins = 0;
            // Number of wins by run B
            int bWins = 0;

            /*
             * Do the straightforward thing until (if ever) one run starts
             * winning consistently.
             */
            do {
                
                if (comp.compare(arr[iterB], tempArray[iterA]) < 0) {
                    arr[dest] = arr[iterB];
                    dest++;
                    iterB++;
                    bWins++;
                    aWins = 0;
                    lengthB--;
                    if (lengthB == 0) {
                        return;
                    }
                } else {
                    arr[dest] = tempArray[iterA];
                    dest++;
                    iterA++;
                    aWins++;
                    bWins = 0;
                    lengthA--;                    
                    if (lengthA == 1) {
                        return;
                    }
                }
            } while ((aWins | bWins) < minGallop);

            /*
             * One run is winning so consistently that galloping may be a
             * huge win. So try that, and continue galloping until (if ever)
             * neither run appears to be winning consistently anymore.
             */
            do {                
                aWins = gallopRight(arr[iterB], tempArray, iterA, lengthA, 0, comp);
                if (aWins != 0) {
                    System.arraycopy(tempArray, iterA, arr, dest, aWins);
                    dest += aWins;
                    iterA += aWins;
                    lengthA -= aWins;
                    /* lengthA==0 is impossible now if the comparison
                    * function is consistent, but we can't assume
                    * that it is.
                    * a propper error will be thrown in mergeLow if lengthA == 0
                    */
                    if (lengthA <= 1){
                        return;
                    }
                }
                arr[dest] = arr[iterB];
                dest++;
                iterB++;
                lengthB--;
                if (lengthB == 0) {
                    return;
                }

                bWins = gallopLeft(tempArray[iterA], arr, iterB, lengthB, 0, comp);
                if (bWins != 0) {
                    System.arraycopy(arr, iterB, arr, dest, bWins);
                    dest += bWins;
                    iterB += bWins;
                    lengthB -= bWins;
                    if (lengthB == 0) {
                        return;
                    }
                }
                arr[dest] = tempArray[iterA];
                dest++;
                iterA++;
                lengthA--;
                if (lengthA == 1) {
                    return;
                }
                minGallop--;
            } while (aWins >= MIN_GALLOP || bWins >= MIN_GALLOP);
            if (minGallop < 0) {
                minGallop = 0;
            }
            //original implementation uses +1 to penalize, Java7 Timsort uses +2
            minGallop += 2;  // Penalize for leaving gallop mode
        }  
    }

    /**
     * Merge the lenA elements starting at idxA with the lenB elements starting
     * at idxB in a stable way, in-place. lenA and lenBb must be > 0, and idxA +
     * lenAa == idxB. Must also have that array[idxB] < array[idxA], that
     * array[idxA + Len1 - 1] belongs at the end of the merge, and should have
     * lenA >= lenB. See listsort.txt for more info.
     *
     * @param idxA index of first element in run A
     * @param lengthA length of run A
     * @param idxB index of first element in run B
     * @param lengthB length of run B
     */
    private void mergeHigh(int idxA, int lenA, int idxB, int lenB) {
        

        lengthA = lenA;
        lengthB = lenB;
        iterA = idxA + lengthA - 1;
        iterB = lengthB - 1;
        dest = idxB + lengthB - 1;
        Comparator<T> comp = this.comparator; 
        
        T[] arr = this.array;
        T[] tempArray = tmpArray;
        System.arraycopy(arr, idxB, tempArray, 0, lengthB);

        arr[dest] = arr[iterA];
        dest--;
        iterA--;
        innerMergeHigh(comp, tempArray, arr, idxA);
        //minGallop shouldn't be < 1;
        minGallop = minGallop < 1 ? 1 : minGallop; 

        if (lengthB == 1) {//CopyA label            
            dest -= lengthA;
            iterA -= lengthA;
            System.arraycopy(arr, iterA + 1, arr, dest + 1, lengthA);
            // The first element of run B belongs at the front of the merge. 
            arr[dest] = tempArray[iterB];  
        } else if (lengthB == 0) {
              throw new UnsupportedOperationException("Compare function result changed! " +
                                                      "Make sure you do not modify the scene from another thread!");
        } else {//Fail label                        
            System.arraycopy(tempArray, 0, arr, dest - (lengthB - 1), lengthB);
        }
    }
    
    /**
     * Attempt to unroll "goto" style original implementation.
     * this method uses and change temp attributes of the class
     * @param comp comparator
     * @param arr the array
     * @param tempArray the temp array
     * @param idxA the index of the first element of run A
     */
    public void innerMergeHigh(Comparator<T> comp, T[] tempArray, T[] arr, int idxA) {
        lengthA--;
        if (lengthA == 0 || lengthB == 1) {
            return;
        }
        if (lengthB == 1) {
            return;
        }
        while (true) {
            // Number of wins by run A
            int aWins = 0;
            // Number of wins by run B
            int bWins = 0;

            /*
             * Do the straightforward thing until (if ever) one run
             * appears to win consistently.
             */
            do {                
                if (comp.compare(tempArray[iterB], arr[iterA]) < 0) {
                    arr[dest] = arr[iterA];
                    dest--;
                    iterA--;
                    aWins++;
                    bWins = 0;
                    lengthA --;
                    if (lengthA == 0) {
                        return;
                    }
                } else {
                    arr[dest] = tempArray[iterB];
                    dest--;
                    iterB--;
                    bWins++;
                    aWins = 0;
                    lengthB--;
                    if (lengthB == 1) {
                        return;
                    }
                }
            } while ((aWins | bWins) < minGallop);

            /*
             * One run is winning so consistently that galloping may be a
             * huge win. So try that, and continue galloping until (if ever)
             * neither run appears to be winning consistently anymore.
             */
            do {                
                aWins = lengthA - gallopRight(tempArray[iterB], arr, idxA, lengthA, lengthA - 1, comp);
                if (aWins != 0) {
                    dest -= aWins;
                    iterA -= aWins;
                    lengthA -= aWins;
                    System.arraycopy(arr, iterA + 1, arr, dest + 1, aWins);
                    if (lengthA == 0) {
                        return;
                    }
                }
                arr[dest] = tempArray[iterB];
                dest--;
                iterB--;
                lengthB--;
                if (lengthB == 1) {
                    return;
                }

                bWins = lengthB - gallopLeft(arr[iterA], tempArray, 0, lengthB, lengthB - 1, comp);
                if (bWins != 0) {
                    dest -= bWins;
                    iterB -= bWins;
                    lengthB -= bWins;
                    System.arraycopy(tempArray, iterB + 1, arr, dest + 1, bWins);
                    /* lengthB==0 is impossible now if the comparison
                    * function is consistent, but we can't assume
                    * that it is.
                    * a propper error will be thrown in mergeLow if lengthB == 0
                    */
                    if (lengthB <= 1){
                        return;
                    }
                }
                arr[dest] = arr[iterA];     
                dest--;
                iterA--;
                lengthA--;
                if (lengthA == 0) {
                    return;
                }
                minGallop--;
            } while (aWins >= MIN_GALLOP || bWins >= MIN_GALLOP);
            if (minGallop < 0) {
                minGallop = 0;
            }
            //original implementation uses +1 to penalize, Java7 Timsort uses +2
            minGallop += 2;  // Penalize for leaving gallop mode
        }
    }

    /**
     * Reverse an array from firstId to lastId
     *
     * @param array the array to reverse
     * @param firstId the index where to start to reverse
     * @param lastId the index where to stop to reverse
     */
    private static void reverseArray(Object[] array, int firstId, int lastId) {
        lastId--;
        while (firstId < lastId) {
            Object o = array[firstId];
            array[firstId] = array[lastId];
            array[lastId] = o;
            firstId++;
            lastId--;
        }        
    }
    
     /**
     * return the useful length of the array being sorted
     * @return the length pass to the last allocateStack method
     */
    public int getLength() {
        return length;
    }
    
    /*
     * test case
     */
    public static void main(String[] argv) {
        Integer[] arr = new Integer[]{5, 6, 2, 9, 10, 11, 12, 8, 3, 12, 3, 7, 12, 32, 458, 12, 5, 3, 78, 45, 12, 32, 58, 45, 65, 45, 98, 45, 65, 2, 3, 47, 21, 35};
        ListSort ls = new ListSort();
        ls.allocateStack(34);
        ls.sort(arr, new Comparator<Integer>() {
            public int compare(Integer o1, Integer o2) {
                int x = o1 - o2;
                return (x == 0) ? 0 : (x > 0) ? 1 : -1;
            }
        });
        for (Integer integer : arr) {
            System.err.print(integer + ",");
        }
        System.err.println();
    }

   
    
}


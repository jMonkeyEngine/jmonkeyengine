package com.jme3.util.collections;

public class PowerSort {

    private static final int MIN_MERGE = 32;
    private static final int MAX_RUNS = 64;

    /**
     *
     * @param array
     * @param fromIndex first element to sort
     * @param toIndex index after the last element to sort
     * @param comparator
     */
    public static void sort(int[] array, int fromIndex, int toIndex, IntComparator comparator) {
        sort(array, fromIndex, toIndex, comparator, null);
    }

    /**
     * Sorts like {@link #sort(int[], int, int, IntComparator)}, but uses {@code scratch} as working
     * memory instead of allocating it, so repeated sorts allocate nothing.
     *
     * @param scratch working memory of at least {@link #scratchLength(int) scratchLength(toIndex - fromIndex)}
     *                elements, or null to allocate it if needed. Its contents are overwritten.
     * @throws IllegalArgumentException if {@code scratch} is too short
     */
    public static void sort(int[] array, int fromIndex, int toIndex, IntComparator comparator, int[] scratch) {
        if (fromIndex < 0 || toIndex > array.length || fromIndex > toIndex) {
            throw new IndexOutOfBoundsException("from " + fromIndex + ", to " + toIndex + ", size " + array.length);
        }
        int n = toIndex - fromIndex;
        if (n < 2) {
            return;
        }
        if (n < MIN_MERGE) {
            int run = countRunAndMakeAscending(array, fromIndex, toIndex, comparator);
            binaryInsertionSort(array, fromIndex, toIndex, fromIndex + run, comparator);
            return;
        }
        int required = scratchLength(n);
        if (scratch == null) {
            scratch = new int[required];
        } else if (scratch.length < required) {
            throw new IllegalArgumentException("Scratch length " + scratch.length + " is less than " + required + ".");
        }
        int minRun = minRunLength(n);
        // a merge only buffers the smaller of its two runs, which is never more than half the range,
        // so scratch[0, n / 2) is the merge buffer and the run stack is stored after it: the base,
        // length, and power (of the boundary between run i and run i + 1) of each pending run.
        // Powers strictly increase up the stack and are at most ~log2(n) + 1, so the stack stays small.
        int baseOff = n >>> 1, lenOff = baseOff + MAX_RUNS, powerOff = lenOff + MAX_RUNS;
        int runs = 0;
        for (int lo = fromIndex; lo < toIndex;) {
            int len = countRunAndMakeAscending(array, lo, toIndex, comparator);
            if (len < minRun) {
                int force = Math.min(minRun, toIndex - lo);
                binaryInsertionSort(array, lo, lo + force, lo + len, comparator);
                len = force;
            }
            if (runs > 0) {
                // powers are relative to the range, as if it were the whole list
                int power = nodePower(scratch[baseOff + runs - 1] - fromIndex, scratch[lenOff + runs - 1], len, n);
                while (runs > 1 && scratch[powerOff + runs - 2] > power) {
                    merge(array, scratch[baseOff + runs - 2], scratch[lenOff + runs - 2], scratch[lenOff + runs - 1], scratch, comparator);
                    scratch[lenOff + runs - 2] += scratch[lenOff + runs - 1];
                    runs--;
                }
                scratch[powerOff + runs - 1] = power;
            }
            scratch[baseOff + runs] = lo;
            scratch[lenOff + runs] = len;
            runs++;
            lo += len;
        }
        while (runs > 1) {
            merge(array, scratch[baseOff + runs - 2], scratch[lenOff + runs - 2], scratch[lenOff + runs - 1], scratch, comparator);
            scratch[lenOff + runs - 2] += scratch[lenOff + runs - 1];
            runs--;
        }
    }

    /**
     * Gets the minimum length of the scratch buffer {@link #sort(int[], int, int, IntComparator, int[])}
     * needs to sort {@code n} elements.
     *
     * @param n number of elements to sort
     * @return minimum scratch length
     */
    public static int scratchLength(int n) {
        return n < MIN_MERGE ? 0 : (n >>> 1) + 3 * MAX_RUNS;
    }

    /**
     * Computes the power of the boundary between the adjacent runs {@code [s1, s1 + n1)} and
     * {@code [s1 + n1, s1 + n1 + n2)}: the depth of the first level at which the midpoints of
     * the two runs, as fractions of {@code n}, fall into different halves.
     */
    private static int nodePower(int s1, int n1, int n2, int n) {
        long a = 2L * s1 + n1; // twice the first run's midpoint
        long b = a + n1 + n2;  // twice the second run's midpoint
        int power = 0;
        while (true) {
            power++;
            if (a >= n) {
                a -= n;
                b -= n;
            } else if (b >= n) {
                return power;
            }
            a <<= 1;
            b <<= 1;
        }
    }

    /**
     * Gets the minimum run length for {@code n} elements, such that {@code n / minRun} is
     * equal to or slightly less than a power of two.
     */
    private static int minRunLength(int n) {
        int r = 0;
        while (n >= MIN_MERGE) {
            r |= n & 1;
            n >>= 1;
        }
        return n + r;
    }

    /**
     * Gets the length of the run starting at {@code lo}, reversing it first if it is strictly
     * descending (strictly, so reversing never reorders equal elements).
     */
    private static int countRunAndMakeAscending(int[] a, int lo, int hi, IntComparator c) {
        int end = lo + 1;
        if (end == hi) {
            return 1;
        }
        if (c.compare(a[end++], a[lo]) < 0) {
            while (end < hi && c.compare(a[end], a[end - 1]) < 0) {
                end++;
            }
            for (int i = lo, j = end - 1; i < j; i++, j--) {
                int t = a[i];
                a[i] = a[j];
                a[j] = t;
            }
        } else {
            while (end < hi && c.compare(a[end], a[end - 1]) >= 0) {
                end++;
            }
        }
        return end - lo;
    }

    /**
     * Sorts {@code [lo, hi)}, of which {@code [lo, start)} is already sorted.
     */
    private static void binaryInsertionSort(int[] a, int lo, int hi, int start, IntComparator c) {
        for (; start < hi; start++) {
            int pivot = a[start];
            int pos = upperBound(a, lo, start, pivot, c);
            System.arraycopy(a, pos, a, pos + 1, start - pos);
            a[pos] = pivot;
        }
    }

    /**
     * Stably merges the adjacent sorted runs {@code [base1, base1 + len1)} and
     * {@code [base1 + len1, base1 + len1 + len2)}.
     */
    private static void merge(int[] a, int base1, int len1, int len2, int[] tmp, IntComparator c) {
        int base2 = base1 + len1;
        // elements of the first run not greater than the second run's first element are already in place
        int start = upperBound(a, base1, base2, a[base2], c);
        len1 = base2 - start;
        if (len1 == 0) {
            return;
        }
        // elements of the second run not less than the first run's last element are already in place
        len2 = lowerBound(a, base2, base2 + len2, a[base2 - 1], c) - base2;
        if (len1 <= len2) {
            mergeLo(a, start, len1, len2, tmp, c);
        } else {
            mergeHi(a, start, len1, len2, tmp, c);
        }
    }

    /**
     * Merges by buffering the first run and filling from the front. Ties take the first run's
     * element, which keeps the merge stable.
     */
    private static void mergeLo(int[] a, int base1, int len1, int len2, int[] tmp, IntComparator c) {
        System.arraycopy(a, base1, tmp, 0, len1);
        int i = 0, j = base1 + len1, end = j + len2, dest = base1;
        while (i < len1 && j < end) {
            a[dest++] = c.compare(a[j], tmp[i]) < 0 ? a[j++] : tmp[i++];
        }
        System.arraycopy(tmp, i, a, dest, len1 - i);
    }

    /**
     * Merges by buffering the second run and filling from the back. Ties take the second run's
     * element, which keeps the merge stable.
     */
    private static void mergeHi(int[] a, int base1, int len1, int len2, int[] tmp, IntComparator c) {
        int base2 = base1 + len1;
        System.arraycopy(a, base2, tmp, 0, len2);
        int i = len2 - 1, j = base2 - 1, dest = base2 + len2 - 1;
        while (i >= 0 && j >= base1) {
            a[dest--] = c.compare(tmp[i], a[j]) < 0 ? a[j--] : tmp[i--];
        }
        System.arraycopy(tmp, 0, a, base1, i + 1);
    }

    /**
     * Gets the first index in sorted {@code [lo, hi)} whose element is greater than {@code key}.
     */
    private static int upperBound(int[] a, int lo, int hi, int key, IntComparator c) {
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (c.compare(key, a[mid]) < 0) {
                hi = mid;
            } else {
                lo = mid + 1;
            }
        }
        return lo;
    }

    /**
     * Gets the first index in sorted {@code [lo, hi)} whose element is not less than {@code key}.
     */
    private static int lowerBound(int[] a, int lo, int hi, int key, IntComparator c) {
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (c.compare(a[mid], key) < 0) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        return lo;
    }

}

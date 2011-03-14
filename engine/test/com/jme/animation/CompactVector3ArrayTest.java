package com.jme.animation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.jme3.animation.CompactVector3Array;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.export.binary.BinaryImporter;
import com.jme3.math.Vector3f;

public class CompactVector3ArrayTest {
    private final Vector3f[] objArray1 = new Vector3f[] {
        new Vector3f(1, 0, 1),  // 0
        new Vector3f(1, 1, 1),  // 1 
        new Vector3f(0, 1, 1),  // 2    
        new Vector3f(1, 1, 1),  // 1 
        new Vector3f(1, 0, 1),  // 0
    };
    private final Vector3f[] objArray2 = new Vector3f[] {
            new Vector3f(1, 0, 2),  // 3
            new Vector3f(1, 1, 1),  // 1
            new Vector3f(0, 1, 1),  // 2
            null,                   // -1
            new Vector3f(1, 0, 2),  // 3
    };
    private static final int[] index1 = new int[] {0, 1, 2, 1, 0};
    private static final int[] index2 = new int[] {3, 1, 2, -1, 3};
    private int[] index12;
    private static final float[] serialData = new float[] {1, 0, 1, 1, 1, 1, 0, 1, 1, 1, 0, 2};
    
    CompactVector3Array compact;

    @Before
    public void setUp() throws Exception {
        compact = new CompactVector3Array();
        index12 = Arrays.copyOf(index1, index1.length+index2.length);
        System.arraycopy(index2, 0, index12, index1.length, index2.length);
    }

    @Test
    public void testCompactVector3ArrayAdd() {
        compact.add(objArray1);
        compact.add(objArray2);
        _testAdd();
        
        try {
            compact.freeze();
            compact.add(objArray1);
            fail();
        } catch (Exception e) {
        }
    }

    private void _testAdd() {
        assertTrue(Arrays.equals(compact.getIndex(objArray1), index1));
        assertTrue(Arrays.equals(compact.getIndex(objArray2), index2));
        assertTrue(Arrays.equals(compact.getSerializedData(), serialData));
    }
    
    @Test
    public void testCompactVector3ArrayFloatArrayIntArray() {
        int[] indexArray = index1;
        float[] dataArray = new float[] {1, 0, 1, 1, 1, 1, 0, 1, 1};
        Vector3f[] objArray = new Vector3f[] {
                new Vector3f(1, 0, 1),  
                new Vector3f(1, 1, 1),  
                new Vector3f(0, 1, 1),  
                new Vector3f(1, 1, 1),  
                new Vector3f(1, 0, 1),  
        };
        CompactVector3Array compact = new CompactVector3Array(dataArray, indexArray);
        assertTrue(Arrays.deepEquals(compact.toObjectArray(), objArray));
    }
    
    @Test
    public void testGetTotalObjectSize() {
        compact.add(objArray1);
        assertTrue(compact.getTotalObjectSize() == 5);
        assertTrue(compact.getCompactObjectSize() == 3);
        compact.add(objArray2);
        _testSize();
    }

    private void _testSize() {
        assertTrue(compact.getTotalObjectSize() == 10);
        assertTrue(compact.getCompactObjectSize() == 4);
    }
    
    @Test
    public void testGet() {
        compact.add(objArray1);
        Vector3f v1 = compact.get(1, new Vector3f());
        assertEquals(new Vector3f(1, 1, 1), v1);
        compact.add(objArray2);
        _testGet();
    }

    private void _testGet() {
        Vector3f v2 = compact.get(1, new Vector3f());
        assertEquals(new Vector3f(1, 1, 1), v2);
        Vector3f v3 = compact.get(5, new Vector3f());
        assertEquals(new Vector3f(1, 0, 2), v3);
    }
    
    @Test
    public void testGetCompactIndex() {
        compact.add(objArray1);
        compact.add(objArray2);
        _testCompactIndex();
    }

    private void _testCompactIndex() {
        for (int i = 0; i < index12.length; i++) {
            assertEquals(index12[i], compact.getCompactIndex(i));
        }
    }
    
    @Test
    public void testGetIndex() {
        compact.add(objArray1);
        compact.add(objArray2);
        _testGetIndex();
    }

    private void _testGetIndex() {
        Vector3f[] reverse = new Vector3f[objArray1.length];
        int[] reverseIndex = new int[objArray1.length];
        for (int i = 0; i < objArray1.length; i++) {
            reverse[i] = objArray1[objArray1.length-1-i];
            reverseIndex[i] = index1[objArray1.length-1-i];
        }
        
        int[] index = compact.getIndex(reverse);
        for (int i = 0; i < index.length; i++) {
            assertEquals(reverseIndex[i], index[i]);
        }
    }
    
    @Test
    public void testRead() throws IOException {
        File file = File.createTempFile("compactArray", "test"); 
        BinaryImporter importer = new BinaryImporter();
        BinaryExporter exporter = new BinaryExporter();
        compact.add(objArray1);
        compact.add(objArray2);
        exporter.save(compact, file);
        compact = (CompactVector3Array) importer.load(file);
        _testSize();
        _testCompactIndex();
        _testGet();
        file.delete();
    }
}

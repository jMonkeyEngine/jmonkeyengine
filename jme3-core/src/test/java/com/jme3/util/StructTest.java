package com.jme3.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;

import com.jme3.shader.bufferobject.BufferObject;
import com.jme3.shader.bufferobject.BufferRegion;
import com.jme3.shader.bufferobject.DirtyRegionsIterator;
import com.jme3.shader.bufferobject.layout.Std140Layout;
import com.jme3.util.struct.*;
import com.jme3.util.struct.fields.*;

import org.junit.Test;

public class StructTest {

    static class SubStruct implements Struct {
        @Member(value = 0) public int subIntField0 = 100;
        @Member(value = 1) public float subFloatField1 = 100f;
    }

    @Consistent
    static class TestStruct implements Struct {
        @Member(value = 0) public int intField0 = 100;
        @Member(value = 1) public float floatField1 = 100f;
        @Member(value = 2) public final float[] floatFieldArray2 = new float[] { 100f, 200f, 300f };
        @Member(value = 3) public SubStruct structField3 = new SubStruct();
        @Member(value = 4) public final SubStruct[] structArrayField5 = new SubStruct[] { new SubStruct(), new SubStruct() };
        @Member(value = 5) public boolean boolField6 = true;
    }

    @Test
    public void testFieldsExtraction() {
        TestStruct test = new TestStruct();
        java.util.List<StructField<?>> fields = StructUtils.getFields(test);
        String checkString = "";
        for (StructField<?> f : fields) {
            String s = f.getPosition() + "\n";
            checkString += s;
        }
        String expectedString = "0 intField0 0\n1 floatField1 0\n2 floatFieldArray2 0\n3 subIntField0 1\n4 subFloatField1 1\n5 subIntField0 1\n6 subFloatField1 1\n7 subIntField0 1\n8 subFloatField1 1\n9 boolField6 0\n";
        assertEquals(expectedString, checkString);
    }

    @Test
    public void testStd140Serialization() {
        TestStruct test = new TestStruct();
        java.util.List<StructField<?>> fields = StructUtils.getFields(test);

        Std140Layout layout = new Std140Layout();
        BufferObject bo = new BufferObject();
        bo.setRegions(layout.generateFieldRegions(fields));
        System.out.println(bo.getData().getInt());
        
        StructUtils.updateBufferData(fields, false, layout, bo);

        ByteBuffer bbf = bo.getData();

        String expectedData = "100 0 0 0 0 0 -56 66 0 0 0 0 0 0 0 0 0 0 -56 66 0 0 0 0 0 0 0 0 0 0 0 0 0 0 72 67 0 0 0 0 0 0 0 0 0 0 0 0 0 0 -106 67 0 0 0 0 0 0 0 0 0 0 0 0 100 0 0 0 0 0 -56 66 0 0 0 0 0 0 0 0 100 0 0 0 0 0 -56 66 0 0 0 0 0 0 0 0 100 0 0 0 0 0 -56 66 0 0 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 ";
        String actualData = "";
        while (bbf.hasRemaining()) {
            actualData += bbf.get() + " ";
        }
        assertEquals(expectedData, actualData);
    }

    @Test
    public void testStd140PartialUpdate() {
        TestStruct test = new TestStruct();

        Std140Layout layout = new Std140Layout();
        BufferObject bo = new BufferObject();

        java.util.List<StructField<?>> fields = StructUtils.getFields(test);
        bo.setRegions(layout.generateFieldRegions(fields));
        int bolength = bo.getData().limit();
        assertEquals(128, bolength);
        assertEquals(128, bo.getData().capacity());
        
        int nUpdated;

        // Update full
        System.out.println("Test Full Update");
        StructUtils.updateBufferData(fields, false, layout, bo);
        DirtyRegionsIterator dirtyI = bo.getDirtyRegions();
        nUpdated = 0;
        assertTrue(bo.isUpdateNeeded());
        while (true) {
            BufferRegion region = dirtyI.next();
            if (region == null) break;
            int start = region.getStart();
            int end = region.getEnd();
            System.out.println("Update from " + start + " to " + end + " in buffer of length " + bolength);
            assertEquals(0, start);
            assertEquals(127,end);
            assertTrue(region.isFullBufferRegion());
            assertTrue(region.isDirty());
            region.clearDirty();
            assertFalse(region.isDirty());
            nUpdated++;
        }
        bo.clearUpdateNeeded();
        assertFalse(bo.isUpdateNeeded());
        assertEquals(1, nUpdated);


        // Update nothing
        System.out.println("Test No Update");
        fields = StructUtils.getFields(test);
        StructUtils.updateBufferData(fields, false, layout, bo);
        dirtyI = bo.getDirtyRegions();
        assertFalse(bo.isUpdateNeeded());
        nUpdated = 0;
        while (true) {
            BufferRegion region = dirtyI.next();
            if (region == null) break;

            assertFalse("Update not expected", true);
            nUpdated++;
        }
        bo.clearUpdateNeeded();
        assertFalse(bo.isUpdateNeeded());
        assertEquals(0, nUpdated);

        // Update something
        System.out.println("Test Partial Update");
        test.floatField1 = 2f;
        StructUtils.updateBufferData(fields, false, layout, bo);
        dirtyI = bo.getDirtyRegions();
        assertTrue(bo.isUpdateNeeded());
        nUpdated = 0;
        while (true) {
            BufferRegion region = dirtyI.next();
            if (region == null) break;
            assertTrue(region.isDirty());

            int start = region.getStart();
            int end = region.getEnd();
            System.out.println("Update from " + start + " to " + end + " in buffer of length " + bolength);
            assertEquals(4, start);
            assertEquals(7, end);
            assertFalse(region.isFullBufferRegion());
            assertTrue(region.isDirty());
            region.clearDirty();
            nUpdated++;
        }
        bo.clearUpdateNeeded();
        assertFalse(bo.isUpdateNeeded());
        assertEquals(1, nUpdated);

        // Update something2
        System.out.println("Test Partial Update 2");
        test.floatField1 = 2f;
        test.boolField6 = true;
        StructUtils.updateBufferData(fields, false, layout, bo);
        dirtyI = bo.getDirtyRegions();
        assertTrue(bo.isUpdateNeeded());
        nUpdated = 0;
        while (true) {
            BufferRegion region = dirtyI.next();
            if (region == null) break;
            assertTrue(region.isDirty());

            int start = region.getStart();
            int end = region.getEnd();
            System.out.println("Update from " + start + " to " + end + " in buffer of length " + bolength);
            if (nUpdated == 0) {
                assertEquals(4, start);
                assertEquals(7, end);
            } else {
                assertEquals(112, start);
                assertEquals(127, end);
            }
            assertFalse(region.isFullBufferRegion());
            assertTrue(region.isDirty());
            region.clearDirty();
            nUpdated++;
        }
        bo.clearUpdateNeeded();
        assertFalse(bo.isUpdateNeeded());
        assertEquals(2, nUpdated);


        // Update substruct 
        System.out.println("Test Partial Update Substruct");
        test.structField3.subIntField0 = 3;
        StructUtils.updateBufferData(fields, false, layout, bo);
        dirtyI = bo.getDirtyRegions();
        assertTrue(bo.isUpdateNeeded());
        nUpdated = 0;
        while (true) {
            BufferRegion region = dirtyI.next();
            if (region == null) break;
            assertTrue(region.isDirty());

            int start = region.getStart();
            int end = region.getEnd();
            System.out.println("Update from " + start + " to " + end + " in buffer of length " + bolength);
            assertEquals(64, start);
            assertEquals(67, end);
            assertFalse(region.isFullBufferRegion());
            assertTrue(region.isDirty());
            region.clearDirty();
            nUpdated++;
        }
        bo.clearUpdateNeeded();
        assertFalse(bo.isUpdateNeeded());
        assertEquals(1, nUpdated);




    }
}

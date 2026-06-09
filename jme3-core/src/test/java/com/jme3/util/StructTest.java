package com.jme3.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import com.jme3.export.binary.BinaryExporter;
import com.jme3.export.binary.BinaryImporter;
import com.jme3.shader.bufferobject.BufferObject;
import com.jme3.shader.bufferobject.BufferRegion;
import com.jme3.shader.bufferobject.DirtyRegionsIterator;
import com.jme3.shader.bufferobject.layout.Std140Layout;
import com.jme3.shader.bufferobject.layout.Std430Layout;
import com.jme3.util.struct.Struct;
import com.jme3.util.struct.StructStd430BufferObject;
import com.jme3.util.struct.StructField;
import com.jme3.util.struct.StructUtils;
import com.jme3.util.struct.fields.*;

import org.junit.jupiter.api.Test;

public class StructTest {
    static class SubStruct implements Struct {
        public final IntField subIntField0 = new IntField(0, "subIntField0", 100);
        public final FloatField subFloatField1 = new FloatField(1, "subFloatField1", 100f);

    }

    static class TestStruct implements Struct {
        public final IntField intField0 = new IntField(0, "intField0", 100);
        public final FloatField floatField1 = new FloatField(1, "floatField1", 100f);
        public final FloatArrayField floatFieldArray2 = new FloatArrayField(2, "floatFieldArray2", new Float[] { 100f, 200f, 300f });
        public final SubStructField<SubStruct> structField3 = new SubStructField<SubStruct>(3, "structField3", new SubStruct());
        public final SubStructArrayField<SubStruct> structArrayField5 = new SubStructArrayField<SubStruct>(5, "structArrayField5", new SubStruct[] { new SubStruct(), new SubStruct() });
        public final BooleanField boolField6 = new BooleanField(6, "boolField6", true);
    }

    static class PackedArrayStruct implements Struct {
        public final FloatArrayField values = new FloatArrayField(0, "values", new Float[] { 1f, 2f, 3f });
        public final FloatField tail = new FloatField(1, "tail", 4f);
    }

    public static class SerializablePackedArrayStruct implements Struct {
        public final FloatArrayField values = new FloatArrayField(0, "values", new Float[] { 1f, 2f, 3f });
        public final FloatField tail = new FloatField(1, "tail", 4f);
    }

    static class ConstantHashSubStruct implements Struct {
        public final IntField intField = new IntField(0, "intField", 100);
        public final FloatField floatField = new FloatField(1, "floatField", 100f);

        @Override
        public int hashCode() {
            return 1;
        }
    }

    static class ConstantHashStructArray implements Struct {
        public final SubStructArrayField<ConstantHashSubStruct> structs =
                new SubStructArrayField<>(0, "structs",
                        new ConstantHashSubStruct[] { new ConstantHashSubStruct(), new ConstantHashSubStruct() });
    }

    @Test
    public void testFieldsExtraction() {
        TestStruct test = new TestStruct();
        java.util.List<StructField<?>> fields = StructUtils.getFields(test);
        String checkString = "";
        for (StructField<?> f : fields) {
            String s = f.getPosition() + " " + f.getName() + " " + f.getDepth() + "\n";
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
        StructUtils.setStd140BufferLayout(fields, layout, bo);
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
        StructUtils.setStd140BufferLayout(fields, layout, bo);
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

            assertFalse(true, "Update not expected");
            nUpdated++;
        }
        bo.clearUpdateNeeded();
        assertFalse(bo.isUpdateNeeded());
        assertEquals(0, nUpdated);

        // Update something
        System.out.println("Test Partial Update");
        test.floatField1.setValue(2f);
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
        test.floatField1.setValue(2f);
        test.boolField6.setValue(true);
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
        test.structField3.getValue().subIntField0.setValue(3);
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

    @Test
    public void testStd430ArrayPacking() {
        PackedArrayStruct test = new PackedArrayStruct();
        java.util.List<StructField<?>> fields = StructUtils.getFields(test);

        BufferObject std140Bo = new BufferObject();
        StructUtils.setBufferLayout(fields, new Std140Layout(), std140Bo);
        assertEquals(64, std140Bo.getData().limit());
        assertEquals(0, std140Bo.getRegion(0).getStart());
        assertEquals(47, std140Bo.getRegion(0).getEnd());
        assertEquals(48, std140Bo.getRegion(1).getStart());
        assertEquals(63, std140Bo.getRegion(1).getEnd());

        BufferObject std430Bo = new BufferObject();
        StructUtils.setBufferLayout(fields, new Std430Layout(), std430Bo);
        assertEquals(16, std430Bo.getData().limit());
        assertEquals(0, std430Bo.getRegion(0).getStart());
        assertEquals(11, std430Bo.getRegion(0).getEnd());
        assertEquals(12, std430Bo.getRegion(1).getStart());
        assertEquals(15, std430Bo.getRegion(1).getEnd());

        StructUtils.updateBufferData(fields, false, new Std430Layout(), std430Bo);
        ByteBuffer data = std430Bo.getData();
        assertEquals(1f, data.getFloat(0));
        assertEquals(2f, data.getFloat(4));
        assertEquals(3f, data.getFloat(8));
        assertEquals(4f, data.getFloat(12));
    }

    @Test
    public void testStd430BufferObjectSerializationRestoresLayout() throws IOException {
        StructStd430BufferObject bo = new StructStd430BufferObject(new SerializablePackedArrayStruct());

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        BinaryExporter.getInstance().save(bo, output);
        StructStd430BufferObject copy = (StructStd430BufferObject) BinaryImporter.getInstance()
                .load(new ByteArrayInputStream(output.toByteArray()));

        assertEquals(16, copy.getData().limit());
        assertEquals(0, copy.getRegion(0).getStart());
        assertEquals(11, copy.getRegion(0).getEnd());
        assertEquals(12, copy.getRegion(1).getStart());
        assertEquals(15, copy.getRegion(1).getEnd());
    }

    @Test
    public void testStructArrayLayoutDoesNotDependOnHashCode() {
        ConstantHashStructArray test = new ConstantHashStructArray();
        java.util.List<StructField<?>> fields = StructUtils.getFields(test);

        BufferObject bo = new BufferObject();
        StructUtils.setBufferLayout(fields, new Std140Layout(), bo);

        assertEquals(0, bo.getRegion(0).getStart());
        assertEquals(3, bo.getRegion(0).getEnd());
        assertEquals(4, bo.getRegion(1).getStart());
        assertEquals(15, bo.getRegion(1).getEnd());
        assertEquals(16, bo.getRegion(2).getStart());
        assertEquals(19, bo.getRegion(2).getEnd());
        assertEquals(20, bo.getRegion(3).getStart());
        assertEquals(31, bo.getRegion(3).getEnd());
    }
}

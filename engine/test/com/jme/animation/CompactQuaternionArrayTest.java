package com.jme.animation;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.jme3.animation.CompactQuaternionArray;
import com.jme3.math.Quaternion;

public class CompactQuaternionArrayTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testCompactQuaternionArrayQuaternionArray() {
		Quaternion[] objArray = new Quaternion[] {
			new Quaternion(1, 0, 1, 1),	
			new Quaternion(1, 1, 1, 0),	
			new Quaternion(0, 1, 1, 0),	
			new Quaternion(1, 1, 1, 0),	
			new Quaternion(1, 0, 1, 1),	
		};
		CompactQuaternionArray compact = new CompactQuaternionArray();
		compact.add(objArray);
		assertTrue(Arrays.equals(compact.getIndex(objArray), new int[] {0, 1, 2, 1, 0}));
		assertTrue(Arrays.equals(compact.getSerializedData(), new float[] {1, 0, 1, 1, 1, 1, 1, 0, 0, 1, 1, 0}));
	}

	@Test
	public void testCompactQuaternionArrayDoubleArrayIntArray() {
		int[] indexArray = new int[] {0, 1, 2, 1, 0};
		float[] dataArray = new float[] {1, 0, 1, 1, 1, 1, 1, 0, 0, 1, 1, 0};
		Quaternion[] objArray = new Quaternion[] {
				new Quaternion(1, 0, 1, 1),	
				new Quaternion(1, 1, 1, 0),	
				new Quaternion(0, 1, 1, 0),	
				new Quaternion(1, 1, 1, 0),	
				new Quaternion(1, 0, 1, 1),	
		};
		CompactQuaternionArray compact = new CompactQuaternionArray(dataArray, indexArray);
		assertTrue(Arrays.deepEquals(compact.toObjectArray(), objArray));
	}
}

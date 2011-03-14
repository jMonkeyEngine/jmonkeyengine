package com.jme3.font;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ColorTagsTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testGetPureText() {
        String str1 = "abcde test";
        ColorTags tag1 = new ColorTags(str1);
        assertEquals(tag1.getPlainText(), str1);
        
        String str2 = "abcde\\#1A3d#test\\#1A3#";
        ColorTags tag2 = new ColorTags(str2);
        assertEquals("abcdetest", tag2.getPlainText());
    }

    @Test
    public void testGetTags() {
        String str1 = "abcde test";
        ColorTags tag1 = new ColorTags(str1);
        assertTrue(tag1.getTags().isEmpty());
        
        String str2 = "abcde\\#1A3d#test\\#abef1211#gogo\\#abef12#yeye\\#ab1#hey";
        ColorTags tag2 = new ColorTags(str2);
        assertEquals(4, tag2.getTags().size());
        assertEquals("abcdetestgogoyeyehey", tag2.getPlainText());
        assertEquals(5, tag2.getTags().get(0).start);
        assertEquals(9, tag2.getTags().get(1).start);
        assertEquals(13, tag2.getTags().get(2).start);
        assertEquals(17, tag2.getTags().get(3).start);
    }

}

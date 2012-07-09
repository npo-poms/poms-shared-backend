package nl.vpro.api.rs.util;


import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * split("apple banana orange") = ["apple","banana","orange"]
 * split("apple    \"banana orange\" gorilla") = ["apple","banana orange","gorilla"]
 * split("apple \"banana orange") = ["apple", "\"banana", "orange"]
 * split("\"Europees beleid\" Nederland Duitsland \"minister van binnenlandse zaken\"      jojo  \"broodje aap\"") = ["apple","banana orange","gorilla"]
 */
public class StringUtilTest {

    @Test
    public void testSplit1() throws Exception {
        ArrayList<String> expected = new ArrayList<String>(Arrays.asList("apple", "banana", "orange"));
        Assert.assertEquals(expected, StringUtil.split("apple banana orange"));
    }

    @Test
    public void testSplit2() throws Exception {
        ArrayList<String> expected = new ArrayList<String>(Arrays.asList("apple", "banana orange", "gorilla"));
        Assert.assertEquals(expected, StringUtil.split("apple    \"banana orange\" gorilla"));
    }

    @Test
    public void testSplit3() throws Exception {
        ArrayList<String> expected = new ArrayList<String>(Arrays.asList("apple", "\"banana", "orange"));
        Assert.assertEquals(expected, StringUtil.split("apple \"banana orange"));
    }

    @Test
    public void testSplit4() throws Exception {
        ArrayList<String> expected = new ArrayList<String>(Arrays.asList("Europees beleid","Nederland","Duitsland", "minister van binnenlandse zaken","jojo","broodje aap"));
        Assert.assertEquals(expected, StringUtil.split("\"Europees beleid\" Nederland Duitsland \"minister van binnenlandse zaken\"      jojo  \"broodje aap\""));
    }

    @Test
    public void testAsLuceneQueryPhrase() throws Exception {
        Assert.assertEquals("Ankeveen", StringUtil.asLuceneQueryPhrase("Ankeveen"));
        Assert.assertEquals("\"Hollandse Rading\"", StringUtil.asLuceneQueryPhrase("Hollandse Rading"));

    }

}

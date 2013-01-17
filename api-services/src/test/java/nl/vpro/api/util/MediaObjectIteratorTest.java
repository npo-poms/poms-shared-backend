package nl.vpro.api.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.junit.Test;

import nl.vpro.api.domain.media.MediaObject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Michiel Meeuwissen
 * @since 1.8
 */
public class MediaObjectIteratorTest {



    @Test
    public void testCouchdbIterator() throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("exampleview.json");
        assertNotNull(inputStream);
        CouchdbViewIterator iterator = new CouchdbViewIterator(inputStream);
        assertTrue(iterator.hasNext());
        assertEquals("urn:vpro:media:program:14367824", iterator.next().get("urn").getTextValue());

        assertEquals("urn:vpro:media:program:14389752", iterator.next().get("urn").getTextValue());

    }

    @Test
    public void test() throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("exampleview.json");
        assertNotNull(inputStream);
        MediaObjectIterator iterator = new MediaObjectIterator(new CouchdbViewIterator(inputStream));
        assertTrue(iterator.hasNext());
        MediaObject object = iterator.next();
        assertEquals("urn:vpro:media:program:14367824", object.getUrn());
        assertEquals(Arrays.asList("AVRO"), object.getBroadcasters());

        assertEquals("urn:vpro:media:program:14389752", iterator.next().getUrn());

    }
}

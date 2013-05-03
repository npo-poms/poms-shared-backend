package nl.vpro.api.service.search.filterbuilder;

import java.util.Arrays;

import org.junit.Test;

import com.google.common.base.Predicate;

import nl.vpro.api.domain.media.*;
import nl.vpro.api.domain.media.search.MediaType;
import nl.vpro.api.domain.media.support.MediaObjectType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Date: 21-3-12
 * Time: 17:59
 *
 * @author Ernst Bunders
 */

public class MediaSearchFilterTest {

    @Test
    public void testEmptyQuery() {
        MediaSearchFilter q = new MediaSearchFilter();
        assertEquals("", q.createSolrQueryString());


        //with term
        q = new MediaSearchFilter();
        q.setQueryString("hallo");
        assertEquals("hallo", q.createSolrQueryString());


    }

    @Test
    public void testSingleConstraint() {
        MediaSearchFilter q = new MediaSearchFilter();
        q.addAvType(AvType.AUDIO);
        assertEquals("(avType:AUDIO)", q.createSolrQueryString());


        //with term
        q = new MediaSearchFilter();
        q.addAvType(AvType.AUDIO);
        q.setQueryString("hallo");
        assertEquals("(avType:AUDIO) hallo", q.createSolrQueryString());
    }

    @Test
    public void testAllConstraints() {
        MediaSearchFilter q = new MediaSearchFilter();
        q.addMediaType(MediaType.ALBUM);
        q.addMediaType(MediaType.CLIP);
        q.addLocationFormat(AvFileFormat.MP3);
        q.addLocationFormat(AvFileFormat.FLV);
        q.addAvType(AvType.AUDIO);
        q.addAvType(AvType.VIDEO);
        q.addDescendant("urn:123");
        q.addDescendant("urn:456");
        q.setMainTitle("main");
        q.setDocumentType(MediaObjectType.group);

        assertEquals("(mediaType:ALBUM AND mediaType:CLIP AND location_formats:MP3 AND location_formats:FLV AND avType:AUDIO AND avType:VIDEO AND descendantOf:\"urn:123\" AND descendantOf:\"urn:456\" AND titleMain:main AND documentType:group)", q.createSolrQueryString());

    }

    @Test
    public void apply() {
        MediaSearchFilter mediaSearchFilter = new MediaSearchFilter();
        Predicate<MediaObject> predicate = mediaSearchFilter.getPredicate();
        Program program = new Program();
        mediaSearchFilter.setDocumentType(MediaObjectType.group);
        assertFalse(predicate.apply(program));
        mediaSearchFilter.setDocumentType(MediaObjectType.program);
        assertTrue(predicate.apply(program));

        mediaSearchFilter.addAvType(AvType.AUDIO);
        assertFalse(predicate.apply(program));
        program.setAvType(AvType.VIDEO);
        assertFalse(predicate.apply(program));
        program.setAvType(AvType.AUDIO);
        assertTrue(predicate.apply(program));

        mediaSearchFilter.addLocationFormat(AvFileFormat.MP3);
        assertFalse(predicate.apply(program));
        program.setLocations(Arrays.asList(new Location()));
    }


}

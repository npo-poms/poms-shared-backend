package nl.vpro.api.service.searchfilterbuilder;

import nl.vpro.api.domain.media.AvFileFormat;
import nl.vpro.api.domain.media.AvType;
import nl.vpro.api.domain.media.search.MediaType;
import nl.vpro.api.service.search.fiterbuilder.DocumentSearchFilter;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Date: 21-3-12
 * Time: 17:59
 *
 * @author Ernst Bunders
 */

public class BooleanMediaSearchQueryTest {

    @Test
    public void testEmptyQuery() {
        DocumentSearchFilter q = new DocumentSearchFilter();
        assertEquals("", q.createQueryString());


        //with term
        q = new DocumentSearchFilter();
        q.setQueryString("hallo");
        assertEquals("hallo", q.createQueryString());


    }

    @Test
    public void testSingleConstraint() {
        DocumentSearchFilter q = new DocumentSearchFilter();
        q.addAvType(AvType.AUDIO);
        assertEquals("(avType:AUDIO)", q.createQueryString());


        //with term
        q = new DocumentSearchFilter();
        q.addAvType(AvType.AUDIO);
        q.setQueryString("hallo");
        assertEquals("(avType:AUDIO) hallo", q.createQueryString());
    }

    @Test
    public void testAllConstraints() {
        DocumentSearchFilter q = new DocumentSearchFilter();
        q.addMediaType(MediaType.ALBUM);
        q.addMediaType(MediaType.CLIP);
        q.addLocationFormat(AvFileFormat.MP3);
        q.addLocationFormat(AvFileFormat.FLV);
        q.addAvType(AvType.AUDIO);
        q.addAvType(AvType.VIDEO);
        q.addDescendant("urn:123");
        q.addDescendant("urn:456");
        q.setMainTitle("main");
        q.setDocumentType(DocumentSearchFilter.DOCUMENT_TYPE_GROUP);

        assertEquals("(mediaType:ALBUM AND mediaType:CLIP AND location_formats:MP3 AND location_formats:FLV AND avType:AUDIO AND avType:VIDEO AND descendantOf:\"urn:123\" AND descendantOf:\"urn:456\" AND titleMain:main AND documentType:group)", q.createQueryString());

    }


}

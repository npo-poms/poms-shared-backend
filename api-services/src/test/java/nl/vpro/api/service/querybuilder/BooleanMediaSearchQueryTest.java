package nl.vpro.api.service.querybuilder;

import nl.vpro.domain.media.AVFileFormat;
import nl.vpro.domain.media.AVType;
import nl.vpro.domain.media.search.MediaType;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Date: 21-3-12
 * Time: 17:59
 *
 * @author Ernst Bunders
 */

public class BooleanMediaSearchQueryTest {
    
    @Test
    public void testEmptyQuery() {
        BooleanMediaSearchQuery q = new BooleanMediaSearchQuery(BooleanOp.AND);
        assertEquals("", q.createQueryString());

        q = new BooleanMediaSearchQuery(BooleanOp.OR);
        assertEquals("", q.createQueryString());

        //with term
        q = new BooleanMediaSearchQuery(BooleanOp.AND);
        q.setQueryString("hallo");
        assertEquals("hallo", q.createQueryString());

        q = new BooleanMediaSearchQuery(BooleanOp.OR);
        q.setQueryString("hallo");
        assertEquals("hallo", q.createQueryString());
    }

    @Test
    public void testSingleConstraint(){
        BooleanMediaSearchQuery q = new BooleanMediaSearchQuery(BooleanOp.AND);
        q.addAvType(AVType.AUDIO);
        assertEquals("(avType:AUDIO)", q.createQueryString());

        q = new BooleanMediaSearchQuery(BooleanOp.OR);
        q.addAvType(AVType.AUDIO);
        assertEquals("(avType:AUDIO)", q.createQueryString());

        //with term
        q = new BooleanMediaSearchQuery(BooleanOp.AND);
        q.addAvType(AVType.AUDIO);
        q.setQueryString("hallo");
        assertEquals("(avType:AUDIO) hallo", q.createQueryString());

        q = new BooleanMediaSearchQuery(BooleanOp.OR);
        q.addAvType(AVType.AUDIO);
        q.setQueryString("hallo");
        assertEquals("(avType:AUDIO) hallo", q.createQueryString());
    }

    @Test
    public void testAllConstraints(){
        BooleanMediaSearchQuery q = new BooleanMediaSearchQuery(BooleanOp.AND);
        q.addMediaType(MediaType.ALBUM);
        q.addMediaType(MediaType.CLIP);
        q.addLocationFormat(AVFileFormat.MP3);
        q.addLocationFormat(AVFileFormat.FLV);
        q.addAvType(AVType.AUDIO);
        q.addAvType(AVType.VIDEO);
        q.addDescendant("urn:123");
        q.addDescendant("urn:456");
        q.setMainTitle("main");
        q.setDocumentType(BooleanMediaSearchQuery.DOCUMENT_TYPE_GROUP);

        assertEquals("(mediaType:ALBUM AND mediaType:CLIP AND location_formats:MP3 AND location_formats:FLV AND avType:AUDIO AND avType:VIDEO AND descendantOf:\"urn:123\" AND descendantOf:\"urn:456\" AND titleMain:main AND documentType:group)", q.createQueryString());

        q = new BooleanMediaSearchQuery(BooleanOp.OR);
        q.addMediaType(MediaType.ALBUM);
        q.addMediaType(MediaType.CLIP);
        q.addLocationFormat(AVFileFormat.MP3);
        q.addLocationFormat(AVFileFormat.FLV);
        q.addAvType(AVType.AUDIO);
        q.addAvType(AVType.VIDEO);
        q.addDescendant("urn:123");
        q.addDescendant("urn:456");
        q.setMainTitle("main");
        q.setDocumentType(BooleanMediaSearchQuery.DOCUMENT_TYPE_GROUP);
        
        assertEquals("(mediaType:ALBUM OR mediaType:CLIP OR location_formats:MP3 OR location_formats:FLV OR avType:AUDIO OR avType:VIDEO OR descendantOf:\"urn:123\" OR descendantOf:\"urn:456\" OR titleMain:main OR documentType:group)", q.createQueryString());
    }


}

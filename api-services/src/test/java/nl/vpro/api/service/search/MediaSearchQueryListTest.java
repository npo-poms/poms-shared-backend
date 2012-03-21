package nl.vpro.api.service.search;

import nl.vpro.domain.media.AVType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Date: 21-3-12
 * Time: 18:15
 *
 * @author Ernst Bunders
 */
public class MediaSearchQueryListTest {
    private static BooleanMediaSearchQuery q = new BooleanMediaSearchQuery(BooleanOp.AND);

    static {
        q.setMainTitle("title");
        q.addAvType(AVType.AUDIO);

    }

    private static final String qs = "(avType:AUDIO AND titleMain:title)";

    @Test
    public void testEmpytList() {
        MediaSearchQueryList ql = new MediaSearchQueryList(BooleanOp.AND);
        assertEquals("", ql.createQueryString());

        ql = new MediaSearchQueryList(BooleanOp.OR);
        assertEquals("", ql.createQueryString());

        //with term
        ql = new MediaSearchQueryList(BooleanOp.AND);
        ql.setQueryString("test");
        assertEquals("test", ql.createQueryString());

        ql = new MediaSearchQueryList(BooleanOp.OR);
        ql.setQueryString("test");
        assertEquals("test", ql.createQueryString());
    }

    @Test
    public void testListWithSingleQuery() {
        MediaSearchQueryList ql = new MediaSearchQueryList(BooleanOp.OR);
        ql.addQuery(q);
        assertEquals(qs, ql.createQueryString());

        ql = new MediaSearchQueryList(BooleanOp.AND);
        ql.addQuery(q);
        assertEquals(qs, ql.createQueryString());

        //with search term
        ql = new MediaSearchQueryList(BooleanOp.OR);
        ql.addQuery(q);
        ql.setQueryString("test");
        assertEquals(qs + " test", ql.createQueryString());

        ql = new MediaSearchQueryList(BooleanOp.AND);
        ql.addQuery(q);
        ql.setQueryString("test");
        assertEquals(qs + " test", ql.createQueryString());
    }

    @Test
    public void testWithMultipleQueries(){
        MediaSearchQueryList ql = new MediaSearchQueryList(BooleanOp.OR);
        ql.addQuery(q);
        ql.addQuery(q);
        assertEquals("((avType:AUDIO AND titleMain:title) OR (avType:AUDIO AND titleMain:title))", ql.createQueryString());

        ql = new MediaSearchQueryList(BooleanOp.AND);
        ql.addQuery(q);
        ql.addQuery(q);
        assertEquals("((avType:AUDIO AND titleMain:title) AND (avType:AUDIO AND titleMain:title))", ql.createQueryString());

        //with search term
        ql = new MediaSearchQueryList(BooleanOp.OR);
        ql.addQuery(q);
        ql.addQuery(q);
        ql.setQueryString("test");
        assertEquals("((avType:AUDIO AND titleMain:title) OR (avType:AUDIO AND titleMain:title)) test", ql.createQueryString());

        ql = new MediaSearchQueryList(BooleanOp.AND);
        ql.addQuery(q);
        ql.addQuery(q);
        ql.setQueryString("test");
        assertEquals("((avType:AUDIO AND titleMain:title) AND (avType:AUDIO AND titleMain:title)) test", ql.createQueryString());
    }
}

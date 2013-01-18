package nl.vpro.api.service.searchfilterbuilder;

import org.junit.Test;

import nl.vpro.api.domain.media.AvType;
import nl.vpro.api.service.search.fiterbuilder.BooleanOp;
import nl.vpro.api.service.search.fiterbuilder.DocumentSearchFilter;
import nl.vpro.api.service.search.fiterbuilder.SearchFilterList;

import static org.junit.Assert.assertEquals;

/**
 * Date: 21-3-12
 * Time: 18:15
 *
 * @author Ernst Bunders
 */
public class SearchFilterListTest {
    private static DocumentSearchFilter q = new DocumentSearchFilter();

    static {
        q.setMainTitle("titles");
        q.addAvType(AvType.AUDIO);

    }

    private static final String qs = "(avType:AUDIO AND titleMain:titles)";

    @Test
    public void testEmpytList() {
        SearchFilterList ql = new SearchFilterList(BooleanOp.AND);
        assertEquals("", ql.createQueryString());

        ql = new SearchFilterList(BooleanOp.OR);
        assertEquals("", ql.createQueryString());

        //with term
        ql = new SearchFilterList(BooleanOp.AND);
        ql.setQueryString("test");
        assertEquals("test", ql.createQueryString());

        ql = new SearchFilterList(BooleanOp.OR);
        ql.setQueryString("test");
        assertEquals("test", ql.createQueryString());
    }

    @Test
    public void testListWithSingleQuery() {
        SearchFilterList ql = new SearchFilterList(BooleanOp.OR);
        ql.addQuery(q);
        assertEquals(qs, ql.createQueryString());

        ql = new SearchFilterList(BooleanOp.AND);
        ql.addQuery(q);
        assertEquals(qs, ql.createQueryString());

        //with search term
        ql = new SearchFilterList(BooleanOp.OR);
        ql.addQuery(q);
        ql.setQueryString("test");
        assertEquals(qs + " test", ql.createQueryString());

        ql = new SearchFilterList(BooleanOp.AND);
        ql.addQuery(q);
        ql.setQueryString("test");
        assertEquals(qs + " test", ql.createQueryString());
    }

    @Test
    public void testWithMultipleQueries() {
        SearchFilterList ql = new SearchFilterList(BooleanOp.OR);
        ql.addQuery(q);
        ql.addQuery(q);
        assertEquals("((avType:AUDIO AND titleMain:titles) OR (avType:AUDIO AND titleMain:titles))", ql.createQueryString());

        ql = new SearchFilterList(BooleanOp.AND);
        ql.addQuery(q);
        ql.addQuery(q);
        assertEquals("((avType:AUDIO AND titleMain:titles) AND (avType:AUDIO AND titleMain:titles))", ql.createQueryString());

        //with search term
        ql = new SearchFilterList(BooleanOp.OR);
        ql.addQuery(q);
        ql.addQuery(q);
        ql.setQueryString("test");
        assertEquals("((avType:AUDIO AND titleMain:titles) OR (avType:AUDIO AND titleMain:titles)) test", ql.createQueryString());

        ql = new SearchFilterList(BooleanOp.AND);
        ql.addQuery(q);
        ql.addQuery(q);
        ql.setQueryString("test");
        assertEquals("((avType:AUDIO AND titleMain:titles) AND (avType:AUDIO AND titleMain:titles)) test", ql.createQueryString());
    }
}

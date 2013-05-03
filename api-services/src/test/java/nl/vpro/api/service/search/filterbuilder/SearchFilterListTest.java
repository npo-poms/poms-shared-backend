package nl.vpro.api.service.search.filterbuilder;

import org.junit.Test;

import nl.vpro.api.domain.media.AvType;

import static org.junit.Assert.assertEquals;

/**
 * Date: 21-3-12
 * Time: 18:15
 *
 * @author Ernst Bunders
 */
public class SearchFilterListTest {
    private static MediaSearchFilter q = new MediaSearchFilter();

    static {
        q.setMainTitle("titles");
        q.addAvType(AvType.AUDIO);

    }

    private static final String qs = "(avType:AUDIO AND titleMain:titles)";

    @Test
    public void testEmpytList() {
        SearchFilterList ql = new SearchFilterList(BooleanOp.AND);
        assertEquals("", ql.createSolrQueryString());

        ql = new SearchFilterList(BooleanOp.OR);
        assertEquals("", ql.createSolrQueryString());

        //with term
        ql = new SearchFilterList(BooleanOp.AND);
        ql.setQueryString("test");
        assertEquals("test", ql.createSolrQueryString());

        ql = new SearchFilterList(BooleanOp.OR);
        ql.setQueryString("test");
        assertEquals("test", ql.createSolrQueryString());
    }

    @Test
    public void testListWithSingleQuery() {
        SearchFilterList ql = new SearchFilterList(BooleanOp.OR);
        ql.add(q);
        assertEquals(qs, ql.createSolrQueryString());

        ql = new SearchFilterList(BooleanOp.AND);
        ql.add(q);
        assertEquals(qs, ql.createSolrQueryString());

        //with search term
        ql = new SearchFilterList(BooleanOp.OR);
        ql.add(q);
        ql.setQueryString("test");
        assertEquals(qs + " test", ql.createSolrQueryString());

        ql = new SearchFilterList(BooleanOp.AND);
        ql.add(q);
        ql.setQueryString("test");
        assertEquals(qs + " test", ql.createSolrQueryString());
    }

    @Test
    public void testWithMultipleQueries() {
        SearchFilterList ql = new SearchFilterList(BooleanOp.OR);
        ql.add(q);
        ql.add(q);
        assertEquals("((avType:AUDIO AND titleMain:titles) OR (avType:AUDIO AND titleMain:titles))", ql.createSolrQueryString());

        ql = new SearchFilterList(BooleanOp.AND);
        ql.add(q);
        ql.add(q);
        assertEquals("((avType:AUDIO AND titleMain:titles) AND (avType:AUDIO AND titleMain:titles))", ql.createSolrQueryString());

        //with search term
        ql = new SearchFilterList(BooleanOp.OR);
        ql.add(q);
        ql.add(q);
        ql.setQueryString("test");
        assertEquals("((avType:AUDIO AND titleMain:titles) OR (avType:AUDIO AND titleMain:titles)) test", ql.createSolrQueryString());

        ql = new SearchFilterList(BooleanOp.AND);
        ql.add(q);
        ql.add(q);
        ql.setQueryString("test");
        assertEquals("((avType:AUDIO AND titleMain:titles) AND (avType:AUDIO AND titleMain:titles)) test", ql.createSolrQueryString());

        ql = new SearchFilterList(BooleanOp.AND);
        ql.add(q);
        ql.add(new FieldFilter("title", "testje"));
        assertEquals("((avType:AUDIO AND titleMain:titles) AND title:testje)",ql.createSolrQueryString());
    }
}

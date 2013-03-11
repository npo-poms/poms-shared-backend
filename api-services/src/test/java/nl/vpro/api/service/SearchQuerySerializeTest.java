/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.service;

import nl.vpro.api.transfer.SearchQuery;
import nl.vpro.jackson.MediaMapper;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import java.io.InputStream;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;

/**
 * User: rico
 * Date: 07/03/2013
 */
public class SearchQuerySerializeTest {

    @Test
    public void testSerializeSearchQuery() {

        try {
            ObjectMapper mapper = new MediaMapper();
            StringWriter buffer = new StringWriter();
            SearchQuery searchQuery = new SearchQuery("TestQuery");
            searchQuery.setType("page");
            searchQuery.addConstraint(new SearchQuery.Constraint("veldje", "waarde"));
            searchQuery.addFacet(new SearchQuery.Facet("veld1", 5));
            searchQuery.addHighlight(new SearchQuery.Highlight("langveld", 50, 3));
            searchQuery.addSortOrder(new SearchQuery.SortField("veld2", "asc"));
            searchQuery.setOffset(10);
            searchQuery.setMax(10);
            mapper.writeValue(buffer, searchQuery);

            InputStream is = this.getClass().getClassLoader().getResourceAsStream("searchquery.json");
            String jsonFile = IOUtils.toString(is);
            assertEquals(buffer.toString(), jsonFile);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Test
    public void testDeSeializeSearchQuery() {
        ObjectMapper mapper = new MediaMapper();
        try {
            InputStream is = this.getClass().getClassLoader().getResourceAsStream("searchquery.json");
            SearchQuery searchQuery = mapper.readValue(is, SearchQuery.class);

            assertEquals("TestQuery", searchQuery.getQuery());
            assertEquals("page", searchQuery.getType());
            assertEquals(1, searchQuery.getConstraints().size());
            assertEquals(1, searchQuery.getFacets().size());
            assertEquals(1, searchQuery.getHighlights().size());
            assertEquals(1, searchQuery.getSortOrder().size());
            SearchQuery.Constraint constraint = searchQuery.getConstraints().get(0);
            assertEquals("veldje", constraint.getField());
            assertEquals("waarde", constraint.getValue());
            SearchQuery.Facet facet = searchQuery.getFacets().get(0);
            assertEquals("veld1", facet.getField());
            assertEquals(5, facet.getNumber().intValue());
            SearchQuery.Highlight highlight = searchQuery.getHighlights().get(0);
            assertEquals("langveld", highlight.getField());
            assertEquals(50, highlight.getSize().intValue());
            assertEquals(3, highlight.getNumber().intValue());
            SearchQuery.SortField sortField = searchQuery.getSortOrder().get(0);
            assertEquals("veld2", sortField.getField());
            assertEquals("ASC", sortField.getDirection());
            assertEquals(10, searchQuery.getOffset().intValue());
            assertEquals(10, searchQuery.getMax().intValue());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

package nl.vpro.domain.api.media;

import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;

public class ESMediaFacetsHandlerTest {

    @Test
    public void testBuildRelationsFacets() throws Exception {
        MediaForm form = new MediaForm();
        MediaFacets facets = new MediaFacets();
        RelationFacetList list = new RelationFacetList();
        facets.setRelations(list);
        form.setFacets(facets);
        SearchSourceBuilder searchBuilder = SearchSourceBuilder.searchSource();
        //ESMediaFacetsHandler.buildFacets(searchBuilder, form, null, "prefix");
        System.out.println(searchBuilder.toString());
    }

    @Test
    public void testAgeRatingFacet() throws Exception {
        MediaForm form = new MediaForm();
        MediaFacets facets = new MediaFacets();
        facets.setAgeRatings(new MediaFacet());
        form.setFacets(facets);
        SearchSourceBuilder searchBuilder = SearchSourceBuilder.searchSource();
        //ESMediaFacetsHandler.(searchBuilder, form, null, "prefix");
        System.out.println(searchBuilder.toString());
    }

}

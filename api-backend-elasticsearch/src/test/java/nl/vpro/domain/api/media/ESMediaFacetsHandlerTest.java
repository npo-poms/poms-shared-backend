package nl.vpro.domain.api.media;

import lombok.extern.log4j.Log4j2;

import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.jupiter.api.Test;

@Log4j2
public class ESMediaFacetsHandlerTest {

    @Test
    public void testBuildRelationsFacets() {
        MediaForm form = new MediaForm();
        MediaFacets facets = new MediaFacets();
        RelationFacetList list = new RelationFacetList();
        facets.setRelations(list);
        form.setFacets(facets);
        SearchSourceBuilder searchBuilder = SearchSourceBuilder.searchSource();
        //ESMediaFacetsHandler.buildFacets(searchBuilder, form, null, "prefix");
        log.info("Search: {}", searchBuilder);
    }

    @Test
    public void testAgeRatingFacet() {
        MediaForm form = new MediaForm();
        MediaFacets facets = new MediaFacets();
        facets.setAgeRatings(new MediaFacet());
        form.setFacets(facets);
        SearchSourceBuilder searchBuilder = SearchSourceBuilder.searchSource();
        //ESMediaFacetsHandler.(searchBuilder, form, null, "prefix");
        log.info("Search: {}", searchBuilder);
    }

}

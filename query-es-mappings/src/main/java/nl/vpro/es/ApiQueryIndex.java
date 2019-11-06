package nl.vpro.es;

import nl.vpro.elasticsearch.ElasticSearchIndex;

/**
 * @author Michiel Meeuwissen
 * @since 5.12
 */
public class ApiQueryIndex extends ElasticSearchIndex {

    public static final String NAME = "apiqueries";
    public static final ApiQueryIndex APIQUERIES = new ApiQueryIndex();

    private ApiQueryIndex() {
        super(NAME,  "/es7/mapping/query.json");
    }

}

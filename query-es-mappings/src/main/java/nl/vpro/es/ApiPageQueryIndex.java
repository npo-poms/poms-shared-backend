package nl.vpro.es;

import java.util.ArrayList;

import nl.vpro.elasticsearch.ElasticSearchIndex;

/**
 * @author Michiel Meeuwissen
 * @since 5.12
 */
public class ApiPageQueryIndex extends ElasticSearchIndex {

    public static final String NAME = "apipagequeries";
    public static final ApiPageQueryIndex APIPAGEQUERIES = new ApiPageQueryIndex();

    private ApiPageQueryIndex() {
        super(NAME,  "/es7/setting/apiqueries.json", "/es7/mapping/query.json", new ArrayList<>());
    }

}

package nl.vpro.es;

import nl.vpro.poms.es.ApiElasticSearchIndex;

/**
 * @author Michiel Meeuwissen
 * @since 5.12
 */
public class ApiQueryIndex extends ApiElasticSearchIndex {

    public static final String NAME = "apiqueries";
    public static final ApiQueryIndex APIQUERIES = new ApiQueryIndex();

    private ApiQueryIndex() {
        super(NAME,  "/es7/mapping/query.json");
    }

}

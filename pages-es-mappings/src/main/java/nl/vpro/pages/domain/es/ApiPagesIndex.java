package nl.vpro.pages.domain.es;

import nl.vpro.poms.es.ApiElasticSearchIndex;

/**
 * @author Michiel Meeuwissen
 * @since 4.7
 */
public class ApiPagesIndex extends ApiElasticSearchIndex {

    public static final String NAME = "apipages";

    public static final ApiPagesIndex APIPAGES = new ApiPagesIndex();

    protected ApiPagesIndex() {
        super(NAME,  "/es7/mapping/page.json");
    }

}

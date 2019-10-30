package nl.vpro.pages.domain.es;

import nl.vpro.poms.es.ApiElasticSearchIndex;

/**
 * @author Michiel Meeuwissen
 * @since 5.1
 */
public class ApiPageUpdatesIndex extends ApiElasticSearchIndex {

    public static String NAME = "pageupdates";

     public static final ApiPageUpdatesIndex PAGEUPDATES = new ApiPageUpdatesIndex();

    protected ApiPageUpdatesIndex() {
        super(NAME,  "/es7/mapping/pageupdate.json");
    }

}

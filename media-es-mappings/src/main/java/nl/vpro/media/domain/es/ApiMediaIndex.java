package nl.vpro.media.domain.es;


import nl.vpro.elasticsearch.ElasticSearchIndex;

/**
 * @author Michiel Meeuwissen
 * @since 4.7
 */
public class ApiMediaIndex extends ElasticSearchIndex {

    public static final String NAME = "apimedia";
    public static final ApiMediaIndex APIMEDIA = new ApiMediaIndex();

    private ApiMediaIndex() {
        super(NAME,  "/es7/mapping/apimedia.json");
    }

}

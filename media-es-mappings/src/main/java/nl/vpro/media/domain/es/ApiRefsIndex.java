package nl.vpro.media.domain.es;

import nl.vpro.elasticsearch.ElasticSearchIndex;

/**
 * @author Michiel Meeuwissen
 * @since 5.12
 */
public class ApiRefsIndex extends ElasticSearchIndex {

    public static String POSTFIX = "_refs";
    public static String NAME = ApiMediaIndex.NAME + POSTFIX;
    public static final ApiRefsIndex APIMEDIA_REFS = new ApiRefsIndex();

    private ApiRefsIndex() {
        super(NAME, "/es7/mapping/ref.json");
    }

}

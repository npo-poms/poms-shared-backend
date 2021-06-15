package nl.vpro.pages.domain.es;


import com.fasterxml.jackson.databind.node.ObjectNode;

import nl.vpro.elasticsearch.ElasticSearchIndex;

/**
 * @author Michiel Meeuwissen
 * @since 4.7
 */
public class ApiPagesIndex extends ElasticSearchIndex {

    public static final String NAME = "apipages";

    public static final ApiPagesIndex APIPAGES = new ApiPagesIndex();

    protected ApiPagesIndex() {
        super(NAME,  "/es7/mapping/page.json", NAME + "-publish");
    }

    @Override
    public ElasticSearchIndex withoutExperimental() {
        return withMappingsProcessor(jsonNode -> {
            ObjectNode properties = jsonNode.with("properties");
            properties.remove("semanticVectorization");
        });
    }


}

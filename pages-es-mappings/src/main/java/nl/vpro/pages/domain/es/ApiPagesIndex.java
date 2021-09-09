package nl.vpro.pages.domain.es;
import java.util.Arrays;

import com.fasterxml.jackson.databind.node.ObjectNode;

import nl.vpro.elasticsearch.ElasticSearchIndex;

/**
 * @author Michiel Meeuwissen
 * @since 4.7
 */
public class ApiPagesIndex extends ElasticSearchIndex {

    public static final int VECTOR_LENGTH = 768;

    public static final String NAME = "apipages";

    public static final ApiPagesIndex APIPAGES = new ApiPagesIndex();

    protected ApiPagesIndex() {
        super(NAME,  "/es7/setting/apipages.json", "/es7/mapping/page.json", Arrays.asList(NAME + "-publish"), jsonNode -> {
            ObjectNode properties = jsonNode.with("properties");
            properties.with("semanticVectorization").put("dims", VECTOR_LENGTH);
        });
    }

    @Override
    public ElasticSearchIndex withoutExperimental() {
        return thenWithMappingsProcessor(jsonNode -> {
            ObjectNode properties = jsonNode.with("properties");
            properties.remove("semanticVectorization");
        });
    }


}

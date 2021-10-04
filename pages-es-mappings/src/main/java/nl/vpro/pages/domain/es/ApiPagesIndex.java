package nl.vpro.pages.domain.es;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

import com.fasterxml.jackson.databind.node.ObjectNode;

import nl.vpro.elasticsearch.ElasticSearchIndex;

import static nl.vpro.elasticsearch.Constants.Mappings.PROPERTIES;

/**
 * @author Michiel Meeuwissen
 * @since 4.7
 */
@Slf4j
public class ApiPagesIndex extends ElasticSearchIndex {

    public static final int SEMANTIC_VECTOR_LENGTH = 768;
    public static final String FIELD_SEMANTIC_VECTORIZATION = "semanticVectorization";

    public static final String NAME = "apipages";

    public static final ApiPagesIndex APIPAGES = new ApiPagesIndex();

    protected ApiPagesIndex() {
        super(NAME,  "/es7/setting/apipages.json", "/es7/mapping/page.json", Arrays.asList(NAME + "-publish"), (distribution, jsonNode) -> {
            ObjectNode properties = jsonNode.with(PROPERTIES);
            switch (distribution) {
                case OPENSEARCH:
                    properties.with(FIELD_SEMANTIC_VECTORIZATION).put("dimension", SEMANTIC_VECTOR_LENGTH);
                    log.info("Detected opensearch");
                    break;
                case ELASTICSEARCH:
                    properties.with(FIELD_SEMANTIC_VECTORIZATION).put("dims", SEMANTIC_VECTOR_LENGTH);
                    log.info("Detected elasticsearch");
                    break;
            }

        });
    }

    @Override
    public ElasticSearchIndex withoutExperimental() {
        return thenWithMappingsProcessor((distribution, jsonNode) -> {
            ObjectNode properties = jsonNode.with(PROPERTIES);
            properties.remove(FIELD_SEMANTIC_VECTORIZATION);
        });
    }


}

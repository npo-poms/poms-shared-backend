package nl.vpro.domain.api;


import java.util.LinkedHashMap;
import java.util.Map;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;

import com.google.common.annotations.Beta;

import nl.vpro.media.domain.es.Common;
import nl.vpro.semantic.VectorizationService;

/**
 * @since 5.28
 *
 */
@Beta
public class SemanticScoring {

    private SemanticScoring() {
        // utilily
    }

    public static <MT extends MatchType> Script buildSemanticScoreScript(
        @NonNull String prefix,
        @NonNull AbstractTextMatcher<MT> textSearch,
        @Nullable VectorizationService vectorizationService) {

        if (vectorizationService == null) {
            throw new UnsupportedOperationException();
        }
        float[] vectorization = vectorizationService.forQuery(textSearch.getValue());
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("query_vector", vectorization);
        String field = prefix + Common.ES_VECTORIZATION;
        Script script = new Script(ScriptType.INLINE, "painless", "doc.containsKey('" + field + "') ? (cosineSimilarity(params.query_vector, doc['"  + field + "']) + 1.0) : 0.0", params);
        return script;
    }
}

package nl.vpro.domain.api;


import java.text.MessageFormat;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import com.google.common.annotations.Beta;

import nl.vpro.media.domain.es.Common;
import nl.vpro.semantic.VectorizationService;

/**
 * @since 5.28
 *
 */
@Beta
@ManagedResource(objectName = "nl.vpro.api:name=SemanticScoring")
@Component
public class SemanticScoring {

    public SemanticScoring() {

    }

    @Value("${semantic.score.script}")
    private String script = "doc[''{0}''].size() == 0 ? 0.0  : (cosineSimilarity(params.query_vector, doc[''{0}'']) + 1) / 2";

    @Value("${semantic.score.min:#{null}}")
    private Float minScore;

    public <MT extends MatchType> Script buildSemanticScoreScript(
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
        return  new Script(ScriptType.INLINE, "painless", MessageFormat.format(script, field), params);
    }

    @ManagedAttribute
    public String getScript() {
        return script;
    }
    @ManagedAttribute
    public void setScript(String script) {
        this.script = script;
    }

    @ManagedAttribute
    public String getMinScore() {
        return minScore == null ? "" : String.valueOf(minScore);
    }
    @ManagedAttribute
    public void setMinScore(String score) {
        this.minScore = StringUtils.isEmpty(score) ? null : Float.parseFloat(score);
    }

    public Optional<Float> getMinScoreOverride() {
        return Optional.ofNullable(minScore);
    }

}

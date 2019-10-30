package nl.vpro.domain.api.suggest;

import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.*;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.core.JsonProcessingException;

import nl.vpro.domain.api.*;
import nl.vpro.domain.api.media.Redirector;
import nl.vpro.elasticsearch.ESClientFactory;
import nl.vpro.jackson2.Jackson2Mapper;
import nl.vpro.poms.es.ApiElasticSearchIndex;
import nl.vpro.util.ThreadPools;

import static nl.vpro.es.ApiQueryIndex.APIQUERIES;

/**
 * @author Roelof Jan Koekoek
 * @since 3.2
 */
public class ESQueryRepository extends AbstractESRepository<Query> implements QuerySearchRepository {
    private static final Integer PROFILE_SEPARATOR_LENGTH = "||".length();

    @Getter
    private final Duration cleanInterval = Duration.ofHours(1);

    @Getter
    @Setter
    private Duration ttl  = Duration.ofDays(14);

    @Value("${elasticSearch.query.index}")
    public void setIndexName(
        @NonNull String indexName) {
        super.setIndexName(APIQUERIES, indexName);
    }

    public String getIndexName() {
        return indexNames.get(APIQUERIES);
    }

    @Autowired
    public ESQueryRepository(ESClientFactory client) {
        super(client);
        ThreadPools.backgroundExecutor.scheduleWithFixedDelay(this::cleanSuggestions,
            0, cleanInterval.toMillis(), TimeUnit.MILLISECONDS );
    }

    @Override
    protected ApiElasticSearchIndex getIndex(String id, Class<?> clazz) {
        return APIQUERIES;

    }

    public void cleanSuggestions() {
        BulkByScrollResponse response = new DeleteByQueryRequestBuilder(client(), DeleteByQueryAction.INSTANCE)
            .filter(QueryBuilders.rangeQuery("sortDate")
                .lte(Instant.now().minus(ttl).toEpochMilli()))
            .source(indexNames.get(APIQUERIES))
            .get();

        log.info("Deleted {}", response.getDeleted());
    }



    @Override
    protected Redirector getDirectsRepository() {
        throw new UnsupportedOperationException();

    }

    @Override
    public void index(Query query) {
        try {
            client().prepareIndex()
                .setIndex(getIndexName())
                //.setOpType(DocWriteRequest.)
                .setSource(Jackson2Mapper.getInstance().writeValueAsString(query), XContentType.JSON)
                .setId(query.getId())
                .execute()
                .actionGet();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public SuggestResult suggest(String input, String profile, Integer max) {
        SearchResponse searchResponse = client().prepareSearch(getIndexName())
            .suggest(
                suggestBuilder(Query.queryId(input, profile), profile, max)
            )
            .execute()
            .actionGet();

        Suggest suggest = searchResponse.getSuggest();
        return adapt(suggest, input, profile);
    }

    private SuggestBuilder suggestBuilder(String input, String profile, Integer max) {
        if (max == null) {
            max = 10;
        }
        int profilePrefixLength = profile != null ? profile.length() + PROFILE_SEPARATOR_LENGTH : 0;
        return new SuggestBuilder()
            .addSuggestion("suggest",
                new CompletionSuggestionBuilder("suggest")
                    .text(input)
                    .size(max))
            //.setFuzziness(Fuzziness.AUTO)
            //setFuzzyMinLength(profilePrefixLength + 3);
        ;
    }

    private Comparator<Suggestion> getLexicalDistanceComparator(final String input) {
        return Comparator.comparingInt(o ->  LevenshteinDistance.getDefaultInstance().apply(input, o.getText()));
    }

    SuggestResult adapt(Suggest suggestions, final String input, final String profile) {
        if (suggestions == null) {
            log.debug("No suggestions given");
            return SuggestResult.emptyResult();
        }
        Suggest.Suggestion<? extends Suggest.Suggestion.Entry<? extends Suggest.Suggestion.Entry.Option>> esSuggestion = suggestions.getSuggestion("suggest");
        if (esSuggestion != null && esSuggestion.getEntries().size() > 0) {
            final List<? extends Suggest.Suggestion.Entry.Option> options = esSuggestion.getEntries().get(0).getOptions();
            if (options != null) {
                final List<Suggestion> suggestionsList = options.stream()
                    .map(option -> {
                        String text = option.getText().string();
                        return new Suggestion(profile != null ? text.substring(profile.length() + PROFILE_SEPARATOR_LENGTH) : text);
                    })
                    .sorted(getLexicalDistanceComparator(input))
                    .collect(Collectors.toList());

                return new SuggestResult(suggestionsList,
                    null,
                    options.size());
            }
        }

        return SuggestResult.emptyResult();
    }
}

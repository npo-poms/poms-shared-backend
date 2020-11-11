package nl.vpro.domain.api.suggest;

import lombok.*;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import nl.vpro.elasticsearch.highlevel.HighLevelClientFactory;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
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
import nl.vpro.elasticsearch.ElasticSearchIndex;
import nl.vpro.jackson2.Jackson2Mapper;
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
    @Value("${elasticSearch.query.ttl}")
    private Duration ttl  = Duration.ofDays(14);


    @Value("${elasticSearch.query.readOnly}")
    private boolean  readOnly;


    @Value("${elasticSearch.query.index}")
    public void setIndexName(
        @NonNull String indexName) {
        super.setIndexName(APIQUERIES, indexName);
    }

    public String getIndexName() {
        return indexNames.get(APIQUERIES);
    }


    public ESQueryRepository(@Autowired HighLevelClientFactory client) {
        super(client);
        ThreadPools.backgroundExecutor.scheduleWithFixedDelay(this::cleanSuggestions,
            0, cleanInterval.toMillis(), TimeUnit.MILLISECONDS );
    }

    @Override
    protected ElasticSearchIndex getIndex(String id, Class<?> clazz) {
        return APIQUERIES;

    }

    public void cleanSuggestions() {
        if (! readOnly) {

            SearchRequest searchRequest = new SearchRequest(indexNames.get(APIQUERIES));
            searchRequest.source(
                QueryBuilders.rangeQuery("sortDate")
                .lte(Instant.now().minus(ttl).toEpochMilli()));
            DeleteByQueryRequest deleteByQueryRequest = new DeleteByQueryRequest(searchRequest);
            client().deleteByQuery(deleteByQueryRequest, RequestOptions.DEFAULT);
            BulkByScrollResponse response = new DeleteByQueryRequestBuilder(client(), DeleteByQueryAction.INSTANCE)
                .filter(QueryBuilders.rangeQuery("sortDate")
                    .lte(Instant.now().minus(ttl).toEpochMilli()))
                .source(indexNames.get(APIQUERIES))
                .get();

            log.info("Deleted {}", response.getDeleted());
        } else {
            log.warn("Skipped while configured read only");
        }
    }




    @Override
    public void index(Query query) {
        if (! readOnly) {

            try {
                IndexRequest indexRequest = new IndexRequest(getIndexName());
                indexRequest.id(query.getId());
                indexRequest.source(Jackson2Mapper.getInstance().writeValueAsString(query), XContentType.JSON);

                IndexResponse response  = client().index(indexRequest, RequestOptions.DEFAULT);
                log.info("indexed {}", response);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            log.warn("Skipped while configured read only");
        }
    }

    @Override
    @SneakyThrows(IOException.class)
    public SuggestResult suggest(String input, String profile, Integer max) {
        SearchRequest searchRequest = new SearchRequest(getIndexName());
        searchRequest.source(suggestBuilder(Query.queryId(input, profile), profile, max));

        SearchResponse searchResponse = client().search(searchRequest, RequestOptions.DEFAULT);
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

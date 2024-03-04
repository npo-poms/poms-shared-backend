 package nl.vpro.domain.api;

 import lombok.Getter;
 import lombok.Setter;

 import java.io.IOException;
 import java.time.Duration;
 import java.util.*;
 import java.util.concurrent.CompletableFuture;
 import java.util.function.Consumer;
 import java.util.stream.Collectors;

 import jakarta.validation.constraints.NotNull;

 import org.apache.commons.lang3.StringUtils;
 import org.apache.http.client.config.RequestConfig;
 import org.apache.logging.log4j.LogManager;
 import org.apache.logging.log4j.Logger;
 import org.apache.lucene.search.TotalHits;
 import org.checkerframework.checker.nullness.qual.NonNull;
 import org.checkerframework.checker.nullness.qual.Nullable;
 import org.elasticsearch.action.get.*;
 import org.elasticsearch.action.search.SearchRequest;
 import org.elasticsearch.action.search.SearchResponse;
 import org.elasticsearch.client.*;
 import org.elasticsearch.common.text.Text;
 import org.elasticsearch.index.IndexNotFoundException;
 import org.elasticsearch.index.query.QueryBuilder;
 import org.elasticsearch.search.SearchHit;
 import org.elasticsearch.search.SearchHits;
 import org.elasticsearch.search.aggregations.AggregationBuilders;
 import org.elasticsearch.search.aggregations.BucketOrder;
 import org.elasticsearch.search.aggregations.bucket.terms.Terms;
 import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
 import org.elasticsearch.search.builder.SearchSourceBuilder;
 import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
 import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
 import org.springframework.beans.factory.annotation.Value;
 import org.springframework.jmx.export.annotation.ManagedAttribute;

 import com.fasterxml.jackson.core.JsonProcessingException;
 import com.fasterxml.jackson.databind.JsonNode;

 import nl.vpro.elasticsearch.ElasticSearchIndex;
 import nl.vpro.elasticsearch.highlevel.HighLevelClientFactory;
 import nl.vpro.jackson2.Jackson2Mapper;
 import nl.vpro.util.ThreadPools;
 import nl.vpro.util.TimeUtils;


/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
public abstract class AbstractESRepository<T> {

    protected final Logger log = LogManager.getLogger(getClass().getName());
    protected final Logger LOG_ERRORS = LogManager.getLogger(getClass().getName() + ".ERRORS");

    protected final HighLevelClientFactory factory;

    protected static final Jackson2Mapper MAPPER = Jackson2Mapper.getInstance();
    protected static final Jackson2Mapper LENIENT = Jackson2Mapper.getLenientInstance();


    @Getter
    @Setter
    protected Integer facetLimit = 10;

    @Getter
    @Setter
    protected Duration timeOut = Duration.ofSeconds(15);

    @Getter
    protected Map<ElasticSearchIndex, String> indexNames = new HashMap<>();

    @Getter
    @Setter
    protected Duration commitDelay = Duration.ofSeconds(30);

    @Getter
    @Setter
    @Value("${elasticSearch.warnSortNotOnDoc:false}")
    protected boolean warnSortNotOnDoc;



    protected AbstractESRepository(
        @NotNull HighLevelClientFactory factory) {
        this.factory = factory;
        if (this.factory == null) {
            throw new IllegalArgumentException("The ES client factory cannot be null");
        }
    }

    public void setIndexName(ElasticSearchIndex index, @NonNull String indexName) {
        indexNames.put(index, indexName);
    }

    protected void logIntro() {
        log.info("ES Repository {} with factory {}", this, factory);
        for (String indexName : indexNames.values()) {
            ThreadPools.backgroundExecutor.execute(() -> {
                try {
                    if (indexName != null) {
                        SearchRequest searchRequest = new SearchRequest(indexName);
                        TermsAggregationBuilder aggregationBuilder =
                            AggregationBuilders.terms("types")
                                .field("_type")
                                .order(BucketOrder.key(true));
                        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

                        searchSourceBuilder.aggregation(aggregationBuilder);
                        searchSourceBuilder.size(0);
                        searchRequest.source(searchSourceBuilder);

                        SearchResponse response = client().search(searchRequest, RequestOptions.DEFAULT);

                        Terms a = response.getAggregations().get("types");
                        String result = a.getBuckets().stream().map(b -> b.getKey() + ":" + b.getDocCount()).collect(Collectors.joining(","));
                        log.info("{} {} currently contains {} items ({})", factory, indexName, response.getHits().getTotalHits(), result);
                    } else {
                        log.error("No indexname in {}", this);
                    }
                    } catch (IndexNotFoundException ime) {
                    log.info("{} does exist yet ({})", indexName, ime.getMessage());
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            });
        }
    }


    @ManagedAttribute
    public String getTimeOutAsString() {
        return String.valueOf(timeOut);
    }

    @ManagedAttribute
    public void setTimeOutAsString(String timeOut) {
        this.timeOut = TimeUtils.parseDuration(timeOut).orElse(this.timeOut);
    }

    @ManagedAttribute
    public String getCommitDelayAsString() {
        return String.valueOf(commitDelay);
    }

    @ManagedAttribute
    @Value("${elasticSearch.commitDelay}")
    public void setCommitDelayAsString(String commitDelay) {
        this.commitDelay = TimeUtils.parseDuration(commitDelay).orElse(this.commitDelay);
    }


    protected final RestHighLevelClient client() {
        return factory.highLevelClient(getClass().getName());
    }

    protected abstract ElasticSearchIndex getIndex(String id, Class<?> clazz);

    protected String getIndexName(String id, Class<?> clazz) {
        ElasticSearchIndex index = getIndex(id, clazz);
        return indexNames.get(index);
    }

    protected T load(
        @NonNull String id,
        @NonNull Class<T> clazz) throws IOException {
        try {

            MultiGetRequest getRequest =
                new MultiGetRequest();
            getRequest.add(getIndexName(id, clazz), id);
            MultiGetResponse response = client().mget(getRequest, requestOptions());
            for (MultiGetItemResponse r : response.getResponses()) {
                if (! r.isFailed() && r.getResponse().isExists()) {
                    return transformResponse(r.getResponse(), clazz);
                }
            }
            return null;
        } catch(IndexNotFoundException ime) {
            log.warn("For {}:{} {}", id, clazz, ime.getMessage());
            return null;
        }
    }

    private T transformResponse(
        @NonNull GetResponse response,
        @NonNull Class<T> clazz) {
        if (!response.isExists()) {
            return null;
        }
        try {
            return LENIENT.readValue(response.getSourceAsString(), clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected CompletableFuture<T> loadAsync(
        @NonNull String id,
        @NonNull Class<T> clazz,
        @NonNull String indexName) {
        GetRequest getRequest = new GetRequest(indexName, id);
        CompletableFuture<GetResponse> responseFuture = new CompletableFuture<>();
        Cancellable all = client().getAsync(getRequest, requestOptions(), ESUtils.actionListener(responseFuture));

        return responseFuture.thenApply(r -> transformResponse(r, clazz));
    }


    /**
     * Returns a list with ${ids.length} entries. Empty optionals if not found.
     */
    @NonNull
    protected <S extends T> List<Optional<S>> loadAll(
        @NonNull Class<S> clazz,
        @NonNull String indexName,
        @NonNull String... ids) throws IOException {
        if (ids.length == 0) {
            return new ArrayList<>();
        }
        MultiGetRequest request = new MultiGetRequest();
        for (String id : ids) {
            if (StringUtils.isNotBlank(id)) {
                request.add(indexName, id);
            } else {
                log.debug("Ignoring empty id in list");
            }
        }

        MultiGetResponse responses = null;
        if (! request.getItems().isEmpty()) {
            responses =
                client().mget(request, requestOptions());
        }

        final Map<String, S> answerMap;
        if (responses != null && responses.iterator().hasNext()) {
            //List<T> answer = new ArrayList<>(responses.getResponses().length); // ES v > 0.20
            answerMap = new HashMap<>(responses.getResponses().length);
            for (MultiGetItemResponse response : responses) {
                if (response.isFailed()) {
                    if (response.getFailure() != null) {
                        log.error("{}", response.getFailure().getMessage(), response.getFailure().getFailure());
                    } else {
                        log.error("{}", response);
                    }
                } else {
                    if (response.getResponse().isExists()) {
                        try {
                            S item = LENIENT.readValue(response.getResponse().getSourceAsString(), clazz);
                            answerMap.put(response.getId(), item);
                        } catch (IllegalArgumentException iae) {
                            log.warn(iae.getMessage());
                        } catch (JsonProcessingException e) {
                            log.error(e.getMessage(), e);
                        }
                    } else {
                        log.debug("{}", response);
                    }
                }
            }
        } else {
            // no requests done
            answerMap = new HashMap<>();
        }
        List<Optional<S>> answer = new ArrayList<>(ids.length);
        for (String id : ids) {
            S object = answerMap.get(id);
            answer.add(Optional.ofNullable(object));
        }
        return answer;
    }


    protected boolean handlePaging(
        long offset,
        @Nullable Integer max,
        @NonNull SearchSourceBuilder searchBuilder,
        @NonNull QueryBuilder queryBuilder,
        @NonNull String... indexNames) throws IOException {
        if (offset != 0) {
            searchBuilder.from((int) offset);
        }
        if (max == null) {
            max = (int) executeCount(queryBuilder, indexNames).value;
        }
        return handleMaxZero(max, searchBuilder::size);
    }

    protected boolean handleMaxZero(Integer max,  Consumer<Integer> setSize) {
         boolean maxIsZero = false;
        if (max != null) {
            if (max == 0) { // NPA-532

                // If we don't do this, then we may get this kind of problems:
                // org.elasticsearch.ElasticsearchException$1: numHits must be > 0; please use TotalHitCountCollector if you just need the total hit count
                // at org.elasticsearch.ElasticsearchException.guessRootCauses(ElasticsearchException.java:644) ~[elasticsearch-7.6.0.jar:7.6.0]
                // This ia quick en dirty work around, because I can't quickly figure out the use 'TotalHitCountCollector'

                maxIsZero = true;
                max = 1;
            }
            setSize.accept(max);
        }
        return maxIsZero;
    }

    protected <S extends T> List<SearchResultItem<? extends S>> adapt(
        @NonNull final SearchHits hits,
        @NonNull final Class<S> clazz) {
        final SearchHit[] array = hits.getHits();
        return new AbstractList<SearchResultItem<? extends S>>() {
            @Override
            public SearchResultItem<S> get(int index) {
                try {
                    return getSearchResultItem(array[index], clazz);
                } catch(Throwable e) {
                    String string = "Error while reading " + array[index].getIndex() + "/" + array[index].getId() + " " + e.getClass().getName();
                    log.warn(string, e);
                    throw new RuntimeException(string, e); // this exception seems to disappear completely, so logged above.
                }
            }

            @Override
            public int size() {
                return array.length;
            }
        };
    }

    protected TotalHits executeCount(
        @NonNull QueryBuilder builder,
        @NonNull String... indexNames) throws IOException {
        SearchRequest request = new SearchRequest(indexNames);
        request.source(new SearchSourceBuilder().size(1).query(builder));
        return client().search(request, RequestOptions.DEFAULT).getHits().getTotalHits();
    }

    protected void buildHighlights(@NonNull SearchSourceBuilder searchBuilder, @Nullable Form form, List<SearchFieldDefinition> searchFields) {
        if (form != null && form.isHighlight()) {
            final HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.tagsSchema("styled");

            for (SearchFieldDefinition searchFieldDefinition : searchFields) {
                if (searchFieldDefinition.isActive() && searchFieldDefinition.isHighlight()) {
                    highlightBuilder.field(searchFieldDefinition.getName(), 100, 5);
                }
            }
            searchBuilder.highlighter(highlightBuilder);
        }
    }

    protected final <S extends T> S getObject(@NonNull SearchHit hit, @NonNull Class<S> clazz) throws IOException {
        return getObject(LENIENT.readTree(hit.getSourceRef().toBytesRef().bytes), clazz);
    }

    protected final <S extends T> S getObject(@NonNull JsonNode source, @NonNull Class<S> clazz) throws IOException {
        return LENIENT
            .readerFor(clazz)
            .readValue(source);
    }

    protected static <S> String[] filterFields(
        @NonNull S mo,
        @NonNull String[] fields,
        @NonNull String fallBack) {
        List<String> result = new ArrayList<>();
        JsonNode root = MAPPER.valueToTree(mo);
        for (String path : fields) {
            if (hasEsPath(root, path)) {
                result.add(path);
            }
        }
        if (result.isEmpty()) {
            result.add(fallBack);
        }
        return result.toArray(new String[0]);
    }

    static <S> boolean hasEsPath(
        @NonNull S o,
        @NonNull String path) {
        JsonNode root = MAPPER.valueToTree(o);
        return hasEsPath(root, path);
    }

    static boolean hasEsPath(
        @NonNull JsonNode o,
        @NonNull String path) {
        return _hasEsPath(o, path.split("\\."));
    }

    static private boolean _hasEsPath(
        @NonNull JsonNode object,
        @NonNull String... path) {
        String f = path[0];
        if (object.isArray()) {
            for (JsonNode o : object) {
                if (_hasEsPath(o, path)) {
                    return true;
                }
            }
            return false;
        } else {
            object = object.get(f);
            if (object == null) {
                return false;
            } else {
                if (path.length == 1) {
                    return true;
                } else {
                    return _hasEsPath(object, Arrays.copyOfRange(path, 1, path.length));
                }
            }
        }
    }

    protected <S extends T> SearchResultItem<S> getSearchResultItem(
        @NonNull SearchHit hit,
        @NonNull Class<S> clazz) throws IOException {
        S object = getObject(hit, clazz);
        SearchResultItem<S> item = new SearchResultItem<>(object);
        item.setScore(hit.getScore());
        Map<String, HighlightField> highlightFields = hit.getHighlightFields();
        if(highlightFields != null) {
            List<HighLight> highLights = new ArrayList<>();
            for(Map.Entry<String, HighlightField> entry : highlightFields.entrySet()) {
                String[] body = new String[entry.getValue().getFragments().length];
                int i = 0;
                for(Text fragment : entry.getValue().getFragments()) {
                    body[i++] = fragment.string();
                }
                highLights.add(new HighLight(entry.getKey(), body));
            }
            item.setHighlights(highLights);
        }
        return item;
    }


    protected Result.Total getTotal(SearchHits hits) {
        TotalHits totalHits = hits.getTotalHits();
        Result.TotalQualifier qualifier = getTotalQualifier(hits);
        return new Result.Total(totalHits.value, qualifier);
    }
    protected Result.TotalQualifier getTotalQualifier(SearchHits hits) {
        TotalHits totalHits = hits.getTotalHits();
        return Result.TotalQualifier.valueOf(totalHits.relation.name());
    }


    protected RequestOptions requestOptions() {
        return requestOptionsBuilder().build();
    }

     protected RequestOptions.Builder requestOptionsBuilder() {
        RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();

        builder.setRequestConfig(RequestConfig.custom().setConnectionRequestTimeout((int) timeOut.toMillis()).build());
        return builder;
    }


    @Override
    public String toString() {
        return getClass().getSimpleName() + "{indexNames='" + indexNames.values() + '}';
    }
}

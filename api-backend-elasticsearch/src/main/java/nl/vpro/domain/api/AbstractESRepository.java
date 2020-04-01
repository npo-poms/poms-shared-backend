package nl.vpro.domain.api;

import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.apache.lucene.search.TotalHits;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.get.*;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jmx.export.annotation.ManagedAttribute;

import com.fasterxml.jackson.databind.JsonNode;

import nl.vpro.domain.api.media.Redirector;
import nl.vpro.elasticsearch.ElasticSearchIndex;
import nl.vpro.elasticsearch7.ESClientFactory;
import nl.vpro.jackson2.Jackson2Mapper;
import nl.vpro.util.ThreadPools;
import nl.vpro.util.TimeUtils;


/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
public abstract class AbstractESRepository<T> {

    protected final Logger log = LoggerFactory.getLogger(getClass().getName());
    protected final Logger LOG_ERRORS = LoggerFactory.getLogger(getClass().getName() + ".ERRORS");

    protected final ESClientFactory factory;

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
    protected Duration commitDelay = Duration.ofSeconds(10);


    protected AbstractESRepository(
        @NotNull ESClientFactory factory) {
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
                        SearchResponse response = client()
                            .prepareSearch(indexName)
                            //.setTypes(getRelevantTypes())
                            .addAggregation(AggregationBuilders.terms("types")
                                    .field("_type")
                                    .order(BucketOrder.key(true))
                            )
                            .setSize(0)
                            .get();

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
    public String setCommitDelayAsString() {
        return String.valueOf(commitDelay);
    }

    @ManagedAttribute
    @Value("${elasticSearch.commitDelay}")
    public void setCommitDelayAsString(String commitDelay) {
        this.commitDelay = TimeUtils.parseDuration(commitDelay).orElse(this.commitDelay);
    }


    protected final Client client() {
        return factory.client(getClass());
    }

    protected abstract ElasticSearchIndex getIndex(String id, Class<?> clazz);

    protected String getIndexName(String id, Class<?> clazz) {
        ElasticSearchIndex index = getIndex(id, clazz);
        String indexName = indexNames.get(index);
        return indexName;
    }

    protected T load(
        @NonNull String id,
        @NonNull Class<T> clazz) {
        try {

            MultiGetRequest getRequest =
                new MultiGetRequest();
            getRequest.add(getIndexName(id, clazz), id);
            ActionFuture<MultiGetResponse> future = client().multiGet(getRequest);
            MultiGetResponse response = future.get(timeOut.toMillis(), TimeUnit.MILLISECONDS);
            for (MultiGetItemResponse r : response.getResponses()) {
                if (! r.isFailed() && r.getResponse().isExists()) {
                    return transformResponse(r.getResponse(), clazz);
                }
            }
            return null;
        } catch(IndexNotFoundException ime) {
            return null;
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ie);
        } catch(ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    private T transformResponse(
        @NonNull GetResponse response,
        @NonNull Class<T> clazz) {
        if (!response.isExists()) {
            return null;
        }
        try {
            return Jackson2Mapper.LENIENT.readValue(response.getSourceAsString(), clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected CompletableFuture<T> loadAsync(
        @NonNull String id,
        @NonNull Class<T> clazz,
        @NonNull String indexName) {
        ActionFuture<GetResponse> all = client().prepareGet(indexName, "_all", id).execute();
        CompletableFuture<GetResponse> reponseFuture = ESUtils.fromActionFuture(all);

        return reponseFuture.thenApply(r -> transformResponse(r, clazz));
    }


    /**
     * Returns a list with ${ids.length} entries. Nulls if not found.
     */
    protected <S extends T> List<Optional<S>> loadAll(
        @NonNull Class<S> clazz,
        @NonNull String indexName,
        @NonNull String... ids) {
        try {
            if (ids.length == 0) {
                return new ArrayList<>();
            }
            MultiGetRequest request = new MultiGetRequest();
            for(String id : ids) {
                request.add(indexName, id);
            }
            ActionFuture<MultiGetResponse> future =
                client()
                    .multiGet(request);

            MultiGetResponse responses = future.get(timeOut.toMillis(), TimeUnit.MILLISECONDS);
            if(responses == null || ! responses.iterator().hasNext()) {
                return null;
            }

            //List<T> answer = new ArrayList<>(responses.getResponses().length); // ES v > 0.20
            Map<String, S> answerMap = new HashMap<>(responses.getResponses().length);
            for(MultiGetItemResponse response : responses) {
                if(response.isFailed()) {
                    if (response.getFailure() != null) {
                        log.error("{}", response.getFailure().getMessage(), response.getFailure().getFailure());
                    } else {
                        log.error("{}", response);
                    }
                } else {
                    if (response.getResponse().isExists()) {
                        try {
                            S item = Jackson2Mapper.LENIENT.readValue(response.getResponse().getSourceAsString(), clazz);
                            answerMap.put(response.getId(), item);
                        } catch (IllegalArgumentException iae) {
                            log.warn(iae.getMessage());
                        }
                    } else {
                        log.debug("{}", response);
                    }
                }
            }
            List<Optional<S>> answer = new ArrayList<>(ids.length);
            for (String id : ids) {
                S object = answerMap.get(id);
                answer.add(Optional.ofNullable(object));
            }
            return answer;
        } catch(InterruptedException | ExecutionException | IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }



    protected boolean handlePaging(
        long offset,
        @Nullable Integer max,
        @NonNull SearchSourceBuilder searchBuilder,
        @NonNull QueryBuilder queryBuilder,
        @NonNull String... indexNames) {
        if (offset != 0) {
            searchBuilder.from((int) offset);
        }
        if (max == null) {
            max = (int) executeCount(queryBuilder, indexNames);
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

    protected long executeCount(
        @NonNull QueryBuilder builder,
        @NonNull String... indexNames) {
        return client().prepareSearch(indexNames)
            .setSource(new SearchSourceBuilder().size(0).query(builder)).get().getHits().getTotalHits().value;

    }

    protected void buildHighlights(@NonNull SearchSourceBuilder searchBuilder, @Nullable Form form, List<SearchFieldDefinition> searchFields) {
        if (form != null && form.isHighlight()) {
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.tagsSchema("styled");

            for (SearchFieldDefinition highlight : searchFields) {
                if (highlight.isHighlight()) {
                    highlightBuilder.field(highlight.getName(), 100, 5);
                }
            }
            searchBuilder.highlighter(highlightBuilder);
        }
    }


    protected final <S extends T> S getObject(@NonNull SearchHit hit, @NonNull Class<S> clazz) throws IOException {
        return Jackson2Mapper.getLenientInstance().readerFor(clazz).readValue(hit.getSourceAsString());
    }



    protected static <S> String[] filterFields(
        @NonNull S mo,
        @NonNull String[] fields,
        @NonNull String fallBack) {
        List<String> result = new ArrayList<>();
        JsonNode root = Jackson2Mapper.getInstance().valueToTree(mo);
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
        JsonNode root = Jackson2Mapper.getInstance().valueToTree(o);
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

    private <S extends T> SearchResultItem<S> getSearchResultItem(
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

    protected void redirectTextMatchers(
        @Nullable TextMatcherList list) {
        if (list == null) {
            return;
        }
        List<TextMatcher> l = list.asList();
        for (int i = 0; i < l.size(); i++) {
            TextMatcher matcher = l.get(i);
            Optional<String> redirect = redirect(matcher.getValue());
            if (redirect.isPresent()) {
                matcher = new TextMatcher(redirect.get(), matcher.getMatch());
                matcher.setMatchType(matcher.getMatchType());
                l.set(i, matcher);
            }

        }
    }

    public final Optional<String> redirect(String mid) {
        return Optional.ofNullable(
            getDirectsRepository().redirects().getMap().get(mid)
        );
    }

    protected Redirector getDirectsRepository() {
        throw new UnsupportedOperationException();
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

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{indexNames='" + indexNames.values() + '}';
    }
}

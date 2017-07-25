package nl.vpro.domain.api;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;

import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.count.CountRequest;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.get.*;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.QuerySourceBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.highlight.HighlightField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jmx.export.annotation.ManagedAttribute;

import com.fasterxml.jackson.databind.JsonNode;

import nl.vpro.domain.api.media.MediaRepository;
import nl.vpro.elasticsearch.ESClientFactory;
import nl.vpro.jackson2.Jackson2Mapper;
import nl.vpro.util.ThreadPools;
import nl.vpro.util.TimeUtils;


/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
@Slf4j
@ToString
public abstract class AbstractESRepository<T> {

    protected final Logger LOG_ERRORS = LoggerFactory.getLogger(getClass().getName() + ".ERRORS");

    private final ESClientFactory factory;

    protected Integer facetLimit = 10;
    protected Duration timeOut = Duration.ofSeconds(15);

    protected String indexName = null;

    private final Set<String> loadTypes;


    public AbstractESRepository(@NotNull ESClientFactory factory) {
        this.factory = factory;
        if (this.factory == null) {
            throw new IllegalArgumentException("The ES client factory cannot be null");
        }
        loadTypes = new HashSet<>(Arrays.asList(getLoadTypes()));
    }

    @PostConstruct
    public void logIntro() {
        log.info("ES Repository {} {}", this, factory);
        ThreadPools.backgroundExecutor.execute(() -> {
            try {
                SearchResponse response = client()
                    .prepareSearch(getIndexName())
                    .setTypes(getRelevantTypes())
                    .addAggregation(AggregationBuilders.terms("types").field("_type").order(Terms.Order.term(true)))
                    .setSize(0)
                    .get();

                Terms a  = response.getAggregations().get("types");
                String result = a.getBuckets().stream().map(b -> b.getKey() + ":" + b.getDocCount()).collect(Collectors.joining(","));
                log.info("{}\n{} currently contains {} items ({})", factory, getIndexName(), response.getHits().getTotalHits(), result);
            } catch (IndexMissingException ime) {
                log.info("{} does exist yet ({})",
                    getIndexName(), ime.getMessage());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        });
    }

    public Integer getFacetLimit() {
        return facetLimit;
    }

    public void setFacetLimit(Integer facetLimit) {
        this.facetLimit = facetLimit;
    }

    public Duration getTimeOut() {
        return timeOut;
    }

    public void setTimeOut(Duration timeOut) {
        this.timeOut = timeOut;
    }


    @ManagedAttribute
    public String getTimeOutAsString() {
        return String.valueOf(timeOut);
    }

    @ManagedAttribute
    public void setTimeOutAsString(String timeOut) {
        this.timeOut = TimeUtils.parseDuration(timeOut).orElse(this.timeOut);
    }

    protected final Client client() {
        return factory.client(getClass());
    }

    /**
     * Returns all types relevant, this e.g. als includes 'deleted' objects types
     */
    abstract protected  String[] getRelevantTypes();

    /**
     * All type relevant for loading objects.
     */
    abstract protected String[] getLoadTypes();


    protected T load(String id, Class<T> clazz) {
        try {
            MultiGetRequest getRequest =
                new MultiGetRequest();
            for (String s : loadTypes) {
                getRequest.add(indexName, s, id);
            }
            ActionFuture<MultiGetResponse> future = client().multiGet(getRequest);
            MultiGetResponse response = future.get(timeOut.toMillis(), TimeUnit.MILLISECONDS);
            for (MultiGetItemResponse r : response.getResponses()) {
                if (! r.isFailed() && r.getResponse().isExists()) {
                    return transformResponse(r.getResponse(), clazz);
                }
            }
            return null;
        } catch(org.elasticsearch.indices.IndexMissingException ime) {
            return null;
        } catch(InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    private T transformResponse(GetResponse response, Class<T> clazz) {
        if (!response.isExists()) {
            return null;
        }
        try {
            return Jackson2Mapper.LENIENT.readValue(response.getSourceAsString(), clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected CompletableFuture<T> loadAsync(String id, Class<T> clazz, String indexName) {
        CompletableFuture<GetResponse> reponseFuture = ESUtils.fromListenableActionFuture(client().prepareGet(indexName, "_all", id).execute());
        return reponseFuture.thenApply(r -> transformResponse(r, clazz));
    }


    /**
     * Returns a list with ${ids.length} entries. Nulls if not found.
     */
    protected <S extends T> List<S> loadAll(Class<S> clazz, String indexName, String... ids) {
        try {
            if (ids.length == 0) {
                return new ArrayList<S>();
            }
            MultiGetRequest request = new MultiGetRequest();
            for(String id : ids) {
                for (String t : loadTypes) {
                    request.add(indexName, t, id);
                }
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
                if(response.getResponse().isExists()) {
                    S item = Jackson2Mapper.INSTANCE.readValue(response.getResponse().getSourceAsString(), clazz);
                    answerMap.put(response.getId(), item);
                }
            }
            List<S> answer = new ArrayList<>(ids.length);
            for (String id : ids) {
                answer.add(answerMap.get(id));
            }
            return answer;
        } catch(InterruptedException | ExecutionException | IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }



    protected void handlePaging(long offset, Integer max, SearchSourceBuilder searchBuilder, QueryBuilder queryBuilder, String indexName) {
        if (offset != 0) {
            searchBuilder.from((int) offset);
        }
        if (max == null) {
            try {
                max = (int) executeCount(queryBuilder, indexName);
                searchBuilder.size(max);
            } catch(ExecutionException | InterruptedException e) {
                log.warn(e.getMessage());
            }
        } else {
            searchBuilder.size(max);
        }
    }

    protected <S extends T> List<SearchResultItem<? extends S>> adapt(final SearchHits hits, final Class<S> clazz) {
        final SearchHit[] array = hits.getHits();
        return new AbstractList<SearchResultItem<? extends S>>() {
            @Override
            public SearchResultItem<S> get(int index) {
                try {
                    return getSearchResultItem(array[index], clazz);
                } catch(Throwable e) {
                    String string = "Error while reading " + array[index].getIndex() + "/" + array[index].getType() + "/" + array[index].getId() + " " + e.getClass().getName();
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

    protected long executeCount(QueryBuilder builder, String indexName) throws ExecutionException, InterruptedException {
        CountRequest countRequest = new CountRequest();
        countRequest.indices(indexName);
        QuerySourceBuilder sourceBuilder = new QuerySourceBuilder();
        countRequest.source(sourceBuilder.setQuery(builder));
        ActionFuture<CountResponse> searchResponseFuture = client().count(countRequest);
        return searchResponseFuture.get().getCount();
    }

    protected void buildHighlights(SearchSourceBuilder searchBuilder, Form form, List<SearchFieldDefinition> searchFields) {
        if (form != null && form.isHighlight()) {
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.tagsSchema("styled");

            for (SearchFieldDefinition highlight : searchFields) {
                if (highlight.isHighlight()) {
                    highlightBuilder.field(highlight.getName(), 100, 5);
                }
            }
            searchBuilder.highlight(highlightBuilder);
        }
    }


    protected final <S extends T> S getObject(SearchHit hit, Class<S> clazz) throws IOException {
        return Jackson2Mapper.getLenientInstance().readerFor(clazz).readValue(hit.getSourceAsString());
    }



    protected static <S> String[] filterFields(S mo, String[] fields, String fallBack) {
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
        return result.toArray(new String[result.size()]);
    }

    static <S> boolean hasEsPath(S o, String path) {
        JsonNode root = Jackson2Mapper.getInstance().valueToTree(o);
        return hasEsPath(root, path);
    }

    static boolean hasEsPath(JsonNode o, String path) {
        return _hasEsPath(o, path.split("\\."));
    }

    static private boolean _hasEsPath(JsonNode object, String... path) {
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

    private <S extends T> SearchResultItem<S> getSearchResultItem(SearchHit hit, Class<S> clazz) throws IOException {
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

    protected void redirectTextMatchers(TextMatcherList list) {
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

    protected MediaRepository getDirectsRepository() {
        throw new UnsupportedOperationException();
    }


    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }
}

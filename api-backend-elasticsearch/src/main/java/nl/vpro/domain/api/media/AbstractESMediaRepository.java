package nl.vpro.domain.api.media;

import lombok.*;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.validation.constraints.Null;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.transport.TransportSerializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import nl.vpro.domain.api.*;
import nl.vpro.domain.api.profile.ProfileDefinition;
import nl.vpro.domain.media.MediaLoader;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.media.support.Workflow;
import nl.vpro.elasticsearch.Distribution;
import nl.vpro.elasticsearch.ElasticSearchIndex;
import nl.vpro.elasticsearch.highlevel.HighLevelClientFactory;
import nl.vpro.elasticsearchclient.IndexHelper;
import nl.vpro.media.domain.es.ApiRefsIndex;
import nl.vpro.util.TimeUtils;

import static nl.vpro.media.domain.es.ApiMediaIndex.APIMEDIA;

/**
 * @author Michiel Meeuwissen
 * @since 3.7
 */

@ToString(callSuper = true)
public abstract class AbstractESMediaRepository extends AbstractESRepository<MediaObject> implements MediaLoader, Redirector {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final IndexHelper helper;
    private final IndexHelper refsHelper;

    private final Distribution distribution = Distribution.ELASTICSEARCH;


    protected AbstractESMediaRepository(HighLevelClientFactory client) {
        super(client);
        this.helper = IndexHelper.builder()
            .log(log)
            .client(client)
            .settings(APIMEDIA.settings())
            .mapping(APIMEDIA.mapping())
            .build();
        this.refsHelper = IndexHelper.builder()
            .log(log)
            .client(client)
            .settings(ApiRefsIndex.APIMEDIA_REFS.settings())
            .mapping(ApiRefsIndex.APIMEDIA_REFS.mapping())
            .build();
    }


    @PostConstruct
    public void init() {
        helper.setIndexName(getIndexName());
        refsHelper.setIndexName(getRefsIndexName());

    }

    public String getRefsIndexName() {
        return getIndexName() + ApiRefsIndex.POSTFIX;
    }

    public String getIndexName() {
        return indexNames.get(APIMEDIA);
    }

    @Value("${elasticSearch.media.index}")
    public void setIndexName(@NonNull String indexName) {
        super.setIndexName(APIMEDIA, indexName);
    }

    @Override
    @Value("${elasticSearch.media.facetLimit}")
    public void setFacetLimit(Integer facetLimit) {
        super.setFacetLimit(facetLimit);
    }


    @Value("${elasticSearch.media.timeout}")
    public void setTimeout(String timeout) {
        super.setTimeOut(TimeUtils.parseDuration(timeout).orElse(Duration.ofSeconds(15)));
    }

    @Override
    protected ElasticSearchIndex getIndex(String id, Class<?> clazz) {
        if (MediaObject.class.isAssignableFrom(clazz)) {
            return APIMEDIA;
        } else {
            throw new IllegalStateException();
        }
    }



    @SneakyThrows(IOException.class)
    @Override
    public MediaObject load(boolean loadDeleted, String mid) {
        mid = redirect(mid).orElse(mid);
        MediaObject mediaObject = load(mid, MediaObject.class);
        if (mediaObject == null || loadDeleted ||  Workflow.PUBLICATIONS.contains(mediaObject.getWorkflow())) {
            return mediaObject;
        } else {
            return null;
        }
    }

    @SneakyThrows(IOException.class)
    @Override
    @NonNull
    public List<MediaObject> loadAll(boolean loadDeleted, List<String> ids) {
        return loadAll(MediaObject.class, ids)
            .stream()
            .map(o -> o.orElse(null))
            .map(o -> o == null || loadDeleted || Workflow.PUBLICATIONS.contains(o.getWorkflow()) ? o : null)
            .collect(Collectors.toList());
    }


    @NonNull
    protected <S extends MediaObject> List<Optional<S>> loadAll(Class<S> clazz, List<String> ids) throws IOException {
        ids = ids.stream()
            .map(id -> redirect(id)
            .orElse(id))
            .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        return loadAll(clazz, getIndexName(ids.get(0), clazz), ids.toArray(new String[0]));
    }


    /**
     * Defaulting version of {@link #mediaSearchRequest(ProfileDefinition, AbstractMediaForm, MediaObject, BoolQueryBuilder, long, Integer)}
     * Where  mediaObject is <code>null</code>
     */
    final protected SearchRequestWrapper mediaSearchRequest(
        @Nullable ProfileDefinition<MediaObject> profile,
        @Nullable AbstractMediaForm form,
        @NonNull BoolQueryBuilder rootQuery,
        long offset,
        @Nullable Integer max) throws IOException {

        return mediaSearchRequest(
            profile,
            form,
            null,
            rootQuery,
            offset,
            max);
    }

    /**
     * Builds a {@link SearchRequest}
     *
     * @param profile The profile will be added as a filter on the resulting query
     * @param form    Handles {@link AbstractMediaForm#getSearches()} and {@link MediaForm#getSortFields()}. Also, if applicate it
     *                will handle {@link MediaForm#getFacets()}
     */
    final protected SearchRequestWrapper mediaSearchRequest(
        @Nullable ProfileDefinition<MediaObject> profile,
        @Nullable AbstractMediaForm form,
        @Nullable MediaObject mediaObject,
        @NonNull BoolQueryBuilder rootQuery,
        long offset,
        Integer max) throws IOException {
        SearchRequest request = new SearchRequest(getIndexName());
        SearchSourceBuilder searchSourceBuilder = mediaSearchBuilder(profile, form, mediaObject, rootQuery, offset, max);
        boolean maxWasZero = handleMaxZero(max, searchSourceBuilder::size);

        request.source(
            searchSourceBuilder
        );
        return new SearchRequestWrapper(request, maxWasZero);
    }

    final protected SearchSourceBuilder mediaSearchBuilder(
        @Null ProfileDefinition<MediaObject> profile,
        @NonNull AbstractMediaForm form,
        @NonNull BoolQueryBuilder filter,
        long offset,
        Integer max) throws IOException {
        return mediaSearchBuilder(profile, form, null, filter, offset, max);
    }

    /**
     * Here is where the actual search query get built
     *
     * Builds  an Elastic Search {@link QueryBuilder} from a {@link MediaSearch}
     * If a score is present it will add it to the query see {@link FunctionScoreQueryBuilder }
     * If the {@link MediaForm} has facets {@link MediaFacets} it will add them to the query {@link QueryBuilder}
     * @param profile
     * @param form the data necessary to build the query
     * @param mediaObject we use the mid to sort in the nested fields
     * @param rootQuery
     * @param offset response position (for pagination)
     * @param max amount of results
     * @return the ES query ready to be executed.
     */
    final protected SearchSourceBuilder mediaSearchBuilder(
        @Nullable ProfileDefinition<MediaObject> profile,
        @Nullable AbstractMediaForm form,
        @Nullable MediaObject mediaObject,
        @NonNull BoolQueryBuilder rootQuery,
        long offset,
        @Nullable Integer max) throws IOException {

        SearchSourceBuilder searchBuilder = new SearchSourceBuilder();
        mediaSearchBuild(searchBuilder, profile, form, mediaObject, rootQuery, offset, max);
        return searchBuilder;
    }
    final protected void mediaSearchBuild(
        SearchSourceBuilder searchBuilder,
        @Nullable ProfileDefinition<MediaObject> profile,
        @Nullable AbstractMediaForm form,
        @Nullable MediaObject mediaObject,
        @NonNull BoolQueryBuilder rootQuery,
        long offset,
        @Nullable Integer max) throws IOException {


        // Handle profile and workflow filtering
        ESMediaFilterBuilder.filter(profile, rootQuery);

        /* This use to be here, I don't know why. I think it is silly.
        // Filtering _should_ influene aggragation
        if (rootQuery.hasClauses()) {
            searchBuilder.post_filter(rootQuery);
        }*/

        QueryBuilder queryBuilder = ESMediaQueryBuilder
            .query("", form != null ? form.getSearches() : null);
        rootQuery.must(queryBuilder);

        if (isScore()) {
            searchBuilder.query(
                ESMediaScoreBuilder.score(rootQuery, Instant.now())
            );
        } else {
            searchBuilder.query(rootQuery);
        }

        if (form instanceof MediaForm) {
            ESMediaFacetsBuilder.buildMediaFacets("", searchBuilder, (MediaForm) form, rootQuery);

            ESMediaSortHandler.sort(searchBuilder, (MediaForm) form, mediaObject);
        }

        buildHighlights(searchBuilder, form, ESMediaQueryBuilder.SEARCH_FIELDS);

        handlePaging(offset, max, searchBuilder, queryBuilder, indexNames.get(APIMEDIA));

        log.debug("ES query: {}", searchBuilder);
    }

    /**
     *
     * @param request The ElasticSearch {@link SearchRequest} to execute and adapt to a API search result.
     * @param facets The requested facets. Will not be used to change the search request, but only to properly extract the facet results
     */
    protected <S extends MediaObject> GenericMediaSearchResult<S> executeSearchRequest(
        @NonNull SearchRequestWrapper request,
        @Nullable MediaFacets facets,
        long offset,
        @Nullable Integer max,
        @NonNull Class<S> clazz) throws IOException {

        try {
            SearchResponse response  = client().search(request.getRequest(),requestOptions());
            SearchHits hits = response.getHits();

            Duration took = Duration.ofMillis(response.getTook().getMillis());
            List<SearchResultItem<? extends S>> adapted = request.maxWasZero ? Collections.emptyList() : adapt(hits, clazz);

            MediaFacetsResult facetsResult =
                ESMediaFacetsHandler.extractMediaFacets(response, facets, this);
            GenericMediaSearchResult<S> result =  new GenericMediaSearchResult<>(adapted,
                facetsResult,
                offset,
                max,
                getTotal(hits)
            );
            result.setTook(took);
            return result;
        } catch (TransportSerializationException e) {
            String detail = e.getDetailedMessage();
            log.warn(e.getMessage() + ":" + detail);
            throw e;
        }
    }


    /**
     * Changes the form to 'redirect' all occurances of mids in it.
     */
    protected <T extends AbstractMediaForm> T redirectForm(T form) {
        if (form == null) {
            return null;
        }
        redirectMediaSearch(form.getSearches());
        return form;
    }





    protected void redirectMemberRefFacet(MemberRefFacet facet) {
        if (facet == null) {
            return;
        }
        redirectMediaSearch(facet.getFilter());
        redirectMemberRefSearch(facet.getSubSearch());
    }

    private void redirectMemberRefSearch(MemberRefSearch search) {
        if (search == null) {
            return;
        }
        redirectTextMatchers(search.getMediaIds());
    }

    public abstract  boolean isScore();

    @Getter
    @ToString
    public static class SearchRequestWrapper {
        final SearchRequest request;
        final boolean maxWasZero;

        SearchRequestWrapper(SearchRequest request, boolean maxWasZero) {
            this.request = request;
            this.maxWasZero = maxWasZero;
        }

    }

}

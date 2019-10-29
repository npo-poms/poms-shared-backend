package nl.vpro.domain.api.media;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.validation.constraints.Null;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
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

import nl.vpro.domain.api.AbstractESRepository;
import nl.vpro.domain.api.ESFilterBuilder;
import nl.vpro.domain.api.GenericMediaSearchResult;
import nl.vpro.domain.api.SearchResultItem;
import nl.vpro.domain.api.profile.ProfileDefinition;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.elasticsearch.ESClientFactory;
import nl.vpro.elasticsearch.IndexHelper;
import nl.vpro.media.domain.es.ApiMediaIndex;
import nl.vpro.media.domain.es.ApiRefsIndex;
import nl.vpro.util.TimeUtils;

/**
 * @author Michiel Meeuwissen
 * @since 3.7
 */

@ToString(callSuper = true)
public abstract class AbstractESMediaRepository extends AbstractESRepository<MediaObject> implements MediaLoader {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final IndexHelper helper;
    private final IndexHelper refsHelper;

    @Getter
    @Setter
    private boolean score = true;

    protected AbstractESMediaRepository(ESClientFactory client) {
        super(client);
        this.helper = IndexHelper.builder()
            .log(log)
            .client(client)
            .settings(ApiMediaIndex.APIMEDIA.settings())
            .mappings(ApiMediaIndex.APIMEDIA.mappingsAsMap())
            .build();
        this.refsHelper = IndexHelper.builder()
            .log(log)
            .client(client)
            .settings(ApiRefsIndex.APIMEDIA_REFS.settings())
            .mappings(ApiRefsIndex.APIMEDIA_REFS.mappingsAsMap())
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

    @Override
    @Value("${elasticSearch.media.index}")
    public void setIndexName(@NonNull String indexName) {
        super.setIndexName(indexName);
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
    public MediaObject load(String mid) {
        mid = redirect(mid).orElse(mid);
        return load(mid, MediaObject.class);
    }

    @Override
    public List<MediaObject> loadAll(List<String> ids) {
        return loadAll(MediaObject.class, ids);
    }


    protected <S extends MediaObject> List<S> loadAll(Class<S> clazz, List<String> ids) {
        ids = ids.stream().map(id -> redirect(id).orElse(id)).collect(Collectors.toList());
        return loadAll(clazz, indexName, ids.toArray(new String[0]));
    }


    /**
     * Defaulting version of {@link #mediaSearchRequest(ProfileDefinition, AbstractMediaForm, MediaObject, BoolQueryBuilder, long, Integer)}
     * Where  mediaObject is <code>null</code>
     */
    final protected SearchRequest mediaSearchRequest(
        @Nullable ProfileDefinition<MediaObject> profile,
        @Nullable AbstractMediaForm form,
        @NonNull BoolQueryBuilder rootQuery,
        long offset,
        @Nullable Integer max) {

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
    final protected SearchRequest mediaSearchRequest(
        @Nullable ProfileDefinition<MediaObject> profile,
        @Nullable AbstractMediaForm form,
        @Nullable MediaObject mediaObject,
        @NonNull BoolQueryBuilder rootQuery,
        long offset,
        Integer max) {
        SearchRequest request = new SearchRequest(indexName);

        request.source(
            mediaSearchBuilder(profile, form, mediaObject, rootQuery, offset, max)
        );
        return request;
    }

    final protected SearchSourceBuilder mediaSearchBuilder(
        @Null ProfileDefinition<MediaObject> profile,
        @NonNull AbstractMediaForm form,
        @NonNull BoolQueryBuilder filter,
        long offset,
        Integer max) {
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
        @Nullable Integer max) {

        SearchSourceBuilder searchBuilder = new SearchSourceBuilder();
        // Handle profile and workflow filtering
        ESFilterBuilder.filter(profile, rootQuery);

        /* This use to be here, I don't know why. I think it is silly.
        // Filtering _should_ influene aggragation
        if (rootQuery.hasClauses()) {
            searchBuilder.post_filter(rootQuery);
        }*/

        QueryBuilder queryBuilder = ESMediaQueryBuilder
            .query("", form != null ? form.getSearches() : null);
        rootQuery.must(queryBuilder);

        if (score) {
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

        handlePaging(offset, max, searchBuilder, queryBuilder, indexName);

        log.debug("ES query: {}", searchBuilder);

        return searchBuilder;
    }

    /**
     *
     * @param request The ElasticSearch {@link SearchRequest} to execute and adapt to a API search result.
     * @param facets The requested facets. Will not be used to change the search request, but only to properly extract the facet results
     */
    protected <S extends MediaObject> GenericMediaSearchResult<S> executeSearchRequest(
        @NonNull SearchRequest request,
        @Nullable MediaFacets facets,
        long offset,
        @Nullable Integer max,
        @NonNull Class<S> clazz) {
        ActionFuture<SearchResponse> searchResponseFuture = client()
            .search(request)
            ;

        try {
            SearchResponse response = searchResponseFuture
                .actionGet(timeOut.toMillis(), TimeUnit.MILLISECONDS)
                ;
            SearchHits hits = response.getHits();

            List<SearchResultItem<? extends S>> adapted = adapt(hits, clazz);

            MediaFacetsResult facetsResult =
                ESMediaFacetsHandler.extractMediaFacets(response, facets, this);
            return new GenericMediaSearchResult<>(adapted, facetsResult, offset, max, hits.getTotalHits().value);
        } catch (TransportSerializationException e) {
            String detail = e.getDetailedMessage();
            log.warn(e.getMessage() + ":" + detail);
            throw e;
        } catch (SearchPhaseExecutionException ee) {
            throw ee;
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


    protected void redirectMediaSearch(MediaSearch search) {
        if (search == null) {
            return;
        }
        redirectTextMatchers(search.getMediaIds());
        redirectTextMatchers(search.getDescendantOf());
        redirectTextMatchers(search.getEpisodeOf());
        redirectTextMatchers(search.getMemberOf());
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

}

package nl.vpro.domain.api.media;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.validation.constraints.Null;

import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
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
import nl.vpro.media.domain.es.MediaESType;
import nl.vpro.util.TimeUtils;

/**
 * @author Michiel Meeuwissen
 * @since 3.7
 */

@ToString(callSuper = true)
public abstract class AbstractESMediaRepository extends AbstractESRepository<MediaObject> implements MediaLoader {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final IndexHelper helper;

    @Getter
    @Setter
    private boolean score = true;

    protected AbstractESMediaRepository(ESClientFactory client) {
        super(client);
        this.helper = IndexHelper.builder()
            .log(log)
            .client(client)
            .settings(ApiMediaIndex::source)
            .mappings(ApiMediaIndex.mappingsAsMap())
            .build();
    }


    @PostConstruct
    public void init() {
        helper.setIndexName(indexName);
    }

    @Override
    @Value("${elasticSearch.media.index}")
    public void setIndexName(@Nonnull String indexName) {
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


    @Override
    protected String[] getLoadTypes() {
        return MediaESType.mediaObjects();
    }

    protected <S extends MediaObject> List<S> loadAll(Class<S> clazz, List<String> ids) {
        ids = ids.stream().map(id -> redirect(id).orElse(id)).collect(Collectors.toList());
        return loadAll(clazz, indexName, ids.toArray(new String[0]));
    }


    /**
     * Defaulting version of {@link #mediaSearchRequest(String[], ProfileDefinition, AbstractMediaForm, MediaObject, BoolQueryBuilder, long, Integer)}
     * Where the types is set to {@link #getLoadTypes()} and mediaObject is <code>null</code>
     */
    final protected SearchRequest mediaSearchRequest(
        @Nullable ProfileDefinition<MediaObject> profile,
        @Nullable AbstractMediaForm form,
        @Nonnull BoolQueryBuilder boolQueryBuilder,
        long offset,
        @Nullable Integer max) {

        return mediaSearchRequest(
            getLoadTypes(),
            profile,
            form,
            null,
            boolQueryBuilder,
            offset,
            max);
    }

    /**
     * Builds a {@link SearchRequest}
     *
     * @param types   Which types to search (defaults to {@link #getLoadTypes()})
     * @param profile The profile will be added as a filter on the resulting query
     * @param form    Handles {@link AbstractMediaForm#getSearches()} and {@link MediaForm#getSortFields()}. Also, if applicate it
     *                will handle {@link MediaForm#getFacets()}
     */
    final protected SearchRequest mediaSearchRequest(
        @Nonnull String[] types,
        @Nullable ProfileDefinition<MediaObject> profile,
        @Nullable AbstractMediaForm form,
        @Nullable MediaObject mediaObject,
        @Nonnull BoolQueryBuilder filter,
        long offset,
        Integer max) {
        SearchRequest request = new SearchRequest(indexName);
        request.types(types);

        request.source(
            mediaSearchBuilder(profile, form, mediaObject, filter, offset, max)
        );
        return request;
    }

    final protected SearchSourceBuilder mediaSearchBuilder(
        @Null ProfileDefinition<MediaObject> profile,
        @Nonnull AbstractMediaForm form,
        @Nullable BoolQueryBuilder filter,
        long offset,
        Integer max) {
        return mediaSearchBuilder(profile, form, null, filter, offset, max);
    }

    /**
     * TODO javadoc
     *
     */
    final protected SearchSourceBuilder mediaSearchBuilder(
        @Nullable ProfileDefinition<MediaObject> profile,
        @Nullable AbstractMediaForm form,
        @Nullable MediaObject mediaObject,
        @Nonnull BoolQueryBuilder filter,
        long offset,
        @Nullable Integer max) {

        SearchSourceBuilder searchBuilder = new SearchSourceBuilder();

        // Handle profile filtering
        ESFilterBuilder.filter(profile, filter);
        if (filter.hasClauses()) {
            searchBuilder.postFilter(filter);
        }
        QueryBuilder queryBuilder = ESMediaQueryBuilder.query("", form != null ? form.getSearches() : null);
        if (score) {
            searchBuilder.query(
                ESMediaScoreBuilder.score(queryBuilder, Instant.now())
            );
        } else {
            searchBuilder.query(queryBuilder);
        }

        if (form instanceof MediaForm) {
            ESMediaFacetsBuilder.buildMediaFacets("", searchBuilder, (MediaForm) form, filter);

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
        @Nonnull SearchRequest request,
        @Nullable MediaFacets facets,
        long offset,
        @Nullable Integer max,
        @Nonnull Class<S> clazz) {
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
            return new GenericMediaSearchResult<>(adapted, facetsResult, offset, max, hits.getTotalHits());
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

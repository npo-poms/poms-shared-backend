package nl.vpro.domain.api.media;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.transport.TransportSerializationException;
import org.springframework.beans.factory.annotation.Value;

import nl.vpro.domain.api.AbstractESRepository;
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
@Slf4j
@ToString(callSuper = true)
public abstract class AbstractESMediaRepository extends AbstractESRepository<MediaObject> implements MediaLoader {

    private final IndexHelper helper;

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
    public void setIndexName(String indexName) {
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
        return loadAll(clazz, indexName, ids.toArray(new String[ids.size()]));
    }


    /**
     * Defaulting version of {@link #searchRequest(String[], ProfileDefinition, AbstractMediaForm, MediaObject, BoolQueryBuilder, long, Integer)}
     * Where the types is set to {@link #getLoadTypes()} and mediaObject is <code>null</code>
     */
    final protected SearchRequest searchRequest(
        ProfileDefinition<MediaObject> profile,
        AbstractMediaForm form,
        long offset,
        Integer max) {

        return searchRequest(
            getLoadTypes(),
            profile,
            form,
            null,
            QueryBuilders.boolQuery(),
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
    final protected SearchRequest searchRequest(
        String[] types,
        ProfileDefinition<MediaObject> profile,
        AbstractMediaForm form,
        MediaObject mediaObject,
        BoolQueryBuilder filter,
        long offset,
        Integer max) {
        SearchRequest request = new SearchRequest(indexName);
        request.types(types);
        request.source(
            searchBuilder(profile, form, mediaObject, filter, offset, max)
        );
        return request;
    }

    final protected SearchSourceBuilder searchBuilder(
        ProfileDefinition<MediaObject> profile,
        AbstractMediaForm form,
        BoolQueryBuilder filter,
        long offset,
        Integer max) {
        return searchBuilder(profile, form, null, filter, offset, max);
    }

    /**
     * TODO javadoc
     *
     */
    final protected SearchSourceBuilder searchBuilder(
        ProfileDefinition<MediaObject> profile,
        AbstractMediaForm form,
        MediaObject mediaObject,
        BoolQueryBuilder filter,
        long offset,
        Integer max) {
        SearchSourceBuilder searchBuilder = new SearchSourceBuilder();

        // Handle profile filtering
        ESMediaFilterBuilder.filter(profile, filter);
        if (filter != null && filter.hasClauses()) {
            searchBuilder.postFilter(filter);
        }
        QueryBuilder queryBuilder = ESMediaQueryBuilder.query(form != null ? form.getSearches() : null);
        searchBuilder.query(
            ESMediaScoreBuilder.score(queryBuilder, Instant.now())
        );

        if (form instanceof MediaForm) {
            ESMediaFacetsBuilder.facets(searchBuilder, (MediaForm) form);

            ESMediaSortHandler.sort(searchBuilder, (MediaForm) form, mediaObject);
        }

        buildHighlights(searchBuilder, form, ESMediaQueryBuilder.SEARCH_FIELDS);

        handlePaging(offset, max, searchBuilder, queryBuilder, indexName);

        return searchBuilder;
    }

    /**
     *
     * @param request The ElasticSearch {@link SearchRequest} to execute and adapt to a API search result.
     * @param facets The requested facets. Will not be used to change the search request, but only to properly extract the facet results
     */
    protected <S extends MediaObject> GenericMediaSearchResult<S> executeSearchRequest(
        SearchRequest request,
        MediaFacets facets,
        long offset,
        Integer max,
        Class<S> clazz) {
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
                ESMediaFacetsHandler.extractFacets(response, facets, this);
            return new GenericMediaSearchResult<>(adapted, facetsResult, offset, max, hits.getTotalHits());
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

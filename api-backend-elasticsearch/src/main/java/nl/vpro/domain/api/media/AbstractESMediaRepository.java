package nl.vpro.domain.api.media;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.http.concurrent.BasicFuture;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
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
import nl.vpro.media.domain.es.ApiCueIndex;
import nl.vpro.media.domain.es.ApiMediaIndex;
import nl.vpro.media.domain.es.MediaESType;
import nl.vpro.util.ThreadPools;
import nl.vpro.util.TimeUtils;

/**
 * @author Michiel Meeuwissen
 * @since 3.7
 */
@Slf4j
@ToString(callSuper = true)
public abstract class AbstractESMediaRepository extends AbstractESRepository<MediaObject> implements MediaLoader {


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

    private final IndexHelper helper;

    protected boolean createIndices = true;


    protected AbstractESMediaRepository(ESClientFactory client) {
        super(client);
        this.helper = new IndexHelper(log, client, null, ApiMediaIndex::source);
        for (MediaESType type : MediaESType.values()) {
            this.helper.mapping(type.name(), type::mapping);
        }
        this.helper.mapping(ApiCueIndex.TYPE, ApiCueIndex::mapping);

    }


    @PostConstruct
    public Future createIndices() {
        helper.setIndexName(indexName);
        if (isCreateIndices()) {
            return ThreadPools.startUpExecutor.submit(helper::prepareIndex);
        } else {
            return new BasicFuture<>(null);
        }
    }

    public boolean isCreateIndices() {
        return createIndices;
    }

    public void setCreateIndices(boolean createIndices) {
        this.createIndices = createIndices;
    }

    @Override
    protected String[] getLoadTypes() {
        return MediaESType.mediaObjects();
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
        return loadAll(clazz, indexName, ids.toArray(new String[ids.size()]));
    }


    final protected SearchRequest searchRequest(ProfileDefinition<MediaObject> profile, AbstractMediaForm form, QueryBuilder extraFilter, long offset, Integer max) {
        return searchRequest(
            getLoadTypes(),
            profile,
            form,
            null,
            extraFilter,
            offset,
            max);
    }

    protected SearchRequest searchRequest(
        String[] types,
        ProfileDefinition<MediaObject> profile,
        AbstractMediaForm form,
        MediaObject mediaObject,
        QueryBuilder extraFilter,
        long offset,
        Integer max) {
        SearchRequest request = new SearchRequest(indexName);
        request.types(types);
        request.source(searchBuilder(profile, form, mediaObject, extraFilter, offset, max));
        return request;
    }

    final protected SearchSourceBuilder searchBuilder(ProfileDefinition<MediaObject> profile, AbstractMediaForm form, QueryBuilder extraFilter, long offset, Integer max) {
        return searchBuilder(profile, form, null, extraFilter, offset, max);
    }

    protected SearchSourceBuilder searchBuilder(ProfileDefinition<MediaObject> profile, AbstractMediaForm form, MediaObject mediaObject, QueryBuilder extraFilter, long offset, Integer max) {
        SearchSourceBuilder searchBuilder = new SearchSourceBuilder();

        QueryBuilder profileFilter = ESMediaFilterBuilder.filter(profile, extraFilter);

        searchBuilder.postFilter(profileFilter);

        QueryBuilder queryBuilder = ESMediaQueryBuilder.query(form != null ? form.getSearches() : null);

        QueryBuilder scoredBuilder = ESMediaScoreBuilder.score(queryBuilder, Instant.now());

        searchBuilder.query(scoredBuilder);

        if (form instanceof MediaForm) {
            ESMediaSortHandler.sort(searchBuilder, (MediaForm) form, mediaObject);
            ESMediaFacetsBuilder.facets(searchBuilder, (MediaForm) form, profileFilter);
        }

        buildHighlights(searchBuilder, form, ESMediaQueryBuilder.SEARCH_FIELDS);

        handlePaging(offset, max, searchBuilder, queryBuilder, indexName);

        return searchBuilder;
    }

    protected <S extends MediaObject> GenericMediaSearchResult<S> executeQuery(
        SearchRequest request, MediaFacets facets, long offset, Integer max, Class<S> clazz) {
        ActionFuture<SearchResponse> searchResponseFuture = client()
            .search(request)
            ;

        try {
            SearchResponse response = searchResponseFuture

                .actionGet(timeOut.toMillis(), TimeUnit.MILLISECONDS)
                ;

            SearchHits hits = response.getHits();

            List<SearchResultItem<? extends S>> adapted = adapt(hits, clazz);

            MediaFacetsResult facetsResult = ESMediaFacetsHandler.extractFacets(response, facets, this);
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

    private void redirectMemberRefSearch(MemberRefSearch search) {
        if (search == null) {
            return;
        }
        redirectTextMatchers(search.getMediaIds());
    }


    protected void redirectMemberRefFacet(MemberRefFacet facet) {
        if (facet == null) {
            return;
        }
        redirectMediaSearch(facet.getFilter());
        redirectMemberRefSearch(facet.getSubSearch());

    }

}

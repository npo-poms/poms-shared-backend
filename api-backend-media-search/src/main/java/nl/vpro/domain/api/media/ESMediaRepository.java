/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.media;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.sql.Date;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.mlt.MoreLikeThisRequestBuilder;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import nl.vpro.api.Settings;
import nl.vpro.domain.api.*;
import nl.vpro.domain.api.profile.ProfileDefinition;
import nl.vpro.domain.media.AgeRating;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.media.Program;
import nl.vpro.domain.media.support.Workflow;
import nl.vpro.elasticsearch.ESClientFactory;
import nl.vpro.elasticsearch.ElasticSearchIterator;
import nl.vpro.media.domain.es.MediaESType;
import nl.vpro.util.FilteringIterator;
import nl.vpro.util.MaxOffsetIterator;
import nl.vpro.util.TailAdder;

/**
 * @author Roelof Jan Koekoek
 * @author Michiel Meeuwissen
 * @since 2.0
 */
@Slf4j
public class ESMediaRepository extends AbstractESMediaRepository implements MediaSearchRepository {

    private final String[] relatedFields;

    @Inject
    Settings settings;

    @Autowired
    @Qualifier("couchDBMediaRepository")
    MediaRepository mediaRepository;

    int iterateBatchSize = 1000;

    private ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    private Map<String, String> redirects = new HashMap<>();

    private Instant lastRedirectRead = Instant.now();

    private Instant lastRedirectChange = Instant.now();


    public ESMediaRepository(ESClientFactory client, String relatedFields) {
        super(client);
        this.relatedFields = relatedFields.split(",");
        EXECUTOR.scheduleAtFixedRate(this::refillRedirectCache, 5, 5, TimeUnit.MINUTES);
    }

    @Override
    public MediaObject load(String mid) {
        mid = redirect(mid).orElse(mid);
        return load(mid, MediaObject.class);
    }

    @Override
    protected String[] getRelevantTypes() {
        return MediaESType.toString(MediaESType.values());
    }

    @Override
    protected MediaRepository getDirectsRepository() {
        return RepositoryType.switchRepository(settings.redirectsRepository, mediaRepository, this);

    }

    @Override
    public List<MediaObject> loadAll(List<String> ids) {
        return loadAll(MediaObject.class, ids);
    }


    protected <S extends MediaObject> List<S> loadAll(Class<S> clazz, List<String> ids) {
        ids = ids.stream().map(id -> redirect(id).orElse(id)).collect(Collectors.toList());
        return loadAll(clazz, indexName, ids.toArray(new String[ids.size()]));
    }


    @Override
    public MediaSearchResult find(ProfileDefinition<MediaObject> profile, MediaForm form, long offset, Integer max) {
        form = redirectForm(form);
        SearchRequest request = searchRequest(profile, form, null, offset, max);
        GenericMediaSearchResult<MediaObject> result = executeQuery(request, form != null ? form.getFacets() : null, offset, max, MediaObject.class);
        if (form != null && form.getFacets() != null) {
            result.setSelectedFacets(new MediaFacetsResult());
        }
        MediaSearchResults.setSelectedFacets(result.getFacets(), result.getSelectedFacets(), form);
        MediaSearchResults.sortFacets(result.getFacets(), result.getSelectedFacets(), form);

        return new MediaSearchResult(result);
    }

    @Override
    public MediaSearchResult findMembers(MediaObject media, ProfileDefinition<MediaObject> profile, MediaForm form, long offset, Integer max) {
        return findAssociated("memberOf", media, profile, form, offset, max);
    }

    @Override
    public ProgramSearchResult findEpisodes(MediaObject media, ProfileDefinition<MediaObject> profile, MediaForm form, long offset, Integer max) {
        form = redirectForm(form);
        GenericMediaSearchResult<Program> result = findAssociatedMedia("episodeOf", media, profile, form, offset, max, Program.class);
        if (form != null && form.getFacets() != null) {
            result.setSelectedFacets(new MediaFacetsResult());
        }
        MediaSearchResults.setSelectedFacets(result.getFacets(), result.getSelectedFacets(), form);
        MediaSearchResults.sortFacets(result.getFacets(), result.getSelectedFacets(), form);

        return new ProgramSearchResult(result);
    }


    @Override
    public MediaSearchResult findDescendants(MediaObject media, ProfileDefinition<MediaObject> profile, MediaForm form, long offset, Integer max) {
        return findAssociated("descendantOf", media, profile, form, offset, max);
    }

    private MediaSearchResult findAssociated(String type, MediaObject media, ProfileDefinition<MediaObject> profile, MediaForm form, long offset, Integer max) {
        form = redirectForm(form);
        GenericMediaSearchResult<MediaObject> result = findAssociatedMedia(type, media, profile, form, offset, max, MediaObject.class);
        if (form != null && form.getFacets() != null) {
            result.setSelectedFacets(new MediaFacetsResult());
        }
        MediaSearchResults.setSelectedFacets(result.getFacets(), result.getSelectedFacets(), form);
        MediaSearchResults.sortFacets(result.getFacets(), result.getSelectedFacets(), form);

        return new MediaSearchResult(result);
    }

    @Override
    public MediaSearchResult findRelated(MediaObject media, ProfileDefinition<MediaObject> profile, MediaForm form, Integer max) {
        form = redirectForm(form);
        AgeRating ageRating = media.getAgeRating();
        BoolFilterBuilder ageRatingFilter = null;
        if (ageRating != null) {
            String ratingString = ageRating == AgeRating.ALL ? ageRating.name() : ageRating.name().substring(1);
            ageRatingFilter = FilterBuilders.boolFilter().must(FilterBuilders.termFilter("ageRating", ratingString));
        }

        SearchSourceBuilder search = searchBuilder(profile, form, ageRatingFilter, 0L, 0x7ffffef);

        MoreLikeThisRequestBuilder moreLikeThis = new MoreLikeThisRequestBuilder(
            client(),
                indexName,
            media.getClass().getSimpleName().toLowerCase(),
            media.getMid());

        moreLikeThis
            .setSearchSource(search) // I doubt this source is honored...
            .setSearchSize(max)
            .maxQueryTerms(10)
            .setField(filterFields(media, relatedFields, "titles.value"))
            .setPercentTermsToMatch(0.1f)
            .setMinWordLen(4) // longer terms are more unique and randomize the outcome at a cost
            .setMinTermFreq(1)  // on limited text high term frequency gives strange results f.e. everything with "actueel"
            .setMinDocFreq(10)
        ;

        ListenableActionFuture<SearchResponse> future;
        try {
            future = moreLikeThis.execute();
        } catch (Exception e) {
            LOG_ERRORS.warn("findRelated max {}, {}, {}", max, profile, search, moreLikeThis);
            throw e;
        }

        SearchResponse response = future.actionGet(timeOut.toMillis(), TimeUnit.MILLISECONDS);

        SearchHits hits = response.getHits();

        List<SearchResultItem<? extends MediaObject>> adapted = adapt(hits, MediaObject.class);
        //MediaSearchResults.setSelectedFacets(hits.getFa, form); // TODO
        return new MediaSearchResult(adapted, 0L, max, hits.getTotalHits());
    }

    protected MediaObject getMediaObject(SearchHit hit) {
        try {
            return getObject(hit, MediaObject.class);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public MediaResult list(Order order, long offset, Integer max) {
        SearchRequest request = client()
            .prepareSearch(indexName)
            .setTypes(MediaESType.mediaObjects())
            .addSort("mid", SortOrder.valueOf(order.name()))
            .setFrom((int) offset)
            .setSize(max)
            .request();

        GenericMediaSearchResult<MediaObject> result = executeQuery(request, null, offset, max, MediaObject.class);
        return new MediaSearchResult(result).asResult();
    }

    @Override
    public MediaResult listMembers(MediaObject media, Order order, long offset, Integer max) {
        SearchResponse response = client()
            .prepareSearch(indexName)
            .setTypes(MediaESType.memberRefs())
            .setQuery(QueryBuilders.termQuery("_parent", media.getMid()))
            .addSort("index", SortOrder.valueOf(order.name()))
            .setFrom((int) offset)
            .setSize(max).get();


        SearchHit[] hits = response.getHits().getHits();
        List<MediaObject> objects = loadAll(Arrays.stream(hits).map(sh -> String.valueOf(sh.getSource().get("childRef"))).collect(Collectors.toList()));

        return new MediaResult(objects, offset, max, response.getHits().getTotalHits());

    }

    @Override
    public MediaResult listDescendants(MediaObject media, Order order, long offset, Integer max) {
        SearchRequest request = client()
            .prepareSearch(indexName)
            .setTypes(MediaESType.mediaObjects())
            .addSort("sortDate", SortOrder.valueOf(order.name()))
            .setQuery(QueryBuilders.termQuery("descendantOf.midRef", media.getMid()))
            .setFrom((int) offset)
            .setSize(max)
            .request();

        GenericMediaSearchResult<MediaObject> objects = executeQuery(request, null, offset, max, MediaObject.class);

        return new MediaSearchResult(objects).asResult();

    }


    @Override
    public ProgramResult listEpisodes(MediaObject media, Order order, long offset, Integer max) {
        SearchResponse response = client()
            .prepareSearch(indexName)
            .setTypes(MediaESType.episodeRef.name())
            .setQuery(QueryBuilders.termQuery("_parent", media.getMid()))
            .addSort("index", SortOrder.valueOf(order.name()))
            .setFrom((int) offset)
            .setSize(max).get();


        SearchHit[] hits = response.getHits().getHits();
        List<Program> objects = loadAll(Program.class, Arrays.stream(hits).map(sh -> String.valueOf(sh.getSource().get("childRef"))).collect(Collectors.toList()));

        return new ProgramResult(objects, offset, max, response.getHits().getTotalHits());

    }

    @Override
    public Iterator<Change> changes(Instant since, ProfileDefinition<MediaObject> currentProfile, ProfileDefinition<MediaObject> previousProfile, Order order, Integer max, Long keepAlive) {
        if (currentProfile == null && previousProfile != null) {
            throw new IllegalStateException("Missing current profile");
        }
        ElasticSearchIterator<Change> i = new ElasticSearchIterator<>(client(), this::of);
        final SearchRequestBuilder searchRequestBuilder = i.prepareSearch(indexName).addSort("publishDate", SortOrder.valueOf(order.name()));

        QueryBuilder restriction = QueryBuilders.matchAllQuery();
        if (!hasProfileUpdate(currentProfile, previousProfile) && since != null) {
            restriction = QueryBuilders.rangeQuery("publishDate").from(Date.from(since));
        }
        searchRequestBuilder.setQuery(QueryBuilders.filteredQuery(restriction, FilterBuilders.existsFilter("publishDate")));

        log.debug("Found {} changes", i.getTotalSize());
        ChangeIterator changes = new ChangeIterator(
            i,
            since,
            currentProfile,
            previousProfile,
            keepAlive == null ? Long.MAX_VALUE : keepAlive
        );

        Iterator<Change> iterator = TailAdder.withFunctions(changes, (last) -> {
            if (last != null) {
                throw new NoSuchElementException();
            }
            Instant se = changes.getPublishDate();
            if (se != null) {
                return Change.tail(se);
            } else {
                return Change.tail(Instant.now());
            }
        });

        return new MaxOffsetIterator<>(iterator, max, 0L, true);
    }

    private Change of(SearchHit hit) {
        try {
            MediaObject media = getObject(hit, MediaObject.class);
            if (media == null) {
                log.warn("No media found in {}", hit);
                return null;
            }
            Long version = hit.getVersion();
            if (version == -1) {
                version = null;
            }
            return Change.of(media, version);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public Iterator<Change> changes(Long since, ProfileDefinition<MediaObject> current, ProfileDefinition<MediaObject> previous, Order order, Integer max, Long keepAlive) {
        throw new UnsupportedOperationException("Not supported");
    }


    @Override
    public Iterator<MediaObject> iterate(ProfileDefinition<MediaObject> profile, MediaForm form, long offset, Integer max, FilteringIterator.KeepAlive keepAlive) {
        ElasticSearchIterator<MediaObject> i = new ElasticSearchIterator<>(client(), this::getMediaObject);

        SearchRequestBuilder builder = i.prepareSearch(indexName);
        ESMediaSortHandler.sort(form, null, builder::addSort);
        builder.setTypes(MediaESType.mediaObjects())
            .setSize(iterateBatchSize)
            .setQuery(ESMediaQueryBuilder.query(form != null ? form.getSearches() : null))
            .setPostFilter(ESMediaFilterBuilder.filter(profile, null))
        ;

        Predicate<MediaObject> filter = Objects::nonNull;
        return new MaxOffsetIterator<>(new FilteringIterator<>(i, filter, keepAlive), max, offset, true);
    }

    @Override
    public RedirectList redirects() {
        return new RedirectList(lastRedirectRead, lastRedirectChange, redirects);


    }

    synchronized void refillRedirectCache() {
        ElasticSearchIterator<MediaObject> i = new ElasticSearchIterator<>(client(), this::getMediaObject);
        i.prepareSearch(indexName)
            .setTypes(MediaESType.deletedMediaObjects())
            .setQuery(QueryBuilders.termQuery("workflow", Workflow.MERGED.name()))
            .setSize(iterateBatchSize)
        ;
        while(i.hasNext()) {
            MediaObject o = i.next();
            redirects.put(o.getMid(), o.getMergedToRef());
        }
    }


    /**
     * Changes the form to 'redirect' all occurances of mids in it.
     */
    MediaForm redirectForm(MediaForm form) {
        if (form == null) {
            return null;
        }
        redirectMediaSearch(form.getSearches());
        if (form.getFacets() != null) {
            redirectMemberRefFacet(form.getFacets().getMemberOf());
        }
        return form;
    }

    private void redirectMediaSearch(MediaSearch search) {
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



    private void redirectMemberRefFacet(MemberRefFacet facet) {
        if (facet == null) {
            return;
        }
        redirectMediaSearch(facet.getFilter());
        redirectMemberRefSearch(facet.getSubSearch());

    }

    private <S extends MediaObject> GenericMediaSearchResult<S> findAssociatedMedia(String axis, MediaObject media, ProfileDefinition<MediaObject> profile, MediaForm form, long offset, Integer max, Class<S> clazz) {
        String ref = media.getMid();
        BoolFilterBuilder booleanFilter = FilterBuilders.boolFilter().must(FilterBuilders.termFilter(axis + ".midRef", ref));

        SearchRequest request = searchRequest(getLoadTypes(), profile, form, media, booleanFilter, offset, max);

        return executeQuery(request, form != null ? form.getFacets() : null, offset, max, clazz);
    }

    private boolean hasProfileUpdate(ProfileDefinition<MediaObject> current, ProfileDefinition<MediaObject> previous) {
        return !(current == null ? previous == null : current.equals(previous));
    }

}

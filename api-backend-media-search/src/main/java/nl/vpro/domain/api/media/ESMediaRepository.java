/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.media;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.TotalHits;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.search.*;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

import nl.vpro.domain.api.*;
import nl.vpro.domain.api.profile.ProfileDefinition;
import nl.vpro.domain.media.*;
import nl.vpro.domain.media.support.Workflow;
import nl.vpro.elasticsearch7.ESClientFactory;
import nl.vpro.elasticsearch7.ElasticSearchIterator;
import nl.vpro.util.*;

import static nl.vpro.domain.media.StandaloneMemberRef.ObjectType.episodeRef;
import static nl.vpro.domain.media.StandaloneMemberRef.ObjectType.memberRef;

/**
 * @author Roelof Jan Koekoek
 * @author Michiel Meeuwissen
 * @since 2.0
 */
@Slf4j
@ManagedResource(objectName = "nl.vpro.api:name=esMediaRepository")
public class ESMediaRepository extends AbstractESMediaRepository implements MediaSearchRepository {

    private final String[] relatedFields;

    int iterateBatchSize = 1000;

    private final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    RedirectList redirects = null;
    Instant lastRedirectRead = Instant.EPOCH;

    private int defaultMax = 1000;

    private final MediaScoreManager scoreManager;

    public ESMediaRepository(ESClientFactory client, String relatedFields, MediaScoreManager scoreManager) {
        super(client);
        this.relatedFields = StringUtils.isBlank(relatedFields) ? new String[0] : relatedFields.split(",");
        this.scoreManager = scoreManager;
    }

    protected void fillRedirects() {
        if (redirects == null) {
            refillRedirectCache();
            EXECUTOR.scheduleAtFixedRate(this::refillRedirectCache, 5, 5, TimeUnit.MINUTES);
        }
    }

    @Override
    @ManagedAttribute
    public MediaObject load(@NonNull String mid) {
        mid = redirect(mid).orElse(mid);
        return load(mid, MediaObject.class);
    }

    @Override
    public List<MediaObject> loadAll(@NonNull List<String> ids) {
        return loadAll(MediaObject.class, ids).stream().map(o -> o.orElse(null)).collect(Collectors.toList());
    }


    @Override
    protected <S extends MediaObject> List<Optional<S>> loadAll(@NonNull Class<S> clazz,
                                                      @NonNull List<String> ids) {
        ids = ids.stream().map(id -> redirect(id).orElse(id)).collect(Collectors.toList());
        return loadAll(clazz, getIndexName(), ids.toArray(new String[0]));
    }

    @Override
    public boolean isScore() {
        return scoreManager.getIsScoring();
    }


    public void setScore(boolean score) {
        scoreManager.setIsScoring(score);
    }

    @Override
    public MediaSearchResult find(
        @Nullable final ProfileDefinition<MediaObject> profile,
        @Nullable MediaForm form,
        long offset,
        @Nullable Integer max) {

        form = redirectForm(form);

        BoolQueryBuilder rootQuery = QueryBuilders.boolQuery();

        SearchRequestWrapper request = mediaSearchRequest(
            profile,
            form,
            rootQuery,
            offset,
            max
        );

        GenericMediaSearchResult<MediaObject> result = executeSearchRequest(
            request,
            form != null ? form.getFacets() : null,
            offset,
            max,
            MediaObject.class
        );
        if (form != null && form.getFacets() != null) {
            result.setSelectedFacets(new MediaFacetsResult());
        }
        MediaSearchResults.setSelectedFacets(result.getFacets(), result.getSelectedFacets(), form);
        MediaSearchResults.sortFacets(result.getFacets(), result.getSelectedFacets(), form);

        boolean needsPostFilter = false;
        if (form != null && form.hasSearches() && form.getSearches().getScheduleEvents() != null) {
            for (ScheduleEventSearch ses : form.getSearches().getScheduleEvents()) {
                if (ses.countSearches() > 1) {
                    needsPostFilter = true;
                    break;
                }
            }
        }

        if (needsPostFilter) {
            List<SearchResultItem<? extends MediaObject>> filtered = new ArrayList<>();
            int correctTotal = 0;
            for (SearchResultItem<? extends MediaObject> item : result) {
                if (form.test(item.getResult())) {
                    filtered.add(item);
                } else {
                    correctTotal++;

                }
            }
            MediaSearchResult filteredResult =  new MediaSearchResult(
                filtered,
                result.getOffset(),
                result.getMax(),
                Result.Total.approximate(result.getTotal() - correctTotal)
            );
            filteredResult.setSelectedFacets(result.getSelectedFacets());
            filteredResult.setFacets(result.getFacets());
            return filteredResult;
        } else {
            return new MediaSearchResult(result);
        }
    }

    @Override
    public MediaSearchResult findMembers(
        @NonNull MediaObject media,
        @Nullable ProfileDefinition<MediaObject> profile,
        @Nullable MediaForm form,
        long offset,
        @Nullable Integer max) {
        return findAssociated("memberOf", media, profile, form, offset, max);
    }

    @Override
    public ProgramSearchResult findEpisodes(
        @NonNull MediaObject media,
        @Nullable ProfileDefinition<MediaObject> profile,
        @Nullable MediaForm form,
        long offset,
        @Nullable Integer max) {
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
    public MediaSearchResult findDescendants(
        @NonNull MediaObject media,
        @Nullable ProfileDefinition<MediaObject> profile,
        @Nullable MediaForm form,
        long offset,
        @Nullable Integer max) {
        return findAssociated("descendantOf", media, profile, form, offset, max);
    }

    private MediaSearchResult findAssociated(
        @NonNull String type,
        @NonNull MediaObject media,
        @Nullable ProfileDefinition<MediaObject> profile,
        @Nullable MediaForm form,
        long offset,
        @Nullable Integer max) {
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
    public MediaSearchResult findRelated(
        @NonNull MediaObject media,
        @Nullable ProfileDefinition<MediaObject> profile,
        @Nullable MediaForm form,
        @Nullable Integer max) {
        form = redirectForm(form);
        AgeRating ageRating = media.getAgeRating();
        BoolQueryBuilder filter = QueryBuilders.boolQuery();
        if (ageRating != null) {
            String ratingString = ageRating == AgeRating.ALL ? ageRating.name() : ageRating.name().substring(1);
            filter.must(QueryBuilders.termQuery("ageRating", ratingString));
        }

        SearchSourceBuilder search = mediaSearchBuilder(profile, form, filter, 0L, 0x7ffffef);
        assert media.getMid() != null;
        MoreLikeThisQueryBuilder.Item item = new MoreLikeThisQueryBuilder.Item(getIndexName(), media.getMid());
        MoreLikeThisQueryBuilder moreLikeThisQueryBuilder = QueryBuilders.moreLikeThisQuery(
            filterFields(media, relatedFields, "objectType,titles.value"),
            new String[] {},
            new MoreLikeThisQueryBuilder.Item[] {item});

        moreLikeThisQueryBuilder
            .maxQueryTerms(10)
            //.percentTermsToMatch(0.1f)
            .minWordLength(4) // longer terms are more unique and randomize the outcome at a cost
            .minTermFreq(1)  // on limited text high term frequency gives strange results f.e. everything with "actueel"
            .minDocFreq(10)
        ;


        ActionFuture<SearchResponse> future;
        try {
            SearchRequest request = client()
                .prepareSearch(getIndexName())
                .setSize(max == null ? defaultMax : max)
                .setQuery(moreLikeThisQueryBuilder)
                .request();
            future = client().search(request);
        } catch (Exception e) {
            LOG_ERRORS.warn("findRelated max {}, {}, {}, {}", max, profile, search, moreLikeThisQueryBuilder);
            throw e;
        }
        SearchResponse response = future.actionGet(timeOut.toMillis(), TimeUnit.MILLISECONDS);

        SearchHits hits = response.getHits();

        List<SearchResultItem<? extends MediaObject>> adapted = adapt(hits, MediaObject.class);
        //MediaSearchResults.setSelectedFacets(hits.getFa, form); // TODO
        return new MediaSearchResult(adapted, 0L, max, getTotal(hits));

    }

    protected MediaObject getMediaObject(
        @NonNull SearchHit hit) {
        try {
            return getObject(hit, MediaObject.class);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public MediaResult list(
        @NonNull Order order,
        long offset,
        @Nullable  Integer max) {

        if (max == null) {
            max = defaultMax;
        }
        AtomicInteger atomicMax = new AtomicInteger(max);
        boolean wasZero = handleMaxZero(max, atomicMax::set);
        SearchRequest request = client()
            .prepareSearch(getIndexName())
            .setQuery(QueryBuilders.termQuery("workflow", Workflow.PUBLISHED.name()))
            .addSort("mid", SortOrder.valueOf(order.name()))
            .setFrom((int) offset)
            .setSize(atomicMax.get())
            .request();

        SearchRequestWrapper wrapper = new SearchRequestWrapper(request, wasZero);
        GenericMediaSearchResult<MediaObject> result = executeSearchRequest(wrapper, null, offset, max, MediaObject.class);
        return new MediaSearchResult(result).asResult();
    }

    @Override
    public MediaResult listMembers(
        @NonNull MediaObject media,
        @Nullable ProfileDefinition<MediaObject> profile,
        @NonNull Order order,
        long offset,
        @NonNull Integer max) {

        MemberRefResult queryResult = listMembersOrEpisodes(memberRef, media, profile, order, offset, max);
        List<MediaObject> objects = loadAll(MediaObject.class, queryResult.mids).stream()
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
        TotalHits totalHits = queryResult.totalHits;

        return new MediaResult(objects, offset, max, getTotal(totalHits, profile, objects, offset, max));

    }

    @Override
    public MediaResult listDescendants(
        @NonNull MediaObject media,
        @Nullable ProfileDefinition<MediaObject> profile,
        @NonNull Order order,
        long offset,
        Integer max) {
        assert media.getMid() != null;
        AtomicInteger atomicMax = new AtomicInteger(max);
        boolean wasZero = handleMaxZero(max, atomicMax::set);
        SearchRequest request = client()
            .prepareSearch(getIndexName())
            .addSort(MediaSortField.sortDate.name(), SortOrder.valueOf(order.name()))
            .setQuery(QueryBuilders.termQuery("descendantOf.midRef", media.getMid()))
            .setFrom((int) offset)
            .setSize(atomicMax.get())
            .setPostFilter(ESMediaFilterBuilder.filter(profile))
            .request();

        SearchRequestWrapper wrapper = new SearchRequestWrapper(request, wasZero);
        GenericMediaSearchResult<MediaObject> objects =
            executeSearchRequest(wrapper, null, offset, max, MediaObject.class);

        return new MediaSearchResult(objects).asResult();

    }


    @Override
    public ProgramResult listEpisodes(
        @NonNull MediaObject media,
        @Nullable ProfileDefinition<MediaObject> profile,
        @NonNull Order order,
        long offset,
        @Nullable Integer max) {

        MemberRefResult queryResult = listMembersOrEpisodes(episodeRef, media, profile, order, offset, max);
        List<Program> objects = loadAll(Program.class, queryResult.mids).stream().filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
        TotalHits totalHits = queryResult.totalHits;


        return new ProgramResult(objects, offset, max, getTotal(totalHits, profile, objects, offset, max));
    }

    <T extends MediaObject> Result.Total getTotal(TotalHits totalHits, ProfileDefinition<MediaObject> profile, List<T> objects, long offset, Integer max) {
        final Long total;
        final Result.TotalQualifier totalQualifier;

        if (profile != null) {
            total = filterWithProfile(objects, profile, offset, max);
            totalQualifier = Result.TotalQualifier.APPROXIMATE;
        } else {
            if (totalHits == null){
                total = null;
                totalQualifier = Result.TotalQualifier.MISSING;
            }  else {
                total = totalHits.value;
                totalQualifier = Result.TotalQualifier.valueOf(totalHits.relation.name());
            }
        }
        return new Result.Total(total, totalQualifier);

    }

    private MemberRefResult listMembersOrEpisodes(
        StandaloneMemberRef.@NonNull ObjectType objectType,
        @NonNull MediaObject media,
        @Nullable ProfileDefinition<MediaObject> profile,
        @NonNull Order order,
        long offset,
        @Nullable Integer max)  {
        List<String> mids;
        TotalHits total = null;
        if (profile == null) {
            SearchRequestBuilder builder = client().prepareSearch(getRefsIndexName());
            listMembersOrEpisodesBuildRequest(builder, objectType, media, order);
            builder.setFrom((int) offset);
            boolean maxIsZero = handleMaxZero(max, builder::setSize);
            SearchResponse response = builder.get();
            SearchHit[] hits = response.getHits().getHits();
            mids = maxIsZero ? Collections.emptyList() : Arrays.stream(hits)
                .map(sh -> String.valueOf(sh.getSourceAsMap().get("childRef")))
                .collect(Collectors.toList());
            total = response.getHits().getTotalHits();
        } else {
            mids = new ArrayList<>();
            try (ElasticSearchIterator<String> iterator = new ElasticSearchIterator<>(client(), (sh) -> (String) sh.getSourceAsMap().get("childRef"))) {
                SearchRequestBuilder builder = iterator.prepareSearch(getRefsIndexName());
                listMembersOrEpisodesBuildRequest(builder, objectType, media, order);
                iterator.forEachRemaining(mids::add);

                total = iterator.getTotalSize().map(s -> new TotalHits(s, TotalHits.Relation.EQUAL_TO)).orElse(null);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        return new MemberRefResult(total, mids);
    }



    protected static class MemberRefResult {
        final TotalHits totalHits;
        final List<String> mids;

        public MemberRefResult(TotalHits totalHits, List<String> mids) {
            this.totalHits = totalHits;
            this.mids = mids;
        }
    }

    protected void listMembersOrEpisodesBuildRequest(SearchRequestBuilder builder, StandaloneMemberRef.ObjectType objectType, MediaObject media, Order order) {
        BoolQueryBuilder must = QueryBuilders.boolQuery();
        must.filter(QueryBuilders.termQuery("objectType", objectType.name()));
        assert media.getMid() != null;
        must.must(QueryBuilders.termQuery("midRef", media.getMid()));
        builder
            .setQuery(must)
            .addSort("index", SortOrder.valueOf(order.name()))
            .addSort("added", SortOrder.ASC)
            .addSort("childRef", SortOrder.ASC)
            .setRouting(media.getMid())
        ;

    }

    private <T extends MediaObject> Long filterWithProfile(
        @NonNull List<T> objects,
        @Nullable ProfileDefinition<MediaObject> profile,
        long offset,
        @Nullable Integer max) {
        if (profile != null) {
            objects.removeIf((p) -> !profile.test(p));
            long result = objects.size();
            if (offset > 0 || max != null) {
                while (offset-- > 0 && objects.size() > 0) {
                    objects.remove(0);
                }
                if (max != null) {
                    while (objects.size() > max) {
                        objects.remove(objects.size() - 1);
                    }
                }
            }
            return result;
        }
        return null;
    }

    @Override
    public CloseableIterator<MediaChange> changes(
        Instant since,
        String mid,
        ProfileDefinition<MediaObject> currentProfile,
        ProfileDefinition<MediaObject> previousProfile,
        Order order,
        Integer max,
        Long keepAlive,
        Deletes deletes,
        Tail tail) {
        if (currentProfile == null && previousProfile != null) {
            throw new IllegalStateException("Missing current profile");
        }


        // NPA-429 since elastic search takes time to show indexed objects in queries we limit our query from since to now - commitdelay.
        final Instant changesUpto = Instant.now().minus(getCommitDelay());
        RangeQueryBuilder restriction = QueryBuilders.rangeQuery("publishDate").to(changesUpto.toEpochMilli());
        if (!hasProfileUpdate(currentProfile, previousProfile) && since != null) {
            if (since.isBefore(changesUpto)) {
                long epoch = since.toEpochMilli();
                restriction = restriction.from(epoch).includeLower(true);
            } else {
                log.debug("Since is after commited changes window (before {}). Returing empty iterator.", changesUpto);
                // This will result exactly nothing, so we return empty iterator immediately:
                return TailAdder.withFunctions(CloseableIterator.empty(), (last) -> MediaChange.tail(changesUpto));
            }
        }
        final ElasticSearchIterator<MediaChange> i = new ElasticSearchIterator<>(client(), this::of);
        final SearchRequestBuilder searchRequestBuilder =
            i.prepareSearch(getIndexName())
                .addSort("publishDate", SortOrder.valueOf(order.name()))
                .addSort("mid", SortOrder.ASC);

        searchRequestBuilder.setQuery(QueryBuilders.boolQuery()
            .must(restriction)
            .filter(QueryBuilders.existsQuery("publishDate"))
        );

        log.debug("Found {} changes", i.getTotalSize());
        ChangeIterator changes = new ChangeIterator(
            i,
            since,
            currentProfile,
            previousProfile,
            keepAlive == null ? Long.MAX_VALUE : keepAlive
        );

        Iterator<MediaChange> iterator = changes;

        if (since != null && mid != null) {
            PeekingIterator<MediaChange> peeking = Iterators.peekingIterator(changes);
            iterator = peeking;
            while(peeking.hasNext()) {
                MediaChange peek = peeking.peek();

                if (peek != null) {
                    if (peek.isTail() || peek.getPublishDate() == null || peek.getMid() == null || peek.getPublishDate().isAfter(since) || peek.getMid().compareTo(mid) > 0) {
                        log.debug("Peek is ok");
                        break;
                    } else {
                        MediaChange skipped = peeking.next();
                        log.debug("Skipping {} because of mid parameter", skipped);
                    }
                }
            }
        }
        if (deletes == null) {
            deletes = Deletes.ID_ONLY;
        }

        switch (deletes) {
            case INCLUDE:
                break;
            case EXCLUDE:
                iterator= FilteringIterator.<MediaChange>builder()
                    .wrapped(iterator)
                    .filter((c) -> c == null || !c.isDeleted())
                    .build();
                break;
            case ID_ONLY:
                iterator = new BasicWrappedIterator<MediaChange>(iterator) {
                    @Override
                    public MediaChange next() {
                        MediaChange n = super.next();
                        if (n.isDeleted()) {
                            n.setMedia(null);
                        }
                        return n;
                    }
                };
                break;
        }
        Tail actualTails = tail == null ? Tail.IF_EMPTY : tail;

        CloseableIterator<MediaChange> maxed =   new MaxOffsetIterator<>(iterator, max, 0L, true);
        CloseableIterator<MediaChange> tailed =  TailAdder.withFunctions(maxed, (last) -> {
            if (actualTails == Tail.NEVER) {
                throw new NoSuchElementException();
            }
            if (last == null) {
                return MediaChange.tail(changesUpto);
            } else if (tail == Tail.ALWAYS) {
                return MediaChange.tail(last.asSince());
            } else {
                throw new NoSuchElementException();
            }
        });
        return tailed;
    }

    private MediaChange of(
        @NonNull SearchHit hit) {
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
            return MediaChange.of(media, version);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    @Override
    @Deprecated
    public CloseableIterator<MediaChange> changes(Long since, ProfileDefinition<MediaObject> current, ProfileDefinition<MediaObject> previous, Order order, Integer max, Long keepAlive) {
        throw new UnsupportedOperationException("Not supported");
    }


    @Override
    public CloseableIterator<MediaObject> iterate(ProfileDefinition<MediaObject> profile, MediaForm form, long offset, Integer max, FilteringIterator.KeepAlive keepAlive) {
        ElasticSearchIterator<MediaObject> i = new ElasticSearchIterator<>(client(), this::getMediaObject);

        SearchRequestBuilder builder = i.prepareSearch(getIndexName());
        ESMediaSortHandler.sort(form, null, builder::addSort);
        builder
            .setSize(iterateBatchSize)
            .setQuery(ESMediaQueryBuilder.query("", form != null ? form.getSearches() : null))
            .setPostFilter(ESMediaFilterBuilder.filter(profile))
        ;

        Predicate<MediaObject> filter = Objects::nonNull;
        return new MaxOffsetIterator<>(new FilteringIterator<>(i, filter, keepAlive), max, offset, true);
    }

    @Override
    public RedirectList redirects() {
        fillRedirects();
        return redirects;
    }

    synchronized void refillRedirectCache() {
        Map<String, String> newRedirects = new HashMap<>();

        try(ElasticSearchIterator<MediaObject> i = new ElasticSearchIterator<>(client(), this::getMediaObject)) {
            i.prepareSearch(getIndexName())
                .setQuery(QueryBuilders.termQuery("workflow", Workflow.MERGED.name()))
                .setSize(iterateBatchSize)
            ;
            while(i.hasNext()) {
                MediaObject o = i.next();
                if (o.getMergedToRef() != null) {
                    newRedirects.put(o.getMid(), o.getMergedToRef());
                } else {
                    log.warn("Found merged object without merged to {}", o.getMid());
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        if (redirects == null || ! newRedirects.equals(redirects.getMap())) {
            redirects = new RedirectList(Instant.now(), newRedirects);
            log.info("Read {} redirects from ES", redirects.size());
        }
        lastRedirectRead = Instant.now();
    }


    /**
     * Changes the form to 'redirect' all occurances of mids in it.
     */
    MediaForm redirectForm(MediaForm form) {
        if (form == null) {
            return null;
        }
        super.redirectForm(form);
        if (form.getFacets() != null) {
            redirectMemberRefFacet(form.getFacets().getMemberOf());
        }
        return form;
    }

    private <S extends MediaObject> GenericMediaSearchResult<S> findAssociatedMedia(
        @NonNull String axis,
        @NonNull MediaObject media,
        @Nullable ProfileDefinition<MediaObject> profile,
        @Nullable MediaForm form,
        long offset,
        @Nullable Integer max,
        @NonNull Class<S> clazz) {
        String ref = media.getMid();
        assert ref != null;
        BoolQueryBuilder booleanFilter =
            QueryBuilders.boolQuery().must(QueryBuilders.termQuery(axis + ".midRef", ref));


        SearchRequestWrapper request = mediaSearchRequest(profile, form, media, booleanFilter, offset, max);

        return executeSearchRequest(request, form != null ? form.getFacets() : null, offset, max, clazz);
    }

    private boolean hasProfileUpdate(
        @Nullable ProfileDefinition<MediaObject> current,
        @Nullable ProfileDefinition<MediaObject> previous) {
        return !(current == null ? previous == null : current.equals(previous));
    }

}

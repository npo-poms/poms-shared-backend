package nl.vpro.domain.api.schedule;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.search.*;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Value;

import nl.vpro.domain.api.*;
import nl.vpro.domain.api.media.*;
import nl.vpro.domain.api.profile.ProfileDefinition;
import nl.vpro.domain.media.*;
import nl.vpro.domain.media.search.InstantRange;
import nl.vpro.domain.media.search.SchedulePager;
import nl.vpro.domain.media.support.Workflow;
import nl.vpro.elasticsearch7.ESClientFactory;
import nl.vpro.elasticsearch7.ElasticSearchIterator;
import nl.vpro.jackson2.Jackson2Mapper;
import nl.vpro.media.domain.es.ApiMediaIndex;
import nl.vpro.util.TimeUtils;

import static nl.vpro.domain.api.ESQueryBuilder.simplifyQuery;

/**
 * @since 4.8
 */
@Slf4j
@ToString(callSuper = true)
public class ESScheduleRepository extends AbstractESMediaRepository implements ScheduleRepository, MediaLoader {

    private static final int MAXRESULT = 100;

    private final Redirector mediaRedirector;

    private final MediaScoreManager scoreManager;


    @Override
    @Value("${elasticSearch.schedule.index}")
    public void setIndexName(@NonNull String indexName) {
        super.setIndexName(indexName);
    }


    @Override
    @Value("${elasticSearch.schedule.facetLimit}")
    public void setFacetLimit(Integer facetLimit) {
        super.setFacetLimit(facetLimit);
    }


    @Override
    @Value("${elasticSearch.schedule.timeout}")
    public void setTimeout(String timeout) {
        super.setTimeOut(TimeUtils.parseDuration(timeout).orElse(Duration.ofSeconds(15)));
    }

    @Inject
    public ESScheduleRepository(
        ESClientFactory client,
        Redirector mediaRedirector,
        MediaScoreManager scoreManager) {
        super(client);
        this.mediaRedirector = mediaRedirector;
        this.scoreManager = scoreManager;
    }


    @Override
    public List<MediaObject> loadAll(boolean loadDeleted, List<String> ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isScore() {
        return scoreManager.getIsScoring();

    }

    public void  setScore(boolean score) {
        scoreManager.setIsScoring(score);

    }

    @Override
    public MediaObject load(boolean loadDeleted, String id) {
        MediaObject mo = loadWithCrid(id);
        if (mo == null || loadDeleted || Workflow.PUBLICATIONS.contains(mo.getWorkflow())) {
            return mo;
        } else {
            return null;
        }
    }

    @Override
    public RedirectList redirects() {
        return mediaRedirector.redirects();
    }


    protected MediaObject loadWithCrid(String id) {
        if(id.startsWith("crid://")) {
            try {
                return findByCrid(id);
            } catch(IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            id = redirect(id).orElse(id);
            return load(id, MediaObject.class);
        }
    }

    @Override
    public ScheduleResult listSchedules(Instant start, Instant stop, Order order, long offset, Integer max) {
        ExtendedScheduleForm form = new ExtendedScheduleForm(
            new SchedulePager(offset, max, null, order.direction()), new InstantRange(start, stop));
        return execute(form);
    }

    @Override
    public ScheduleResult listSchedules(Channel channel, Instant start, Instant stop, Order order, long offset, Integer max) {
        ExtendedScheduleForm form = new ExtendedScheduleForm(
            new SchedulePager(offset, max, null, order.direction()), new InstantRange(start, stop));
        form.setChannels(Collections.singletonList(channel));
        return execute(form);
    }

    @Override
    public ScheduleResult listSchedules(Channel channel, LocalDate guideDay, Order order, long offset, Integer max) {
        ExtendedScheduleForm form = new ExtendedScheduleForm(
            new SchedulePager(offset, max, null, order.direction()), guideDay);
        form.setChannels(Collections.singletonList(channel));
        return execute(form);
    }

    @Override
    public ScheduleResult listSchedules(Net net, Instant start, Instant stop, Order order, long offset, Integer max) {
        ExtendedScheduleForm form = new ExtendedScheduleForm(
            new SchedulePager(offset, max, null, order.direction()), new InstantRange(start, stop));
        form.setNet(net.getId());
        return execute(form);
    }

    @Override
    public ScheduleResult listSchedulesForBroadcaster(String broadcaster, Instant start, Instant stop, Order order, long offset, Integer max) {
        ExtendedScheduleForm form = new ExtendedScheduleForm(
            new SchedulePager(offset, max, null, order.direction()),
            new InstantRange(start, stop)
        );
        form.setBroadcaster(broadcaster);
        return execute(form);
    }

    @Override
    public ScheduleResult listSchedulesForMediaType(MediaType mediaType, Instant start, Instant stop, Order order, long offset, Integer max) {
        ExtendedScheduleForm form = new ExtendedScheduleForm(
            new SchedulePager(offset, max, null, order.direction()), new InstantRange(start, stop));
        form.setMediaType(mediaType);
        return execute(form);
    }

    @Override
    public ScheduleResult listSchedulesForAncestor(String mediaId, Instant start, Instant stop, Order order, long offset, Integer max) {
        ExtendedScheduleForm form = new ExtendedScheduleForm(
            new SchedulePager(offset, max, null, order.direction()), new InstantRange(start, stop));
        form.setDescendantOf(Collections.singletonList(mediaId));
        return execute(form);
    }

    private MediaObject findByCrid(String crid) throws IOException {
        TermQueryBuilder query = QueryBuilders.termQuery("crids", crid);
        SearchRequest request = new SearchRequest(indexNames.get(ApiMediaIndex.APIMEDIA));
        SearchSourceBuilder searchBuilder = new SearchSourceBuilder();
        searchBuilder.query(query);

        request.source(searchBuilder);

        ActionFuture<SearchResponse> searchResponseFuture = client().search(request);
        SearchResponse response = searchResponseFuture.actionGet(timeOut.toMillis(), TimeUnit.MILLISECONDS);

        SearchHits hits = response.getHits();
        if(hits.getTotalHits().value == 0) {
            return null;
        }
        return Jackson2Mapper.getInstance().readValue(hits.getHits()[0].getSourceAsString(), MediaObject.class);
    }

    private ScheduleResult execute(ExtendedScheduleForm form) {
        QueryBuilder toExecute;
        {
            BoolQueryBuilder query = QueryBuilders.boolQuery();
            query.filter(QueryBuilders.termQuery("workflow", Workflow.PUBLISHED.name()));
            if (form.hasChannels()) {
                BoolQueryBuilder channelQuery = QueryBuilders.boolQuery();
                for (Channel channel : form.getChannels()) {
                    channelQuery.should(QueryBuilders.termQuery("scheduleEvents.channel", channel.name()));
                }
                query.must(channelQuery);
            }
            if (form.getBroadcaster() != null) {
                query.must(QueryBuilders.termQuery("broadcasters.id", form.getBroadcaster()));

            }
            if (form.getNet() != null) {
                query.must(QueryBuilders.termQuery("scheduleEvents.net", form.getNet()));

            }
            if (form.getMediaType() != null) {
                query.must(QueryBuilders.termQuery("type", form.getMediaType().name()));
            }
            if (form.getDescendantOf() != null) {
                for (String descendantOf : form.getDescendantOf()) {
                    query.must(QueryBuilders.termQuery("descendantOf.midRef", descendantOf));
                }
            }

            if (form.hasStart() || form.hasStop()) {
                RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("scheduleEvents.start");
                if (form.hasStart()) {
                    Instant start = form.getDateRange().getStartValue();
                    if (!form.getDateRange().getStart().isInclusive()) {
                        start = start.plusMillis(1);
                    }
                    rangeQueryBuilder.from(start.toEpochMilli());
                }
                if (form.hasStop()) {
                    Instant stop = form.getDateRange().getStopValue();
                    if (!form.getDateRange().getStop().isInclusive()) {
                        stop = stop.minusMillis(1);
                    }
                    rangeQueryBuilder.to(stop.toEpochMilli());
                }
                query.must(rangeQueryBuilder);

            }
            if (form.getGuideDay() != null) {
                query.must(QueryBuilders.termQuery("scheduleEvents.guideDay", form.getGuideDay().atStartOfDay(Schedule.ZONE_ID).toEpochSecond() * 1000));
            }

            toExecute = simplifyQuery(query);
        }
        final List<ApiScheduleEvent> results = new ArrayList<>();
        long mediaObjectCount = 0;
        long offset = form.getPager().getOffset();
        Integer max = form.getPager().getMax();
        Optional<Long> total = Optional.empty();
        try (ElasticSearchIterator<MediaObject> searchIterator = new ElasticSearchIterator<>(client(), this::getMediaObject)) {
            SearchRequestBuilder requestBuilder = searchIterator.prepareSearch(getIndexName());
            requestBuilder.setQuery(toExecute);
            requestBuilder.addSort("scheduleEvents.start", SortOrder.DESC);

            long skipped = 0;
            long count = 0;
            searchIterator.getTotalSize();
            total = searchIterator.getTotalSize();

            OUTER:
            while (searchIterator.hasNext()) {
                MediaObject mo = searchIterator.next();
                int eventCountForMediaObject = 0;
                mediaObjectCount++;
                if (mo instanceof Program) {
                    for (ScheduleEvent e : ((Program) mo).getScheduleEvents()) {
                        if (form.test(e)) {
                            if (skipped < offset) {
                                skipped++;
                            } else {
                                ApiScheduleEvent ae = new ApiScheduleEvent(e);
                                ae.setParent(e.getParent());
                                results.add(ae);
                                eventCountForMediaObject++;
                                count++;
                            }
                            if (max != null && count > max + 2000) {
                                break OUTER;
                            }
                        } else {
                            log.debug("{} not in {}", e, form);
                        }
                    }
                }
                if (eventCountForMediaObject == 0) {
                    // this may happen if it broadcaster on the correct channel, and on the correct time
                    // _but not together_
                    // 1 schedule event has the correct channel, the other one the correct scheduleEvent.start
                    // it doesn't really matter for now, we simply didn't add it to the result
                    log.debug("Mediaobject {} not added, since it did unexpectedly not apply to {}", mo, form);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        log.debug("Found {} different media objects" , mediaObjectCount);

        switch (form.getPager().getOrder()) {
            case ASC:
                Collections.sort(results);
                break;
            case DESC:
                results.sort(Comparator.reverseOrder());
                break;
        }
        List<ApiScheduleEvent> truncated;
        if (max != null && results.size() > max) {
            truncated = results.subList(0, max);
        } else {
            truncated = results;
        }
        return new ScheduleResult(new Result<>(truncated,
            offset,
            max,
            total.map(t -> Result.Total.atLeast(Math.max(t, truncated.size()))).orElse(Result.Total.MISSING))
        );
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
    public ScheduleSearchResult findSchedules(
        ProfileDefinition<MediaObject> profile,
        ScheduleForm form,
        long offset,
        Integer max) {
        form = redirectForm(form);
        Schedule schedule;
        List<ScheduleEventSearch> scheduleEventSearches = form.getSearches() == null ? null : form.getSearches().getScheduleEvents();
        if (scheduleEventSearches != null && scheduleEventSearches.size() > 1) {
            throw new IllegalArgumentException("It is impossible to create a schedule from more then one constraint on schedule event. Please search on media in stead.");
        }
        ScheduleEventSearch scheduleEventSearch = scheduleEventSearches != null && ! scheduleEventSearches.isEmpty() ? scheduleEventSearches.get(0) : null;
        if (scheduleEventSearch != null) {
            Instant  start  = scheduleEventSearch.getBegin();
            Instant stop    = scheduleEventSearch.getEnd();
            Channel channel = scheduleEventSearch.getChannel();
            Net net = null;
            if (StringUtils.isNotEmpty(scheduleEventSearch.getNet())) {
                net = new Net(scheduleEventSearch.getNet());
            }

            if (channel != null) {
                schedule = new Schedule(channel, start, stop);
            } else if (net != null) {
                schedule = new Schedule(net, start, stop);
            } else {
                schedule = new Schedule(start, stop);
            }
            schedule.setReruns(scheduleEventSearch.getRerun());
        } else {
            scheduleEventSearch = new ScheduleEventSearch(null, null, Instant.MAX);
            if (form.getSearches() == null) {
                form.setSearches(new MediaSearch());
            }
            form.getSearches().setScheduleEvents(Collections.singletonList(scheduleEventSearch));
            schedule = new Schedule();
        }
        schedule.setFiltered(true);


        // Make sure we query enough as to get a 'full' schedule
        int maxresult = Math.max(max, MAXRESULT);
        AbstractESMediaRepository.SearchRequestWrapper request = mediaSearchRequest(profile, form, null, QueryBuilders.boolQuery(), 0L, maxresult);
        log.debug("Executing {}", request);
        GenericMediaSearchResult<MediaObject> result = executeSearchRequest(request, null, 0, maxresult, MediaObject.class);


        for (SearchResultItem<? extends MediaObject> resultItem : result.getItems()) {
            MediaObject mediaObject = resultItem.getResult();
            if (mediaObject instanceof Program) {
                Program program = (Program) mediaObject;
                for (nl.vpro.domain.media.ScheduleEvent event : program.getScheduleEvents()) {
                    ApiScheduleEvent apiEvent = new ApiScheduleEvent(event, program);
                    schedule.addScheduleEvent(apiEvent);
                }
            }
        }
        List<SearchResultItem<? extends ApiScheduleEvent>> items = new ArrayList<>();
        for (nl.vpro.domain.media.ScheduleEvent event : schedule.getScheduleEvents()) {
            SearchResultItem<ApiScheduleEvent> item = new SearchResultItem<>((ApiScheduleEvent) event, 0.0f, Collections.emptyList());
            items.add(item);
        }
        int total = items.size();
        int end = new Long(offset + max).intValue();
        items = items.subList((int) offset, Math.min(end, total));
        return new ScheduleSearchResult(items, offset, max, Result.Total.equalsTo(total));
    }
}

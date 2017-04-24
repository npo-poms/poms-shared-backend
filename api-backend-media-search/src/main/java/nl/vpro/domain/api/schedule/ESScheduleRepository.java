package nl.vpro.domain.api.schedule;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.apache.http.concurrent.BasicFuture;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Value;

import nl.vpro.domain.api.*;
import nl.vpro.domain.api.media.*;
import nl.vpro.domain.api.profile.ProfileDefinition;
import nl.vpro.domain.media.*;
import nl.vpro.domain.media.search.DateRange;
import nl.vpro.domain.media.search.SchedulePager;
import nl.vpro.elasticsearch.ESClientFactory;
import nl.vpro.elasticsearch.IndexHelper;
import nl.vpro.jackson2.Jackson2Mapper;
import nl.vpro.media.domain.es.MediaESType;
import nl.vpro.util.ThreadPools;
import nl.vpro.util.TimeUtils;

/**
 * @since 4.8
 */
@Slf4j
@ToString(callSuper = true)
public class ESScheduleRepository extends AbstractESMediaRepository implements ScheduleRepository, MediaLoader {

    private static final int MAXRESULT = 100;

    @Override
    @Value("${elasticSearch.schedule.index}")
    public void setIndexName(String indexName) {
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


    protected boolean createIndices = true;

    private final IndexHelper helper;

    public ESScheduleRepository() {
        this(null);
    }

    @Inject
    public ESScheduleRepository(ESClientFactory client) {
        super(client);
        this.helper = new IndexHelper(log, client, null, "setting/apimedia.json").mapping("program", "mapping/program.json").mapping("group", "mapping/group.json").mapping("segment", "mapping/segment.json");
    }

    @PostConstruct
    public Future createIndices() {
        helper.setIndexName(indexName);
        if(isCreateIndices()) {
            return ThreadPools.startUpExecutor.submit(helper::prepareIndex);
        } else {
            return new BasicFuture<>(null);
        }
    }

    @Override
    public List<MediaObject> loadAll(List<String> ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MediaObject load(String id) {
        if(id.startsWith("crid://")) {
            try {
                return findByCrid(id);
            } catch(IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            /* todo */
            //id = redirect(id).orElse(id);
            return load(id, MediaObject.class);
        }
    }

    @Override
    public ScheduleResult listSchedules(Instant start, Instant stop, Order order, long offset, Integer max) {
        ExtendedScheduleForm form = new ExtendedScheduleForm(
            new SchedulePager(offset, max, null, order.direction()), new DateRange(start, stop));
        return execute(form, offset, max);
    }

    @Override
    public ScheduleResult listSchedules(Channel channel, Instant start, Instant stop, Order order, long offset, Integer max) {
        ExtendedScheduleForm form = new ExtendedScheduleForm(
            new SchedulePager(offset, max, null, order.direction()), new DateRange(start, stop));
        form.setChannels(Collections.singletonList(channel));
        return execute(form, offset, max);
    }

    @Override
    public ScheduleResult listSchedules(Channel channel, LocalDate guideDay, Order order, long offset, Integer max) {
        ExtendedScheduleForm form = new ExtendedScheduleForm(
            new SchedulePager(offset, max, null, order.direction()), guideDay);
        form.setChannels(Collections.singletonList(channel));
        return execute(form, offset, max);
    }

    @Override
    public ScheduleResult listSchedules(Net net, Instant start, Instant stop, Order order, long offset, Integer max) {
        ExtendedScheduleForm form = new ExtendedScheduleForm(
            new SchedulePager(offset, max, null, order.direction()), new DateRange(start, stop));
        form.setNet(net.getId());
        return execute(form, offset, max);
    }

    @Override
    public ScheduleResult listSchedulesForBroadcaster(String broadcaster, Instant start, Instant stop, Order order, long offset, Integer max) {
        ExtendedScheduleForm form = new ExtendedScheduleForm(
            new SchedulePager(offset, max, null, order.direction()), new DateRange(start, stop));
        form.setBroadcaster(broadcaster);
        return execute(form, offset, max);
    }

    @Override
    public ScheduleResult listSchedulesForMediaType(MediaType mediaType, Instant start, Instant stop, Order order, long offset, Integer max) {
        ExtendedScheduleForm form = new ExtendedScheduleForm(
            new SchedulePager(offset, max, null, order.direction()), new DateRange(start, stop));
        form.setMediaType(mediaType);
        return execute(form, offset, max);
    }

    @Override
    public ScheduleResult listSchedulesForAncestor(String mediaId, Instant start, Instant stop, Order order, long offset, Integer max) {
        throw new UnsupportedOperationException();
    }

    public boolean isCreateIndices() {
        return createIndices;
    }

    public void setCreateIndices(boolean createIndices) {
        this.createIndices = createIndices;
    }

    public long count() {
        return client().prepareCount(indexName).get().getCount();
    }

    private MediaObject findByCrid(String crid) throws IOException {
        TermQueryBuilder query = QueryBuilders.termQuery("crids", crid);
        SearchRequest request = new SearchRequest(indexName);
        request.types(getLoadTypes());
        SearchSourceBuilder searchBuilder = new SearchSourceBuilder();
        searchBuilder.query(query);

        request.source(searchBuilder);

        ActionFuture<SearchResponse> searchResponseFuture = client().search(request);
        SearchResponse response = searchResponseFuture.actionGet(timeOut.toMillis(), TimeUnit.MILLISECONDS);

        SearchHits hits = response.getHits();
        if(hits.getTotalHits() == 0) {
            return null;
        }
        return Jackson2Mapper.getInstance().readValue(hits.getHits()[0].getSourceAsString(), MediaObject.class);
    }

    private ScheduleResult execute(ExtendedScheduleForm form, long offset, Integer max) {
        BoolQueryBuilder query = QueryBuilders.boolQuery();
        if (form.hasChannels()) {
            BoolQueryBuilder channelQuery = QueryBuilders.boolQuery();
            for(Channel channel : form.getChannels()) {
                channelQuery.should(QueryBuilders.termQuery("scheduleEvents.channel", channel.name()));
            }
            query.must(channelQuery);
        }
        if (form.getBroadcaster() != null) {
            query.should(QueryBuilders.termQuery("broadcasters.id", form.getBroadcaster()));

        }
        if (form.getNet() != null) {
            query.should(QueryBuilders.termQuery("scheduleEvents.net", form.getNet()));

        }
        if (form.getMediaType() != null) {
            query.must(QueryBuilders.termQuery("type", form.getMediaType().name()));
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
            query.must(QueryBuilders.termQuery("scheduleEvents.guideDay", form.getGuideDay().atStartOfDay(ScheduleService.ZONE_ID).toEpochSecond() * 1000));
        }

        Long total;
        try {
            total = executeCount(query, getIndexName());
        } catch(InterruptedException | ExecutionException e) {
            log.error(e.getMessage(), e);
            total = null;
        }

        SearchRequest request = new SearchRequest(getIndexName());
        request.types(getScheduleEventTypes());

        SearchSourceBuilder searchBuilder = new SearchSourceBuilder();
        searchBuilder.query(query);

        handlePaging(offset, max, searchBuilder, query, getIndexName());

        request.source(searchBuilder);

        ActionFuture<SearchResponse> searchResponseFuture =
            client().search(request);
        SearchResponse response = searchResponseFuture.actionGet(
                timeOut.toMillis(), TimeUnit.MILLISECONDS);

        SearchHits hits = response.getHits();

        List<SearchResultItem<? extends MediaObject>> adapted = adapt(hits, MediaObject.class);
        List<ApiScheduleEvent> results = new ArrayList<>();

        for (SearchResultItem<? extends MediaObject> mo : adapted) {
            int count = 0;
            for (ScheduleEvent e : mo.getResult().getScheduleEvents()) {
                if (form.test(e)) {
                    ApiScheduleEvent ae = new ApiScheduleEvent(e);
                    ae.setMediaObject(e.getMediaObject());
                    results.add(ae);
                    count++;
                }
            }
            if (count == 0) {
                // this may happen if it broadcaster on the correct channel, and on the correct time
                // _but not together_
                // 1 schedule event has the correct channel, the other one the correct scheduleEvent.start
                // it doesn't really matter for now, we simply didn't add it to the result
                log.debug("Mediaobject {} not added, since it did unexpectedly not apply to {}", mo, form);
            }

        }

        switch (form.getPager().getOrder()) {
            case ASC:
                Collections.sort(results);
                break;
            case DESC:
                results.sort(Comparator.reverseOrder());
                break;
        }

        return new ScheduleResult(new Result<>(results, offset, max, total));
    }

    protected String[] getScheduleEventTypes() {
        return MediaESType.toString(MediaESType.program);
    }

    @Override
    protected String[] getRelevantTypes() {
        return getLoadTypes();
    }



    @Override
    public ScheduleSearchResult findSchedules(ProfileDefinition<MediaObject> profile, ScheduleForm form, long offset, Integer max) {
        Schedule schedule;
        ScheduleEventSearch scheduleEventSearch = form.getSearches() == null ? null : form.getSearches().getScheduleEvents();
        if (scheduleEventSearch != null) {
            Instant  start = scheduleEventSearch.getBegin();
            Instant stop = scheduleEventSearch.getEnd();
            Channel channel = null;
            if (StringUtils.isNotEmpty(scheduleEventSearch.getChannel())) {
                channel = Channel.valueOf(scheduleEventSearch.getChannel());
            }
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
        } else {
            ScheduleEventSearch matcher = new ScheduleEventSearch(null, null, Instant.MAX);
            form.getSearches().setScheduleEvents(matcher);
            schedule = new Schedule();
        }
        schedule.setFiltered(true);

        // Make sure we query enough as to get a 'full' schedule
        int maxresult = Math.max(max, MAXRESULT);
        SearchRequest request = searchRequest(getScheduleEventTypes(), profile, form, null, null, 0L, maxresult);
        GenericMediaSearchResult<MediaObject> result = executeQuery(request, null, 0, maxresult, MediaObject.class);


        for (SearchResultItem<? extends MediaObject> resultItem : result.getItems()) {
            MediaObject mediaObject = resultItem.getResult();
            for (nl.vpro.domain.media.ScheduleEvent event : mediaObject.getScheduleEvents()) {
                ApiScheduleEvent apiEvent = new ApiScheduleEvent(event, mediaObject);
                schedule.addScheduleEvent(apiEvent);
            }
        }
        List<SearchResultItem<? extends ApiScheduleEvent>> items = new ArrayList<>();
        for (nl.vpro.domain.media.ScheduleEvent event : schedule.getScheduleEvents()) {
            SearchResultItem<ApiScheduleEvent> item = new SearchResultItem<>((ApiScheduleEvent) event, 0.0f, Collections.emptyList());
            items.add(item);
        }
        int total = items.size();
        int end = new Long(offset + max).intValue();
        items = items.subList((int) offset, end < total ? end : total);
        return new ScheduleSearchResult(items, offset, max, total);
    }
}

/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.schedule;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.annotations.cache.Cache;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.newrelic.api.agent.Trace;

import nl.vpro.api.rs.v3.exception.Exceptions;
import nl.vpro.api.rs.v3.filter.ApiMediaFilter;
import nl.vpro.domain.api.ApiScheduleEvent;
import nl.vpro.domain.api.Constants;
import nl.vpro.domain.api.Order;
import nl.vpro.domain.api.media.ScheduleForm;
import nl.vpro.domain.api.media.ScheduleResult;
import nl.vpro.domain.api.media.ScheduleSearchResult;
import nl.vpro.domain.api.media.ScheduleService;
import nl.vpro.domain.media.Channel;
import nl.vpro.domain.media.Net;
import nl.vpro.domain.media.ScheduleEvent;
import nl.vpro.swagger.SwaggerApplication;

import static nl.vpro.api.rs.v3.exception.Exceptions.handleTooManyResults;
import static nl.vpro.domain.api.Constants.*;


/**
 * @author rico
 */
@Service
@Api(tags = ScheduleRestService.TAG)
@Path(ScheduleRestService.PATH)
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public class ScheduleRestServiceImpl implements ScheduleRestService {


    private static final String MESSAGE_GUIDE_DAY = "Guide day in simple ISO8601 format, e.g. 2014-02-27";
    private static final String MESSAGE_START = "Start time in full ISO8601 format, e.g. 2014-02-27T07:06:00Z";
    private static final String MESSAGE_STOP = "Stop time in full ISO8601 format, e.g. 2014-02-28T22:06:00Z";
    private static final String MESSAGE_BROADCASTER = "Broadcaster, e.g. NTR";
    private static final String MESSAGE_ANCESTOR = "Media ID to list descendants for, e.g. 044411213";


    private static final String DEFAULT_FORM = "{\n" +
        "    \"searches\" : {\n" +
        "        \"text\" : {\n" +
        "                \"value\" : \"Argos\"\n" +
        "        }\n" +
        "    }\n" +
        "}";

    private final ScheduleService scheduleService;

    @Value("${api.schedule.expose}")
    private boolean expose;

    @Value("${api.schedule.maxResults}")
    private int maxResults = Constants.MAX_RESULTS;

    @Autowired
    public ScheduleRestServiceImpl(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PostConstruct
    private void init() {
        if(expose) {
            SwaggerApplication.inject(this);
        }
    }

    @Override
    @ApiOperation(
        httpMethod = HttpMethod.GET,
        value = "List scheduled media",
        position = 0
    )
    @GET
    @Trace(dispatcher = true)
    @Cache(maxAge = 600, isPrivate = true)
    public ScheduleResult list(
        @ApiParam(value = MESSAGE_GUIDE_DAY, required = false) @QueryParam(GUIDE_DAY) LocalDate guideDay,
        @ApiParam(value = MESSAGE_START, required = false) @QueryParam(START) Instant start,
        @ApiParam(value = MESSAGE_STOP, required = false) @QueryParam(STOP) Instant stop,
        @ApiParam(value = PROPERTIES_MESSAGE, required = false) @QueryParam(PROPERTIES) @DefaultValue(PROPERTIES_NONE) String properties,
        @QueryParam(SORT) @DefaultValue(ASC) String sort,
        @QueryParam(OFFSET) @DefaultValue(ZERO) @Min(0) long offset,
        @QueryParam(MAX) @DefaultValue(Constants.DEFAULT_MAX_RESULTS_STRING) Integer max
    ) {
        max = handleTooManyResults(max, maxResults);
        Order order = parseOrder(sort);
        if (guideDay != null) {
            if (start != null || stop != null) {
                throw new IllegalArgumentException("Specify either " + GUIDE_DAY + " _or_ " + START + "/" + STOP);
            }
            start = guideDayStart(guideDay);
            stop = guideDayStop(guideDay);
        }
        ScheduleResult result = scheduleService.list(start, stop, order, offset, max);

        ApiMediaFilter.set(properties);

        return result;
    }

    @Override
    @ApiOperation(
            httpMethod = HttpMethod.POST,
            value = "Find scheduled media",
            position = 1
    )
    @POST
    @Trace(dispatcher = true)
    public ScheduleSearchResult find(
        @Valid @ApiParam(value = "Search form", required = true, defaultValue = DEFAULT_FORM)
        ScheduleForm form,
        @QueryParam(PROFILE) String profile,
        @ApiParam(value = PROPERTIES_MESSAGE, required = false) @QueryParam(PROPERTIES) @DefaultValue(PROPERTIES_NONE) String properties,
        @QueryParam(OFFSET) @DefaultValue(ZERO) @Min(0) long offset,
        @QueryParam(MAX) @DefaultValue(Constants.DEFAULT_MAX_RESULTS_STRING) Integer max
    ) {
        max = handleTooManyResults(max, maxResults);

        ScheduleSearchResult result = scheduleService.find(form, profile, offset, max);

        ApiMediaFilter.set(properties);

        return result;
    }

    @Override
    @ApiOperation(httpMethod = HttpMethod.GET,
            value = "List scheduled media for an ancestor",
            position = 2
    )
    @Path("/ancestor/{ancestor}")
    @GET
    @Trace(dispatcher = true)
    @Cache(maxAge = 600, isPrivate = true)
    public ScheduleResult listForAncestor(
            @ApiParam(value = MESSAGE_ANCESTOR, required = true) @PathParam(ANCESTOR) String mediaId,
            @ApiParam(value = MESSAGE_GUIDE_DAY, required = false) @QueryParam(GUIDE_DAY) LocalDate guideDay,
            @ApiParam(value = MESSAGE_START, required = false) @QueryParam(START) Instant start,
            @ApiParam(value = MESSAGE_STOP, required = false) @QueryParam(STOP) Instant stop,
            @ApiParam(value = PROPERTIES_MESSAGE, required = false) @QueryParam(PROPERTIES) @DefaultValue(PROPERTIES_NONE) String properties,
            @QueryParam(SORT) @DefaultValue(ASC) String sort,
            @QueryParam(OFFSET) @DefaultValue(ZERO) @Min(0) long offset,
            @QueryParam(MAX) @DefaultValue(Constants.DEFAULT_MAX_RESULTS_STRING) Integer max
    ) {
        max = handleTooManyResults(max, maxResults);

        Order order = parseOrder(sort);

        if (guideDay != null) {
            if (start != null || stop != null) {
                throw new IllegalArgumentException("Specify either " + GUIDE_DAY + " _or_ " + START + "/" + STOP);
            }
            start = guideDayStart(guideDay);
            stop = guideDayStop(guideDay);
        }

        ScheduleResult result = scheduleService.listForAncestor(mediaId, start, stop, order, offset, max);

        ApiMediaFilter.set(properties);

        return result;
    }

    @Override
    @ApiOperation(httpMethod = HttpMethod.GET,
            value = "Current item for an ancestor",
            position = 3
    )
    @Path("/ancestor/{ancestor}/now")
    @GET
    @Trace(dispatcher = true)
    @NoCache
    public ApiScheduleEvent nowForAncestor(
            @ApiParam(value = MESSAGE_ANCESTOR, required = true) @PathParam(ANCESTOR) String mediaId,
            @ApiParam(value = PROPERTIES_MESSAGE, required = false) @QueryParam(PROPERTIES) @DefaultValue(PROPERTIES_NONE) String properties
    ) {
        Instant now = Instant.now();
        List<? extends ApiScheduleEvent> scheduleEvents = scheduleService.listForAncestor(mediaId, null, now, Order.DESC, 0L, 1).getItems();

        if (scheduleEvents.isEmpty()) {
            throw Exceptions.notFound("No current event for ancestor " + mediaId);
        }

        ApiScheduleEvent scheduleEvent = scheduleEvents.get(0);

        if (!isActiveEvent(scheduleEvent, now)) {
            throw Exceptions.notFound("No current event for ancestor " + mediaId);
        }

        ApiMediaFilter.set(properties);

        return scheduleEvent;
    }

    @Override
    @ApiOperation(httpMethod = HttpMethod.GET,
            value = "Next item for an ancestor",
            position = 4
    )
    @Path("/ancestor/{ancestor}/next")
    @GET
    @NoCache
    @Trace(dispatcher = true)
    public ApiScheduleEvent nextForAncestor(
            @ApiParam(value = MESSAGE_ANCESTOR, required = true) @PathParam(ANCESTOR) String mediaId,
            @ApiParam(value = PROPERTIES_MESSAGE, required = false) @QueryParam(PROPERTIES) @DefaultValue(PROPERTIES_NONE) String properties
    ) {
        Instant now = Instant.now();
        List<? extends ApiScheduleEvent> scheduleEvents = scheduleService.listForAncestor(mediaId, now, null, Order.ASC, 0L, 1).getItems();

        if (scheduleEvents.isEmpty()) {
            throw Exceptions.notFound("No next event for ancestor " + mediaId);
        }

        ApiScheduleEvent scheduleEvent = scheduleEvents.get(0);

        ApiMediaFilter.set(properties);

        return scheduleEvent;
    }

    @Override
    @ApiOperation(httpMethod = HttpMethod.GET,
            value = "List scheduled media for a broadcaster",
            position = 5
    )
    @Path("/broadcaster/{broadcaster}")
    @GET
    @Trace(dispatcher = true)
    @Cache(maxAge = 600, isPrivate = true)
    public ScheduleResult listBroadcaster(
            @ApiParam(value = MESSAGE_BROADCASTER, required = true) @PathParam(BROADCASTER) String broadcaster,
            @ApiParam(value = MESSAGE_GUIDE_DAY, required = false) @QueryParam(GUIDE_DAY) LocalDate guideDay,
            @ApiParam(value = MESSAGE_START, required = false) @QueryParam(START) Instant start,
            @ApiParam(value = MESSAGE_STOP, required = false) @QueryParam(STOP) Instant stop,
            @ApiParam(value = PROPERTIES_MESSAGE, required = false) @QueryParam(PROPERTIES) @DefaultValue(PROPERTIES_NONE) String properties,
            @QueryParam(SORT) @DefaultValue(ASC) String sort,
            @QueryParam(OFFSET) @DefaultValue(ZERO) @Min(0) long offset,
            @QueryParam(MAX) @DefaultValue(Constants.DEFAULT_MAX_RESULTS_STRING) Integer max
    ) {
        max = handleTooManyResults(max, maxResults);
        Order order = parseOrder(sort);
        if (guideDay != null) {
            if (start != null || stop != null) {
                throw new IllegalArgumentException("Specify either " + GUIDE_DAY + " _or_ " + START + "/" + STOP);
            }
            start = guideDayStart(guideDay);
            stop = guideDayStop(guideDay);
        }
        ScheduleResult result = scheduleService.listForBroadcaster(broadcaster, start, stop, order, offset, max);

        ApiMediaFilter.set(properties);

        return result;
    }

    @Override
    @ApiOperation(
            httpMethod = HttpMethod.GET,
            value = "Current item for broadcaster",
            position = 6
    )
    @Path("/broadcaster/{broadcaster}/now")
    @Trace(dispatcher = true)
    @GET
    @NoCache
    public ApiScheduleEvent nowForBroadcaster(
            @ApiParam(value = MESSAGE_BROADCASTER, required = true) @PathParam(BROADCASTER) String broadcaster,
            @ApiParam(value = PROPERTIES_MESSAGE, required = false) @QueryParam(PROPERTIES) @DefaultValue(PROPERTIES_NONE) String properties
    ) {
        Instant now = Instant.now();
        List<? extends ApiScheduleEvent> scheduleEvents = scheduleService.listForBroadcaster(broadcaster, null, now, Order.DESC, 0L, 1).getItems();

        if (scheduleEvents.isEmpty()) {
            throw Exceptions.notFound("No current event for broadcaster " + broadcaster);
        }

        ApiScheduleEvent scheduleEvent = scheduleEvents.get(0);

        if (!isActiveEvent(scheduleEvent, now)) {
            throw Exceptions.notFound("No current event for broadcaster " + broadcaster);
        }

        ApiMediaFilter.set(properties);

        return scheduleEvent;
    }

    @Override
    @ApiOperation(
            httpMethod = HttpMethod.GET,
            value = "Next item for broadcaster",
            position = 7
    )
    @Path("/broadcaster/{broadcaster}/next")
    @GET
    @Trace(dispatcher = true)
    @NoCache
    public ApiScheduleEvent nextForBroadcaster(
            @ApiParam(value = MESSAGE_BROADCASTER, required = true) @PathParam(BROADCASTER) String broadcaster,
            @ApiParam(value = PROPERTIES_MESSAGE, required = false) @QueryParam(PROPERTIES) @DefaultValue(PROPERTIES_NONE) String properties
    ) {
        final Instant now = Instant.now();
        final List<? extends ApiScheduleEvent> scheduleEvents = scheduleService.listForBroadcaster(broadcaster, now, null, Order.ASC, 0L, 1).getItems();

        if (scheduleEvents.isEmpty()) {
            throw Exceptions.notFound("No next event for broadcaster " + broadcaster);
        }

        ApiScheduleEvent scheduleEvent = scheduleEvents.get(0);

        ApiMediaFilter.set(properties);

        return scheduleEvent;
    }

    @Override
    @ApiOperation(httpMethod = HttpMethod.GET,
        value = "List scheduled media for a channel",
        position = 8
    )
    @Path("/channel/{channel}")
    @GET
    @Trace(dispatcher = true)
    @Cache(maxAge = 600, isPrivate = true)
    public ScheduleResult listChannel(
        @ApiParam(required = true, defaultValue = "NED1") @PathParam(CHANNEL) String channel,
        @ApiParam(value = MESSAGE_GUIDE_DAY, required = false) @QueryParam(GUIDE_DAY) LocalDate guideDay,
        @ApiParam(value = MESSAGE_START, required = false) @QueryParam(START) Instant start,
        @ApiParam(value = MESSAGE_STOP, required = false) @QueryParam(STOP) Instant stop,
        @ApiParam(value = PROPERTIES_MESSAGE, required = false) @QueryParam(PROPERTIES) @DefaultValue(PROPERTIES_NONE) String properties,
        @QueryParam(SORT) @DefaultValue(ASC) String sort,
        @QueryParam(OFFSET) @DefaultValue(ZERO) @Min(0) long offset,
        @QueryParam(MAX) @DefaultValue(Constants.DEFAULT_MAX_RESULTS_STRING) Integer max
    ) {
        max = handleTooManyResults(max, maxResults);
        Order order = parseOrder(sort);
        Channel chan = getChannel(channel);
        if (guideDay != null) {
            if (start != null || stop != null) {
                throw new IllegalArgumentException("Specify either " + GUIDE_DAY + " _or_ " + START + "/" + STOP);
            }
            start = guideDayStart(guideDay);
            stop = guideDayStop(guideDay);
        }
        ApiMediaFilter.removeFilter();
        ScheduleResult result = scheduleService.list(chan, start, stop, order, offset, max);

        ApiMediaFilter.set(properties);

        return result;
    }

    @Override
    @ApiOperation(
        httpMethod = HttpMethod.GET,
        value = "Current item on channel",
        position = 9
    )
    @Path("/channel/{channel}/now")
    @Trace(dispatcher = true)
    @GET
    @NoCache
    public ApiScheduleEvent nowForChannel(
        @ApiParam(required = true, defaultValue = "NED1") @PathParam(CHANNEL) String channel,
        @ApiParam(value = PROPERTIES_MESSAGE, required = false) @QueryParam(PROPERTIES) @DefaultValue(PROPERTIES_NONE) String properties
    ) {
        Instant now = Instant.now();
        Channel chan = getChannel(channel);
        List<? extends ApiScheduleEvent> scheduleEvents = scheduleService.list(chan, null, now, Order.DESC, 0L, 1).getItems();

        if (scheduleEvents.isEmpty()) {
            throw Exceptions.notFound("No current event on channel " + chan);
        }

        ApiScheduleEvent scheduleEvent = scheduleEvents.get(0);

        if (!isActiveEvent(scheduleEvent, now)) {
            throw Exceptions.notFound("No current event on channel " + chan);
        }

        ApiMediaFilter.set(properties);

        return scheduleEvent;
    }

    @Override
    @ApiOperation(
            httpMethod = HttpMethod.GET,
            value = "Next item on channel",
            position = 10
    )
    @Path("/channel/{channel}/next")
    @GET
    @Trace(dispatcher = true)
    @NoCache
    public ApiScheduleEvent nextForChannel(
        @ApiParam(required = true, defaultValue = "NED1") @PathParam(CHANNEL) String channel,
        @ApiParam(value = PROPERTIES_MESSAGE, required = false) @QueryParam(PROPERTIES) @DefaultValue(PROPERTIES_NONE) String properties
    ) {
        Instant now = Instant.now();
        Channel chan = getChannel(channel);
        List<? extends ApiScheduleEvent> scheduleEvents = scheduleService.list(chan, now, null, Order.ASC, 0L, 1).getItems();

        if (scheduleEvents.isEmpty()) {
            throw Exceptions.notFound("No next event on channel " + chan);
        }

        ApiScheduleEvent scheduleEvent = scheduleEvents.get(0);

        ApiMediaFilter.set(properties);

        return scheduleEvent;
    }

    @Override
    @ApiOperation(
        httpMethod = HttpMethod.GET,
        value = "List scheduled media for a net",
        position = 11
    )
    @Path("/net/{net}")
    @GET
    @Trace(dispatcher = true)
    @Cache(maxAge = 600, isPrivate = true)
    public ScheduleResult listNet(
        @ApiParam(required = true, defaultValue = "ZAPP") @PathParam(NET) String net,
        @ApiParam(value = MESSAGE_GUIDE_DAY, required = false) @QueryParam(GUIDE_DAY) LocalDate guideDay,
        @ApiParam(value = MESSAGE_START, required = false) @QueryParam(START) Instant start,
        @ApiParam(value = MESSAGE_STOP, required = false) @QueryParam(STOP) Instant stop,
        @ApiParam(value = PROPERTIES_MESSAGE, required = false) @QueryParam(PROPERTIES) @DefaultValue(PROPERTIES_NONE) String properties,
        @QueryParam(SORT) @DefaultValue(ASC) String sort,
        @QueryParam(OFFSET) @DefaultValue(ZERO) @Min(0) long offset,
        @QueryParam(MAX) @DefaultValue(Constants.DEFAULT_MAX_RESULTS_STRING) Integer max
    ) {
        max = handleTooManyResults(max, maxResults);
        Order order = parseOrder(sort);
        if (guideDay != null) {
            if (start != null || stop != null) {
                throw new IllegalArgumentException("Specify either " + GUIDE_DAY + " _or_ " + START + "/" + STOP);
            }
            start = guideDayStart(guideDay);
            stop = guideDayStop(guideDay);
        }
        ScheduleResult result = scheduleService.list(new Net(net), start, stop, order, offset, max);

        ApiMediaFilter.set(properties);

        return result;
    }

    @Override
    @ApiOperation(
        httpMethod = HttpMethod.GET,
        value = "Current item on net",
        position = 12
    )
    @Path("/net/{net}/now")
    @GET
    @Trace(dispatcher = true)
    @NoCache
    public ApiScheduleEvent nowForNet(
        @ApiParam(required = true, defaultValue = "ZAPP") @PathParam(NET) String net,
        @ApiParam(value = PROPERTIES_MESSAGE, required = false) @QueryParam(PROPERTIES) @DefaultValue(PROPERTIES_NONE) String properties
    ) {
        Instant now = Instant.now();
        List<? extends ApiScheduleEvent> scheduleEvents = scheduleService.list(new Net(net), null, now, Order.DESC, 0L, 1).getItems();

        if (scheduleEvents.isEmpty()) {
            throw Exceptions.notFound("No current event on net " + net);
        }

        ApiScheduleEvent scheduleEvent = scheduleEvents.get(0);

        if (!isActiveEvent(scheduleEvent, now)) {
            throw Exceptions.notFound("No current event on net " + net);
        }

        ApiMediaFilter.set(properties);

        return scheduleEvent;
    }

    @Override
    @ApiOperation(
        httpMethod = HttpMethod.GET,
        value = "Next item on net",
        position = 13
    )
    @Path("/net/{net}/next")
    @GET
    @Trace(dispatcher = true)
    @NoCache
    public ApiScheduleEvent nextForNet(
        @ApiParam(required = true, defaultValue = "ZAPP") @PathParam(NET) String net,
        @ApiParam(value = PROPERTIES_MESSAGE, required = false) @QueryParam(PROPERTIES) @DefaultValue(PROPERTIES_NONE) String properties
    ) {
        Instant now = Instant.now();
        List<? extends ApiScheduleEvent> scheduleEvents = scheduleService.list(new Net(net), now, null, Order.ASC, 0L, 1).getItems();

        if (scheduleEvents.isEmpty()) {
            throw Exceptions.notFound("No next event on net " + net);
        }

        ApiScheduleEvent scheduleEvent = scheduleEvents.get(0);

        ApiMediaFilter.set(properties);

        return scheduleEvent;
    }

    private static Channel getChannel(String channel) {
        return Channel.valuesOf(Collections.singletonList(channel)).get(0);
    }

    private static Instant  guideDayStart(LocalDate guideDay) {
        ZonedDateTime zoned = guideDay.atStartOfDay(ScheduleService.ZONE_ID);
        return ScheduleService.guideDayStart(zoned.toLocalDate()).toInstant();
    }



    private static Instant guideDayStop(LocalDate guideDay) {
        ZonedDateTime zoned = guideDay.atStartOfDay(ScheduleService.ZONE_ID);
        return ScheduleService.guideDayStop(zoned.toLocalDate()).toInstant();
    }

    private Order parseOrder(String order) {
        try {
            return Order.valueOf(order.toUpperCase());
        } catch(IllegalArgumentException e) {
            throw Exceptions.badRequest("Invalid order \"{}\"", order);
        }
    }

    boolean isActiveEvent(ScheduleEvent scheduleEvent, Instant time) {
        if (scheduleEvent == null || time == null) {
            return false;
        }

        Instant start = scheduleEvent.getStartInstant();
        Instant end = start.plus(scheduleEvent.getDurationTime());

        return ! start.isAfter(time) && end.isAfter(time);
    }
}

/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.schedule;

import io.swagger.annotations.*;

import java.time.*;
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

import nl.vpro.api.rs.exception.Exceptions;
import nl.vpro.api.rs.filter.ApiMediaFilter;
import nl.vpro.api.rs.v3.media.MediaRestService;
import nl.vpro.domain.api.*;
import nl.vpro.domain.api.media.*;
import nl.vpro.domain.media.*;
import nl.vpro.swagger.SwaggerApplication;

import static nl.vpro.api.rs.exception.Exceptions.handleTooManyResults;
import static nl.vpro.domain.api.Constants.*;


/**
 * @author rico
 */
@Service
@Api(tags = {ScheduleRestService.TAG, MediaRestService.TAG}) //  documented with media, so also in that tag!
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

    private Duration scheduleEventWindow = Duration.ofDays(7);


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
        value = "List scheduled media"
    )
    @GET
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
        max = handleTooManyResults(offset, max, maxResults);
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
            value = "Find scheduled media"
    )
    @POST
    public ScheduleSearchResult find(
        @Valid @ApiParam(value = "Search form", required = true, defaultValue = DEFAULT_FORM)
        ScheduleForm form,
        @QueryParam(SORT) @DefaultValue(ASC) String sort,
        @QueryParam(PROFILE) String profile,
        @ApiParam(value = PROPERTIES_MESSAGE, required = false) @QueryParam(PROPERTIES) @DefaultValue(PROPERTIES_NONE) String properties,
        @QueryParam(OFFSET) @DefaultValue(ZERO) @Min(0) long offset,
        @QueryParam(MAX) @DefaultValue(Constants.DEFAULT_MAX_RESULTS_STRING) Integer max
    ) {
        max = handleTooManyResults(offset, max, maxResults);

        ScheduleSearchResult result = scheduleService.find(form, parseOrder(sort), profile, offset, max);

        ApiMediaFilter.set(properties);

        return result;
    }

    @Override
    @ApiOperation(httpMethod = HttpMethod.GET,
            value = "List scheduled media for an ancestor"
    )
    @Path("/ancestor/{ancestor}")
    @GET
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
        max = handleTooManyResults(offset, max, maxResults);

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
            value = "Current item for an ancestor"
    )
    @Path("/ancestor/{ancestor}/now")
    @GET
    @NoCache
    public ApiScheduleEvent nowForAncestor(
            @ApiParam(value = MESSAGE_ANCESTOR, required = true) @PathParam(ANCESTOR) String mediaId,
            @ApiParam(value = PROPERTIES_MESSAGE, required = false) @QueryParam(PROPERTIES) @DefaultValue(PROPERTIES_NONE) String properties,
            @QueryParam(NOW) Instant now
    ) {
        if (now == null) {
            now = Instant.now();
        }
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
            value = "Next item for an ancestor"
    )
    @Path("/ancestor/{ancestor}/next")
    @GET
    @NoCache
    public ApiScheduleEvent nextForAncestor(
            @ApiParam(value = MESSAGE_ANCESTOR, required = true) @PathParam(ANCESTOR) String mediaId,
            @ApiParam(value = PROPERTIES_MESSAGE, required = false) @QueryParam(PROPERTIES) @DefaultValue(PROPERTIES_NONE) String properties,
            @QueryParam(NOW) Instant now

    ) {
        if (now == null) {
            now = Instant.now();
        }
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
            value = "List scheduled media for a broadcaster"
    )
    @Path("/broadcaster/{broadcaster}")
    @GET
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
        max = handleTooManyResults(offset, max, maxResults);
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
            value = "Current item for broadcaster. I.e. the item that started most recently, and is still running."
    )
    @Path("/broadcaster/{broadcaster}/now")
    @GET
    @NoCache
    public ApiScheduleEvent nowForBroadcaster(
            @ApiParam(value = MESSAGE_BROADCASTER, required = true) @PathParam(BROADCASTER) String broadcaster,
            @ApiParam(value = PROPERTIES_MESSAGE, required = false) @QueryParam(PROPERTIES) @DefaultValue(PROPERTIES_NONE) String properties,
            @ApiParam(value = MUST_BE_RUNNING_MESSAGE)  @QueryParam(MUST_BE_RUNNING) @DefaultValue("true") boolean mustBeRunning,
            @QueryParam(NOW) Instant now

    ) {
        if (now == null) {
            now = Instant.now();
        }
        Instant start = mustBeRunning ? now.minus(Duration.ofDays(1)) : now.minus(scheduleEventWindow);
        List<? extends ApiScheduleEvent> scheduleEvents = scheduleService.listForBroadcaster(
            broadcaster,
            start,
            now,
            Order.DESC, 0L, 100).getItems();

        if (scheduleEvents.isEmpty()) {
            throw Exceptions.notFound("No current event for broadcaster " + broadcaster);
        }

        ApiScheduleEvent scheduleEvent = scheduleEvents.get(0);

        if (mustBeRunning && !isActiveEvent(scheduleEvent, now)) {
            throw Exceptions.notFound("No current event for broadcaster " + broadcaster);
        }

        ApiMediaFilter.set(properties);

        return scheduleEvent;
    }

    @Override
    @ApiOperation(
            httpMethod = HttpMethod.GET,
            value = "Next item for broadcaster"
    )
    @Path("/broadcaster/{broadcaster}/next")
    @GET
    @NoCache
    public ApiScheduleEvent nextForBroadcaster(
            @ApiParam(value = MESSAGE_BROADCASTER, required = true) @PathParam(BROADCASTER) String broadcaster,
            @ApiParam(value = PROPERTIES_MESSAGE, required = false) @QueryParam(PROPERTIES) @DefaultValue(PROPERTIES_NONE) String properties,
            @QueryParam(NOW) Instant now

    ) {
        if (now == null) {
            now = Instant.now();
        }
        Instant to = now.plus(Duration.ofDays(7));
        final List<? extends ApiScheduleEvent> scheduleEvents = scheduleService.listForBroadcaster(broadcaster, now, to, Order.ASC, 0L, 1).getItems();

        if (scheduleEvents.isEmpty()) {
            throw Exceptions.notFound("No next event for broadcaster " + broadcaster);
        }

        ApiScheduleEvent scheduleEvent = scheduleEvents.get(0);

        ApiMediaFilter.set(properties);

        return scheduleEvent;
    }

    @Override
    @ApiOperation(httpMethod = HttpMethod.GET,
        value = "List scheduled media for a channel"
    )
    @Path("/channel/{channel}")
    @GET
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
        max = handleTooManyResults(offset, max, maxResults);
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
        value = "Current item on channel"
    )
    @Path("/channel/{channel}/now")
    @GET
    @NoCache
    public ApiScheduleEvent nowForChannel(
        @ApiParam(required = true, defaultValue = "NED1") @PathParam(CHANNEL) String channel,
        @ApiParam(value = PROPERTIES_MESSAGE, required = false) @QueryParam(PROPERTIES) @DefaultValue(PROPERTIES_NONE) String properties,
        @ApiParam(value = MUST_BE_RUNNING_MESSAGE)  @QueryParam(MUST_BE_RUNNING) @DefaultValue("true") boolean mustBeRunning,
        @QueryParam(NOW) Instant now

    ) {
        if (now == null) {
            now = Instant.now();
        }
        Instant start = mustBeRunning ? now.minus(Duration.ofDays(1)) : now.minus(scheduleEventWindow);

        Channel chan = getChannel(channel);
        List<? extends ApiScheduleEvent> scheduleEvents = scheduleService.list(chan, start, now, Order.DESC, 0L, 1).getItems();

        if (scheduleEvents.isEmpty()) {
            throw Exceptions.notFound("No current event on channel " + chan);
        }

        ApiScheduleEvent scheduleEvent = scheduleEvents.get(0);

        if (mustBeRunning && !isActiveEvent(scheduleEvent, now)) {
            throw Exceptions.notFound("No current event on channel " + chan);
        }

        ApiMediaFilter.set(properties);

        return scheduleEvent;
    }

    @Override
    @ApiOperation(
            httpMethod = HttpMethod.GET,
            value = "Next item on channel"
    )
    @Path("/channel/{channel}/next")
    @GET
    @NoCache
    public ApiScheduleEvent nextForChannel(
        @ApiParam(required = true, defaultValue = "NED1") @PathParam(CHANNEL) String channel,
        @ApiParam(value = PROPERTIES_MESSAGE, required = false) @QueryParam(PROPERTIES) @DefaultValue(PROPERTIES_NONE) String properties,
        @QueryParam(NOW) Instant now

    ) {
        if (now == null) {
            now = Instant.now();
        }
        Instant to = now.plus(scheduleEventWindow);
        Channel chan = getChannel(channel);
        List<? extends ApiScheduleEvent> scheduleEvents = scheduleService.list(chan, now, to, Order.ASC, 0L, 1).getItems();

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
        value = "List scheduled media for a net"
    )
    @Path("/net/{net}")
    @GET
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
        max = handleTooManyResults(offset, max, maxResults);
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
        value = "Current item on net"
    )
    @Path("/net/{net}/now")
    @GET
    @NoCache
    public ApiScheduleEvent nowForNet(
        @ApiParam(required = true, defaultValue = "ZAPP") @PathParam(NET) String net,
        @ApiParam(value = PROPERTIES_MESSAGE, required = false) @QueryParam(PROPERTIES) @DefaultValue(PROPERTIES_NONE) String properties,
        @ApiParam(value = MUST_BE_RUNNING_MESSAGE)  @QueryParam(MUST_BE_RUNNING) @DefaultValue("true") boolean mustBeRunning,

        @QueryParam(NOW) Instant now

    ) {
        if (now == null) {
            now = Instant.now();
        }
        Instant start = mustBeRunning ? now.minus(Duration.ofDays(1)) : now.minus(scheduleEventWindow);
        List<? extends ApiScheduleEvent> scheduleEvents = scheduleService.list(new Net(net), start, now, Order.DESC, 0L, 1).getItems();

        if (scheduleEvents.isEmpty()) {
            throw Exceptions.notFound("No current event on net " + net);
        }

        ApiScheduleEvent scheduleEvent = scheduleEvents.get(0);

        if (mustBeRunning && !isActiveEvent(scheduleEvent, now)) {
            throw Exceptions.notFound("No current event on net " + net);
        }

        ApiMediaFilter.set(properties);

        return scheduleEvent;
    }

    @Override
    @ApiOperation(
        httpMethod = HttpMethod.GET,
        value = "Next item on net"
    )
    @Path("/net/{net}/next")
    @GET
    @NoCache
    public ApiScheduleEvent nextForNet(
        @ApiParam(required = true, defaultValue = "ZAPP") @PathParam(NET) String net,
        @ApiParam(value = PROPERTIES_MESSAGE, required = false) @QueryParam(PROPERTIES) @DefaultValue(PROPERTIES_NONE) String properties,
        @QueryParam(NOW) Instant now

    ) {
        if (now == null) {
            now = Instant.now();
        }
        Instant to = now.plus(scheduleEventWindow);
        List<? extends ApiScheduleEvent> scheduleEvents = scheduleService.list(new Net(net), now, to, Order.ASC, 0L, 1).getItems();

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

    boolean isActiveEvent(ScheduleEvent scheduleEvent, Instant now) {
        if (scheduleEvent == null || now == null) {
            return false;
        }
        if (scheduleEvent.getDuration() == null){
            return false;
        }
        return scheduleEvent.asRange().contains(now);
    }
}

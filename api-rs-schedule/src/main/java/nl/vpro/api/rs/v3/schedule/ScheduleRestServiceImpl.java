/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.schedule;

import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.newrelic.api.agent.Trace;
import com.wordnik.swagger.annotations.*;

import nl.vpro.api.rs.v3.exception.Exceptions;
import nl.vpro.api.rs.v3.filter.ApiMediaFilter;
import nl.vpro.domain.api.ApiScheduleEvent;
import nl.vpro.domain.api.Constants;
import nl.vpro.domain.api.Order;
import nl.vpro.domain.api.media.MediaForm;
import nl.vpro.domain.api.media.ScheduleResult;
import nl.vpro.domain.api.media.ScheduleSearchResult;
import nl.vpro.domain.api.media.ScheduleService;
import nl.vpro.domain.media.Channel;
import nl.vpro.domain.media.Net;
import nl.vpro.domain.media.ScheduleEvent;
import nl.vpro.resteasy.DateFormat;
import nl.vpro.resteasy.ISO8601Format;
import nl.vpro.swagger.SwaggerApplication;

import static nl.vpro.api.rs.v3.exception.Exceptions.handleTooManyResults;

/**
 * @author rico
 */
@Service
@Api(
    value = ScheduleRestService.PATH,
    description = "see <a href='http://wiki.publiekeomroep.nl/display/npoapi/Media-+en+gids-API'>wiki</a>",
    position = 1)
@Path(ScheduleRestService.PATH)
@Produces(
    {MediaType.APPLICATION_JSON,
        MediaType.APPLICATION_XML})
public class ScheduleRestServiceImpl implements ScheduleRestService {

    private static final String CLIENT_ERROR = "Client error";
    private static final String NOT_FOUND = "Not found";
    private static final String SERVER_ERROR = "Server error";

    private static final String MESSAGE_GUIDE_DAY = "Guide day in simple ISO8601 format, e.g. 2014-02-27";
    private static final String MESSAGE_START = "Start time in full ISO8601 format, e.g. 2014-02-27T07:06:00Z";
    private static final String MESSAGE_STOP = "Stop time in full ISO8601 format, e.g. 2014-02-28T22:06:00Z";
    private static final String MESSAGE_PROPERTIES = "Optimize media result for these returned properties";
    private static final String MESSAGE_BROADCASTER = "Broadcaster, e.g. NTR";
    private static final String MESSAGE_ANCESTOR = "Media ID to list descendants for, e.g. 044411213";

    private static final String NONE = "none";

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
    @ApiResponses(value = {@ApiResponse(code = 400, message = CLIENT_ERROR), @ApiResponse(code = 404, message = NOT_FOUND), @ApiResponse(code = 500, message = SERVER_ERROR)})
    @Trace(dispatcher = true)
    public ScheduleResult list(
        @ApiParam(value = MESSAGE_GUIDE_DAY, required = false) @QueryParam(GUIDE_DAY) @DateFormat(YEAR_MONTH_DATE) Date guideDay,
        @ApiParam(value = MESSAGE_START, required = false) @QueryParam(START) @ISO8601Format Date start,
        @ApiParam(value = MESSAGE_STOP, required = false) @QueryParam(STOP) @ISO8601Format Date stop,
        @ApiParam(value = MESSAGE_PROPERTIES, required = false) @QueryParam(PROPERTIES) @DefaultValue(NONE) String properties,
        @QueryParam(SORT) @DefaultValue(ASC) String sort,
        @QueryParam(OFFSET) @DefaultValue(ZERO) Long offset,
        @QueryParam(MAX) @DefaultValue(Constants.DEFAULT_MAX_RESULTS_STRING) Integer max
    ) {
        max = handleTooManyResults(max);
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
    @ApiResponses(value = {@ApiResponse(code = 400, message = CLIENT_ERROR), @ApiResponse(code = 404, message = NOT_FOUND), @ApiResponse(code = 500, message = SERVER_ERROR)})
    @Trace(dispatcher = true)
    public ScheduleSearchResult find(
            @ApiParam(value = "Search form", required = true, defaultValue = DEFAULT_FORM) MediaForm form,
            @QueryParam(PROFILE) String profile,
            @ApiParam(value = MESSAGE_PROPERTIES, required = false) @QueryParam(PROPERTIES) @DefaultValue(NONE) String properties,
            @QueryParam(OFFSET) @DefaultValue(ZERO) Long offset,
            @QueryParam(MAX) @DefaultValue(Constants.DEFAULT_MAX_RESULTS_STRING) Integer max
    ) {
        max = handleTooManyResults(max);

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
    @ApiResponses(value = {@ApiResponse(code = 400, message = CLIENT_ERROR), @ApiResponse(code = 404, message = NOT_FOUND), @ApiResponse(code = 500, message = SERVER_ERROR)})
    @Trace(dispatcher = true)
    public ScheduleResult listForAncestor(
            @ApiParam(value = MESSAGE_ANCESTOR, required = true) @PathParam(ANCESTOR) String mediaId,
            @ApiParam(value = MESSAGE_GUIDE_DAY, required = false) @QueryParam(GUIDE_DAY) @DateFormat(YEAR_MONTH_DATE) Date guideDay,
            @ApiParam(value = MESSAGE_START, required = false) @QueryParam(START) @ISO8601Format Date start,
            @ApiParam(value = MESSAGE_STOP, required = false) @QueryParam(STOP) @ISO8601Format Date stop,
            @ApiParam(value = MESSAGE_PROPERTIES, required = false) @QueryParam(PROPERTIES) @DefaultValue(NONE) String properties,
            @QueryParam(SORT) @DefaultValue(ASC) String sort,
            @QueryParam(OFFSET) @DefaultValue(ZERO) Long offset,
            @QueryParam(MAX) @DefaultValue(Constants.DEFAULT_MAX_RESULTS_STRING) Integer max
    ) {
        max = handleTooManyResults(max);

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
    @ApiResponses(value = {@ApiResponse(code = 400, message = CLIENT_ERROR), @ApiResponse(code = 404, message = NOT_FOUND), @ApiResponse(code = 500, message = SERVER_ERROR)})
    @Trace(dispatcher = true)
    public ApiScheduleEvent nowForAncestor(
            @ApiParam(value = MESSAGE_ANCESTOR, required = true) @PathParam(ANCESTOR) String mediaId,
            @ApiParam(value = MESSAGE_PROPERTIES, required = false) @QueryParam(PROPERTIES) @DefaultValue(NONE) String properties
    ) {
        Date now = new Date();
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
    @ApiResponses(value = {@ApiResponse(code = 400, message = CLIENT_ERROR), @ApiResponse(code = 404, message = NOT_FOUND), @ApiResponse(code = 500, message = SERVER_ERROR)})
    @Trace(dispatcher = true)
    public ApiScheduleEvent nextForAncestor(
            @ApiParam(value = MESSAGE_ANCESTOR, required = true) @PathParam(ANCESTOR) String mediaId,
            @ApiParam(value = MESSAGE_PROPERTIES, required = false) @QueryParam(PROPERTIES) @DefaultValue(NONE) String properties
    ) {
        Date now = new Date();
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
    @ApiResponses(value = {@ApiResponse(code = 400, message = CLIENT_ERROR), @ApiResponse(code = 404, message = NOT_FOUND), @ApiResponse(code = 500, message = SERVER_ERROR)})
    @Trace(dispatcher = true)
    public ScheduleResult listBroadcaster(
            @ApiParam(value = MESSAGE_BROADCASTER, required = true) @PathParam(BROADCASTER) String broadcaster,
            @ApiParam(value = MESSAGE_GUIDE_DAY, required = false) @QueryParam(GUIDE_DAY) @DateFormat(YEAR_MONTH_DATE) Date guideDay,
            @ApiParam(value = MESSAGE_START, required = false) @QueryParam(START) @ISO8601Format Date start,
            @ApiParam(value = MESSAGE_STOP, required = false) @QueryParam(STOP) @ISO8601Format Date stop,
            @ApiParam(value = MESSAGE_PROPERTIES, required = false) @QueryParam(PROPERTIES) @DefaultValue(NONE) String properties,
            @QueryParam(SORT) @DefaultValue(ASC) String sort,
            @QueryParam(OFFSET) @DefaultValue(ZERO) Long offset,
            @QueryParam(MAX) @DefaultValue(Constants.DEFAULT_MAX_RESULTS_STRING) Integer max
    ) {
        max = handleTooManyResults(max);
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
    @ApiResponses(value = {@ApiResponse(code = 400, message = CLIENT_ERROR), @ApiResponse(code = 404, message = NOT_FOUND), @ApiResponse(code = 500, message = SERVER_ERROR)})
    @Trace(dispatcher = true)
    @GET
    public ApiScheduleEvent nowForBroadcaster(
            @ApiParam(value = MESSAGE_BROADCASTER, required = true) @PathParam(BROADCASTER) String broadcaster,
            @ApiParam(value = MESSAGE_PROPERTIES, required = false) @QueryParam(PROPERTIES) @DefaultValue(NONE) String properties
    ) {
        Date now = new Date();
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
    @ApiResponses(value = {@ApiResponse(code = 400, message = CLIENT_ERROR), @ApiResponse(code = 404, message = NOT_FOUND), @ApiResponse(code = 500, message = SERVER_ERROR)})
    @Trace(dispatcher = true)
    public ApiScheduleEvent nextForBroadcaster(
            @ApiParam(value = MESSAGE_BROADCASTER, required = true) @PathParam(BROADCASTER) String broadcaster,
            @ApiParam(value = MESSAGE_PROPERTIES, required = false) @QueryParam(PROPERTIES) @DefaultValue(NONE) String properties
    ) {
        final Date now = new Date();
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
    @ApiResponses(value = {@ApiResponse(code = 400, message = CLIENT_ERROR), @ApiResponse(code = 404, message = NOT_FOUND), @ApiResponse(code = 500, message = SERVER_ERROR)})
    @Trace(dispatcher = true)
    public ScheduleResult listChannel(
        @ApiParam(required = true, defaultValue = "NED1") @PathParam(CHANNEL) String channel,
        @ApiParam(value = MESSAGE_GUIDE_DAY, required = false) @QueryParam(GUIDE_DAY) @DateFormat(YEAR_MONTH_DATE) Date guideDay,
        @ApiParam(value = MESSAGE_START, required = false) @QueryParam(START) @ISO8601Format Date start,
        @ApiParam(value = MESSAGE_STOP, required = false) @QueryParam(STOP) @ISO8601Format Date stop,
        @ApiParam(value = MESSAGE_PROPERTIES, required = false) @QueryParam(PROPERTIES) @DefaultValue(NONE) String properties,
        @QueryParam(SORT) @DefaultValue(ASC) String sort,
        @QueryParam(OFFSET) @DefaultValue(ZERO) Long offset,
        @QueryParam(MAX) @DefaultValue(Constants.DEFAULT_MAX_RESULTS_STRING) Integer max
    ) {
        max = handleTooManyResults(max);
        Order order = parseOrder(sort);
        Channel chan = getChannel(channel);
        if (guideDay != null) {
            if (start != null || stop != null) {
                throw new IllegalArgumentException("Specify either " + GUIDE_DAY + " _or_ " + START + "/" + STOP);
            }
            start = guideDayStart(guideDay);
            stop = guideDayStop(guideDay);
        }
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
    @ApiResponses(value = {@ApiResponse(code = 400, message = CLIENT_ERROR), @ApiResponse(code = 404, message = NOT_FOUND), @ApiResponse(code = 500, message = SERVER_ERROR)})
    @Trace(dispatcher = true)
    @GET
    public ApiScheduleEvent nowForChannel(
        @ApiParam(required = true, defaultValue = "NED1") @PathParam(CHANNEL) String channel,
        @ApiParam(value = MESSAGE_PROPERTIES, required = false) @QueryParam(PROPERTIES) @DefaultValue(NONE) String properties
    ) {
        Date now = new Date();
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
    @ApiResponses(value = {@ApiResponse(code = 400, message = CLIENT_ERROR), @ApiResponse(code = 404, message = NOT_FOUND), @ApiResponse(code = 500, message = SERVER_ERROR)})
    @Trace(dispatcher = true)
    public ApiScheduleEvent nextForChannel(
        @ApiParam(required = true, defaultValue = "NED1") @PathParam(CHANNEL) String channel,
        @ApiParam(value = MESSAGE_PROPERTIES, required = false) @QueryParam(PROPERTIES) @DefaultValue(NONE) String properties
    ) {
        Date now = new Date();
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
    @ApiResponses(value = {@ApiResponse(code = 400, message = CLIENT_ERROR), @ApiResponse(code = 404, message = NOT_FOUND), @ApiResponse(code = 500, message = SERVER_ERROR)})
    @Trace(dispatcher = true)
    public ScheduleResult listNet(
        @ApiParam(required = true, defaultValue = "ZAPP") @PathParam(NET) String net,
        @ApiParam(value = MESSAGE_GUIDE_DAY, required = false) @QueryParam(GUIDE_DAY) @DateFormat(YEAR_MONTH_DATE) Date guideDay,
        @ApiParam(value = MESSAGE_START, required = false) @QueryParam(START) @ISO8601Format Date start,
        @ApiParam(value = MESSAGE_STOP, required = false) @QueryParam(STOP) @ISO8601Format Date stop,
        @ApiParam(value = MESSAGE_PROPERTIES, required = false) @QueryParam(PROPERTIES) @DefaultValue(NONE) String properties,
        @QueryParam(SORT) @DefaultValue(ASC) String sort,
        @QueryParam(OFFSET) @DefaultValue(ZERO) Long offset,
        @QueryParam(MAX) @DefaultValue(Constants.DEFAULT_MAX_RESULTS_STRING) Integer max
    ) {
        max = handleTooManyResults(max);
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
    @ApiResponses(value = {@ApiResponse(code = 400, message = CLIENT_ERROR), @ApiResponse(code = 404, message = NOT_FOUND), @ApiResponse(code = 500, message = SERVER_ERROR)})
    @Trace(dispatcher = true)
    public ApiScheduleEvent nowForNet(
        @ApiParam(required = true, defaultValue = "ZAPP") @PathParam(NET) String net,
        @ApiParam(value = MESSAGE_PROPERTIES, required = false) @QueryParam(PROPERTIES) @DefaultValue(NONE) String properties
    ) {
        Date now = new Date();
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
    @ApiResponses(value = {@ApiResponse(code = 400, message = CLIENT_ERROR), @ApiResponse(code = 404, message = NOT_FOUND), @ApiResponse(code = 500, message = SERVER_ERROR)})
    @Trace(dispatcher = true)
    public ApiScheduleEvent nextForNet(
        @ApiParam(required = true, defaultValue = "ZAPP") @PathParam(NET) String net,
        @ApiParam(value = MESSAGE_PROPERTIES, required = false) @QueryParam(PROPERTIES) @DefaultValue(NONE) String properties
    ) {
        Date now = new Date();
        List<? extends ApiScheduleEvent> scheduleEvents = scheduleService.list(new Net(net), now, null, Order.ASC, 0L, 1).getItems();

        if (scheduleEvents.isEmpty()) {
            throw Exceptions.notFound("No next event on net " + net);
        }

        ApiScheduleEvent scheduleEvent = scheduleEvents.get(0);

        ApiMediaFilter.set(properties);

        return scheduleEvent;
    }

    private static Channel getChannel(String channel) {
        // todo (poms 3.2)
        // return Channel.getByEnumValue(channel)
        //return Channel.valuesOf(Arrays.asList(channel)).get(0);
        return Channel.valueOf(channel);
    }

    private static Date guideDayStart(Date guideDay) {
        DateTimeZone timeZone = DateTimeZone.forID("Europe/Amsterdam");
        DateTime dateTime = new DateTime(guideDay, timeZone);
        return dateTime.withHourOfDay(6).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0).toDate();
    }

    private static Date guideDayStop(Date guideDay) {
        DateTimeZone timeZone = DateTimeZone.forID("Europe/Amsterdam");
        DateTime dateTime = new DateTime(guideDay, timeZone);
        return dateTime.withHourOfDay(6).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0).plusDays(1).toDate();
    }

    private Order parseOrder(String order) {
        try {
            return Order.valueOf(order.toUpperCase());
        } catch(IllegalArgumentException e) {
            throw Exceptions.badRequest("Invalid order \"{}\"", order);
        }
    }

    private boolean isActiveEvent(ScheduleEvent scheduleEvent, Date time) {
        if (scheduleEvent == null || time == null) {
            return false;
        }

        long start = scheduleEvent.getStart().getTime();
        long end = start + scheduleEvent.getDuration().getTime();
        long timestamp = time.getTime();

        return start <= timestamp && timestamp <= end;
    }
}

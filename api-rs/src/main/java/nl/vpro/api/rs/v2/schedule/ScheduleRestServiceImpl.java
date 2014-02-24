/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2.schedule;

import com.wordnik.swagger.annotations.*;
import nl.vpro.api.rs.v2.exception.Exceptions;
import nl.vpro.api.rs.v2.filter.ApiMediaFilter;
import nl.vpro.domain.api.Constants;
import nl.vpro.domain.api.Order;
import nl.vpro.domain.api.ScheduleResult;
import nl.vpro.domain.api.ScheduleSearchResult;
import nl.vpro.domain.api.media.MediaForm;
import nl.vpro.domain.api.media.ScheduleService;
import nl.vpro.domain.media.Channel;
import nl.vpro.domain.media.Net;
import nl.vpro.swagger.SwaggerApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import java.util.Date;

/**
 * User: rico
 * Date: 06/02/2014
 */
@Service
@Api(value = ScheduleRestService.PATH, position = 0)
public class ScheduleRestServiceImpl implements ScheduleRestService {

    private static final String DEFAULT_FORM = "{\n" +
            "    \"searches\" : {\n" +
            "        \"text\" : {\n" +
            "                \"value\" : \"Argos\"\n" +
            "        }\n" +
            "    }\n" +
            "}";

    private final ScheduleService scheduleService;

    @Value("${api.media.expose}")
    private boolean expose;

    @Autowired
    public ScheduleRestServiceImpl(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PostConstruct
    private void init() {
        if (expose) {
            SwaggerApplication.inject(this);
        }
    }

    @ApiOperation(httpMethod = "get",
            value = "List scheduled media",
            notes = "",
            position = 0
    )
    @ApiResponses(value = {@ApiResponse(code = 400, message = "Bad request"), @ApiResponse(code = 500, message = "Server error")})
    public ScheduleResult list(
            @ApiParam(value = "Channel name e.g NED1", required = false) @QueryParam("channel") String channel,
            @ApiParam(value = "Net name e.g. ZAPP", required = false)  @QueryParam("net") String net,
            @ApiParam(value = "Guide day in simple ISO8601 format, ie 2014-02-27", required = false) @QueryParam("guideDay") Date guideDay,
            @ApiParam(value = "Start time in full ISO8601 format, ie 2014-02-27T07:06:00Z", required = false) @QueryParam("start") Date start,
            @ApiParam(value = "Stop time in full ISO8601 format, ie 2014-02-28T22:06:00Z", required = false) @QueryParam("stop") Date stop,
            @ApiParam(value = "Optimise media result for these returned properties", required = false) @QueryParam("properties") String properties,
            @QueryParam("sort") @DefaultValue("asc") String sort,
            @QueryParam("offset") @DefaultValue("0") Long offset,
            @QueryParam("max") @DefaultValue(Constants.DEFAULT_MAX_RESULTS_STRING) Integer max
    ) {
        Channel chan = null;
        Net thenet = null;
        if (channel!=null) {
            chan = Channel.valueOf(channel);
        } else if (net!=null) {
            thenet = new Net(net);
        }

        Order order = parseOrder(sort);
        ScheduleResult result = scheduleService.list(chan, thenet, guideDay, start, stop, order, offset, max);

        ApiMediaFilter.set(properties);

        return result;
    }

    @ApiOperation(httpMethod = "post",
            value = "Find scheduled media",
            notes = "",
            position = 1
    )
    @ApiResponses(value = {@ApiResponse(code = 400, message = "Bad request"), @ApiResponse(code = 500, message = "Server error")})
    public ScheduleSearchResult find(
            @ApiParam(value = "Search form", required = false, defaultValue = DEFAULT_FORM) MediaForm form,
            @QueryParam("profile") String profile,
            @ApiParam(value = "Optimise media result for these returned properties", required = false) @QueryParam("properties") String properties,
            @QueryParam("offset") @DefaultValue("0") Long offset,
            @QueryParam("max") @DefaultValue(Constants.DEFAULT_MAX_RESULTS_STRING) Integer max
    ) {
        ScheduleSearchResult result =  scheduleService.find(form, profile, offset, max);

        ApiMediaFilter.set(properties);

        return result;
    }

    private Order parseOrder(String order) {
        try {
            return Order.valueOf(order.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new Exceptions().badRequest("Invalid order \"{}\"", order);
        }
    }

}
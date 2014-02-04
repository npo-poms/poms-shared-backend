/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2.statistics;

import com.wordnik.swagger.annotations.*;
import nl.vpro.api.rs.v2.Responses;
import nl.vpro.domain.api.Constants;
import nl.vpro.domain.api.MediaResult;
import nl.vpro.domain.api.transfer.ResultFilter;
import nl.vpro.domain.statistic.StatsService;
import nl.vpro.domain.statistic.media.PlayService;
import nl.vpro.swagger.SwaggerApplication;
import nl.vpro.transfer.media.PropertySelection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

/**
 * @author Roelof Jan Koekoek
 * @since 2.1
 */
@Service
@Api(value = StatsRestService.PATH, position = 4)
public class StatsRestServiceImpl implements StatsRestService {

    @Value("${api.stats.expose}")
    private boolean expose;

    private final PlayService playService;

    @Autowired
    public StatsRestServiceImpl(PlayService playService) {
        this.playService = playService;
    }

    @PostConstruct
    private void init() {
        if(expose) {
            SwaggerApplication.inject(this);
        }
    }

    @GET
    @Path("/media")
    @ApiOperation(httpMethod = "get",
        value = "Top media overall",
        notes = "High score list containing the most played media")
    @ApiResponses(value = {@ApiResponse(code = 400, message = "Bad request"), @ApiResponse(code = 500, message = "Server error")})
    @Override
    public Response stats(
        @ApiParam(value = "On of hour/day/week") @QueryParam("period") @DefaultValue("hour") String periodString,
        @ApiParam(value = "Optimise media result for these returned properties <a href=\"#!/media/load_get_0\">See Media API</a>", required = false) @QueryParam("properties") String properties,
        @ApiParam @QueryParam("offset") @DefaultValue("0") Long offset,
        @ApiParam @QueryParam("max") @DefaultValue(Constants.MAX_RESULTS_STRING) Integer max
    ) {
        StatsService.Period period;
        try {
            period = StatsService.Period.valueOf(periodString);
        } catch(IllegalArgumentException e) {
            return Responses.clientError("Can't parse period '{}'", periodString);
        }

        MediaResult result = playService.mediaScoreOverall(period, offset, max);
        return filteredMediaResult(result, properties);
    }

    @GET
    @Path("/media/broadcasters/{broadcaster}")
    @ApiOperation(httpMethod = "get",
        value = "Top media per broadcaster",
        notes = "High score list containing the most played media per broadcaster")
    @ApiResponses(value = {@ApiResponse(code = 400, message = "Bad request"), @ApiResponse(code = 500, message = "Server error")})
    @Override
    public Response statsPerBroadcaster(
        @ApiParam(required = true, defaultValue = "VPRO") @PathParam("broadcaster") String broadcaster,
        @ApiParam(value = "On of hour/day/week") @QueryParam("period") @DefaultValue("hour") String periodString,
        @ApiParam(value = "Optimise media result for these returned properties <a href=\"#!/media/load_get_0\">See Media API</a>", required = false) @QueryParam("properties") String properties,
        @ApiParam @QueryParam("offset") @DefaultValue("0") Long offset,
        @ApiParam @QueryParam("max") @DefaultValue(Constants.MAX_RESULTS_STRING) Integer max
    ) {
        StatsService.Period period;
        try {
            period = StatsService.Period.valueOf(periodString);
        } catch(IllegalArgumentException e) {
            return Responses.clientError("Can't parse period '{}'");
        }

        MediaResult result = playService.mediaScorePerBroadcaster(broadcaster, period, offset, max);
        return filteredMediaResult(result, properties);
    }

    @GET
    @Path("/media/portals/{portal}")
    @ApiOperation(httpMethod = "get",
        value = "Top media per portal",
        notes = "High score list containing the most played media per portal")
    @ApiResponses(value = {@ApiResponse(code = 400, message = "Bad request"), @ApiResponse(code = 500, message = "Server error")})
    @Override
    public Response statsPerPortal(
        @ApiParam(required = true, defaultValue = "VproNL") @PathParam("portal") String portal,
        @ApiParam(value = "On of hour/day/week") @QueryParam("period") @DefaultValue("hour") String periodString,
        @ApiParam(value = "Optimise media result for these returned properties <a href=\"#!/media/load_get_0\">See Media API</a>", required = false) @QueryParam("properties") String properties,
        @ApiParam @QueryParam("offset") @DefaultValue("0") Long offset,
        @ApiParam @QueryParam("max") @DefaultValue(Constants.MAX_RESULTS_STRING) Integer max
    ) {
        StatsService.Period period;
        try {
            period = StatsService.Period.valueOf(periodString);
        } catch(IllegalArgumentException e) {
            return Responses.clientError("Can't parse period '{}'");
        }

        MediaResult result = playService.mediaScorePerPortal(portal, period, offset, max);
        return filteredMediaResult(result, properties);
    }


    private Response filteredMediaResult(MediaResult result, String properties) {
        if(properties == null) {
            return Response.ok(result).build();
        }

        return Response.ok(ResultFilter.filter(result, new PropertySelection(properties))).build();
    }
}

/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2.schedule;

import com.wordnik.swagger.annotations.ApiParam;
import nl.vpro.domain.api.Constants;
import nl.vpro.domain.api.ScheduleResult;
import nl.vpro.domain.api.ScheduleSearchResult;
import nl.vpro.domain.api.media.MediaForm;
import nl.vpro.domain.media.Net;
import nl.vpro.resteasy.DateFormat;
import nl.vpro.resteasy.ISO8601Format;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Date;

/**
 * Endpoint which facilitates RPC like requests on scheduled content. This API intents to capture meaningful and frequent
 * queries on scheduled media used when building a site or apps containing POMS media. This not a real REST API. It has no update
 * statements and it is mainly document oriented. Most calls will return a full media document and there are no separate
 * calls for sub-resources.
 * <p/>
 * The API returns three media instance pageTypes: Programs, Groups and Segments.
 * <p/>
 * Media id's may be either a full urn or a mid. Retrieval by crid is not implemented at this moment.
 *
 * @author Rico Jansen
 * @since 3.0
 */
@Path(ScheduleRestService.PATH)
@Produces({MediaType.APPLICATION_JSON + "; charset=utf-8", MediaType.APPLICATION_XML + "; charset=utf-8"})
public interface ScheduleRestService {
    public static final String PATH = "/schedule";

    @GET
    public ScheduleResult list(
            @QueryParam("channel") String channel,
            @QueryParam("net") Net net,
            @ApiParam(value = "Guide day in simple ISO8601 format, ie 2014-02-27", required = false) @QueryParam("guideDay") @DateFormat("yyyy-MM-dd") Date guideDay,
            @ApiParam(value = "Start time in full ISO8601 format, ie 2014-02-27T07:06:00Z", required = false) @QueryParam("start") @ISO8601Format Date start,
            @ApiParam(value = "Stop time in full ISO8601 format, ie 2014-02-28T22:06:00Z", required = false) @QueryParam("stop") @ISO8601Format Date stop,
            @ApiParam(value = "Optimise media result for these returned properties", required = false) @QueryParam("properties") String properties,
            @QueryParam("sort") @DefaultValue("asc") String sort,
            @QueryParam("offset") @DefaultValue("0") Long offset,
            @QueryParam("max") @DefaultValue(Constants.DEFAULT_MAX_RESULTS_STRING) Integer max
    );

    @POST
    public ScheduleSearchResult find(
            MediaForm form,
            @QueryParam("profile") String profile,
            @ApiParam(value = "Optimise media result for these returned properties", required = false) @QueryParam("properties") String properties,
            @QueryParam("offset") @DefaultValue("0") Long offset,
            @QueryParam("max") @DefaultValue(Constants.DEFAULT_MAX_RESULTS_STRING) Integer max
    );


}

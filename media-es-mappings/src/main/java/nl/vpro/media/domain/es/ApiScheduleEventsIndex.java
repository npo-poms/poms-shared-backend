package nl.vpro.media.domain.es;

import nl.vpro.poms.es.ApiElasticSearchIndex;

/**
 * @author Michiel Meeuwissen
 * @since 5.12
 */
public class ApiScheduleEventsIndex extends ApiElasticSearchIndex {
    public static final String NAME = "scheduleevents";
    public static final ApiScheduleEventsIndex APISCHEDULEEVENTS = new ApiScheduleEventsIndex();

    private ApiScheduleEventsIndex() {
        super(NAME,  "/es7/mapping/scheduleevent.json");
    }


}

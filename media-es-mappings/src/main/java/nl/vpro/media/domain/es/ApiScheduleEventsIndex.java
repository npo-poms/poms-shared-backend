package nl.vpro.media.domain.es;

import nl.vpro.poms.es.AbstractIndex;

/**
 * @author Michiel Meeuwissen
 * @since 5.12
 */
public class ApiScheduleEventsIndex extends AbstractIndex {
    public static final String NAME = "scheduleevents";
    public static final ApiScheduleEventsIndex INSTANCE = new ApiScheduleEventsIndex();

    private ApiScheduleEventsIndex() {
        super(NAME,  "/es7/mapping/scheduleevent.json");
    }


}

package nl.vpro.media.domain.es;

import java.time.Clock;

/**
 * @author Michiel Meeuwissen
 * @since 5.13
 */
public class Common {

    private Common() {

    }

    public static  Clock CLOCK = Clock.systemUTC();


    public static final String ES_PUBLISH_DATE  = "esPublishDate";

    public static final String ES_VECTORIZATION = "semanticVectorization";

    public static final String ES_REASONS       = "republicationReasons";
    public static final String ES_REASONS_TEXT  = "text";
    public static final String ES_REASONS_PUBLISH_DATE  = ES_PUBLISH_DATE;

}

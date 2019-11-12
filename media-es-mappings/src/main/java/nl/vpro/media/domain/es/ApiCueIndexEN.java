package nl.vpro.media.domain.es;

import java.util.Locale;

/**
 * @author Michiel Meeuwissen
 * @since 5.12
 */
public class ApiCueIndexEN extends ApiCueIndex {

    public static ApiCueIndexEN INSTANCE = new ApiCueIndexEN();
      static {
        instances.add(INSTANCE);
    }
    private ApiCueIndexEN() {
        super(Locale.ENGLISH);
    }
}

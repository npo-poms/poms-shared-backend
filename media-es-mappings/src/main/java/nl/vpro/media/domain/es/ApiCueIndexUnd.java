package nl.vpro.media.domain.es;

import nl.vpro.i18n.Locales;

/**
 * @author Michiel Meeuwissen
 * @since 5.12
 */
public class ApiCueIndexUnd extends ApiCueIndex {

    public static ApiCueIndexUnd INSTANCE = new ApiCueIndexUnd();
      static {
        instances.add(INSTANCE);
    }
    private ApiCueIndexUnd() {
        super(Locales.UNDETERMINED);
    }
}

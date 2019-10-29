package nl.vpro.media.domain.es;

import nl.vpro.i18n.Locales;

/**
 * @author Michiel Meeuwissen
 * @since 5.12
 */
public class ApiCueIndexAR extends ApiCueIndex {
    public static ApiCueIndexAR INSTANCE = new ApiCueIndexAR();
    static {
        instances.add(INSTANCE);
    }
    private ApiCueIndexAR() {
        super("subtitles_ar", Locales.ARABIC);
    }
}

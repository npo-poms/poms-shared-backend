package nl.vpro.media.domain.es;

import nl.vpro.i18n.Locales;

/**
 * @author Michiel Meeuwissen
 * @since 5.12
 */
public class ApiCueIndexNL extends ApiCueIndex {

    public static ApiCueIndexNL INSTANCE = new ApiCueIndexNL();
    static {
        instances.add(INSTANCE);
    }
    private ApiCueIndexNL() {
        super(Locales.DUTCH);
    }
}

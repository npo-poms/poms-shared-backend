package nl.vpro.media.domain.es;

import nl.vpro.poms.es.AbstractIndex;

/**
 * @author Michiel Meeuwissen
 * @since 5.12
 */
public class ApiRefsIndex extends AbstractIndex {

    public static String NAME = "apimedia_refs";
    public static final ApiRefsIndex INSTANCE = new ApiRefsIndex();


    public ApiRefsIndex() {
        super(NAME, "es7/mapping/ref.json");
    }
}

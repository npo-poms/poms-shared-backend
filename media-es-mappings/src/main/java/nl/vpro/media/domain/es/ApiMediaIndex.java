package nl.vpro.media.domain.es;

import nl.vpro.poms.es.AbstractIndex;

/**
 * @author Michiel Meeuwissen
 * @since 4.7
 */
public class ApiMediaIndex extends AbstractIndex {

    public static final String NAME = "apimedia";
    public static final ApiMediaIndex INSTANCE = new ApiMediaIndex();

    private ApiMediaIndex() {
        super(NAME,  "/es7/mapping/apimedia.json");
    }

}

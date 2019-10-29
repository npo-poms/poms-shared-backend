package nl.vpro.es;

import nl.vpro.poms.es.AbstractIndex;

/**
 * @author Michiel Meeuwissen
 * @since 5.12
 */
public class ApiQueryIndex extends AbstractIndex {

    public static final String NAME = "apiqueries";
    public static final ApiQueryIndex INSTANCE = new ApiQueryIndex();

    private ApiQueryIndex() {
        super(NAME,  "/es7/mapping/query.json");
    }

}

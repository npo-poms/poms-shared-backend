package nl.vpro.media.domain.es;

import nl.vpro.poms.es.AbstractIndex;

/**
 * @author Michiel Meeuwissen
 * @since 4.8
 */
public class ApiCueIndex extends AbstractIndex {
    public static final String NAME = "subtitles";
    public static final ApiCueIndex INSTANCE = new ApiCueIndex();

    public ApiCueIndex() {
        super(NAME,  "es7/mapping/cue.json");
    }


}

package nl.vpro.media.domain.es;


import lombok.extern.slf4j.Slf4j;

import java.util.*;

import nl.vpro.elasticsearch.ElasticSearchIndex;

import static nl.vpro.i18n.Locales.score;

/**
 * @author Michiel Meeuwissen
 * @since 4.8
 */
@Slf4j
public abstract class ApiCueIndex extends ElasticSearchIndex {

    public static final String NAME = "subtitles_%s";

    protected static List<ApiCueIndex> instances = new ArrayList<>();

    public static List<ApiCueIndex> getInstances() {
        log.debug("Loading classes {}, {}, {}, {}", ApiCueIndexNL.INSTANCE, ApiCueIndexAR.INSTANCE, ApiCueIndexEN.INSTANCE, ApiCueIndexUnd.INSTANCE);
        return Collections.unmodifiableList(instances);
    }
    @lombok.Getter
    private final Locale locale;


    protected ApiCueIndex(Locale locale) {
        super(String.format(NAME, locale.toLanguageTag()),
            "/es7/setting/subtitles_" + locale.toLanguageTag() + ".json",
            "/es7/mapping/cue.json",
            Arrays.asList(String.format(NAME, locale.toLanguageTag()) + "-publish"),
            null
        );
        this.locale = locale;
    }



    public static Optional<ApiCueIndex> forLanguage(Locale locale) {
        ApiCueIndex result = null;
        int score = 0;
        for (ApiCueIndex candidate : getInstances()) {
            int scoreOfCandidate = score(locale, candidate.getLocale());
            if (scoreOfCandidate > score) {
                result = candidate;
                score = scoreOfCandidate;
            }
        }
        return Optional.ofNullable(result);

    }

}

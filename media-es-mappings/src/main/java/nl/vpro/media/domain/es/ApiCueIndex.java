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

    protected static List<ApiCueIndex> instances = new ArrayList<>();

    public static List<ApiCueIndex> getInstances() {
        log.debug("Loading classes {}, {}, {}", ApiCueIndexNL.INSTANCE, ApiCueIndexAR.INSTANCE, ApiCueIndexEN.INSTANCE);
        return Collections.unmodifiableList(instances);
    }
    @lombok.Getter
    private final Locale locale;


    protected ApiCueIndex(String name, Locale locale) {
        super(name,  "/es7/mapping/cue.json");
        this.locale = locale;
    }


    public String getIndexName(String baseName) {
        return baseName + "_" + locale.toLanguageTag();
    }

    public static Optional<ApiCueIndex> forLanguage(Locale locale) {
        ApiCueIndex result = null;
        int score = 0;
        Iterator<ApiCueIndex> iterator = getInstances().iterator();
        while(iterator.hasNext()) {
            ApiCueIndex candidate = iterator.next();
            int scoreOfCandidate = score(locale, candidate.getLocale());
            if (scoreOfCandidate > score) {
                result = candidate;
                score = scoreOfCandidate;
            }
        }
        return Optional.ofNullable(result);

    }

}

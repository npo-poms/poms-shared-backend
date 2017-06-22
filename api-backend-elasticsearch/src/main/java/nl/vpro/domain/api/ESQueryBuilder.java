package nl.vpro.domain.api;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.nl.DutchAnalyzer;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.WordlistLoader;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.Version;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.*;

import nl.vpro.domain.api.media.DurationRangeMatcher;

/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */

@Slf4j
public abstract class ESQueryBuilder {

    private static final float PHRASE_FACTOR = 2.0f;

    private static final float QUOTE_FACTOR = 2.0f;

    private static final int PHRASE_SLOP = 4;

    private static Version LUCENE_VERSION = Version.LUCENE_47;
    private static CharArraySet STOP_WORDS;

    static {
        try {
            STOP_WORDS = CharArraySet.unmodifiableSet(
                CharArraySet.copy(LUCENE_VERSION, WordlistLoader.getSnowballWordSet(IOUtils.getDecodingReader(SnowballFilter.class, DutchAnalyzer.DEFAULT_STOPWORD_FILE, IOUtils.CHARSET_UTF_8), LUCENE_VERSION)));
        } catch (IOException ioe) {

        }
    }

    protected static <MT extends MatchType> BoolQueryBuilder buildTextQuery(AbstractTextMatcher<MT> textSearch, String prefix, List<SearchFieldDefinition> searchFields) {
        final BoolQueryBuilder answer = QueryBuilders.boolQuery();

        //answer(QueryBuilders.hasChildQuery(ApiCueIndex.NAME))

        String textWithoutStopWords = filterStopWords(textSearch.getValue());
        List<String> splitText = split(textSearch.getValue());
        List<String> quoted = splitText.stream().filter(ESQueryBuilder::isQuoted).collect(Collectors.toList());

        // we entirely don't use http://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-query-string-query.html#query-string-syntax
        // So some of the features of that are redone here.

        Fuzziness fuzziness = getFuzziness(textSearch);
        for (SearchFieldDefinition searchField : searchFields) {
            BoolQueryBuilder fieldQuery = QueryBuilders.boolQuery();

            quoted.forEach(entry -> {
                    String unquoted = entry.substring(1, entry.length() - 1);
                    MatchQueryBuilder phraseQuery = phraseQuery(prefix, searchField, unquoted, PHRASE_FACTOR * QUOTE_FACTOR, 0);

                    fieldQuery.should(phraseQuery);
                    if (fuzziness != null) {
                        fieldQuery.should(
                            fuzzy(fuzziness, phraseQuery)
                        );
                    }
                }
            );

            if (quoted.size() < splitText.size()) {
                MatchQueryBuilder matchQuery = QueryBuilders.matchQuery(prefix + searchField.getName(), textWithoutStopWords)
                    .boost(searchField.getBoost())
                    .operator(MatchQueryBuilder.Operator.OR);
                MatchQueryBuilder phraseQuery = phraseQuery(prefix, searchField, textSearch.getValue(), PHRASE_FACTOR, PHRASE_SLOP);

                fieldQuery.should(matchQuery);
                fieldQuery.should(phraseQuery);

                if (fuzziness != null) {
                    fieldQuery.should(
                        fuzzy(fuzziness, matchQuery)
                    );

                    fieldQuery.should(
                        fuzzy(fuzziness, phraseQuery)
                    );
                }
            }

            apply(answer, fieldQuery, Match.SHOULD);
        }

        return answer;
    }

    static MatchQueryBuilder phraseQuery(String prefix, SearchFieldDefinition searchField, String value, float boost, int slop) {
        return QueryBuilders.matchPhraseQuery(prefix + searchField.getName(), value)
            .boost(searchField.getBoost() * boost)
            .operator(MatchQueryBuilder.Operator.OR)
            .slop(slop);
    }

    static Fuzziness getFuzziness(AbstractTextMatcher<?> textMatcher) {
        String fuzziness = textMatcher.getFuzziness();
        if (StringUtils.isBlank(fuzziness)) {
            return null;
        } else {
            return Fuzziness.build(fuzziness);
        }
    }

    static MatchQueryBuilder fuzzy(Fuzziness fuzziness, MatchQueryBuilder queryBuilder) {
        if (fuzziness != null) {
            queryBuilder.fuzziness(fuzziness);
        }
        return queryBuilder;
    }

    // NPA-186
    protected static String filterStopWords(String value) {
        String textWithoutStopWords = value;
        try {
            StringBuilder builder = new StringBuilder();
            TokenStream stream = new StopFilter(LUCENE_VERSION, new StandardTokenizer(LUCENE_VERSION, new StringReader(value)), STOP_WORDS);
            CharTermAttribute termAttribute = stream.getAttribute(CharTermAttribute.class);
            stream.reset();
            while (stream.incrementToken()) {
                if (builder.length() > 0) {
                    builder.append(" ");
                }
                builder.append(termAttribute.toString());
            }
            if (builder.length() > 0) {
                textWithoutStopWords = builder.toString();
            }
        } catch (IOException ioe) {
            log.error(ioe.getMessage());
        }
        return textWithoutStopWords;
    }

    private static final Set<Character> QUOTES = new HashSet<>(Arrays.asList('\'', '\"'));

    protected static List<String> split(String value) {
        List<String> result = new ArrayList<>();
        value = value.trim();
        int start = 0;
        Character quote = null;
        boolean spacing = true;
        for (int i = 0; i < value.length(); i++) {
            char currentChar = value.charAt(i);
            if (QUOTES.contains(currentChar)) {
                if (quote == null) {
                    quote = currentChar;
                } else {
                    if (quote == currentChar) {
                        quote = null;
                    }
                }
            }
            boolean isSpace = Character.isSpaceChar(currentChar);
            if (isSpace) {
                if (quote == null && !spacing) {
                    if (i > start) {
                        result.add(value.substring(start, i));
                    }
                }
            } else {
                if (spacing) {
                    start = i;
                }
            }
            spacing = quote == null && isSpace;


        }
        result.add(value.substring(start, value.length()));
        return result;
    }

    protected static boolean isQuoted(String value) {
        char charAtStart = value.charAt(0);
        return QUOTES.contains(charAtStart) && charAtStart == value.charAt(value.length() - 1);
    }

    public static void apply(BoolQueryBuilder answer, QueryBuilder subQuery, Match match) {
        switch (match) {
            case SHOULD:
                answer.should(subQuery);
                break;
            case MUST:
                answer.must(subQuery);
                break;
            case NOT:
                answer.mustNot(subQuery);
                break;
        }
    }


    public interface FieldApplier {
        void applyField(BoolQueryBuilder booleanQueryBuilder, AbstractTextMatcher matcher);

        void applyField(BoolQueryBuilder booleanQueryBuilder, DateRangeMatcher matcher);

        void applyField(BoolQueryBuilder booleanQueryBuilder, DurationRangeMatcher matcher);

    }

    public static class SingleFieldApplier implements FieldApplier {
        private final String fieldName;

        public SingleFieldApplier(String fieldName) {
            this.fieldName = fieldName;
        }

        @Override
        public void applyField(BoolQueryBuilder booleanQueryBuilder, AbstractTextMatcher matcher) {
            QueryBuilder typeQuery = buildQuery(fieldName, matcher);
            apply(booleanQueryBuilder, typeQuery, matcher.getMatch());
        }

        @Override
        public void applyField(BoolQueryBuilder booleanQueryBuilder, DateRangeMatcher matcher) {
            QueryBuilder typeQuery = buildQuery(fieldName, matcher);
            apply(booleanQueryBuilder, typeQuery, matcher.getMatch());
        }

        @Override
        public void applyField(BoolQueryBuilder booleanQueryBuilder, DurationRangeMatcher matcher) {
            QueryBuilder typeQuery = buildQuery(fieldName, matcher);
            apply(booleanQueryBuilder, typeQuery, matcher.getMatch());
        }
    }

    public static class MultipleFieldsApplier implements FieldApplier {
        private final String[] fieldNames;

        public MultipleFieldsApplier(String... fieldNames) {
            this.fieldNames = fieldNames;
        }

        @Override
        public void applyField(BoolQueryBuilder booleanQueryBuilder, AbstractTextMatcher matcher) {
            BoolQueryBuilder bool = QueryBuilders.boolQuery();
            for (String fieldName : fieldNames) {
                QueryBuilder extensionQuery = buildQuery(fieldName, matcher);
                bool.should(extensionQuery);
            }
            apply(booleanQueryBuilder, bool, matcher.getMatch());

        }

        @Override
        public void applyField(BoolQueryBuilder booleanQueryBuilder, DateRangeMatcher matcher) {
            BoolQueryBuilder bool = QueryBuilders.boolQuery();
            for (String fieldName : fieldNames) {
                QueryBuilder extensionQuery = buildQuery(fieldName, matcher);
                bool.should(extensionQuery);
            }
            apply(booleanQueryBuilder, bool, matcher.getMatch());
        }

        @Override
        public void applyField(BoolQueryBuilder booleanQueryBuilder, DurationRangeMatcher matcher) {
            BoolQueryBuilder bool = QueryBuilders.boolQuery();
            for (String fieldName : fieldNames) {
                QueryBuilder extensionQuery = buildQuery(fieldName, matcher);
                bool.should(extensionQuery);
            }
            apply(booleanQueryBuilder, bool, matcher.getMatch());
        }
    }

    protected static <T extends MatchType> void build(BoolQueryBuilder booleanQueryBuilder, AbstractTextMatcherList<? extends AbstractTextMatcher<T>, T> textMatchers, FieldApplier applier) {
        if (textMatchers != null) {
            BoolQueryBuilder sub = QueryBuilders.boolQuery();
            for (AbstractTextMatcher matcher : textMatchers.asList()) {
                applier.applyField(sub, matcher);
            }
            apply(booleanQueryBuilder, sub, textMatchers.getMatch());
        }
    }

    protected static void build(BoolQueryBuilder booleanQuery, DateRangeMatcherList rangeMatchers, FieldApplier applier) {
        if (rangeMatchers != null) {
            BoolQueryBuilder sub = QueryBuilders.boolQuery();

            for (DateRangeMatcher rangeMatcher : rangeMatchers) {
                applier.applyField(sub, rangeMatcher);
            }
            apply(booleanQuery, sub, rangeMatchers.getMatch());
        }
    }

    protected static void build(BoolQueryBuilder booleanQuery, DurationRangeMatcherList rangeMatchers, FieldApplier applier) {
        if (rangeMatchers != null) {
            BoolQueryBuilder sub = QueryBuilders.boolQuery();

            for (DurationRangeMatcher rangeMatcher : rangeMatchers) {
                applier.applyField(sub, rangeMatcher);
            }
            apply(booleanQuery, sub, rangeMatchers.getMatch());
        }
    }

    protected static void nested(String path, BoolQueryBuilder booleanQueryBuilder, TextMatcherList textMatchers, FieldApplier applier) {
        if (textMatchers != null) {
            BoolQueryBuilder query = QueryBuilders.boolQuery();
            for (TextMatcher matcher : textMatchers) {
                applier.applyField(query, matcher);
            }

            QueryBuilder nested = QueryBuilders.nestedQuery(path, query);
            apply(booleanQueryBuilder, nested, textMatchers.getMatch());
        }
    }

    public static QueryBuilder buildQuery(String fieldName, AbstractTextMatcher matcher) {
        String value = matcher.getValue();

        ESMatchType matchType = ESMatchType.valueOf(matcher.getMatchType().getName());

        return matchType.getQueryBuilder(fieldName, value, matcher.isCaseSensitive());
    }


    /**
     * Builds a relation relationQuery for standalone or embedded media media when the prefix argument is left blank.
     *
     * @param prefix not null path to the media node in the documents to search, including the last dot, can be blank
     */
    public static QueryBuilder relationQuery(AbstractRelationSearch relationSearch, @NotNull BoolQueryBuilder booleanQuery, @NotNull String prefix) {
        if (relationSearch == null) {
            return booleanQuery;
        }

        BoolQueryBuilder fieldWrapper = QueryBuilders.boolQuery();

        build(fieldWrapper, relationSearch.getTypes(), new SingleFieldApplier(prefix + "relations.type"));
        build(fieldWrapper, relationSearch.getBroadcasters(), new SingleFieldApplier(prefix + "relations.broadcaster"));
        build(fieldWrapper, relationSearch.getValues(), new SingleFieldApplier(prefix + "relations.value"));
        build(fieldWrapper, relationSearch.getUriRefs(), new SingleFieldApplier(prefix + "relations.uriRef"));

        NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery(prefix + "relations", fieldWrapper);

        apply(booleanQuery, nestedQuery, relationSearch.getMatch());

        return booleanQuery;
    }

    public static QueryBuilder buildQuery(String fieldName, DateRangeMatcher matcher) {
        RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery(fieldName);

        rangeQuery.includeLower(matcher.includeBegin());
        rangeQuery.includeUpper(matcher.includeEnd());

        if (matcher.getBegin() != null) {
            rangeQuery.from(matcher.getBegin().toEpochMilli());
        }

        if (matcher.getEnd() != null) {
            rangeQuery.to(matcher.getEnd().toEpochMilli());
        }

        return rangeQuery;
    }

    public static QueryBuilder buildQuery(String fieldName, DurationRangeMatcher matcher) {
        RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery(fieldName);

        rangeQuery.includeLower(matcher.includeBegin());
        rangeQuery.includeUpper(matcher.includeEnd());

        if (matcher.getBegin() != null) {
            rangeQuery.from(matcher.getBegin().toMillis());
        }

        if (matcher.getEnd() != null) {
            rangeQuery.to(matcher.getEnd().toMillis());
        }

        return rangeQuery;
    }
}

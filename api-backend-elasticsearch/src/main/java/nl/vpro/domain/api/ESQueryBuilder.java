package nl.vpro.domain.api;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.text.Collator;
import java.util.*;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.WordlistLoader;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.nl.DutchAnalyzer;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.collation.CollationAttributeFactory;
import org.apache.lucene.search.join.ScoreMode;
import org.apache.lucene.util.IOUtils;
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

    private static CharArraySet STOP_WORDS;

    static {
        try {
            STOP_WORDS = CharArraySet.unmodifiableSet(CharArraySet.copy(WordlistLoader.getSnowballWordSet(IOUtils.getDecodingReader(SnowballFilter.class, DutchAnalyzer.DEFAULT_STOPWORD_FILE, Charset.forName("UTF-8")))));
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
                    MatchPhraseQueryBuilder phraseQuery = phraseQuery(prefix, searchField, unquoted, PHRASE_FACTOR * QUOTE_FACTOR, 0);

                    fieldQuery.should(phraseQuery);
                    if (fuzziness != null) {
                        fieldQuery.should(
                            sloppy(fuzziness, phraseQuery)
                        );
                    }
                }
            );

            if (quoted.size() < splitText.size()) {
                MatchQueryBuilder matchQuery = QueryBuilders.matchQuery(prefix + searchField.getName(), textWithoutStopWords)
                    .boost(searchField.getBoost())
                    .operator(Operator.OR);
                MatchPhraseQueryBuilder phraseQuery = phraseQuery(prefix, searchField, textSearch.getValue(), PHRASE_FACTOR, PHRASE_SLOP);

                fieldQuery.should(matchQuery);
                fieldQuery.should(phraseQuery);

                if (fuzziness != null) {
                    fieldQuery.should(
                        fuzzy(fuzziness, matchQuery));

                    fieldQuery.should(
                        sloppy(fuzziness, phraseQuery)
                    );
                }
            }

            apply(answer, fieldQuery, Match.SHOULD);
        }

        return answer;
    }

    static MatchPhraseQueryBuilder phraseQuery(String prefix, SearchFieldDefinition searchField, String value, float boost, int slop) {
        return QueryBuilders.matchPhraseQuery(prefix + searchField.getName(), value)
            .boost(searchField.getBoost() * boost)
            //.(Operator.OR)
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

    //Doesn't do anything (yet)
    static MatchPhraseQueryBuilder sloppy(Fuzziness fuzziness, MatchPhraseQueryBuilder queryBuilder) {
        if (fuzziness == null) {
            queryBuilder.slop(1);
        }
        return queryBuilder;
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

            WhitespaceTokenizer whitespace = new WhitespaceTokenizer(new CollationAttributeFactory(Collator.getInstance()));
            whitespace.setReader(new StringReader(value));
            StopFilter stream = new StopFilter(whitespace, STOP_WORDS);
            stream.reset();
            CharTermAttribute termAttribute = stream.getAttribute(CharTermAttribute.class);
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
        } catch (IllegalStateException ise) {
            // TODO
            log.error(ise.getMessage());
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


    public interface FieldApplier<M extends Matcher> {
        void applyField(BoolQueryBuilder booleanQueryBuilder, M matcher);

    }


    protected static abstract class SingleFieldApplier<M extends Matcher> implements FieldApplier<M> {
        protected final String fieldName;

        public SingleFieldApplier(String fieldName) {
            this.fieldName = fieldName;
        }
    }

    public static class ExtendedTextSingleFieldApplier extends SingleFieldApplier<ExtendedTextMatcher> {
        public ExtendedTextSingleFieldApplier(String fieldName) {
            super(fieldName);
        }

        @Override
        public void applyField(BoolQueryBuilder booleanQueryBuilder, ExtendedTextMatcher matcher) {

            QueryBuilder typeQuery = buildQuery(matcher.isCaseSensitive() ? fieldName + ".full" : fieldName + ".lower", matcher, ESMatchType.FieldInfo.TEXT);
            apply(booleanQueryBuilder, typeQuery, matcher.getMatch());
        }

    }


    public static class TextSingleFieldApplier<MT extends MatchType, TM extends AbstractTextMatcher<MT>> extends SingleFieldApplier<TM> {
        final ESMatchType.FieldInfo fieldInfo;

        public TextSingleFieldApplier(String fieldName) {
            super(fieldName);
            fieldInfo = ESMatchType.FieldInfo.TEXT;
        }

        public TextSingleFieldApplier(String fieldName, ESMatchType.FieldInfo fieldInfo) {
            super(fieldName);
            this.fieldInfo = fieldInfo;
        }

        @Override
        public void applyField(BoolQueryBuilder booleanQueryBuilder, TM matcher) {
            QueryBuilder typeQuery = buildQuery(fieldName, matcher, fieldInfo);
            apply(booleanQueryBuilder, typeQuery, matcher.getMatch());
        }
    }



    public static class DateSingleFieldApplier extends SingleFieldApplier<DateRangeMatcher> {
        public DateSingleFieldApplier(String fieldName) {
            super(fieldName);
        }

        @Override
        public void applyField(BoolQueryBuilder booleanQueryBuilder, DateRangeMatcher matcher) {
            QueryBuilder typeQuery = buildQuery(fieldName, matcher);
            apply(booleanQueryBuilder, typeQuery, matcher.getMatch());
        }

    }

    public static class DurationSingleFieldApplier extends SingleFieldApplier<DurationRangeMatcher> {
        public DurationSingleFieldApplier(String fieldName) {
            super(fieldName);
        }

        @Override
        public void applyField(BoolQueryBuilder booleanQueryBuilder, DurationRangeMatcher matcher) {
            QueryBuilder typeQuery = buildQuery(fieldName, matcher);
            apply(booleanQueryBuilder, typeQuery, matcher.getMatch());
        }

    }

    public static abstract class AbstractMultipleFieldsApplier<M extends Matcher> implements FieldApplier<M> {
        protected final String[] fieldNames;

        protected AbstractMultipleFieldsApplier(String[] fieldNames) {
            this.fieldNames = fieldNames;
        }
    }

    public static class TextMultipleFieldsApplier<MT extends MatchType, TM extends AbstractTextMatcher<MT>> extends AbstractMultipleFieldsApplier<TM> {

        public TextMultipleFieldsApplier(String... fieldNames) {
            super(fieldNames);
        }

        @Override
        public void applyField(BoolQueryBuilder booleanQueryBuilder, TM matcher) {
            BoolQueryBuilder bool = QueryBuilders.boolQuery();
            for (String fieldName : fieldNames) {
                QueryBuilder extensionQuery = buildQuery(fieldName, matcher, ESMatchType.FieldInfo.TEXT);
                bool.should(extensionQuery);
            }
            apply(booleanQueryBuilder, bool, matcher.getMatch());

        }

    }

    public static class DateMultipleFieldsApplier extends AbstractMultipleFieldsApplier<DateRangeMatcher> {

        public DateMultipleFieldsApplier(String[] fieldNames) {
            super(fieldNames);
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
    }

    protected static <MT extends MatchType, TM extends AbstractTextMatcher<MT>, TML extends AbstractTextMatcherList<TM, MT>>
    void buildFromList(BoolQueryBuilder booleanQueryBuilder, TML textMatchers,
                        FieldApplier<TM> applier) {
        if (textMatchers != null) {
            BoolQueryBuilder sub = QueryBuilders.boolQuery();
            for (TM matcher : textMatchers.asList()) {
                applier.applyField(sub, matcher);
            }
            apply(booleanQueryBuilder, sub, textMatchers.getMatch());
        }
    }

    protected static void buildFromList(BoolQueryBuilder booleanQuery, DateRangeMatcherList rangeMatchers,
                                        FieldApplier<DateRangeMatcher> applier) {
        if (rangeMatchers != null) {
            BoolQueryBuilder sub = QueryBuilders.boolQuery();

            for (DateRangeMatcher rangeMatcher : rangeMatchers) {
                applier.applyField(sub, rangeMatcher);
            }
            apply(booleanQuery, sub, rangeMatchers.getMatch());
        }
    }


    protected static void buildFromList(BoolQueryBuilder booleanQuery, DurationRangeMatcherList rangeMatchers,
                                        FieldApplier<DurationRangeMatcher> applier) {
        if (rangeMatchers != null) {
            BoolQueryBuilder sub = QueryBuilders.boolQuery();

            for (DurationRangeMatcher rangeMatcher : rangeMatchers) {
                applier.applyField(sub, rangeMatcher);
            }
            apply(booleanQuery, sub, rangeMatchers.getMatch());
        }
    }

    protected static void nested(String path, BoolQueryBuilder booleanQueryBuilder, TextMatcherList textMatchers, FieldApplier<TextMatcher> applier) {
        if (textMatchers != null) {
            BoolQueryBuilder query = QueryBuilders.boolQuery();
            for (TextMatcher matcher : textMatchers) {
                applier.applyField(query, matcher);
            }

            QueryBuilder nested = QueryBuilders.nestedQuery(path, query, ScoreMode.Avg);
            apply(booleanQueryBuilder, nested, textMatchers.getMatch());
        }
    }


    protected static void build(BoolQueryBuilder booleanQuery, ExtendedTextMatcher textMatcher, ExtendedTextSingleFieldApplier applier) {
        if (textMatcher!= null) {
            applier.applyField(booleanQuery, textMatcher);
        }
    }

    protected static void build(BoolQueryBuilder booleanQuery, SimpleTextMatcher textMatcher, FieldApplier<SimpleTextMatcher> applier) {
        if (textMatcher != null) {
            applier.applyField(booleanQuery, textMatcher);
        }
    }



    public static <MT extends MatchType, TM extends AbstractTextMatcher<MT>> QueryBuilder buildQuery(String fieldName, TM  matcher, ESMatchType.FieldInfo fieldInfo) {
        String value = matcher.getValue();
        ESMatchType matchType = ESMatchType.valueOf(matcher.getMatchType().getName());
        return matchType.getQueryBuilder(fieldName, ESMatchType.esValue(value, matcher.isCaseSensitive()), fieldInfo);
    }


    /**
     * Builds a relation relationQuery for standalone or embedded media media when the prefix argument is left blank.
     *
     * @param prefix not null path to the media node in the documents to search, including the last dot, can be blank
     */
    public static QueryBuilder relationQuery(
        AbstractRelationSearch relationSearch,
        @NotNull BoolQueryBuilder booleanQuery, @NotNull String prefix) {
        if (relationSearch == null) {
            return booleanQuery;
        }

        BoolQueryBuilder fieldWrapper = QueryBuilders.boolQuery();
        buildFromList(fieldWrapper, relationSearch.getTypes(), new TextSingleFieldApplier<>(prefix + "relations.type"));
        buildFromList(fieldWrapper, relationSearch.getBroadcasters(), new TextSingleFieldApplier<>(prefix + "relations.broadcaster"));
        ExtendedTextMatcherList values = relationSearch.getValues();
        //build(fieldWrapper, relationSearch.getValues(), new ExtendedTextSingleFieldApplier(prefix + "relations.value"));
        buildFromList(fieldWrapper, relationSearch.getUriRefs(), new TextSingleFieldApplier<>(prefix + "relations.uriRef"));

        NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery(prefix + "relations", fieldWrapper, ScoreMode.Max);

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

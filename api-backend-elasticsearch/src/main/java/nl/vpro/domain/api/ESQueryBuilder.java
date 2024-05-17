package nl.vpro.domain.api;

import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.text.Collator;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.nl.DutchAnalyzer;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.collation.CollationAttributeFactory;
import org.apache.lucene.search.join.ScoreMode;
import org.apache.lucene.util.IOUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.*;

import nl.vpro.domain.api.media.DurationRangeMatcher;
import nl.vpro.semantic.VectorizationService;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */

@Log4j2
public abstract class ESQueryBuilder {

    static final TextSingleFieldApplier<StandardMatchType, TextMatcher> RELATIONS_APPLIER = new TextSingleFieldApplier<>("relations.broadcaster");
    private static final float PHRASE_FACTOR = 2.0f;

    private static final float QUOTE_FACTOR = 2.0f;

    private static final int PHRASE_SLOP = 4;

    private static final CharArraySet STOP_WORDS;

    static {
        CharArraySet sw;
        try {
            sw = CharArraySet.unmodifiableSet(
                CharArraySet.copy(WordlistLoader.getSnowballWordSet(IOUtils.getDecodingReader(SnowballFilter.class, DutchAnalyzer.DEFAULT_STOPWORD_FILE, StandardCharsets.UTF_8)))
            );
        } catch (IOException ioe) {
            log.warn(ioe.getMessage());
            sw = CharArraySet.unmodifiableSet(new CharArraySet(0, false));
        }
        STOP_WORDS = sw;
    }

    protected static <MT extends MatchType> QueryBuilder buildTextQuery(
        @NonNull String prefix,
        @NonNull AbstractTextMatcher<MT> textSearch,
        @NonNull List<SearchFieldDefinition> searchFields,
        @Nullable VectorizationService vectorizationService) {
        if (textSearch.isSemantic()) {
            if (vectorizationService == null) {
                throw new UnsupportedOperationException("Semantic search not supported");
            }
            // semantic search must be done via scoring only
            return QueryBuilders.matchAllQuery();
            ///return buildSemanticTextQuery(prefix, textSearch, vectorizationService);
        } else {
            return buildNonSemanticTextQuery(prefix, textSearch, searchFields);
        }
    }

    protected static <MT extends MatchType> BoolQueryBuilder buildNonSemanticTextQuery(
        @NonNull String prefix,
        @NonNull AbstractTextMatcher<MT> textSearch,
        @NonNull List<SearchFieldDefinition> searchFields
    ) {
        final BoolQueryBuilder answer = QueryBuilders.boolQuery();

        String textWithoutStopWords = filterStopWords(textSearch.getValue());
        List<String> splitText = split(textSearch.getValue());
        List<String> quoted = splitText.stream().filter(ESQueryBuilder::isQuoted).toList();

        // we entirely don't use http://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-query-string-query.html#query-string-syntax
        // So some of the features of that are redone here.

        Fuzziness fuzziness = getFuzziness(textSearch);
        for (SearchFieldDefinition searchField : searchFields) {
            BoolQueryBuilder fieldQuery = QueryBuilders.boolQuery();
            if (! searchField.isActive()) {
                log.debug("Skipped {} since its boost == 0", searchField);
                continue;
            }
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

    static MatchPhraseQueryBuilder phraseQuery(
        @NonNull String prefix,
        @NonNull SearchFieldDefinition searchField,
        String value, float boost, int slop) {
        return QueryBuilders.matchPhraseQuery(prefix + searchField.getName(), value)
            .boost(searchField.getBoost() * boost)
            //.(Operator.OR)
            .slop(slop);
    }

    static Fuzziness getFuzziness(
        @NonNull AbstractTextMatcher<?> textMatcher) {
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
    protected static String filterStopWords(@NonNull String value) {
        String textWithoutStopWords = value;

        try {
            StringBuilder builder = new StringBuilder();

            WhitespaceTokenizer whitespace = new WhitespaceTokenizer(new CollationAttributeFactory(Collator.getInstance()));
            whitespace.setReader(new StringReader(value));
            StopFilter stream = new StopFilter(whitespace, STOP_WORDS);
            stream.reset();
            CharTermAttribute termAttribute = stream.getAttribute(CharTermAttribute.class);
            while (stream.incrementToken()) {
                if (!builder.isEmpty()) {
                    builder.append(" ");
                }
                builder.append(termAttribute.toString());
            }
            if (!builder.isEmpty()) {
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

    private static final Set<Character> QUOTES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList('\'', '\"')));

    protected static List<String> split(
        @NonNull String value) {
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
        result.add(value.substring(start));
        return result;
    }

    protected static boolean isQuoted(
        @NonNull  String value) {
        char charAtStart = value.charAt(0);
        return QUOTES.contains(charAtStart) && charAtStart == value.charAt(value.length() - 1);
    }

    public static void apply(
        @NonNull BoolQueryBuilder answer,
        @NonNull QueryBuilder subQuery,
        @NonNull Match match) {

        if (match == null) {
            match = Match.MUST;
        }
        switch (match) {
            case SHOULD -> answer.should(subQuery);
            case MUST -> answer.must(subQuery);
            case NOT -> answer.mustNot(subQuery);
        }
    }


    public interface FieldApplier<M extends Matcher<?>> {
        void applyField(String prefix, BoolQueryBuilder booleanQueryBuilder, M matcher);

    }


    protected static abstract class SingleFieldApplier<M extends Matcher<?>> implements FieldApplier<M> {
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
        public void applyField(String prefix, BoolQueryBuilder booleanQueryBuilder, ExtendedTextMatcher matcher) {
            QueryBuilder queryBuilder = buildQuery(prefix, prefix + (matcher.isCaseSensitive() ? fieldName + ".full" : fieldName + ".lower"), matcher, ESMatchType.FieldInfo.TEXT);

            apply(booleanQueryBuilder, queryBuilder, matcher.getMatch());
        }

    }


    public static class TextSingleFieldApplier<MT extends MatchType, TM extends AbstractTextMatcher<MT>>
        extends SingleFieldApplier<TM> {
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
        public void applyField(String prefix, BoolQueryBuilder booleanQueryBuilder, TM matcher) {
            QueryBuilder queryBuilder = buildQuery(prefix, fieldName, matcher, fieldInfo);
            apply(booleanQueryBuilder, queryBuilder, matcher.getMatch());
        }
    }


    public static class DateSingleFieldApplier extends SingleFieldApplier<DateRangeMatcher> {
        public DateSingleFieldApplier(String fieldName) {
            super(fieldName);
        }

        @Override
        public void applyField(String prefix, BoolQueryBuilder booleanQueryBuilder, DateRangeMatcher matcher) {
            QueryBuilder typeQuery = buildQuery(prefix + fieldName, matcher);
            apply(booleanQueryBuilder, typeQuery, matcher.getMatch());
        }

    }

    public static class DurationSingleFieldApplier extends SingleFieldApplier<DurationRangeMatcher> {
        public DurationSingleFieldApplier(String fieldName) {
            super(fieldName);
        }

        @Override
        public void applyField(String prefix, BoolQueryBuilder booleanQueryBuilder, DurationRangeMatcher matcher) {
            QueryBuilder typeQuery = buildQuery(prefix + fieldName, matcher);
            apply(booleanQueryBuilder, typeQuery, matcher.getMatch());
        }

    }


    public static class TextMultipleFieldsApplier<MT extends MatchType, TM extends AbstractTextMatcher<MT>> implements FieldApplier<TM> {

        ESMatchType.FieldInfoWrapper[] fields;

        public TextMultipleFieldsApplier(ESMatchType.FieldInfoWrapper... fieldNames) {
            this.fields = fieldNames;
        }

        public TextMultipleFieldsApplier(String... fieldNames) {
            this.fields = Arrays.stream(fieldNames).map(e -> ESMatchType.FieldInfoWrapper.builder().name(e).fieldInfo(ESMatchType.FieldInfo.TEXT).build()).toArray(ESMatchType.FieldInfoWrapper[]::new);
        }

        @Override
        public void applyField(String prefix, BoolQueryBuilder booleanQueryBuilder, TM matcher) {
            BoolQueryBuilder bool = QueryBuilders.boolQuery();
            List<QueryBuilder> clauses = new ArrayList<>();
            for (ESMatchType.FieldInfoWrapper field : fields) {
                buildOptionalQuery(prefix, field.getName(), matcher, field.getFieldInfo()).ifPresent(queryBuilder -> {
                    bool.should(queryBuilder);
                    clauses.add(queryBuilder);
                });
            }
            if (clauses.isEmpty()) {
                return;
            } else if (clauses.size() == 1) {
                apply(booleanQueryBuilder, clauses.get(0), matcher.getMatch());
            } else {
                apply(booleanQueryBuilder, bool, matcher.getMatch());
            }

        }

    }

    public static class DateMultipleFieldsApplier implements FieldApplier<DateRangeMatcher> {
        String[] fieldNames;

        public DateMultipleFieldsApplier(String[] fieldNames) {
            this.fieldNames = fieldNames;
        }

        @Override
        public void applyField(String prefix, BoolQueryBuilder booleanQueryBuilder, DateRangeMatcher matcher) {
            BoolQueryBuilder bool = QueryBuilders.boolQuery();
            for (String fieldName : fieldNames) {
                QueryBuilder extensionQuery = buildQuery(prefix + fieldName, matcher);
                bool.should(extensionQuery);
            }
            apply(booleanQueryBuilder, bool, matcher.getMatch());
        }
    }

    protected static <MT extends MatchType, TM extends AbstractTextMatcher<MT>, TML extends AbstractTextMatcherList<TM, MT>>
    void buildFromList(
        @NonNull String prefix,
        @NonNull BoolQueryBuilder booleanQueryBuilder,
        @Nullable TML textMatchers,
        @NonNull FieldApplier<TM> applier) {
        if (textMatchers != null) {
            BoolQueryBuilder sub = QueryBuilders.boolQuery();
            for (TM matcher : textMatchers.asList()) {
                applier.applyField(prefix, sub, matcher);
            }
            apply(booleanQueryBuilder, sub, textMatchers.getMatch());
        }
    }

    protected static void buildFromList(
        @NonNull String prefix,
        @NonNull BoolQueryBuilder booleanQuery,
        @Nullable DateRangeMatcherList rangeMatchers,
        @NonNull FieldApplier<DateRangeMatcher> applier) {
        if (rangeMatchers != null) {
            BoolQueryBuilder sub = QueryBuilders.boolQuery();

            for (DateRangeMatcher rangeMatcher : rangeMatchers) {
                applier.applyField(prefix, sub, rangeMatcher);
            }
            apply(booleanQuery, sub, rangeMatchers.getMatch());
        }
    }


    protected static void buildFromList(
        @NonNull String prefix,
        @NonNull BoolQueryBuilder booleanQuery,
        @Nullable DurationRangeMatcherList rangeMatchers,
        @NonNull FieldApplier<DurationRangeMatcher> applier) {
        if (rangeMatchers != null) {
            BoolQueryBuilder sub = QueryBuilders.boolQuery();

            for (DurationRangeMatcher rangeMatcher : rangeMatchers) {
                applier.applyField(prefix, sub, rangeMatcher);
            }
            apply(booleanQuery, sub, rangeMatchers.getMatch());
        }
    }

    protected static void nested(
        @NonNull String prefix,
        @NonNull String path,
        @NonNull BoolQueryBuilder booleanQueryBuilder,
        @Nullable TextMatcherList textMatchers,
        @NonNull FieldApplier<TextMatcher> applier) {
        if (textMatchers != null) {
            BoolQueryBuilder query = QueryBuilders.boolQuery();
            for (TextMatcher matcher : textMatchers) {
                applier.applyField(prefix, query, matcher);
            }

            QueryBuilder nested = QueryBuilders.nestedQuery(prefix + path, query, ScoreMode.Avg);
            apply(booleanQueryBuilder, nested, textMatchers.getMatch());
        }
    }


    protected static void build(
        @NonNull String prefix,
        @NonNull BoolQueryBuilder booleanQuery,
        @Nullable ExtendedTextMatcher textMatcher,
        @NonNull ExtendedTextSingleFieldApplier applier) {
        if (textMatcher != null) {
            applier.applyField(prefix, booleanQuery, textMatcher);
        }
    }

    protected static void build(
        @NonNull String prefix,
        @NonNull BoolQueryBuilder booleanQuery,
        @Nullable SimpleTextMatcher textMatcher,
        @NonNull FieldApplier<SimpleTextMatcher> applier) {
        if (textMatcher != null) {
            applier.applyField(prefix, booleanQuery, textMatcher);
        }
    }


    public static <MT extends MatchType, TM extends AbstractTextMatcher<MT>> Optional<QueryBuilder> buildOptionalQuery(
        @NonNull String prefix,
        @NonNull String fieldName,
        @NonNull TM matcher,
        ESMatchType.@NonNull FieldInfo fieldInfo) {
        if (fieldInfo.getCardinality().isPresent()) {
            boolean canMatch = false;
            for (String possibleValue : fieldInfo.getPossibleValues()) {
                if (matcher.test(possibleValue)) {
                    canMatch = true;
                }
            }
            if (!canMatch) {
                return Optional.empty();
            }
        }
        return Optional.of(buildQuery(prefix, fieldName, matcher, fieldInfo));
    }

      public static <MT extends MatchType, TM extends AbstractTextMatcher<MT>> QueryBuilder buildQuery(
        @NonNull String prefix,
        @NonNull String fieldName,
        @NonNull TM matcher,
        ESMatchType. @NonNull FieldInfo fieldInfo) {
        String value = matcher.getValue();
        ESMatchType matchType = ESMatchType.valueOf(matcher.getMatchType().getName());

        String esValue = ESMatchType.esValue(value, matcher.isCaseSensitive());
        return matchType.getQueryBuilder(prefix + fieldName, esValue, fieldInfo);
    }


    /**
     * Builds a relation relationQuery for standalone or embedded media media when the prefix argument is left blank.
     *
     * @param prefix not null path to the media node in the documents to search, including the last dot, can be blank
     */
    public static void relationNestedQuery(
        @NonNull String prefix,
        @NonNull  AbstractRelationSearch relationSearch,
        @NonNull BoolQueryBuilder booleanQuery) {

        BoolQueryBuilder fieldWrapper = QueryBuilders.boolQuery();
        relationQuery(prefix, relationSearch, fieldWrapper);

        NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery(prefix + "relations", fieldWrapper, ScoreMode.Max);

        apply(booleanQuery, nestedQuery, relationSearch.getMatch());
    }

     /**
     * Builds a relation relationQuery for standalone or embedded media media when the prefix argument is left blank.
     *
     * @param prefix not null path to the media node in the documents to search, including the last dot, can be blank
     */
    public static void relationQuery(
        @NonNull String prefix,
        @NonNull AbstractRelationSearch relationSearch,
        @NonNull BoolQueryBuilder boolQueryBuilder) {



        buildFromList(prefix, boolQueryBuilder, relationSearch.getTypes(),
            new TextSingleFieldApplier<>("relations.type"));

        buildFromList(prefix, boolQueryBuilder,
            relationSearch.getBroadcasters(), RELATIONS_APPLIER);

        buildFromList(prefix, boolQueryBuilder, relationSearch.getValues(), new ExtendedTextSingleFieldApplier(prefix + "relations.value"));
        buildFromList(prefix, boolQueryBuilder, relationSearch.getUriRefs(), new TextSingleFieldApplier<>("relations.uriRef"));
    }


    public static RangeQueryBuilder buildQuery(
        @NonNull String fieldName,
        @NonNull DateRangeMatcher matcher) {
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

    public static RangeQueryBuilder buildQuery(
        @NonNull String fieldName,
        @NonNull DurationRangeMatcher matcher) {
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

    public static QueryBuilder simplifyQuery(@NonNull BoolQueryBuilder booleanQuery) {
        if (booleanQuery.hasClauses()) {
            if ((booleanQuery.must().size() + booleanQuery.should().size() == 1 && booleanQuery.filter().isEmpty())) {
                if (booleanQuery.must().size() == 1) {
                    return booleanQuery.must().get(0);
                } else {
                    return booleanQuery.should().get(0);
                }
            }
            return booleanQuery;
        } else {
            return QueryBuilders.matchAllQuery();
        }
    }




    protected static <MT extends MatchType, TM extends AbstractTextMatcher<MT>> void
    build(String prefix, BoolQueryBuilder booleanQueryBuilder, AbstractTextMatcherList<TM, MT> list, ESQueryBuilder.FieldApplier<TM> applier) {
        BoolQueryBuilder append = QueryBuilders.boolQuery();
        for (TM matcher : list) {
            applier.applyField(prefix, append, matcher);
        }
        ESQueryBuilder.apply(booleanQueryBuilder, append, list.getMatch());
    }

    public static QueryBuilder search(
        @Nullable TermSearch searches,
        @NonNull String axis,
        @NonNull String field) {
        return search(searches, "", axis, field);
    }

    public static QueryBuilder search(
        @Nullable TermSearch searches,
        @NonNull String prefix,
        @NonNull String axis,
        @NonNull String field) {
        if (searches == null || searches.getIds() == null || searches.getIds().isEmpty()) {
            return matchAllQuery();
        }

        BoolQueryBuilder booleanFilter = boolQuery();
        build(prefix, booleanFilter, searches.getIds(), new ESQueryBuilder.TextSingleFieldApplier<>(axis + '.' + field));
        return simplifyQuery(booleanFilter);
    }


    protected static boolean boostField(
        @NonNull String field, float boost, Collection<SearchFieldDefinition> searchFieldDefinitions) {
        boolean found = false;
        for (SearchFieldDefinition definition : searchFieldDefinitions) {
            if(definition.getName().equals(field)) {
                float prevBoost = definition.getBoost();
                if (prevBoost != boost) {
                    definition.setBoost(boost);
                    if (definition.isActive()) {
                        log.info("Set boost of {} from {} to {}", definition.getName(), prevBoost, boost);
                    }
                }
                found = true;
            }
        }
        if (!found) {
            log.warn("Could not set boost of field {}", field);
        }
        return found;
    }


}

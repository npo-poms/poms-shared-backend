/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.media;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.*;
import org.elasticsearch.join.query.JoinQueryBuilders;

import nl.vpro.domain.api.*;
import nl.vpro.domain.api.subtitles.ESSubtitlesQueryBuilder;
import nl.vpro.domain.api.subtitles.SubtitlesSearch;
import nl.vpro.media.domain.es.ApiCueIndex;

//import org.elasticsearch.join.query.JoinQueryBuilders;

/**
 * NOTE!: There is also a {@link ESMediaFilterBuilder} equivalent that more or less contains the same code for
 * building filters.
 *
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
@Slf4j
public class ESMediaQueryBuilder extends ESQueryBuilder {

    public static final List<SearchFieldDefinition> SEARCH_FIELDS = Arrays.asList(
        new SearchFieldDefinition("broadcasters.value.text", 2f),
        new SearchFieldDefinition("countries.value.text", 1.2f),
        new SearchFieldDefinition("credits.fullName.text", 2f),
        new SearchFieldDefinition("descriptions.value", 1.1f),
        new SearchFieldDefinition("descriptions.stemmed", 1.0f, false),
        new SearchFieldDefinition("genres.terms.text", 2f),
        new SearchFieldDefinition("images.title", 1.1f),
        new SearchFieldDefinition("images.title.stemmed", 1.5f, false),
        new SearchFieldDefinition("images.description", 1.1f),
        new SearchFieldDefinition("images.description.stemmed", 1.0f, false),
        new SearchFieldDefinition("portals.value.text", 1.5f),
        new SearchFieldDefinition("segments.tags.text", 1.1f),
        new SearchFieldDefinition("segments.tags.stemmed", 1.2f, false),
        new SearchFieldDefinition("segments.titles.value", 1.1f),
        new SearchFieldDefinition("segments.titles.stemmed", 1.5f, false),
        new SearchFieldDefinition("tags.text", 1.2f),
        new SearchFieldDefinition("tags.stemmed", 2.0f, false),
        new SearchFieldDefinition("titles.value", 1.3f),
        new SearchFieldDefinition("titles.stemmed", 3.0f, false)
    );

    public static final SearchFieldDefinition SUBTITLES = new SearchFieldDefinition("subtitles", 0.0f);

    public static QueryBuilder query(MediaSearch searches) {
        if(searches == null) {
            return QueryBuilders.matchAllQuery();
        }

        return query(searches, QueryBuilders.boolQuery(), "");
    }

    /**
     * Builds a media relationQuery for standalone or embedded media when the prefix argument is left blank.
     *
     * @param searches The 'searches' part of a MediaForm
     * @param prefix   not null path to the media node in the documents to search, including the last dot, can be blank
     */
    public static QueryBuilder query(MediaSearch searches, @NotNull BoolQueryBuilder booleanQuery, @NotNull String prefix) {
        if(searches == null) {
            return booleanQuery;
        }

        {
            SimpleTextMatcher textSearch = searches.getText();
            if(textSearch != null && StringUtils.isNotBlank(textSearch.getValue())) {
                BoolQueryBuilder textQuery = buildTextQuery(
                    textSearch,
                    prefix,
                    SEARCH_FIELDS
                );

                if (SUBTITLES.getBoost() > 0.0f) {
                    SubtitlesSearch subtitlesSearch = new SubtitlesSearch();
                    subtitlesSearch.setText(textSearch);
                    //subtitlesSearch.setLanguages(TextMatcherList.must(TextMatcher.must("nl")));
                    QueryBuilder subtitlesQuery = ESSubtitlesQueryBuilder.query(subtitlesSearch);
                    textQuery.should(JoinQueryBuilders.hasChildQuery(ApiCueIndex.TYPE, subtitlesQuery, ScoreMode.Max)).boost(SUBTITLES.getBoost());
                } else {
                    log.debug("Searching in subtitles is disabled");
                }

                apply(booleanQuery, textQuery, textSearch.getMatch());
            }
        }

        build(booleanQuery, searches.getMediaIds(), new TextMultipleFieldsApplier(new String[] {prefix + "mid", prefix + "urn", prefix + "crids"}));
        build(booleanQuery, searches.getTypes(), new TextSingleFieldApplier(prefix + "type"));
        build(booleanQuery, searches.getAvTypes(), new TextSingleFieldApplier(prefix + "avType"));
        build(booleanQuery, searches.getSortDates(), new DateSingleFieldApplier(prefix + "sortDate"));
        build(booleanQuery, searches.getPublishDates(), new DateSingleFieldApplier(prefix + "publishDate"));
        build(booleanQuery, searches.getCreationDates(), new DateSingleFieldApplier(prefix + "creationDate"));
        build(booleanQuery, searches.getLastModifiedDates(), new DateSingleFieldApplier(prefix + "lastModified"));
        build(booleanQuery, searches.getBroadcasters(), new TextSingleFieldApplier(prefix + "broadcasters.id"));
        build(booleanQuery, searches.getAgeRatings(), new TextSingleFieldApplier(prefix + "ageRating"));
        build(booleanQuery, searches.getContentRatings(), new TextSingleFieldApplier(prefix + "contentRatings"));
        {
            TextMatcherList locations = searches.getLocations();
            if(locations != null && !locations.isEmpty()) {
                buildLocationQuery(booleanQuery, prefix, locations);
            }
        }
        build(booleanQuery, searches.getTags(), new ExtendedTextSingleFieldApplier(prefix + "tags"));

        nested(prefix + "genres", booleanQuery, searches.getGenres(), new TextSingleFieldApplier(prefix + "genres.id"));

        build(booleanQuery, searches.getDurations(), new DateSingleFieldApplier(prefix + "duration"));

        nested(prefix + "descendantOf", booleanQuery, searches.getDescendantOf(), new TextMultipleFieldsApplier(new String[] {prefix + "descendantOf.midRef", prefix + "descendantOf.type"}));
        nested(prefix + "episodeOf", booleanQuery, searches.getEpisodeOf(), new TextMultipleFieldsApplier(new String[]{prefix + "episodeOf.midRef", prefix + "episodeOf.type"}));
        nested(prefix + "memberOf", booleanQuery, searches.getMemberOf(), new TextMultipleFieldsApplier(new String[] {prefix + "memberOf.midRef", prefix + "memberOf.type"}));

        {
            if(searches.getRelations() != null) {
                for(RelationSearch relationSearch : searches.getRelations()) {
                    relationQuery(relationSearch, booleanQuery, prefix);
                }
            }
        }

        {
            List<ScheduleEventSearch> scheduleEventSearches = searches.getScheduleEvents();
            if(scheduleEventSearches != null && ! scheduleEventSearches.isEmpty()) {
                for (ScheduleEventSearch scheduleEventSearch : scheduleEventSearches) {
                    buildScheduleQuery(booleanQuery, prefix, scheduleEventSearch);
                }
            }
        }

        {
            List<TitleSearch> titleSearches = searches.getTitles();
            if(titleSearches != null && ! titleSearches.isEmpty()) {
                for (TitleSearch titleSearch : titleSearches) {
                    buildTitleQuery(booleanQuery, prefix, titleSearch);

                }
            }
        }

        if(booleanQuery.hasClauses()) {
            return booleanQuery;
        }

        return QueryBuilders.matchAllQuery();
    }

    private static void buildLocationQuery(BoolQueryBuilder boolQueryBuilder, final String prefix, TextMatcherList locations) {
        build(boolQueryBuilder, locations, new FieldApplier<AbstractTextMatcher>() {
            @Override
            public void applyField(BoolQueryBuilder booleanQueryBuilder, AbstractTextMatcher matcher) {
                BoolQueryBuilder bool = QueryBuilders.boolQuery();
                QueryBuilder extensionQuery = buildQuery(prefix + "locations.programUrl", matcher);
                bool.should(extensionQuery);
                QueryBuilder formatQuery = buildQuery(prefix + "locations.programUrl.extension", matcher.toLowerCase());
                bool.should(formatQuery);
                apply(booleanQueryBuilder, bool, matcher.getMatch());
            }

        });
    }


    static BoolQueryBuilder buildTitleQuery(BoolQueryBuilder boolQueryBuilder, String prefix, TitleSearch titleSearch) {

        if (titleSearch == null) {
            return boolQueryBuilder;
        }

        BoolQueryBuilder titleSub = QueryBuilders.boolQuery();
        if(titleSearch.getOwner() != null) {
            QueryBuilder titleQuery = QueryBuilders.termQuery(prefix + "expandedTitles.owner", titleSearch.getOwner().name());
            titleSub.must(titleQuery);
        }
        if(titleSearch.getType() != null) {
            QueryBuilder typeQuery = QueryBuilders.termQuery(prefix + "expandedTitles.type", titleSearch.getType().name());
            titleSub.must(typeQuery);
        }
        if(titleSearch.getValue() != null) {
            ExtendedTextSingleFieldApplier titleApplier = new ExtendedTextSingleFieldApplier("expandedTitles.value");
            titleApplier.applyField(titleSub, titleSearch.getValue());
        }


        NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery(prefix + "expandedTitles", titleSub, ScoreMode.Max);

        apply(boolQueryBuilder, nestedQuery, titleSearch.getMatch());

        return boolQueryBuilder;
    }





    private static void buildScheduleQuery(BoolQueryBuilder boolQueryBuilder, String prefix, ScheduleEventSearch matcher) {
        BoolQueryBuilder scheduleSub = QueryBuilders.boolQuery();
        if(matcher.getChannel() != null) {
            QueryBuilder channelQuery = QueryBuilders.termQuery(prefix + "scheduleEvents.channel", matcher.getChannel().name());
            scheduleSub.must(channelQuery);
        }
        if(StringUtils.isNotEmpty(matcher.getNet())) {
            QueryBuilder netQuery = QueryBuilders.termQuery(prefix + "scheduleEvents.net", matcher.getNet());
            scheduleSub.must(netQuery);
        }
        if(matcher.getRerun() != null) {
            if (matcher.getRerun()) {
                scheduleSub.must(QueryBuilders.termQuery(prefix + "scheduleEvents.repeat.isRerun", true));
            } else {
                // don't filter.. The propery may be missing.
                // We filter afterwards
                // NPA-404
            }
        }

        if(matcher.getBegin() != null || matcher.getEnd() != null) {
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery(prefix + "scheduleEvents.start");
            rangeQuery.includeLower(true);
            rangeQuery.includeUpper(matcher.includeEnd());
            rangeQuery.from(instantToLong(matcher.getBegin()));
            rangeQuery.to(instantToLong(matcher.getEnd()));
            scheduleSub.must(rangeQuery);
        }

        if(scheduleSub.hasClauses()) {
            apply(boolQueryBuilder, scheduleSub, matcher.getMatch());
        }
    }



    static private Long instantToLong(Instant instant) {
        if (instant == null) {
            return null;
        }
        try {
            return instant.toEpochMilli();
        }  catch (ArithmeticException ignored) {
            // See javadoc of Instant#toEpochMillis
            return null;
        }
    }


    static void boostField(String field, float boost) {
        getSearchFields().forEach(definition -> {
            if (definition.getName().equals(field)) {
                definition.setBoost(boost);
            }
        });

    }

    static List<SearchFieldDefinition> getSearchFields() {
        return getSearchFieldStream().collect(Collectors.toList());
    }

    static Stream<SearchFieldDefinition> getSearchFieldStream() {
        return Stream.concat(SEARCH_FIELDS.stream(), Stream.of(SUBTITLES));
    }
}

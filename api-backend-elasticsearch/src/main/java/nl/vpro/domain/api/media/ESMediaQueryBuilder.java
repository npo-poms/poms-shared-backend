/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.media;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.elasticsearch.index.query.*;

import nl.vpro.domain.api.ESMatchType.FieldInfo;
import nl.vpro.domain.api.ESMatchType.FieldInfoWrapper;
import nl.vpro.domain.api.*;
import nl.vpro.domain.classification.ClassificationServiceLocator;
import nl.vpro.domain.media.*;
import nl.vpro.domain.media.support.OwnerType;
import nl.vpro.media.broadcaster.BroadcasterServiceLocator;

import static nl.vpro.domain.api.ESMatchType.FieldInfo.TEXT;
import static nl.vpro.domain.api.ESMatchType.FieldInfo.enumValue;

;

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
        new SearchFieldDefinition("credits.name.text", 2f),
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

    //public static final SearchFieldDefinition SUBTITLES = new SearchFieldDefinition("subtitles", 0.0f);

    private static final FieldInfo MEDIA_TYPE = FieldInfo.enumValue(MediaType.class);


    /**
     * Builds  an Elastic Search {@link QueryBuilder} from a {@link MediaSearch}
     * @param searches or <code>null</code> (resulting in a matchAllQuery)
     */
    public static QueryBuilder query(
        @NonNull String prefix,
        MediaSearch searches) {
        if(searches == null) {
            return QueryBuilders.matchAllQuery();
        }
        BoolQueryBuilder builder = QueryBuilders.boolQuery();
        buildMediaQuery(prefix, builder, searches);
        return simplifyQuery(builder);
    }
    public static QueryBuilder query(MediaSearch searches) {
        return query("", searches);
    }

    /**
     * Builds a media relationQuery for standalone or embedded media when the prefix argument is left blank.
     *  @param prefix   not null path to the media node in the documents to search, including the last dot, can be blank
     * @param searches The 'searches' part of a MediaForm
     */
    public static void buildMediaQuery(
        @NonNull final String prefix,
        @NonNull final BoolQueryBuilder booleanQuery,
        @Nullable final MediaSearch searches) {

        if (searches == null) {
            return;
        }

        {
            SimpleTextMatcher textSearch = searches.getText();
            if(textSearch != null && StringUtils.isNotBlank(textSearch.getValue())) {
                BoolQueryBuilder textQuery = buildTextQuery(
                    prefix, textSearch,
                    SEARCH_FIELDS
                );

            /*    if (SUBTITLES.getBoost() > 0.0f) {
                    SubtitlesSearch subtitlesSearch = new SubtitlesSearch();
                    subtitlesSearch.setText(textSearch);
                    //subtitlesSearch.setLanguages(TextMatcherList.must(TextMatcher.must("nl")));
                    QueryBuilder subtitlesQuery = ESSubtitlesQueryBuilder.query(subtitlesSearch);
                    textQuery
                        .should(
                            JoinQueryBuilders.hasChildQuery(ApiCueIndex.TYPE, subtitlesQuery, ScoreMode.Max)
                        )
                        .boost(SUBTITLES.getBoost());
                } else {
                    log.debug("Searching in subtitles is disabled");
                }
*/
                apply(booleanQuery, textQuery, textSearch.getMatch());
            }
        }

        buildFromList(
            prefix,
            booleanQuery,
            searches.getMediaIds(),
            new TextMultipleFieldsApplier<>("mid", "urn", "crids")
        );
        buildFromList(
            prefix,
            booleanQuery,
            searches.getTypes(),
            new TextSingleFieldApplier<>("type", MEDIA_TYPE)
        );
        buildFromList(
            prefix,
            booleanQuery,
            searches.getAvTypes(),
            new TextSingleFieldApplier<>("avType", enumValue(AVType.class))
        );
        buildFromList(
            prefix,
            booleanQuery,
            searches.getSortDates(),
            new DateSingleFieldApplier("sortDate")
        );
        buildFromList(
            prefix,
            booleanQuery,
            searches.getPublishDates(),
            new DateSingleFieldApplier("publishDate")
        );
        buildFromList(
            prefix,
            booleanQuery,
            searches.getCreationDates(),
            new DateSingleFieldApplier("creationDate")
        );
        buildFromList(
            prefix,
            booleanQuery,
            searches.getLastModifiedDates(),
            new DateSingleFieldApplier("lastModified")
        );
        buildFromList(
            prefix,
            booleanQuery,
            searches.getBroadcasters(),
            new TextSingleFieldApplier<>("broadcasters.id",
                FieldInfo.builder()
                    .possibleValues(BroadcasterServiceLocator.getIds())
                    .build()
            )
        );
        buildFromList(
            prefix,
            booleanQuery,
            searches.getAgeRatings(),
            new TextSingleFieldApplier<>("ageRating", enumValue(AgeRating.class))
        );
        buildFromList(
            prefix,
            booleanQuery,
            searches.getContentRatings(),
            new TextSingleFieldApplier<>("contentRatings", enumValue(ContentRating.class))
        );
        {
            TextMatcherList locations = searches.getLocations();
            if(locations != null && !locations.isEmpty()) {
                buildLocationQuery(booleanQuery, prefix, locations);
            }
        }
        buildFromList(
            prefix,
            booleanQuery,
            searches.getTags(),
            new ExtendedTextSingleFieldApplier("tags")
        );
        nested(
            prefix,
            "genres",
            booleanQuery,
            searches.getGenres(),
            new TextSingleFieldApplier<>("genres.id",
                FieldInfo.builder()
                    .possibleValues(ClassificationServiceLocator.getTerms())
                    .build()
            )
        );

        buildFromList(
            prefix,
            booleanQuery,
            searches.getDurations(),
            new DurationSingleFieldApplier("duration")
        );

        nested(
            prefix,
            "descendantOf",
            booleanQuery,
            searches.getDescendantOf(),
            new TextMultipleFieldsApplier<>(
                FieldInfoWrapper.builder()
                    .name("descendantOf.midRef")
                    .fieldInfo(TEXT)
                    .build(),
                FieldInfoWrapper.builder()
                    .name("descendantOf.type")
                    .fieldInfo(MEDIA_TYPE)
                    .build()
            )
        );
        nested(
            prefix, "episodeOf",
            booleanQuery,
            searches.getEpisodeOf(),
            new TextMultipleFieldsApplier<>(
                FieldInfoWrapper.builder()
                    .name("episodeOf.midRef")
                    .fieldInfo(TEXT)
                    .build(),
                FieldInfoWrapper.builder()
                    .name("episodeOf.type")
                    .fieldInfo(MEDIA_TYPE)
                    .build()
            )
        );
        nested(prefix, "memberOf", booleanQuery, searches.getMemberOf(),
            new TextMultipleFieldsApplier<>(
                FieldInfoWrapper.builder()
                    .name("memberOf.midRef")
                    .fieldInfo(TEXT)
                    .build(),
                FieldInfoWrapper.builder()
                    .name("memberOf.type")
                    .fieldInfo(MEDIA_TYPE)
                    .build()
            )
        );

        if(searches.getRelations() != null) {
            for(RelationSearch relationSearch : searches.getRelations()) {
                relationQuery(prefix, relationSearch, booleanQuery);

            }
        }

        {
            List<ScheduleEventSearch> scheduleEventSearches = searches.getScheduleEvents();
            if(scheduleEventSearches != null && ! scheduleEventSearches.isEmpty()) {
                for (ScheduleEventSearch scheduleEventSearch : scheduleEventSearches) {
                    buildScheduleQuery(
                        prefix,
                        booleanQuery,
                        scheduleEventSearch
                    );
                }
            }
        }

        {
            List<TitleSearch> titleSearches = searches.getTitles();
            if(titleSearches != null && ! titleSearches.isEmpty()) {
                for (TitleSearch titleSearch : titleSearches) {
                    buildTitleQuery(prefix, booleanQuery, titleSearch);

                }
            }
        }
        {
            List<GeoLocationSearch> geoLocationSearches = searches.getGeoLocations();
            if (geoLocationSearches != null && ! geoLocationSearches.isEmpty()) {
                for (GeoLocationSearch geoLocationSearch : geoLocationSearches) {
                    buildOwnedFieldQuery(prefix, "expandedGeoLocations", booleanQuery, geoLocationSearch);

                }
            }
        }
    }



    /**
     *
     * @param axis Non empty string
     */
    public static QueryBuilder filter(
        @NonNull String prefix,
        @NonNull String axis,
        MemberRefSearch searches) {
        if(searches == null) {
            return QueryBuilders.matchAllQuery();
        }

        BoolQueryBuilder booleanFilter = QueryBuilders.boolQuery();
        if(searches.getMediaIds() != null && !searches.getMediaIds().isEmpty()) {
            build(prefix, booleanFilter, searches.getMediaIds(), new TextSingleFieldApplier<>(axis + ".midRef"));
        }

        if(searches.getTypes() != null && !searches.getTypes().isEmpty()) {
            build(prefix, booleanFilter, searches.getTypes(), new TextSingleFieldApplier<>(axis + ".type"));
        }
        return booleanFilter;
    }


    public static QueryBuilder filter(MemberRefSearch searches, @NonNull String axis) {
        return filter("", axis, searches);
    }

    public static QueryBuilder filter(@NonNull String axis, TitleSearch searches) {
        return filter("", axis, searches);
    }

    public static QueryBuilder filter(@NonNull String prefix, String axis, TitleSearch titleSearch) {
        if(titleSearch == null) {
            return QueryBuilders.matchAllQuery();
        }

        BoolQueryBuilder booleanFilter = QueryBuilders.boolQuery();
        ESMediaQueryBuilder.buildTitleQuery(prefix, booleanFilter, titleSearch);
        return simplifyQuery(booleanFilter);
    }




    private static void buildLocationQuery(
        @NonNull BoolQueryBuilder boolQueryBuilder,
        @NonNull final String prefix,
        TextMatcherList locations) {

        buildFromList(prefix, boolQueryBuilder, locations, (pref, booleanQueryBuilder, matcher) -> {
            BoolQueryBuilder bool = QueryBuilders.boolQuery();
            buildOptionalQuery(prefix, "locations.programUrl", matcher, TEXT).ifPresent(bool::should);
            buildOptionalQuery(prefix, "locations.programUrl.extension", matcher.toLowerCase(), TEXT).ifPresent(bool::should);
            apply(booleanQueryBuilder, bool, matcher.getMatch());
        });
    }


    static BoolQueryBuilder buildTitleQuery(
        @NonNull String prefix,
        @NonNull BoolQueryBuilder boolQueryBuilder,
        TitleSearch titleSearch) {

        if (titleSearch == null) {
            return boolQueryBuilder;
        }

        BoolQueryBuilder titleSub = QueryBuilders.boolQuery();
        if(titleSearch.getOwner() != null) {
            QueryBuilder titleQuery = QueryBuilders.termQuery(prefix + "expandedTitles.owner", titleSearch.getOwner().name());
            titleSub.must(titleQuery);
        }
        if(titleSearch.getType() != null) {
            QueryBuilder typeQuery =
                QueryBuilders.termQuery(prefix + "expandedTitles.type", titleSearch.getType().name());
            titleSub.must(typeQuery);
        }
        if(titleSearch.getValue() != null) {
            ExtendedTextSingleFieldApplier titleApplier = new ExtendedTextSingleFieldApplier("expandedTitles.value");
            titleApplier.applyField(prefix, titleSub, titleSearch.asExtendedTextMatcher());
        }


        NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery(prefix + "expandedTitles", titleSub, ScoreMode.Max);

        apply(boolQueryBuilder, nestedQuery, titleSearch.getMatch());

        return boolQueryBuilder;
    }


    /**
     * @since 5.11
     */
    static BoolQueryBuilder buildOwnedFieldQuery(
        @NonNull String prefix,
        @NonNull String expandedValuesField,
        @NonNull BoolQueryBuilder boolQueryBuilder,
        @NonNull GeoLocationSearch geoLocationSearch) {


        BoolQueryBuilder sub = QueryBuilders.boolQuery();
        OwnerType owner = geoLocationSearch.getOwner();
        if (owner == null) {
            owner = OwnerType.BROADCASTER;
        }
        QueryBuilder ownerQuery = QueryBuilders.termQuery(prefix + expandedValuesField + ".owner", owner.name());
        sub.must(ownerQuery);

        if(geoLocationSearch.getGtaaUri() != null) {
            QueryBuilder uriQuery =
                QueryBuilders.termQuery(prefix + expandedValuesField + ".values.gtaaUri", geoLocationSearch.getGtaaUri().toString());
            sub.must(uriQuery);
        }

        // TODO Not all owned fields will have role.
        if(geoLocationSearch.getRole() != null) {
            QueryBuilder roleQuery =
                QueryBuilders.termQuery(prefix + expandedValuesField + ".values.role", geoLocationSearch.getRole().name());
            sub.must(roleQuery);
        }
        if(geoLocationSearch.getValue() != null) {
            ExtendedTextSingleFieldApplier valueApplier = new ExtendedTextSingleFieldApplier(expandedValuesField + ".values.name");
            valueApplier.applyField(prefix, sub, geoLocationSearch.asExtendedTextMatcher());
        }


        NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery(prefix + expandedValuesField , sub, ScoreMode.Max);

        apply(boolQueryBuilder, nestedQuery, geoLocationSearch.getMatch());

        return boolQueryBuilder;
    }


    private static void buildScheduleQuery(String prefix, BoolQueryBuilder boolQueryBuilder, ScheduleEventSearch matcher) {
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
                // This would be simpler, but reindex should be done first otherwise it's even more broken
                //scheduleSub.must(QueryBuilders.termQuery(prefix + "scheduleEvents.rerun", true));
            } else {
                scheduleSub.must(QueryBuilders.termQuery(prefix + "scheduleEvents.rerun", false));
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
        return getSearchFieldStream().collect(
            Collectors.toList()
        );
    }

    static Stream<SearchFieldDefinition> getSearchFieldStream() {
        //return Stream.concat(SEARCH_FIELDS.stream(), Stream.of(SUBTITLES));
        return SEARCH_FIELDS.stream();
    }
}

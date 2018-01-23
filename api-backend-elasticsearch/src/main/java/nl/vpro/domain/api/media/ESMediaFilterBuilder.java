/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.media;

import java.util.List;

import javax.annotation.Nonnull;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import nl.vpro.domain.api.*;
import nl.vpro.domain.api.ESQueryBuilder.TextMultipleFieldsApplier;
import nl.vpro.domain.api.ESQueryBuilder.TextSingleFieldApplier;

/**
 * NOTE!: There is also a{@link ESMediaQueryBuilder} equivalent that more or less contains the same code for
 * building queries.
 *
 * @author Roelof Jan Koekoek
 * @since 2.0
 * @TODO This can be dropped in favour of {@link ESMediaQueryBuilder}
 */
public class ESMediaFilterBuilder extends ESFilterBuilder {

    public static QueryBuilder filter(MemberRefSearch searches, @Nonnull String axis) {
        return filter(searches, "", axis);
    }

    public static QueryBuilder filter(MemberRefSearch searches, @Nonnull String prefix, String axis) {
        if(searches == null) {
            return QueryBuilders.matchAllQuery();
        }

        BoolQueryBuilder booleanFilter = QueryBuilders.boolQuery();
        if(searches.getMediaIds() != null && !searches.getMediaIds().isEmpty()) {
            build(booleanFilter, searches.getMediaIds(), new TextSingleFieldApplier(prefix + axis + ".midRef"));
        }

        if(searches.getTypes() != null && !searches.getTypes().isEmpty()) {
            build(booleanFilter, searches.getTypes(), new TextSingleFieldApplier(prefix + axis + ".type"));
        }
        return booleanFilter;
    }

    public static QueryBuilder filter(RelationSearch searches, @Nonnull String axis) {
        return filter(searches, "", axis);
    }

    public static QueryBuilder filter(RelationSearch relationSearch, @Nonnull String prefix, String axis) {
        if(relationSearch == null) {
            return QueryBuilders.matchAllQuery();
        }

        BoolQueryBuilder booleanFilter = QueryBuilders.boolQuery();
        filter(booleanFilter, relationSearch, prefix, axis);
        if (booleanFilter.hasClauses()) {
            return booleanFilter;
        } else {
            return QueryBuilders.matchAllQuery();
        }
    }

    public static void filter(BoolQueryBuilder booleanFilter, RelationSearch relationSearch, @Nonnull String prefix, String axis) {

        if(relationSearch.getTypes() != null && !relationSearch.getTypes().isEmpty()) {
            build(booleanFilter, relationSearch.getTypes(), new TextSingleFieldApplier(prefix + axis + ".type"));
        }

        if(relationSearch.getBroadcasters() != null && !relationSearch.getBroadcasters().isEmpty()) {
            build(booleanFilter, relationSearch.getBroadcasters(), new TextSingleFieldApplier(prefix + axis + ".broadcaster"));
        }

        if(relationSearch.getValues() != null && !relationSearch.getValues().isEmpty()) {
            build(booleanFilter, relationSearch.getValues(), new TextSingleFieldApplier(prefix + axis + ".value"));
        }

        if(relationSearch.getUriRefs() != null && !relationSearch.getUriRefs().isEmpty()) {
            build(booleanFilter, relationSearch.getUriRefs(), new TextSingleFieldApplier(prefix + axis + ".uriRef"));
        }
    }

    public static QueryBuilder filter(TitleSearch searches, @Nonnull String axis) {
        return filter(searches, "", axis);
    }

    public static QueryBuilder filter(TitleSearch titleSearch, @Nonnull String prefix, String axis) {
        if(titleSearch == null) {
            return QueryBuilders.matchAllQuery();
        }

        BoolQueryBuilder booleanFilter = QueryBuilders.boolQuery();
        ESMediaQueryBuilder.buildTitleQuery(booleanFilter, prefix, titleSearch);
        if (booleanFilter.hasClauses()) {
            return booleanFilter;
        } else {
            return QueryBuilders.matchAllQuery();
        }
    }



    public static QueryBuilder filter(MediaSearch searches) {
        return filter(searches, "");
    }

    public static QueryBuilder filter(MediaSearch searches, @Nonnull String prefix) {

        if(searches == null) {
            return QueryBuilders.matchAllQuery();
        }

        BoolQueryBuilder booleanFilter = QueryBuilders.boolQuery();
        boolean hasClause = false;

        {
            SimpleTextMatcher textSearch = searches.getText();
            if(textSearch != null && textSearch.getValue() != null) {
                QueryBuilder textFilter = buildTextFilter(
                    textSearch,
                    ESMediaQueryBuilder.SEARCH_FIELDS
                );
                booleanFilter.must(textFilter);
                hasClause = true;
            }
        }


        {
            TextMatcherList mediaIds = searches.getMediaIds();
            if(mediaIds != null && !mediaIds.isEmpty()) {
                build(booleanFilter, mediaIds, new TextSingleFieldApplier(prefix + "mid"));
                hasClause = true;
            }
        }

        {
            TextMatcherList types = searches.getTypes();
            if(types != null && !types.isEmpty()) {
                build(booleanFilter, types, new TextSingleFieldApplier(prefix + "type"));
                hasClause = true;
            }
        }

        {
            TextMatcherList avTypes = searches.getAvTypes();
            if(avTypes != null && !avTypes.isEmpty()) {
                build(booleanFilter, avTypes, new TextSingleFieldApplier(prefix + "avType"));
                hasClause = true;
            }
        }
        {
            TextMatcherList broadcasters = searches.getBroadcasters();
            if(broadcasters != null && !broadcasters.isEmpty()) {
                build(booleanFilter, broadcasters, new TextSingleFieldApplier(prefix + "broadcasters.id"));
                hasClause = true;
            }
        }

        {
            TextMatcherList locations = searches.getLocations();
            if(locations != null && !locations.isEmpty()) {
                buildLocationFilter(booleanFilter, locations);
                hasClause = true;
            }
        }

        {
            ExtendedTextMatcherList tags = searches.getTags();
            if(tags != null && !tags.isEmpty()) {
                build(booleanFilter, tags, new TextSingleFieldApplier(prefix + "tags"));
                hasClause = true;
            }
        }

        {
            TextMatcherList genres = searches.getGenres();
            if(genres != null && !genres.isEmpty()) {
                build(booleanFilter, genres, new TextSingleFieldApplier(prefix + "genres.id"));
                hasClause = true;
            }
        }

        {
            DurationRangeMatcherList durations = searches.getDurations();
            // not implemented
            if(durations != null && !durations.isEmpty()) {
                //throw new Unsuppported
                ///buildDurationFilter(booleanFilter, durations, new SimpleFieldApplier(prefix + "duration"));
                //hasClause = true;
            }
        }

        {
            TextMatcherList descendantOf = searches.getDescendantOf();
            if(descendantOf != null && !descendantOf.isEmpty()) {
                build(booleanFilter, descendantOf, new TextMultipleFieldsApplier(new String[]{prefix + "descendantOf.midRef", prefix + "descendantOf.type"}));
                hasClause = true;
            }
        }

        {
            TextMatcherList episodeOf = searches.getEpisodeOf();
            if(episodeOf != null && !episodeOf.isEmpty()) {
                build(booleanFilter, episodeOf, new TextMultipleFieldsApplier(new String[]{prefix + "episodeOf.midRef", prefix + "episodeOf.type"}));
                hasClause = true;
            }
        }

        {
            TextMatcherList memberOf = searches.getMemberOf();
            if(memberOf != null && !memberOf.isEmpty()) {
                build(booleanFilter, memberOf, new TextMultipleFieldsApplier(new String[]{prefix + "memberOf.midRef", prefix + "memberOf.type"}));
                hasClause = true;
            }
        }

        if(hasClause) {
            return booleanFilter;
        }

        return QueryBuilders.matchAllQuery();
    }

    public static QueryBuilder filter(MediaSearch searches, QueryBuilder filter) {
        return filter(searches, filter, "");
    }

    public static QueryBuilder filter(MediaSearch searches, QueryBuilder filter, @Nonnull String prefix) {
        if(filter == null || filter instanceof MatchAllQueryBuilder) {
            return filter(searches, prefix);
        }

        if(searches == null) {
            return filter;
        }

        // unwrap
        if(filter instanceof BoolQueryBuilder) {
            return ((BoolQueryBuilder)filter).must(filter(searches, prefix));
        } else {
            BoolQueryBuilder bool = QueryBuilders.boolQuery();
            bool.must(filter(searches, prefix));
            bool.must(filter);
            return bool;
        }
    }

    static QueryBuilder buildTextFilter(SimpleTextMatcher textSearch, List<SearchFieldDefinition> searchFields) {
        BoolQueryBuilder answer = QueryBuilders.boolQuery();
        for(SearchFieldDefinition searchField : searchFields) {
            QueryBuilder textFilter = ESQueryBuilder.buildQuery(searchField.getName(), textSearch, ESMatchType.FieldInfo.TEXT);
            answer.should(textFilter);
        }
        return answer;
    }


    private static void buildLocationFilter(BoolQueryBuilder boolFilterBuilder, TextMatcherList locations) {
        build(boolFilterBuilder, locations, new ESQueryBuilder.FieldApplier() {

            @Override
            public void applyField(BoolQueryBuilder booleanQueryBuilder, Matcher matcher) {

            }

            public <MT extends MatchType> void applyField(BoolQueryBuilder booleanQueryBuilder, AbstractTextMatcher<MT> matcher) {
                QueryBuilder extensionFilter = ESQueryBuilder.buildQuery("locations.programUrl.extension", matcher, ESMatchType.FieldInfo.TEXT);
                booleanQueryBuilder.should(extensionFilter);
                QueryBuilder formatFilter = ESQueryBuilder.buildQuery("locations.avAttributes.avFileFormat", matcher, ESMatchType.FieldInfo.TEXT);
                booleanQueryBuilder.should(formatFilter);
            }

        });
    }


}

/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.media;

import java.util.List;

import javax.annotation.Nonnull;

import org.elasticsearch.index.query.*;

import nl.vpro.domain.api.*;

/**
 * NOTE!: There is also a{@link ESMediaQueryBuilder} equivalent that more or less contains the same code for
 * building queries.
 *
 * @author Roelof Jan Koekoek
 * @since 2.0
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
            build(booleanFilter, searches.getMediaIds(), new SimpleFieldApplier(prefix + axis + ".midRef"));
        }

        if(searches.getTypes() != null && !searches.getTypes().isEmpty()) {
            build(booleanFilter, searches.getTypes(), new SimpleFieldApplier(prefix + axis + ".type"));
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
            build(booleanFilter, relationSearch.getTypes(), new SimpleFieldApplier(prefix + axis + ".type"));
        }

        if(relationSearch.getBroadcasters() != null && !relationSearch.getBroadcasters().isEmpty()) {
            build(booleanFilter, relationSearch.getBroadcasters(), new SimpleFieldApplier(prefix + axis + ".broadcaster"));
        }

        if(relationSearch.getValues() != null && !relationSearch.getValues().isEmpty()) {
            build(booleanFilter, relationSearch.getValues(), new SimpleFieldApplier(prefix + axis + ".value"));
        }

        if(relationSearch.getUriRefs() != null && !relationSearch.getUriRefs().isEmpty()) {
            build(booleanFilter, relationSearch.getUriRefs(), new SimpleFieldApplier(prefix + axis + ".uriRef"));
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
        filter(booleanFilter, titleSearch, prefix, axis);
        if (booleanFilter.hasClauses()) {
            return booleanFilter;
        } else {
            return QueryBuilders.matchAllQuery();
        }
    }

    public static void filter(BoolQueryBuilder booleanFilter, TitleSearch titleSearch, @Nonnull String prefix, String axis) {
        titleSearch.getMatch();

        if(titleSearch.getOwner() != null) {
            QueryBuilder titleQuery = QueryBuilders.termQuery(prefix + "expandedTitles.owner", titleSearch.getOwner().name());
            booleanFilter.must(titleQuery);
        }
        if(titleSearch.getType() != null) {
            QueryBuilder typeQuery = QueryBuilders.termQuery(prefix + "expandedTitles.type", titleSearch.getType().name());
            booleanFilter.must(typeQuery);
        }
        if(titleSearch.getValue() != null) {
            ESQueryBuilder.SingleFieldApplier titleApplier = new ESQueryBuilder.SingleFieldApplier("expandedTitles.value");
            titleApplier.applyField(booleanFilter, titleSearch.getValue());
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
                build(booleanFilter, mediaIds, new SimpleFieldApplier(prefix + "mid"));
                hasClause = true;
            }
        }

        {
            TextMatcherList types = searches.getTypes();
            if(types != null && !types.isEmpty()) {
                build(booleanFilter, types, new SimpleFieldApplier(prefix + "type"));
                hasClause = true;
            }
        }

        {
            TextMatcherList avTypes = searches.getAvTypes();
            if(avTypes != null && !avTypes.isEmpty()) {
                build(booleanFilter, avTypes, new SimpleFieldApplier(prefix + "avType"));
                hasClause = true;
            }
        }
        {
            TextMatcherList broadcasters = searches.getBroadcasters();
            if(broadcasters != null && !broadcasters.isEmpty()) {
                build(booleanFilter, broadcasters, new SimpleFieldApplier(prefix + "broadcasters.id"));
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
                build(booleanFilter, tags, new SimpleFieldApplier(prefix + "tags"));
                hasClause = true;
            }
        }

        {
            TextMatcherList genres = searches.getGenres();
            if(genres != null && !genres.isEmpty()) {
                build(booleanFilter, genres, new SimpleFieldApplier(prefix + "genres.id"));
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
                build(booleanFilter, descendantOf, new MultipleFieldApplier(prefix + "descendantOf.midRef", prefix + "descendantOf.type"));
                hasClause = true;
            }
        }

        {
            TextMatcherList episodeOf = searches.getEpisodeOf();
            if(episodeOf != null && !episodeOf.isEmpty()) {
                build(booleanFilter, episodeOf, new MultipleFieldApplier(prefix + "episodeOf.midRef", prefix + "episodeOf.type"));
                hasClause = true;
            }
        }

        {
            TextMatcherList memberOf = searches.getMemberOf();
            if(memberOf != null && !memberOf.isEmpty()) {
                build(booleanFilter, memberOf, new MultipleFieldApplier(prefix + "memberOf.midRef", prefix + "memberOf.type"));
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
            QueryBuilder textFilter = buildFilter(searchField.getName(), textSearch);
            answer.should(textFilter);
        }
        return answer;
    }


    private static void buildLocationFilter(BoolQueryBuilder boolFilterBuilder, TextMatcherList locations) {
        build(boolFilterBuilder, locations, new FieldApplier() {
            @Override
            public <S extends MatchType> void applyField(BoolQueryBuilder booleanQueryBuilder, AbstractTextMatcher<S> matcher) {
                QueryBuilder extensionFilter = buildFilter("locations.programUrl.extension", matcher, true);
                booleanQueryBuilder.should(extensionFilter);
                QueryBuilder formatFilter = buildFilter("locations.avAttributes.avFileFormat", matcher, true);
                booleanQueryBuilder.should(formatFilter);
            }
        });
    }


}

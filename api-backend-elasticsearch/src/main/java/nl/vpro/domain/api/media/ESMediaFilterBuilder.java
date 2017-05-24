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

    public static FilterBuilder filter(MemberRefSearch searches, @Nonnull String axis) {
        return filter(searches, "", axis);
    }

    public static FilterBuilder filter(MemberRefSearch searches, @Nonnull String prefix, String axis) {
        if(searches == null) {
            return FilterBuilders.matchAllFilter();
        }

        BoolFilterBuilder booleanFilter = FilterBuilders.boolFilter();
        if(searches.getMediaIds() != null && !searches.getMediaIds().isEmpty()) {
            build(booleanFilter, searches.getMediaIds(), new SimpleFieldApplier(prefix + axis + ".midRef"));
        }

        if(searches.getTypes() != null && !searches.getTypes().isEmpty()) {
            build(booleanFilter, searches.getTypes(), new SimpleFieldApplier(prefix + axis + ".type"));
        }
        return booleanFilter;
    }

    public static FilterBuilder filter(RelationSearch searches, @Nonnull String axis) {
        return filter(searches, "", axis);
    }

    public static FilterBuilder filter(RelationSearch relationSearch, @Nonnull String prefix, String axis) {
        if(relationSearch == null) {
            return FilterBuilders.matchAllFilter();
        }

        BoolFilterBuilder booleanFilter = FilterBuilders.boolFilter();
        filter(booleanFilter, relationSearch, prefix, axis);
        if (booleanFilter.hasClauses()) {
            return booleanFilter;
        } else {
            return FilterBuilders.matchAllFilter();
        }
    }

    public static void filter(BoolFilterBuilder booleanFilter, RelationSearch relationSearch, @Nonnull String prefix, String axis) {

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


    public static FilterBuilder filter(MediaSearch searches) {
        return filter(searches, "");
    }

    public static FilterBuilder filter(MediaSearch searches, @Nonnull String prefix) {
        if(searches == null) {
            return FilterBuilders.matchAllFilter();
        }

        BoolFilterBuilder booleanFilter = FilterBuilders.boolFilter();
        boolean hasClause = false;

        {
            SimpleTextMatcher textSearch = searches.getText();
            if(textSearch != null && textSearch.getValue() != null) {
                FilterBuilder textFilter = buildTextFilter(
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

        return FilterBuilders.matchAllFilter();
    }

    public static FilterBuilder filter(MediaSearch searches, FilterBuilder filter) {
        return filter(searches, filter, "");
    }

    public static FilterBuilder filter(MediaSearch searches, FilterBuilder filter, @Nonnull String prefix) {
        if(filter == null || filter instanceof MatchAllFilterBuilder) {
            return filter(searches, prefix);
        }

        if(searches == null) {
            return filter;
        }

        // unwrap
        if(filter instanceof AndFilterBuilder) {
            return ((AndFilterBuilder)filter).add(filter(searches, prefix));
        } else if(filter instanceof BoolFilterBuilder) {
            return ((BoolFilterBuilder)filter).must(filter(searches, prefix));
        } else {
            return FilterBuilders.andFilter(filter(searches, prefix), filter);
        }
    }

    static FilterBuilder buildTextFilter(SimpleTextMatcher textSearch, List<SearchFieldDefinition> searchFields) {
        BoolFilterBuilder answer = FilterBuilders.boolFilter();
        for(SearchFieldDefinition searchField : searchFields) {
            FilterBuilder textFilter = buildFilter(searchField.getName(), textSearch);
            answer.should(textFilter);
        }
        return answer;
    }


    private static void buildLocationFilter(BoolFilterBuilder boolFilterBuilder, TextMatcherList locations) {
        build(boolFilterBuilder, locations, new FieldApplier() {
            @Override
            public <S extends MatchType> void applyField(BoolFilterBuilder booleanQueryBuilder, AbstractTextMatcher<S> matcher) {
                FilterBuilder extensionFilter = buildFilter("locations.programUrl.extension", matcher, true);
                booleanQueryBuilder.should(extensionFilter);
                FilterBuilder formatFilter = buildFilter("locations.avAttributes.avFileFormat", matcher, true);
                booleanQueryBuilder.should(formatFilter);
            }
        });
    }


}

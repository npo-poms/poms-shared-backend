/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.media;

import javax.annotation.Nonnull;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import nl.vpro.domain.api.*;
import nl.vpro.domain.media.support.TextualType;

/**
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
public class ESMediaFacetsBuilder extends ESFacetsBuilder {

    public static void facets(SearchSourceBuilder searchBuilder, MediaForm form, QueryBuilder profileFilter) {
        buildFacets(searchBuilder, form, profileFilter, "");
    }

    /**
     * @param prefix empty string of path to this field including the last dot e.g., "embeds.media."
     */
    public static void buildFacets(SearchSourceBuilder searchBuilder, MediaForm form, QueryBuilder profileFilter, @Nonnull String prefix) {
        if (form != null && form.isFaceted()) {

            MediaFacets facets = form.getFacets();

            // Under default ES behaviour profile filtering does not influence facet results
            QueryBuilder facetFilter;
            if (facets.getFilter() == null) {
                facetFilter = profileFilter;
            } else {
                facetFilter = ESMediaFilterBuilder.filter(facets.getFilter(), profileFilter);
            }

            // Append all aggregations to this filtered aggregation builder
            FilterAggregationBuilder aggregationBuilder = AggregationBuilders
                .filter(ROOT_FILTER, facetFilter);

            searchBuilder.aggregation(aggregationBuilder);

            {
                TitleFacetList titles = facets.getTitles();
                if (titles != null) {
                    if (titles.asMediaFacet()) {
                        addTextualTypeFacet(searchBuilder, addFacetFilter(titles, facetFilter, prefix), prefix + "titles", TextualType.MAIN, titles);
                    }
                    addNestedTitlesAggregations(aggregationBuilder, titles, prefix);
                }

            }

            {
                MediaFacet types = facets.getTypes();
                addFacet(searchBuilder, addFacetFilter(types, facetFilter, prefix), prefix + "type", types, null);
            }

            {
                MediaFacet avTypes = facets.getAvTypes();
                addFacet(searchBuilder, addFacetFilter(avTypes, facetFilter, prefix), prefix + "avType", avTypes, null);
            }

            {
                DateRangeFacets sortDates = facets.getSortDates();
                addFacet(searchBuilder, facetFilter, prefix + "sortDate", sortDates, null);
            }

            {
                MediaFacet broadcasters = facets.getBroadcasters();
                addFacet(searchBuilder, addFacetFilter(broadcasters, facetFilter, prefix), prefix + "broadcasters.id", broadcasters, null);
            }

            {
                MediaSearchableTermFacet genres = facets.getGenres();
                addNestedAggregation("genres", aggregationBuilder, "id", genres, prefix);
            }

            {
                ExtendedMediaFacet tags = facets.getTags();
                addFacet(searchBuilder, addFacetFilter(tags, facetFilter, prefix), prefix + esField("tags", tags), tags,
                        null);

            }

            {
                DurationRangeFacets durations = facets.getDurations();
                addFacet(searchBuilder, facetFilter, prefix + "duration", durations, null);
            }

            {
                MemberRefFacet descendantOf = facets.getDescendantOf();
                addNestedAggregation("descendantOf", aggregationBuilder, "midRef", descendantOf, prefix);
            }

            {
                MemberRefFacet episodeOf = facets.getEpisodeOf();
                addNestedAggregation("episodeOf", aggregationBuilder, "midRef", episodeOf, prefix);
            }

            {
                MemberRefFacet memberOf = facets.getMemberOf();
                addNestedAggregation("memberOf", aggregationBuilder, "midRef", memberOf, prefix);
            }

            {
                RelationFacetList relations = facets.getRelations();
                addNestedRelationAggregations(aggregationBuilder, "value", relations, prefix);
            }

            {
                MediaFacet ageRatings = facets.getAgeRatings();
                addFacet(searchBuilder, addFacetFilter(ageRatings, facetFilter, prefix), prefix + "ageRating", ageRatings, null);
            }

            {
                MediaFacet contentRatings = facets.getContentRatings();
                addFacet(searchBuilder, addFacetFilter(contentRatings, facetFilter, prefix), prefix + "contentRatings", contentRatings, null);
            }
        }
    }

    protected static void addNestedAggregation(String nestedField, FilterAggregationBuilder rootAggregation, String facetField, MediaSearchableTermFacet facet, String pathPrefix) {
        if (facet == null) {
            return;
        }

        AggregationBuilder terms = getFilteredTermsBuilder(
            pathPrefix,
            nestedField,
            facetField,
            facet,
            facet.hasSubSearch() ? ESFilterBuilder.filter(facet.getSubSearch(), pathPrefix, nestedField, facetField) : null
        );

        NestedAggregationBuilder nestedBuilder = getNestedBuilder(
            pathPrefix,
            nestedField,
            null,
            terms
        );
        AggregationBuilder builder = filterAggregation(pathPrefix, nestedField, nestedBuilder, facet.getFilter());
        rootAggregation.subAggregation(builder);

    }

    protected static void addNestedAggregation(String nestedField, FilterAggregationBuilder rootAggregation, String facetField, MemberRefFacet facet, String pathPrefix) {
        if (facet == null) {
            return;
        }

        AggregationBuilder terms = getFilteredTermsBuilder(
            pathPrefix,
            nestedField,
            facetField,
            facet,
            facet.hasSubSearch() ? ESMediaFilterBuilder.filter(facet.getSubSearch(), pathPrefix, nestedField) : null
        );

        NestedAggregationBuilder nestedBuilder = getNestedBuilder(
            pathPrefix,
            nestedField,
            null,
            terms
        );
        AggregationBuilder builder = filterAggregation(pathPrefix, nestedField, nestedBuilder, facet.getFilter());
        rootAggregation.subAggregation(builder);

    }

    protected static void addNestedRelationAggregations(FilterAggregationBuilder rootAggregation, String facetField, RelationFacetList facets, String pathPrefix) {
        if (facets == null || facets.isEmpty()) {
            return;
        }

        RelationSearch relationSearch = facets.getSubSearch();
        for (RelationFacet facet : facets) {
            QueryBuilder facetFilter = facet.hasSubSearch() ? ESMediaFilterBuilder.filter(facet.getSubSearch(), pathPrefix, "relations") : null;
            if (relationSearch != null) {
                if (facetFilter == null) {
                    facetFilter = ESMediaFilterBuilder.filter(relationSearch, pathPrefix, "relations");
                } else {
                    ESMediaFilterBuilder.filter((BoolQueryBuilder) facetFilter, relationSearch, pathPrefix, "relations");
                }
            }
            AggregationBuilder termsBuilder = getFilteredRelationTermsBuilder(
                pathPrefix,
                "relations",
                facetField,
                facet,
                facetFilter
            );

            NestedAggregationBuilder nestedBuilder = getNestedBuilder(
                pathPrefix,
                "relations",
                null,
                termsBuilder
            );
            String facetName = escapeFacetName(facet.getName());
            AggregationBuilder builder = filterAggregation(pathPrefix, facetName, nestedBuilder, facet.getFilter(), facets.getFilter());
            rootAggregation.subAggregation(builder);
        }
    }

    protected static void addNestedTitlesAggregations(FilterAggregationBuilder rootAggregation, TitleFacetList facets, String pathPrefix) {
        if (facets == null || facets.isEmpty()) {
            return;
        }

        TitleSearch titleSearch = facets.getSubSearch();
        for (TitleFacet facet : facets) {
            QueryBuilder filter = facet.hasSubSearch() ?
                ESMediaFilterBuilder.filter(facet.getSubSearch(), pathPrefix, "expandedTitles") : null;
            if (titleSearch != null) {
                if (filter == null) {
                    filter = ESMediaFilterBuilder.filter(titleSearch, pathPrefix, "expandedTitles");
                } else {
                    ESMediaQueryBuilder.buildTitleQuery((BoolQueryBuilder) filter, "", titleSearch);
                }
            }
            if (filter == null) {
                throw new IllegalArgumentException();
            }

            FilterAggregationBuilder filterAggregationBuilder =
                AggregationBuilders.filter(escapeFacetName(facet.getName()), filter);
            rootAggregation.subAggregation(filterAggregationBuilder);
        }
    }

    protected static AggregationBuilder getFilteredRelationTermsBuilder(
        String pathPrefix,
        String nestedField,
        String facetField,
        RelationFacet facet,
        QueryBuilder subSearch
    ) {
        return getFilteredTermsBuilder(pathPrefix, nestedField, esField(facetField, facet.isCaseSensitive()), facet, facet.getName(), subSearch);
    }

    private static AggregationBuilder filterAggregation(String pathPrefix, String facetName, AggregationBuilder aggregationBuilder, MediaSearch... mediaSearch) {
        if (mediaSearch == null) {
            return aggregationBuilder;
        }

        for (MediaSearch search : mediaSearch) {
            QueryBuilder facetFilter = ESMediaFilterBuilder.filter(search, pathPrefix);
            aggregationBuilder = AggregationBuilders
                .filter("filter_" + facetName, facetFilter)
                .subAggregation(aggregationBuilder);
        }

        return aggregationBuilder;
    }

    private static void addTextualTypeFacet(SearchSourceBuilder searchBuilder, QueryBuilder filterBuilder, String fieldName, TextualType type, TextFacet facet) {
        BoolQueryBuilder mainTitleFilter = QueryBuilders.boolQuery();
        mainTitleFilter.must(filterBuilder);
        mainTitleFilter.must(QueryBuilders.termQuery(fieldName + ".type", type.name()));
        addFacet(searchBuilder, mainTitleFilter, fieldName + ".value.full", facet, null);
    }

    private static QueryBuilder addFacetFilter(TextFacet<MediaSearch> facet, QueryBuilder filterBuilder, String prefix) {
        if (facet != null && facet.getFilter() != null) {
            filterBuilder = ESMediaFilterBuilder.filter(facet.getFilter(), filterBuilder, prefix);
        }
        return filterBuilder;
    }
}

/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.media;

import javax.annotation.Nonnull;

import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.NestedBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import nl.vpro.domain.api.*;
import nl.vpro.domain.media.support.TextualType;

/**
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
public class ESMediaFacetsBuilder extends ESFacetsBuilder {

    public static void facets(SearchSourceBuilder searchBuilder, MediaForm form, FilterBuilder profileFilter) {
        buildFacets(searchBuilder, form, profileFilter, "");
    }

    /**
     * @param prefix        empty string of path to this field including the last dot e.g., "embeds.media."
     */
    public static void buildFacets(SearchSourceBuilder searchBuilder, MediaForm form, FilterBuilder profileFilter, @Nonnull String prefix) {
        if(form != null && form.isFaceted()) {

            MediaFacets facets = form.getFacets();

            // Under default ES behaviour profile filtering does not influence facet results
            FilterBuilder facetFilter;
            if(facets.getFilter() == null) {
                facetFilter = profileFilter;
            } else {
                facetFilter = ESMediaFilterBuilder.filter(facets.getFilter(), profileFilter);
            }

            // Append all aggregations to this filtered aggregation builder
            FilterAggregationBuilder aggregationBuilder = AggregationBuilders
                .filter(ROOT_FILTER)
                .filter(facetFilter);

            searchBuilder.aggregation(aggregationBuilder);

            {
                MediaFacet titles = facets.getTitles();
                addTextualTypeFacet(searchBuilder, addFacetFilter(titles, facetFilter, prefix), prefix + "titles", TextualType.MAIN, titles);
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
                addFacet(searchBuilder, addFacetFilter(tags, facetFilter, prefix), prefix + esField("tags", tags), tags, null);
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

        NestedBuilder nestedBuilder = getNestedBuilder(
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

        NestedBuilder nestedBuilder = getNestedBuilder(
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

        RelationSearch subSearch = facets.getSubSearch();
        for (RelationFacet facet : facets) {

            FilterBuilder filter = facet.hasSubSearch() ? ESMediaFilterBuilder.filter(facet.getSubSearch(), pathPrefix, "relations") : null;
            if (subSearch != null) {
                if (filter == null) {
                    filter = ESMediaFilterBuilder.filter(subSearch, pathPrefix, "relations");
                } else {
                    ESMediaFilterBuilder.filter((BoolFilterBuilder) filter, subSearch, pathPrefix, "relations");
                }
            }
            AggregationBuilder termsBuilder = getFilteredRelationTermsBuilder(
                pathPrefix,
                "relations",
                facetField,
                facet,
                filter
            );

            NestedBuilder nestedBuilder = getNestedBuilder(
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


    protected static AggregationBuilder getFilteredRelationTermsBuilder(
        String pathPrefix,
        String nestedField,
        String facetField,
        RelationFacet facet,
        FilterBuilder subSearch
    ) {
        return getFilteredTermsBuilder(pathPrefix, nestedField, esField(facetField, facet), facet, facet.getName(), subSearch);
    }

    private static AggregationBuilder filterAggregation(String pathPrefix, String facetName, AggregationBuilder aggregationBuilder, MediaSearch... mediaSearch) {
        if (mediaSearch == null) {
            return aggregationBuilder;
        }

        for (MediaSearch search : mediaSearch) {
            FilterBuilder facetFilter = ESMediaFilterBuilder.filter(search, pathPrefix);
            aggregationBuilder = AggregationBuilders
                .filter("filter_" + facetName)
                .filter(facetFilter)
                .subAggregation(aggregationBuilder);
        }

        return aggregationBuilder;
    }

    private static void addTextualTypeFacet(SearchSourceBuilder searchBuilder, FilterBuilder filterBuilder, String fieldName, TextualType type, TextFacet facet) {
        FilterBuilder mainTitleFilter = FilterBuilders.andFilter(
            filterBuilder,
            FilterBuilders.termFilter(fieldName + ".type", type.name())
        );

        addFacet(searchBuilder, mainTitleFilter, fieldName + ".value.full", facet, null);
    }

    private static FilterBuilder addFacetFilter(TextFacet<MediaSearch> facet, FilterBuilder filterBuilder, String prefix) {
        if(facet != null && facet.getFilter() != null) {
            filterBuilder = ESMediaFilterBuilder.filter(facet.getFilter(), filterBuilder, prefix);
        }
        return filterBuilder;
    }
}

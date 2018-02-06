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

    protected static final String ROOT_FILTER = "mediaRootFilter";


    public static void facets(SearchSourceBuilder searchBuilder, MediaForm form) {
        buildFacets("", searchBuilder, form);
    }

    /**
     * @param prefix empty string of path to this field including the last dot e.g., "embeds.media."
     */
    public static void buildFacets(
        @Nonnull String prefix,
        SearchSourceBuilder searchBuilder,
        MediaForm form) {
        if (form != null && form.isFaceted()) {

            MediaFacets facets = form.getFacets();
            BoolQueryBuilder facetFilter = QueryBuilders.boolQuery();

            // Under default ES behaviour profile filtering does not influence facet results
            if (facets.getFilter() != null) {
                MediaSearch search = facets.getFilter();
                ESMediaFilterBuilder.filter(prefix, search, facetFilter);
            }

            // Append all aggregations to this filtered aggregation builder
            FilterAggregationBuilder aggregationBuilder = AggregationBuilders
                .filter(ROOT_FILTER, facetFilter);

            searchBuilder.aggregation(aggregationBuilder);

            {
                TitleFacetList titles = facets.getTitles();
                if (titles != null) {
                    if (titles.asMediaFacet()) {
                        addFacetFilter(prefix, titles, facetFilter);
                        addTextualTypeFacet(searchBuilder, facetFilter, prefix + "titles", TextualType.MAIN, titles);
                    }
                    addNestedTitlesAggregations(aggregationBuilder, titles, prefix);
                }

            }

            {
                MediaFacet types = facets.getTypes();
                addFacetFilter(prefix, types, facetFilter);
                addFacet(searchBuilder, prefix + "type", types);

            }

            {
                MediaFacet avTypes = facets.getAvTypes();
                addFacetFilter(prefix, avTypes, facetFilter);
                addFacet(searchBuilder, prefix + "avType", avTypes);

            }

            {
                DateRangeFacets sortDates = facets.getSortDates();
                addFacet(searchBuilder, prefix + "sortDate", sortDates);
            }

            {
                MediaFacet broadcasters = facets.getBroadcasters();
                addFacetFilter(prefix, broadcasters, facetFilter);
                addFacet(searchBuilder, prefix + "broadcasters.id", broadcasters);

            }

            {
                MediaSearchableTermFacet genres = facets.getGenres();
                addNestedAggregation("genres", aggregationBuilder, "id", genres, prefix);
            }

            {
                ExtendedMediaFacet tags = facets.getTags();
                addFacetFilter(prefix, tags, facetFilter);
                addFacet(searchBuilder, prefix + esField("tags", tags), tags);
            }

            {
                DurationRangeFacets durations = facets.getDurations();
                addFacet(searchBuilder, prefix + "duration", durations);
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
                addFacetFilter(prefix, ageRatings, facetFilter);
                addFacet(searchBuilder, prefix + "ageRating", ageRatings);
            }

            {
                MediaFacet contentRatings = facets.getContentRatings();
                addFacetFilter(prefix, contentRatings, facetFilter);
                addFacet(searchBuilder, prefix + "contentRatings", contentRatings);

            }
        }
    }

    protected static void addNestedAggregation(
        String nestedField,
        FilterAggregationBuilder rootAggregation,
        String facetField,
        MediaSearchableTermFacet facet,
        String pathPrefix) {
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

    protected static void addNestedAggregation(
        String nestedField,
        FilterAggregationBuilder rootAggregation,
        String facetField,
        MemberRefFacet facet,
        String pathPrefix) {
        if (facet == null) {
            return;
        }

        AggregationBuilder terms = getFilteredTermsBuilder(
            pathPrefix,
            nestedField,
            facetField,
            facet,
            facet.hasSubSearch() ? ESMediaFilterBuilder.filter(pathPrefix, nestedField, facet.getSubSearch()) : null
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

    protected static void addNestedRelationAggregations(
        FilterAggregationBuilder rootAggregation,
        String facetField,
        RelationFacetList facets,
        String pathPrefix) {
        if (facets == null || facets.isEmpty()) {
            return;
        }

        RelationSearch relationSearch = facets.getSubSearch();
        for (RelationFacet facet : facets) {
            BoolQueryBuilder facetFilter = QueryBuilders.boolQuery();
            if (facet.hasSubSearch()) {
                ESMediaFilterBuilder.filter(pathPrefix, "relations", facet.getSubSearch(), facetFilter);
            }
            if (relationSearch != null) {
                ESMediaQueryBuilder.relationQuery(pathPrefix + "relations", relationSearch,  facetFilter);

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
            AggregationBuilder builder = filterAggregation(
                pathPrefix,
                facetName,
                nestedBuilder,
                facet.getFilter(),
                facets.getFilter()
            );
            rootAggregation.subAggregation(builder);
        }
    }

    protected static void addNestedTitlesAggregations(
        FilterAggregationBuilder rootAggregation,
        TitleFacetList facets,
        String pathPrefix) {
        if (facets == null || facets.isEmpty()) {
            return;
        }

        TitleSearch titleSearch = facets.getSubSearch();
        for (TitleFacet facet : facets) {
            QueryBuilder filter = facet.hasSubSearch() ?
                ESMediaFilterBuilder.filter(pathPrefix, "expandedTitles", facet.getSubSearch()) : null;
            if (titleSearch != null) {
                if (filter == null) {
                    filter = ESMediaFilterBuilder.filter(pathPrefix, "expandedTitles", titleSearch);
                } else {
                    ESMediaQueryBuilder.buildTitleQuery("", (BoolQueryBuilder) filter, titleSearch);
                }
            }
            if (filter == null) {
                throw new RuntimeException("No filter found for " + facet);
            }

            FilterAggregationBuilder filterAggregationBuilder = AggregationBuilders.filter(escapeFacetName(facet.getName()), filter);
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

    private static AggregationBuilder filterAggregation(
        String pathPrefix,
        String facetName,
        AggregationBuilder aggregationBuilder,
        MediaSearch... mediaSearch) {
        if (mediaSearch == null) {
            return aggregationBuilder;
        }

        for (MediaSearch search : mediaSearch) {
            BoolQueryBuilder facetFilter = QueryBuilders.boolQuery();
            ESMediaFilterBuilder.filter(pathPrefix, search, facetFilter);
            aggregationBuilder = AggregationBuilders
                .filter("filter_" + facetName, facetFilter)
                .subAggregation(aggregationBuilder);
        }

        return aggregationBuilder;
    }

    private static void addTextualTypeFacet(
        SearchSourceBuilder searchBuilder,
        QueryBuilder filterBuilder,
        String fieldName,
        TextualType type,
        TextFacet facet) {
        BoolQueryBuilder mainTitleFilter = QueryBuilders.boolQuery();
        mainTitleFilter.must(filterBuilder);
        mainTitleFilter.must(QueryBuilders.termQuery(fieldName + ".type", type.name()));
        addFacet(searchBuilder, fieldName + ".value.full", facet);
    }

    private static void addFacetFilter(
        String prefix,
        TextFacet<MediaSearch> facet,
        BoolQueryBuilder filterBuilder) {
        if (facet != null) {
            MediaSearch filter = facet.getFilter();
            if (filter != null) {
                ESMediaFilterBuilder.filter(prefix, filter, filterBuilder);
            }
        }
    }
}

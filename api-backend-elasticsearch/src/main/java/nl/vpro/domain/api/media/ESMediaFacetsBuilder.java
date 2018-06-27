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
            BoolQueryBuilder facetRootFilter = QueryBuilders.boolQuery();

            // Under default ES behaviour profile filtering does not influence facet results
            if (facets.getFilter() != null) {
                MediaSearch search = facets.getFilter();
                ESMediaQueryBuilder.query(prefix, search, facetRootFilter);
            }

            // Append all aggregations to this filtered aggregation builder
            FilterAggregationBuilder rootAggregation = AggregationBuilders
                .filter(ROOT_FILTER, facetRootFilter);

            searchBuilder.aggregation(rootAggregation);

            {
                TitleFacetList titles = facets.getTitles();
                if (titles != null) {
                    if (titles.asMediaFacet()) {
                        addFacet(searchBuilder, prefix +  "titles", titles, facetRootFilter);
                        //addTextualTypeFacet(searchBuilder, filterAggregationBuilder, prefix + "titles", TextualType.MAIN, titles);
                    }
                    addNestedTitlesAggregations(rootAggregation, titles, prefix);
                }

            }

            {
                MediaFacet types = facets.getTypes();
                addFacet(searchBuilder, prefix + "type", types, facetRootFilter);

            }

            {
                MediaFacet avTypes = facets.getAvTypes();
                addFacet(searchBuilder, prefix + "avType", avTypes, facetRootFilter);

            }

            {
                DateRangeFacets sortDates = facets.getSortDates();
                addFacet(searchBuilder, prefix + "sortDate", sortDates);
            }

            {
                MediaFacet broadcasters = facets.getBroadcasters();
                //addFacetFilter(prefix, broadcasters, facetRootFilter);
                addFacet(searchBuilder, prefix + "broadcasters.id", broadcasters, facetRootFilter);

            }

            {
                MediaSearchableTermFacet genres = facets.getGenres();
                addNestedAggregation("genres", rootAggregation, "id", genres, prefix);
            }

            {
                ExtendedMediaFacet tags = facets.getTags();
                //addFacetFilter(prefix, tags, facetRootFilter);
                addFacet(searchBuilder, prefix + esField("tags", tags), tags, facetRootFilter);
            }

            {
                DurationRangeFacets durations = facets.getDurations();
                addFacet(searchBuilder, prefix + "duration", durations);
            }

            {
                MemberRefFacet descendantOf = facets.getDescendantOf();
                addNestedAggregation("descendantOf", rootAggregation, "midRef", descendantOf, prefix);
            }

            {
                MemberRefFacet episodeOf = facets.getEpisodeOf();
                addNestedAggregation("episodeOf", rootAggregation, "midRef", episodeOf, prefix);
            }

            {
                MemberRefFacet memberOf = facets.getMemberOf();
                addNestedAggregation("memberOf", rootAggregation, "midRef", memberOf, prefix);
            }

            {
                RelationFacetList relations = facets.getRelations();
                addNestedRelationAggregations(rootAggregation, "value", relations, prefix);
            }

            {
                MediaFacet ageRatings = facets.getAgeRatings();
                //addFacetFilter(prefix, ageRatings, facetRootFilter);
                addFacet(searchBuilder, prefix + "ageRating", ageRatings, facetRootFilter);
            }

            {
                MediaFacet contentRatings = facets.getContentRatings();
                //addFacetFilter(prefix, contentRatings, facetRootFilter);
                addFacet(searchBuilder, prefix + "contentRatings", contentRatings, facetRootFilter);

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
            ESMediaQueryBuilder.query(pathPrefix, search, facetFilter);
            aggregationBuilder = AggregationBuilders
                .filter("filter_" + facetName, facetFilter)
                .subAggregation(aggregationBuilder);
        }

        return aggregationBuilder;
    }

    private static void addTextualTypeFacet(
        @Nonnull  SearchSourceBuilder searchBuilder,
        @Nonnull  QueryBuilder filterBuilder,
        @Nonnull  String fieldName,
        @Nonnull  TextualType type,
        TextFacet facet) {


        BoolQueryBuilder textualFilter = QueryBuilders.boolQuery();
        textualFilter.must(filterBuilder);
        textualFilter.must(QueryBuilders.termQuery(fieldName + ".type", type.name()));

        addFacet(searchBuilder, fieldName + ".value.full", facet, textualFilter);
    }




    /**
     * Returns the filter to wich given facet must be added.
     *
     * @param rootAggregationBuilder  If the facet itself does not have a filter, it must be added to the root filter.
     * @return The filter associated with the facet.
     */
    private static FilterAggregationBuilder addFacetFilter(
        String prefix,
        String facetName,
        TextFacet<MediaSearch> facet,
        FilterAggregationBuilder rootAggregationBuilder) {
        if (facet != null) {
            MediaSearch filter = facet.getFilter();
            if (filter != null) {
                BoolQueryBuilder facetFilter = QueryBuilders.boolQuery();
                ESMediaQueryBuilder.query(prefix, filter, facetFilter);
                FilterAggregationBuilder filterAggregationBuilder =
                    AggregationBuilders.filter(facetName, facetFilter);

                rootAggregationBuilder.subAggregation(filterAggregationBuilder);
                return filterAggregationBuilder;
            }
        }
        return rootAggregationBuilder;
    }
}
